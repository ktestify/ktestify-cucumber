/*
 * Copyright 2026 Nil MALHOMME (malhomme.nil+oss@icloud.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.ktestify.integration;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.github.ktestify.KtestifyMain;
import io.github.ktestify.config.KtestifyConfig;
import io.github.ktestify.notifications.hooks.NotificationHooksIntegrationTestSupport;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test runner for Cucumber scenarios tagged with {@code @integration}.
 *
 * <p>This class replaces the previous Docker Compose + fat-JAR approach by:
 *
 * <ol>
 *   <li>Spinning up a Kafka broker and Confluent Schema Registry via Testcontainers
 *   <li>Wiring their dynamic host/port values into {@link KtestifyConfig} before any scenario runs
 *   <li>Delegating test execution to {@link KtestifyMain#run(String[])} so bundled plugins are discovered and their
 *       Cucumber glue is registered before scenarios run
 * </ol>
 *
 * <p>Run via Maven:
 *
 * <pre>
 * mvn verify -Pintegration-tests
 * mvn verify -Pintegration-tests -Dcucumber.it.tags="@integration and @batch"
 * </pre>
 *
 * <p>The test is picked up by the Maven Failsafe plugin (suffix {@code IT}) during the {@code integration-test} phase.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CucumberIntegrationITTests {

    // ── Container images — keep in sync with KafkaTestExtension / SchemaRegistryTestExtension ──
    private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("apache/kafka:4.2.0");
    private static final DockerImageName SCHEMA_REGISTRY_IMAGE =
            DockerImageName.parse("confluentinc/cp-schema-registry:7.9.0");
    private static final int SCHEMA_REGISTRY_PORT = 8081;
    private static final String KAFKA_NETWORK_ALIAS = "kafka";
    private static final int AZURITE_BLOB_PORT = 10000;
    private static final DockerImageName AZURITE_IMAGE =
            DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:3.35.0");
    private static final String AZURITE_ACCOUNT_KEY =
            "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";
    private static final List<String> AZURE_CONTAINERS =
            List.of("ktestify-plugin-it-json", "ktestify-plugin-it-xml");

    // ── Topics required by all @integration scenarios ──────────────────────────────────────────
    private static final List<String> INTEGRATION_TOPICS =
            List.of("ktestify.raw-roundtrip", "ktestify.raw-batch", "ktestify.avro-orders", "ktestify.avro-roundtrip");

    // ── Order Avro schema — mirrors docker/schemas/Order.avsc exactly ──────────────────────────
    private static final String ORDER_AVRO_SCHEMA = """
            {
              "type": "record",
              "name": "Order",
              "namespace": "io.github.ktestify.sample",
              "doc": "Sample order event for ktestify integration tests",
              "fields": [
                { "name": "orderId",   "type": "string",         "doc": "Unique order identifier" },
                { "name": "amount",    "type": "double",          "doc": "Order total amount" },
                { "name": "currency",  "type": "string",          "doc": "ISO 4217 currency code" },
                { "name": "status",    "type": "string",          "doc": "Order status" },
                { "name": "createdAt", "type": ["null", "long"],  "default": null,
                  "doc": "Epoch millis — excluded from comparisons" }
              ]
            }
            """;

    // ── Schema Registry subjects to register (Order schema) ───────────────────────────────────
    private static final List<String> ORDER_SCHEMA_SUBJECTS = List.of(
            "Order-value", // schemaName = "Order" in DataTable
            "ktestify.avro-orders-value", // TopicNameStrategy fallback for avro-orders
            "ktestify.avro-roundtrip-value" // TopicNameStrategy fallback for avro-roundtrip
            );

    // ── Shared containers (started once for the whole IT class) ────────────────────────────────
    private static Network network;
    private static KafkaContainer kafkaContainer;
    private static GenericContainer<?> schemaRegistryContainer;
    private static GenericContainer<?> azuriteContainer;
    private static HttpServer httpServer;
    /** Must match the {@code baseUrl} declared in {@code plugin-steps.feature}. */
    private static final int HTTP_PORT = 18765;

    private static final String PLUGIN_FEATURE = "src/test/resources/features/integration/plugin-steps.feature";
    private static final AtomicInteger successfulPollRequests = new AtomicInteger();
    private static final ConcurrentLinkedQueue<CapturedRequest> httpRequests = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<CapturedRequest> notificationRequests = new ConcurrentLinkedQueue<>();

    // ── Default Cucumber tag filter — overridable via -Dcucumber.it.tags ──────────────────────
    private static final String DEFAULT_TAGS = "@integration";

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────────────────────

    @BeforeAll
    static void startContainers() throws IOException {
        // 1. Shared Docker network so Kafka and Schema Registry can talk to each other
        network = Network.newNetwork();

        // 2. Kafka (KRaft mode via apache/kafka image, same as KafkaTestExtension)
        kafkaContainer = new KafkaContainer(KAFKA_IMAGE)
                .withNetwork(network)
                .withNetworkAliases(KAFKA_NETWORK_ALIAS)
                .withReuse(false);
        kafkaContainer.start();

        // 3. Schema Registry (same image / config as SchemaRegistryTestExtension)
        schemaRegistryContainer = new GenericContainer<>(SCHEMA_REGISTRY_IMAGE)
                .withNetwork(network)
                .withExposedPorts(SCHEMA_REGISTRY_PORT)
                .withEnv(Map.of(
                        "SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS",
                        "PLAINTEXT://" + KAFKA_NETWORK_ALIAS + ":9093",
                        "SCHEMA_REGISTRY_HOST_NAME",
                        "schema-registry",
                        "SCHEMA_REGISTRY_LISTENERS",
                        "http://0.0.0.0:" + SCHEMA_REGISTRY_PORT,
                        "SCHEMA_REGISTRY_DEBUG",
                        "true"))
                .withReuse(false);
        schemaRegistryContainer.start();

        // 4. Start Azurite and a loopback HTTP service for the bundled transport plugins.
        azuriteContainer = new GenericContainer<>(AZURITE_IMAGE)
                .withExposedPorts(AZURITE_BLOB_PORT, 10001, 10002)
                .withCommand(
                        "azurite",
                        "--blobHost",
                        "0.0.0.0",
                        "--queueHost",
                        "0.0.0.0",
                        "--tableHost",
                        "0.0.0.0",
                        "--skipApiVersionCheck")
                .withReuse(false);
        azuriteContainer.start();
        startHttpServer();

        // 5. Wire the dynamic service URLs into KtestifyConfig before any scenario runs.
        wireConfig();

        // 6. Create required Kafka topics, Avro schemas, and Azure containers.
        String bootstrapServers = kafkaContainer.getBootstrapServers();
        String schemaRegistryUrl = "http://" + schemaRegistryContainer.getHost() + ":"
                + schemaRegistryContainer.getMappedPort(SCHEMA_REGISTRY_PORT);
        provisionTopics(bootstrapServers);
        registerSchemas(schemaRegistryUrl);
        provisionAzureContainers();
    }

    @AfterAll
    static void stopContainers() {
        KtestifyConfig.reset();
        NotificationHooksIntegrationTestSupport.reset();
        if (httpServer != null) {
            httpServer.stop(0);
        }
        if (azuriteContainer != null && azuriteContainer.isRunning()) {
            azuriteContainer.stop();
        }
        if (schemaRegistryContainer != null && schemaRegistryContainer.isRunning()) {
            schemaRegistryContainer.stop();
        }
        if (kafkaContainer != null && kafkaContainer.isRunning()) {
            kafkaContainer.stop();
        }
        if (network != null) {
            try {
                network.close();
            } catch (Exception e) {
                System.err.println("[CucumberIntegrationIT] Error closing network: " + e.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // Test
    // ─────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Runs all Cucumber scenarios whose tag expression matches {@code cucumber.it.tags} system property (default:
     * {@value #DEFAULT_TAGS}).
     *
     * <p>Reports are written to {@code target/cucumber-reports/} in both JSON and pretty formats so that the existing
     * CI summary / PR-comment steps can consume them without modification.
     */
    @Test
    @Order(1)
    void runIntegrationScenarios() {
        String tags = System.getProperty("cucumber.it.tags", DEFAULT_TAGS);
        String assetsDir = resolveAssetsDir();

        System.out.printf("[CucumberIntegrationIT] Running Cucumber with tags='%s', assetsDir='%s'%n", tags, assetsDir);

        int exitCode = runCucumber(tags, "src/test/resources/features");

        assertEquals(
                0,
                exitCode,
                "One or more Cucumber @integration scenarios failed — check target/cucumber-reports/cucumber.json for details.");
    }

    @Test
    @Order(2)
    void runsFirstPartyPluginGherkinScenariosAndNotifiesOnSuccess() throws Exception {
        int exitCode = runCucumber("@pluginIntegration", PLUGIN_FEATURE);

        assertEquals(0, exitCode, "A first-party plugin Gherkin scenario failed.");
        assertHttpPluginRequests();

        JsonNode summary = notificationSummary("/notifications/webhook");
        assertEquals("KTestify plugin integration tests", summary.path("suiteName").asText());
        assertEquals("PASSED", summary.path("status").asText());
        assertEquals(6, summary.path("totalCount").asInt());
        assertEquals(6, summary.path("passedCount").asInt());
        assertEquals(0, summary.path("failedCount").asInt());
        assertFalse(hasNotification("/notifications/slack"));
        assertTrue(hasNotification("/notifications/teams"));
        assertTrue(new ObjectMapper().readTree(notificationBody("/notifications/teams")).isObject());
    }

    @Test
    @Order(3)
    void reportsPluginFailurePathsWithoutMaskingTheirCucumberResult() throws Exception {
        int exitCode = runCucumber("@pluginFailure", PLUGIN_FEATURE);

        assertEquals(1, exitCode, "The negative plugin scenarios should fail inside Cucumber.");

        JsonNode summary = notificationSummary("/notifications/webhook");
        assertEquals("FAILED", summary.path("status").asText());
        assertEquals(10, summary.path("totalCount").asInt());
        assertEquals(10, summary.path("failedCount").asInt());
        assertTrue(hasNotification("/notifications/slack"));
        assertTrue(new ObjectMapper().readTree(notificationBody("/notifications/slack")).isObject());
        assertTrue(hasNotification("/notifications/teams"));
        assertEquals(3, notificationRequests.size());
    }

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Creates all Kafka topics required by the {@code @integration} Cucumber scenarios.
     *
     * <p>Uses the standard {@link AdminClient} API — single partition, replication factor 1 (single-broker container).
     */
    private static void provisionTopics(String bootstrapServers) {
        try (AdminClient admin =
                AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers))) {
            List<NewTopic> newTopics = INTEGRATION_TOPICS.stream()
                    .map(name -> new NewTopic(name, 1, (short) 1))
                    .toList();
            admin.createTopics(newTopics).all().get(30, TimeUnit.SECONDS);
            System.out.printf(
                    "[CucumberIntegrationIT] Created %d topics: %s%n", INTEGRATION_TOPICS.size(), INTEGRATION_TOPICS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Kafka topics for integration tests", e);
        }
    }

    /**
     * Registers the {@code Order} Avro schema in Schema Registry under all subjects needed by the Cucumber scenarios.
     *
     * <p>Uses the Schema Registry REST API directly (POST {@code /subjects/{subject}/versions}) so that this class has
     * no compile-time dependency on the Confluent Schema Registry Java client. The schema JSON is serialised via
     * Jackson (already a compile-time dependency of {@code ktestify-core}).
     *
     * <p>Subjects registered:
     *
     * <ul>
     *   <li>{@code Order-value} — resolved when {@code schemaName = "Order"} is set in the DataTable
     *   <li>{@code ktestify.avro-orders-value} — TopicNameStrategy fallback for the {@code avro-orders} topic
     *   <li>{@code ktestify.avro-roundtrip-value} — TopicNameStrategy fallback for the {@code avro-roundtrip} topic
     * </ul>
     */
    private static void registerSchemas(String schemaRegistryUrl) {
        // Build the SR REST payload: {"schema": "<escaped-json-string>"}
        String payload;
        try {
            payload = new ObjectMapper()
                    .createObjectNode()
                    .put("schema", ORDER_AVRO_SCHEMA)
                    .toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build schema registration payload", e);
        }

        HttpClient httpClient = HttpClient.newHttpClient();
        for (String subject : ORDER_SCHEMA_SUBJECTS) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(schemaRegistryUrl + "/subjects/" + subject + "/versions"))
                        .header("Content-Type", "application/vnd.schemaregistry.v1+json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200 || response.statusCode() == 409) {
                    // 200 = registered successfully, 409 = already exists — both are fine
                    System.out.printf(
                            "[CucumberIntegrationIT] Schema registered for subject '%s' (HTTP %d).%n",
                            subject, response.statusCode());
                } else {
                    throw new RuntimeException(String.format(
                            "Schema registration failed for subject '%s' — HTTP %d: %s",
                            subject, response.statusCode(), response.body()));
                }
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception e) {
                throw new RuntimeException("Failed to register schema for subject: " + subject, e);
            }
        }
    }

    /**
     * Resets and reloads {@link KtestifyConfig} with the Testcontainer bootstrap/schema-registry URLs so that all
     * Cucumber step definitions pick up the correct in-process values.
     */
    private static void wireConfig() {
        String bootstrapServers = kafkaContainer.getBootstrapServers();
        String schemaRegistryUrl = "http://" + schemaRegistryContainer.getHost() + ":"
                + schemaRegistryContainer.getMappedPort(SCHEMA_REGISTRY_PORT);
        String assetsDir = resolveAssetsDir();
        String azuriteConnectionString = azuriteConnectionString();
        String notificationBaseUrl = "http://" + httpServer.getAddress().getHostString() + ":"
                + httpServer.getAddress().getPort();

        System.out.printf("[CucumberIntegrationIT] Kafka bootstrap-servers : %s%n", bootstrapServers);
        System.out.printf("[CucumberIntegrationIT] Schema Registry URL     : %s%n", schemaRegistryUrl);
        System.out.printf("[CucumberIntegrationIT] Assets directory        : %s%n", assetsDir);

        Config overrides = ConfigFactory.parseMap(Map.of(
                "ktestify.kafka.bootstrap-servers", bootstrapServers,
                "ktestify.schema-registry.url", schemaRegistryUrl,
                "ktestify.framework.directories.assets", assetsDir,
                "ktestify.framework.reporting.enabled", false,
                "ktestify.plugins.http", Map.of("poll-interval", "50ms"),
                "ktestify.plugins.azure-blob",
                        Map.of("connection-string", azuriteConnectionString, "poll-interval", "50ms"),
                "ktestify.plugins.notifications", notificationsConfig(notificationBaseUrl)));

        KtestifyConfig.reset();
        KtestifyConfig.load(overrides);
    }

    private static Map<String, Object> notificationsConfig(String baseUrl) {
        return Map.of(
                "enabled",
                true,
                "suite",
                Map.of("name", "KTestify plugin integration tests"),
                "channels",
                List.of(
                        notificationChannel("webhook", "url", baseUrl + "/notifications/webhook", false),
                        notificationChannel("slack", "webhook-url", baseUrl + "/notifications/slack", true),
                        notificationChannel("teams", "webhook-url", baseUrl + "/notifications/teams", false)));
    }

    private static Map<String, Object> notificationChannel(
            String type, String urlKey, String url, boolean onFailureOnly) {
        return Map.of("type", type, "enabled", true, urlKey, url, "on-failure-only", onFailureOnly);
    }

    private static int runCucumber(String tags, String... features) {
        NotificationHooksIntegrationTestSupport.reset();
        notificationRequests.clear();
        httpRequests.clear();
        successfulPollRequests.set(0);
        wireConfig();
        clearAzureContainers();

        List<String> cucumberArgs = new ArrayList<>(List.of(
                "--tags",
                tags,
                "--plugin",
                "json:target/cucumber-reports/cucumber.json",
                "--plugin",
                "pretty"));
        cucumberArgs.addAll(List.of(features));
        return KtestifyMain.run(cucumberArgs.toArray(String[]::new));
    }

    private static void startHttpServer() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", HTTP_PORT), 0);
        httpServer.createContext("/", CucumberIntegrationITTests::handleHttpRequest);
        httpServer.start();
    }

    private static void handleHttpRequest(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        CapturedRequest request = new CapturedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestURI().toString(),
                requestBody,
                exchange.getRequestHeaders().getFirst("Authorization"));

        if (path.startsWith("/notifications/")) {
            notificationRequests.add(request);
            respond(exchange, 200, "application/json", "{}");
            return;
        }

        httpRequests.add(request);
        switch (path) {
            case "/api/orders/validate" -> {
                if (request.authorization() == null) {
                    respond(exchange, 401, "text/plain", "unauthorized");
                } else {
                    respond(
                            exchange,
                            200,
                            "application/json",
                            "{\"orderId\":\"HTTP-001\",\"status\":\"validated\",\"traceId\":\"volatile\"}\n");
                }
            }
            case "/api/echo" -> respond(exchange, 200, "application/json", request.body() + "\n");
            case "/api/orders/xml" -> respond(
                    exchange,
                    200,
                    "application/xml",
                    "<order><id>HTTP-XML-001</id><processedAt>volatile</processedAt></order>");
            case "/api/orders/status" -> {
                int status = successfulPollRequests.incrementAndGet() < 3 ? 202 : 200;
                respond(exchange, status, "application/json", "{}");
            }
            case "/api/orders/never-ready" -> respond(exchange, 202, "application/json", "{}");
            case "/api/orders/steady" -> respond(exchange, 200, "application/json", "{\"status\":\"steady\"}");
            default -> respond(exchange, 404, "text/plain", "not found");
        }
    }

    private static void respond(HttpExchange exchange, int status, String contentType, String body)
            throws IOException {
        byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, responseBody.length);
        try (var output = exchange.getResponseBody()) {
            output.write(responseBody);
        }
    }

    private static String azuriteConnectionString() {
        return "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=" + AZURITE_ACCOUNT_KEY
                + ";BlobEndpoint=http://" + azuriteContainer.getHost() + ":"
                + azuriteContainer.getMappedPort(AZURITE_BLOB_PORT) + "/devstoreaccount1;";
    }

    private static void provisionAzureContainers() {
        BlobServiceClient blobService =
                new BlobServiceClientBuilder().connectionString(azuriteConnectionString()).buildClient();
        AZURE_CONTAINERS.forEach(name -> blobService.getBlobContainerClient(name).createIfNotExists());
    }

    private static void clearAzureContainers() {
        BlobServiceClient blobService =
                new BlobServiceClientBuilder().connectionString(azuriteConnectionString()).buildClient();
        for (String name : AZURE_CONTAINERS) {
            BlobContainerClient container = blobService.getBlobContainerClient(name);
            container.listBlobs().forEach(blob -> container.getBlobClient(blob.getName()).deleteIfExists());
        }
    }

    private static void assertHttpPluginRequests() throws IOException {
        CapturedRequest fileRequest = httpRequests.stream()
                .filter(request -> request.path().equals("/api/orders/validate?source=it"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("HTTP file request with its query parameter was not observed."));
        assertEquals("POST", fileRequest.method());
        assertEquals("Bearer cucumber-token", fileRequest.authorization());
        assertEquals(
                Files.readString(Path.of(resolveAssetsDir(), "integration", "plugins", "http-request.json")),
                fileRequest.body());

        assertTrue(httpRequests.stream().anyMatch(request -> request.path().equals("/api/echo")));
        assertTrue(httpRequests.stream().anyMatch(request -> request.path().equals("/api/orders/xml")));
        assertTrue(httpRequests.stream().filter(request -> request.path().startsWith("/api/orders/status")).count()
                >= 3);
    }

    private static JsonNode notificationSummary(String path) throws IOException {
        return new ObjectMapper().readTree(notificationBody(path));
    }

    private static String notificationBody(String path) {
        return notificationRequests.stream()
                .filter(request -> request.path().equals(path))
                .map(CapturedRequest::body)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No notification request was sent to " + path));
    }

    private static boolean hasNotification(String path) {
        return notificationRequests.stream().anyMatch(request -> request.path().equals(path));
    }

    /**
     * Resolves the test assets directory. Honours the {@code ktestify.assets.dir} system property set by the Failsafe
     * plugin, then falls back to the standard Maven project layout.
     */
    private static String resolveAssetsDir() {
        String fromProp = System.getProperty("ktestify.assets.dir");
        if (fromProp != null && !fromProp.isBlank()) {
            return fromProp;
        }
        // Fallback: relative path that works when running from the module root
        return Path.of("src", "test", "resources", "data").toAbsolutePath().toString();
    }

    private record CapturedRequest(String method, String path, String body, String authorization) {}
}

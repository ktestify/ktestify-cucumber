/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.github.ktestify.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.cucumber.core.cli.Main;
import io.github.ktestify.config.KtestifyConfig;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Integration test runner for Cucumber scenarios tagged with {@code @integration}.
 *
 * <p>This class replaces the previous Docker Compose + fat-JAR approach by:
 *
 * <ol>
 *   <li>Spinning up a Kafka broker and Confluent Schema Registry via Testcontainers
 *   <li>Wiring their dynamic host/port values into {@link KtestifyConfig} before any scenario runs
 *   <li>Delegating test execution to the Cucumber CLI ({@link Main#run}) so that all {@code @integration}-tagged
 *       scenarios are run in-process as part of the standard Maven {@code verify} lifecycle
 * </ol>
 *
 * <p>Run via Maven:
 *
 * <pre>
 * mvn verify -Pintegration-tests
 * mvn verify -Pintegration-tests -Dcucumber.it.tags="@integration and @batch"
 * </pre>
 *
 * <p>The test is picked up by the Maven Failsafe plugin (suffix {@code IT}) during the
 * {@code integration-test} phase.
 */
class CucumberIntegrationIT {

    // ── Container images — keep in sync with KafkaTestExtension / SchemaRegistryTestExtension ──
    private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("apache/kafka:4.2.0");
    private static final DockerImageName SCHEMA_REGISTRY_IMAGE =
            DockerImageName.parse("confluentinc/cp-schema-registry:7.9.0");
    private static final int SCHEMA_REGISTRY_PORT = 8081;
    private static final String KAFKA_NETWORK_ALIAS = "kafka";

    // ── Shared containers (started once for the whole IT class) ────────────────────────────────
    private static Network network;
    private static KafkaContainer kafkaContainer;
    private static GenericContainer<?> schemaRegistryContainer;

    // ── Default Cucumber tag filter — overridable via -Dcucumber.it.tags ──────────────────────
    private static final String DEFAULT_TAGS = "@integration";

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────────────────────

    @BeforeAll
    static void startContainers() {
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

        // 4. Wire the dynamic container URLs into KtestifyConfig before any scenario runs
        wireConfig();
    }

    @AfterAll
    static void stopContainers() {
        KtestifyConfig.reset();
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
     * <p>Reports are written to {@code target/cucumber-reports/} in both JSON and pretty formats so that the existing CI
     * summary / PR-comment steps can consume them without modification.
     */
    @Test
    void runIntegrationScenarios() {
        String tags = System.getProperty("cucumber.it.tags", DEFAULT_TAGS);
        String assetsDir = resolveAssetsDir();

        String[] cucumberArgs = {
            "--glue", "io.github.ktestify",
            "--tags", tags,
            "--plugin", "json:target/cucumber-reports/cucumber.json",
            "--plugin", "pretty",
            "src/test/resources/features"
        };

        System.out.printf(
                "[CucumberIntegrationIT] Running Cucumber with tags='%s', assetsDir='%s'%n", tags, assetsDir);

        byte exitCode = Main.run(cucumberArgs, Thread.currentThread().getContextClassLoader());

        assertEquals(0, exitCode, "One or more Cucumber @integration scenarios failed — check target/cucumber-reports/cucumber.json for details.");
    }

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Resets and reloads {@link KtestifyConfig} with the Testcontainer bootstrap/schema-registry URLs so that all
     * Cucumber step definitions pick up the correct in-process values.
     */
    private static void wireConfig() {
        String bootstrapServers = kafkaContainer.getBootstrapServers();
        String schemaRegistryUrl = "http://" + schemaRegistryContainer.getHost() + ":"
                + schemaRegistryContainer.getMappedPort(SCHEMA_REGISTRY_PORT);
        String assetsDir = resolveAssetsDir();

        System.out.printf(
                "[CucumberIntegrationIT] Kafka bootstrap-servers : %s%n", bootstrapServers);
        System.out.printf(
                "[CucumberIntegrationIT] Schema Registry URL     : %s%n", schemaRegistryUrl);
        System.out.printf(
                "[CucumberIntegrationIT] Assets directory        : %s%n", assetsDir);

        Config overrides = ConfigFactory.parseMap(Map.of(
                "ktestify.kafka.bootstrap-servers", bootstrapServers,
                "ktestify.schema-registry.url", schemaRegistryUrl,
                "ktestify.framework.directories.assets", assetsDir));

        KtestifyConfig.reset();
        KtestifyConfig.load(overrides);
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
}


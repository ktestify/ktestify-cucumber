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
package io.github.ktestify.services;

import com.google.gson.Gson;
import io.github.ktestify.entities.KtestifyAssetsDirectory;
import io.github.ktestify.io.kafka.KafkaClientFactory;
import io.github.ktestify.io.kafka.ProducerContext;
import io.github.ktestify.io.kafka.impl.AvroKafkaProducer;
import io.github.ktestify.io.kafka.impl.RawKafkaProducer;
import io.github.ktestify.models.Topic;
import io.github.ktestify.utils.DataTableUtils;
import io.github.ktestify.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.Producer;

import java.io.File;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static io.github.ktestify.utils.DataTableUtils.Constants.*;

/**
 * Orchestrates Kafka producer operations for Cucumber step definitions.
 *
 * <p>Builds a typed {@link ProducerContext} from the step DataTable row and delegates to the appropriate concrete
 * producer ({@link RawKafkaProducer} or {@link AvroKafkaProducer}). Kafka clients are created through
 * {@link KafkaClientFactory}.
 *
 * <p><b>Producer lifecycle.</b> One raw and one Avro {@link Producer} are created lazily, on the first row that needs
 * them, and are then reused for every subsequent row and step handled by this instance. Kafka producers are thread-safe
 * and expensive to create (network threads, metadata fetch, buffer pool), so creating one per DataTable row would be
 * wasteful and would leak sockets and threads. The owner of this service (the step definition class) must call
 * {@link #close()} when the scenario ends; {@link #close()} is idempotent and the service may be reused afterwards, in
 * which case new producers are created on demand.
 *
 * @since 1.2.1
 */
@Slf4j
public class ProducerValidationService implements AutoCloseable {

    public static final String DATA_TABLE_HEADER_FILE = "headerFile";

    /** Maximum time {@link #close()} waits for each producer to flush in-flight records and release resources. */
    static final Duration CLOSE_TIMEOUT = Duration.ofSeconds(10);

    private static final Gson GSON = new Gson();

    private final Supplier<Producer<String, String>> rawProducerFactory;
    private final Supplier<Producer<String, GenericRecord>> avroProducerFactory;

    private Producer<String, String> rawProducer;
    private Producer<String, GenericRecord> avroProducer;

    /** Creates a service whose producers are built by {@link KafkaClientFactory} from the ktestify configuration. */
    public ProducerValidationService() {
        this(KafkaClientFactory::createRawProducer, KafkaClientFactory::createAvroProducer);
    }

    /**
     * Creates a service with custom producer factories (for example to inject a {@code MockProducer} in tests).
     *
     * <p>Each factory is invoked at most once between two calls to {@link #close()}.
     *
     * @param rawProducerFactory supplies the raw (String key/value) producer
     * @param avroProducerFactory supplies the Avro producer
     * @since 1.2.1
     */
    public ProducerValidationService(
            Supplier<Producer<String, String>> rawProducerFactory,
            Supplier<Producer<String, GenericRecord>> avroProducerFactory) {
        this.rawProducerFactory = Objects.requireNonNull(rawProducerFactory, "rawProducerFactory");
        this.avroProducerFactory = Objects.requireNonNull(avroProducerFactory, "avroProducerFactory");
    }

    // =========================================================================
    // Raw (String) producers
    // =========================================================================

    /**
     * Sends a raw JSON/text record from a file to a Kafka INPUT topic.
     *
     * @param row DataTable row from the step
     * @param topic the resolved INPUT topic
     * @param assets optional assets directory for resolving relative paths
     */
    public void sendRawFromFile(Map<String, String> row, Topic topic, KtestifyAssetsDirectory assets) {
        String filePath = resolve(assets, getString(row, DATA_TABLE_FILE));
        String recordKey = getString(row, DATA_TABLE_RECORD_KEY);
        String headerFilePath = resolve(assets, getString(row, DATA_TABLE_HEADER_FILE));

        File payloadFile = new File(filePath);
        Map<String, String> headers = loadHeaders(headerFilePath);

        ProducerContext<String, String> ctx = ProducerContext.<String, String>builder()
                .topic(topic)
                .producer(rawProducer())
                .payloadFile(payloadFile)
                .recordKey(recordKey)
                .headers(headers)
                .build();

        try {
            new RawKafkaProducer(ctx).send();
            log.info("Raw record sent to topic '{}' from file '{}'.", topic.getNamespacedTopic(), filePath);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to send raw record to topic '" + topic.getNamespacedTopic() + "': " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // Avro producers
    // =========================================================================

    /**
     * Sends an Avro record from a JSON file to a Kafka INPUT topic, using the named schema.
     *
     * @param row DataTable row from the step
     * @param topic the resolved INPUT topic
     * @param assets optional assets directory
     */
    public void sendAvroFromFile(Map<String, String> row, Topic topic, KtestifyAssetsDirectory assets) {
        String filePath = resolve(assets, getString(row, DATA_TABLE_FILE));
        String recordKey = getString(row, DATA_TABLE_RECORD_KEY);
        String schemaName = getString(row, DATA_TABLE_SCHEMA_NAME);
        String schemaVersion = getString(row, DATA_TABLE_SCHEMA_VERSION);

        File payloadFile = new File(filePath);

        ProducerContext<String, GenericRecord> ctx = ProducerContext.<String, GenericRecord>builder()
                .topic(topic)
                .producer(avroProducer())
                .payloadFile(payloadFile)
                .recordKey(recordKey)
                .schemaName(schemaName)
                .schemaVersion(schemaVersion)
                .build();

        try {
            new AvroKafkaProducer(ctx).send();
            log.info("Avro record sent to topic '{}' from file '{}'.", topic.getNamespacedTopic(), filePath);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to send Avro record to topic '" + topic.getNamespacedTopic() + "': " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    /**
     * Closes the producers created by this service, if any. Each producer is given up to {@link #CLOSE_TIMEOUT} to
     * flush. Failures are logged and never propagated, so that a close problem cannot mask the scenario result.
     * Idempotent.
     *
     * @since 1.2.1
     */
    @Override
    public synchronized void close() {
        closeQuietly(rawProducer, "raw");
        closeQuietly(avroProducer, "Avro");
        rawProducer = null;
        avroProducer = null;
    }

    private synchronized Producer<String, String> rawProducer() {
        if (rawProducer == null) {
            rawProducer = Objects.requireNonNull(rawProducerFactory.get(), "raw producer factory returned null");
            log.debug("Raw Kafka producer created.");
        }
        return rawProducer;
    }

    private synchronized Producer<String, GenericRecord> avroProducer() {
        if (avroProducer == null) {
            avroProducer = Objects.requireNonNull(avroProducerFactory.get(), "Avro producer factory returned null");
            log.debug("Avro Kafka producer created.");
        }
        return avroProducer;
    }

    private static void closeQuietly(Producer<?, ?> producer, String kind) {
        if (producer == null) {
            return;
        }
        try {
            producer.close(CLOSE_TIMEOUT);
            log.debug("{} Kafka producer closed.", kind);
        } catch (RuntimeException e) {
            log.warn("Failed to close {} Kafka producer: {}", kind, e.getMessage(), e);
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private static String getString(Map<String, String> row, String col) {
        return DataTableUtils.getString(row, col);
    }

    private static String resolve(KtestifyAssetsDirectory assets, String path) {
        return assets != null ? assets.resolve(path) : path;
    }

    /**
     * Loads headers from a JSON file at the given path. Returns an empty map if path is null. Header file format: a
     * flat JSON object where each key/value becomes a Kafka header.
     */
    private static Map<String, String> loadHeaders(String headerFilePath) {
        if (headerFilePath == null) {
            return Map.of();
        }
        try {
            String content = FileUtils.getFileContent(FileUtils.getFile(headerFilePath));
            @SuppressWarnings("unchecked")
            Map<String, String> headers = GSON.fromJson(content, Map.class);
            return headers != null ? headers : Map.of();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load headers from '" + headerFilePath + "': " + e.getMessage(), e);
        }
    }
}

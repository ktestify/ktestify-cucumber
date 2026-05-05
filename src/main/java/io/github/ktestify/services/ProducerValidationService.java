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
package io.github.ktestify.services;

import static io.github.ktestify.utils.DataTableUtils.Constants.*;

import io.github.ktestify.entities.KtestifyAssetsDirectory;
import io.github.ktestify.io.kafka.KafkaClientFactory;
import io.github.ktestify.io.kafka.ProducerContext;
import io.github.ktestify.io.kafka.impl.AvroKafkaProducer;
import io.github.ktestify.io.kafka.impl.RawKafkaProducer;
import io.github.ktestify.models.Topic;
import io.github.ktestify.utils.DataTableUtils;
import java.io.File;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;

/**
 * Orchestrates Kafka producer operations for Cucumber step definitions.
 *
 * <p>Builds a typed {@link ProducerContext} from the step DataTable row and delegates to the appropriate concrete
 * producer ({@link RawKafkaProducer} or {@link AvroKafkaProducer}).
 *
 * <p>This service never imports raw {@code org.apache.kafka.*} classes — client creation is fully delegated to
 * {@link KafkaClientFactory}.
 */
@Slf4j
public class ProducerValidationService {

    public static final String DATA_TABLE_HEADER_FILE = "headerFile";


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
                .producer(KafkaClientFactory.createRawProducer())
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
                .producer(KafkaClientFactory.createAvroProducer())
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
            String content = io.github.ktestify.utils.FileUtils.getFileContent(
                    io.github.ktestify.utils.FileUtils.getFile(headerFilePath));
            // Parse simple flat JSON object: {"key": "value", ...}
            com.google.gson.Gson gson = new com.google.gson.Gson();
            @SuppressWarnings("unchecked")
            Map<String, String> headers = gson.fromJson(content, Map.class);
            return headers != null ? headers : Map.of();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load headers from '" + headerFilePath + "': " + e.getMessage(), e);
        }
    }
}

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

import static io.github.ktestify.match.RecordMatcherFactory.*;
import static io.github.ktestify.utils.DataTableUtils.Constants.*;

import io.github.ktestify.entities.KtestifyAssetsDirectory;
import io.github.ktestify.exceptions.ConsumerException;
import io.github.ktestify.io.kafka.ConsumerContext;
import io.github.ktestify.io.kafka.KafkaClientFactory;
import io.github.ktestify.io.kafka.impl.AvroKafkaConsumer;
import io.github.ktestify.io.kafka.impl.RawKafkaConsumer;
import io.github.ktestify.models.Topic;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;

/**
 * Orchestrates Kafka consumer validation for Cucumber step definitions.
 *
 * <p>Builds a typed {@link ConsumerContext}, submits the consumer to an {@link ExecutorService}, and applies a
 * two-layer timeout (inner: consumer poll; outer: executor guard with {@code BUFFER_TIME} ms extra.
 *
 * <p>All delta-time conversions are handled here: DataTable {@code consumerDeltaTime} is in <b>seconds</b> → multiplied
 * by 1000 before setting {@link ConsumerContext#getConsumerDeltaTime()} which expects <b>milliseconds</b>.
 *
 * <p>This service never imports raw {@code org.apache.kafka.*} classes directly — it delegates client creation entirely
 * to {@link KafkaClientFactory}.
 */
@Slf4j
public class ConsumerValidationService {

    /** Extra buffer added on top of the read timeout for the outer executor guard (ms). */
    private static final long BUFFER_TIME_MS = 5000;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    // =========================================================================
    // Raw (String) consumers
    // =========================================================================

    /**
     * Validates a single raw record against a file.
     *
     * @param row DataTable row from the step
     * @param topic the resolved OUTPUT topic
     * @param assets optional assets directory for resolving relative paths
     */
    public void validateRawFromFile(Map<String, String> row, Topic topic, KtestifyAssetsDirectory assets) {

        String file = resolve(assets, getString(row, DATA_TABLE_FILE));
        String expectedKey = getString(row, DATA_TABLE_EXPECTED_RECORD_KEY);
        Long readTimeout = getReadTimeoutMs(row);
        Long deltaTime = getSecondsToMillis(row, DATA_TABLE_CONSUMER_DELTA_TIME);

        ConsumerContext<String, String> ctx = ConsumerContext.<String, String>builder()
                .topic(topic)
                .consumer(KafkaClientFactory.createRawConsumer())
                .matchMethod(METHOD_MATCH_FILE)
                .matchFilePath(file)
                .expectedRecordKey(expectedKey)
                .readTimeout(readTimeout)
                .consumerDeltaTime(deltaTime)
                .build();

        execute(ctx, new RawKafkaConsumer(ctx), readTimeout);
    }

    /**
     * Validates a single raw record using positional field matching.
     *
     * @param row DataTable row
     * @param topic the resolved OUTPUT topic
     * @param assets optional assets directory
     */
    public void validateRawFields(Map<String, String> row, Topic topic, KtestifyAssetsDirectory assets) {
        String file = resolve(assets, getString(row, DATA_TABLE_FILE));
        String expectedKey = getString(row, DATA_TABLE_EXPECTED_RECORD_KEY);
        Integer line = getInt(row, DATA_TABLE_FIELD_TO_MATCH_LINE);
        Integer from = getInt(row, DATA_TABLE_FIELD_TO_MATCH_FROM);
        Integer to = getInt(row, DATA_TABLE_FIELD_TO_MATCH_TO);
        Long readTimeout = getReadTimeoutMs(row);
        Long deltaTime = getSecondsToMillis(row, DATA_TABLE_CONSUMER_DELTA_TIME);

        // Position descriptor encoded in matchKey as "line:from:to"
        String posDescriptor = line + ":" + from + ":" + to;

        ConsumerContext<String, String> ctx = ConsumerContext.<String, String>builder()
                .topic(topic)
                .consumer(KafkaClientFactory.createRawConsumer())
                .matchMethod(METHOD_FIELDS_TO_MATCH)
                .matchFilePath(file)
                .expectedRecordKey(expectedKey)
                .readTimeout(readTimeout)
                .consumerDeltaTime(deltaTime)
                .build();

        // FieldsRecordMatcher reads posDescriptor via matchKey — inject via a custom consumer
        io.github.ktestify.match.MatchContext matchCtx = io.github.ktestify.match.MatchContext.builder()
                .matchMethod(METHOD_FIELDS_TO_MATCH)
                .matchFilePath(file)
                .matchKey(posDescriptor)
                .strictMatching(false)
                .build();

        executeWithContext(
                ctx,
                new RawKafkaConsumer(ctx, new io.github.ktestify.match.impl.FieldsRecordMatcher()) {
                    @Override
                    protected io.github.ktestify.match.MatchContext buildMatchContext() {
                        return matchCtx;
                    }
                },
                readTimeout);
    }

    /**
     * Validates a single raw record against an XML file with optional element exclusions.
     *
     * @param row DataTable row
     * @param topic the resolved OUTPUT topic
     * @param assets optional assets directory
     */
    public void validateRawXml(Map<String, String> row, Topic topic, KtestifyAssetsDirectory assets) {
        String file = resolve(assets, getString(row, DATA_TABLE_FILE));
        String expectedKey = getString(row, DATA_TABLE_EXPECTED_RECORD_KEY);
        List<String> excluded = splitComma(getString(row, DATA_TABLE_FIELD_TO_MATCH_EXCLUDE_ELEMENTS));
        Long readTimeout = getReadTimeoutMs(row);
        Long deltaTime = getSecondsToMillis(row, DATA_TABLE_CONSUMER_DELTA_TIME);

        ConsumerContext<String, String> ctx = ConsumerContext.<String, String>builder()
                .topic(topic)
                .consumer(KafkaClientFactory.createRawConsumer())
                .matchMethod(METHOD_MATCH_XML)
                .matchFilePath(file)
                .expectedRecordKey(expectedKey)
                .excludedFields(excluded)
                .readTimeout(readTimeout)
                .consumerDeltaTime(deltaTime)
                .build();

        execute(ctx, new RawKafkaConsumer(ctx), readTimeout);
    }

    /**
     * Validates a single raw record against an XML file using XPath expressions.
     *
     * @param row DataTable row
     * @param topic the resolved OUTPUT topic
     * @param assets optional assets directory
     */
    public void validateRawXPath(Map<String, String> row, Topic topic, KtestifyAssetsDirectory assets) {
        String file = resolve(assets, getString(row, DATA_TABLE_FILE));
        String expectedKey = getString(row, DATA_TABLE_EXPECTED_RECORD_KEY);
        List<String> xpaths = splitComma(getString(row, DATA_TABLE_FIELD_TO_MATCH_XPATH));
        Long readTimeout = getReadTimeoutMs(row);
        Long deltaTime = getSecondsToMillis(row, DATA_TABLE_CONSUMER_DELTA_TIME);

        ConsumerContext<String, String> ctx = ConsumerContext.<String, String>builder()
                .topic(topic)
                .consumer(KafkaClientFactory.createRawConsumer())
                .matchMethod(METHOD_MATCH_XPATH)
                .matchFilePath(file)
                .expectedRecordKey(expectedKey)
                .excludedFields(xpaths) // XPathRecordMatcher reads expressions from excludedFields
                .readTimeout(readTimeout)
                .consumerDeltaTime(deltaTime)
                .build();

        execute(ctx, new RawKafkaConsumer(ctx), readTimeout);
    }

    /**
     * Validates a batch of raw records each against a corresponding file (by index).
     *
     * @param row DataTable row
     * @param topic the resolved OUTPUT topic
     * @param assets optional assets directory
     */
    public void validateRawBatch(Map<String, String> row, Topic topic, KtestifyAssetsDirectory assets) {
        List<String> files = splitAndResolve(assets, getString(row, DATA_TABLE_FILES));
        int expectedCount = Integer.parseInt(getString(row, DATA_TABLE_EXPECTED_RECORDS_COUNT));
        Long readTimeout = getReadTimeoutMs(row);
        Long deltaTime = getSecondsToMillis(row, DATA_TABLE_CONSUMER_DELTA_TIME);

        ConsumerContext<String, String> ctx = ConsumerContext.<String, String>builder()
                .topic(topic)
                .consumer(KafkaClientFactory.createRawConsumer())
                .matchMethod(METHOD_MATCH_FILE)
                .matchFilePaths(files)
                .isBatchConsumer(true)
                .batchSize(expectedCount)
                .readTimeout(readTimeout)
                .consumerDeltaTime(deltaTime)
                .build();

        execute(ctx, new RawKafkaConsumer(ctx), readTimeout);
    }

    /**
     * Validates that a record does <em>not</em> appear (watcher / negative assertion). Uses
     * {@link io.github.ktestify.match.impl.NoOpRecordMatcher} — if a record arrives the step fails; timeout means pass.
     */
    public void validateNoRecord(Map<String, String> row, Topic topic) {
        String expectedKey = getString(row, DATA_TABLE_EXPECTED_RECORD_KEY);
        Long readTimeout = getReadTimeoutMs(row);
        Long deltaTime = getSecondsToMillis(row, DATA_TABLE_CONSUMER_DELTA_TIME);

        ConsumerContext<String, String> ctx = ConsumerContext.<String, String>builder()
                .topic(topic)
                .consumer(KafkaClientFactory.createRawConsumer())
                .expectedRecordKey(expectedKey)
                .readTimeout(readTimeout)
                .consumerDeltaTime(deltaTime)
                .build();

        // A record appearing within the timeout = test failure
        boolean recordArrived;
        try {
            recordArrived = runWithTimeout(new RawKafkaConsumer(ctx), readTimeout);
        } catch (ConsumerException e) {
            // Timeout = no record found = expected
            log.info("No record found on topic '{}' as expected.", topic.getNamespacedTopic());
            return;
        }
        if (recordArrived) {
            throw new AssertionError(
                    "Expected no record on topic '" + topic.getNamespacedTopic() + "' but one was found.");
        }
    }

    /** Validates that a record <em>does</em> appear (positive watcher). */
    public void validateRecordExists(Map<String, String> row, Topic topic) {
        String expectedKey = getString(row, DATA_TABLE_EXPECTED_RECORD_KEY);
        Long readTimeout = getReadTimeoutMs(row);
        Long deltaTime = getSecondsToMillis(row, DATA_TABLE_CONSUMER_DELTA_TIME);

        ConsumerContext<String, String> ctx = ConsumerContext.<String, String>builder()
                .topic(topic)
                .consumer(KafkaClientFactory.createRawConsumer())
                .expectedRecordKey(expectedKey)
                .readTimeout(readTimeout)
                .consumerDeltaTime(deltaTime)
                .build();

        execute(ctx, new RawKafkaConsumer(ctx), readTimeout);
    }

    /**
     * Validates that the key of a single raw record equals the expected key (KeyRecordMatcher).
     *
     * <p>Uses {@code expectedRecordKey} both as a KafkaRecordFetcher filter <em>and</em> as the assertion target.
     *
     * @param row DataTable row
     * @param topic the resolved OUTPUT topic
     */
    public void validateRawKeyOnly(Map<String, String> row, Topic topic) {
        String expectedKey = getString(row, DATA_TABLE_EXPECTED_RECORD_KEY);
        Long readTimeout = getReadTimeoutMs(row);
        Long deltaTime = getSecondsToMillis(row, DATA_TABLE_CONSUMER_DELTA_TIME);

        ConsumerContext<String, String> ctx = ConsumerContext.<String, String>builder()
                .topic(topic)
                .consumer(KafkaClientFactory.createRawConsumer())
                .matchMethod(METHOD_RECORD_KEY_MATCH)
                .expectedRecordKey(expectedKey)
                .readTimeout(readTimeout)
                .consumerDeltaTime(deltaTime)
                .build();

        io.github.ktestify.match.MatchContext matchCtx = io.github.ktestify.match.MatchContext.builder()
                .matchMethod(METHOD_RECORD_KEY_MATCH)
                .matchKey(expectedKey)
                .strictMatching(false)
                .build();

        executeWithContext(
                ctx,
                new RawKafkaConsumer(ctx, new io.github.ktestify.match.impl.KeyRecordMatcher()) {
                    @Override
                    protected io.github.ktestify.match.MatchContext buildMatchContext() {
                        return matchCtx;
                    }
                },
                readTimeout);
    }

    /**
     * Validates both the key and value of a single raw record (FileKeyRecordMatcher).
     *
     * <p>{@code expectedRecordKey} is used as the KafkaRecordFetcher key filter and also as the expected key assertion.
     * {@code file} provides the expected value content.
     *
     * @param row DataTable row
     * @param topic the resolved OUTPUT topic
     * @param assets optional assets directory
     */
    public void validateRawKeyValue(Map<String, String> row, Topic topic, KtestifyAssetsDirectory assets) {
        String file = resolve(assets, getString(row, DATA_TABLE_FILE));
        String expectedKey = getString(row, DATA_TABLE_EXPECTED_RECORD_KEY);
        Long readTimeout = getReadTimeoutMs(row);
        Long deltaTime = getSecondsToMillis(row, DATA_TABLE_CONSUMER_DELTA_TIME);

        ConsumerContext<String, String> ctx = ConsumerContext.<String, String>builder()
                .topic(topic)
                .consumer(KafkaClientFactory.createRawConsumer())
                .matchMethod(METHOD_MATCH_KEY_FILE)
                .matchFilePath(file)
                .expectedRecordKey(expectedKey)
                .readTimeout(readTimeout)
                .consumerDeltaTime(deltaTime)
                .build();

        io.github.ktestify.match.MatchContext matchCtx = io.github.ktestify.match.MatchContext.builder()
                .matchMethod(METHOD_MATCH_KEY_FILE)
                .matchFilePath(file)
                .matchKey(expectedKey)
                .strictMatching(false)
                .build();

        executeWithContext(
                ctx,
                new RawKafkaConsumer(ctx, new io.github.ktestify.match.impl.FileKeyRecordMatcher()) {
                    @Override
                    protected io.github.ktestify.match.MatchContext buildMatchContext() {
                        return matchCtx;
                    }
                },
                readTimeout);
    }

    // =========================================================================
    // Avro consumers
    // =========================================================================

    /**
     * Validates a single Avro record against a JSON file with optional field exclusions.
     *
     * @param row DataTable row
     * @param topic the resolved OUTPUT topic
     * @param assets optional assets directory
     */
    public void validateAvroFromFile(Map<String, String> row, Topic topic, KtestifyAssetsDirectory assets) {
        String file = resolve(assets, getString(row, DATA_TABLE_FILE));
        List<String> excluded = splitComma(getString(row, DATA_TABLE_FIELD_TO_MATCH_EXCLUDED_KEYS));
        Long readTimeout = getReadTimeoutMs(row);
        Long deltaTime = getSecondsToMillis(row, DATA_TABLE_CONSUMER_DELTA_TIME);

        ConsumerContext<String, GenericRecord> ctx = ConsumerContext.<String, GenericRecord>builder()
                .topic(topic)
                .consumer(KafkaClientFactory.createAvroConsumer())
                .matchMethod(METHOD_MATCH_FILE)
                .matchFilePath(file)
                .excludedFields(excluded)
                .readTimeout(readTimeout)
                .consumerDeltaTime(deltaTime)
                .build();

        execute(ctx, new AvroKafkaConsumer(ctx), readTimeout);
    }

    /**
     * Validates a single Avro record field against an inline key/value.
     *
     * @param row DataTable row
     * @param topic the resolved OUTPUT topic
     */
    public void validateAvroFieldValue(Map<String, String> row, Topic topic) {
        String key = getString(row, DATA_TABLE_FIELD_TO_MATCH_KEY);
        String value = getString(row, DATA_TABLE_FIELD_TO_MATCH_VALUE);
        Long readTimeout = getReadTimeoutMs(row);
        Long deltaTime = getSecondsToMillis(row, DATA_TABLE_CONSUMER_DELTA_TIME);

        ConsumerContext<String, GenericRecord> ctx = ConsumerContext.<String, GenericRecord>builder()
                .topic(topic)
                .consumer(KafkaClientFactory.createAvroConsumer())
                .matchMethod(METHOD_FIELDS_TO_MATCH)
                .readTimeout(readTimeout)
                .consumerDeltaTime(deltaTime)
                .build();

        io.github.ktestify.match.MatchContext matchCtx = io.github.ktestify.match.MatchContext.builder()
                .matchMethod(METHOD_FIELDS_TO_MATCH)
                .matchKey(key)
                .matchValue(value)
                .strictMatching(false)
                .build();

        executeWithContext(
                ctx,
                new AvroKafkaConsumer(ctx, new io.github.ktestify.match.impl.AvroFieldsRecordMatcher()) {
                    @Override
                    protected io.github.ktestify.match.MatchContext buildMatchContext() {
                        return matchCtx;
                    }
                },
                readTimeout);
    }

    /**
     * Validates a batch of Avro records each against a corresponding JSON file (by index).
     *
     * @param row DataTable row
     * @param topic the resolved OUTPUT topic
     * @param assets optional assets directory
     */
    public void validateAvroBatch(Map<String, String> row, Topic topic, KtestifyAssetsDirectory assets) {
        List<String> files = splitAndResolve(assets, getString(row, DATA_TABLE_FILES));
        int expectedCount = Integer.parseInt(getString(row, DATA_TABLE_EXPECTED_RECORDS_COUNT));
        List<String> excluded = splitComma(getString(row, DATA_TABLE_FIELD_TO_MATCH_EXCLUDED_KEYS));
        Long readTimeout = getReadTimeoutMs(row);
        Long deltaTime = getSecondsToMillis(row, DATA_TABLE_CONSUMER_DELTA_TIME);

        ConsumerContext<String, GenericRecord> ctx = ConsumerContext.<String, GenericRecord>builder()
                .topic(topic)
                .consumer(KafkaClientFactory.createAvroConsumer())
                .matchMethod(METHOD_MATCH_FILE)
                .matchFilePaths(files)
                .excludedFields(excluded)
                .isBatchConsumer(true)
                .batchSize(expectedCount)
                .readTimeout(readTimeout)
                .consumerDeltaTime(deltaTime)
                .build();

        execute(ctx, new AvroKafkaConsumer(ctx), readTimeout);
    }

    /**
     * Validates that the key of a single Avro record equals the expected key (AvroKeyRecordMatcher).
     *
     * @param row DataTable row
     * @param topic the resolved OUTPUT topic
     */
    public void validateAvroKeyOnly(Map<String, String> row, Topic topic) {
        String expectedKey = getString(row, DATA_TABLE_EXPECTED_RECORD_KEY);
        Long readTimeout = getReadTimeoutMs(row);
        Long deltaTime = getSecondsToMillis(row, DATA_TABLE_CONSUMER_DELTA_TIME);

        ConsumerContext<String, GenericRecord> ctx = ConsumerContext.<String, GenericRecord>builder()
                .topic(topic)
                .consumer(KafkaClientFactory.createAvroConsumer())
                .matchMethod(METHOD_RECORD_KEY_MATCH)
                .expectedRecordKey(expectedKey)
                .readTimeout(readTimeout)
                .consumerDeltaTime(deltaTime)
                .build();

        io.github.ktestify.match.MatchContext matchCtx = io.github.ktestify.match.MatchContext.builder()
                .matchMethod(METHOD_RECORD_KEY_MATCH)
                .matchKey(expectedKey)
                .strictMatching(false)
                .build();

        executeWithContext(
                ctx,
                new AvroKafkaConsumer(ctx, new io.github.ktestify.match.impl.AvroKeyRecordMatcher()) {
                    @Override
                    protected io.github.ktestify.match.MatchContext buildMatchContext() {
                        return matchCtx;
                    }
                },
                readTimeout);
    }

    // =========================================================================
    // Private execution helpers
    // =========================================================================

    private <K, V> void execute(
            ConsumerContext<K, V> ctx, java.util.concurrent.Callable<Boolean> consumer, Long readTimeoutSeconds) {
        boolean passed = runWithTimeout(consumer, readTimeoutSeconds);
        if (!passed) {
            throw new AssertionError(
                    "Consumer validation failed for topic '" + ctx.getTopic().getNamespacedTopic() + "'.");
        }
    }

    private <K, V> void executeWithContext(
            ConsumerContext<K, V> ctx, java.util.concurrent.Callable<Boolean> consumer, Long readTimeoutSeconds) {
        execute(ctx, consumer, readTimeoutSeconds);
    }

    private boolean runWithTimeout(java.util.concurrent.Callable<Boolean> consumer, Long readTimeoutMs) {
        long timeoutMs = (readTimeoutMs != null ? readTimeoutMs : 10_000L) + BUFFER_TIME_MS;
        Future<Boolean> future = executor.submit(consumer);
        try {
            return Boolean.TRUE.equals(future.get(timeoutMs, TimeUnit.MILLISECONDS));
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new ConsumerException("Outer timeout exceeded after " + timeoutMs + "ms.");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ConsumerException ce) throw ce;
            throw new ConsumerException("Consumer execution failed: " + cause.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConsumerException("Consumer thread interrupted.");
        }
    }

    // =========================================================================
    // DataTable / path helpers
    // =========================================================================

    private static String getString(Map<String, String> row, String col) {
        String v = row.get(col);
        return (v != null && !v.isBlank()) ? v : null;
    }

    private static Long getSeconds(Map<String, String> row, String col) {
        String v = getString(row, col);
        return v != null ? Long.parseLong(v.trim()) : null;
    }

    private static Integer getInt(Map<String, String> row, String col) {
        String v = getString(row, col);
        return v != null ? Integer.parseInt(v.trim()) : null;
    }

    /** Reads seconds from the DataTable and converts to milliseconds. */
    private static Long getSecondsToMillis(Map<String, String> row, String col) {
        Long seconds = getSeconds(row, col);
        return seconds != null ? seconds * 1000L : null;
    }

    /**
     * Reads consumerReadTimeout from the DataTable (in seconds) and converts to milliseconds for use with
     * {@link ConsumerContext#getReadTimeout()} which expects ms.
     */
    private static Long getReadTimeoutMs(Map<String, String> row) {
        return getSecondsToMillis(row, DATA_TABLE_READ_TIMEOUT);
    }

    private static List<String> splitComma(String value) {
        if (value == null || value.isBlank()) return Collections.emptyList();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private static String resolve(KtestifyAssetsDirectory assets, String path) {
        return assets != null ? assets.resolve(path) : path;
    }

    private static List<String> splitAndResolve(KtestifyAssetsDirectory assets, String commaSeparated) {
        return splitComma(commaSeparated).stream().map(f -> resolve(assets, f)).toList();
    }
}

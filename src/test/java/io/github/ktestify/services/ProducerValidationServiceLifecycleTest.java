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

import io.github.ktestify.models.Topic;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies that {@link ProducerValidationService} reuses and closes its Kafka producers. No broker is required. */
@DisplayName("ProducerValidationService: producer lifecycle")
class ProducerValidationServiceLifecycleTest {

    @TempDir
    Path tmp;

    private final List<MockProducer<String, String>> rawCreated = new ArrayList<>();
    private final List<MockProducer<String, GenericRecord>> avroCreated = new ArrayList<>();
    private final AtomicInteger avroFactoryCalls = new AtomicInteger();

    private ProducerValidationService service;
    private Topic topic;
    private Path payload;

    @BeforeEach
    void setUp() throws IOException {
        service = new ProducerValidationService(
                () -> {
                    MockProducer<String, String> p =
                            new MockProducer<>(true, null, new StringSerializer(), new StringSerializer());
                    rawCreated.add(p);
                    return p;
                },
                () -> {
                    avroFactoryCalls.incrementAndGet();
                    Serializer<GenericRecord> noop = (t, v) -> new byte[0];
                    MockProducer<String, GenericRecord> p =
                            new MockProducer<>(true, null, new StringSerializer(), noop);
                    avroCreated.add(p);
                    return p;
                });
        topic = Topic.builder()
                .topicName("lifecycle-in")
                .topicType(Topic.Type.INPUT)
                .build();
        payload = Files.writeString(tmp.resolve("payload.json"), "{\"a\":1}", StandardCharsets.UTF_8);
    }

    private Map<String, String> row(String key) {
        return Map.of("file", payload.toString(), "recordKey", key);
    }

    @Test
    @DisplayName("one raw producer is shared by every DataTable row")
    void rawProducerReusedAcrossRows() {
        service.sendRawFromFile(row("k1"), topic, null);
        service.sendRawFromFile(row("k2"), topic, null);
        service.sendRawFromFile(row("k3"), topic, null);

        assertEquals(1, rawCreated.size());
        assertEquals(3, rawCreated.getFirst().history().size());
        assertFalse(rawCreated.getFirst().closed());
    }

    @Test
    @DisplayName("close() closes the producers it created and nothing else")
    void closeClosesOnlyCreatedProducers() {
        service.sendRawFromFile(row("k1"), topic, null);

        service.close();

        assertTrue(rawCreated.getFirst().closed());
        assertEquals(0, avroFactoryCalls.get(), "Avro producer must not be created when unused");
    }

    @Test
    @DisplayName("close() is idempotent and safe when nothing was created")
    void closeIsIdempotent() {
        assertDoesNotThrow(service::close);
        service.sendRawFromFile(row("k1"), topic, null);
        service.close();
        assertDoesNotThrow(service::close);
        assertEquals(1, rawCreated.size());
    }

    @Test
    @DisplayName("a new producer is created on demand after close()")
    void reusableAfterClose() {
        service.sendRawFromFile(row("k1"), topic, null);
        service.close();
        service.sendRawFromFile(row("k2"), topic, null);

        assertEquals(2, rawCreated.size());
        assertTrue(rawCreated.get(0).closed());
        assertFalse(rawCreated.get(1).closed());
        service.close();
        assertTrue(rawCreated.get(1).closed());
    }

    @Test
    @DisplayName("a failing close() is logged, not propagated")
    void failingCloseDoesNotPropagate() {
        ProducerValidationService failing = new ProducerValidationService(
                () -> new MockProducer<>(true, null, new StringSerializer(), new StringSerializer()) {
                    @Override
                    public void close(java.time.Duration timeout) {
                        throw new IllegalStateException("boom");
                    }
                },
                () -> {
                    MockProducer<String, GenericRecord> p =
                            new MockProducer<>(true, null, new StringSerializer(), (t, v) -> new byte[0]);
                    avroCreated.add(p);
                    return p;
                });
        failing.sendRawFromFile(row("k1"), topic, null);

        assertDoesNotThrow(failing::close);
    }
}

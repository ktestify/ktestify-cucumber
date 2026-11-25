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
package io.github.ktestify.steps;

import io.github.ktestify.exceptions.ConfigException;
import io.github.ktestify.exceptions.ConsumerException;
import io.github.ktestify.exceptions.ProducerException;
import io.github.ktestify.models.Topic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StepRowExecution — multi-row failure context")
class StepRowExecutionTest {

    private static final Topic TOPIC = Topic.builder()
            .topicName("orders")
            .topicNamespace(Topic.TopicNamespace.builder().namespace("dev").build())
            .topicType(Topic.Type.OUTPUT)
            .build();

    @Test
    @DisplayName("wraps AssertionError with step, row, topic, file, and key while preserving cause")
    void wrapsAssertionErrorWithFullRowContext() {
        AssertionError original = new AssertionError("field mismatch");
        Map<String, String> row = Map.of("file", "expected-2.json", "expectedRecordKey", "order-2");

        AssertionError wrapped = assertThrows(
                AssertionError.class,
                () -> StepRowExecution.runRow("Then expected record from file", 1, 3, TOPIC, row, () -> {
                    throw original;
                }));

        assertSame(original, wrapped.getCause());
        assertTrue(wrapped.getMessage().contains("step='Then expected record from file'"));
        assertTrue(wrapped.getMessage().contains("row=2/3"));
        assertTrue(wrapped.getMessage().contains("topic='dev.orders'"));
        assertTrue(wrapped.getMessage().contains("file='expected-2.json'"));
        assertTrue(wrapped.getMessage().contains("key='order-2'"));
        assertTrue(wrapped.getMessage().contains("field mismatch"));
    }

    @Test
    @DisplayName("preserves ConsumerException, ProducerException, and ConfigException types and causes")
    void preservesDomainExceptionTypes() {
        Map<String, String> row = Map.of("recordKey", "k-1");

        ConsumerException consumerCause = new ConsumerException("timeout");
        ConsumerException consumerWrapped = assertThrows(
                ConsumerException.class,
                () -> StepRowExecution.runRow("Then expected tombstone record", 0, 2, TOPIC, row, () -> {
                    throw consumerCause;
                }));
        assertSame(consumerCause, consumerWrapped.getCause());
        assertTrue(consumerWrapped.getMessage().contains("row=1/2"));
        assertTrue(consumerWrapped.getMessage().contains("key='k-1'"));

        ProducerException producerCause = new ProducerException("broker error");
        ProducerException producerWrapped = assertThrows(
                ProducerException.class,
                () -> StepRowExecution.runRow("When tombstone record is sent", 1, 2, TOPIC, row, () -> {
                    throw producerCause;
                }));
        assertSame(producerCause, producerWrapped.getCause());
        assertTrue(producerWrapped.getMessage().contains("row=2/2"));

        ConfigException configCause = new ConfigException("negative duration");
        ConfigException configWrapped = assertThrows(
                ConfigException.class,
                () -> StepRowExecution.runRow("Then expected record from file", 0, 1, TOPIC, Map.of(), () -> {
                    throw configCause;
                }));
        assertSame(configCause, configWrapped.getCause());
        assertFalse(configWrapped.getMessage().contains("file="));
        assertFalse(configWrapped.getMessage().contains("key="));
    }
}

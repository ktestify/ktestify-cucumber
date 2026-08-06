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

import static org.junit.jupiter.api.Assertions.*;

import io.cucumber.datatable.DataTable;
import io.cucumber.datatable.DataTableTypeRegistry;
import io.cucumber.datatable.DataTableTypeRegistryTableConverter;
import io.github.ktestify.exceptions.TopicMismatchException;
import io.github.ktestify.models.Topic;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ValidationStepDefinition}'s guard rails.
 *
 * <p>Single-record assertion steps ({@code expected record from file}, watchers, key-only assertions, …) support
 * multi-row DataTables as long as every row resolves to the <em>same physical topic</em>
 * ({@code resolveAndAssertSingleTopic}, backed by {@link io.github.ktestify.utils.TopicUtils#assertSingleTopic}).
 * Mixing topics in one DataTable throws {@link TopicMismatchException} before any consumer call is made.
 *
 * <p>Batch consumer steps ({@code expected records from files} / {@code ...based on schema}) remain single-row only
 * ({@code singleRow}), since their one row already describes an any-to-all match across N files in a single consumer
 * call.
 *
 * <p>All of these guards run before any Kafka broker interaction, so these tests require no live broker.
 */
@DisplayName("ValidationStepDefinition — guard rails")
class ValidationStepDefinitionMultiRowGuardTest {

    private static final Topic TOPIC_A = Topic.builder()
            .topicName("topic-a")
            .topicAlias("alias-a")
            .topicType(Topic.Type.OUTPUT)
            .build();

    private static final Topic TOPIC_B = Topic.builder()
            .topicName("topic-b")
            .topicAlias("alias-b")
            .topicType(Topic.Type.OUTPUT)
            .build();

    @Nested
    @DisplayName("resolveAndAssertSingleTopic (single-record assertion steps)")
    class ResolveAndAssertSingleTopic {

        @Test
        @DisplayName("all rows targeting the same topic (by alias) resolve without error")
        void sameTopicAllRowsPasses() throws Exception {
            SharedStepsResources resources = new SharedStepsResources();
            resources.topics.register(TOPIC_A.getTopicName(), TOPIC_A.getTopicAlias(), TOPIC_A);
            ValidationStepDefinition validation = new ValidationStepDefinition(resources);

            List<Map<String, String>> rows = List.of(
                    Map.of("topicAlias", "alias-a", "file", "f1.json", "expectedRecordKey", "k1"),
                    Map.of("topicAlias", "alias-a", "file", "f2.json", "expectedRecordKey", "k2"));

            Topic resolved = invokeResolveAndAssertSingleTopic(validation, rows);
            assertEquals(TOPIC_A.getNamespacedTopic(), resolved.getNamespacedTopic());
        }

        @Test
        @DisplayName("a single-row DataTable resolves without error")
        void singleRowPasses() throws Exception {
            SharedStepsResources resources = new SharedStepsResources();
            resources.topics.register(TOPIC_A.getTopicName(), TOPIC_A.getTopicAlias(), TOPIC_A);
            ValidationStepDefinition validation = new ValidationStepDefinition(resources);

            List<Map<String, String>> rows =
                    List.of(Map.of("topicAlias", "alias-a", "file", "f1.json", "expectedRecordKey", "k1"));

            Topic resolved = invokeResolveAndAssertSingleTopic(validation, rows);
            assertEquals(TOPIC_A.getNamespacedTopic(), resolved.getNamespacedTopic());
        }

        @Test
        @DisplayName("rows targeting two distinct topics throw TopicMismatchException before any consumer call")
        void mixedTopicsThrowsMismatch() {
            SharedStepsResources resources = new SharedStepsResources();
            resources.topics.register(TOPIC_A.getTopicName(), TOPIC_A.getTopicAlias(), TOPIC_A);
            resources.topics.register(TOPIC_B.getTopicName(), TOPIC_B.getTopicAlias(), TOPIC_B);
            ValidationStepDefinition validation = new ValidationStepDefinition(resources);

            List<Map<String, String>> rows = List.of(
                    Map.of("topicAlias", "alias-a", "file", "f1.json", "expectedRecordKey", "k1"),
                    Map.of("topicAlias", "alias-b", "file", "f2.json", "expectedRecordKey", "k2"));

            InvocationTargetException wrapper = assertThrows(
                    InvocationTargetException.class, () -> invokeResolveAndAssertSingleTopic(validation, rows));
            assertInstanceOf(TopicMismatchException.class, wrapper.getCause());
            assertTrue(wrapper.getCause().getMessage().contains("single topic"));
        }
    }

    @Nested
    @DisplayName("singleRow (batch consumer steps only)")
    class SingleRow {

        @Test
        @DisplayName("single-row DataTable passes through unchanged")
        void singleRowPasses() throws Exception {
            ValidationStepDefinition validation = new ValidationStepDefinition(new SharedStepsResources());
            DataTable dataTable = buildDataTable(
                    List.of("topicAlias", "file", "expectedRecordKey"), List.of("rt-out", "expected.json", "key-1"));

            Map<String, String> row = invokeSingleRow(validation, dataTable, "expected records from files");
            assertEquals("rt-out", row.get("topicAlias"));
        }

        @Test
        @DisplayName("multi-row DataTable is rejected with a message pointing to the batch step")
        void multiRowRejectedWithBatchHint() {
            ValidationStepDefinition validation = new ValidationStepDefinition(new SharedStepsResources());
            DataTable dataTable = buildDataTable(
                    List.of("topicAlias", "file", "expectedRecordKey"),
                    List.of("rt-out", "expected-1.json", "key-1"),
                    List.of("rt-out", "expected-2.json", "key-2"));

            InvocationTargetException wrapper = assertThrows(
                    InvocationTargetException.class,
                    () -> invokeSingleRow(validation, dataTable, "expected records from files"));
            assertInstanceOf(AssertionError.class, wrapper.getCause());
            assertTrue(wrapper.getCause().getMessage().contains("expected records from files"));
        }

        @Test
        @DisplayName("multi-row DataTable with no batch equivalent is rejected without a redirect hint")
        void multiRowRejectedWithoutHint() {
            ValidationStepDefinition validation = new ValidationStepDefinition(new SharedStepsResources());
            DataTable dataTable = buildDataTable(
                    List.of("topicAlias", "expectedRecordKey"), List.of("rt-out", "key-1"), List.of("rt-out", "key-2"));

            InvocationTargetException wrapper =
                    assertThrows(InvocationTargetException.class, () -> invokeSingleRow(validation, dataTable, null));
            assertInstanceOf(AssertionError.class, wrapper.getCause());
            assertTrue(wrapper.getCause().getMessage().contains("single record per call"));
        }
    }

    // ── Reflection helpers ───────────────────────────────────────────────────────

    private static Topic invokeResolveAndAssertSingleTopic(
            ValidationStepDefinition validation, List<Map<String, String>> rows) throws Exception {
        Method m = ValidationStepDefinition.class.getDeclaredMethod("resolveAndAssertSingleTopic", List.class);
        m.setAccessible(true);
        return (Topic) m.invoke(validation, rows);
    }

    private static Map<String, String> invokeSingleRow(
            ValidationStepDefinition validation, DataTable dataTable, String batchHint) throws Exception {
        Method m = ValidationStepDefinition.class.getDeclaredMethod("singleRow", DataTable.class, String.class);
        m.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) m.invoke(validation, dataTable, batchHint);
        return result;
    }

    /**
     * Builds a {@link DataTable} backed by a real {@link DataTableTypeRegistryTableConverter} —
     * {@code DataTable.create} without a converter cannot perform {@code asMaps()} conversions and throws
     * {@code CucumberDataTableException}.
     */
    @SafeVarargs
    private static DataTable buildDataTable(List<String> header, List<String>... rows) {
        DataTableTypeRegistry registry = new DataTableTypeRegistry(Locale.ENGLISH);
        DataTableTypeRegistryTableConverter converter = new DataTableTypeRegistryTableConverter(registry);
        List<List<String>> all = new java.util.ArrayList<>();
        all.add(header);
        all.addAll(List.of(rows));
        return DataTable.create(all, converter);
    }
}

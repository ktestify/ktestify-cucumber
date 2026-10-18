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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.cucumber.datatable.DataTable;
import io.cucumber.datatable.DataTableTypeRegistry;
import io.cucumber.datatable.DataTableTypeRegistryTableConverter;
import io.cucumber.java.Scenario;
import io.github.ktestify.config.ConfigBuilder;
import io.github.ktestify.config.KtestifyConfig;
import io.github.ktestify.hooks.KtestifyHooks;
import io.github.ktestify.models.Topic;
import io.github.ktestify.services.ConsumerValidationService;
import io.github.ktestify.services.ProducerValidationService;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Step Definitions and Hooks Logging Tests")
class StepsAndHooksLoggingTest {

    @Test
    @DisplayName("KtestifyHooks logs before and after scenario")
    void hooksLogBeforeAndAfterScenario() {
        KtestifyHooks hooks = new KtestifyHooks();
        Scenario scenario = mock(Scenario.class);
        when(scenario.getName()).thenReturn("Test Scenario");
        when(scenario.getSourceTagNames()).thenReturn(Set.of("@smoke"));

        when(scenario.isFailed()).thenReturn(false);
        assertDoesNotThrow(() -> hooks.beforeScenario(scenario));
        assertDoesNotThrow(() -> hooks.afterScenario(scenario));

        when(scenario.isFailed()).thenReturn(true);
        assertDoesNotThrow(() -> hooks.afterScenario(scenario));
    }

    @Test
    @DisplayName("BackgroundStepDefinition registers namespaces, topics, assets directory, and schemas")
    void backgroundStepDefinitionRegistersAllEntities() {
        KtestifyConfig.load(ConfigBuilder.create()
                .assetsDirectory("/tmp/assets")
                .set("ktestify.kafka.topic-namespace", "default-ns")
                .buildConfig());

        SharedStepsResources resources = new SharedStepsResources();
        assertNotNull(resources.assetsDirectory);
        assertTrue(resources.namespaces.contains("default-ns"));

        BackgroundStepDefinition bg = new BackgroundStepDefinition(resources);

        bg.givenNamespace(table(List.of("namespace"), List.of("ns-single")));
        assertTrue(resources.namespaces.contains("ns-single"));

        bg.givenNamespaces(table(
                List.of("namespace", "namespaceAlias"), List.of("ns-one", "alias-1"), List.of("ns-two", "alias-2")));
        assertTrue(resources.namespaces.contains("alias-1"));

        bg.givenInputTopic(table(
                List.of("topicName", "topicAlias", "namespaceAlias"), List.of("orders-in", "in-alias", "alias-1")));
        assertEquals("ns-one.orders-in", resources.topics.getOrThrow("in-alias").getNamespacedTopic());

        bg.givenOutputTopic(table(
                List.of("topicName", "topicAlias", "namespace"),
                List.of("orders-out", "out-alias", "ns-single"),
                List.of("orders-raw", "raw-alias", "inline-ns")));
        assertEquals(
                "ns-single.orders-out", resources.topics.getOrThrow("out-alias").getNamespacedTopic());
        assertEquals(
                "inline-ns.orders-raw", resources.topics.getOrThrow("raw-alias").getNamespacedTopic());

        bg.givenAssetsDirectory(table(List.of("absolutePath"), List.of("/custom/assets")));
        assertEquals("/custom/assets", resources.assetsDirectory.getAbsolutePath());

        bg.givenSchema(table(
                List.of("schemaName", "schemaAlias", "schemaVersion"), List.of("OrderSchema", "order-schema", "2")));
        assertEquals(2, resources.schemas.getOrThrow("order-schema").getSchemaVersion());
    }

    @Test
    @DisplayName("ActionStepDefinition delegates multi-row raw and Avro sends and clears known messages")
    void actionStepDefinitionDelegatesAndLogs() throws Exception {
        SharedStepsResources resources = new SharedStepsResources();
        Topic inputTopic = Topic.builder()
                .topicName("orders-in")
                .topicAlias("in-alias")
                .topicType(Topic.Type.INPUT)
                .build();
        resources.topics.register("orders-in", "in-alias", inputTopic);

        ActionStepDefinition action = new ActionStepDefinition(resources);
        ProducerValidationService mockProducerService = mock(ProducerValidationService.class);
        setField(action, "producerService", mockProducerService);

        DataTable rawTable = table(
                List.of("topicAlias", "file", "recordKey"),
                List.of("in-alias", "f1.json", "k1"),
                List.of("in-alias", "f2.json", "k2"));
        action.whenRecordFromFileIsSent(rawTable);
        verify(mockProducerService, times(2)).sendRawFromFile(any(), eq(inputTopic), any());

        DataTable avroTable = table(
                List.of("topicName", "file", "recordKey"),
                List.of("orders-in", "f1.json", "k1"),
                List.of("orders-in", "f2.json", "k2"));
        action.whenRecordFromFileBasedOnSchemaIsSent(avroTable);
        verify(mockProducerService, times(2)).sendAvroFromFile(any(), eq(inputTopic), any());

        action.andWaitForSeconds(0);
        action.andIClearKnownMessages();
        action.closeProducers();
        verify(mockProducerService).close();
    }

    @Test
    @DisplayName("ValidationStepDefinition delegates all validation steps and logs progress")
    void validationStepDefinitionDelegatesAllSteps() throws Exception {
        SharedStepsResources resources = new SharedStepsResources();
        Topic outputTopic = Topic.builder()
                .topicName("orders-out")
                .topicAlias("out-alias")
                .topicType(Topic.Type.OUTPUT)
                .build();
        resources.topics.register("orders-out", "out-alias", outputTopic);

        ValidationStepDefinition validation = new ValidationStepDefinition(resources);
        ConsumerValidationService mockConsumerService = mock(ConsumerValidationService.class);
        setField(validation, "consumerService", mockConsumerService);

        DataTable multiRow = table(
                List.of("topicAlias", "file", "expectedRecordKey"),
                List.of("out-alias", "f1.json", "k1"),
                List.of("out-alias", "f2.json", "k2"));

        validation.thenExpectedRecordFromFile(multiRow);
        verify(mockConsumerService, times(2)).validateRawFromFile(any(), eq(outputTopic), any(), anyLong());

        validation.thenExpectedRecordShouldHaveFieldsMatchingFromFile(multiRow);
        verify(mockConsumerService, times(2)).validateRawFields(any(), eq(outputTopic), any(), anyLong());

        validation.thenExpectedRecordFromFileBasedOnXml(multiRow);
        verify(mockConsumerService, times(2)).validateRawXml(any(), eq(outputTopic), any(), anyLong());

        validation.thenExpectedRecordBasedOnXmlShouldHaveFieldsMatchingFromFile(multiRow);
        verify(mockConsumerService, times(2)).validateRawXPath(any(), eq(outputTopic), any(), anyLong());

        validation.thenExpectedRecordFromFileBasedOnSchema(multiRow);
        verify(mockConsumerService, times(2)).validateAvroFromFile(any(), eq(outputTopic), any(), anyLong());

        validation.thenExpectedRecordBasedOnSchemaShouldHaveFieldsMatchingFromGivenValue(multiRow);
        verify(mockConsumerService, times(2)).validateAvroFieldValue(any(), eq(outputTopic), anyLong());

        validation.thenExpectedRecordKeyMatches(multiRow);
        verify(mockConsumerService, times(2)).validateRawKeyOnly(any(), eq(outputTopic), anyLong());

        validation.thenExpectedRecordKeyAndValueMatchFromFile(multiRow);
        verify(mockConsumerService, times(2)).validateRawKeyValue(any(), eq(outputTopic), any(), anyLong());

        validation.thenExpectedAvroRecordKeyMatches(multiRow);
        verify(mockConsumerService, times(2)).validateAvroKeyOnly(any(), eq(outputTopic), anyLong());

        validation.andRecordShouldNotAppearInTopic(multiRow);
        verify(mockConsumerService, times(2)).validateNoRecord(any(), eq(outputTopic), anyLong());

        validation.andRecordShouldAppearInTopic(multiRow);
        verify(mockConsumerService, times(2)).validateRecordExists(any(), eq(outputTopic), anyLong());

        DataTable singleBatchRow = table(
                List.of("topicName", "files", "expectedRecordsCount"), List.of("orders-out", "f1.json,f2.json", "2"));

        validation.thenExpectedRecordsFromFiles(singleBatchRow);
        verify(mockConsumerService).validateRawBatch(any(), eq(outputTopic), any());

        validation.thenExpectedRecordsFromFilesBasedOnSchema(singleBatchRow);
        verify(mockConsumerService).validateAvroBatch(any(), eq(outputTopic), any());
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    @SafeVarargs
    private static DataTable table(List<String> header, List<String>... rows) {
        DataTableTypeRegistry registry = new DataTableTypeRegistry(Locale.ENGLISH);
        DataTableTypeRegistryTableConverter converter = new DataTableTypeRegistryTableConverter(registry);
        List<List<String>> all = new ArrayList<>();
        all.add(header);
        all.addAll(List.of(rows));
        return DataTable.create(all, converter);
    }
}

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

import static io.github.ktestify.utils.DataTableUtils.Constants.DATA_TABLE_TOPIC_ALIAS;
import static io.github.ktestify.utils.DataTableUtils.Constants.DATA_TABLE_TOPIC_NAME;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.github.ktestify.models.Topic;
import io.github.ktestify.services.ConsumerValidationService;
import io.github.ktestify.utils.DataTableUtils;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Cucumber {@code @Then} and {@code @And} step definitions for consumer validation.
 *
 * <p>Each step resolves the target topic from {@link SharedStepsResources} by alias or name, then delegates to
 * {@link ConsumerValidationService} which builds the appropriate {@code ConsumerContext} and executes the consumer with
 * the correct matcher.
 */
@Slf4j
public class ValidationStepDefinition {

    private final SharedStepsResources resources;
    private final ConsumerValidationService consumerService;

    public ValidationStepDefinition(SharedStepsResources resources) {
        this.resources = resources;
        this.consumerService = new ConsumerValidationService();
    }

    // =========================================================================
    // Raw — single record, file match
    // =========================================================================

    @Then("expected record from file")
    public void thenExpectedRecordFromFile(DataTable dataTable) {
        Map<String, String> row = DataTableUtils.firstRow(dataTable);
        Topic topic = resolveTopic(row);
        consumerService.validateRawFromFile(row, topic, resources.assetsDirectory);
    }

    // =========================================================================
    // Raw — single record, positional fields
    // =========================================================================

    @Then("expected record should have fields matching from file")
    public void thenExpectedRecordShouldHaveFieldsMatchingFromFile(DataTable dataTable) {
        Map<String, String> row = DataTableUtils.firstRow(dataTable);
        Topic topic = resolveTopic(row);
        consumerService.validateRawFields(row, topic, resources.assetsDirectory);
    }

    // =========================================================================
    // Raw — XML structural match
    // =========================================================================

    @Then("expected record from file based on XML")
    public void thenExpectedRecordFromFileBasedOnXml(DataTable dataTable) {
        Map<String, String> row = DataTableUtils.firstRow(dataTable);
        Topic topic = resolveTopic(row);
        consumerService.validateRawXml(row, topic, resources.assetsDirectory);
    }

    // =========================================================================
    // Raw — XML XPath match
    // =========================================================================

    @Then("expected record based on XML should have fields matching from file")
    public void thenExpectedRecordBasedOnXmlShouldHaveFieldsMatchingFromFile(DataTable dataTable) {
        Map<String, String> row = DataTableUtils.firstRow(dataTable);
        Topic topic = resolveTopic(row);
        consumerService.validateRawXPath(row, topic, resources.assetsDirectory);
    }

    // =========================================================================
    // Raw — batch records
    // =========================================================================

    @Then("expected records from files")
    public void thenExpectedRecordsFromFiles(DataTable dataTable) {
        Map<String, String> row = DataTableUtils.firstRow(dataTable);
        Topic topic = resolveTopic(row);
        consumerService.validateRawBatch(row, topic, resources.assetsDirectory);
    }

    // =========================================================================
    // Avro — single record, file match
    // =========================================================================

    @Then("expected record from file based on schema")
    public void thenExpectedRecordFromFileBasedOnSchema(DataTable dataTable) {
        Map<String, String> row = DataTableUtils.firstRow(dataTable);
        Topic topic = resolveTopic(row);
        consumerService.validateAvroFromFile(row, topic, resources.assetsDirectory);
    }

    // =========================================================================
    // Avro — single field, inline key/value
    // =========================================================================

    @Then("expected record based on schema should have fields matching from given value")
    public void thenExpectedRecordBasedOnSchemaShouldHaveFieldsMatchingFromGivenValue(DataTable dataTable) {
        Map<String, String> row = DataTableUtils.firstRow(dataTable);
        Topic topic = resolveTopic(row);
        consumerService.validateAvroFieldValue(row, topic);
    }

    // =========================================================================
    // Avro — batch records
    // =========================================================================

    @Then("expected records from files based on schema")
    public void thenExpectedRecordsFromFilesBasedOnSchema(DataTable dataTable) {
        Map<String, String> row = DataTableUtils.firstRow(dataTable);
        Topic topic = resolveTopic(row);
        consumerService.validateAvroBatch(row, topic, resources.assetsDirectory);
    }

    // =========================================================================
    // Raw — key-only assertion (KeyRecordMatcher)
    // =========================================================================

    @Then("expected record key matches")
    public void thenExpectedRecordKeyMatches(DataTable dataTable) {
        Map<String, String> row = DataTableUtils.firstRow(dataTable);
        Topic topic = resolveTopic(row);
        consumerService.validateRawKeyOnly(row, topic);
    }

    // =========================================================================
    // Raw — key + value file assertion (FileKeyRecordMatcher)
    // =========================================================================

    @Then("expected record key and value match from file")
    public void thenExpectedRecordKeyAndValueMatchFromFile(DataTable dataTable) {
        Map<String, String> row = DataTableUtils.firstRow(dataTable);
        Topic topic = resolveTopic(row);
        consumerService.validateRawKeyValue(row, topic, resources.assetsDirectory);
    }

    // =========================================================================
    // Avro — key-only assertion (AvroKeyRecordMatcher)
    // =========================================================================

    @Then("expected Avro record key matches")
    public void thenExpectedAvroRecordKeyMatches(DataTable dataTable) {
        Map<String, String> row = DataTableUtils.firstRow(dataTable);
        Topic topic = resolveTopic(row);
        consumerService.validateAvroKeyOnly(row, topic);
    }

    // =========================================================================
    // Watcher — negative assertion (record must NOT appear)
    // =========================================================================

    @And("record should not appear in topic")
    public void andRecordShouldNotAppearInTopic(DataTable dataTable) {
        Map<String, String> row = DataTableUtils.firstRow(dataTable);
        Topic topic = resolveTopicByAliasAndType(row);
        consumerService.validateNoRecord(row, topic);
    }

    // =========================================================================
    // Watcher — positive assertion (record MUST appear)
    // =========================================================================

    @And("record should appear in topic")
    public void andRecordShouldAppearInTopic(DataTable dataTable) {
        Map<String, String> row = DataTableUtils.firstRow(dataTable);
        Topic topic = resolveTopicByAliasAndType(row);
        consumerService.validateRecordExists(row, topic);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Resolves a topic from the DataTable {@code topicAlias} column. Falls back to {@code topicName} if the alias is
     * not found.
     */
    private Topic resolveTopic(Map<String, String> row) {
        String alias = DataTableUtils.getString(row, DATA_TABLE_TOPIC_ALIAS);
        if (alias != null && resources.topics.contains(alias)) {
            return resources.topics.getOrThrow(alias);
        }
        String name = DataTableUtils.getString(row, DATA_TABLE_TOPIC_NAME);
        return resources.topics.getOrThrow(name);
    }

    /**
     * Resolves a topic for watcher steps which identify the topic via {@code topicAlias} and may also carry a
     * {@code topicType} hint.
     */
    private Topic resolveTopicByAliasAndType(Map<String, String> row) {
        return resolveTopic(row);
    }
}

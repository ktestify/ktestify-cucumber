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
import io.github.ktestify.utils.TopicUtils;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Cucumber {@code @Then} and {@code @And} step definitions for consumer validation.
 *
 * <p>Each step resolves the target topic from {@link SharedStepsResources} by alias or name, then delegates to
 * {@link ConsumerValidationService} which builds the appropriate {@code ConsumerContext} and executes the consumer with
 * the correct matcher.
 *
 * <p><b>Multi-row, same-topic only:</b> a DataTable with more than one data row is supported for single-record
 * assertion steps, <em>provided every row resolves to the same physical topic</em> — enforced via
 * {@link TopicUtils#assertSingleTopic(List)}. Rows are validated sequentially, in order, and all rows in the same step
 * share one pinned {@code referenceTimestamp} ("now" captured once at the top of the step) so their delta-time seek
 * windows are computed identically instead of drifting row-to-row. Mixing topics in one DataTable throws
 * {@code TopicMismatchException} before any consumer call is made — split into separate steps instead.
 *
 * <p>Batch consumer steps ({@code expected records from files} / {@code expected records from files based on schema})
 * remain single-row: their one row already describes an any-to-all match across N files in a single consumer call.
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
        List<Map<String, String>> rows = DataTableUtils.toListOfMaps(dataTable);
        Topic topic = resolveAndAssertSingleTopic(rows);
        long referenceTimestamp = now();
        for (Map<String, String> row : rows) {
            consumerService.validateRawFromFile(row, topic, resources.assetsDirectory, referenceTimestamp);
        }
    }

    // =========================================================================
    // Raw — single record, positional fields
    // =========================================================================

    @Then("expected record should have fields matching from file")
    public void thenExpectedRecordShouldHaveFieldsMatchingFromFile(DataTable dataTable) {
        List<Map<String, String>> rows = DataTableUtils.toListOfMaps(dataTable);
        Topic topic = resolveAndAssertSingleTopic(rows);
        long referenceTimestamp = now();
        for (Map<String, String> row : rows) {
            consumerService.validateRawFields(row, topic, resources.assetsDirectory, referenceTimestamp);
        }
    }

    // =========================================================================
    // Raw — XML structural match
    // =========================================================================

    @Then("expected record from file based on XML")
    public void thenExpectedRecordFromFileBasedOnXml(DataTable dataTable) {
        List<Map<String, String>> rows = DataTableUtils.toListOfMaps(dataTable);
        Topic topic = resolveAndAssertSingleTopic(rows);
        long referenceTimestamp = now();
        for (Map<String, String> row : rows) {
            consumerService.validateRawXml(row, topic, resources.assetsDirectory, referenceTimestamp);
        }
    }

    // =========================================================================
    // Raw — XML XPath match
    // =========================================================================

    @Then("expected record based on XML should have fields matching from file")
    public void thenExpectedRecordBasedOnXmlShouldHaveFieldsMatchingFromFile(DataTable dataTable) {
        List<Map<String, String>> rows = DataTableUtils.toListOfMaps(dataTable);
        Topic topic = resolveAndAssertSingleTopic(rows);
        long referenceTimestamp = now();
        for (Map<String, String> row : rows) {
            consumerService.validateRawXPath(row, topic, resources.assetsDirectory, referenceTimestamp);
        }
    }

    // =========================================================================
    // Raw — batch records
    // =========================================================================

    @Then("expected records from files")
    public void thenExpectedRecordsFromFiles(DataTable dataTable) {
        Map<String, String> row = singleRow(dataTable, null);
        Topic topic = resolveTopic(row);
        consumerService.validateRawBatch(row, topic, resources.assetsDirectory);
    }

    // =========================================================================
    // Avro — single record, file match
    // =========================================================================

    @Then("expected record from file based on schema")
    public void thenExpectedRecordFromFileBasedOnSchema(DataTable dataTable) {
        List<Map<String, String>> rows = DataTableUtils.toListOfMaps(dataTable);
        Topic topic = resolveAndAssertSingleTopic(rows);
        long referenceTimestamp = now();
        for (Map<String, String> row : rows) {
            consumerService.validateAvroFromFile(row, topic, resources.assetsDirectory, referenceTimestamp);
        }
    }

    // =========================================================================
    // Avro — single field, inline key/value
    // =========================================================================

    @Then("expected record based on schema should have fields matching from given value")
    public void thenExpectedRecordBasedOnSchemaShouldHaveFieldsMatchingFromGivenValue(DataTable dataTable) {
        List<Map<String, String>> rows = DataTableUtils.toListOfMaps(dataTable);
        Topic topic = resolveAndAssertSingleTopic(rows);
        long referenceTimestamp = now();
        for (Map<String, String> row : rows) {
            consumerService.validateAvroFieldValue(row, topic, referenceTimestamp);
        }
    }

    // =========================================================================
    // Avro — batch records
    // =========================================================================

    @Then("expected records from files based on schema")
    public void thenExpectedRecordsFromFilesBasedOnSchema(DataTable dataTable) {
        Map<String, String> row = singleRow(dataTable, null);
        Topic topic = resolveTopic(row);
        consumerService.validateAvroBatch(row, topic, resources.assetsDirectory);
    }

    // =========================================================================
    // Raw — key-only assertion (KeyRecordMatcher)
    // =========================================================================

    @Then("expected record key matches")
    public void thenExpectedRecordKeyMatches(DataTable dataTable) {
        List<Map<String, String>> rows = DataTableUtils.toListOfMaps(dataTable);
        Topic topic = resolveAndAssertSingleTopic(rows);
        long referenceTimestamp = now();
        for (Map<String, String> row : rows) {
            consumerService.validateRawKeyOnly(row, topic, referenceTimestamp);
        }
    }

    // =========================================================================
    // Raw — key + value file assertion (FileKeyRecordMatcher)
    // =========================================================================

    @Then("expected record key and value match from file")
    public void thenExpectedRecordKeyAndValueMatchFromFile(DataTable dataTable) {
        List<Map<String, String>> rows = DataTableUtils.toListOfMaps(dataTable);
        Topic topic = resolveAndAssertSingleTopic(rows);
        long referenceTimestamp = now();
        for (Map<String, String> row : rows) {
            consumerService.validateRawKeyValue(row, topic, resources.assetsDirectory, referenceTimestamp);
        }
    }

    // =========================================================================
    // Avro — key-only assertion (AvroKeyRecordMatcher)
    // =========================================================================

    @Then("expected Avro record key matches")
    public void thenExpectedAvroRecordKeyMatches(DataTable dataTable) {
        List<Map<String, String>> rows = DataTableUtils.toListOfMaps(dataTable);
        Topic topic = resolveAndAssertSingleTopic(rows);
        long referenceTimestamp = now();
        for (Map<String, String> row : rows) {
            consumerService.validateAvroKeyOnly(row, topic, referenceTimestamp);
        }
    }

    // =========================================================================
    // Watcher — negative assertion (record must NOT appear)
    // =========================================================================

    @And("record should not appear in topic")
    public void andRecordShouldNotAppearInTopic(DataTable dataTable) {
        List<Map<String, String>> rows = DataTableUtils.toListOfMaps(dataTable);
        Topic topic = resolveAndAssertSingleTopic(rows);
        long referenceTimestamp = now();
        for (Map<String, String> row : rows) {
            consumerService.validateNoRecord(row, topic, referenceTimestamp);
        }
    }

    // =========================================================================
    // Watcher — positive assertion (record MUST appear)
    // =========================================================================

    @And("record should appear in topic")
    public void andRecordShouldAppearInTopic(DataTable dataTable) {
        List<Map<String, String>> rows = DataTableUtils.toListOfMaps(dataTable);
        Topic topic = resolveAndAssertSingleTopic(rows);
        long referenceTimestamp = now();
        for (Map<String, String> row : rows) {
            consumerService.validateRecordExists(row, topic, referenceTimestamp);
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Resolves the topic for every row of a multi-row assertion DataTable and asserts they all target the same physical
     * topic. A DataTable driving these steps is only allowed to reference one topic — see
     * {@link TopicUtils#assertSingleTopic(List)}. Throws before any consumer call is made.
     */
    private Topic resolveAndAssertSingleTopic(List<Map<String, String>> rows) {
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("DataTable has no data rows.");
        }
        List<Topic> topics = rows.stream().map(this::resolveTopic).toList();
        return TopicUtils.assertSingleTopic(topics);
    }

    /** Captures "now" once per step so every row of a multi-row DataTable shares an identical seek window. */
    private static long now() {
        return System.currentTimeMillis();
    }

    /**
     * Ensures the given DataTable carries exactly one data row and returns it. Used only by the batch consumer steps,
     * whose single row already describes an any-to-all match across N files.
     *
     * @param dataTable the step's DataTable
     * @param batchStepHint the Gherkin step name of the multi-record equivalent, or {@code null} if this step has no
     *     batch equivalent
     * @return the single data row
     * @throws AssertionError if the table has more than one data row
     */
    private Map<String, String> singleRow(DataTable dataTable, String batchStepHint) {
        List<Map<String, String>> rows = DataTableUtils.toListOfMaps(dataTable);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("DataTable has no data rows.");
        }
        if (rows.size() > 1) {
            String suffix = batchStepHint != null
                    ? " Use the \"" + batchStepHint + "\" step instead for multi-record validation."
                    : " This assertion step only supports a single record per call.";
            throw new AssertionError("This step only supports a single DataTable row but " + rows.size()
                    + " rows were provided." + suffix);
        }
        return rows.getFirst();
    }

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
}

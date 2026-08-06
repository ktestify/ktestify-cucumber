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

import static io.github.ktestify.utils.DataTableUtils.Constants.*;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;
import io.github.ktestify.io.kafka.KafkaRecordFetcher;
import io.github.ktestify.models.Topic;
import io.github.ktestify.script.ScriptService;
import io.github.ktestify.services.ProducerValidationService;
import io.github.ktestify.utils.DataTableUtils;
import io.github.ktestify.utils.TopicUtils;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Cucumber {@code @When} (and {@code @And}) step definitions for actions: producing records, sending files, executing
 * scripts, and waiting.
 */
@Slf4j
public class ActionStepDefinition {

    private final SharedStepsResources resources;
    private final ProducerValidationService producerService;
    private final ScriptService scriptService;

    public ActionStepDefinition(SharedStepsResources resources) {
        this.resources = resources;
        this.producerService = new ProducerValidationService();
        this.scriptService = new ScriptService();
    }

    // =========================================================================
    // Kafka raw producer
    // =========================================================================

    @When("record from file is sent")
    public void whenRecordFromFileIsSent(DataTable dataTable) {
        List<Map<String, String>> rows = DataTableUtils.toListOfMaps(dataTable);
        Topic topic = resolveAndAssertSingleTopic(rows);
        for (Map<String, String> row : rows) {
            producerService.sendRawFromFile(row, topic, resources.assetsDirectory);
        }
    }

    // =========================================================================
    // Kafka Avro producer
    // =========================================================================

    @When("record from file based on schema is sent")
    public void whenRecordFromFileBasedOnSchemaIsSent(DataTable dataTable) {
        List<Map<String, String>> rows = DataTableUtils.toListOfMaps(dataTable);
        Topic topic = resolveAndAssertSingleTopic(rows);
        for (Map<String, String> row : rows) {
            producerService.sendAvroFromFile(row, topic, resources.assetsDirectory);
        }
    }

    // =========================================================================
    // Wait
    // =========================================================================

    @And("wait for {int} seconds")
    public void andWaitForSeconds(int seconds) throws InterruptedException {
        log.info("Waiting {} second(s)…", seconds);
        Thread.sleep(seconds * 1000L);
    }

    // =========================================================================
    // Matched-record registry reset
    // =========================================================================

    /**
     * Clears the static deduplication registry in {@link KafkaRecordFetcher} so that records already matched in
     * previous steps or scenarios cannot be re-matched.
     *
     * <p>Use this step whenever you need an explicit reset — for example between logically independent scenarios in the
     * same feature file that share a Kafka topic.
     */
    @And("clear known messages")
    public void andIClearKnownMessages() {
        log.info("Clearing matched-record registry (KafkaRecordFetcher)…");
        KafkaRecordFetcher.clearMatchedRecords();
    }

    // =========================================================================
    // Script execution
    // =========================================================================

    @When("script is executed")
    public void whenScriptIsExecuted(DataTable dataTable) throws IOException, InterruptedException {
        executeScript(dataTable);
    }

    @And("execute script")
    public void andExecuteScript(DataTable dataTable) throws IOException, InterruptedException {
        executeScript(dataTable);
    }

    private void executeScript(DataTable dataTable) throws IOException, InterruptedException {
        List<Map<String, String>> rows = DataTableUtils.toListOfMaps(dataTable);
        for (Map<String, String> row : rows) {
            String scriptPath = DataTableUtils.getString(row, DATA_TABLE_SCRIPT_PATH);
            String scriptArgs = DataTableUtils.getString(row, DATA_TABLE_SCRIPT_ARGS);

            int exitCode = scriptService.execute(scriptPath, scriptArgs);
            if (exitCode != 0) {
                throw new AssertionError("Script '" + scriptPath + "' exited with non-zero exit code: " + exitCode);
            }
        }
    }

    /**
     * Resolves a topic from the DataTable by alias first, then by name. Supports both {@code topicAlias} and
     * {@code topicName} columns.
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
     * Resolves the topic for every row of a multi-row producer DataTable and asserts they all target the same physical
     * topic — a DataTable driving a single producer step is only allowed to reference one topic. See
     * {@link TopicUtils#assertSingleTopic(List)}.
     */
    private Topic resolveAndAssertSingleTopic(List<Map<String, String>> rows) {
        List<Topic> topics = rows.stream().map(this::resolveTopic).toList();
        return TopicUtils.assertSingleTopic(topics);
    }
}

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
package io.github.ktestify.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.github.ktestify.constants.ConfigConstants;
import io.github.ktestify.entities.KtestifyAssetsDirectory;
import io.github.ktestify.entities.KtestifyCftHost;
import io.github.ktestify.entities.KtestifyNamespace;
import io.github.ktestify.entities.KtestifyQueue;
import io.github.ktestify.entities.KtestifySchema;
import io.github.ktestify.models.Topic;
import io.github.ktestify.utils.DataTableUtils;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Cucumber {@code @Given} step definitions for scenario background setup.
 *
 * <p>Registers topics, namespaces, schemas, queues, CFT hosts, and the assets directory into the
 * {@link SharedStepsResources} so that later {@code @When} and {@code @Then} steps can look them up by name or alias.
 */
@Slf4j
public class BackgroundStepDefinition {

    private final SharedStepsResources resources;

    public BackgroundStepDefinition(SharedStepsResources resources) {
        this.resources = resources;
    }

    // =========================================================================
    // Namespaces
    // =========================================================================

    @Given("namespace")
    public void givenNamespace(DataTable dataTable) {
        Map<String, String> row = DataTableUtils.firstRow(dataTable);
        String ns = DataTableUtils.getString(row, ConfigConstants.DATA_TABLE_NAMESPACE);
        KtestifyNamespace namespace = KtestifyNamespace.builder().namespace(ns).build();
        resources.namespaces.register(ns, namespace);
        log.debug("Registered namespace '{}'", ns);
    }

    @Given("namespaces")
    public void givenNamespaces(DataTable dataTable) {
        List<Map<String, String>> rows = DataTableUtils.toListOfMaps(dataTable);
        for (Map<String, String> row : rows) {
            String ns = DataTableUtils.getString(row, ConfigConstants.DATA_TABLE_NAMESPACE);
            String alias = DataTableUtils.getString(row, ConfigConstants.DATA_TABLE_NAMESPACE_ALIAS);
            KtestifyNamespace namespace = KtestifyNamespace.builder()
                    .namespace(ns)
                    .namespaceAlias(alias)
                    .build();
            resources.namespaces.register(ns, alias, namespace);
            log.debug("Registered namespace '{}' (alias: '{}')", ns, alias);
        }
    }

    // =========================================================================
    // Topics
    // =========================================================================

    @Given("input topic")
    public void givenInputTopic(DataTable dataTable) {
        registerTopic(dataTable, Topic.Type.INPUT);
    }

    @Given("output topic")
    public void givenOutputTopic(DataTable dataTable) {
        registerTopic(dataTable, Topic.Type.OUTPUT);
    }

    private void registerTopic(DataTable dataTable, Topic.Type type) {
        List<Map<String, String>> rows = DataTableUtils.toListOfMaps(dataTable);
        for (Map<String, String> row : rows) {
            String topicName = DataTableUtils.getString(row, ConfigConstants.DATA_TABLE_TOPIC_NAME);
            String topicAlias = DataTableUtils.getString(row, ConfigConstants.DATA_TABLE_TOPIC_ALIAS);
            String nsAlias = DataTableUtils.getString(row, ConfigConstants.DATA_TABLE_NAMESPACE_ALIAS);
            String nsValue = DataTableUtils.getString(row, ConfigConstants.DATA_TABLE_NAMESPACE);

            // Resolve namespace: try alias first, then direct value
            Topic.TopicNamespace namespace = resolveNamespace(nsAlias, nsValue);

            Topic topic = Topic.builder()
                    .topicName(topicName)
                    .topicAlias(topicAlias)
                    .topicNamespace(namespace)
                    .topicType(type)
                    .build();

            resources.topics.register(topicName, topicAlias, topic);
            log.debug("Registered {} topic '{}' (alias: '{}')", type, topicName, topicAlias);
        }
    }

    // =========================================================================
    // Assets directory
    // =========================================================================

    @Given("assets directory")
    public void givenAssetsDirectory(DataTable dataTable) {
        String path = DataTableUtils.getString(dataTable, "absolutePath");
        resources.assetsDirectory =
                KtestifyAssetsDirectory.builder().absolutePath(path).build();
        log.debug("Assets directory set to '{}'", path);
    }

    // =========================================================================
    // Schemas
    // =========================================================================

    @Given("schema")
    public void givenSchema(DataTable dataTable) {
        List<Map<String, String>> rows = DataTableUtils.toListOfMaps(dataTable);
        for (Map<String, String> row : rows) {
            String name = DataTableUtils.getString(row, ConfigConstants.DATA_TABLE_SCHEMA_NAME);
            String alias = DataTableUtils.getString(row, ConfigConstants.DATA_TABLE_SCHEMA_ALIAS);
            Integer version = DataTableUtils.getInt(row, ConfigConstants.DATA_TABLE_SCHEMA_VERSION);
            KtestifySchema schema = KtestifySchema.builder()
                    .schemaName(name)
                    .schemaAlias(alias)
                    .schemaVersion(version)
                    .build();
            resources.schemas.register(name, alias, schema);
            log.debug("Registered schema '{}' (alias: '{}')", name, alias);
        }
    }

    // =========================================================================
    // Queues (IBM MQ)
    // =========================================================================

    @Given("input queue")
    public void givenInputQueue(DataTable dataTable) {
        registerQueue(dataTable, KtestifyQueue.Type.INPUT);
    }

    @Given("output queue")
    public void givenOutputQueue(DataTable dataTable) {
        registerQueue(dataTable, KtestifyQueue.Type.OUTPUT);
    }

    private void registerQueue(DataTable dataTable, KtestifyQueue.Type type) {
        List<Map<String, String>> rows = DataTableUtils.toListOfMaps(dataTable);
        for (Map<String, String> row : rows) {
            String name = DataTableUtils.getString(row, ConfigConstants.DATA_TABLE_QUEUE_NAME);
            String alias = DataTableUtils.getString(row, ConfigConstants.DATA_TABLE_QUEUE_ALIAS);
            String manager = DataTableUtils.getString(row, ConfigConstants.DATA_TABLE_QUEUE_MANAGER);
            String channel = DataTableUtils.getString(row, ConfigConstants.DATA_TABLE_QUEUE_CHANNEL);
            KtestifyQueue queue = KtestifyQueue.builder()
                    .queueName(name)
                    .queueAlias(alias)
                    .queueManager(manager)
                    .channel(channel)
                    .type(type)
                    .build();
            resources.queues.register(name, alias, queue);
            log.debug("Registered {} queue '{}' (alias: '{}')", type, name, alias);
        }
    }

    // =========================================================================
    // CFT hosts
    // =========================================================================

    @Given("CFT hosts")
    public void givenCftHosts(DataTable dataTable) {
        List<Map<String, String>> rows = DataTableUtils.toListOfMaps(dataTable);
        for (Map<String, String> row : rows) {
            String host = DataTableUtils.getString(row, ConfigConstants.DATA_TABLE_CFT_HOST);
            Integer port = DataTableUtils.getInt(row, ConfigConstants.DATA_TABLE_CFT_PORT);
            String username = DataTableUtils.getString(row, ConfigConstants.DATA_TABLE_CFT_USERNAME);
            String password = DataTableUtils.getString(row, ConfigConstants.DATA_TABLE_CFT_PASSWORD);
            String basePath = DataTableUtils.getString(row, ConfigConstants.DATA_TABLE_CFT_BASE_PATH);
            String alias = DataTableUtils.getString(row, ConfigConstants.DATA_TABLE_CFT_ALIAS);
            KtestifyCftHost cftHost = KtestifyCftHost.builder()
                    .host(host)
                    .port(port != null ? port : 22)
                    .username(username)
                    .password(password)
                    .basePath(basePath)
                    .cftAlias(alias)
                    .build();
            resources.cftHosts.register(alias != null ? alias : host, cftHost);
            log.debug("Registered CFT host '{}' (alias: '{}')", host, alias);
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private Topic.TopicNamespace resolveNamespace(String nsAlias, String nsValue) {
        // Try to look up by alias first
        if (nsAlias != null && resources.namespaces.contains(nsAlias)) {
            KtestifyNamespace found = resources.namespaces.getOrThrow(nsAlias);
            return Topic.TopicNamespace.builder()
                    .namespace(found.getNamespace())
                    .namespaceAlias(found.getNamespaceAlias())
                    .build();
        }
        // Try to look up by namespace value
        if (nsValue != null && resources.namespaces.contains(nsValue)) {
            KtestifyNamespace found = resources.namespaces.getOrThrow(nsValue);
            return Topic.TopicNamespace.builder()
                    .namespace(found.getNamespace())
                    .namespaceAlias(found.getNamespaceAlias())
                    .build();
        }
        // Inline value provided directly in the DataTable (no alias)
        if (nsValue != null) {
            return Topic.TopicNamespace.builder().namespace(nsValue).build();
        }
        return null;
    }
}

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

import io.github.ktestify.config.KtestifyConfig;
import io.github.ktestify.entities.KtestifyAssetsDirectory;
import io.github.ktestify.entities.KtestifyCftHost;
import io.github.ktestify.entities.KtestifyNamespace;
import io.github.ktestify.entities.KtestifyQueue;
import io.github.ktestify.entities.KtestifySchema;
import io.github.ktestify.manager.ObjectManager;
import io.github.ktestify.models.Topic;

/**
 * PicoContainer-managed shared state injected into all step definition classes.
 *
 * <p>A single instance is created per Cucumber scenario by PicoContainer and passed to every step class that declares
 * it as a constructor parameter. This avoids static fields while keeping all object registries in one place.
 *
 * <p>{@link #assetsDirectory} is pre-initialised from {@code KtestifyConfig} (the
 * {@code ktestify.framework.directories.assets} key). The {@code Given assets directory} step overrides it for a
 * specific scenario. This means you can set the assets path once in {@code local.conf} / an env var and omit the step
 * entirely from feature files.
 */
public class SharedStepsResources {

    /** Registry for Kafka topics (both INPUT and OUTPUT). Keyed by topic name and/or alias. */
    public final ObjectManager<Topic> topics = new ObjectManager<>();

    /** Registry for topic namespaces. Keyed by namespace value and/or alias. */
    public final ObjectManager<KtestifyNamespace> namespaces = new ObjectManager<>();

    /** Registry for Avro schemas. Keyed by schema name and/or alias. */
    public final ObjectManager<KtestifySchema> schemas = new ObjectManager<>();

    /** Registry for IBM MQ queues. Keyed by queue name and/or alias. */
    public final ObjectManager<KtestifyQueue> queues = new ObjectManager<>();

    /** Registry for CFT hosts. Keyed by CFT alias. */
    public final ObjectManager<KtestifyCftHost> cftHosts = new ObjectManager<>();

    /**
     * The assets base directory for the current scenario. Initialised from
     * {@code ktestify.framework.directories.assets} in config; overridden per-scenario by the {@code Given assets
     * directory} step.
     */
    public KtestifyAssetsDirectory assetsDirectory;

    public SharedStepsResources() {
        KtestifyConfig cfg = KtestifyConfig.getOrLoad();

        // Pre-populate assetsDirectory from config so feature files don't need to
        // repeat the path — a single setting in local.conf / env var is enough.
        cfg.getFramework()
                .getAssetsDirectory()
                .filter(path -> !path.isBlank())
                .ifPresent(path -> assetsDirectory =
                        KtestifyAssetsDirectory.builder().absolutePath(path).build());

        // Pre-register a default namespace from config (ktestify.kafka.topic-namespace).
        // The 'Given namespace' step overrides or adds to this per-scenario.
        cfg.getKafka()
                .getTopicNamespace()
                .filter(ns -> !ns.isBlank())
                .ifPresent(ns -> namespaces.register(
                        ns, KtestifyNamespace.builder().namespace(ns).build()));
    }
}

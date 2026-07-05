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
package io.github.ktestify;

import io.cucumber.core.cli.Main;
import io.github.ktestify.banner.KtestifyBanner;
import io.github.ktestify.config.KtestifyConfig;
import io.github.ktestify.plugin.PluginContext;
import io.github.ktestify.plugin.PluginRegistry;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Docker / CLI entry point for ktestify-cucumber.
 *
 * <p>Invokes the Cucumber CLI directly so the process exit code maps cleanly to CI success/failure:
 *
 * <ul>
 *   <li>{@code 0} — all scenarios passed
 *   <li>{@code 1} — one or more scenarios failed
 *   <li>{@code 2} — Cucumber configuration error
 * </ul>
 *
 * <h2>Configuration</h2>
 *
 * <p>Config is loaded via Typesafe Config (HOCON) in the following priority order:
 *
 * <ol>
 *   <li>External config file — {@code KTESTIFY_CONFIG_FILE=/path/to/my.conf} env var <b>or</b>
 *       {@code -Dconfig.file=/path/to/my.conf} JVM property (highest priority — overrides all)
 *   <li>JVM system properties: {@code -Dktestify.kafka.bootstrap-servers=...}
 *   <li>Environment variables: {@code KTESTIFY_KAFKA_BOOTSTRAP_SERVERS=...}
 *   <li>{@code application.conf} bundled in this JAR (ktestify-cucumber defaults)
 *   <li>{@code reference.conf} bundled in this JAR (ktestify-core base defaults)
 * </ol>
 *
 * <p>Config is loaded eagerly at startup so misconfigurations are reported immediately before any Cucumber scenario
 * runs.
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * # Run with defaults (features path from env KTESTIFY_FEATURES_PATH)
 * java -jar ktestify-cucumber.jar
 *
 * # Pass a complete config file via env var (Docker-friendly)
 * KTESTIFY_CONFIG_FILE=/config/my-env.conf java -jar ktestify-cucumber.jar
 *
 * # Pass a complete config file via JVM property
 * java -Dconfig.file=/config/my-env.conf -jar ktestify-cucumber.jar
 *
 * # Forward any Cucumber CLI options directly
 * java -jar ktestify-cucumber.jar --tags @smoke src/test/resources/features/smoke
 *
 * # Override individual values via environment variables
 * KTESTIFY_FEATURES_PATH=/tests/features KTESTIFY_KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
 *   java -jar ktestify-cucumber.jar
 * </pre>
 *
 * <h2>Default behaviour (no args)</h2>
 *
 * <ul>
 *   <li>Features path: env {@code KTESTIFY_FEATURES_PATH} → {@code src/test/resources/features}
 *   <li>Glue: {@code io.github.ktestify} (always injected)
 *   <li>Plugins: {@code pretty}, JSON report, {@code CucumberReportr}
 * </ul>
 */
public class KtestifyMain {

    private static final Logger LOG = LoggerFactory.getLogger(KtestifyMain.class);

    private static final String DEFAULT_GLUE = "io.github.ktestify";
    private static final String DEFAULT_FEATURES_ENV = "KTESTIFY_FEATURES_PATH";
    private static final String DEFAULT_FEATURES_PATH = "src/test/resources/features";
    //  private static final String REPORTS_DIR = "target/cucumber-reports";

    /** Env var that points to a complete HOCON config file to load. */
    private static final String CONFIG_FILE_ENV = "KTESTIFY_CONFIG_FILE";
    /** Typesafe Config's own system property for an external config file. */
    private static final String TYPESAFE_CONFIG_FILE_PROP = "config.file";

    static void main(String[] args) {
        KtestifyBanner.print();
        // ── 0. Wire external config file BEFORE the singleton loads ──────────
        applyExternalConfigFile(args);

        // ── 1. Eagerly load and log effective config ──────────────────────────
        logEffectiveConfig();

        // ── 2. Initialize plugin system ───────────────────────────────────────
        PluginContext pluginCtx = KtestifyConfig::getOrLoad;
        PluginRegistry pluginRegistry = initPlugins(pluginCtx);

        // ── 3. Strip --config from args so Cucumber CLI doesn't see it ────────
        List<String> pluginGlue = pluginRegistry.getGluePackages();
        String[] cucumberArgs = buildArgs(stripConfigArg(args), pluginGlue);
        LOG.info("Starting ktestify-cucumber — effective args: {}", (Object) cucumberArgs);

        byte exitCode = Main.run(cucumberArgs, Thread.currentThread().getContextClassLoader());
        LOG.info("Cucumber finished with exit code {}", exitCode);

        // ── 4. Shutdown plugins before exit ───────────────────────────────────
        pluginRegistry.shutdown();

        System.exit(exitCode);
    }

    // -------------------------------------------------------------------------
    // Package-private — tested directly
    // -------------------------------------------------------------------------

    /**
     * Builds the final argument array for the Cucumber CLI. If the caller supplies arguments they are used as-is (with
     * glue injected if absent). Otherwise, a fully defaulted set is built from env vars.
     *
     * @param userArgs raw CLI args (with {@code --config} already stripped)
     * @param pluginGlue glue packages contributed by loaded plugins
     */
    static String[] buildArgs(String[] userArgs, List<String> pluginGlue) {
        if (userArgs != null && userArgs.length > 0) {
            List<String> args = new ArrayList<>(List.of(ensureGlue(userArgs, pluginGlue)));
            addReportingPlugins(args);
            return args.toArray(String[]::new);
        }
        return buildDefaultArgs(pluginGlue);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves the external config file path from (in priority order):
     *
     * <ol>
     *   <li>{@code --config /path/to/file.conf} CLI argument
     *   <li>{@code KTESTIFY_CONFIG_FILE} environment variable
     *   <li>{@code -Dconfig.file=...} JVM system property (already handled natively by Typesafe)
     * </ol>
     *
     * <p>Sets {@code config.file} as a JVM system property so Typesafe Config picks it up on its first load. Must be
     * called before {@link io.github.ktestify.config.KtestifyConfig#getOrLoad()}.
     */
    private static void applyExternalConfigFile(String[] args) {
        // Priority 1: --config <path> CLI arg
        String configPath = extractConfigArg(args);

        // Priority 2: env var (only if not already set via CLI)
        if (configPath == null) {
            configPath = System.getenv(CONFIG_FILE_ENV);
        }

        // Priority 3: -Dconfig.file is already a JVM property — Typesafe handles it natively
        if (configPath == null) {
            configPath = System.getProperty(TYPESAFE_CONFIG_FILE_PROP);
        }

        if (configPath != null && !configPath.isBlank()) {
            java.io.File file = new java.io.File(configPath);
            if (!file.exists()) {
                LOG.error("Config file not found: '{}'. Aborting.", configPath);
                System.exit(2);
            }
            // Tell Typesafe Config to use this file as the primary config source
            System.setProperty(TYPESAFE_CONFIG_FILE_PROP, file.getAbsolutePath());
            LOG.info("Using external config file: '{}'", file.getAbsolutePath());
        }
    }

    /** Extracts the value of {@code --config <path>} from the arg array. Returns {@code null} if not present. */
    private static String extractConfigArg(String[] args) {
        if (args == null) return null;
        for (int i = 0; i < args.length - 1; i++) {
            if ("--config".equals(args[i])) {
                return args[i + 1];
            }
        }
        return null;
    }

    /**
     * Returns a copy of {@code args} with {@code --config <path>} removed so the Cucumber CLI never sees it (it is not
     * a valid Cucumber option).
     */
    private static String[] stripConfigArg(String[] args) {
        if (args == null || extractConfigArg(args) == null) return args;
        List<String> filtered = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            if ("--config".equals(args[i])) {
                i++; // skip the value too
            } else {
                filtered.add(args[i]);
            }
        }
        return filtered.toArray(String[]::new);
    }

    /**
     * Loads ktestify config eagerly and logs effective values so operators can verify the running configuration from
     * container logs without reading config files.
     */
    private static void logEffectiveConfig() {
        try {
            KtestifyConfig cfg = KtestifyConfig.getOrLoad();
            LOG.info("╔══ KTestify effective configuration ═══════════════════════════");
            LOG.info("║  kafka.bootstrap-servers : {}", cfg.getKafka().getBootstrapServers());
            LOG.info("║  schema-registry.url     : {}", cfg.getSchemaRegistry().getUrl());
            LOG.info("║  read-timeout            : {}ms", cfg.getFramework().getDefaultReadTimeoutMillis());
            LOG.info("║  consumer-delta-time     : {}s", cfg.getFramework().getConsumerDeltaTimeSeconds());
            LOG.info(
                    "║  assets-directory        : '{}'",
                    cfg.getFramework().getAssetsDirectory().orElse("<not set>"));
            LOG.info("╚═══════════════════════════════════════════════════════════════");
        } catch (Exception e) {
            LOG.error("Failed to load ktestify configuration: {}", e.getMessage());
            LOG.error("Check your environment variables or -D system properties.");
            System.exit(2);
        }
    }

    private static String[] buildDefaultArgs(List<String> pluginGlue) {
        String featuresPath = System.getenv(DEFAULT_FEATURES_ENV);
        if (featuresPath == null || featuresPath.isBlank()) {
            featuresPath = DEFAULT_FEATURES_PATH;
        }

        List<String> args = new ArrayList<>();
        args.add("--glue");
        args.add(DEFAULT_GLUE);

        // Inject each plugin's glue package as a separate --glue argument
        for (String glue : pluginGlue) {
            args.add("--glue");
            args.add(glue);
            LOG.debug("Injecting plugin glue package: {}", glue);
        }

        addReportingPlugins(args);

        args.add(featuresPath);
        return args.toArray(String[]::new);
    }

    /**
     * Appends {@code --plugin} arguments based on the effective reporting configuration.
     *
     * <ul>
     *   <li>{@code html} — adds the cucumber-reportr HTML plugin ({@code io.github.nil_malh.cucumber.reportr.Core})
     *   <li>{@code json} — adds the built-in JSON file reporter ({@code json:target/cucumber-reports/cucumber.json})
     *   <li>{@code pretty} — adds the built-in pretty console reporter
     * </ul>
     *
     * <p>If reporting is disabled ({@code ktestify.framework.reporting.enabled = false}) no plugin is added. Falls back
     * to {@code pretty} for unknown format values.
     */
    private static void addReportingPlugins(List<String> args) {
        KtestifyConfig cfg;
        try {
            cfg = KtestifyConfig.getOrLoad();
        } catch (Exception e) {
            LOG.warn(
                    "Could not load config for reporting plugins — falling back to 'pretty'. Reason: {}",
                    e.getMessage());
            args.add("--plugin");
            args.add("pretty");
            return;
        }

        if (!cfg.getFramework().isEnableReporting()) {
            LOG.info("Reporting disabled — no --plugin added.");
            return;
        }

        String format = cfg.getFramework().getReportFormat();
        LOG.info("Reporting format: '{}'", format);

        String outputDir = cfg.getFramework().getReportOutputDirectory();

        switch (format == null ? "" : format.toLowerCase()) {
            case "html" -> {
                args.add("--plugin");
                args.add("io.github.nil_malh.cucumber.reportr.Core:" + outputDir);
            }
            case "json" -> {
                args.add("--plugin");
                args.add("json:" + outputDir + "/cucumber.json");
            }
            case "pretty" -> {
                args.add("--plugin");
                args.add("pretty");
            }
            default -> {
                LOG.warn("Unknown report format '{}' — falling back to 'pretty'.", format);
                args.add("--plugin");
                args.add("pretty");
            }
        }
    }

    /** Prepends {@code --glue io.github.ktestify} and any plugin glue packages if not already present. */
    private static String[] ensureGlue(String[] userArgs, List<String> pluginGlue) {
        List<String> args = new ArrayList<>(List.of(userArgs));

        // Check and prepend plugin glue (in reverse so insertion order is preserved at front)
        List<String> toInject = new ArrayList<>();
        for (String glue : pluginGlue) {
            if (args.stream().noneMatch(a -> a.equals(glue))) {
                toInject.add(glue);
            }
        }
        // Insert at front: --glue <pkg> for each missing plugin glue
        for (int i = toInject.size() - 1; i >= 0; i--) {
            args.addFirst(toInject.get(i));
            args.addFirst("--glue");
            LOG.debug("Injecting plugin glue package: {}", toInject.get(i));
        }

        // Ensure the core glue is present
        if (args.stream().noneMatch(a -> a.equals(DEFAULT_GLUE))) {
            args.addFirst(DEFAULT_GLUE);
            args.addFirst("--glue");
        }
        return args.toArray(String[]::new);
    }

    /**
     * Initializes the plugin system, logging each discovery phase.
     *
     * @param ctx the plugin context wrapping the loaded config
     * @return the fully initialized {@link PluginRegistry}
     */
    private static PluginRegistry initPlugins(PluginContext ctx) {
        try {
            return PluginRegistry.load(ctx);
        } catch (Exception e) {
            LOG.error("Fatal: plugin system failed to initialize — {}", e.getMessage(), e);
            System.exit(2);
            throw new IllegalStateException("unreachable");
        }
    }
}

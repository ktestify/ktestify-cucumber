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

import com.typesafe.config.ConfigFactory;
import io.cucumber.core.cli.Main;
import io.github.ktestify.config.KtestifyConfig;
import io.github.ktestify.exceptions.PluginException;
import io.github.ktestify.plugin.PluginContext;
import io.github.ktestify.plugin.PluginRegistry;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KtestifyMainTest {
    private PluginRegistry registry;
    private MockedStatic<PluginRegistry> plugins;
    private MockedStatic<Main> cucumber;
    private String originalConfigFile;

    @BeforeEach
    void setUp() {
        originalConfigFile = System.getProperty("config.file");
        KtestifyConfig.reset();
        KtestifyConfig.load(ConfigFactory.parseString("ktestify.framework.reporting.enabled = false"));
        registry = mock(PluginRegistry.class);
        when(registry.getGluePackages()).thenReturn(List.of("test.plugin.steps"));
        plugins = mockStatic(PluginRegistry.class);
        plugins.when(() -> PluginRegistry.load(any(PluginContext.class))).thenReturn(registry);
        cucumber = mockStatic(Main.class);
    }

    @AfterEach
    void tearDown() {
        cucumber.closeOnDemand();
        plugins.close();
        if (originalConfigFile == null) {
            System.clearProperty("config.file");
        } else {
            System.setProperty("config.file", originalConfigFile);
        }
        KtestifyConfig.reset();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void returnsCucumberExitCodeAfterShutdown(int code) {
        cucumber.when(() -> Main.run(any(String[].class), any(ClassLoader.class))).thenReturn((byte) code);

        assertEquals(code, KtestifyMain.run(new String[]{"features"}));

        cucumber.verify(() -> Main.run(
                aryEq(new String[]{"--glue", "io.github.ktestify.steps", "--glue", "test.plugin.steps", "features"}),
                eq(Thread.currentThread().getContextClassLoader())));
        verify(registry).shutdown();
    }

    @Test
    void defaultArgumentsIncludeCoreAndPluginGlue() {
        assertEquals(0, KtestifyMain.run(new String[0]));

        cucumber.verify(() -> Main.run(
                argThat(args -> Arrays.asList(args).containsAll(List.of(
                        "--glue", "io.github.ktestify.steps", "test.plugin.steps"))),
                any(ClassLoader.class)));
        verify(registry).shutdown();
    }

    @Test
    void realCucumberHelpReturnsWithoutTerminatingJvm() {
        cucumber.closeOnDemand();

        assertEquals(0, KtestifyMain.run(new String[]{"--help"}));

        verify(registry).shutdown();
    }

    @Test
    void executionFailureStillShutsDownPlugins() {
        cucumber.when(() -> Main.run(any(String[].class), any(ClassLoader.class)))
                .thenThrow(new IllegalStateException("simulated execution failure"));

        assertEquals(2, KtestifyMain.run(new String[]{"features"}));

        verify(registry).shutdown();
    }

    @Test
    void executionErrorStillShutsDownPluginsAndPropagates() {
        AssertionError error = new AssertionError("simulated execution error");
        cucumber.when(() -> Main.run(any(String[].class), any(ClassLoader.class))).thenThrow(error);

        assertSame(error, assertThrows(AssertionError.class, () -> KtestifyMain.run(new String[]{"features"})));

        verify(registry).shutdown();
    }

    @Test
    void argumentPreparationFailureStillShutsDownPlugins() {
        when(registry.getGluePackages()).thenThrow(new IllegalStateException("simulated glue failure"));

        assertEquals(2, KtestifyMain.run(new String[]{"features"}));

        verify(registry).shutdown();
        cucumber.verifyNoInteractions();
    }

    @Test
    void pluginInitializationFailureReturnsErrorWithoutUsingUnassignedRegistry() {
        plugins.when(() -> PluginRegistry.load(any(PluginContext.class)))
                .thenThrow(new PluginException("simulated init failure"));

        assertEquals(2, KtestifyMain.run(new String[]{"features"}));

        verifyNoInteractions(registry);
        cucumber.verifyNoInteractions();
    }

    @Test
    void missingConfigFileAbortsBeforePluginInitialization(@TempDir Path tempDir) {
        assertEquals(2, KtestifyMain.run(new String[]{"--config", tempDir.resolve("missing.conf").toString()}));

        plugins.verifyNoInteractions();
        cucumber.verifyNoInteractions();
    }

    @Test
    void invalidConfigurationAbortsBeforePluginInitialization() {
        KtestifyConfig.reset();
        try (MockedStatic<KtestifyConfig> config = mockStatic(KtestifyConfig.class)) {
            config.when(KtestifyConfig::getOrLoad).thenThrow(new IllegalArgumentException("simulated config failure"));

            assertEquals(2, KtestifyMain.run(new String[]{"features"}));
        }

        plugins.verifyNoInteractions();
        cucumber.verifyNoInteractions();
    }

    @Test
    void stripsExternalConfigOptionBeforeCallingCucumber(@TempDir Path tempDir) throws Exception {
        Path file = Files.writeString(tempDir.resolve("test.conf"), "");

        assertEquals(0, KtestifyMain.run(new String[]{"--config", file.toString(), "features"}));

        assertEquals(file.toAbsolutePath().toString(), System.getProperty("config.file"));
        cucumber.verify(() -> Main.run(
                aryEq(new String[]{"--glue", "io.github.ktestify.steps", "--glue", "test.plugin.steps", "features"}),
                any(ClassLoader.class)));
        verify(registry).shutdown();
    }

    @ParameterizedTest
    @ValueSource(strings = {"INFO", "DEBUG"})
    void logsPlainConfigurationWithAllowlistedDebugDetails(String level) {
        KtestifyConfig.reset();
        KtestifyConfig.load(ConfigFactory.parseString("""
                ktestify.framework.reporting.enabled = false
                ktestify.framework.timeouts.poll-interval = 123ms
                ktestify.framework.timeouts.buffer-time = 456ms
                ktestify.framework.directories.schemas = "/test/schemas"
                ktestify.framework.directories.output = "/test/output"
                ktestify.plugins.http.token = "must-not-be-logged"
                ktestify.kafka.topic-namespace = "debug-tests"
                ktestify.kafka.producer.retries = 7
                ktestify.kafka.consumer.group-id = "debug-consumer"
                ktestify.kafka.consumer.max-poll-records = 42
                ktestify.kafka.security.sasl-jaas-config = "must-not-be-logged"
                ktestify.kafka.security.ssl-truststore-password = "must-not-be-logged"
                ktestify.schema-registry.cache-capacity = 123
                ktestify.schema-registry.auth.basic-auth-user-info = "must-not-be-logged"
                ktestify.schema-registry.ssl.keystore-password = "must-not-be-logged"
                """).withValue("ktestify.logging.level", com.typesafe.config.ConfigValueFactory.fromAnyRef(level)));

        List<LogEvent> events = new ArrayList<>();
        AbstractAppender appender = new AbstractAppender("config-test", null, null, true, null) {
            @Override
            public void append(LogEvent event) {
                events.add(event.toImmutable());
            }
        };
        Logger logger = (Logger) LogManager.getLogger(KtestifyMain.class);
        Level originalLevel = logger.getLevel();
        logger.setLevel(Level.valueOf(level));
        appender.start();
        logger.addAppender(appender);
        try {
            assertEquals(0, KtestifyMain.run(new String[]{"features"}));
        } finally {
            logger.removeAppender(appender);
            logger.setLevel(originalLevel);
            appender.stop();
        }

        List<String> messages = events.stream()
                .map(event -> event.getMessage().getFormattedMessage())
                .toList();
        assertTrue(messages.contains("KTestify effective configuration"));
        assertTrue(messages.stream().anyMatch(message -> message.startsWith("read-timeout: ")));
        assertFalse(messages.stream().anyMatch(message -> message.chars()
                .anyMatch(character -> character >= 0x2500 && character <= 0x257F)));
        assertFalse(messages.stream().anyMatch(message -> message.contains("must-not-be-logged")));
        List<String> debugMessages = events.stream()
                .filter(event -> event.getLevel() == Level.DEBUG)
                .map(event -> event.getMessage().getFormattedMessage())
                .toList();
        if ("DEBUG".equals(level)) {
            assertTrue(debugMessages.containsAll(List.of(
                    "poll-interval: 123ms",
                    "buffer-time: 456ms",
                    "schemas-directory: '/test/schemas'",
                    "output-directory: '/test/output'",
                    "reporting.enabled: false",
                    "kafka.topic-namespace: 'debug-tests'",
                    "kafka.producer.retries: 7",
                    "kafka.consumer.group-id: 'debug-consumer'",
                    "kafka.consumer.max-poll-records: 42",
                    "kafka.security.sasl-jaas-configured: true",
                    "kafka.security.truststore-configured: false",
                    "schema-registry.cache-capacity: 123",
                    "schema-registry.auth.user-info-configured: true")));
            assertTrue(debugMessages.stream().anyMatch(message -> message.startsWith("kafka.producer.linger-ms: ")));
            assertTrue(debugMessages.stream().anyMatch(message -> message.startsWith("kafka.consumer.session-timeout: ")));
            assertTrue(debugMessages.stream().anyMatch(message -> message.startsWith("schema-registry.compatibility-level: ")));
            assertTrue(debugMessages.stream().anyMatch(message -> message.startsWith("snapshot-mode: ")));
            assertTrue(debugMessages.stream().anyMatch(message -> message.startsWith("strict-matching: ")));
            assertTrue(debugMessages.stream().anyMatch(message -> message.startsWith("max-retries: ")));
            assertTrue(debugMessages.stream().anyMatch(message -> message.startsWith("reporting.format: ")));
            assertTrue(debugMessages.stream().anyMatch(message -> message.startsWith("reporting.output: ")));
        } else {
            assertTrue(debugMessages.isEmpty());
        }
    }
}

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
package io.github.ktestify.services;

import io.github.ktestify.entities.KtestifyAssetsDirectory;
import io.github.ktestify.exceptions.ConsumerException;
import io.github.ktestify.exceptions.FetchTimeoutException;
import io.github.ktestify.match.KeyMatchStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the private helper methods of {@link ConsumerValidationService}.
 *
 * <p>The public API methods require a live Kafka broker and are covered by integration tests. These tests focus on the
 * pure-logic helpers (string parsing, path resolution, comma splitting) that are exercised on every validation call.
 */
@DisplayName("ConsumerValidationService — private helpers")
class ConsumerValidationServiceHelpersTest {

    // ── getString ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getString(row, col)")
    class GetString {

        @Test
        @DisplayName("returns the value for a present, non-blank column")
        void returnsValue() throws Exception {
            assertEquals("rt-out", invokeGetString(Map.of("col", "rt-out"), "col"));
        }

        @Test
        @DisplayName("returns null for absent column")
        void nullForAbsent() throws Exception {
            assertNull(invokeGetString(Map.of(), "col"));
        }

        @Test
        @DisplayName("returns null for blank value")
        void nullForBlank() throws Exception {
            Map<String, String> row = new HashMap<>();
            row.put("col", "   ");
            assertNull(invokeGetString(row, "col"));
        }
    }

    // ── getSecondsToMillis ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getSecondsToMillis(row, col)")
    class GetSecondsToMillis {

        @Test
        @DisplayName("converts seconds string to milliseconds")
        void convertsToMs() throws Exception {
            assertEquals(30_000L, invokeGetSecondsToMillis(Map.of("timeout", "30"), "timeout"));
        }

        @Test
        @DisplayName("returns null when column is absent")
        void nullForAbsent() throws Exception {
            assertNull(invokeGetSecondsToMillis(Map.of(), "timeout"));
        }

        @Test
        @DisplayName("1 second → 1000 ms")
        void oneSecond() throws Exception {
            assertEquals(1_000L, invokeGetSecondsToMillis(Map.of("t", "1"), "t"));
        }
    }

    // ── splitComma ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("splitComma(value)")
    class SplitComma {

        @Test
        @DisplayName("splits and trims a comma-separated string")
        void splitsAndTrims() throws Exception {
            List<String> result = invokeSplitComma("a, b ,c");
            assertEquals(List.of("a", "b", "c"), result);
        }

        @Test
        @DisplayName("returns empty list for null input")
        void nullInput() throws Exception {
            assertEquals(Collections.emptyList(), invokeSplitComma(null));
        }

        @Test
        @DisplayName("returns empty list for blank input")
        void blankInput() throws Exception {
            assertEquals(Collections.emptyList(), invokeSplitComma("   "));
        }

        @Test
        @DisplayName("filters blank tokens between commas")
        void filtersBlankTokens() throws Exception {
            assertEquals(List.of("a", "b"), invokeSplitComma("a,,b"));
        }
    }

    // ── resolve ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("resolve(assets, path)")
    class Resolve {

        @Test
        @DisplayName("returns path as-is when assets is null")
        void nullAssetsReturnsPath() throws Exception {
            assertEquals("integration/file.json", invokeResolve(null, "integration/file.json"));
        }

        @Test
        @DisplayName("delegates to KtestifyAssetsDirectory.resolve when assets is set")
        void delegatesToAssets() throws Exception {
            KtestifyAssetsDirectory assets =
                    KtestifyAssetsDirectory.builder().absolutePath("/data").build();
            assertEquals("/data/integration/file.json", invokeResolve(assets, "integration/file.json"));
        }
    }

    // ── getKeyMatchStrategy ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getKeyMatchStrategy(row)")
    class GetKeyMatchStrategy {

        @Test
        @DisplayName("returns EXACT when keyMatchStrategy column is absent")
        void exactWhenAbsent() throws Exception {
            assertEquals(KeyMatchStrategy.EXACT, invokeGetKeyMatchStrategy(Map.of()));
        }

        @Test
        @DisplayName("returns EXACT when keyMatchStrategy is blank")
        void exactWhenBlank() throws Exception {
            Map<String, String> row = new HashMap<>();
            row.put("keyMatchStrategy", "   ");
            assertEquals(KeyMatchStrategy.EXACT, invokeGetKeyMatchStrategy(row));
        }

        @Test
        @DisplayName("returns EXACT when value is 'exact'")
        void parsesExact() throws Exception {
            assertEquals(KeyMatchStrategy.EXACT, invokeGetKeyMatchStrategy(Map.of("keyMatchStrategy", "exact")));
        }

        @Test
        @DisplayName("returns CONTAINS when value is 'contains'")
        void parsesContains() throws Exception {
            assertEquals(KeyMatchStrategy.CONTAINS, invokeGetKeyMatchStrategy(Map.of("keyMatchStrategy", "contains")));
        }

        @Test
        @DisplayName("returns STARTS_WITH when value is 'starts_with'")
        void parsesStartsWith() throws Exception {
            assertEquals(
                    KeyMatchStrategy.STARTS_WITH, invokeGetKeyMatchStrategy(Map.of("keyMatchStrategy", "starts_with")));
        }

        @Test
        @DisplayName("returns STARTS_WITH when value is 'starts-with' (hyphen tolerant)")
        void parsesStartsWithHyphen() throws Exception {
            assertEquals(
                    KeyMatchStrategy.STARTS_WITH, invokeGetKeyMatchStrategy(Map.of("keyMatchStrategy", "starts-with")));
        }

        @Test
        @DisplayName("returns ENDS_WITH when value is 'ends_with'")
        void parsesEndsWith() throws Exception {
            assertEquals(
                    KeyMatchStrategy.ENDS_WITH, invokeGetKeyMatchStrategy(Map.of("keyMatchStrategy", "ends_with")));
        }

        @Test
        @DisplayName("returns REGEX when value is 'regex'")
        void parsesRegex() throws Exception {
            assertEquals(KeyMatchStrategy.REGEX, invokeGetKeyMatchStrategy(Map.of("keyMatchStrategy", "regex")));
        }

        @Test
        @DisplayName("returns EXACT for unrecognized value (backward compatibility)")
        void exactForUnrecognized() throws Exception {
            assertEquals(KeyMatchStrategy.EXACT, invokeGetKeyMatchStrategy(Map.of("keyMatchStrategy", "fuzzy")));
        }

        @Test
        @DisplayName("parses case-insensitively")
        void parsesCaseInsensitively() throws Exception {
            assertEquals(KeyMatchStrategy.CONTAINS, invokeGetKeyMatchStrategy(Map.of("keyMatchStrategy", "CONTAINS")));
        }
    }

    @Nested
    @DisplayName("isTimeoutLikeFailure(error)")
    class TimeoutClassification {

        @Test
        @DisplayName("returns true for timeout-style messages")
        void trueForFetchTimeoutType() throws Exception {
            ConsumerException failure = new ConsumerException(
                    "wrapped", new FetchTimeoutException("Timed out after 1000ms waiting for a record."));
            assertTrue(invokeIsTimeoutLikeFailure(failure));
        }

        @Test
        @DisplayName("returns true when cause chain contains TimeoutException")
        void trueForTimeoutCause() throws Exception {
            ConsumerException failure = new ConsumerException(
                    "Consumer execution failed", new IllegalStateException("wrapped", new TimeoutException("boom")));
            assertTrue(invokeIsTimeoutLikeFailure(failure));
        }

        @Test
        @DisplayName("returns false for non-timeout errors")
        void falseForNonTimeoutFailure() throws Exception {
            ConsumerException failure = new ConsumerException("Authentication failed for broker");
            assertFalse(invokeIsTimeoutLikeFailure(failure));
        }
    }

    // ── Reflection helpers ────────────────────────────────────────────────────

    private static String invokeGetString(Map<String, String> row, String col) throws Exception {
        Method m = ConsumerValidationService.class.getDeclaredMethod("getString", Map.class, String.class);
        m.setAccessible(true);
        return (String) m.invoke(null, row, col);
    }

    private static Long invokeGetSecondsToMillis(Map<String, String> row, String col) throws Exception {
        Method m = ConsumerValidationService.class.getDeclaredMethod("getSecondsToMillis", Map.class, String.class);
        m.setAccessible(true);
        return (Long) m.invoke(null, row, col);
    }

    @SuppressWarnings("unchecked")
    private static List<String> invokeSplitComma(String value) throws Exception {
        Method m = ConsumerValidationService.class.getDeclaredMethod("splitComma", String.class);
        m.setAccessible(true);
        return (List<String>) m.invoke(null, value);
    }

    private static String invokeResolve(KtestifyAssetsDirectory assets, String path) throws Exception {
        Method m = ConsumerValidationService.class.getDeclaredMethod(
                "resolve", KtestifyAssetsDirectory.class, String.class);
        m.setAccessible(true);
        return (String) m.invoke(null, assets, path);
    }

    private static KeyMatchStrategy invokeGetKeyMatchStrategy(Map<String, String> row) throws Exception {
        Method m = ConsumerValidationService.class.getDeclaredMethod("getKeyMatchStrategy", Map.class);
        m.setAccessible(true);
        return (KeyMatchStrategy) m.invoke(null, row);
    }

    private static boolean invokeIsTimeoutLikeFailure(Throwable failure) throws Exception {
        Method m = ConsumerValidationService.class.getDeclaredMethod("isTimeoutLikeFailure", Throwable.class);
        m.setAccessible(true);
        return (boolean) m.invoke(null, failure);
    }
}

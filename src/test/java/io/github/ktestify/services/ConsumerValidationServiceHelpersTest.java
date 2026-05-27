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

import static org.junit.jupiter.api.Assertions.*;

import io.github.ktestify.entities.KtestifyAssetsDirectory;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
}

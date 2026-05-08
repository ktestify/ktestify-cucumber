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
package io.github.ktestify.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DataTableUtils}.
 *
 * <p>All methods that accept a {@code Map<String, String>} row are tested directly without involving a Cucumber
 * {@code DataTable}, keeping tests fast and dependency-free.
 */
@DisplayName("DataTableUtils")
class DataTableUtilsTest {

    // ── getString(row, column) ────────────────────────────────────────────────

    @Nested
    @DisplayName("getString(row, column)")
    class GetString {

        @Test
        @DisplayName("returns the value when the column is present and non-blank")
        void returnsValue() {
            Map<String, String> row = Map.of("topicAlias", "rt-out");
            assertEquals("rt-out", DataTableUtils.getString(row, "topicAlias"));
        }

        @Test
        @DisplayName("returns null when the column is absent")
        void nullForAbsentColumn() {
            assertNull(DataTableUtils.getString(Map.of(), "missing"));
        }

        @Test
        @DisplayName("returns null when the value is blank")
        void nullForBlankValue() {
            Map<String, String> row = new HashMap<>();
            row.put("col", "   ");
            assertNull(DataTableUtils.getString(row, "col"));
        }

        @Test
        @DisplayName("returns null when the value is an empty string")
        void nullForEmptyString() {
            Map<String, String> row = new HashMap<>();
            row.put("col", "");
            assertNull(DataTableUtils.getString(row, "col"));
        }
    }

    // ── getLong(row, column) ──────────────────────────────────────────────────

    @Nested
    @DisplayName("getLong(row, column)")
    class GetLong {

        @Test
        @DisplayName("parses a valid long value")
        void parsesLong() {
            Map<String, String> row = Map.of("timeout", "30000");
            assertEquals(30000L, DataTableUtils.getLong(row, "timeout"));
        }

        @Test
        @DisplayName("returns null for absent column")
        void nullForAbsentColumn() {
            assertNull(DataTableUtils.getLong(Map.of(), "timeout"));
        }

        @Test
        @DisplayName("throws IllegalArgumentException for non-numeric value")
        void throwsForNonNumeric() {
            Map<String, String> row = Map.of("timeout", "abc");
            IllegalArgumentException ex =
                    assertThrows(IllegalArgumentException.class, () -> DataTableUtils.getLong(row, "timeout"));
            assertTrue(ex.getMessage().contains("timeout"));
        }
    }

    // ── getInt(row, column) ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getInt(row, column)")
    class GetInt {

        @Test
        @DisplayName("parses a valid integer value")
        void parsesInt() {
            Map<String, String> row = Map.of("line", "0");
            assertEquals(0, DataTableUtils.getInt(row, "line"));
        }

        @Test
        @DisplayName("returns null for absent column")
        void nullForAbsentColumn() {
            assertNull(DataTableUtils.getInt(Map.of(), "line"));
        }

        @Test
        @DisplayName("throws IllegalArgumentException for non-numeric value")
        void throwsForNonNumeric() {
            Map<String, String> row = Map.of("line", "one");
            IllegalArgumentException ex =
                    assertThrows(IllegalArgumentException.class, () -> DataTableUtils.getInt(row, "line"));
            assertTrue(ex.getMessage().contains("line"));
        }

        @Test
        @DisplayName("trims whitespace before parsing")
        void trimsWhitespace() {
            Map<String, String> row = Map.of("line", " 5 ");
            assertEquals(5, DataTableUtils.getInt(row, "line"));
        }
    }

    // ── getList(row, column) ──────────────────────────────────────────────────

    @Nested
    @DisplayName("getList(row, column)")
    class GetList {

        @Test
        @DisplayName("splits a comma-separated value into trimmed tokens")
        void splitsList() {
            Map<String, String> row = Map.of("files", "a.json, b.json ,c.json");
            assertEquals(List.of("a.json", "b.json", "c.json"), DataTableUtils.getList(row, "files"));
        }

        @Test
        @DisplayName("returns empty list for absent column")
        void emptyListForAbsentColumn() {
            assertEquals(Collections.emptyList(), DataTableUtils.getList(Map.of(), "files"));
        }

        @Test
        @DisplayName("returns empty list for blank value")
        void emptyListForBlankValue() {
            Map<String, String> row = new HashMap<>();
            row.put("files", "   ");
            assertEquals(Collections.emptyList(), DataTableUtils.getList(row, "files"));
        }

        @Test
        @DisplayName("single-element CSV returns a one-element list")
        void singleElement() {
            Map<String, String> row = Map.of("files", "only.json");
            assertEquals(List.of("only.json"), DataTableUtils.getList(row, "files"));
        }

        @Test
        @DisplayName("filters out blank tokens between commas")
        void filtersBlankTokens() {
            Map<String, String> row = Map.of("files", "a.json,,b.json");
            assertEquals(List.of("a.json", "b.json"), DataTableUtils.getList(row, "files"));
        }
    }
}

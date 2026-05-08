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
package io.github.ktestify.entities;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("KtestifySchema")
class KtestifySchemaTest {

    @Nested
    @DisplayName("builder")
    class Builder {

        @Test
        @DisplayName("builds with all fields")
        void buildsFull() {
            KtestifySchema s = KtestifySchema.builder()
                    .schemaName("OrderSchema")
                    .schemaAlias("order-schema")
                    .schemaVersion(3)
                    .build();

            assertEquals("OrderSchema", s.getSchemaName());
            assertEquals("order-schema", s.getSchemaAlias());
            assertEquals(3, s.getSchemaVersion());
        }

        @Test
        @DisplayName("builds with schema name only (alias and version null)")
        void buildsNameOnly() {
            KtestifySchema s = KtestifySchema.builder().schemaName("MySchema").build();
            assertEquals("MySchema", s.getSchemaName());
            assertNull(s.getSchemaAlias());
            assertNull(s.getSchemaVersion());
        }

        @Test
        @DisplayName("builds with version 0 (use latest)")
        void buildsVersionZero() {
            KtestifySchema s =
                    KtestifySchema.builder().schemaName("S").schemaVersion(0).build();
            assertEquals(0, s.getSchemaVersion());
        }

        @Test
        @DisplayName("builds with all-null fields")
        void buildsAllNull() {
            KtestifySchema s = KtestifySchema.builder().build();
            assertNull(s.getSchemaName());
            assertNull(s.getSchemaAlias());
            assertNull(s.getSchemaVersion());
        }
    }

    @Nested
    @DisplayName("equals and hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("equal objects with same fields")
        void equalObjects() {
            KtestifySchema a =
                    KtestifySchema.builder().schemaName("S").schemaVersion(1).build();
            KtestifySchema b =
                    KtestifySchema.builder().schemaName("S").schemaVersion(1).build();
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("unequal when schemaName differs")
        void unequalByName() {
            KtestifySchema a = KtestifySchema.builder().schemaName("A").build();
            KtestifySchema b = KtestifySchema.builder().schemaName("B").build();
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("unequal when schemaVersion differs")
        void unequalByVersion() {
            KtestifySchema a =
                    KtestifySchema.builder().schemaName("S").schemaVersion(1).build();
            KtestifySchema b =
                    KtestifySchema.builder().schemaName("S").schemaVersion(2).build();
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("not equal to null")
        void notEqualToNull() {
            KtestifySchema s = KtestifySchema.builder().schemaName("S").build();
            assertNotEquals(null, s);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        @DisplayName("contains schemaName")
        void containsSchemaName() {
            KtestifySchema s =
                    KtestifySchema.builder().schemaName("OrderSchema").build();
            assertTrue(s.toString().contains("OrderSchema"));
        }

        @Test
        @DisplayName("contains schemaVersion when set")
        void containsVersion() {
            KtestifySchema s = KtestifySchema.builder().schemaVersion(5).build();
            assertTrue(s.toString().contains("5"));
        }
    }
}

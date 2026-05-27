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

@DisplayName("KtestifyNamespace")
class KtestifyNamespaceTest {

    @Nested
    @DisplayName("builder")
    class Builder {

        @Test
        @DisplayName("builds with namespace and alias")
        void buildsFull() {
            KtestifyNamespace ns = KtestifyNamespace.builder()
                    .namespace("my-ns")
                    .namespaceAlias("ns-alias")
                    .build();
            assertEquals("my-ns", ns.getNamespace());
            assertEquals("ns-alias", ns.getNamespaceAlias());
        }

        @Test
        @DisplayName("builds with namespace only (alias null)")
        void buildsNamespaceOnly() {
            KtestifyNamespace ns = KtestifyNamespace.builder().namespace("prod").build();
            assertEquals("prod", ns.getNamespace());
            assertNull(ns.getNamespaceAlias());
        }

        @Test
        @DisplayName("builds with all-null fields")
        void buildsAllNull() {
            KtestifyNamespace ns = KtestifyNamespace.builder().build();
            assertNull(ns.getNamespace());
            assertNull(ns.getNamespaceAlias());
        }
    }

    @Nested
    @DisplayName("equals and hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("equal objects with same field values")
        void equalObjects() {
            KtestifyNamespace a = KtestifyNamespace.builder()
                    .namespace("ns")
                    .namespaceAlias("alias")
                    .build();
            KtestifyNamespace b = KtestifyNamespace.builder()
                    .namespace("ns")
                    .namespaceAlias("alias")
                    .build();
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("unequal objects with different namespace")
        void unequalObjects() {
            KtestifyNamespace a = KtestifyNamespace.builder().namespace("ns-a").build();
            KtestifyNamespace b = KtestifyNamespace.builder().namespace("ns-b").build();
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("not equal to null")
        void notEqualToNull() {
            KtestifyNamespace ns = KtestifyNamespace.builder().namespace("ns").build();
            assertNotEquals(null, ns);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        @DisplayName("contains namespace value")
        void containsNamespace() {
            KtestifyNamespace ns =
                    KtestifyNamespace.builder().namespace("my-ns").build();
            assertTrue(ns.toString().contains("my-ns"));
        }
    }
}

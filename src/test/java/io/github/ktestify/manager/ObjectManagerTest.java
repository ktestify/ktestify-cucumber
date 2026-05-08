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
package io.github.ktestify.manager;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ObjectManager")
class ObjectManagerTest {

    private ObjectManager<String> manager;

    @BeforeEach
    void setUp() {
        manager = new ObjectManager<>();
    }

    // ── register(name, object) ────────────────────────────────────────────────

    @Nested
    @DisplayName("register(name, object)")
    class RegisterByName {

        @Test
        @DisplayName("stores the object under the given name")
        void storesObject() {
            manager.register("topic-a", "raw-topic");
            assertTrue(manager.contains("topic-a"));
        }

        @Test
        @DisplayName("get() returns the stored object")
        void getReturnsObject() {
            manager.register("topic-a", "raw-topic");
            assertEquals(Optional.of("raw-topic"), manager.get("topic-a"));
        }

        @Test
        @DisplayName("overwriting a key replaces the previous value")
        void overwriteReplaces() {
            manager.register("key", "first");
            manager.register("key", "second");
            assertEquals("second", manager.getOrThrow("key"));
        }
    }

    // ── register(name, alias, object) ────────────────────────────────────────

    @Nested
    @DisplayName("register(name, alias, object)")
    class RegisterWithAlias {

        @Test
        @DisplayName("object is reachable by canonical name")
        void reachableByName() {
            manager.register("raw-roundtrip", "rt-out", "value");
            assertTrue(manager.contains("raw-roundtrip"));
        }

        @Test
        @DisplayName("object is reachable by alias")
        void reachableByAlias() {
            manager.register("raw-roundtrip", "rt-out", "value");
            assertTrue(manager.contains("rt-out"));
        }

        @Test
        @DisplayName("both keys return the same object instance")
        void bothKeysReturnSameObject() {
            manager.register("raw-roundtrip", "rt-out", "value");
            assertSame(manager.getOrThrow("raw-roundtrip"), manager.getOrThrow("rt-out"));
        }

        @Test
        @DisplayName("null alias is silently ignored")
        void nullAliasIgnored() {
            manager.register("topic", null, "value");
            assertTrue(manager.contains("topic"));
            assertFalse(manager.contains("null"));
        }

        @Test
        @DisplayName("blank alias is silently ignored")
        void blankAliasIgnored() {
            manager.register("topic", "   ", "value");
            assertTrue(manager.contains("topic"));
            assertFalse(manager.contains("   "));
        }
    }

    // ── get() ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("get(key)")
    class Get {

        @Test
        @DisplayName("returns empty Optional for unknown key")
        void unknownKeyReturnsEmpty() {
            assertEquals(Optional.empty(), manager.get("nonexistent"));
        }
    }

    // ── getOrThrow() ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getOrThrow(key)")
    class GetOrThrow {

        @Test
        @DisplayName("throws IllegalStateException and includes the key in the message")
        void throwsForUnknownKey() {
            IllegalStateException ex =
                    assertThrows(IllegalStateException.class, () -> manager.getOrThrow("missing-key"));
            assertTrue(ex.getMessage().contains("missing-key"));
        }

        @Test
        @DisplayName("returns the object when key is present")
        void returnsObjectWhenPresent() {
            manager.register("k", "v");
            assertEquals("v", manager.getOrThrow("k"));
        }
    }

    // ── contains() ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("contains(key)")
    class Contains {

        @Test
        @DisplayName("returns false for unknown key")
        void falseForUnknownKey() {
            assertFalse(manager.contains("x"));
        }

        @Test
        @DisplayName("returns true for registered key")
        void trueForRegisteredKey() {
            manager.register("x", "val");
            assertTrue(manager.contains("x"));
        }
    }

    // ── clear() ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("clear()")
    class Clear {

        @Test
        @DisplayName("removes all entries")
        void removesAll() {
            manager.register("a", "1");
            manager.register("b", "2");
            manager.clear();
            assertFalse(manager.contains("a"));
            assertFalse(manager.contains("b"));
        }

        @Test
        @DisplayName("get() returns empty after clear")
        void getEmptyAfterClear() {
            manager.register("k", "v");
            manager.clear();
            assertEquals(Optional.empty(), manager.get("k"));
        }
    }
}

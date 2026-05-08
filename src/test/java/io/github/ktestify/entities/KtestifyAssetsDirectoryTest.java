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

@DisplayName("KtestifyAssetsDirectory")
class KtestifyAssetsDirectoryTest {

    // ── resolve(relativePath) ─────────────────────────────────────────────────

    @Nested
    @DisplayName("resolve(path)")
    class Resolve {

        @Test
        @DisplayName("relative path is resolved against the base directory")
        void resolvesRelativePath() {
            KtestifyAssetsDirectory dir = KtestifyAssetsDirectory.builder()
                    .absolutePath("/data/assets")
                    .build();
            assertEquals("/data/assets/integration/payload.json", dir.resolve("integration/payload.json"));
        }

        @Test
        @DisplayName("absolute path is returned unchanged")
        void absolutePathPassedThrough() {
            KtestifyAssetsDirectory dir = KtestifyAssetsDirectory.builder()
                    .absolutePath("/data/assets")
                    .build();
            assertEquals("/tmp/other/file.json", dir.resolve("/tmp/other/file.json"));
        }

        @Test
        @DisplayName("null path is returned as null")
        void nullPathReturnsNull() {
            KtestifyAssetsDirectory dir = KtestifyAssetsDirectory.builder()
                    .absolutePath("/data/assets")
                    .build();
            assertNull(dir.resolve(null));
        }

        @Test
        @DisplayName("blank path is returned as-is")
        void blankPathReturnedAsIs() {
            KtestifyAssetsDirectory dir = KtestifyAssetsDirectory.builder()
                    .absolutePath("/data/assets")
                    .build();
            assertEquals("   ", dir.resolve("   "));
        }

        @Test
        @DisplayName("relative path with null base is returned as-is")
        void nullBaseReturnsPathAsIs() {
            KtestifyAssetsDirectory dir =
                    KtestifyAssetsDirectory.builder().absolutePath(null).build();
            assertEquals("integration/payload.json", dir.resolve("integration/payload.json"));
        }

        @Test
        @DisplayName("relative path with blank base is returned as-is")
        void blankBaseReturnsPathAsIs() {
            KtestifyAssetsDirectory dir =
                    KtestifyAssetsDirectory.builder().absolutePath("   ").build();
            assertEquals("integration/payload.json", dir.resolve("integration/payload.json"));
        }

        @Test
        @DisplayName("filename-only path is joined to the base")
        void filenameOnlyJoinedToBase() {
            KtestifyAssetsDirectory dir =
                    KtestifyAssetsDirectory.builder().absolutePath("/assets").build();
            assertEquals("/assets/file.json", dir.resolve("file.json"));
        }
    }
}

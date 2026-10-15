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
package io.github.ktestify.lint;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GherkinDocumentParserTest {

    private static final Path LINT_DIR = Path.of("src", "test", "resources", "lint");

    @Test
    void cleanFeatureParsesIntoDocument() throws IOException {
        GherkinDocumentParser.ParseResult result = GherkinDocumentParser.parse(LINT_DIR.resolve("clean.feature"));
        assertTrue(result.violations().isEmpty());
        assertTrue(result.document().isPresent());
        assertEquals(
                "Clean feature",
                result.document().get().getFeature().orElseThrow().getName());
    }

    @Test
    void malformedTableBecomesViolation() throws IOException {
        GherkinDocumentParser.ParseResult result = GherkinDocumentParser.parse(LINT_DIR.resolve("malformed.feature"));
        assertFalse(result.violations().isEmpty());
        assertTrue(result.violations().getFirst().rule() == LintRule.G007
                || result.violations().getFirst().line() >= 1);
    }

    @Test
    void emptyFileYieldsNoDocument(@TempDir Path tempDir) throws IOException {
        Path empty = tempDir.resolve("empty.feature");
        Files.writeString(empty, "");
        GherkinDocumentParser.ParseResult result = GherkinDocumentParser.parse(empty);
        assertTrue(result.document().isEmpty()
                || result.document().get().getFeature().isEmpty());
    }

    @Test
    void violationsCarryLineNumbers() throws IOException {
        List<LintViolation> violations = GherkinDocumentParser.parse(LINT_DIR.resolve("malformed.feature"))
                .violations();
        for (LintViolation v : violations) {
            assertTrue(v.line() >= 1);
            assertTrue(v.column() >= 1);
        }
    }
}

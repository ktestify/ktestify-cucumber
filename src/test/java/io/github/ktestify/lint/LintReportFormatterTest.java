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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LintReportFormatterTest {

    private final Path path = Path.of("test.feature");

    private final List<LintViolation> violations = List.of(
            new LintViolation(LintRule.G001, 3, 2, "Tab character found; use spaces for indentation."),
            new LintViolation(LintRule.D002, 7, 1, "'out' used as topicAlias is not declared."));

    @Test
    void textIncludesRuleAndMessage() {
        String text = LintReportFormatter.format(LintReportFormatter.Format.TEXT, Map.of(path, violations));
        assertTrue(text.contains("G001"));
        assertTrue(text.contains("Tab character found"));
        assertTrue(text.contains("2 error(s), 0 warning(s)"));
    }

    @Test
    void jsonIncludesEveryViolation() {
        String json = LintReportFormatter.format(LintReportFormatter.Format.JSON, Map.of(path, violations));
        assertTrue(json.contains("\"rule\""));
        assertTrue(json.contains("\"G001\""));
        assertTrue(json.contains("\"D002\""));
    }

    @Test
    void codeclimateIncludesFingerprintAndSeverity() {
        String cc = LintReportFormatter.format(LintReportFormatter.Format.CODECLIMATE, Map.of(path, violations));
        assertTrue(cc.contains("\"check_name\""));
        assertTrue(cc.contains("\"fingerprint\""));
        assertTrue(cc.contains("\"major\""));
    }

    @Test
    void emptyResultsRenderCleanText() {
        String text = LintReportFormatter.format(LintReportFormatter.Format.TEXT, Map.of(path, List.of()));
        assertTrue(text.contains("no problems"));
        assertTrue(text.contains("0 error(s), 0 warning(s)"));
    }
}

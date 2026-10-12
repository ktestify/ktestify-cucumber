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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureLintChecksTest {

    private final LintOptions options = LintOptions.defaults();

    private List<LintRule> rules(String text) {
        return StructureLintChecks.check(text, options).stream()
                .map(LintViolation::rule)
                .toList();
    }

    @Test
    void tabIsReported() {
        assertTrue(rules("Feature: F\n\n  Scenario: S\n\tGiven a step\n").contains(LintRule.G001));
    }

    @Test
    void trailingWhitespaceIsReported() {
        assertTrue(rules("Feature: F \n").contains(LintRule.G002));
    }

    @Test
    void missingEofNewlineIsReported() {
        assertTrue(rules("Feature: F\n  Scenario: S\n    Given a step\n    Then another")
                .contains(LintRule.G003));
    }

    @Test
    void doubleEofNewlineIsReported() {
        assertTrue(rules("Feature: F\n\n").contains(LintRule.G003));
    }

    @Test
    void consecutiveBlankLinesAreReported() {
        assertTrue(rules("Feature: F\n\n\n  Scenario: S\n").contains(LintRule.G013));
    }

    @Test
    void wrongStepIndentationIsReported() {
        assertTrue(rules("Feature: F\n  Scenario: S\n   Given a step\n").contains(LintRule.G012));
    }

    @Test
    void wrongTableIndentationIsReported() {
        assertTrue(rules("Feature: F\n  Scenario: S\n    Given a step\n      | col |\n     | val |\n")
                .contains(LintRule.G012));
    }

    @Test
    void cleanTextHasNoViolations() {
        String text = """
                Feature: F

                  Scenario: S
                    Given a step
                      | col |
                      | val |
                """;
        assertTrue(rules(text).isEmpty());
    }

    @Test
    void commentsAndTagsAreIgnored() {
        String text = "Feature: F\n  # comment\n  @tag\n  Scenario: S\n    Given a step\n";
        assertFalse(rules(text).contains(LintRule.G012));
    }
}

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

import java.util.ArrayList;
import java.util.List;

/**
 * Structural and formatting checks that operate on the raw lines of a feature file.
 *
 * <p>These rules are invisible in the parsed {@link io.cucumber.messages.types.GherkinDocument} AST, so they run over
 * the plain text: tabs, trailing whitespace, EOF newline, blank-line runs, and indentation.
 *
 * @since 1.2.2
 */
final class StructureLintChecks {

    /** Expected indentation (in spaces) per construct, matching the two-space house style. */
    static final int FEATURE_INDENT = 0;

    static final int SCENARIO_INDENT = 2;
    static final int STEP_INDENT = 4;
    static final int TABLE_INDENT = 6;

    private StructureLintChecks() {}

    /**
     * Runs every raw-line structural check over the file text.
     *
     * @param text the full file content
     * @param options the lint options
     * @return the violations, sorted by position
     * @since 1.2.2
     */
    static List<LintViolation> check(String text, LintOptions options) {
        List<LintViolation> out = new ArrayList<>();
        List<String> lines = text.endsWith("\n")
                ? List.of(text.substring(0, text.length() - 1).split("\n", -1))
                : List.of(text.split("\n", -1));
        checkWhitespace(lines, options, out);
        checkEofNewline(text, lines.size(), options, out);
        checkIndentation(lines, options, out);
        return out;
    }

    private static void checkWhitespace(List<String> lines, LintOptions options, List<LintViolation> out) {
        int blankRun = 0;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int lineNo = i + 1;
            if (line.contains("\t")) {
                add(
                        out,
                        options,
                        new LintViolation(LintRule.G001, lineNo, line.indexOf('\t') + 1, LintRule.G001.description()));
            }
            if (!line.equals(line.stripTrailing())) {
                add(
                        out,
                        options,
                        new LintViolation(
                                LintRule.G002, lineNo, line.stripTrailing().length() + 1, LintRule.G002.description()));
            }
            if (line.isBlank()) {
                blankRun++;
                if (blankRun == 2) {
                    add(out, options, new LintViolation(LintRule.G013, lineNo, 1, LintRule.G013.description()));
                }
            } else {
                blankRun = 0;
            }
        }
    }

    private static void checkEofNewline(String text, int lineCount, LintOptions options, List<LintViolation> out) {
        if (text.isEmpty()) {
            return;
        }
        if (!text.endsWith("\n")) {
            add(out, options, new LintViolation(LintRule.G003, lineCount, 1, "File does not end with a newline."));
        } else if (text.endsWith("\n\n")) {
            add(
                    out,
                    options,
                    new LintViolation(LintRule.G003, lineCount + 1, 1, "File ends with more than one blank line."));
        }
    }

    private static void checkIndentation(List<String> lines, LintOptions options, List<LintViolation> out) {
        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            String stripped = raw.strip();
            if (stripped.isEmpty() || stripped.startsWith("#")) {
                continue;
            }
            int indent = raw.length() - raw.stripLeading().length();
            int lineNo = i + 1;
            if (stripped.startsWith("Feature:")) {
                if (indent != FEATURE_INDENT) {
                    add(out, options, new LintViolation(LintRule.G012, lineNo, 1, "Feature should be at column 1."));
                }
            } else if (stripped.startsWith("Background:")
                    || stripped.startsWith("Scenario:")
                    || stripped.startsWith("Scenario Outline:")) {
                if (indent != SCENARIO_INDENT) {
                    add(
                            out,
                            options,
                            new LintViolation(
                                    LintRule.G012,
                                    lineNo,
                                    1,
                                    "Background/Scenario should be indented " + SCENARIO_INDENT + " spaces."));
                }
            } else if (isStepLine(stripped)) {
                if (indent != STEP_INDENT) {
                    add(
                            out,
                            options,
                            new LintViolation(
                                    LintRule.G012, lineNo, 1, "Step should be indented " + STEP_INDENT + " spaces."));
                }
            } else if (stripped.startsWith("|")) {
                if (indent != TABLE_INDENT) {
                    add(
                            out,
                            options,
                            new LintViolation(
                                    LintRule.G012,
                                    lineNo,
                                    1,
                                    "Table row should be indented " + TABLE_INDENT + " spaces."));
                }
            }
        }
    }

    private static boolean isStepLine(String stripped) {
        String first = stripped.split(" ", 2)[0];
        return switch (first) {
            case "Given", "When", "Then", "And", "But" -> true;
            default -> stripped.startsWith("*");
        };
    }

    private static void add(List<LintViolation> out, LintOptions options, LintViolation violation) {
        if (!options.isDisabled(violation.rule())) {
            out.add(violation);
        }
    }
}

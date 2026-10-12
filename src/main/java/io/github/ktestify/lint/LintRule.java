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

/**
 * Catalogue of feature-file lint rules.
 *
 * <p>Rules are grouped in two families:
 *
 * <ul>
 *   <li>{@code G*} — structural and formatting correctness of the Gherkin file itself
 *   <li>{@code D*} — semantic correctness specific to the ktestify test style (aliases, asset files)
 * </ul>
 *
 * @since 1.2.2
 */
public enum LintRule {
    // ---- Structural (G) ---------------------------------------------------------------------

    /** A tab character was found; spaces are required for indentation. */
    G001("G001", Severity.ERROR, "Tab character found; use spaces for indentation."),
    /** The line has trailing whitespace. */
    G002("G002", Severity.WARN, "Trailing whitespace."),
    /** The file does not end with exactly one newline. */
    G003("G003", Severity.WARN, "File does not end with a single newline."),
    /** The file must contain exactly one Feature declaration. */
    G004("G004", Severity.ERROR, "Exactly one Feature declaration is required per file."),
    /** The Feature has no name. */
    G005("G005", Severity.ERROR, "Feature has no name."),
    /** A Scenario or Scenario Outline has no name. */
    G006("G006", Severity.ERROR, "Scenario has no name."),
    /** A line does not start with a recognised Gherkin keyword. */
    G007("G007", Severity.WARN, "Line does not start with a recognised Gherkin keyword."),
    /** A step keyword has no text. */
    G008("G008", Severity.ERROR, "Step keyword has no text."),
    /** The Feature declares no scenario. */
    G009("G009", Severity.WARN, "Feature has no scenarios."),
    /** A data table row is malformed (must start and end with an unescaped pipe). */
    G010("G010", Severity.ERROR, "Malformed table row: every row must start and end with '|'."),
    /** A data table row has a different column count than the header. */
    G011("G011", Severity.ERROR, "Table row column count does not match the header."),
    /** A construct is not indented as the house style expects. */
    G012("G012", Severity.WARN, "Unexpected indentation."),
    /** More than one consecutive blank line. */
    G013("G013", Severity.WARN, "More than one consecutive blank line."),
    /** A data table row is not attached to any step. */
    G014("G014", Severity.ERROR, "Table row is not attached to any step."),
    /** Content appears before the Feature declaration. */
    G015("G015", Severity.ERROR, "Content appears before the Feature declaration."),
    /** A step appears outside of a Background or Scenario. */
    G016("G016", Severity.ERROR, "Step outside of a Background or Scenario."),
    /** A data table header cell is empty. */
    G017("G017", Severity.WARN, "Empty header cell in data table."),

    // ---- Domain (D) -------------------------------------------------------------------------

    /** A file-name cell value contains whitespace. */
    D001("D001", Severity.ERROR, "File value contains whitespace."),
    /** An alias referenced in a Scenario is not declared in any Background table. */
    D002("D002", Severity.ERROR, "Alias is not declared in any Background table."),
    /** A referenced asset file is missing from the assets folder. */
    D003("D003", Severity.ERROR, "Referenced file is missing from the assets folder."),
    /** The assets directory does not exist; file-existence checks are skipped. */
    D004("D004", Severity.WARN, "Assets directory not found; file-existence checks skipped.");

    private final String id;
    private final Severity severity;
    private final String description;

    LintRule(String id, Severity severity, String description) {
        this.id = id;
        this.severity = severity;
        this.description = description;
    }

    /**
     * Returns the rule identifier as printed in reports, for example {@code G011}.
     *
     * @return the rule id
     * @since 1.2.2
     */
    public String id() {
        return id;
    }

    /**
     * Returns the severity of this rule.
     *
     * @return the severity
     * @since 1.2.2
     */
    public Severity severity() {
        return severity;
    }

    /**
     * Returns a short human description of this rule.
     *
     * @return the rule description
     * @since 1.2.2
     */
    public String description() {
        return description;
    }

    /**
     * Resolves a rule by its identifier.
     *
     * @param id the rule id, for example {@code G011}
     * @return the rule, or {@code null} if no rule carries the id
     * @since 1.2.2
     */
    public static LintRule byId(String id) {
        for (LintRule rule : values()) {
            if (rule.id.equals(id)) {
                return rule;
            }
        }
        return null;
    }

    /** Violation severity. */
    public enum Severity {
        /** Fails the lint run. */
        ERROR,
        /** Reported but never fails the run on its own. */
        WARN
    }
}

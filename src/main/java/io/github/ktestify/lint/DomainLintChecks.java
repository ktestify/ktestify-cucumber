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

import io.cucumber.messages.types.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Semantic checks specific to the ktestify test style, run over the parsed
 * {@link io.cucumber.messages.types.GherkinDocument} AST.
 *
 * <p>Checks that:
 *
 * <ul>
 *   <li>file-name cells (values ending in a known extension) contain no whitespace ({@code D001})
 *   <li>every {@code topicAlias} / {@code schemaAlias} / {@code namespaceAlias} referenced in a Scenario is declared in
 *       the corresponding Background table ({@code D002})
 *   <li>every value in a {@code file} / {@code files} column exists in the feature's assets folder ({@code D003})
 * </ul>
 *
 * <p>Alias declaration is derived from the real step catalogue: the Background steps {@code namespace(s)}, {@code input
 * topic}, {@code output topic}, and {@code schema(s)}.
 *
 * @since 1.2.2
 */
final class DomainLintChecks {

    /** File-name cells: values ending in one of these extensions are treated as file paths. */
    private static final List<String> FILENAME_EXTENSIONS =
            List.of(".json", ".avro", ".txt", ".csv", ".xml", ".yaml", ".yml");

    /** Columns whose values are treated as referenced test files. */
    private static final Set<String> FILE_COLUMNS = Set.of("file", "files");

    /** Alias columns and the Background step texts that declare them. */
    private static final String TOPIC_ALIAS = "topicAlias";

    private static final String SCHEMA_ALIAS = "schemaAlias";
    private static final String NAMESPACE_ALIAS = "namespaceAlias";

    private DomainLintChecks() {}

    /**
     * Runs every domain check over the parsed feature.
     *
     * @param feature the parsed feature AST
     * @param featurePath path of the feature file on disk
     * @param options the lint options
     * @return the violations, sorted by position
     * @since 1.2.2
     */
    static List<LintViolation> check(Feature feature, Path featurePath, LintOptions options) {
        List<LintViolation> out = new ArrayList<>();
        if (!options.semantic()) {
            return out;
        }

        Path assetsDir = options.resolveAssetsDir(featurePath);
        boolean assetsPresent = Files.isDirectory(assetsDir);
        if (options.checkFiles() && !assetsPresent) {
            add(
                    out,
                    options,
                    new LintViolation(
                            LintRule.D004,
                            feature.getLocation().getLine(),
                            1,
                            "Assets directory not found: '" + assetsDir + "'. Skipping file-existence checks (D003)."));
        }

        List<Step> backgroundSteps = backgroundSteps(feature);
        List<Step> scenarioSteps = scenarioSteps(feature);

        checkFilenameCells(backgroundSteps, options, out);
        checkFilenameCells(scenarioSteps, options, out);
        if (options.checkFiles() && assetsPresent) {
            checkFileExistence(backgroundSteps, assetsDir, options, out);
            checkFileExistence(scenarioSteps, assetsDir, options, out);
        }

        checkAliases(backgroundSteps, scenarioSteps, options, out);
        return out;
    }

    // ---- D001: no whitespace inside filename-looking cells ---------------------------------

    private static void checkFilenameCells(List<Step> steps, LintOptions options, List<LintViolation> out) {
        for (Step step : steps) {
            Optional<DataTable> tableOpt = step.getDataTable();
            if (tableOpt.isEmpty()) {
                continue;
            }
            List<TableRow> rows = tableOpt.get().getRows();
            if (rows.isEmpty()) {
                continue;
            }
            for (TableRow row : rows.subList(1, rows.size())) {
                for (TableCell cell : row.getCells()) {
                    String value = cell.getValue();
                    if (isFilename(value) && containsWhitespace(value)) {
                        add(
                                out,
                                options,
                                new LintViolation(
                                        LintRule.D001,
                                        row.getLocation().getLine(),
                                        1,
                                        "File value contains whitespace: '" + value + "'."));
                    }
                }
            }
        }
    }

    // ---- D003: referenced files must exist in the assets folder ----------------------------

    private static void checkFileExistence(
            List<Step> steps, Path assetsDir, LintOptions options, List<LintViolation> out) {
        for (Step step : steps) {
            Optional<DataTable> tableOpt = step.getDataTable();
            if (tableOpt.isEmpty() || tableOpt.get().getRows().isEmpty()) {
                continue;
            }
            List<TableRow> rows = tableOpt.get().getRows();
            List<TableCell> header = rows.getFirst().getCells();
            for (TableRow row : rows.subList(1, rows.size())) {
                List<TableCell> cells = row.getCells();
                for (int col = 0; col < header.size() && col < cells.size(); col++) {
                    String colName = header.get(col).getValue().toLowerCase();
                    String value = cells.get(col).getValue();
                    if (!FILE_COLUMNS.contains(colName) || value.isBlank()) {
                        continue;
                    }
                    List<String> names = "files".equals(colName) ? splitList(value) : List.of(value);
                    for (String name : names) {
                        if (name.isBlank() || containsWhitespace(name)) {
                            continue; // malformed name: reported by D001
                        }
                        if (!Files.isRegularFile(assetsDir.resolve(name))) {
                            add(
                                    out,
                                    options,
                                    new LintViolation(
                                            LintRule.D003,
                                            row.getLocation().getLine(),
                                            1,
                                            "Referenced file '" + name + "' (column '" + colName
                                                    + "') is missing from assets folder '" + assetsDir + "'."));
                        }
                    }
                }
            }
        }
    }

    // ---- D002: aliases used in Scenarios must be declared in Background ---------------------

    private static void checkAliases(
            List<Step> backgroundSteps, List<Step> scenarioSteps, LintOptions options, List<LintViolation> out) {
        Set<String> declaredTopics = new HashSet<>();
        Set<String> declaredSchemas = new HashSet<>();
        Set<String> declaredNamespaces = new HashSet<>();
        for (Step step : backgroundSteps) {
            String text = step.getText().toLowerCase();
            if (text.startsWith("input topic") || text.startsWith("output topic")) {
                declaredTopics.addAll(columnValues(step, TOPIC_ALIAS));
            } else if (text.startsWith("schema")) {
                declaredSchemas.addAll(columnValues(step, SCHEMA_ALIAS));
            } else if (text.startsWith("namespace")) {
                declaredNamespaces.addAll(columnValues(step, NAMESPACE_ALIAS));
            }
        }

        for (Step step : scenarioSteps) {
            checkAliasColumn(step, TOPIC_ALIAS, declaredTopics, "input/output topic", options, out);
            checkAliasColumn(step, SCHEMA_ALIAS, declaredSchemas, "schema", options, out);
            checkAliasColumn(step, NAMESPACE_ALIAS, declaredNamespaces, "namespace", options, out);
        }
    }

    private static void checkAliasColumn(
            Step step,
            String column,
            Set<String> declared,
            String source,
            LintOptions options,
            List<LintViolation> out) {
        Optional<DataTable> tableOpt = step.getDataTable();
        if (tableOpt.isEmpty() || tableOpt.get().getRows().isEmpty()) {
            return;
        }
        List<TableRow> rows = tableOpt.get().getRows();
        List<TableCell> header = rows.getFirst().getCells();
        for (TableRow row : rows.subList(1, rows.size())) {
            List<TableCell> cells = row.getCells();
            for (int col = 0; col < header.size() && col < cells.size(); col++) {
                if (!header.get(col).getValue().equals(column)) {
                    continue;
                }
                String value = cells.get(col).getValue();
                if (!value.isBlank() && !declared.contains(value)) {
                    add(
                            out,
                            options,
                            new LintViolation(
                                    LintRule.D002,
                                    row.getLocation().getLine(),
                                    1,
                                    "'" + value + "' used as " + column + " is not declared in any Background " + source
                                            + " table."));
                }
            }
        }
    }

    // ---- helpers ---------------------------------------------------------------------------

    private static List<String> columnValues(Step step, String column) {
        Optional<DataTable> tableOpt = step.getDataTable();
        if (tableOpt.isEmpty() || tableOpt.get().getRows().isEmpty()) {
            return List.of();
        }
        List<TableRow> rows = tableOpt.get().getRows();
        List<TableCell> header = rows.getFirst().getCells();
        List<String> values = new ArrayList<>();
        for (TableRow row : rows.subList(1, rows.size())) {
            List<TableCell> cells = row.getCells();
            for (int col = 0; col < header.size() && col < cells.size(); col++) {
                if (header.get(col).getValue().equals(column)) {
                    values.add(cells.get(col).getValue());
                }
            }
        }
        return values;
    }

    private static List<Step> backgroundSteps(Feature feature) {
        List<Step> steps = new ArrayList<>();
        for (FeatureChild child : feature.getChildren()) {
            Optional<Background> background = child.getBackground();
            background.ifPresent(value -> steps.addAll(value.getSteps()));
        }
        return steps;
    }

    private static List<Step> scenarioSteps(Feature feature) {
        List<Step> steps = new ArrayList<>();
        for (FeatureChild child : feature.getChildren()) {
            child.getRule()
                    .ifPresent(rule -> rule.getChildren().forEach(rc -> {
                        rc.getBackground().ifPresent(b -> steps.addAll(b.getSteps()));
                        rc.getScenario().ifPresent(s -> steps.addAll(s.getSteps()));
                    }));
            child.getScenario().ifPresent(scenario -> steps.addAll(scenario.getSteps()));
        }
        return steps;
    }

    private static boolean isFilename(String value) {
        String lower = value.toLowerCase();
        return FILENAME_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    private static boolean containsWhitespace(String value) {
        return value.chars().anyMatch(c -> c == ' ' || c == '\t');
    }

    private static List<String> splitList(String value) {
        return java.util.Arrays.stream(value.split(",")).map(String::trim).toList();
    }

    private static void add(List<LintViolation> out, LintOptions options, LintViolation violation) {
        if (!options.isDisabled(violation.rule())) {
            out.add(violation);
        }
    }
}

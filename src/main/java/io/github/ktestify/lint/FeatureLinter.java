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

import io.cucumber.messages.types.Feature;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Lints {@code .feature} files.
 *
 * <p>Combines three passes:
 *
 * <ol>
 *   <li>{@link GherkinDocumentParser} — Cucumber's own parser; every parse error becomes a violation, so a file
 *       Cucumber cannot run always fails the lint run
 *   <li>{@link StructureLintChecks} — raw-line formatting rules ({@code G001}–{@code G013})
 *   <li>{@link DomainLintChecks} — ktestify-specific semantic rules ({@code D001}–{@code D004}) over the parsed AST
 * </ol>
 *
 * <p>Structural AST rules ({@code G004}–{@code G017}) are covered by the parser itself: a file that parses cleanly has
 * exactly one named Feature, named scenarios, well-formed tables with consistent column counts, and no steps outside a
 * Background or Scenario.
 *
 * @since 1.2.2
 */
public final class FeatureLinter {

    private final LintOptions options;

    /**
     * Creates a linter with the given options.
     *
     * @param options the lint options
     * @since 1.2.2
     */
    public FeatureLinter(LintOptions options) {
        this.options = options;
    }

    /**
     * Lints a single feature file.
     *
     * @param featurePath path to the {@code .feature} file
     * @return the violations, sorted by position, with disabled rules filtered out
     * @throws IOException when the file cannot be read
     * @since 1.2.2
     */
    public List<LintViolation> lint(Path featurePath) throws IOException {
        List<LintViolation> violations = new ArrayList<>();

        String text = Files.readString(featurePath, StandardCharsets.UTF_8);
        violations.addAll(StructureLintChecks.check(text, options));

        GherkinDocumentParser.ParseResult parse = GherkinDocumentParser.parse(featurePath);
        violations.addAll(parse.violations());

        if (parse.document().isPresent() && parse.document().get().getFeature().isPresent()) {
            Feature feature = parse.document().get().getFeature().get();
            violations.addAll(DomainLintChecks.check(feature, featurePath, options));
        }

        return violations.stream()
                .filter(v -> !options.isDisabled(v.rule()))
                .sorted(Comparator.comparingInt(LintViolation::line).thenComparingInt(LintViolation::column))
                .toList();
    }

    /**
     * Lints every {@code .feature} file under the given paths, recursing into directories.
     *
     * @param paths feature files or directories to lint
     * @return the violations of every file, sorted by position
     * @throws IOException when a file cannot be read
     * @since 1.2.2
     */
    public List<LintViolation> lintAll(List<Path> paths) throws IOException {
        List<LintViolation> violations = new ArrayList<>();
        for (Path path : paths) {
            if (Files.isDirectory(path)) {
                try (Stream<Path> stream = Files.walk(path)) {
                    List<Path> features = stream.filter(p -> p.toString().endsWith(".feature"))
                            .filter(Files::isRegularFile)
                            .sorted()
                            .toList();
                    for (Path feature : features) {
                        violations.addAll(lint(feature));
                    }
                }
            } else {
                violations.addAll(lint(path));
            }
        }
        return violations;
    }
}

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
import io.cucumber.messages.types.FeatureChild;
import io.cucumber.messages.types.RuleChild;
import io.cucumber.messages.types.Tag;
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
 *   <li>{@link GherkinDocumentParser}: Cucumber's own parser; every parse error becomes a violation, so a file Cucumber
 *       cannot run always fails the lint run
 *   <li>{@link StructureLintChecks}: raw-line formatting rules ({@code G001} to {@code G013})
 *   <li>{@link DomainLintChecks}: ktestify-specific semantic rules ({@code D001} to {@code D005}) over the parsed AST
 * </ol>
 *
 * <p>Structural AST rules ({@code G004} to {@code G017}) are covered by the parser itself: a file that parses cleanly
 * has exactly one named Feature, named scenarios, well-formed tables with consistent column counts, and no steps
 * outside a Background or Scenario.
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

        String text = Files.readString(featurePath, StandardCharsets.UTF_8);
        List<LintViolation> violations = new ArrayList<>(StructureLintChecks.check(text, options));

        GherkinDocumentParser.ParseResult parse = GherkinDocumentParser.parse(featurePath);
        violations.addAll(parse.violations());

        List<int[]> ignoredRanges = List.of();
        if (parse.document().isPresent() && parse.document().get().getFeature().isPresent()) {
            Feature feature = parse.document().get().getFeature().get();
            if (options.isIgnored(tagNames(feature.getTags()))) {
                return List.of();
            }
            violations.addAll(DomainLintChecks.check(feature, featurePath, options));
            ignoredRanges = ignoredScenarioRanges(feature);
        }
        List<int[]> ranges = ignoredRanges;

        return violations.stream()
                .filter(v -> ranges.stream().noneMatch(r -> v.line() >= r[0] && v.line() <= r[1]))
                .filter(v -> !options.isDisabled(v.rule()))
                .sorted(Comparator.comparingInt(LintViolation::line).thenComparingInt(LintViolation::column))
                .toList();
    }

    private static List<String> tagNames(List<Tag> tags) {
        return tags.stream().map(Tag::getName).toList();
    }

    /** Line ranges {@code [first, last]} of scenarios (or rules) that carry an ignored tag, tag lines included. */
    private List<int[]> ignoredScenarioRanges(Feature feature) {
        if (options.ignoredTags().isEmpty()) {
            return List.of();
        }
        // Flatten children into (startLine, ignored) blocks in document order.
        List<int[]> blocks = new ArrayList<>();
        for (FeatureChild child : feature.getChildren()) {
            if (child.getScenario().isPresent()) {
                var scenario = child.getScenario().get();
                blocks.add(block(scenario.getTags(), scenario.getLocation().getLine(), false));
            } else if (child.getRule().isPresent()) {
                var rule = child.getRule().get();
                boolean ruleIgnored = options.isIgnored(tagNames(rule.getTags()));
                blocks.add(block(rule.getTags(), rule.getLocation().getLine(), ruleIgnored));
                for (RuleChild rc : rule.getChildren()) {
                    if (rc.getScenario().isPresent()) {
                        var scenario = rc.getScenario().get();
                        blocks.add(
                                block(scenario.getTags(), scenario.getLocation().getLine(), ruleIgnored));
                    }
                }
            } else if (child.getBackground().isPresent()) {
                blocks.add(new int[] {child.getBackground().get().getLocation().getLine(), 0});
            }
        }
        List<int[]> ranges = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i)[1] == 1) {
                int end = i + 1 < blocks.size() ? blocks.get(i + 1)[0] - 1 : Integer.MAX_VALUE;
                ranges.add(new int[] {blocks.get(i)[0], end});
            }
        }
        return ranges;
    }

    private int[] block(List<Tag> tags, int keywordLine, boolean inheritedIgnored) {
        int start = tags.stream().mapToInt(t -> t.getLocation().getLine()).min().orElse(keywordLine);
        boolean ignored = inheritedIgnored || options.isIgnored(tagNames(tags));
        return new int[] {Math.min(start, keywordLine), ignored ? 1 : 0};
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

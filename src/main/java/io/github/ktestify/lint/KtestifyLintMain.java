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

import io.github.ktestify.lint.LintReportFormatter.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CLI entry point for feature-file linting.
 *
 * <p>Usage:
 *
 * <pre>
 * java -cp ktestify-cucumber.jar io.github.ktestify.lint.KtestifyLintMain features/
 * java -cp ktestify-cucumber.jar io.github.ktestify.lint.KtestifyLintMain features/ --format json
 * java -cp ktestify-cucumber.jar io.github.ktestify.lint.KtestifyLintMain features/ --format codeclimate
 * java -cp ktestify-cucumber.jar io.github.ktestify.lint.KtestifyLintMain features/ --no-semantic
 * java -cp ktestify-cucumber.jar io.github.ktestify.lint.KtestifyLintMain features/ --no-file-check
 * java -cp ktestify-cucumber.jar io.github.ktestify.lint.KtestifyLintMain features/ --assets-subdir data
 * java -cp ktestify-cucumber.jar io.github.ktestify.lint.KtestifyLintMain features/ --assets-dir /abs/path
 * java -cp ktestify-cucumber.jar io.github.ktestify.lint.KtestifyLintMain features/ --disable G013,G001
 * </pre>
 *
 * <p>Exit code is {@code 0} when there are no ERROR-severity violations, {@code 1} otherwise (also when warnings exceed
 * {@code --max-warnings}), and {@code 2} for usage errors.
 *
 * @since 1.2.2
 */
public final class KtestifyLintMain {

    private static final Logger LOG = LoggerFactory.getLogger(KtestifyLintMain.class);

    private KtestifyLintMain() {}

    /**
     * CLI entry point.
     *
     * @param args raw CLI arguments
     * @return the exit code
     * @since 1.2.2
     */
    public static int entrypoint(String[] args) {
        if (args == null || args.length == 0) {
            usage();
            return 2;
        }

        List<Path> paths = new ArrayList<>();
        Format format = Format.TEXT;
        boolean semantic = true;
        boolean checkFiles = true;
        String assetsSubdir = LintOptions.DEFAULT_ASSETS_SUBDIR;
        Path assetsDir = null;
        Set<String> disabled = Set.of();
        long maxWarnings = -1;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--format" -> {
                    if (++i >= args.length) {
                        return usageError("--format requires a value: text, json, or codeclimate.");
                    }
                    try {
                        format = Format.valueOf(args[i].toUpperCase());
                    } catch (IllegalArgumentException e) {
                        return usageError("Unknown format '" + args[i] + "': use text, json, or codeclimate.");
                    }
                }
                case "--no-semantic" -> semantic = false;
                case "--no-file-check" -> checkFiles = false;
                case "--assets-subdir" -> {
                    if (++i >= args.length) {
                        return usageError("--assets-subdir requires a value.");
                    }
                    assetsSubdir = args[i];
                }
                case "--assets-dir" -> {
                    if (++i >= args.length) {
                        return usageError("--assets-dir requires a value.");
                    }
                    assetsDir = Path.of(args[i]);
                }
                case "--disable" -> {
                    if (++i >= args.length) {
                        return usageError("--disable requires a comma-separated list of rule ids.");
                    }
                    disabled = java.util.Arrays.stream(args[i].split(","))
                            .map(String::trim)
                            .filter(s -> !s.isBlank())
                            .collect(Collectors.toUnmodifiableSet());
                }
                case "--max-warnings" -> {
                    if (++i >= args.length) {
                        return usageError("--max-warnings requires a value.");
                    }
                    try {
                        maxWarnings = Long.parseLong(args[i]);
                    } catch (NumberFormatException e) {
                        return usageError("--max-warnings requires an integer.");
                    }
                }
                case "--help", "-h" -> {
                    usage();
                    return 0;
                }
                default -> {
                    if (arg.startsWith("--")) {
                        return usageError("Unknown option '" + arg + "'.");
                    }
                    Path path = Path.of(arg);
                    if (!Files.exists(path)) {
                        System.err.println("File or directory not found: " + path);
                        return 2;
                    }
                    paths.add(path);
                }
            }
        }

        if (paths.isEmpty()) {
            usage();
            return 2;
        }

        LintOptions options = new LintOptions(semantic, checkFiles, assetsSubdir, assetsDir, disabled);
        FeatureLinter linter = new FeatureLinter(options);

        Map<Path, List<LintViolation>> results = new LinkedHashMap<>();
        try {
            for (Path path : paths) {
                if (Files.isDirectory(path)) {
                    try (var stream = Files.walk(path)) {
                        List<Path> features = stream.filter(p -> p.toString().endsWith(".feature"))
                                .filter(Files::isRegularFile)
                                .sorted()
                                .toList();
                        for (Path feature : features) {
                            results.put(feature, linter.lint(feature));
                        }
                    }
                } else {
                    results.put(path, linter.lint(path));
                }
            }
        } catch (IOException e) {
            LOG.error("Lint run failed: {}", e.getMessage(), e);
            return 2;
        }

        System.out.println(LintReportFormatter.format(format, results));

        long errors = results.values().stream()
                .flatMap(List::stream)
                .filter(v -> v.severity() == LintRule.Severity.ERROR)
                .count();
        long warnings = results.values().stream()
                .flatMap(List::stream)
                .filter(v -> v.severity() == LintRule.Severity.WARN)
                .count();
        if (errors > 0) {
            return 1;
        }
        if (maxWarnings >= 0 && warnings > maxWarnings) {
            return 1;
        }
        return 0;
    }

    /**
     * CLI entry point; terminates the JVM with the exit code.
     *
     * @param args raw CLI arguments
     * @since 1.2.2
     */
    static void main(String[] args) {
        System.exit(entrypoint(args));
    }

    private static void usage() {
        System.out.println("""
                Usage: KtestifyLintMain <paths> [options]

                  paths                     feature files or directories (recursed for *.feature)

                Options:
                  --format <fmt>            text (default), json, or codeclimate (GitLab Code Quality)
                  --no-semantic             skip the D-rules (alias and file checks)
                  --no-file-check           skip on-disk asset file-existence checks (D003/D004)
                  --assets-subdir <name>    assets folder beside each feature file (default: assets)
                  --assets-dir <path>       absolute assets folder, overrides --assets-subdir
                  --disable <ids>           comma-separated rule ids to disable, e.g. G013,G001
                  --max-warnings <n>        fail (exit 1) if warnings exceed this count; -1 = never
                  --help                    show this help
                """);
    }

    private static int usageError(String message) {
        System.err.println(message);
        usage();
        return 2;
    }
}

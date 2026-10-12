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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Renders lint results as text, JSON, or GitLab Code Quality (Code Climate) JSON.
 *
 * @since 1.2.2
 */
public final class LintReportFormatter {

    /** Output format of a lint report. */
    public enum Format {
        /** Human-readable ANSI-coloured console output. */
        TEXT,
        /** Plain JSON object keyed by file path. */
        JSON,
        /** GitLab Code Quality JSON array (for {@code artifacts:reports:codequality}). */
        CODECLIMATE
    }

    private static final String ANSI_RESET = "\033[0m";
    private static final String ANSI_RED = "\033[31m";
    private static final String ANSI_YELLOW = "\033[33m";
    private static final String ANSI_GREEN = "\033[32m";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private LintReportFormatter() {}

    /**
     * Formats a lint result.
     *
     * @param format the output format
     * @param results violations per feature file
     * @return the rendered report
     * @since 1.2.2
     */
    public static String format(Format format, Map<Path, List<LintViolation>> results) {
        return switch (format) {
            case TEXT -> formatText(results);
            case JSON -> formatJson(results);
            case CODECLIMATE -> formatCodeclimate(results);
        };
    }

    private static String formatText(Map<Path, List<LintViolation>> results) {
        StringBuilder sb = new StringBuilder();
        int errors = 0;
        int warnings = 0;
        for (Map.Entry<Path, List<LintViolation>> entry : results.entrySet()) {
            if (entry.getValue().isEmpty()) {
                sb.append(ANSI_GREEN)
                        .append("✓")
                        .append(ANSI_RESET)
                        .append(' ')
                        .append(entry.getKey())
                        .append(": no problems\n");
                continue;
            }
            sb.append('\n').append(entry.getKey()).append('\n');
            for (LintViolation v : entry.getValue()) {
                String colour = v.severity() == LintRule.Severity.ERROR ? ANSI_RED : ANSI_YELLOW;
                sb.append("  ")
                        .append(v.line())
                        .append(':')
                        .append(v.column())
                        .append("  ")
                        .append(colour)
                        .append(v.severity() == LintRule.Severity.ERROR ? "error" : "warn")
                        .append(ANSI_RESET)
                        .append("  ")
                        .append(v.rule().id())
                        .append("  ")
                        .append(v.message())
                        .append('\n');
                if (v.severity() == LintRule.Severity.ERROR) {
                    errors++;
                } else {
                    warnings++;
                }
            }
        }
        String summaryColour = errors > 0 ? ANSI_RED : (warnings > 0 ? ANSI_YELLOW : ANSI_GREEN);
        sb.append('\n')
                .append(summaryColour)
                .append(errors)
                .append(" error(s), ")
                .append(warnings)
                .append(" warning(s)")
                .append(ANSI_RESET)
                .append('\n');
        return sb.toString();
    }

    private static String formatJson(Map<Path, List<LintViolation>> results) {
        ObjectNode root = MAPPER.createObjectNode();
        results.forEach((path, violations) -> {
            ArrayNode array = root.putArray(path.toString());
            for (LintViolation v : violations) {
                ObjectNode node = array.addObject();
                node.put("rule", v.rule().id());
                node.put("severity", v.severity() == LintRule.Severity.ERROR ? "error" : "warn");
                node.put("line", v.line());
                node.put("column", v.column());
                node.put("message", v.message());
            }
        });
        return write(root);
    }

    /**
     * Emits the GitLab Code Quality (Code Climate) JSON array.
     *
     * <p>Pipe this to a file and expose it as {@code artifacts:reports:codequality}.
     */
    private static String formatCodeclimate(Map<Path, List<LintViolation>> results) {
        ArrayNode issues = MAPPER.createArrayNode();
        results.forEach((path, violations) -> {
            for (LintViolation v : violations) {
                ObjectNode issue = issues.addObject();
                issue.put("description", "[" + v.rule().id() + "] " + v.message());
                issue.put("check_name", v.rule().id());
                issue.put("fingerprint", fingerprint(path, v));
                issue.put("severity", v.severity() == LintRule.Severity.ERROR ? "major" : "minor");
                ObjectNode location = issue.putObject("location");
                location.put("path", path.toString());
                location.putObject("lines").put("begin", v.line());
            }
        });
        return write(issues);
    }

    private static String fingerprint(Path path, LintViolation v) {
        String raw = path + ":" + v.rule().id() + ":" + v.line() + ":" + v.column() + ":" + v.message();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(raw.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 is not available.", e);
        }
    }

    private static String write(Object json) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (java.io.IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

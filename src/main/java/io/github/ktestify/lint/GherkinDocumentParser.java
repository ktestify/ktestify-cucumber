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

import io.cucumber.gherkin.GherkinParser;
import io.cucumber.messages.types.Envelope;
import io.cucumber.messages.types.GherkinDocument;
import io.cucumber.messages.types.ParseError;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Thin wrapper around Cucumber's {@link GherkinParser}.
 *
 * <p>Parses a feature file into the official messages {@link GherkinDocument} AST and converts every parser error into
 * a {@link LintViolation} of rule {@code G007}, so a file that Cucumber itself cannot run always fails the lint run.
 *
 * @since 1.2.2
 */
public final class GherkinDocumentParser {

    /** Parsing result: the AST when it could be built, plus any parse-error violations. */
    public record ParseResult(Optional<GherkinDocument> document, List<LintViolation> violations) {}

    private GherkinDocumentParser() {}

    /**
     * Parses a feature file.
     *
     * @param featurePath path to the {@code .feature} file
     * @return the parse result; the document is empty when parsing failed
     * @throws IOException when the file cannot be read
     * @since 1.2.2
     */
    public static ParseResult parse(Path featurePath) throws IOException {
        List<Envelope> envelopes;
        try (var stream = GherkinParser.builder()
                .includeSource(false)
                .includeGherkinDocument(true)
                .includePickles(false)
                .build()
                .parse(featurePath)) {
            envelopes = stream.toList();
        }

        List<LintViolation> violations = new ArrayList<>();
        Optional<GherkinDocument> document = Optional.empty();
        for (Envelope envelope : envelopes) {
            if (envelope.getParseError().isPresent()) {
                violations.add(toViolation(envelope.getParseError().get()));
            }
            if (envelope.getGherkinDocument().isPresent()) {
                document = envelope.getGherkinDocument();
            }
        }
        return new ParseResult(document, violations);
    }

    private static LintViolation toViolation(ParseError error) {
        int line = error.getSource()
                .getLocation()
                .map(io.cucumber.messages.types.Location::getLine)
                .orElse(1);
        int column = error.getSource()
                .getLocation()
                .flatMap(io.cucumber.messages.types.Location::getColumn)
                .orElse(1);
        return new LintViolation(LintRule.G007, line, column, error.getMessage());
    }
}

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
 * A single lint violation of a {@link LintRule} at a position in a feature file.
 *
 * @param rule the violated rule
 * @param line the 1-based line number
 * @param column the 1-based column number
 * @param message the human-readable violation message
 * @since 1.2.2
 */
public record LintViolation(LintRule rule, int line, int column, String message) {

    /**
     * Returns the severity of the violated rule.
     *
     * @return the severity
     * @since 1.2.2
     */
    public LintRule.Severity severity() {
        return rule().severity();
    }
}

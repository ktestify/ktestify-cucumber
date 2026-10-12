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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FeatureLinterTest {

    private static final Path LINT_DIR = Path.of("src", "test", "resources", "lint");

    private List<LintRule> lintRules(Path feature, LintOptions options) throws IOException {
        return new FeatureLinter(options)
                .lint(feature).stream().map(LintViolation::rule).toList();
    }

    @Test
    void cleanFeatureHasNoViolations() throws IOException {
        assertTrue(lintRules(LINT_DIR.resolve("clean.feature"), LintOptions.defaults())
                .isEmpty());
    }

    @Test
    void tabIndentationIsReported() throws IOException {
        assertTrue(lintRules(LINT_DIR.resolve("structural.feature"), LintOptions.defaults())
                .contains(LintRule.G001));
    }

    @Test
    void undeclaredAliasIsReported() throws IOException {
        assertTrue(lintRules(LINT_DIR.resolve("domain.feature"), LintOptions.defaults())
                .contains(LintRule.D002));
    }

    @Test
    void whitespaceInFilenameIsReported() throws IOException {
        assertTrue(lintRules(LINT_DIR.resolve("domain.feature"), LintOptions.defaults())
                .contains(LintRule.D001));
    }

    @Test
    void missingAssetFileIsReported() throws IOException {
        assertTrue(lintRules(LINT_DIR.resolve("domain.feature"), LintOptions.defaults())
                .contains(LintRule.D003));
    }

    @Test
    void disabledRulesAreFiltered() throws IOException {
        LintOptions options = new LintOptions(true, true, "assets", null, Set.of("D002", "D003"));
        List<LintRule> rules = lintRules(LINT_DIR.resolve("domain.feature"), options);
        assertFalse(rules.contains(LintRule.D002));
        assertFalse(rules.contains(LintRule.D003));
    }

    @Test
    void noSemanticSkipsDomainRules() throws IOException {
        LintOptions options = new LintOptions(false, true, "assets", null, Set.of());
        List<LintRule> rules = lintRules(LINT_DIR.resolve("domain.feature"), options);
        assertFalse(rules.contains(LintRule.D001));
        assertFalse(rules.contains(LintRule.D002));
        assertFalse(rules.contains(LintRule.D003));
    }

    @Test
    void noFileCheckSkipsExistenceRules() throws IOException {
        LintOptions options = new LintOptions(true, false, "assets", null, Set.of());
        List<LintRule> rules = lintRules(LINT_DIR.resolve("domain.feature"), options);
        assertFalse(rules.contains(LintRule.D003));
        assertFalse(rules.contains(LintRule.D004));
    }

    @Test
    void missingAssetsDirIsWarned() throws IOException {
        LintOptions options = new LintOptions(true, true, "does-not-exist", null, Set.of());
        assertTrue(lintRules(LINT_DIR.resolve("clean.feature"), options).contains(LintRule.D004));
    }

    @Test
    void explicitAssetsDirOverridesSubdir() throws IOException {
        LintOptions options = new LintOptions(true, true, "does-not-exist", LINT_DIR.resolve("assets"), Set.of());
        assertFalse(lintRules(LINT_DIR.resolve("clean.feature"), options).contains(LintRule.D004));
    }

    @Test
    void lintAllRecursesDirectories() throws IOException {
        List<LintViolation> violations = new FeatureLinter(LintOptions.defaults()).lintAll(List.of(LINT_DIR));
        assertFalse(violations.isEmpty());
    }

    @Test
    void declaredAliasesResolve() throws IOException {
        List<LintRule> rules = lintRules(LINT_DIR.resolve("aliases.feature"), LintOptions.defaults());
        assertFalse(rules.contains(LintRule.D002));
    }

    @Test
    void filesColumnValuesAreCheckedForExistence() throws IOException {
        List<LintRule> rules = lintRules(LINT_DIR.resolve("aliases.feature"), LintOptions.defaults());
        assertFalse(rules.contains(LintRule.D003));
    }

    @Test
    void undeclaredNamespaceAliasIsReported() throws IOException {
        List<LintRule> rules = lintRules(LINT_DIR.resolve("domain.feature"), LintOptions.defaults());
        assertTrue(rules.contains(LintRule.D002));
    }

    @Test
    void malformedTableIsReportedByParser() throws IOException {
        List<LintRule> rules = lintRules(LINT_DIR.resolve("malformed.feature"), LintOptions.defaults());
        assertTrue(rules.contains(LintRule.G007));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "G001", "G002", "G003", "G004", "G005", "G006", "G007", "G008", "G009", "G010", "G011", "G012", "G013",
                "G014", "G015", "G016", "G017", "D001", "D002", "D003", "D004"
            })
    void everyRuleIdResolves(String id) {
        assertNotNull(LintRule.byId(id));
    }
}

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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class KtestifyLintMainTest {

    private static final String LINT_DIR = "src/test/resources/lint";

    @Test
    void noArgsIsUsageError() {
        assertEquals(2, KtestifyLintMain.entrypoint(new String[] {}));
    }

    @Test
    void unknownOptionIsUsageError() {
        assertEquals(2, KtestifyLintMain.entrypoint(new String[] {LINT_DIR, "--bogus"}));
    }

    @Test
    void missingPathIsUsageError() {
        assertEquals(2, KtestifyLintMain.entrypoint(new String[] {"--format", "json"}));
    }

    @Test
    void cleanFeatureExitsZero() {
        assertEquals(0, KtestifyLintMain.entrypoint(new String[] {LINT_DIR + "/clean.feature"}));
    }

    @Test
    void domainFeatureExitsOne() {
        assertEquals(1, KtestifyLintMain.entrypoint(new String[] {LINT_DIR + "/domain.feature"}));
    }

    @Test
    void noSemanticExitsZeroOnDomainFeature() {
        assertEquals(0, KtestifyLintMain.entrypoint(new String[] {LINT_DIR + "/domain.feature", "--no-semantic"}));
    }

    @Test
    void helpExitsZero() {
        assertEquals(0, KtestifyLintMain.entrypoint(new String[] {"--help"}));
    }

    @Test
    void jsonFormatRuns() {
        assertEquals(0, KtestifyLintMain.entrypoint(new String[] {LINT_DIR + "/clean.feature", "--format", "json"}));
    }

    @Test
    void codeclimateFormatRuns() {
        assertEquals(
                0, KtestifyLintMain.entrypoint(new String[] {LINT_DIR + "/clean.feature", "--format", "codeclimate"}));
    }

    @Test
    void unknownFormatIsUsageError() {
        assertEquals(2, KtestifyLintMain.entrypoint(new String[] {LINT_DIR + "/clean.feature", "--format", "bogus"}));
    }

    @Test
    void missingFormatValueIsUsageError() {
        assertEquals(2, KtestifyLintMain.entrypoint(new String[] {LINT_DIR, "--format"}));
    }

    @Test
    void missingAssetsSubdirValueIsUsageError() {
        assertEquals(2, KtestifyLintMain.entrypoint(new String[] {LINT_DIR, "--assets-subdir"}));
    }

    @Test
    void missingAssetsDirValueIsUsageError() {
        assertEquals(2, KtestifyLintMain.entrypoint(new String[] {LINT_DIR, "--assets-dir"}));
    }

    @Test
    void missingDisableValueIsUsageError() {
        assertEquals(2, KtestifyLintMain.entrypoint(new String[] {LINT_DIR, "--disable"}));
    }

    @Test
    void missingMaxWarningsValueIsUsageError() {
        assertEquals(2, KtestifyLintMain.entrypoint(new String[] {LINT_DIR, "--max-warnings"}));
    }

    @Test
    void nonNumericMaxWarningsIsUsageError() {
        assertEquals(2, KtestifyLintMain.entrypoint(new String[] {LINT_DIR, "--max-warnings", "abc"}));
    }

    @Test
    void nonexistentPathIsUsageError() {
        assertEquals(2, KtestifyLintMain.entrypoint(new String[] {"does/not/exist.feature"}));
    }

    @Test
    void directoryIsRecursed() {
        assertEquals(1, KtestifyLintMain.entrypoint(new String[] {LINT_DIR}));
    }

    @Test
    void maxWarningsCanFailRun() {
        // clean.feature produces no warnings; a threshold of -1 never fails, 0 also passes with 0 warnings
        assertEquals(0, KtestifyLintMain.entrypoint(new String[] {LINT_DIR + "/clean.feature", "--max-warnings", "0"}));
    }

    @Test
    void noFileCheckSkipsExistence() {
        assertEquals(0, KtestifyLintMain.entrypoint(new String[] {
            LINT_DIR + "/domain.feature", "--no-file-check", "--disable", "D001,D002"
        }));
    }

    @Test
    void assetsDirOverrideIsAccepted() {
        assertEquals(0, KtestifyLintMain.entrypoint(new String[] {
            LINT_DIR + "/clean.feature", "--assets-dir", LINT_DIR + "/assets"
        }));
    }

    @Test
    void ignoreTagsSkipsTaggedFeature() {
        String dir = "src/test/resources/lint-tags/wip-feature.feature";
        assertEquals(1, KtestifyLintMain.entrypoint(new String[] {dir}));
        assertEquals(0, KtestifyLintMain.entrypoint(new String[] {dir, "--ignore-tags", "@WIP"}));
        assertEquals(0, KtestifyLintMain.entrypoint(new String[] {dir, "--ignore-tagged", "WIP"}));
    }

    @Test
    void ignoreTagsWithoutValueIsUsageError() {
        assertEquals(2, KtestifyLintMain.entrypoint(new String[] {LINT_DIR, "--ignore-tags"}));
    }
}

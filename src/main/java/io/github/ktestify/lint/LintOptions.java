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

import java.nio.file.Path;
import java.util.Set;

/**
 * Options that drive the {@link FeatureLinter}.
 *
 * @param semantic whether the domain rules ({@code D*}) run at all
 * @param checkFiles whether on-disk asset file-existence checks ({@code D003}/{@code D004}) run
 * @param assetsSubdir name of the assets folder beside each feature file; used when no explicit assets directory is set
 * @param assetsDir absolute assets directory override; applies to every feature file
 * @param disabled rule ids that are suppressed from every report
 * @since 1.2.2
 */
public record LintOptions(
        boolean semantic, boolean checkFiles, String assetsSubdir, Path assetsDir, Set<String> disabled) {

    /** Default name of the assets folder that sits next to each feature file. */
    public static final String DEFAULT_ASSETS_SUBDIR = "assets";

    /** Default options: semantic checks on, file checks on, default assets subdir, nothing disabled. */
    public static LintOptions defaults() {
        return new LintOptions(true, true, DEFAULT_ASSETS_SUBDIR, null, Set.of());
    }

    /**
     * Returns whether the given rule is suppressed.
     *
     * @param rule the rule to test
     * @return {@code true} if the rule is disabled
     * @since 1.2.2
     */
    public boolean isDisabled(LintRule rule) {
        return disabled().contains(rule.id());
    }

    /**
     * Resolves the assets directory for a given feature file.
     *
     * @param featurePath the feature file path
     * @return the assets directory to check referenced files against
     * @since 1.2.2
     */
    public Path resolveAssetsDir(Path featurePath) {
        if (assetsDir() != null) {
            return assetsDir();
        }
        return featurePath.toAbsolutePath().getParent().resolve(assetsSubdir());
    }
}

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
import java.util.stream.Collectors;

/**
 * Options that drive the {@link FeatureLinter}.
 *
 * @param semantic whether the domain rules ({@code D*}) run at all
 * @param checkFiles whether on-disk asset file-existence checks ({@code D003}/{@code D004}) run
 * @param assetsSubdir name of the assets folder beside each feature file; used when no explicit assets directory is set
 * @param assetsDir absolute assets directory override; applies to every feature file
 * @param disabled rule ids that are suppressed from every report
 * @param ignoredTags tags (with or without the leading {@code @}); a feature carrying one is skipped entirely, a
 *     scenario carrying one is excluded from the report
 * @since 1.2.2
 */
public record LintOptions(
        boolean semantic,
        boolean checkFiles,
        String assetsSubdir,
        Path assetsDir,
        Set<String> disabled,
        Set<String> ignoredTags) {

    /**
     * Creates options and normalizes the ignored tags to their {@code @}-prefixed form.
     *
     * @since 1.2.2
     */
    public LintOptions {
        ignoredTags = ignoredTags == null
                ? Set.of()
                : ignoredTags.stream()
                        .map(String::trim)
                        .filter(t -> !t.isEmpty() && !t.equals("@"))
                        .map(t -> t.startsWith("@") ? t : "@" + t)
                        .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Creates options with no ignored tags.
     *
     * @param semantic whether the domain rules run
     * @param checkFiles whether asset file-existence checks run
     * @param assetsSubdir assets folder name beside each feature file
     * @param assetsDir absolute assets directory override
     * @param disabled rule ids that are suppressed
     * @since 1.2.2
     */
    public LintOptions(
            boolean semantic, boolean checkFiles, String assetsSubdir, Path assetsDir, Set<String> disabled) {
        this(semantic, checkFiles, assetsSubdir, assetsDir, disabled, Set.of());
    }

    /** Default name of the assets folder that sits next to each feature file. */
    public static final String DEFAULT_ASSETS_SUBDIR = "assets";

    /** Default options: semantic checks on, file checks on, default assets subdir, nothing disabled. */
    public static LintOptions defaults() {
        return new LintOptions(true, true, DEFAULT_ASSETS_SUBDIR, null, Set.of(), Set.of());
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
     * Returns whether any of the given tag names is ignored.
     *
     * @param tagNames tag names as written in the feature file, including the leading {@code @}
     * @return {@code true} if at least one tag is in the ignored set
     * @since 1.2.2
     */
    public boolean isIgnored(java.util.Collection<String> tagNames) {
        return tagNames.stream().anyMatch(ignoredTags()::contains);
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

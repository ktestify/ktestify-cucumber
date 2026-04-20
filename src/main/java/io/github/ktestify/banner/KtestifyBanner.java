/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.github.ktestify.banner;

import io.github.ktestify.KtestifyMain;

/**
 * Prints the KTestify ASCII banner with version information to {@code System.out} when the test suite starts.
 *
 * <p>The version is resolved at build time via a Maven-filtered {@code version.properties} file bundled in the
 * classpath.
 */
public final class KtestifyBanner {

    // ── ANSI escape codes ────────────────────────────────────────────────────
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String CYAN = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";
    private static final String DIM = "\u001B[2m";

    /** KTESTIFY — ANSI Shadow block font */
    private static final String BANNER = """
             _  _______         _   _  __      \s
            | |/ /_   _|__  ___| |_(_)/ _|_   _\s
            | ' /  | |/ _ \\/ __| __| | |_| | | |
            | . \\  | |  __/\\__ \\ |_| |  _| |_| |
            |_|\\_\\ |_|\\___||___/\\__|_|_|  \\__, |
                                          |___/\s <version>
            """;

    private KtestifyBanner() {}

    /**
     * Prints the banner to {@code System.out}. Safe to call multiple times — output always goes to stdout regardless of
     * the logging configuration so the banner is never swallowed by log filters.
     */
    public static void print() {
        String version = resolveVersion();
        System.out.println(BOLD + BANNER.replace("version", YELLOW + "v " + version + RESET) + RESET);
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static String resolveVersion() {
        return KtestifyMain.class.getPackage().getImplementationVersion() != null
                ? KtestifyMain.class.getPackage().getImplementationVersion()
                : "dev";
    }
}

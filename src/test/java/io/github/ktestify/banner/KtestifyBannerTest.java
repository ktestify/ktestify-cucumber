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
package io.github.ktestify.banner;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("KtestifyBanner")
class KtestifyBannerTest {

    private PrintStream originalOut;
    private ByteArrayOutputStream captured;

    @BeforeEach
    void redirectStdout() {
        originalOut = System.out;
        captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured));
    }

    @AfterEach
    void restoreStdout() {
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("print() does not throw")
    void printDoesNotThrow() {
        assertDoesNotThrow(KtestifyBanner::print);
    }

    @Test
    @DisplayName("print() outputs the KTestify name to stdout")
    void printOutputsName() {
        KtestifyBanner.print();
        String output = captured.toString();
        // The ASCII art contains KTestify letters — check for the logo anchor
        assertTrue(
                output.contains("KTestify") || output.contains("_  _______"),
                "Expected banner text in output but got: " + output);
    }

    @Test
    @DisplayName("print() includes a version string")
    void printIncludesVersion() {
        KtestifyBanner.print();
        String output = captured.toString();
        // Version is either the MANIFEST value or the fallback "dev"
        assertTrue(
                output.contains("dev") || output.contains("SNAPSHOT") || output.matches("(?s).*\\d\\.\\d.*"),
                "Expected version token in banner output");
    }
}

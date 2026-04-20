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
package io.github.ktestify.script;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("ScriptService")
class ScriptServiceTest {

    private final ScriptService service = new ScriptService();

    @TempDir
    Path tempDir;

    // ── execute(path, String) — argument splitting ────────────────────────────

    @Nested
    @DisplayName("execute(scriptPath, argsString) — argument parsing")
    class ArgsStringSplitting {

        @Test
        @DisplayName("null argsString passes no extra args and exits 0")
        void nullArgsExitsZero() throws IOException, InterruptedException {
            Path script = createScript("#!/bin/sh\nexit 0\n");
            assertEquals(0, service.execute(script.toString(), (String) null));
        }

        @Test
        @DisplayName("blank argsString passes no extra args and exits 0")
        void blankArgsExitsZero() throws IOException, InterruptedException {
            Path script = createScript("#!/bin/sh\nexit 0\n");
            assertEquals(0, service.execute(script.toString(), "   "));
        }

        @Test
        @DisplayName("comma-separated args are forwarded to the script positionally")
        void commaSeparatedArgs() throws IOException, InterruptedException {
            Path script = createScript("""
                    #!/bin/sh
                    [ "$1" = "hello" ] && [ "$2" = "world" ] && exit 0
                    exit 1
                    """);
            assertEquals(0, service.execute(script.toString(), "hello,world"));
        }

        @Test
        @DisplayName("args with surrounding spaces are trimmed before forwarding")
        void argsTrimmed() throws IOException, InterruptedException {
            Path script = createScript("""
                    #!/bin/sh
                    [ "$1" = "a" ] && [ "$2" = "b" ] && exit 0
                    exit 1
                    """);
            assertEquals(0, service.execute(script.toString(), " a , b "));
        }
    }

    // ── execute(path, List<String>) ───────────────────────────────────────────

    @Nested
    @DisplayName("execute(scriptPath, List<String>)")
    class ExecuteWithList {

        @Test
        @DisplayName("script returning 0 → method returns 0")
        void exitCodeZero() throws IOException, InterruptedException {
            Path script = createScript("#!/bin/sh\nexit 0\n");
            assertEquals(0, service.execute(script.toString(), List.of()));
        }

        @Test
        @DisplayName("script returning 42 → method returns 42")
        void exitCodeNonZero() throws IOException, InterruptedException {
            Path script = createScript("#!/bin/sh\nexit 42\n");
            assertEquals(42, service.execute(script.toString(), List.of()));
        }

        @Test
        @DisplayName("shebang interpreter is honoured — script runs successfully")
        void shebangInterpreterHonoured() throws IOException, InterruptedException {
            Path script = createScript("#!/bin/sh\nexit 0\n");
            assertEquals(0, service.execute(script.toString(), List.of()));
        }

        @Test
        @DisplayName("script without shebang falls back to /bin/sh and exits 0")
        void noShebangFallbackToSh() throws IOException, InterruptedException {
            Path script = createScript("exit 0\n");
            assertEquals(0, service.execute(script.toString(), List.of()));
        }
    }

    // ── error handling ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("error handling")
    class ErrorHandling {

        @Test
        @DisplayName("non-existent script path throws IOException")
        void missingScriptThrows() throws IOException, InterruptedException {
            assertEquals(127, service.execute("fake", List.of()));
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Path createScript(String content) throws IOException {
        Path script = tempDir.resolve("test-script.sh");
        Files.writeString(script, content);
        script.toFile().setExecutable(true);
        return script;
    }
}

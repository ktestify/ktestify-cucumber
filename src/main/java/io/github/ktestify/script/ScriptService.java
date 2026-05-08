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
package io.github.ktestify.script;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Executes shell scripts in a subprocess.
 *
 * <p>The script's shebang line (first line starting with {@code #!}) is read to determine the interpreter. If no
 * shebang is present, {@code /bin/sh} is used as the default interpreter.
 *
 * <p>The subprocess inherits the parent process's standard IO streams (stdout and stderr are visible in the test
 * output). The exit code is returned to the caller.
 *
 * <p>Lives in {@code ktestify-cucumber} because it has no Kafka dependency and is specific to the Cucumber test
 * execution lifecycle.
 */
@Slf4j
public class ScriptService {

    private static final String DEFAULT_INTERPRETER = "/bin/sh";

    /**
     * Executes the script at {@code scriptPath} with the given {@code args}.
     *
     * @param scriptPath path to the script file
     * @param args optional arguments passed to the script (may be {@code null} or empty)
     * @return the process exit code ({@code 0} = success)
     * @throws IOException if the script cannot be read or the process cannot be started
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public int execute(String scriptPath, List<String> args) throws IOException, InterruptedException {
        String interpreter = resolveInterpreter(scriptPath);
        List<String> command = buildCommand(interpreter, scriptPath, args);
        log.info("Executing script: {}", command);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        Process process = pb.start();
        int exitCode = process.waitFor();
        log.info("Script '{}' exited with code {}", scriptPath, exitCode);
        return exitCode;
    }

    /**
     * Convenience overload that accepts a comma-separated args string (as it comes from a DataTable cell).
     *
     * @param scriptPath path to the script file
     * @param argsString comma-separated argument string; may be {@code null} or blank
     * @return the process exit code
     */
    public int execute(String scriptPath, String argsString) throws IOException, InterruptedException {
        List<String> args = (argsString == null || argsString.isBlank())
                ? List.of()
                : Arrays.stream(argsString.split(",")).map(String::trim).toList();
        return execute(scriptPath, args);
    }

    /**
     * Reads the first line of the script and extracts the interpreter from the shebang ({@code #!}). Falls back to
     * {@value #DEFAULT_INTERPRETER} if no shebang is present.
     */
    private String resolveInterpreter(String scriptPath) {
        File file = new File(scriptPath);
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String firstLine = reader.readLine();
            if (firstLine != null && firstLine.startsWith("#!")) {
                String interpreter = firstLine.substring(2).trim();
                log.debug("Resolved interpreter from shebang: '{}'", interpreter);
                return interpreter;
            }
        } catch (IOException e) {
            log.warn(
                    "Could not read shebang from '{}', using default interpreter: {}", scriptPath, DEFAULT_INTERPRETER);
        }
        return DEFAULT_INTERPRETER;
    }

    private List<String> buildCommand(String interpreter, String scriptPath, List<String> args) {
        List<String> command = new ArrayList<>();
        command.add(interpreter);
        command.add(scriptPath);
        if (args != null) {
            command.addAll(args);
        }
        return command;
    }
}

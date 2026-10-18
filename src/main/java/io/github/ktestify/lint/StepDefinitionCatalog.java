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

import io.cucumber.java.en.*;
import io.github.ktestify.plugin.KtestifyPlugin;
import io.github.ktestify.steps.ActionStepDefinition;
import io.github.ktestify.steps.BackgroundStepDefinition;
import io.github.ktestify.steps.ValidationStepDefinition;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Discovers all Cucumber step expressions defined by {@code ktestify-cucumber} and any {@link KtestifyPlugin} visible
 * on the classpath or in the external plugins directory, without calling {@link KtestifyPlugin#initialize}.
 *
 * <p>Used by {@link DomainLintChecks} to enforce rule {@link LintRule#D005} (every Gherkin step must match a defined
 * step).
 *
 * @since 1.2.7
 */
final class StepDefinitionCatalog {

    private static final String CORE_STEPS_PACKAGE = "io.github.ktestify.steps";
    private static final String DEFAULT_PLUGINS_DIR = "/workspace/plugins";
    private static final Pattern CUCUMBER_PARAM_PATTERN = Pattern.compile("\\{([^}]*)}");

    private static volatile List<Pattern> cachedPatterns;

    private StepDefinitionCatalog() {}

    /**
     * Returns whether {@code stepText} matches at least one step definition in {@code ktestify-cucumber} or its
     * plugins.
     *
     * @param stepText the Gherkin step text (without the leading {@code Given}/{@code When}/{@code Then}/{@code And}
     *     keyword)
     * @return {@code true} if a matching step definition exists
     */
    static boolean isDefinedStep(String stepText) {
        if (stepText == null || stepText.isBlank()) {
            return false;
        }
        String trimmed = stepText.trim();
        for (Pattern pattern : patterns()) {
            if (pattern.matcher(trimmed).matches()) {
                return true;
            }
        }
        return false;
    }

    /** Returns the compiled step patterns, discovering them once and caching the result. */
    static List<Pattern> patterns() {
        List<Pattern> local = cachedPatterns;
        if (local == null) {
            synchronized (StepDefinitionCatalog.class) {
                local = cachedPatterns;
                if (local == null) {
                    local = Collections.unmodifiableList(discoverAllPatterns());
                    cachedPatterns = local;
                }
            }
        }
        return local;
    }

    private static List<Pattern> discoverAllPatterns() {
        Set<String> expressions = new LinkedHashSet<>();

        // 1. Core step classes (always present)
        extractExpressionsFromClass(BackgroundStepDefinition.class, expressions);
        extractExpressionsFromClass(ActionStepDefinition.class, expressions);
        extractExpressionsFromClass(ValidationStepDefinition.class, expressions);

        ClassLoader contextCL = Thread.currentThread().getContextClassLoader();
        scanPackage(CORE_STEPS_PACKAGE, contextCL, expressions);

        // 2. Classpath plugins via ServiceLoader (no plugin.initialize() called)
        discoverPluginStepExpressions(contextCL, expressions);

        // 3. External plugin JARs if present
        String pluginsDirPath = System.getProperty(
                "ktestify.plugins.dir", System.getenv().getOrDefault("KTESTIFY_PLUGINS_DIR", DEFAULT_PLUGINS_DIR));
        File pluginsDir = new File(pluginsDirPath);
        if (pluginsDir.isDirectory()) {
            File[] jars = pluginsDir.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"));
            if (jars != null && jars.length > 0) {
                URL[] urls = Arrays.stream(jars)
                        .map(f -> {
                            try {
                                return f.toURI().toURL();
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .toArray(URL[]::new);
                try (URLClassLoader externalCL = new URLClassLoader(urls, contextCL)) {
                    discoverPluginStepExpressions(externalCL, expressions);
                } catch (Exception ignored) {
                    // Best-effort external plugin scan during static linting
                }
            }
        }

        return expressions.stream().map(StepDefinitionCatalog::toRegexPattern).toList();
    }

    private static void discoverPluginStepExpressions(ClassLoader cl, Set<String> expressions) {
        ServiceLoader<KtestifyPlugin> loader = ServiceLoader.load(KtestifyPlugin.class, cl);
        for (ServiceLoader.Provider<KtestifyPlugin> provider : loader.stream().toList()) {
            try {
                KtestifyPlugin plugin = provider.get();
                String glue = plugin.getGluePackage();
                if (glue != null && !glue.isBlank()) {
                    scanPackage(glue.trim(), cl, expressions);
                }
            } catch (Throwable ignored) {
                // Ignore broken provider during static discovery
            }
        }
    }

    private static void scanPackage(String packageName, ClassLoader cl, Set<String> expressions) {
        String path = packageName.replace('.', '/');
        try {
            Enumeration<URL> resources = cl.getResources(path);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    File dir = new File(URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8));
                    scanDirectory(packageName, dir, cl, expressions);
                } else if ("jar".equals(protocol)) {
                    scanJar(path, url, cl, expressions);
                }
            }
        } catch (Exception ignored) {
            // Best-effort classpath scan
        }
    }

    private static void scanDirectory(String packageName, File dir, ClassLoader cl, Set<String> expressions) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(packageName + "." + file.getName(), file, cl, expressions);
            } else if (file.getName().endsWith(".class") && !file.getName().contains("$")) {
                String className = packageName + "."
                        + file.getName().substring(0, file.getName().length() - ".class".length());
                loadAndExtract(className, cl, expressions);
            }
        }
    }

    private static void scanJar(String packagePath, URL resourceUrl, ClassLoader cl, Set<String> expressions) {
        try {
            if (resourceUrl.openConnection() instanceof JarURLConnection conn) {
                try (JarFile jar = conn.getJarFile()) {
                    String prefix = packagePath.endsWith("/") ? packagePath : packagePath + "/";
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (name.startsWith(prefix) && name.endsWith(".class") && !name.contains("$")) {
                            String className = name.substring(0, name.length() - ".class".length())
                                    .replace('/', '.');
                            loadAndExtract(className, cl, expressions);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Best-effort JAR scan
        }
    }

    private static void loadAndExtract(String className, ClassLoader cl, Set<String> expressions) {
        try {
            Class<?> clazz = Class.forName(className, false, cl);
            extractExpressionsFromClass(clazz, expressions);
        } catch (Throwable ignored) {
            // Skip classes that cannot be loaded
        }
    }

    static void extractExpressionsFromClass(Class<?> clazz, Set<String> expressions) {
        for (Method method : clazz.getDeclaredMethods()) {
            for (Annotation annotation : method.getDeclaredAnnotations()) {
                if (annotation instanceof Given g && !g.value().isBlank()) {
                    expressions.add(g.value());
                } else if (annotation instanceof When w && !w.value().isBlank()) {
                    expressions.add(w.value());
                } else if (annotation instanceof Then t && !t.value().isBlank()) {
                    expressions.add(t.value());
                } else if (annotation instanceof And a && !a.value().isBlank()) {
                    expressions.add(a.value());
                } else if (annotation instanceof But b && !b.value().isBlank()) {
                    expressions.add(b.value());
                }
            }
        }
    }

    /** Converts a Cucumber expression (e.g. {@code "wait for {int} seconds"}) into a compiled {@link Pattern}. */
    static Pattern toRegexPattern(String cucumberExpression) {
        Matcher matcher = CUCUMBER_PARAM_PATTERN.matcher(cucumberExpression);
        StringBuilder regex = new StringBuilder("^");
        int lastEnd = 0;
        while (matcher.find()) {
            regex.append(Pattern.quote(cucumberExpression.substring(lastEnd, matcher.start())));
            String paramType = matcher.group(1).trim();
            regex.append(parameterRegex(paramType));
            lastEnd = matcher.end();
        }
        regex.append(Pattern.quote(cucumberExpression.substring(lastEnd)));
        regex.append("$");
        return Pattern.compile(regex.toString());
    }

    private static String parameterRegex(String paramType) {
        return switch (paramType) {
            case "int", "byte", "short", "long", "biginteger" -> "-?\\d+";
            case "float", "double", "bigdecimal" -> "-?\\d+(?:\\.\\d+)?";
            case "word" -> "\\S+";
            case "string" -> "(?:\"[^\"]*\"|'[^']*')";
            default -> ".+";
        };
    }
}

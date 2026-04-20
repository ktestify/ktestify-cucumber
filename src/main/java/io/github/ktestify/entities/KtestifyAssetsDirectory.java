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
package io.github.ktestify.entities;

import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.Builder;
import lombok.Value;

/**
 * Represents the assets base directory registered via {@code Given assets directory}.
 *
 * <p>All relative file paths used in step DataTables (e.g. {@code file}, {@code headerFile}) are resolved against this
 * base path. Absolute paths bypass the base directory.
 */
@Value
@Builder
public class KtestifyAssetsDirectory {

    /** The base path for test asset files. May be absolute or relative to the working directory. */
    String absolutePath;

    /**
     * Resolves {@code relativePath} against this assets directory. If {@code relativePath} is already absolute, it is
     * returned as-is.
     *
     * @param relativePath the file path from a step DataTable
     * @return the resolved absolute path string
     */
    public String resolve(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return relativePath;
        }
        Path p = Paths.get(relativePath);
        if (p.isAbsolute()) {
            return relativePath;
        }
        String base = absolutePath != null ? absolutePath : "";
        if (base.isBlank()) {
            return relativePath;
        }
        return Paths.get(base, relativePath).toString();
    }
}

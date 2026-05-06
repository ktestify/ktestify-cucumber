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
package io.github.ktestify.entities;

import lombok.Builder;
import lombok.Value;

/**
 * Represents an Avro schema registered in a Cucumber scenario via {@code Given schema}.
 *
 * <p>The schema is identified by its canonical name, an optional alias used in step definitions, and an optional
 * version number for Schema Registry subject versioning.
 */
@Value
@Builder
public class KtestifySchema {

    /** The Avro schema name / Schema Registry subject (e.g. {@code MySchema}). */
    String schemaName;

    /** Optional alias used in step definitions to reference this schema. */
    String schemaAlias;

    /** Optional Schema Registry subject version. When {@code null} or {@code 0}, the latest version is used. */
    Integer schemaVersion;
}

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
 * Represents a Kafka topic namespace registered in a Cucumber scenario via {@code Given namespace} or {@code Given
 * namespaces}.
 *
 * <p>When a namespace is set, the effective topic name becomes {@code namespace.topicName} (see
 * {@code Topic.getNamespacedTopic()}).
 */
@Value
@Builder
public class KtestifyNamespace {

    /** The namespace string prepended to topic names (e.g. {@code my-ns}). */
    String namespace;

    /** Optional alias used in step definitions to reference this namespace. */
    String namespaceAlias;
}

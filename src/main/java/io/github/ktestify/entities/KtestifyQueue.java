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
 * Represents an IBM MQ queue registered in a Cucumber scenario via {@code Given input queue} or {@code Given output
 * queue}.
 */
@Value
@Builder
public class KtestifyQueue {

    /** The IBM MQ queue name (e.g. {@code MY.QUEUE}). */
    String queueName;

    /** Optional alias used in step definitions to reference this queue. */
    String queueAlias;

    /** IBM MQ Queue Manager name. */
    String queueManager;

    /** IBM MQ channel name. */
    String channel;

    /** Queue direction — {@code INPUT} for producer queues, {@code OUTPUT} for consumer queues. */
    Type type;

    public enum Type {
        INPUT,
        OUTPUT
    }
}

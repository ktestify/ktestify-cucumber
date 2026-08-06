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
package io.github.ktestify.steps;

import static org.junit.jupiter.api.Assertions.*;

import io.github.ktestify.exceptions.TopicMismatchException;
import io.github.ktestify.models.Topic;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ActionStepDefinition}'s multi-row same-topic guard rail ({@code resolveAndAssertSingleTopic}).
 *
 * <p>These tests exercise only the topic-resolution logic (via reflection), which runs entirely before any Kafka
 * producer is created — no broker is required.
 */
@DisplayName("ActionStepDefinition — multi-row same-topic guard rail")
class ActionStepDefinitionMultiRowGuardTest {

    private static final Topic TOPIC_A = Topic.builder()
            .topicName("topic-a")
            .topicAlias("alias-a")
            .topicType(Topic.Type.INPUT)
            .build();

    private static final Topic TOPIC_B = Topic.builder()
            .topicName("topic-b")
            .topicAlias("alias-b")
            .topicType(Topic.Type.INPUT)
            .build();

    @Test
    @DisplayName("all rows targeting the same topic (by alias) resolve without error")
    void sameTopicAllRowsPasses() throws Exception {
        SharedStepsResources resources = new SharedStepsResources();
        resources.topics.register(TOPIC_A.getTopicName(), TOPIC_A.getTopicAlias(), TOPIC_A);

        ActionStepDefinition action = new ActionStepDefinition(resources);

        List<Map<String, String>> rows = List.of(
                Map.of("topicAlias", "alias-a", "file", "f1.json", "recordKey", "k1"),
                Map.of("topicAlias", "alias-a", "file", "f2.json", "recordKey", "k2"));

        Topic resolved = invokeResolveAndAssertSingleTopic(action, rows);
        assertEquals(TOPIC_A.getNamespacedTopic(), resolved.getNamespacedTopic());
    }

    @Test
    @DisplayName("rows targeting two distinct topics throw TopicMismatchException before any send")
    void mixedTopicsThrowsMismatch() {
        SharedStepsResources resources = new SharedStepsResources();
        resources.topics.register(TOPIC_A.getTopicName(), TOPIC_A.getTopicAlias(), TOPIC_A);
        resources.topics.register(TOPIC_B.getTopicName(), TOPIC_B.getTopicAlias(), TOPIC_B);

        ActionStepDefinition action = new ActionStepDefinition(resources);

        List<Map<String, String>> rows = List.of(
                Map.of("topicAlias", "alias-a", "file", "f1.json", "recordKey", "k1"),
                Map.of("topicAlias", "alias-b", "file", "f2.json", "recordKey", "k2"));

        InvocationTargetException wrapper =
                assertThrows(InvocationTargetException.class, () -> invokeResolveAndAssertSingleTopic(action, rows));
        assertInstanceOf(TopicMismatchException.class, wrapper.getCause());
        assertTrue(wrapper.getCause().getMessage().contains("single topic"));
    }

    // ── Reflection helper ───────────────────────────────────────────────────────

    private static Topic invokeResolveAndAssertSingleTopic(ActionStepDefinition action, List<Map<String, String>> rows)
            throws Exception {
        Method m = ActionStepDefinition.class.getDeclaredMethod("resolveAndAssertSingleTopic", List.class);
        m.setAccessible(true);
        return (Topic) m.invoke(action, rows);
    }
}

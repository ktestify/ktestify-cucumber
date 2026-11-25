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

import io.github.ktestify.exceptions.ConfigException;
import io.github.ktestify.exceptions.ConsumerException;
import io.github.ktestify.exceptions.ProducerException;
import io.github.ktestify.models.Topic;
import io.github.ktestify.utils.DataTableUtils;

import java.util.Map;

import static io.github.ktestify.utils.DataTableUtils.Constants.*;

/**
 * Executes a single DataTable row for a Cucumber step and enriches any failure with consistent, safe row context (step
 * name, 1-based row position, physical topic, file, and key) while preserving the original exception type and cause.
 */
final class StepRowExecution {

    private StepRowExecution() {}

    static void runRow(
            String stepName, int rowIndex, int totalRows, Topic topic, Map<String, String> row, Runnable action) {
        try {
            action.run();
        } catch (AssertionError e) {
            throw new AssertionError(formatPrefix(stepName, rowIndex, totalRows, topic, row) + " " + e.getMessage(), e);
        } catch (ConfigException e) {
            throw new ConfigException(
                    formatPrefix(stepName, rowIndex, totalRows, topic, row) + " " + e.getMessage(), e);
        } catch (ConsumerException e) {
            throw new ConsumerException(
                    formatPrefix(stepName, rowIndex, totalRows, topic, row) + " " + e.getMessage(), e);
        } catch (ProducerException e) {
            throw new ProducerException(
                    formatPrefix(stepName, rowIndex, totalRows, topic, row) + " " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new RuntimeException(
                    formatPrefix(stepName, rowIndex, totalRows, topic, row) + " " + e.getMessage(), e);
        }
    }

    static String formatPrefix(String stepName, int rowIndex, int totalRows, Topic topic, Map<String, String> row) {
        StringBuilder sb = new StringBuilder();
        sb.append("[step='").append(stepName).append("'");
        sb.append(", row=").append(rowIndex + 1).append('/').append(totalRows);
        if (topic != null) {
            sb.append(", topic='").append(topic.getNamespacedTopic()).append("'");
        }
        if (row != null) {
            String file = firstNonBlank(
                    DataTableUtils.getString(row, DATA_TABLE_FILE),
                    DataTableUtils.getString(row, DATA_TABLE_FILES),
                    DataTableUtils.getString(row, DATA_TABLE_SCRIPT_PATH));
            if (file != null) {
                sb.append(", file='").append(file).append("'");
            }
            String key = firstNonBlank(
                    DataTableUtils.getString(row, DATA_TABLE_RECORD_KEY),
                    DataTableUtils.getString(row, DATA_TABLE_EXPECTED_RECORD_KEY),
                    DataTableUtils.getString(row, DATA_TABLE_FIELD_TO_MATCH_KEY));
            if (key != null) {
                sb.append(", key='").append(key).append("'");
            }
        }
        sb.append(']');
        return sb.toString();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}

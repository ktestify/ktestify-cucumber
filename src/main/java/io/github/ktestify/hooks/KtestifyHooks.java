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
package io.github.ktestify.hooks;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import lombok.extern.slf4j.Slf4j;

/**
 * Cucumber lifecycle hooks for ktestify.
 *
 * <p><b>@Before</b> — logs the start of each scenario. The matched-record deduplication registry
 * ({@code KafkaRecordFetcher}) is intentionally <em>not</em> cleared automatically so that records consumed in one
 * scenario are not re-matched in subsequent ones within the same test run. Use the {@code And I clear known messages}
 * step when an explicit reset is needed.
 *
 * <p><b>@After</b> — logs the scenario result for traceability.
 */
@Slf4j
public class KtestifyHooks {

    /**
     * Runs before every scenario. Logs the scenario name and tags.
     *
     * <p>The matched-record registry is <em>not</em> reset here by design — this prevents already-matched messages from
     * being picked up again in later scenarios. If you need a clean slate mid-test, use the {@code And I clear known
     * messages} step.
     *
     * @param scenario the current Cucumber scenario
     */
    @Before
    public void beforeScenario(Scenario scenario) {
        log.info(
                "▶ Starting scenario: '{}' with tags {}",
                scenario.getName(),
                scenario.getSourceTagNames().toString());
    }

    /**
     * Runs after every scenario. Logs the final status.
     *
     * @param scenario the current Cucumber scenario
     */
    @After
    public void afterScenario(Scenario scenario) {
        if (scenario.isFailed()) {
            log.error("✖ Scenario FAILED: '{}'", scenario.getName());
        } else {
            log.info("✔ Scenario PASSED: '{}'", scenario.getName());
        }
    }
}

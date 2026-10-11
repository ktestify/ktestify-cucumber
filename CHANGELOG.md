# Changelog

All notable changes to this project will be documented in this file.

## [1.2.1] — 2026-10-11

### ♻️ Refactoring

- Removed ObjectManager from cucumber it is provided by ktestify-core — [@nil-malh](https://github.com/nil-malh) ([#53](https://github.com/ktestify/ktestify-cucumber/pull/53))

- Improve error handling in ConsumerValidationService by extracting cause finding logic — [@nil-malh](https://github.com/nil-malh) ([#53](https://github.com/ktestify/ktestify-cucumber/pull/53))


### ✨ Features

- Feat/bug fixes (#53) — [@nil-malh](https://github.com/nil-malh) ([#53](https://github.com/ktestify/ktestify-cucumber/pull/53))

- Enhance timeout handling in ConsumerValidationService and add tests — [@nil-malh](https://github.com/nil-malh) ([#53](https://github.com/ktestify/ktestify-cucumber/pull/53))

- Add keyMatchStrategy support for flexible key matching (#52) — [@nil-malh](https://github.com/nil-malh) ([#53](https://github.com/ktestify/ktestify-cucumber/pull/53))

- Add keyMatchStrategy support for flexible key matching in ConsumerValidationService — [@nil-malh](https://github.com/nil-malh) ([#53](https://github.com/ktestify/ktestify-cucumber/pull/53))


### ⬆️ Dependency Updates

- Updates ktestify first-party plugins versions to latest — [@nil-malh](https://github.com/nil-malh) ([#53](https://github.com/ktestify/ktestify-cucumber/pull/53))


### 🐛 Bug Fixes

- Close kafka producer after step — [@nil-malh](https://github.com/nil-malh) ([#53](https://github.com/ktestify/ktestify-cucumber/pull/53))

- Handle AssertionError in ConsumerValidationService and update ktestify dependency to 1.2.2 — [@nil-malh](https://github.com/nil-malh) ([#53](https://github.com/ktestify/ktestify-cucumber/pull/53))


### 🧪 Tests

- Refactor ConsumerValidationServiceHelpersTest for improved readability and structure — [@nil-malh](https://github.com/nil-malh) ([#53](https://github.com/ktestify/ktestify-cucumber/pull/53))


## [1.2.0] — 2026-09-29

### ⬆️ Dependency Updates

- Bump io.github.ktestify:ktestify-core from 1.1.0 to 1.1.2 (#41) *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot]) ([#41](https://github.com/ktestify/ktestify-cucumber/pull/41))

- Bump io.cucumber:datatable in the cucumber group (#42) *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot]) ([#42](https://github.com/ktestify/ktestify-cucumber/pull/42))

- Bump org.apache.avro:avro from 1.12.1 to 1.12.2 (#43) *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot]) ([#43](https://github.com/ktestify/ktestify-cucumber/pull/43))

- Bump io.cucumber:datatable in the cucumber group (#45) *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot]) ([#45](https://github.com/ktestify/ktestify-cucumber/pull/45))

- Bump io.github.ktestify:ktestify-parent from 1.0.2 to 1.0.3 (#48) *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot]) ([#48](https://github.com/ktestify/ktestify-cucumber/pull/48))


## [1.1.0] — 2026-08-06

### ✨ Features

- Added centralised actions (#37) — [@nil-malh](https://github.com/nil-malh) ([#37](https://github.com/ktestify/ktestify-cucumber/pull/37))

- Added centralised actions — [@nil-malh](https://github.com/nil-malh) ([#37](https://github.com/ktestify/ktestify-cucumber/pull/37))

- Implement multi-row support for producer and consumer steps wit… (#40) — [@nil-malh](https://github.com/nil-malh) ([#40](https://github.com/ktestify/ktestify-cucumber/pull/40))

- Implement multi-row support for producer and consumer steps with topic validation — [@nil-malh](https://github.com/nil-malh) ([#40](https://github.com/ktestify/ktestify-cucumber/pull/40))


### ⬆️ Dependency Updates

- Bump io.cucumber:datatable in the cucumber group (#39) *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot]) ([#39](https://github.com/ktestify/ktestify-cucumber/pull/39))


## [1.0.5] — 2026-07-07

### ✨ Features

- Feat/ktestify parent pom (#35) — [@nil-malh](https://github.com/nil-malh) ([#35](https://github.com/ktestify/ktestify-cucumber/pull/35))

- Switched to a parent pom for dependencies management — [@nil-malh](https://github.com/nil-malh) ([#35](https://github.com/ktestify/ktestify-cucumber/pull/35))


### ⬆️ Dependency Updates

- Updated ktestify-core to 0.1.2 — [@nil-malh](https://github.com/nil-malh) ([#31](https://github.com/ktestify/ktestify-cucumber/pull/31))

- Updated ktestify-plugin-azureblob & ktestify-plugin-notifications to 0.1.2 — [@nil-malh](https://github.com/nil-malh) ([#35](https://github.com/ktestify/ktestify-cucumber/pull/35))


### 🐛 Bug Fixes

- Fix/30 (#31) — [@nil-malh](https://github.com/nil-malh) ([#31](https://github.com/ktestify/ktestify-cucumber/pull/31))

- Fixed an issue where the version banner was displaying dev — [@nil-malh](https://github.com/nil-malh) ([#31](https://github.com/ktestify/ktestify-cucumber/pull/31))

- Fixed an issue where the reporting plugin was not added to the Cucumber glue when args were provided — [@nil-malh](https://github.com/nil-malh) ([#31](https://github.com/ktestify/ktestify-cucumber/pull/31))

- Update dependencies and align Jackson versions to prevent runtime errors — [@nil-malh](https://github.com/nil-malh) ([#31](https://github.com/ktestify/ktestify-cucumber/pull/31))

- Added missing version.properties file — [@nil-malh](https://github.com/nil-malh)


## [0.1.4] — 2026-06-30

### ✨ Features

- Added Azure Blob & Notifications as first party plugins (#26) — [@nil-malh](https://github.com/nil-malh) ([#26](https://github.com/ktestify/ktestify-cucumber/pull/26))


### ⬆️ Dependency Updates

- Bump maven.surefire.plugin.version from 3.5.5 to 3.5.6 *(deps-dev)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump org.jacoco:jacoco-maven-plugin *(deps-dev)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump io.github.ktestify:ktestify-core from 0.1.0 to 0.1.1 *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump com.diffplug.spotless:spotless-maven-plugin *(deps-dev)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump the cucumber group with 4 updates *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump the junit5 group with 3 updates *(deps-dev)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump com.diffplug.spotless:spotless-maven-plugin *(deps-dev)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump org.sonatype.central:central-publishing-maven-plugin *(deps-dev)* — [@dependabot[bot]](https://github.com/dependabot[bot])


### 🐛 Bug Fixes

- Fixed an issue where the version was not displayed in the banner (#29) — [@nil-malh](https://github.com/nil-malh) ([#29](https://github.com/ktestify/ktestify-cucumber/pull/29))


## [0.1.3] — 2026-06-02

### ✨ Features

- Docker for cucumber impl — [@nil-malh](https://github.com/nil-malh)

- Add cliff config — [@nil-malh](https://github.com/nil-malh)

- Added more integration tests — [@nil-malh](https://github.com/nil-malh)

- Added new steps for key only assertions — [@nil-malh](https://github.com/nil-malh)

- Add README.md and update changelog workflow with new PAT — [@nil-malh](https://github.com/nil-malh)


### ⬆️ Dependency Updates

- Update cucumber-reportr to 1.3.0 — [@nil-malh](https://github.com/nil-malh)

- Bump the testcontainers group across 1 directory with 3 updates *(deps-dev)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump the junit5 group across 1 directory with 3 updates *(deps-dev)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump commons-io:commons-io *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump com.google.cloud.tools:jib-maven-plugin *(deps-dev)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump com.google.code.gson:gson from 2.13.2 to 2.14.0 *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump the logging group with 3 updates *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump org.apache.maven.plugins:maven-shade-plugin *(deps-dev)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump org.projectlombok:lombok from 1.18.44 to 1.18.46 *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump io.github.ktestify:ktestify-core *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump the junit5 group with 3 updates *(deps-dev)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump com.diffplug.spotless:spotless-maven-plugin *(deps-dev)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump org.slf4j:slf4j-api *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot])


### 🐛 Bug Fixes

- Fixed integration-tests.yml — [@nil-malh](https://github.com/nil-malh)

- Fixed integration-tests.yml — [@nil-malh](https://github.com/nil-malh)

- Fixed an issue where the release was failing due to no device found /dev/tty — [@nil-malh](https://github.com/nil-malh)

- Fixed an issue where the release was failing due to no device found /dev/tty — [@nil-malh](https://github.com/nil-malh)

- Fixed an issue where the release was failing due to no device found /dev/tty — [@nil-malh](https://github.com/nil-malh)


### 🔧 Miscellaneous

- Removed all ConfigConstants usage from ktestify-core — [@nil-malh](https://github.com/nil-malh)

- Update copyright information and license headers across multiple files — [@nil-malh](https://github.com/nil-malh)

- Removed .docker directory — [@nil-malh](https://github.com/nil-malh)

- Updated workflows to run only once when in a PR context — [@nil-malh](https://github.com/nil-malh)


### 🎉 New Contributors




---
*Generated by [git-cliff](https://github.com/orhun/git-cliff)*

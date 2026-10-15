<!--
  Thanks for contributing to KTestify! 🙌

  PR title: please follow Conventional Commits, e.g.
    feat(core): add JSONPath record matcher
    fix(cucumber): honour consumerReadTimeout in batch steps
    docs: document Azure Blob auth modes
  Add `!` for breaking changes:  feat(core)!: rename RecordMatcher#matches

  Keep the PR focused: one logical change per PR is much easier to review.
-->

## 📝 Summary

<!-- WHAT does this PR change, and WHY? 2-4 sentences is usually enough.
     Focus on the motivation, since the diff already shows the how. -->

Closes #<!-- issue number (use "Refs #" if it only partially addresses it) -->

## 🔄 Type of change

<!-- Tick one or more. -->

- [ ] 🐛 **Bug fix** (`fix`): non-breaking change that fixes an issue
- [ ] ✨ **Feature** (`feat`): non-breaking change that adds functionality
- [ ] 💥 **Breaking change** (`!`): changes existing behaviour, public API, Gherkin steps or config keys
- [ ] ♻️ **Refactor** (`refactor`): no functional change
- [ ] ⚡ **Performance** (`perf`)
- [ ] 📖 **Documentation** (`docs`)
- [ ] ⬆️ **Dependencies** (`build(deps)`)
- [ ] 🔧 **CI / build** (`ci` / `build`): workflows, `pom.xml`, tooling

## 💥 Breaking changes & migration

<!-- Only if you ticked "Breaking change", otherwise delete this section.
     Describe what breaks (public API, SPI contract such as RecordMatcher /
     RecordFetcher, Gherkin step wording, HOCON keys, env vars) and how users
     or plugin authors should migrate. A before/after snippet helps a lot. -->

## ✅ Checklist

<!-- Tick what applies. Strike through (~~like this~~) items that don't apply,
     rather than deleting them, so reviewers know they were considered.
     Docs-only PRs (ktestify-doc) can skip "Code" and "Tests". -->

**Code**
- [ ] Formatting passes `mvn spotless:check` (run `mvn spotless:apply` to fix it and add the Apache 2.0 headers)
- [ ] Public types and methods have up-to-date **Javadoc**
- [ ] No secrets, tokens or real webhook URLs in code, tests or sample config

**Tests**
- [ ] New / changed behaviour is covered by tests
- [ ] `mvn verify` passes locally, and **JaCoCo line coverage stays ≥ 70 %**
- [ ] Testcontainers-based tests are isolated and clean up after themselves

**Compatibility & docs**
- [ ] New or changed HOCON keys have a default in `reference.conf` and a documented env-var override
- [ ] New or changed **Gherkin steps** are documented, with an example scenario
- [ ] New SPI implementations are registered in `META-INF/services/`
- [ ] User-facing changes are documented (docs PR: <!-- ktestify/ktestify-doc#… or "not needed" -->)

## 🧪 How to test

<!-- Steps a reviewer can follow to verify the change. The defaults below
     need Docker running for Testcontainers. -->

```bash
mvn spotless:check
mvn verify

# ktestify-cucumber / plugins: also run the integration suite
mvn verify -Pintegration-tests -Dcucumber.it.tags="@integration"
```

<!-- Add PR-specific steps here: feature file to run, config to set, expected output… -->

## 📸 Logs / screenshots

<!-- Optional. Wrap long output so the PR stays readable: -->

<details>
<summary>Output</summary>

```text

```

</details>

## 💬 Notes for reviewers

<!-- Optional: design decisions, trade-offs, alternatives you rejected,
     areas you'd like a closer look at, or follow-up work planned. -->
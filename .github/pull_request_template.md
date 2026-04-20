## 📝 Description

<!-- Clearly describe WHAT this PR does and WHY.
     Link the issue(s) it resolves below. -->

Resolves #<!-- issue number -->

---

## 🔄 Type of Change

<!-- Delete options that are not relevant. -->

- [ ] 🐛 **Bug fix** — non-breaking change that fixes an issue
- [ ] ✨ **New feature** — non-breaking change that adds functionality
- [ ] 💥 **Breaking change** — fix or feature that changes existing behaviour
- [ ] ♻️ **Refactoring / technical debt** — no functional change
- [ ] 📖 **Documentation** — doc-only changes
- [ ] ⬆️ **Dependency update** — bumped library/plugin version(s)
- [ ] 🔧 **CI / build** — changes to workflows, pom.xml, tooling

---

## ✅ Pre-Merge Checklist

### Code Quality
- [ ] My code follows the **Palantir Java Format** style (passes `mvn spotless:check`)
- [ ] I have added or updated **Javadoc** on public types and methods

### Tests
- [ ] I have added **unit tests** that cover the new / changed behaviour
- [ ] All existing and new tests pass locally (`mvn verify`)
- [ ] **JaCoCo line coverage ≥ 70 %** is maintained
- [ ] Tests using Testcontainers are isolated and do not leave containers running


### Licensing
- [ ] **Apache 2.0 license header** has been added to every new source file (using spotless)

---

## 🧪 How to Test Locally

```bash
# 1. Build and run all tests (requires Docker for Testcontainers)
mvn verify

# 2. Check code style
mvn spotless:check
```

<!-- Add any additional test steps specific to this PR -->

---

## 📸 Screenshots / Logs

<!-- If applicable, paste relevant log output or screenshots. -->

---

## 📚 Additional Context

<!-- Any other information reviewers should know. -->


# Security Policy

## Supported Versions

We release patches for security vulnerabilities for the latest stable version only.

| Version  | Supported          |
|----------|--------------------|
| latest   | ✅ Yes             |
| < latest | ❌ No              |

## Reporting a Vulnerability

**Please do NOT open a public GitHub issue for security vulnerabilities.**

Instead, use one of the following channels:

### Option 1 — GitHub Private Advisory (preferred)

Use the [GitHub Security Advisory](https://github.com/ktestify/ktestify-coresecurity/advisories/new)
feature to report the vulnerability privately.  
We will acknowledge it within **3 business days** and aim to release a fix within **14 days**
for critical issues.

### Option 2 — Email

Send a detailed report to the maintainers.  
Include:
- A description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)

## What to Expect

1. **Acknowledgement** within 3 business days.
2. **Triage** — we assess severity using CVSS v3.
3. **Fix** — developed on a private branch.
4. **Coordinated Disclosure** — we'll notify you before the public release.
5. **CVE** — we will request a CVE ID for valid, critical vulnerabilities.
6. **Credit** — reporters are credited in the release notes (unless they prefer anonymity).

## Scope

This policy applies to the `ktestify-core` library and its direct dependencies.  
Vulnerabilities in transitive dependencies should be reported upstream.


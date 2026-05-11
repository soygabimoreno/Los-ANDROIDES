---
name: android-pr-review
description: Review Los ANDROIDES pull requests with emphasis on bugs, architecture regressions, missing tests, and Android/Kotlin quality.
---
# Android PR Review Workflow

Use this skill when reviewing a pull request or a local diff in this repository.

First read `AGENTS.md` from the repository root and use it as the review standard.

## Review Priorities

1. User-visible bugs and regressions.
2. Architecture boundary violations.
3. Missing or weak tests.
4. Coroutine, Flow, lifecycle, or threading risks.
5. Compose state, recomposition, or accessibility problems.
6. Dependency, Gradle, or version catalog issues.
7. Style issues only when they affect maintainability or violate project rules.

## Output Format

Lead with findings ordered by severity.

Each finding should include:

* file and line reference
* concrete risk
* suggested fix when useful

If no findings are found, say that clearly and mention any verification gaps.

---
name: android-tests
description: Add or update unit tests in Los ANDROIDES using the project's strict MockK, Kluent, and GIVEN WHEN THEN conventions.
---
# Android Test Workflow

Use this skill when adding or updating tests in this repository.

First read `AGENTS.md` from the repository root, especially the Testing section.

## Workflow

1. Inspect nearby tests for naming, fixture, mock, assertion, and verification style.
2. Use MockK and Kluent.
3. Use typed mocks explicitly.
4. Follow `GIVEN ... WHEN ... THEN ...` test names.
5. Keep exactly three implicit blocks separated by exactly two blank lines.
6. Do not write `GIVEN`, `WHEN`, or `THEN` inside the test body.
7. Use `verifyOnce`, `verifyNever`, `coVerifyOnce`, or `coVerifyNever`.
8. Avoid Robolectric in new tests.
9. Keep test data deterministic.
10. Run the relevant test task after editing.

## Preferred Commands

```bash
./gradlew testDebugUnitTest
```

For a narrow module check, use the matching module test task when available.

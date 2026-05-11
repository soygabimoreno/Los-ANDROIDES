---
name: android-feature
description: Create or modify Android features in Los ANDROIDES following the repository architecture, module boundaries, Compose conventions, and verification rules.
---
# Android Feature Workflow

Use this skill when creating or changing user-facing Android behavior in this repository.

First read `AGENTS.md` from the repository root and follow it as the source of truth.

## Workflow

1. Inspect the nearby feature, screen, ViewModel, use cases, repositories, mappers, and tests before editing.
2. Identify the owning module and avoid moving code across modules unless the task requires it.
3. Keep UI behavior in Compose and presentation state.
4. Keep business rules in use cases or domain code.
5. Keep data access behind repository interfaces.
6. Add or update mappers when data crosses layer boundaries.
7. Reuse existing DI, navigation, theme, and design system patterns.
8. Add or update tests for behavior changes.
9. Run the narrowest useful verification, then broaden if shared code changed.

## Output Expectations

When finished, summarize:

* changed behavior
* touched modules/files
* verification commands run
* any verification that could not be run

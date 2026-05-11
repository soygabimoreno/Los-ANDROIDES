---
name: android-modularization
description: |
  Module layout, dependency rules, and Gradle convention plugins for Android and KMP.
  Trigger on: "set up the project", "add a module", "create a feature", "how should I
  structure", "project structure", "convention plugin", "build-logic", "where does X live",
  "new feature module", "core submodule".
---

# Android / KMP Modular Architecture

## Core Philosophy

- **Feature-layered modularization**: split by feature first, then by layer within each feature.
- **Clean Architecture layers**: `presentation` → `domain` ← `data`. Domain is innermost
  and depends on nothing.
- **Code lives in a feature module unless it is needed by more than one feature** — then
  it moves to the appropriate `core` submodule.
- **Features never depend on each other.** Cross-feature shared data belongs in
  `core:domain` (domain models) or `core:presentation` (shared UI logic), not in the
  owning feature.

---

## Module Layout

```
:app
:build-logic                    ← Gradle convention plugins
:core:domain                    ← Shared domain models, interfaces, error types, Result
:core:data                      ← Shared data logic, Ktor HttpClient factory
:core:database                  ← Shared Room DB, all entities, DAOs, migrations
:core:presentation              ← Shared UI utilities (ObserveAsEvents, UiText, etc.)
:core:design-system             ← Reusable Compose components, colors, theme, typography
:feature:<name>:domain          ← Feature-specific domain models, interfaces, error types
:feature:<name>:data            ← Repo implementations, DTOs, mappers, Room DAOs
:feature:<name>:presentation    ← ViewModel, screen composables, state, actions, events
```

For standalone concerns involving meaningful complexity (multiple classes, configuration,
or a non-trivial API surface), create a dedicated module under `:core`
(e.g., `:core:location`, `:core:analytics`, `:core:auth`). Do not create a separate
module for a single class or trivial utility — that belongs in an existing `core` module.

A shared Room database lives in `:core:database`. Feature modules that need DB access
depend on `:core:database` directly; features that don't need it remain decoupled.

---

## Dependency Rules

| Layer | May depend on |
|---|---|
| `presentation` | `domain` (own feature), `core:domain`, `core:presentation`, `core:design-system` |
| `data` | `domain` (own feature), `core:domain`, `core:data`, `core:database` |
| `domain` | `core:domain` only — never `data` or `presentation` |
| `:app` | everything (wires all modules) |

Every layer and module may access `core:domain`.

---

## Convention Plugins (`:build-logic`)

Define a convention plugin for every non-trivial Gradle config:

| Plugin | Purpose |
|---|---|
| `android-application` | App module config (applicationId, versionCode, etc.) |
| `android-library` | Base Android library config |
| `android-feature` | Android library + Compose + Koin + shared feature deps bundled |
| `domain-module` | Pure Kotlin/KMP module, no Android deps |
| `compose` | Compose compiler + BOM |
| `koin` | Koin dependency block |
| `ktor` | Ktor client + serialization |
| `room` | Room + KSP config |
| `kotlinx-serialization` | KotlinX Serialization plugin + dep |

Use **version catalogs** (`libs.versions.toml`) for all dependency and version
management. No hardcoded versions in build files.

---

## Key Libraries

| Concern | Library |
|---|---|
| DI | Koin |
| Networking | Ktor Client |
| Local DB | Room |
| Preferences | DataStore |
| Navigation | Compose Navigation (type-safe) |
| Serialization | KotlinX Serialization |
| Image loading | Coil |
| Logging | Kermit |
| Async | Coroutines + Flow |
| Background tasks | WorkManager |
| Secrets | `local.properties` + `BuildConfig` (Android); `BuildKonfig` (KMP) |
| Testing | JUnit5, Turbine, AssertK, `kotlinx-coroutines-test` |
| UI testing | `ComposeTestRule` |

---

## Checklist: Adding a New Feature Module

- [ ] Create `:feature:<name>:domain`, `:feature:<name>:data`,
      `:feature:<name>:presentation` modules
- [ ] Apply appropriate convention plugins (`domain-module`, `android-library` or
      `android-feature`)
- [ ] Verify no cross-feature dependencies are introduced
- [ ] If logic is shared across 2+ features, extract to the appropriate `core` submodule

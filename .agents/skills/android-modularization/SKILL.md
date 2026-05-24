---
name: android-modularization
description: |
  Module layout, dependency rules, and Gradle convention plugins for Los ANDROIDES.
  Trigger on: "add a module", "create a feature", "new feature module",
  "how should I structure", "project structure", "convention plugin", "build-logic",
  "where does X live", "core submodule", "library submodule".
---

# Modular architecture — Los ANDROIDES

## Core philosophy

- **Feature-layered modularization**: split by feature first, then by layer within each feature.
- **Clean Architecture layers**: `presentation` → `domain` ← `data`. Domain is innermost
  and depends on nothing.
- **Code lives in a feature module unless it is needed by more than one feature** — then
  it moves to the appropriate `core` submodule.
- **Features never depend on each other.** Cross-feature shared data belongs in
  `:core:core-domain` (domain models) or `:core:core-view` (shared UI logic), not in the
  owning feature.

---

## Current module layout

Authoritative source: `settings.gradle.kts`.

```
:app                            ← Application entry point, wires all modules
:build-logic                    ← Gradle convention plugins (composite build)
:core:core                      ← Shared base utilities and base infra
:core:core-domain               ← Shared domain models, interfaces, error types, Result
:core:core-view                 ← Shared Compose UI utilities (ObserveAsEvents, UiText, etc.)
:core:core-testing              ← Shared test doubles, fakes, fixtures
:feature:auth                   ← Auth feature
:feature:podcast                ← Podcast feature
:library:framework              ← Framework abstractions
:library:player                 ← Audio player
:library:raffle                 ← Raffle library
:library:remote-config:api      ← Remote config public API
:library:remote-config:impl     ← Remote config implementation
```

`:library:remote-config` uses the **api/impl split** pattern: consumers depend on `:api`
only; `:impl` is wired in `:app` and supplied at runtime, keeping consumers decoupled
from the concrete implementation.

---

## Aspirational layering inside features

A feature module should split into layers as it grows. For a new or expanding feature,
create three modules so each layer is enforceable at the Gradle level:

```
:feature:<name>:domain          ← Feature domain models, interfaces, error types
:feature:<name>:data            ← Repo implementations, DTOs, mappers, Retrofit services, Room DAOs
:feature:<name>:presentation    ← ViewModel, screen composables, state, actions, events
```

For standalone concerns involving meaningful complexity (multiple classes, configuration,
or a non-trivial API surface), create a dedicated module under `:core` or `:library`
(e.g., `:core:core-analytics`, `:library:notifications`). Do not create a separate
module for a single class or trivial utility — that belongs in an existing module.

---

## Dependency rules

| Layer | May depend on |
|---|---|
| `presentation` | own `domain`, `:core:core-domain`, `:core:core-view`, `:core:core` |
| `data` | own `domain`, `:core:core-domain`, `:core:core` |
| `domain` | `:core:core-domain` only — never `data` or `presentation` |
| `:app` | everything (wires all modules) |

Every layer and module may access `:core:core-domain`.

`:core:core-testing` is only depended on from test source sets (`testImplementation`).

---

## Convention plugins (`:build-logic`)

`:build-logic` is a composite build (`includeBuild("build-logic")` in
`settings.gradle.kts`) that exposes shared Gradle plugin configurations.

Current convention plugins:

| Plugin | Purpose |
|---|---|
| `androidLibrary` | Base Android library config (compileSdk, minSdk, kotlin, lint) |

Aspirational convention plugins to introduce as the project grows (do not invent these
until they are actually needed):

| Plugin | Purpose |
|---|---|
| `androidApplication` | App module config (applicationId, versionCode, etc.) |
| `androidFeature` | `androidLibrary` + Compose + Hilt + shared feature deps bundled |
| `androidCompose` | Compose compiler + BOM + Material |
| `androidHilt` | Hilt plugin + KSP + runtime deps |
| `androidRoom` | Room plugin + KSP config |
| `kotlinDomain` | Pure Kotlin module (no Android deps) for domain layers |

Use **version catalogs** (`gradle/libs.versions.toml`) for all dependency and version
management. No hardcoded versions in build files.

---

## Key libraries

| Concern | Library |
|---|---|
| DI | Hilt (`com.google.dagger:hilt-android`) |
| Networking | Retrofit 3 + OkHttp |
| JSON | Moshi (codegen via `moshi-kotlin-codegen`) |
| Local DB | Room |
| Preferences | DataStore (`androidx.datastore:datastore-preferences`) |
| Navigation | Compose Navigation |
| Image loading | Coil |
| Async | Coroutines + Flow |
| Functional types | Arrow |
| Test framework | JUnit 4 |
| Assertions | Kluent |
| Mocking | MockK |
| Flow testing | Turbine |
| Coroutine testing | `kotlinx-coroutines-test` |
| UI testing | `ComposeTestRule` (`androidx.compose.ui:ui-test-junit4`) |

See `gradle/libs.versions.toml` for exact versions.

---

## Checklist — adding a new feature module

- [ ] Decide: single module `:feature:<name>` (small surface) or layered
      `:feature:<name>:domain` + `:data` + `:presentation` (growing surface)
- [ ] Register the module(s) in `settings.gradle.kts`
- [ ] Apply the appropriate convention plugin (`androidLibrary` today; eventually
      `androidFeature` / `kotlinDomain`)
- [ ] Verify no cross-feature dependencies are introduced
- [ ] If logic is shared across 2+ features, extract to the appropriate `:core` submodule
- [ ] Add Hilt bindings under the feature's `data`/`presentation` module

# AGENTS.md

This file is the source of truth for AI coding agents working in this repository.
Keep tool-specific files such as `CLAUDE.md`, `GEMINI.md`, or GitHub assistant instructions
thin and pointed back to this document whenever possible.

## Project

Los ANDROIDES is an Android app for listening to Android development audios.

The project is written in Kotlin and follows modern Android development practices:

* MVVM
* Clean Architecture
* Coroutines and Flow
* Jetpack Compose
* Firebase
* Hilt
* KSP
* Arrow
* Retrofit
* Moshi
* ExoPlayer
* DataStore
* Room
* MockK
* Kluent
* MockWebServer

## General Rules

* Write production code in English.
* Keep explanations to the user clear, brief, and direct.
* Prefer existing project patterns over new abstractions.
* Do not introduce new architectural patterns unless explicitly requested.
* Keep changes minimal, focused, and easy to review.
* Do not commit or push without showing the diff and asking for confirmation first.
* Do not refactor unrelated code.
* Do not add unnecessary dependencies.
* Do not duplicate logic when updating existing code is enough.
* Preserve public APIs and method signatures unless the requested change requires otherwise.
* Use Git deliberately and never overwrite unrelated local changes.

## Repository Structure

Important modules and folders:

* `app`: main Android application module.
* `core:core`: shared core utilities.
* `core:core-domain`: shared domain models and contracts.
* `core:core-testing`: shared test utilities.
* `core:core-view`: shared UI/view utilities.
* `feature:auth`: authentication feature module.
* `feature:podcast`: podcast feature module.
* `library:framework`: Android framework helpers.
* `library:player`: audio player library.
* `library:raffle`: raffle library.
* `library:remote-config:api`: remote config API contracts.
* `library:remote-config:impl`: remote config implementation.
* `build-logic`: convention plugins.
* `gradle/libs.versions.toml`: dependency and plugin versions.
* `config/detekt/detekt.yml`: Detekt configuration.
* `.agents/skills`: shared project skills for AI agents.
* `.claude/skills`: symlink to `../.agents/skills` so Claude Code can discover the shared skills.

## Shared Skills

Project skills live in `.agents/skills` and are versioned with the repository so contributors
and AI agents use the same procedures.

Rules:

* Keep project-specific skills in this repository.
* Keep shared Android skills here as a project snapshot when collaborators need them.
* Do not symlink project skills to local personal paths.
* If a personal/global skill is useful for this project, copy it into `.agents/skills` and adapt it to this codebase.
* Keep `.claude/skills` as a symlink to `../.agents/skills`.
* Do not add unrelated skills such as content creation, Remotion, or general skill authoring unless the project needs them.

## Architecture

Follow Clean Architecture with clear boundaries between:

* `data`
* `domain`
* `presentation`

Rules:

* Domain code must not depend on Android APIs whenever possible.
* Business logic belongs in use cases or domain services, not in composables.
* Presentation code coordinates UI state and user events.
* Repositories hide implementation details from the domain layer.
* Data sources and DTOs must not leak into domain models.
* Prefer unidirectional data flow.
* Keep side effects controlled and visible.
* Prefer domain-specific error models when they make UI and business behavior clearer.

## UI and Compose

* Use Jetpack Compose for UI unless the touched area clearly uses another approach.
* Prefer stateless composables.
* Hoist state out of composables.
* Keep composables small and focused.
* Do not put business logic inside composables.
* Follow existing design system components and theme definitions.
* Use `@Preview` only when it adds clear value.
* Prefer stable models and avoid unnecessary recomposition.
* Use accessibility-friendly APIs and semantics where relevant.

## ViewModels

* ViewModels orchestrate UI state and call use cases.
* ViewModels must not contain heavy business logic.
* Expose immutable UI state.
* Prefer explicit state objects.
* Keep UI events and one-shot effects clearly modeled.
* Avoid leaking repository or data source implementation details into the UI layer.

## Use Cases

* Each use case should represent one business action or query.
* Prefer `operator fun invoke`.
* Keep use cases small and focused.
* Do not add Android dependencies to use cases unless there is already a project pattern for it.

Example:

```kotlin
class GetUserProfileUseCase(
    private val repository: UserRepository,
) {

    suspend operator fun invoke(userId: String): UserProfile {
        return repository.getProfile(userId)
    }
}
```

## Repositories and Data

* Domain depends on repository interfaces.
* Data layers provide repository implementations.
* Repositories map remote/local models into domain models.
* Avoid exposing DTOs, database entities, or network response models outside data boundaries.
* Prefer explicit mappers when models cross architectural layers.
* Keep persistence, networking, and framework-specific concerns out of domain code.

## Dependency Injection

* Use Hilt, following the existing project setup.
* Prefer constructor injection.
* Keep DI modules explicit and easy to navigate.
* Do not introduce another DI framework.

## Coroutines and Flow

* Prefer `suspend` functions and `Flow` over callbacks.
* Avoid blocking calls in coroutines.
* Respect structured concurrency.
* Make threading decisions explicit when needed.
* Handle cancellation correctly.
* Keep long-running work outside the UI thread.

## Gradle and Dependencies

* Use Kotlin DSL.
* Use the Version Catalog in `gradle/libs.versions.toml`.
* Do not use Groovy snippets.
* Keep dependency additions minimal.
* Reuse existing versions and libraries before introducing new ones.
* If a new dependency is required, add the catalog entry and the module usage in the same coherent change.
* In `build.gradle.kts` `dependencies` blocks, sort entries alphabetically within each configuration group (`implementation`, `debugImplementation`, `ksp`, `testImplementation`, etc.) and do not add blank lines within a group. Only separate groups with a blank line.

## Testing

Use the project's existing testing style.

**Write tests as part of the same task**, not as a follow-up. Every new behavior in a use case,
repository, or ViewModel must have at least one test covering it before the task is closed.

Core rules:

* Use MockK.
* Use Kluent.
* Do not use Robolectric for new tests.
* Do not use `@ExtendWith(RobolectricExtension::class)`.
* Do not use `@Config`.
* Do not use `wasNot Called`.
* Do not use `verify(exactly = 0)`.
* Do not use `verify(exactly = 1)`.
* Do not use plain `verify`.
* Use `verifyOnce` and `verifyNever` from the project test utilities.
* For coroutine verification, use `coVerifyOnce` and `coVerifyNever`.
* Do not declare local helper functions named `verifyOnce` or `verifyNever`.
* Do not use `returns Unit`; use `just runs` when needed.
* Do not use `relaxed = true`.
* Prefer explicit typed mocks:

```kotlin
val context: Context = mockk()
val value: Slot<Foo> = slot()
```

Do not write:

```kotlin
val context = mockk<Context>()
val value = slot<Foo>()
```

### Test Naming and Layout

* Test names must follow `GIVEN ... WHEN ... THEN ...`.
* `GIVEN`, `WHEN`, and `THEN` must appear only in the test name.
* Do not write `GIVEN`, `WHEN`, or `THEN` inside the test body.
* The test body must contain exactly three implicit blocks separated by exactly two blank lines.
* The first block is setup.
* The second block executes the behavior and assigns a result when applicable.
* The third block asserts and verifies.
* Do not add extra blank lines.
* Do not add blank lines inside an implicit block.

For async tests, prefer:

```kotlin
val deferred = async(start = CoroutineStart.UNDISPATCHED) { useCase(...) }
val result = deferred.await()
```

## Code Style

* Keep lines at 120 characters maximum.
* Prefer immutable values and `val`.
* Use meaningful names.
* Keep classes and functions focused.
* Prefer composition over inheritance.
* Prefer explicit code over clever compact code.
* When a function or constructor has more than one argument, prefer multiline formatting.
* Extract repeated literals into local values or constants.
* Replace `.invoke()` with `()`.

## Verification

Run the narrowest verification that proves the change, then broaden when the change affects shared behavior.

Common commands:

```bash
./gradlew ktlintCheck
./gradlew detekt
./gradlew lintDebug
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

CI runs:

* `ktlintCheck`
* `detekt`
* `lintDebug`
* `assembleDebug`
* `testDebugUnitTest`

If a command cannot be run locally because secrets, SDKs, network access, or environment setup are missing, say that explicitly.

## Agent Workflow

For non-trivial changes:

1. Inspect the existing implementation before editing.
2. Identify affected modules, tests, and verification commands.
3. Make a small plan before changing files.
4. Keep edits scoped to the requested behavior.
5. Add or update tests when behavior changes.
6. Run relevant verification.
7. Summarize changed files, verification, and remaining risks.

For large or risky changes:

* Split the work into smaller coherent steps.
* Prefer separate commits for separate concerns when asked to commit.
* Use Git worktrees for parallel agent work on different branches.
* Do not run multiple agents against the same files unless the write scopes are clear.

## Creating New Features

When creating a feature:

* Place UI in the presentation layer or feature module that owns the screen.
* Place business behavior in use cases.
* Place data access behind repositories.
* Default implementation of an interface uses the **`Default` prefix** (`DefaultFooRepository`),
  never the `Impl` suffix. Other data-layer classes are named after their mechanism
  (`AssetQuizRepository`, `BestStreakDataStore`).
* Add mappers at layer boundaries.
* Add tests at the lowest useful level first.
* Follow the naming and package structure of nearby features.

## Pull Request Review

When reviewing a PR:

* Lead with bugs, regressions, architecture risks, and missing tests.
* Reference exact files and lines.
* Keep summaries secondary to findings.
* If no issues are found, say that clearly and mention any residual test or verification gaps.

## When Unsure

* Prefer consistency with nearby code.
* Make the safest architectural assumption.
* Keep the change minimal.
* Ask only when the decision cannot be inferred and the wrong choice would be costly.

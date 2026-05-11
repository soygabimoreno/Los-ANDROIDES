---
name: create-usecase
description: Generate a Kotlin UseCase following Clean Architecture for Android projects
---

# Purpose

Generate a UseCase class located in the domain layer following Clean Architecture
principles.

# Rules

* Use Kotlin
* The UseCase must contain a single responsibility
* Prefer `operator fun invoke`
* The UseCase must not depend on Android APIs
* Dependencies must be injected through the constructor
* The UseCase must interact with a repository interface defined in the domain layer
* Keep the UseCase small and focused

# Structure

Use this structure:

```kotlin
class ExampleUseCase(
    private val repository: ExampleRepository
) {

    suspend operator fun invoke(param: String): ResultType {
        return repository.doSomething(param)
    }
}
```

# Guidelines

* Place the UseCase inside the `domain` layer
* The repository must be an interface defined in the domain layer
* Do not include Android dependencies
* Keep the implementation simple
* Prefer descriptive naming

# When to use this skill

Use this skill when the user asks to:

* create a use case
* implement business logic
* generate a domain action
* encapsulate business rules

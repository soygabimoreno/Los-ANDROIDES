---
name: android-typed-errors
description: |
  Generic Result wrapper, typed errors, and extension helpers for Android/KMP:
  Result<T, E>, DataError, EmptyResult, map, mapError, onSuccess, onFailure, fold.
  Trigger on: "Result wrapper", "error handling", "DataError", "onSuccess", "onFailure",
  "EmptyResult", "map result", "error type", "validation error", "typed errors".
---

# Android / KMP Typed Error Handling

## Result Wrapper (`core:domain`)

A generic, typed Result that works across all layers — data, domain, presentation,
validation, anywhere a function can succeed or fail with a typed error.

```kotlin
interface Error

sealed interface Result<out D, out E : Error> {
    data class Success<out D>(val data: D) : Result<D, Nothing>
    data class Error<out E : com.example.Error>(val error: E) : Result<Nothing, E>
}

typealias EmptyResult<E> = Result<Unit, E>
```

---

## Extension Helpers (`core:domain`)

These live alongside the `Result` definition:

```kotlin
inline fun <T, E : Error, R> Result<T, E>.map(
    transform: (T) -> R
): Result<R, E> = when (this) {
    is Result.Error -> Result.Error(error)
    is Result.Success -> Result.Success(transform(data))
}

inline fun <T, E : Error, R : Error> Result<T, E>.mapError(
    transform: (E) -> R
): Result<T, R> = when (this) {
    is Result.Error -> Result.Error(transform(error))
    is Result.Success -> Result.Success(data)
}

inline fun <T, E : Error> Result<T, E>.onSuccess(
    action: (T) -> Unit
): Result<T, E> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T, E : Error> Result<T, E>.onFailure(
    action: (E) -> Unit
): Result<T, E> {
    if (this is Result.Error) action(error)
    return this
}

inline fun <T, E : Error, R> Result<T, E>.fold(
    onSuccess: (T) -> R,
    onFailure: (E) -> R
): R = when (this) {
    is Result.Success -> onSuccess(data)
    is Result.Error -> onFailure(error)
}

fun <T, E : Error> Result<T, E>.asEmptyResult(): EmptyResult<E> = map { }
```

All helpers return `Result` so they can be chained:

```kotlin
repository.saveNote(note)
    .onSuccess { updateUi() }
    .onFailure { error -> showSnackbar(error.toUiText()) }
    .asEmptyResult()
```

Use `fold` when you need to branch into two different return types:

```kotlin
val message = result.fold(
    onSuccess = { "Saved: ${it.title}" },
    onFailure = { it.toUiText().asString(context) }
)
```

Use `mapError` to lift a layer-specific error to a broader type (e.g., when a
repository wraps both a local and a remote data source):

```kotlin
// Map DataError.Local → DataError so both sources share the same error type
fun NoteLocalDataSource.getNotesMapped(): Result<List<Note>, DataError> =
    getNotes().mapError { it }
```

---

## Shared Error Types (`core:domain`)

### DataError

```kotlin
sealed interface DataError : Error {
    enum class Network : DataError {
        BAD_REQUEST,
        REQUEST_TIMEOUT,
        UNAUTHORIZED,
        FORBIDDEN,
        NOT_FOUND,
        CONFLICT,
        TOO_MANY_REQUESTS,
        NO_INTERNET,
        PAYLOAD_TOO_LARGE,
        SERVER_ERROR,
        SERVICE_UNAVAILABLE,
        SERIALIZATION,
        UNKNOWN
    }

    enum class Local : DataError {
        DISK_FULL,
        NOT_FOUND,
        UNKNOWN
    }
}
```

### Feature-Specific Errors

Features define their own error types by implementing `Error`:

```kotlin
enum class PasswordValidationError : Error {
    TOO_SHORT,
    NO_UPPERCASE,
    NO_DIGIT
}

fun validatePassword(pw: String): EmptyResult<PasswordValidationError>
```

Always return a single error type per `Result` — not a list of errors.

---

## Exception Handling Philosophy

Never throw exceptions for expected failures — always return `Result.Error`. Catch
exceptions at the layer responsible for them:

| Exception origin | Catch in | Mapped to |
|---|---|---|
| HTTP / network | Data layer | `DataError.Network.*` |
| Database / disk | Data layer | `DataError.Local.*` |
| Business logic | Domain layer | Feature-specific `Error` |
| Presentation | Presentation layer | `Result.Error` at that layer |

Upper layers never see raw exceptions for expected failures.

---

## Mapping Errors to UiText

Every error type displayed to the user should have a `.toUiText()` extension:

- **Feature's `presentation` module** — for feature-specific errors
  (e.g., `AuthError.toUiText()`)
- **`core:presentation`** — for shared errors across features
  (e.g., `DataError.toUiText()`)

Errors that are purely internal and never shown to the user don't need a mapping.

```kotlin
// core:presentation
fun DataError.toUiText(): UiText = when (this) {
    DataError.Network.NO_INTERNET ->
        UiText.StringResource(R.string.error_no_internet)
    DataError.Network.SERVER_ERROR ->
        UiText.StringResource(R.string.error_server)
    DataError.Network.UNAUTHORIZED ->
        UiText.StringResource(R.string.error_unauthorized)
    DataError.Local.DISK_FULL ->
        UiText.StringResource(R.string.error_disk_full)
    else -> UiText.StringResource(R.string.error_unknown)
}
```

---

## Safe Call Helpers (`core:data`)

Typed extension functions on `HttpClient` that wrap Ktor calls and map HTTP responses
to `Result<T, DataError.Network>`:

```kotlin
suspend inline fun <reified Response : Any> HttpClient.get(
    route: String,
    queryParameters: Map<String, Any?> = mapOf()
): Result<Response, DataError.Network> = safeCall {
    get {
        url(constructRoute(route))
        queryParameters.forEach { (key, value) -> parameter(key, value) }
    }
}

suspend inline fun <reified Request, reified Response : Any> HttpClient.post(
    route: String,
    body: Request
): Result<Response, DataError.Network> = safeCall {
    post {
        url(constructRoute(route))
        setBody(body)
    }
}

suspend inline fun <reified T> safeCall(
    execute: () -> HttpResponse
): Result<T, DataError.Network> {
    val response = try {
        execute()
    } catch (e: UnresolvedAddressException) {
        return Result.Error(DataError.Network.NO_INTERNET)
    } catch (e: SerializationException) {
        return Result.Error(DataError.Network.SERIALIZATION)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        return Result.Error(DataError.Network.UNKNOWN)
    }
    return responseToResult(response)
}

suspend inline fun <reified T> responseToResult(
    response: HttpResponse
): Result<T, DataError.Network> = when (response.status.value) {
    in 200..299 -> Result.Success(response.body<T>())
    401 -> Result.Error(DataError.Network.UNAUTHORIZED)
    408 -> Result.Error(DataError.Network.REQUEST_TIMEOUT)
    409 -> Result.Error(DataError.Network.CONFLICT)
    413 -> Result.Error(DataError.Network.PAYLOAD_TOO_LARGE)
    429 -> Result.Error(DataError.Network.TOO_MANY_REQUESTS)
    in 500..599 -> Result.Error(DataError.Network.SERVER_ERROR)
    else -> Result.Error(DataError.Network.UNKNOWN)
}

fun constructRoute(route: String): String = when {
    route.contains(BuildConfig.BASE_URL) -> route
    route.startsWith("/") -> BuildConfig.BASE_URL + route
    else -> "${BuildConfig.BASE_URL}/$route"
}
```

---

## When to Use What

| Scenario | Error type | Return type |
|---|---|---|
| Network call | `DataError.Network` | `Result<List<NoteDto>, DataError.Network>` |
| Local DB access | `DataError.Local` | `Result<Note, DataError.Local>` |
| Repository (multi-source) | `DataError` (supertype) | `Result<List<Note>, DataError>` |
| Domain validation | Custom `Error` enum | `EmptyResult<PasswordValidationError>` |
| Auth logic | Custom `Error` enum | `Result<User, AuthError>` |

The `Result` wrapper is not limited to the data layer — use it anywhere a function
has typed success and failure outcomes.

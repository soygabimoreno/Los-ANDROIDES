---
name: android-data-architecture
description: |
  Data layer patterns for Android/KMP: data sources, repositories, DTOs, mappers,
  Room entities, Ktor HttpClient, safe call helpers, token storage, and offline-first.
  Trigger on: "create a repository", "create a data source", "add a DAO", "Ktor client",
  "write a mapper", "DTO", "Room entity", "network call", "token storage", "offline-first".
---

# Android / KMP Data Architecture

## Error Handling

This skill uses `Result<T, E>`, `DataError`, and the extension helpers defined in the
**android-typed-errors** skill. Refer to that skill for the full `Result` wrapper,
`DataError` sealed interface, and `map`/`onSuccess`/`onFailure`/`asEmptyResult` extensions.

---

## Data Source vs Repository

- **Data source** — accesses a single source (local DB, remote API, file system).
  Most classes in the data layer are data sources.
- **Repository** — coordinates multiple data sources (e.g., remote API + local DB for
  offline-first). Only use this abstraction when the class genuinely combines sources.

```kotlin
// Single source → data source
interface NoteLocalDataSource {
    suspend fun getNotes(): Result<List<Note>, DataError.Local>
    suspend fun insertNote(note: Note): EmptyResult<DataError.Local>
}

interface NoteRemoteDataSource {
    suspend fun fetchNotes(): Result<List<Note>, DataError.Network>
}

// Multiple sources → repository
interface NoteRepository {
    suspend fun getNotes(): Result<List<Note>, DataError>
    suspend fun sync(): EmptyResult<DataError>
}
```

## Domain Layer Contracts

- Pure Kotlin — no Android/framework imports.
- Contains: domain models, data source/repository **interfaces**, error types.
- **Every data source or repository used by a ViewModel must have an interface in
  `domain`** — enforces that `presentation` never depends on `data`, and enables testing.

---

## DTOs and Domain Models

- Always separate: DTOs (data layer) ↔ Domain Models (domain layer).
- Domain models never go directly into Room entities or Ktor request/response bodies.
- Mappers are simple extension functions living in the data layer alongside the DTO:

```kotlin
fun NoteDto.toNote(): Note = Note(id = id, title = title, ...)
fun Note.toNoteDto(): NoteDto = NoteDto(id = id, title = title, ...)
fun NoteEntity.toNote(): Note = ...
fun Note.toNoteEntity(): NoteEntity = ...
```

---

## Implementations

Name implementations for what makes them unique — never suffix with `Impl`.

### Data source (single source)

```kotlin
class RoomNoteDataSource(private val dao: NoteDao) : NoteLocalDataSource {
    override suspend fun getNotes(): Result<List<Note>, DataError.Local> {
        return try {
            Result.Success(dao.getAllNotes().map { it.toNote() })
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }
}
```

### Observing with Flow

For data sources that need to emit continuous updates (e.g., a Room DAO reacting to
DB changes), expose a `Flow` alongside the suspend functions:

```kotlin
interface NoteLocalDataSource {
    fun observeNotes(): Flow<List<Note>>
    suspend fun insertNote(note: Note): EmptyResult<DataError.Local>
}

class RoomNoteDataSource(private val dao: NoteDao) : NoteLocalDataSource {
    override fun observeNotes(): Flow<List<Note>> =
        dao.observeAllNotes().map { entities -> entities.map { it.toNote() } }
}
```

The ViewModel collects the `Flow` directly — no manual refresh needed.

### Repository (multiple sources)

```kotlin
class OfflineFirstNoteRepository(
    private val local: NoteLocalDataSource,
    private val remote: NoteRemoteDataSource
) : NoteRepository {
    override suspend fun getNotes(): Result<List<Note>, DataError> {
        val remoteResult = remote.fetchNotes()
        return when (remoteResult) {
            is Result.Success -> {
                local.insertAll(remoteResult.data)
                local.getNotes()
            }
            is Result.Error -> local.getNotes()
        }
    }
}
```

Use names like `RoomNoteDataSource`, `KtorNoteDataSource`, `OfflineFirstNoteRepository`.
The name should tell you what the class wraps or how it behaves.

---

## Ktor — HttpClient Factory (`core:data`)

Configure the client once. Accept the engine externally so tests can swap in a mock engine:

```kotlin
object HttpClientFactory {
    fun create(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
        install(ContentNegotiation) { json() }
        install(Auth) {
            bearer {
                loadTokens { /* load from DataStore */ }
                refreshTokens { /* call refresh endpoint, save new tokens */ }
            }
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
        defaultRequest { contentType(ContentType.Application.Json) }
    }
}
```

Inject `HttpClient` via Koin. For KMP, use the platform default engine.

---

## Ktor — Safe Call Helpers (`core:data`)

Use `safeCall` / `responseToResult` helpers and typed extension functions
(`HttpClient.get`, `HttpClient.post`, `HttpClient.delete`) to keep data source call
sites clean and uniform. See the **android-typed-errors** skill for the full
implementation of these helpers.

```kotlin
suspend fun getNotes(): Result<List<NoteDto>, DataError.Network> {
    return httpClient.get(route = "/notes")
}
```

---

## Room — Key Patterns

Use `@Transaction` for any DAO operation that reads or writes multiple tables atomically:

```kotlin
@Transaction
@Query("SELECT * FROM notes WHERE id = :id")
suspend fun getNoteWithTags(id: String): NoteWithTagsEntity
```

Prefer `autoMigrations` for simple schema changes. Use manual `Migration` objects when
the change is too complex for auto-migration:

```kotlin
@Database(
    entities = [NoteEntity::class],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)]
)
```

---

## Token Storage

Store tokens in DataStore (in `core:data` or a dedicated `:core:auth` module). The
Ktor `Auth` plugin reads/writes tokens and handles 401 refresh automatically.

---

## Offline-First (when applicable)

Follow **Room as single source of truth**: fetch from network → persist to Room →
expose DB `Flow` to the ViewModel. The ViewModel never observes network responses
directly. Apply this pattern only when the project requires offline support.

---

## Naming Conventions

| Thing | Convention | Example |
|---|---|---|
| Data source interface | `<Entity><Local/Remote>DataSource` | `NoteLocalDataSource` |
| Data source impl | describe what makes it unique | `RoomNoteDataSource` |
| Repository interface | `<Entity>Repository` (multi-source only) | `NoteRepository` |
| Repository impl | describe what makes it unique | `OfflineFirstNoteRepository` |
| DTO | `<Model>Dto` | `NoteDto` |
| Room entity | `<Model>Entity` | `NoteEntity` |
| Mapper | extension fun on source type | `fun NoteDto.toNote()` |

---

## Checklist: Adding a New Data Source or Repository

- [ ] Define domain model(s) in `feature:domain`
- [ ] Define data source or repository interface in `feature:domain`
- [ ] Define feature-specific error types in `feature:domain` (implement `Error`)
- [ ] Define DTOs and Room entities in `feature:data`
- [ ] Write mappers as extension functions in `feature:data`
- [ ] Implement the data source or repository in `feature:data`, named for what makes
      it unique

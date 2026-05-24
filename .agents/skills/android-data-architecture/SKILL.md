---
name: android-data-architecture
description: |
  Data layer patterns for Los ANDROIDES: data sources, repositories, DTOs, mappers,
  Room entities, Retrofit + Moshi networking, OkHttp interceptors, DataStore for tokens,
  Hilt provisioning, and offline-first. Trigger on: "create a repository",
  "create a data source", "add a DAO", "Retrofit service", "Moshi adapter",
  "write a mapper", "DTO", "Room entity", "network call", "token storage",
  "offline-first", "OkHttp interceptor".
---

# Data architecture — Los ANDROIDES

## Error handling

This skill uses `Result<T, E>`, `DataError`, and the extension helpers defined in the
**android-typed-errors** skill. Refer to that skill for the full `Result` wrapper,
`DataError` sealed interface, and `map`/`onSuccess`/`onFailure`/`asEmptyResult` extensions.

---

## Data source vs Repository

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

## Domain layer contracts

- Pure Kotlin — no Android/framework imports.
- Contains: domain models, data source/repository **interfaces**, error types.
- **Every data source or repository used by a ViewModel must have an interface in
  `domain`** — enforces that `presentation` never depends on `data`, and enables testing.

---

## DTOs and domain models

- Always separate: DTOs (data layer) ↔ Domain Models (domain layer).
- Domain models never go directly into Room entities or Retrofit request/response bodies.
- Mappers are simple extension functions living in the data layer alongside the DTO:

```kotlin
fun NoteDto.toNote(): Note = Note(id = id, title = title, ...)
fun Note.toNoteDto(): NoteDto = NoteDto(id = id, title = title, ...)
fun NoteEntity.toNote(): Note = ...
fun Note.toNoteEntity(): NoteEntity = ...
```

Annotate DTOs with `@JsonClass(generateAdapter = true)` so Moshi codegen generates the
adapter at build time:

```kotlin
@JsonClass(generateAdapter = true)
data class NoteDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "body") val body: String,
)
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
    private val remote: NoteRemoteDataSource,
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

Use names like `RoomNoteDataSource`, `RetrofitNoteDataSource`,
`OfflineFirstNoteRepository`. The name should tell you what the class wraps or how
it behaves.

---

## Retrofit — Service interfaces (feature module)

Define a Retrofit service per feature in its `data` layer:

```kotlin
interface NoteService {
    @GET("notes")
    suspend fun getNotes(): List<NoteDto>

    @POST("notes")
    suspend fun createNote(@Body note: NoteDto): NoteDto

    @DELETE("notes/{id}")
    suspend fun deleteNote(@Path("id") id: String)
}
```

Suspend functions are first-class in Retrofit — no `Call<T>` wrappers. Throwing
exceptions are mapped to `DataError.Network` in the data source layer via the
helpers from **android-typed-errors**.

---

## Retrofit + Moshi + OkHttp wiring (Hilt, shared `core` module)

Configure Retrofit once. Inject `OkHttpClient` and `Moshi` separately so they can be
shared and customized:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi =
        Moshi.Builder()
            // .add(KotlinJsonAdapterFactory()) // only if not using codegen
            .build()

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenStore: TokenStore): Interceptor =
        Interceptor { chain ->
            val token = runBlocking { tokenStore.accessToken() }
            val request = chain.request().newBuilder()
                .apply { token?.let { addHeader("Authorization", "Bearer $it") } }
                .build()
            chain.proceed(request)
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: Interceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    fun provideNoteService(retrofit: Retrofit): NoteService =
        retrofit.create(NoteService::class.java)
}
```

`logging-interceptor` is in the version catalog (`com.squareup.okhttp3:logging-interceptor`).

---

## Safe call helpers

Wrap Retrofit service calls with the `safeCall` / `responseToResult` helpers from
**android-typed-errors** so data source call sites stay clean and uniform:

```kotlin
class RetrofitNoteDataSource(
    private val service: NoteService,
) : NoteRemoteDataSource {
    override suspend fun fetchNotes(): Result<List<NoteDto>, DataError.Network> =
        safeCall { service.getNotes() }
}
```

The helper catches `IOException`, `HttpException`, and maps HTTP status codes to
`DataError.Network` variants.

---

## Room — key patterns

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
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class AppDatabase : RoomDatabase()
```

Provide the database and DAOs via Hilt:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "app.db")
            .build()

    @Provides
    fun provideNoteDao(db: AppDatabase): NoteDao = db.noteDao()
}
```

---

## Token storage

Store tokens in DataStore (`androidx.datastore:datastore-preferences`). Wrap access in
a `TokenStore` interface so the `OkHttp` interceptor depends on the contract, not on
DataStore directly:

```kotlin
interface TokenStore {
    suspend fun accessToken(): String?
    suspend fun save(access: String, refresh: String)
    suspend fun clear()
}
```

For 401 refresh, implement an `okhttp3.Authenticator` that calls the refresh endpoint
and retries the original request with the new token.

---

## Offline-first (when applicable)

Follow **Room as single source of truth**: fetch from network → persist to Room →
expose DB `Flow` to the ViewModel. The ViewModel never observes network responses
directly. Apply this pattern only when the project requires offline support.

---

## Naming conventions

| Thing | Convention | Example |
|---|---|---|
| Data source interface | `<Entity><Local/Remote>DataSource` | `NoteLocalDataSource` |
| Data source impl | describe what makes it unique | `RoomNoteDataSource`, `RetrofitNoteDataSource` |
| Repository interface | `<Entity>Repository` (multi-source only) | `NoteRepository` |
| Repository impl | describe what makes it unique | `OfflineFirstNoteRepository` |
| DTO | `<Model>Dto` | `NoteDto` |
| Retrofit service | `<Entity>Service` | `NoteService` |
| Room entity | `<Model>Entity` | `NoteEntity` |
| Room DAO | `<Entity>Dao` | `NoteDao` |
| Mapper | extension fun on source type | `fun NoteDto.toNote()` |

---

## Checklist — adding a new data source or repository

- [ ] Define domain model(s) in the feature's `domain` layer
- [ ] Define data source or repository interface in the feature's `domain` layer
- [ ] Define feature-specific error types in `domain` (implement `Error`)
- [ ] Define DTOs (with `@JsonClass(generateAdapter = true)`) and Room entities in `data`
- [ ] Write mappers as extension functions in `data`
- [ ] Define the Retrofit service interface in `data` (or in `:core:core` if shared)
- [ ] Implement the data source or repository, named for what makes it unique
- [ ] Provide via Hilt `@Module @InstallIn(SingletonComponent::class)` (binding
      interface to impl with `@Binds`, or constructing impl with `@Provides`)

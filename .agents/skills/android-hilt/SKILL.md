---
name: android-hilt
description: |
  Hilt dependency injection for Android: setup, module definitions, scopes, ViewModel
  injection, component hierarchy, and testing with HiltAndroidTest. Android-only — not
  available for KMP. Trigger on: "set up Hilt", "add Hilt", "@HiltViewModel",
  "@AndroidEntryPoint", "@InstallIn", "@Provides", "@Binds", "Hilt module",
  "Hilt testing", "inject with Hilt".
---

# Android Dependency Injection — Hilt

## When to Use Hilt vs Koin

- **Hilt** — Android-only projects. Provides compile-time dependency graph validation,
  better IDE support, and tighter integration with Android Jetpack.
- **Koin** — KMP or projects that need runtime DI without annotation processing.

Hilt requires kapt or KSP and cannot be used in KMP shared modules.

---

## Setup

### Gradle

```kotlin
// build.gradle.kts (app or android-library)
plugins {
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp) // or kapt
}

dependencies {
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler) // or kapt(libs.hilt.compiler)
}
```

### Application class

```kotlin
@HiltAndroidApp
class App : Application()
```

### Activities / Fragments

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() { ... }
```

---

## ViewModel Injection

Annotate the ViewModel with `@HiltViewModel` and its constructor with `@Inject`:

```kotlin
@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() { ... }
```

Inject in Compose with `hiltViewModel()`:

```kotlin
@Composable
fun NoteListRoot(
    onNavigateToDetail: (String) -> Unit,
    viewModel: NoteListViewModel = hiltViewModel()
) { ... }
```

Inject in a Fragment with `by viewModels()` (no extra setup needed once
`@AndroidEntryPoint` is applied to the Fragment):

```kotlin
@AndroidEntryPoint
class NoteListFragment : Fragment() {
    private val viewModel: NoteListViewModel by viewModels()
}
```

---

## Module Definitions

Use `@Module` + `@InstallIn` to provide dependencies. Prefer `@Binds` (abstract
function) over `@Provides` when binding an implementation to an interface — it
generates less code:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class NotesDataModule {

    // @Binds — preferred for interface→impl bindings
    @Binds
    @Singleton
    abstract fun bindNoteRepository(
        impl: OfflineFirstNoteRepository
    ): NoteRepository

    companion object {
        // @Provides — required for factory methods or third-party types
        @Provides
        @Singleton
        fun provideHttpClient(engine: HttpClientEngine): HttpClient =
            HttpClientFactory.create(engine)
    }
}
```

`@Binds` and `@Provides` cannot be in the same class directly — put `@Provides` in a
`companion object` inside the abstract module, or in a separate `@Module`.

---

## Scopes

| Scope annotation | Component | Lifetime |
|---|---|---|
| `@Singleton` | `SingletonComponent` | App lifetime |
| `@ActivityRetainedScoped` | `ActivityRetainedComponent` | Survives config changes |
| `@ViewModelScoped` | `ViewModelComponent` | ViewModel lifetime |
| `@ActivityScoped` | `ActivityComponent` | Activity lifetime |

Use `@Singleton` for repositories, HTTP clients, and databases.
Use `@ViewModelScoped` for dependencies that should be tied to a single ViewModel
instance (e.g., a use case that holds ViewModel-scoped state).

---

## Component Hierarchy

```
SingletonComponent
  └── ActivityRetainedComponent
        ├── ViewModelComponent
        └── ActivityComponent
              └── FragmentComponent
                    └── ViewComponent
```

A binding installed in a parent component is available to all child components.
A `@Singleton` binding is available everywhere; a `@ViewModelScoped` binding is only
available within the `ViewModelComponent`.

---

## Providing Platform Types

Use `@ApplicationContext` and `@ActivityContext` qualifiers to inject Android contexts:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "app.db").build()

    @Provides
    fun provideNoteDao(db: AppDatabase): NoteDao = db.noteDao()
}
```

---

## Testing with Hilt

Annotate the test class with `@HiltAndroidTest` and use `HiltAndroidRule`:

```kotlin
@HiltAndroidTest
class NoteListViewModelTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var repository: NoteRepository

    @Before
    fun setUp() {
        hiltRule.inject()
    }
}
```

To replace a binding with a test fake, use `@UninstallModules` and define a
`@TestInstallIn` replacement:

```kotlin
@UninstallModules(NotesDataModule::class)
@HiltAndroidTest
class NoteListViewModelTest { ... }

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [NotesDataModule::class]
)
abstract class FakeNotesDataModule {
    @Binds
    @Singleton
    abstract fun bindFakeRepository(impl: FakeNoteRepository): NoteRepository
}
```

For unit tests that don't need Android components, test the class directly by
injecting fakes via the constructor — no Hilt setup required.

---

## Checklist: Adding Hilt to a New Feature

- [ ] Annotate ViewModel with `@HiltViewModel` and constructor with `@Inject`
- [ ] Create `@Module @InstallIn(SingletonComponent::class)` for data layer bindings
- [ ] Use `@Binds` for interface→impl, `@Provides` for factory methods
- [ ] Apply correct scope annotation (`@Singleton` for shared, `@ViewModelScoped` for
      per-ViewModel)
- [ ] Annotate entry-point Activity / Fragment with `@AndroidEntryPoint`
- [ ] Use `hiltViewModel()` in Compose Root composables

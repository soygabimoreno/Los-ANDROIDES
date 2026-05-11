---
name: android-unit-testing
description: |
  Deep-dive unit testing for Android/KMP: GIVEN/WHEN/THEN naming, MockK, Kluent,
  coroutine testing with runTest, Flow testing with Turbine, fakes vs mocks, subject
  under test conventions. Trigger on: "unit test", "write a test for", "test this
  function", "test naming", "MockK", "Kluent", "runTest", "Turbine", "fake vs mock".
---

# Android / KMP Unit Testing

## Test Naming — GIVEN / WHEN / THEN

Every test name follows the pattern:

```
`GIVEN <precondition> WHEN <action> THEN <expected outcome>`
```

```kotlin
@Test
fun `GIVEN notes in repository WHEN refresh clicked THEN state contains notes`()

@Test
fun `GIVEN network error WHEN refresh clicked THEN snackbar event is emitted`()

@Test
fun `GIVEN blank password WHEN validate THEN TOO_SHORT error is returned`()
```

Do **not** add `// GIVEN`, `// WHEN`, or `// THEN` comments inside the test body.
The name is enough.

---

## Stack

| Concern | Library |
|---|---|
| Test framework | JUnit5 |
| Assertions | Kluent |
| Mocking | MockK |
| Flow / StateFlow testing | Turbine |
| Coroutine testing | `kotlinx-coroutines-test` + `UnconfinedTestDispatcher` |

---

## Subject Under Test Convention

Declare the subject under test as `lateinit var` immediately before `@BeforeEach`.
Use a simplified name — drop the feature prefix:

| Class | Variable name |
|---|---|
| `NoteListViewModel` | `viewModel` |
| `NoteRepository` | `repository` |
| `ValidatePasswordUseCase` | `useCase` |
| `NoteListViewModel` | `viewModel` |

```kotlin
class NoteListViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val noteRepository = mockk<NoteRepository>()

    private lateinit var viewModel: NoteListViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = NoteListViewModel(noteRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
```

---

## Kluent Assertions

```kotlin
// Equality
result shouldBe Result.Success(emptyList<Note>())
result shouldBeEqualTo expected

// Nullability
error shouldNotBe null
error shouldBe null

// Booleans — always enabled test first, disabled second
isLoading shouldBe true
isLoading shouldBe false

// Collections
notes shouldHaveSize 3
notes shouldContain note
notes.shouldBeEmpty()

// Type assertions
result.shouldBeInstanceOf<Result.Error<*>>()
```

---

## MockK Conventions

Use `relaxedMockk<T>()` instead of `mockk<T>(relaxed = true)`:

```kotlin
val callback: () -> Unit = relaxedMockk()
val tracker: AnalyticsTracker = relaxedMockk()
```

Use `verifyOnce` instead of `verify(exactly = 1)` and `verifyNever` instead of
`verify(inverse = true)`:

```kotlin
verifyOnce { noteRepository.insertNote(any()) }
verifyNever { noteRepository.deleteNote(any()) }
```

Replace `.invoke()` calls with `()` — it's an operator fun:

```kotlin
// Bad
callback.invoke()

// Good
callback()
```

---

## Testing ViewModels — State with Turbine

```kotlin
@Test
fun `GIVEN notes in repository WHEN refresh clicked THEN state contains notes`() = runTest {
    val notes = listOf(Note(id = "1", title = "Meeting"))
    coEvery { noteRepository.getNotes() } returns Result.Success(notes)

    viewModel.state.test {
        viewModel.onAction(NoteListAction.OnRefreshClick)
        awaitItem().isLoading shouldBe true
        awaitItem().notes shouldHaveSize 1
    }
}
```

---

## Testing ViewModels — Events

```kotlin
@Test
fun `GIVEN valid note id WHEN note clicked THEN NavigateToDetail event is emitted`() = runTest {
    viewModel.events.test {
        viewModel.onAction(NoteListAction.OnNoteClick("123"))
        awaitItem() shouldBe NoteListEvent.NavigateToDetail("123")
        cancelAndIgnoreRemainingEvents()
    }
}
```

---

## Testing ViewModels — Error States

```kotlin
@Test
fun `GIVEN network error WHEN refresh clicked THEN ShowSnackbar event is emitted`() = runTest {
    coEvery { noteRepository.getNotes() } returns Result.Error(DataError.Network.NO_INTERNET)

    viewModel.events.test {
        viewModel.onAction(NoteListAction.OnRefreshClick)
        awaitItem().shouldBeInstanceOf<NoteListEvent.ShowSnackbar>()
        cancelAndIgnoreRemainingEvents()
    }
}
```

---

## Testing Boolean States — Order Convention

When writing two tests to verify a boolean flag, always write the **enabled** test
first and the **disabled** test second:

```kotlin
@Test
fun `GIVEN loading in progress WHEN state observed THEN isLoading is true`() = runTest {
    coEvery { noteRepository.getNotes() } coAnswers { delay(100); Result.Success(emptyList()) }
    viewModel.onAction(NoteListAction.OnRefreshClick)
    viewModel.state.value.isLoading shouldBe true
}

@Test
fun `GIVEN loading finished WHEN state observed THEN isLoading is false`() = runTest {
    coEvery { noteRepository.getNotes() } returns Result.Success(emptyList())
    viewModel.onAction(NoteListAction.OnRefreshClick)
    viewModel.state.value.isLoading shouldBe false
}
```

---

## Fakes vs Mocks

| Situation | Use |
|---|---|
| Your own interface (repository, data source) | **Fake** — simple in-memory impl |
| Third-party class you cannot implement | **MockK** |
| Verifying a specific call was made | MockK + `verifyOnce` / `verifyNever` |

Fakes exercise the full contract and catch more real bugs. Use MockK only when a fake
would be impractical.

```kotlin
class FakeNoteRepository : NoteRepository {
    private val notes = mutableListOf<Note>()
    var shouldReturnError = false

    fun givenNotes(vararg note: Note) = notes.addAll(note)

    override suspend fun getNotes(): Result<List<Note>, DataError.Local> =
        if (shouldReturnError) Result.Error(DataError.Local.UNKNOWN)
        else Result.Success(notes.toList())
}
```

---

## Testing Pure Kotlin Classes

```kotlin
class ValidatePasswordUseCaseTest {

    private lateinit var useCase: ValidatePasswordUseCase

    @BeforeEach
    fun setUp() {
        useCase = ValidatePasswordUseCase()
    }

    @Test
    fun `GIVEN password shorter than 8 chars WHEN validate THEN TOO_SHORT error is returned`() {
        val result = useCase("abc")
        result shouldBe Result.Error(PasswordValidationError.TOO_SHORT)
    }

    @Test
    fun `GIVEN valid password WHEN validate THEN success is returned`() {
        val result = useCase("Secure1!")
        result.shouldBeInstanceOf<Result.Success<*>>()
    }
}
```

Note: `useCase("Secure1!")` works because `invoke` is an operator — no need for
`useCase.invoke("Secure1!")`.

---

## What to Unit Test

- Every ViewModel (state transitions, events, error handling).
- Every non-trivial domain class (validators, use cases, mappers with logic).
- Data source error mapping (HTTP codes → `DataError`).
- Any logic that is likely to change or has edge cases.

Do **not** unit test simple data classes, mappers with no conditional logic, DI modules,
or nav graph wiring.

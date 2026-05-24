---
name: android-tests
description: |
  Add or update unit tests in Los ANDROIDES using the project's strict MockK, Kluent,
  Turbine, runTest, GIVEN/WHEN/THEN conventions on JUnit4. Trigger on: "write a test",
  "unit test", "test the ViewModel", "test a repository", "MockK", "Kluent", "Turbine",
  "runTest", "fake repository", "GIVEN WHEN THEN".
---

# Android tests — Los ANDROIDES

Use this skill when adding or updating tests in this repository.

First read `AGENTS.md` from the repository root, especially the Testing section.

## Stack

| Concern | Library |
|---|---|
| Test framework | JUnit 4 (`junit:junit:4.13.2`) |
| Assertions | Kluent |
| Mocking | MockK |
| Flow / StateFlow | Turbine |
| Coroutines | `kotlinx-coroutines-test` + `UnconfinedTestDispatcher` |
| UI testing | `ComposeTestRule` (`androidx.compose.ui:ui-test-junit4`) |

Avoid Robolectric in new tests (the catalog has it for legacy tests only).

---

## Workflow

1. Inspect nearby tests for naming, fixture, mock, assertion, and verification style.
2. Use MockK and Kluent. Typed mocks explicitly.
3. Follow `GIVEN ... WHEN ... THEN ...` test names.
4. Keep exactly three implicit blocks separated by exactly two blank lines.
5. Do not write `GIVEN`, `WHEN`, or `THEN` inside the test body.
6. Use `verifyOnce`, `verifyNever`, `coVerifyOnce`, or `coVerifyNever`.
7. Keep test data deterministic.
8. Run the relevant test task after editing.

```bash
./gradlew testDebugUnitTest
```

For a narrow module check, use the matching module test task when available.

---

## Test naming — GIVEN / WHEN / THEN

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

## Subject under test convention

Declare the subject under test as `lateinit var` immediately before `@Before`. Use a
simplified name — drop the feature prefix:

| Class | Variable name |
|---|---|
| `NoteListViewModel` | `viewModel` |
| `NoteRepository` | `repository` |
| `ValidatePasswordUseCase` | `useCase` |

```kotlin
class NoteListViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val noteRepository = mockk<NoteRepository>()

    private lateinit var viewModel: NoteListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = NoteListViewModel(noteRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
```

---

## Kluent assertions

```kotlin
result shouldBe Result.Success(emptyList<Note>())
result shouldBeEqualTo expected

error shouldNotBe null
error shouldBe null

isLoading shouldBe true
isLoading shouldBe false

notes shouldHaveSize 3
notes shouldContain note
notes.shouldBeEmpty()

result.shouldBeInstanceOf<Result.Error<*>>()
```

---

## MockK conventions

Use `relaxedMockk<T>()` instead of `mockk<T>(relaxed = true)`:

```kotlin
val callback: () -> Unit = relaxedMockk()
val tracker: AnalyticsTracker = relaxedMockk()
```

Use `verifyOnce` / `verifyNever` instead of `verify(exactly = 1)` and `verify(inverse = true)`:

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

## Testing ViewModels — state with Turbine

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

## Testing ViewModels — events

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

## Testing ViewModels — error states

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

## Boolean states — order convention

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

## Testing pure Kotlin classes

```kotlin
class ValidatePasswordUseCaseTest {

    private lateinit var useCase: ValidatePasswordUseCase

    @Before
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

## What to unit test

- Every ViewModel (state transitions, events, error handling).
- Every non-trivial domain class (validators, use cases, mappers with logic).
- Data source error mapping (HTTP codes → `DataError`).
- Any logic that is likely to change or has edge cases.

Do **not** unit test simple data classes, mappers with no conditional logic, DI modules,
or nav graph wiring.

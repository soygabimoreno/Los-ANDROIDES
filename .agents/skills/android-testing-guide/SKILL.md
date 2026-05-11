---
name: android-testing-guide
description: |
  Testing strategy for Android/KMP: ViewModel tests with JUnit5, MockK, Kluent,
  Turbine, GIVEN/WHEN/THEN naming, fakes, SavedStateHandle, Compose UI tests, Robot
  Pattern, and screenshot testing with Paparazzi. Trigger on: "write a test", "unit
  test the ViewModel", "test a repository", "Turbine", "fake repository", "runTest",
  "ComposeTestRule", "Robot Pattern", "screenshot test", "JUnit5".
---

# Android / KMP Testing Guide

## Stack

| Concern | Library |
|---|---|
| Test framework | JUnit5 |
| Assertions | Kluent |
| Mocking | MockK |
| Flow / StateFlow testing | Turbine |
| Coroutine testing | `kotlinx-coroutines-test` + `UnconfinedTestDispatcher` |
| UI testing | `ComposeTestRule` |
| Screenshot testing | Paparazzi |

For in-depth unit testing conventions (naming, MockK patterns, subject under test
setup, fakes vs mocks), see the **android-unit-testing** skill.

---

## ViewModel Unit Tests

### Setup

Declare the subject under test as `lateinit var` immediately before `@BeforeEach`:

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

### Testing State with Turbine

```kotlin
@Test
fun `GIVEN notes in repository WHEN refresh clicked THEN state contains notes`() = runTest {
    coEvery { noteRepository.getNotes() } returns Result.Success(
        listOf(Note(id = "1", title = "Meeting notes"))
    )

    viewModel.state.test {
        viewModel.onAction(NoteListAction.OnRefreshClick)
        awaitItem().isLoading shouldBe true
        awaitItem().notes shouldHaveSize 1
    }
}
```

### Testing Events

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

### Testing Error States

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

## Fake Repositories

Prefer **fakes** over mocks for your own interfaces. Fakes are simpler and exercise
the full contract:

```kotlin
class FakeNoteRepository : NoteRepository {
    private val notes = mutableListOf<Note>()
    var shouldReturnError = false

    fun givenNotes(vararg note: Note) = notes.addAll(note)

    override suspend fun getNotes(): Result<List<Note>, DataError.Local> =
        if (shouldReturnError) Result.Error(DataError.Local.UNKNOWN)
        else Result.Success(notes.toList())

    override suspend fun insertNote(note: Note): EmptyResult<DataError.Local> {
        notes.add(note)
        return Result.Success(Unit)
    }
}
```

Use `relaxedMockk<T>()` (not `mockk<T>(relaxed = true)`) for third-party dependencies
you can't implement.

---

## SavedStateHandle in Tests

Instantiate it directly — no mocking needed:

```kotlin
val savedStateHandle = SavedStateHandle(mapOf("noteId" to "123"))
val viewModel = NoteEditorViewModel(savedStateHandle, FakeNoteRepository())
```

---

## When to Inject Dispatchers

Only inject `CoroutineDispatcher` when the class dispatches to a non-main dispatcher
**and** is directly unit-tested. ViewModels that only use `viewModelScope` do not need
injected dispatchers — use `Dispatchers.setMain()` in test setup instead.

```kotlin
class ImageCompressor(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun compress(bytes: ByteArray): ByteArray =
        withContext(ioDispatcher) { ... }
}

// In test:
val compressor = ImageCompressor(ioDispatcher = UnconfinedTestDispatcher())
```

---

## Compose UI Tests

For simple screen verification:

```kotlin
@get:Rule
val composeTestRule = createComposeRule()

@Test
fun `GIVEN notes loaded WHEN screen shown THEN note title is displayed`() {
    composeTestRule.setContent {
        NoteListScreen(
            state = NoteListState(notes = listOf(NoteUi("1", "Hello", "Mar 15"))),
            onAction = {}
        )
    }
    composeTestRule.onNodeWithText("Hello").assertIsDisplayed()
}
```

---

## Robot Pattern (Complex UI / E2E Tests)

For screens with 3+ UI test cases or multi-step flows, use the Robot Pattern to
separate test intent from Compose interactions:

```kotlin
class NoteListRobot(private val composeTestRule: ComposeContentTestRule) {

    fun setContent(
        state: NoteListState,
        onAction: (NoteListAction) -> Unit = {}
    ) = apply {
        composeTestRule.setContent {
            NoteListScreen(state = state, onAction = onAction)
        }
    }

    fun assertNoteVisible(title: String) = apply {
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }

    fun clickNote(title: String) = apply {
        composeTestRule.onNodeWithText(title).performClick()
    }

    fun assertEmptyState() = apply {
        composeTestRule.onNodeWithTag("empty_state").assertIsDisplayed()
    }
}
```

```kotlin
class NoteListScreenTest {
    @get:Rule val composeTestRule = createComposeRule()
    private val robot by lazy { NoteListRobot(composeTestRule) }

    @Test
    fun `GIVEN notes loaded WHEN screen shown THEN notes are visible`() {
        robot
            .setContent(NoteListState(notes = listOf(NoteUi("1", "Hello", "Mar 15"))))
            .assertNoteVisible("Hello")
    }

    @Test
    fun `GIVEN no notes WHEN screen shown THEN empty state is visible`() {
        robot
            .setContent(NoteListState(notes = emptyList()))
            .assertEmptyState()
    }
}
```

---

## Screenshot Testing with Paparazzi

Use Paparazzi for pixel-level regression testing without a device:

```kotlin
@get:Rule
val paparazzi = Paparazzi(
    deviceConfig = DeviceConfig.PIXEL_5,
    theme = "Theme.MyApp"
)

@Test
fun `GIVEN default state WHEN NoteListScreen rendered THEN snapshot matches`() {
    paparazzi.snapshot {
        NoteListScreen(
            state = NoteListState(notes = listOf(NoteUi("1", "Meeting notes", "Mar 15"))),
            onAction = {}
        )
    }
}
```

Run `./gradlew recordPaparazziDebug` to record goldens,
`./gradlew verifyPaparazziDebug` in CI.

---

## What to Test

- Unit-test every ViewModel and any non-trivial domain/data logic.
- Use fakes over mocks for your own interfaces.
- Write Compose tests for critical user flows.
- Use the Robot Pattern for screens with 3+ test cases.
- Use Paparazzi for visual regression on design system components.

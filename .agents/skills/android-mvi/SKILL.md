---
name: android-mvi
description: |
  MVI presentation layer for Android/KMP: State, Action, Event, ViewModel, Root/Screen
  composable split, UI models, UiText error mapping, ObserveAsEvents, and process death
  with SavedStateHandle. Trigger on: "add a ViewModel", "create a screen", "MVI",
  "state", "action", "event", "screen composable", "UiText", "SavedStateHandle",
  "ObserveAsEvents", "UI model".
---

# Android / KMP Presentation Layer — MVI

## Overview

Every screen has:
1. **State** — a single data class holding all UI state fields.
2. **Action** (Intent) — a sealed interface of all user-triggered actions.
3. **Event** — a sealed interface of one-time side effects (navigation, snackbar).
4. **ViewModel** — holds `StateFlow<State>`, processes `Action`, emits `Event` via `Channel`.

---

## State

```kotlin
data class NoteListState(
    val notes: List<NoteUi> = emptyList(),
    val isLoading: Boolean = false,
    val error: UiText? = null
)
```

Always update state with `.update { }` — never replace the entire flow:

```kotlin
_state.update { it.copy(isLoading = true) }
```

---

## Action (Intent)

```kotlin
sealed interface NoteListAction {
    data object OnRefreshClick : NoteListAction
    data class OnNoteClick(val noteId: String) : NoteListAction
    data class OnDeleteNote(val noteId: String) : NoteListAction
}
```

---

## Event (one-time side effects)

```kotlin
sealed interface NoteListEvent {
    data class NavigateToDetail(val noteId: String) : NoteListEvent
    data class ShowSnackbar(val message: UiText) : NoteListEvent
}
```

---

## ViewModel

Inject dependencies via Hilt (`@HiltViewModel` + `@Inject constructor`):

```kotlin
@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(NoteListState())
    val state = _state.asStateFlow()

    private val _events = Channel<NoteListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onAction(action: NoteListAction) {
        when (action) {
            is NoteListAction.OnRefreshClick -> loadNotes()
            is NoteListAction.OnNoteClick -> viewModelScope.launch {
                _events.send(NoteListEvent.NavigateToDetail(action.noteId))
            }
        }
    }

    private fun loadNotes() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            noteRepository.getNotes()
                .onSuccess { notes ->
                    _state.update {
                        it.copy(notes = notes.map { n -> n.toNoteUi() }, isLoading = false)
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false) }
                    _events.send(NoteListEvent.ShowSnackbar(error.toUiText()))
                }
        }
    }
}
```

Use `Channel.BUFFERED` so events sent before the UI subscribes are not lost.

---

## Coroutine Dispatchers

Do not inject dispatchers unless the class dispatches to a non-main dispatcher **and**
is directly unit-tested. For ViewModel tests, use `Dispatchers.setMain(UnconfinedTestDispatcher())`
in test setup.

For blocking code that doesn't support suspension, wrap it:

```kotlin
suspend fun compressImage(bytes: ByteArray): ByteArray = withContext(Dispatchers.IO) {
    // blocking compression logic
}
```

Only inject `CoroutineDispatcher` when the class dispatches to a non-main dispatcher
AND is unit-tested directly.

---

## Mapping Errors to UI Strings

`UiText` (lives in `:core:core-view`) wraps strings that originate from — or could
originate from — a string resource:

```kotlin
sealed interface UiText {
    data class DynamicString(val value: String) : UiText
    class StringResource(
        val id: Int,
        val args: Array<Any> = emptyArray()
    ) : UiText
}
```

- Use `UiText` for any string that maps to a string resource or could be localized
  (e.g., error messages).
- Use plain `String` for values that are always dynamic and never come from resources
  (e.g., user name, formatted date, currency amount).

```kotlin
// UiText — error message that maps to a string resource
data class NoteListState(val error: UiText? = null)

// Plain String — always dynamic, never a resource
data class NoteUi(val authorName: String, val formattedDate: String)
```

---

## ObserveAsEvents

`ObserveAsEvents` collects a `Flow` of one-time events in a lifecycle-aware way,
consuming each event exactly once. Lives in `:core:core-view`:

```kotlin
@Composable
fun <T> ObserveAsEvents(
    flow: Flow<T>,
    key1: Any? = null,
    key2: Any? = null,
    onEvent: (T) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(flow, lifecycleOwner, key1, key2) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) {
                flow.collect(onEvent)
            }
        }
    }
}
```

---

## UI Model (Presentation Model)

When a domain model needs UI-specific formatting (dates, units, currency), create a
dedicated UI model in the presentation layer:

```kotlin
data class NoteUi(
    val id: String,
    val title: String,
    val formattedDate: String
)

fun Note.toNoteUi(): NoteUi = NoteUi(
    id = id,
    title = title,
    formattedDate = date.format(...)
)
```

UI models are always suffixed with `Ui` (e.g., `NoteUi`, `TodoItemUi`).

---

## Composable Structure

Both the Root and Screen composable live in the **same file** (e.g., `NoteListScreen.kt`).

**Root composable** (suffixed `Root`): receives the ViewModel via `hiltViewModel()` and
any navigation callbacks. Observes events. Passes state and `onAction` down.

**Screen composable** (suffixed `Screen`): receives only `state` and `onAction`. No
ViewModel reference — can be previewed independently.

```kotlin
@Composable
fun NoteListRoot(
    onNavigateToDetail: (String) -> Unit,
    viewModel: NoteListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is NoteListEvent.NavigateToDetail -> onNavigateToDetail(event.noteId)
            is NoteListEvent.ShowSnackbar -> { /* show snackbar */ }
        }
    }

    NoteListScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun NoteListScreen(
    state: NoteListState,
    onAction: (NoteListAction) -> Unit
) { ... }
```

---

## Process Death

When a screen involves complex forms or critical user input, restore essential fields
using `SavedStateHandle`. Only save what truly matters after process death — not the
entire state:

```kotlin
@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val noteRepository: NoteRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        NoteEditorState(
            title = savedStateHandle["title"] ?: "",
            body = savedStateHandle["body"] ?: ""
        )
    )

    fun onAction(action: NoteEditorAction) {
        when (action) {
            is NoteEditorAction.OnTitleChange -> {
                savedStateHandle["title"] = action.title
                _state.update { it.copy(title = action.title) }
            }
        }
    }
}
```

---

## Naming Conventions

| Thing | Convention | Example |
|---|---|---|
| ViewModel | `<Screen>ViewModel` | `NoteListViewModel` |
| State | `<Screen>State` | `NoteListState` |
| Action | `<Screen>Action` | `NoteListAction` |
| Event | `<Screen>Event` | `NoteListEvent` |
| Root composable | `<Screen>Root` | `NoteListRoot` |
| Screen composable | `<Screen>Screen` | `NoteListScreen` |
| UI model | `<Model>Ui` | `NoteUi`, `TodoItemUi` |

---

## Checklist: Adding a New Screen

- [ ] Define `State`, `Action`, `Event` in the feature's `presentation` layer
- [ ] Implement `ViewModel` annotated `@HiltViewModel` with `@Inject constructor`,
      using `Channel.BUFFERED` for events
- [ ] Create `<Screen>Root` (holds ViewModel via `hiltViewModel()`, observes events
      via `ObserveAsEvents`)
- [ ] Create `<Screen>Screen` (pure state + onAction, previewable)
- [ ] Map domain errors to `UiText` via extension functions
- [ ] Add `SavedStateHandle` for any form fields that must survive process death

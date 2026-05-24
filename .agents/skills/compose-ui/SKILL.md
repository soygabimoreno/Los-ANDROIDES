---
name: compose-ui
description: |
  Los ANDROIDES project conventions for Compose: ViewModel-owned state, lazy list keys,
  the :core:design-system module, Preview patterns with PreviewParameterProvider,
  accessibility, and TextField wired through MVI Actions. Trigger on: Los ANDROIDES
  Compose screen, design-system, AppTheme, @Preview, PreviewParameterProvider,
  contentDescription, TextField + Action, collectAsStateWithLifecycle, LazyColumn key.
---

# Compose UI — Los ANDROIDES conventions

For general Compose patterns, use the focused skills:

- State authoring/ownership: `compose-state-authoring`, `compose-state-hoisting`, `compose-state-holder-ui-split`
- Side effects: `compose-side-effects`
- Performance, stability, deferred reads: `compose-recomposition-performance`, `compose-stability-diagnostics`, `compose-state-deferred-reads`
- Layout, modifiers, slots: `compose-modifier-and-layout-style`, `compose-slot-api-pattern`
- Animations: `compose-animations`
- Testing: `compose-ui-testing-patterns`
- Kotlin idioms used by Compose: `kotlin-flow-state-event-modeling`, `kotlin-types-value-class`

This skill covers only what is specific to Los ANDROIDES.

---

## Core principle

The UI is dumb. Composables render state and forward user actions — nothing more.
All state lives in the ViewModel. All logic lives in the ViewModel, domain, or data
layer. Compose code should contain zero business logic, zero data transformation, and
minimal side effects.

See `android-mvi` for the full Action/State pattern.

---

## State ownership

All application state lives in the ViewModel. Do not use `remember` or `rememberSaveable`
for application state — that belongs in the ViewModel's `StateFlow`, surfaced via
`collectAsStateWithLifecycle()`.

The only exception is Compose-internal state the framework requires in composition,
such as `LazyListState`, `ScrollState`, or `PagerState`:

```kotlin
val lazyListState = rememberLazyListState()

val showScrollToTop by remember {
    derivedStateOf { lazyListState.firstVisibleItemIndex > 5 }
}
```

Use `derivedStateOf` only when Compose-internal state drives a derived value. If the
derivation can happen in the ViewModel, it should.

Always collect ViewModel state with lifecycle awareness:

```kotlin
val state by viewModel.state.collectAsStateWithLifecycle()
```

For deeper hoisting decisions see `compose-state-hoisting` and `compose-state-holder-ui-split`.

---

## Lazy layouts

Add `key` to lazy list items when there is a clear unique identifier. Don't force it
if uniqueness is unclear:

```kotlin
LazyColumn {
    items(items = state.notes, key = { it.id }) { note ->
        NoteItem(
            note = note,
            onClick = { onAction(OnNoteClick(note.id)) }
        )
    }
}
```

---

## Design system

The design system lives in `:core:design-system` and contains reusable components,
colors, theme, and typography. Feature modules consume it; they do not redefine
colors, typography, or shared components.

Feature-level composables should prefer typed parameters over slot APIs for clarity.
For genuine slot-based reusable components in `:core:design-system`, see
`compose-slot-api-pattern`.

---

## Previews

Every Screen composable should have at least one `@Preview` with realistic state.
Use `PreviewParameterProvider` to cover multiple states without duplicating preview
functions:

```kotlin
class NoteListStateProvider : PreviewParameterProvider<NoteListState> {
    override val values = sequenceOf(
        NoteListState(isLoading = true),
        NoteListState(notes = listOf(NoteUi("1", "Meeting notes", "Mar 15"))),
        NoteListState(notes = emptyList())
    )
}

@Preview
@Composable
private fun NoteListScreenPreview(
    @PreviewParameter(NoteListStateProvider::class) state: NoteListState
) {
    AppTheme {
        NoteListScreen(state = state, onAction = {})
    }
}
```

Wrap previews in `AppTheme`. Use realistic data, not empty states (unless specifically
previewing the empty state).

---

## Accessibility

Use `contentDescription` on all interactive or informational visual elements. Always
use string resources for localization:

```kotlin
Icon(
    imageVector = Icons.Default.Delete,
    contentDescription = stringResource(R.string.cd_delete_note)
)
```

For decorative elements that convey no information, set `contentDescription = null`.

Use `Modifier.semantics` for richer accessibility metadata — grouping related elements,
exposing custom actions, or overriding how the accessibility tree represents a node:

```kotlin
Row(modifier = Modifier.semantics(mergeDescendants = true) {}) {
    Icon(Icons.Default.Star, contentDescription = null)
    Text("Favorite")
}

Box(
    modifier = Modifier
        .clickable { onAction(OnNoteClick(note.id)) }
        .semantics {
            onClick(label = stringResource(R.string.cd_open_note)) {
                onAction(OnNoteClick(note.id))
                true
            }
        }
)
```

---

## TextField

Text input state lives in the ViewModel. Every keystroke dispatches an Action:

```kotlin
TextField(
    value = state.title,
    onValueChange = { onAction(NoteEditorAction.OnTitleChange(it)) }
)
```

The ViewModel updates state (and optionally persists to `SavedStateHandle`) in
response to the Action. See `android-mvi` for the full pattern.

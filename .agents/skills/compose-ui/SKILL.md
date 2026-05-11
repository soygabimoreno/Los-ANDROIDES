---
name: compose-ui
description: |
  Compose UI for Android/KMP: stability, recomposition, side effects, lazy lists,
  animations, previews, accessibility, modifier extensions, design system composables.
  Trigger on: composable, recomposition, LaunchedEffect, Modifier, LazyColumn, preview,
  animation, design system, stability, contentDescription, graphicsLayer, slot API,
  Compose performance.
---

# Android / KMP Compose UI Patterns

## Core Principle

The UI is dumb. Composables render state and forward user actions — nothing more.
All state lives in the ViewModel. All logic lives in the ViewModel, domain, or data
layer. Compose code should contain zero business logic, zero data transformation, and
minimal side effects.

---

## Stability & Recomposition

Strong skipping mode is enabled by default in modern Compose — no explicit opt-in needed.

Annotate state classes to help the compiler skip recomposition when inputs haven't changed:

- Use `@Immutable` when all properties are deeply immutable after construction
  (safe for MVI state with `ImmutableList` / `PersistentList` from
  `kotlinx.collections.immutable`).
- Use `@Stable` when the class contains fields the compiler considers unstable
  (`List`, `Map`, `Set`, interfaces, abstract types) but you guarantee changes are
  notified through Compose snapshot state.

```kotlin
// @Immutable — backed by a truly immutable collection
@Immutable
data class NoteListState(
    val notes: ImmutableList<NoteUi> = persistentListOf(),
    val isLoading: Boolean = false
)

// @Stable — List<T> is unstable by default; we promise Compose is notified of changes
@Stable
data class NoteListState(
    val notes: List<NoteUi> = emptyList(),
    val isLoading: Boolean = false
)

// No annotation needed — all fields are primitive or String
data class NoteDetailState(
    val title: String = "",
    val body: String = "",
    val isSaving: Boolean = false
)
```

---

## State Ownership

All state lives in the ViewModel. Do not use `remember` or `rememberSaveable` for
application state — that belongs in the ViewModel's `StateFlow`, surfaced via
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

---

## Side Effects

Prefer routing work through ViewModel Actions over using composable side effects.

When a side effect is unavoidable (e.g., Android lifecycle APIs with no ViewModel
equivalent), extract it into a dedicated composable. Use `rememberUpdatedState` to
always capture the latest lambda without restarting the effect:

```kotlin
@Composable
fun ObserveLifecycle(onStart: () -> Unit, onStop: () -> Unit) {
    val currentOnStart by rememberUpdatedState(onStart)
    val currentOnStop by rememberUpdatedState(onStop)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> currentOnStart()
                Lifecycle.Event.ON_STOP -> currentOnStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
```

Use `snapshotFlow` to convert Compose state into a Flow when you need operators like
`distinctUntilChanged`, `filter`, or `debounce`:

```kotlin
LaunchedEffect(lazyListState) {
    snapshotFlow { lazyListState.firstVisibleItemIndex }
        .distinctUntilChanged()
        .filter { it > 5 }
        .collect { onAction(OnScrolledPast(it)) }
}
```

Do not use custom `CompositionLocal`s.

---

## Lazy Layouts

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

## Animations

Prefer approaches that animate below the recomposition layer:

- **`graphicsLayer`** — alpha, scale, rotation, translation
- **Offset lambda** — position changes via `offset { ... }`
- **`Canvas`** — custom drawing
- **`animateFloatAsState` + `graphicsLayer`** — float animation applied in draw phase

The key distinction is *where* the state is read. Reading animated state inside the
composition body (e.g. passing it as a parameter) triggers recomposition every frame.
Reading it inside a `graphicsLayer` or offset lambda defers the read to the draw/layout
phase, which skips recomposition entirely:

```kotlin
val alpha by animateFloatAsState(if (state.isVisible) 1f else 0f)

// Good — alpha is read inside the graphicsLayer lambda (draw phase)
Box(modifier = Modifier.graphicsLayer { this.alpha = alpha })

// Bad — alpha is read at the call site (composition phase), recomposes every frame
Box(modifier = Modifier.alpha(alpha))
```

**Deferred state reads via lambdas:**

```kotlin
// Good — offsetProvider is called in the layout phase
fun Modifier.animatedOffset(offsetProvider: () -> IntOffset) =
    offset { offsetProvider() }

// Bad — offset value is read at composition time
fun Modifier.animatedOffset(offset: IntOffset) =
    offset(x = offset.x.dp, y = offset.y.dp)
```

---

## Modifier Extensions

For simple modifier chains that don't need composition, use plain extension functions:

```kotlin
fun Modifier.roundedBackground(color: Color, radius: Dp) =
    background(color, RoundedCornerShape(radius))
```

For modifiers that require custom draw, layout, or pointer input logic, use the
`Modifier.Node` API. Do not use `composed {}` — it is deprecated since Compose 1.3:

```kotlin
private class ShimmerNode : DrawModifierNode, Modifier.Node() {
    override fun ContentDrawScope.draw() {
        drawContent()
        // shimmer overlay drawn here
    }
}

private object ShimmerElement : ModifierNodeElement<ShimmerNode>() {
    override fun create() = ShimmerNode()
    override fun update(node: ShimmerNode) = Unit
    override fun hashCode() = System.identityHashCode(this)
    override fun equals(other: Any?) = (other === this)
}

fun Modifier.shimmerEffect(): Modifier = this then ShimmerElement
```

Do not make modifier extensions `@Composable`.

---

## Design System & Slot APIs

The design system lives in `:core:design-system` and contains reusable components,
colors, theme, and typography.

Use slot APIs (passing `@Composable` lambdas) for design system components that need
flexible content areas:

```kotlin
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Card(modifier = modifier) {
        header()
        content()
    }
}
```

Feature-level composables should prefer typed parameters over slots for clarity.

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

Wrap previews in the app theme. Use realistic data, not empty states (unless
specifically previewing the empty state).

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
// Merge descendant nodes into a single accessibility node
Row(modifier = Modifier.semantics(mergeDescendants = true) {}) {
    Icon(Icons.Default.Star, contentDescription = null)
    Text("Favorite")
}

// Custom click label for screen readers
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
response to the Action — see the **android-mvi** skill for the full
pattern.

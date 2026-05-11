---
name: compose-navigation
description: |
  Type-safe Compose Navigation for Android/KMP: route objects, feature nav graphs,
  cross-feature callbacks, bottom navigation, deep links, and wiring in :app.
  Trigger on: "set up navigation", "add a route", "navigate between screens", "nav graph",
  "NavController", "type-safe nav", "cross-feature navigation", "NavGraphBuilder",
  "bottom navigation", "deep link".
---

# Android / KMP Compose Navigation

## Principles

- **Type-safe navigation** with `@Serializable` route objects (KotlinX Serialization).
- **One nav graph per feature**, defined in the feature's `presentation` module.
- Feature nav graphs are assembled in `:app`.
- Navigation **within** a feature uses the `NavController` passed into the feature graph.
- Feature-to-feature navigation uses **callbacks**, keeping features decoupled.

---

## Route Objects

Define routes as `@Serializable` objects or data classes in the feature's `presentation`
module. Use `data object` for screens with no parameters, `data class` for screens with
arguments:

```kotlin
// feature:notes:presentation
@Serializable data object NoteListRoute
@Serializable data class NoteDetailRoute(val noteId: String)
```

---

## Feature Nav Graph

Each feature exposes a `NavGraphBuilder` extension function:

```kotlin
// feature:notes:presentation
fun NavGraphBuilder.notesGraph(
    navController: NavController,
    onNavigateToEditor: (String) -> Unit
) {
    navigation<NoteListRoute>(startDestination = NoteListRoute) {
        composable<NoteListRoute> {
            NoteListRoot(
                onNavigateToDetail = { navController.navigate(NoteDetailRoute(it)) }
            )
        }
        composable<NoteDetailRoute> { backStackEntry ->
            val route: NoteDetailRoute = backStackEntry.toRoute()
            NoteDetailRoot(
                noteId = route.noteId,
                onNavigateToEditor = onNavigateToEditor
            )
        }
    }
}
```

---

## Wiring in `:app`

All feature nav graphs are assembled in one place:

```kotlin
NavHost(navController, startDestination = NoteListRoute) {
    notesGraph(
        navController = navController,
        onNavigateToEditor = { navController.navigate(EditorRoute(it)) }
    )
    editorGraph(navController)
}
```

Cross-feature navigation is always expressed as a lambda callback — never by importing
a route from another feature module.

---

## Passing Arguments

For simple scalar arguments, use `@Serializable data class` routes. Avoid passing
complex objects via navigation — pass IDs and load data in the destination ViewModel:

```kotlin
@Serializable data class NoteDetailRoute(val noteId: String)

// Navigate
navController.navigate(NoteDetailRoute(noteId = "abc123"))

// Receive
composable<NoteDetailRoute> { backStackEntry ->
    val route: NoteDetailRoute = backStackEntry.toRoute()
}
```

---

## Back Stack Options

Use `NavOptions` to control the back stack when navigating:

```kotlin
// Navigate to destination, clearing everything up to (and including) the start
navController.navigate(HomeRoute) {
    popUpTo<HomeRoute> { inclusive = true }
    launchSingleTop = true
}

// Clear entire back stack back to the start destination (e.g., after login)
navController.navigate(HomeRoute) {
    popUpTo(navController.graph.startDestinationId) { inclusive = true }
}
```

---

## Bottom Navigation

Keep bottom navigation destination routes at the `:app` level. The selected tab is
derived from the current back stack entry:

```kotlin
val navBackStackEntry by navController.currentBackStackEntryAsState()
val currentRoute = navBackStackEntry?.destination?.route

NavigationBar {
    bottomNavItems.forEach { item ->
        NavigationBarItem(
            selected = currentRoute == item.route::class.qualifiedName,
            onClick = {
                navController.navigate(item.route) {
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(item.icon, contentDescription = null) },
            label = { Text(stringResource(item.labelRes)) }
        )
    }
}
```

---

## Deep Links

Declare deep links in the composable destination:

```kotlin
composable<NoteDetailRoute>(
    deepLinks = listOf(
        navDeepLink<NoteDetailRoute>(
            basePath = "https://myapp.com/note"
        )
    )
) { backStackEntry ->
    val route: NoteDetailRoute = backStackEntry.toRoute()
    NoteDetailRoot(noteId = route.noteId)
}
```

Register the intent filter in `AndroidManifest.xml` for the host Activity.

---

## Naming Conventions

| Thing | Convention | Example |
|---|---|---|
| Nav route | `<Screen>Route` | `NoteListRoute`, `NoteDetailRoute` |
| Feature nav graph | `<feature>Graph(...)` on `NavGraphBuilder` | `notesGraph(...)` |

---

## Checklist: Adding Navigation to a New Feature

- [ ] Define `@Serializable` route objects for each screen in `feature:presentation`
- [ ] Add feature nav graph (`NavGraphBuilder.<feature>Graph(...)`)
- [ ] Pass `NavController` for intra-feature navigation
- [ ] Expose cross-feature destinations as lambda callbacks (not direct route imports)
- [ ] Wire nav graph and cross-feature callbacks in `:app`'s `NavHost`

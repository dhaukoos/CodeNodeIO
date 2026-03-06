# Contract: PreviewRegistry API

**Location**: `graphEditor/src/jvmMain/kotlin/ui/PreviewRegistry.kt`

## Purpose

Registry that maps composable names to preview rendering functions. Replaces the hardcoded `when` block in RuntimePreviewPanel with dynamic dispatch.

## Interface Definition

```kotlin
typealias PreviewComposable = @Composable (viewModel: Any, modifier: Modifier) -> Unit

object PreviewRegistry {
    fun register(composableName: String, preview: PreviewComposable)
    fun get(composableName: String): PreviewComposable?
    fun hasPreview(composableName: String): Boolean
    fun registeredNames(): Set<String>
}
```

## Methods

| Method | Parameters | Returns | Description |
|--------|-----------|---------|-------------|
| `register` | `composableName: String, preview: PreviewComposable` | — | Register a preview function for a composable name |
| `get` | `composableName: String` | `PreviewComposable?` | Get the preview function for a composable, or null |
| `hasPreview` | `composableName: String` | `Boolean` | Check if a preview is registered for this composable |
| `registeredNames` | — | `Set<String>` | All registered composable names |

## Registration Pattern

Each module's preview provider registers its composables at initialization:

```kotlin
// In StopWatchPreviewProvider initialization
PreviewRegistry.register("StopWatch") { viewModel, modifier ->
    val vm = viewModel as StopWatchViewModel
    // render StopWatch preview
}
PreviewRegistry.register("StopWatchScreen") { viewModel, modifier ->
    val vm = viewModel as StopWatchViewModel
    // render StopWatchScreen preview
}

// In UserProfilesPreviewProvider initialization
PreviewRegistry.register("UserProfiles") { viewModel, modifier ->
    val vm = viewModel as UserProfilesViewModel
    // render UserProfiles preview
}
```

## Usage in RuntimePreviewPanel

```kotlin
// Replace hardcoded when block with:
val previewFn = PreviewRegistry.get(selectedComposable)
if (previewFn != null) {
    previewFn(runtimeSession.viewModel, Modifier)
} else {
    Text("Preview not available for: $selectedComposable")
}
```

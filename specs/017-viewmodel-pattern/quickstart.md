# Quickstart: ViewModel Pattern Migration

**Feature**: 017-viewmodel-pattern
**Date**: 2026-02-16

## Overview

This guide helps developers implement the ViewModel pattern migration for the graphEditor module. The migration extracts state management from UI composables into dedicated ViewModel classes.

## Prerequisites

- Kotlin 2.1.21 or later
- Compose Desktop 1.7.3
- kotlinx-coroutines 1.8.0
- lifecycle-viewmodel-compose 2.10.0-alpha06 (JetBrains multiplatform ViewModel)
- Existing graphEditor module compiled and running

## New Dependency

Add to `graphEditor/build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0-alpha06")
        }
    }
}
```

## Key Files to Understand

Before implementing, review these existing files:

| File | Why Review |
|------|------------|
| `graphEditor/src/jvmMain/kotlin/state/GraphState.kt` | Current state management pattern |
| `graphEditor/src/jvmMain/kotlin/state/UndoRedoManager.kt` | Command pattern for undo/redo |
| `graphEditor/src/jvmMain/kotlin/Main.kt` | Current composition root |
| `graphEditor/src/jvmMain/kotlin/ui/NodeGeneratorPanel.kt` | Example of local state to extract |

## Implementation Order

### Phase 1: Setup ViewModel Infrastructure

1. **Create `viewmodel/` package** at `graphEditor/src/jvmMain/kotlin/viewmodel/`

2. **Create `SharedStateProvider.kt`** for dependency injection:
   ```kotlin
   data class SharedStateProvider(
       val graphState: GraphState,
       val undoRedoManager: UndoRedoManager,
       val propertyChangeTracker: PropertyChangeTracker,
       val ipTypeRegistry: IPTypeRegistry,
       val customNodeRepository: CustomNodeRepository
   )

   val LocalSharedState = staticCompositionLocalOf<SharedStateProvider> {
       error("SharedStateProvider not provided")
   }
   ```

### Phase 2: Extract Simplest ViewModel First

3. **Create `NodeGeneratorViewModel.kt`**:
   - Extract state from NodeGeneratorPanel
   - Move business logic (validation, node creation)
   - Expose state via StateFlow

4. **Update `NodeGeneratorPanel.kt`**:
   - Remove local state
   - Observe ViewModel state via collectAsState()
   - Call ViewModel actions on user events

### Phase 3: Continue Extraction

5. Extract ViewModels in order:
   - NodePaletteViewModel
   - IPPaletteViewModel
   - PropertiesPanelViewModel
   - CanvasInteractionViewModel
   - GraphEditorViewModel

## Code Snippets

### ViewModel Base Pattern

```kotlin
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NodeGeneratorViewModel(
    private val customNodeRepository: CustomNodeRepository
) : ViewModel() {
    private val _state = MutableStateFlow(NodeGeneratorPanelState())
    val state: StateFlow<NodeGeneratorPanelState> = _state.asStateFlow()

    fun setName(name: String) {
        _state.update { it.copy(name = name) }
    }

    fun setInputCount(count: Int) {
        _state.update { it.copy(inputCount = count.coerceIn(0, 3)) }
    }

    fun setOutputCount(count: Int) {
        _state.update { it.copy(outputCount = count.coerceIn(0, 3)) }
    }

    fun toggleExpanded() {
        _state.update { it.copy(isExpanded = !it.isExpanded) }
    }

    fun createNode(): CustomNodeDefinition? {
        val currentState = _state.value
        if (!currentState.isValid) return null

        val node = CustomNodeDefinition.create(
            name = currentState.name.trim(),
            inputCount = currentState.inputCount,
            outputCount = currentState.outputCount
        )
        customNodeRepository.add(node)
        reset()
        return node
    }

    fun reset() {
        _state.update { NodeGeneratorPanelState() }
    }
}
```

### State Data Class

```kotlin
data class NodeGeneratorPanelState(
    val name: String = "",
    val inputCount: Int = 1,
    val outputCount: Int = 1,
    val isExpanded: Boolean = false,
    val inputDropdownExpanded: Boolean = false,
    val outputDropdownExpanded: Boolean = false
) {
    val isValid: Boolean
        get() = name.isNotBlank() && !(inputCount == 0 && outputCount == 0)

    val genericType: String
        get() = "in${inputCount}out${outputCount}"
}
```

### Composable Using ViewModel

```kotlin
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun NodeGeneratorPanel(
    viewModel: NodeGeneratorViewModel = viewModel { NodeGeneratorViewModel(customNodeRepository) },
    onNodeCreated: (CustomNodeDefinition) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = modifier) {
        // Collapsible header
        Row(
            modifier = Modifier.clickable { viewModel.toggleExpanded() }
        ) {
            Text(if (state.isExpanded) "▼" else "▶")
            Text("Node Generator")
        }

        if (state.isExpanded) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.setName(it) },
                label = { Text("Name") }
            )

            // Dropdowns...

            Button(
                onClick = {
                    viewModel.createNode()?.let { onNodeCreated(it) }
                },
                enabled = state.isValid
            ) {
                Text("Create")
            }
        }
    }
}
```

### ViewModel Test Example

```kotlin
class NodeGeneratorViewModelTest {

    @Test
    fun `createNode returns null when state is invalid`() {
        val mockRepository = MockCustomNodeRepository()
        val viewModel = NodeGeneratorViewModel(mockRepository)

        // Name is empty, so invalid
        val result = viewModel.createNode()

        assertNull(result)
        assertEquals(0, mockRepository.addedNodes.size)
    }

    @Test
    fun `createNode adds node to repository when valid`() {
        val mockRepository = MockCustomNodeRepository()
        val viewModel = NodeGeneratorViewModel(mockRepository)

        viewModel.setName("TestNode")
        viewModel.setInputCount(2)
        viewModel.setOutputCount(1)

        val result = viewModel.createNode()

        assertNotNull(result)
        assertEquals("TestNode", result.name)
        assertEquals(1, mockRepository.addedNodes.size)
    }

    @Test
    fun `setInputCount coerces to valid range`() = runTest {
        val viewModel = NodeGeneratorViewModel(MockCustomNodeRepository())

        viewModel.setInputCount(5)
        assertEquals(3, viewModel.state.value.inputCount)

        viewModel.setInputCount(-1)
        assertEquals(0, viewModel.state.value.inputCount)
    }
}
```

### CompositionLocal Integration in Main.kt

```kotlin
@Composable
fun GraphEditorApp() {
    val graphState = remember { GraphState(initialGraph) }
    val undoRedoManager = rememberUndoRedoManager()
    val propertyChangeTracker = rememberPropertyChangeTracker(undoRedoManager, graphState)
    val customNodeRepository = remember { FileCustomNodeRepository() }
    val ipTypeRegistry = remember { IPTypeRegistry.withDefaults() }

    val sharedState = remember {
        SharedStateProvider(
            graphState = graphState,
            undoRedoManager = undoRedoManager,
            propertyChangeTracker = propertyChangeTracker,
            ipTypeRegistry = ipTypeRegistry,
            customNodeRepository = customNodeRepository
        )
    }

    // Create ViewModels
    val nodeGeneratorViewModel = remember {
        NodeGeneratorViewModel(customNodeRepository)
    }

    CompositionLocalProvider(LocalSharedState provides sharedState) {
        MaterialTheme {
            // Use ViewModels in UI
            NodeGeneratorPanel(
                viewModel = nodeGeneratorViewModel,
                onNodeCreated = { /* refresh palette */ }
            )
        }
    }
}
```

## Testing Approach

1. **Unit Tests** (ViewModelTest.kt)
   - State transitions for all actions
   - Validation logic
   - Edge cases (empty state, boundary values)
   - No Compose dependencies

2. **Integration Tests**
   - ViewModel + Repository interaction
   - Command execution through UndoRedoManager
   - State propagation to GraphState

3. **UI Tests** (Optional)
   - Composable renders correctly for given state
   - User interactions trigger correct ViewModel actions

## Common Pitfalls

1. **Forgetting collectAsState()** - State won't update UI without collecting
2. **Modifying state directly** - Always use `_state.update { }` or `_state.value = `
3. **Tight coupling** - ViewModels should not reference each other directly
4. **Testing with Compose** - Test ViewModels in isolation, not through UI
5. **Large state objects** - Split into smaller, focused state classes

## Validation Checklist

- [ ] ViewModel created in `viewmodel/` package
- [ ] State exposed as `StateFlow<State>`
- [ ] Actions are public methods (no state mutation from UI)
- [ ] UI composable uses `collectAsState()`
- [ ] UI composable has no business logic
- [ ] Unit tests pass without Compose dependencies
- [ ] Existing functionality preserved
- [ ] Undo/redo still works

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

---

## Template: Adding a New ViewModel

Use this template when adding a new UI component with its own ViewModel.

### Step 1: Create the ViewModel File

Create `graphEditor/src/jvmMain/kotlin/viewmodel/MyFeatureViewModel.kt`:

```kotlin
/*
 * MyFeatureViewModel - ViewModel for [describe feature]
 * [Brief description of what this ViewModel manages]
 * License: Apache 2.0
 */

package io.codenode.grapheditor.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * State data class for [Feature Name].
 * Contains all UI state for the feature.
 *
 * @param fieldOne Description of first field
 * @param fieldTwo Description of second field
 * @param isExpanded Whether the panel/section is expanded
 */
data class MyFeatureState(
    val fieldOne: String = "",
    val fieldTwo: Int = 0,
    val isExpanded: Boolean = false
) : BaseState {
    /**
     * Computed property: validation result based on current state.
     */
    val isValid: Boolean
        get() = fieldOne.isNotBlank() && fieldTwo > 0
}

/**
 * ViewModel for [Feature Name].
 * Manages state and business logic for [describe functionality].
 *
 * @param dependency Any injected dependencies (repositories, services)
 */
class MyFeatureViewModel(
    private val dependency: SomeDependency
) : ViewModel() {

    private val _state = MutableStateFlow(MyFeatureState())
    val state: StateFlow<MyFeatureState> = _state.asStateFlow()

    // ========== Actions ==========

    /**
     * Updates fieldOne.
     */
    fun setFieldOne(value: String) {
        _state.update { it.copy(fieldOne = value) }
    }

    /**
     * Updates fieldTwo with validation.
     */
    fun setFieldTwo(value: Int) {
        _state.update { it.copy(fieldTwo = value.coerceAtLeast(0)) }
    }

    /**
     * Toggles expanded state.
     */
    fun toggleExpanded() {
        _state.update { it.copy(isExpanded = !it.isExpanded) }
    }

    /**
     * Performs the main action of this feature.
     * @return Result object or null if invalid state
     */
    fun performAction(): SomeResult? {
        val currentState = _state.value
        if (!currentState.isValid) return null

        val result = dependency.doSomething(currentState.fieldOne, currentState.fieldTwo)
        reset()
        return result
    }

    /**
     * Resets state to defaults, preserving UI state like expansion.
     */
    fun reset() {
        _state.update { MyFeatureState(isExpanded = it.isExpanded) }
    }
}
```

### Step 2: Create the Test File

Create `graphEditor/src/jvmTest/kotlin/viewmodel/MyFeatureViewModelTest.kt`:

```kotlin
/*
 * MyFeatureViewModelTest - Unit tests for MyFeatureViewModel
 * Verifies state transitions and business logic without Compose UI dependencies
 * License: Apache 2.0
 */

package io.codenode.grapheditor.viewmodel

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Fake dependency for testing - implements the interface without side effects
 */
class FakeSomeDependency : SomeDependency {
    val actions = mutableListOf<Pair<String, Int>>()

    override fun doSomething(field1: String, field2: Int): SomeResult {
        actions.add(field1 to field2)
        return SomeResult(/* ... */)
    }
}

class MyFeatureViewModelTest {

    private fun createViewModel(
        dependency: SomeDependency = FakeSomeDependency()
    ): MyFeatureViewModel {
        return MyFeatureViewModel(dependency)
    }

    @Test
    fun `initial state has default values`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.state.first()

        assertEquals("", state.fieldOne)
        assertEquals(0, state.fieldTwo)
        assertFalse(state.isExpanded)
        assertFalse(state.isValid)
    }

    @Test
    fun `setFieldOne updates state`() = runTest {
        val viewModel = createViewModel()

        viewModel.setFieldOne("test value")

        assertEquals("test value", viewModel.state.first().fieldOne)
    }

    @Test
    fun `setFieldTwo coerces negative values to zero`() = runTest {
        val viewModel = createViewModel()

        viewModel.setFieldTwo(-5)

        assertEquals(0, viewModel.state.first().fieldTwo)
    }

    @Test
    fun `toggleExpanded flips expansion state`() = runTest {
        val viewModel = createViewModel()

        assertFalse(viewModel.state.first().isExpanded)

        viewModel.toggleExpanded()
        assertTrue(viewModel.state.first().isExpanded)

        viewModel.toggleExpanded()
        assertFalse(viewModel.state.first().isExpanded)
    }

    @Test
    fun `isValid returns true when fieldOne is not blank and fieldTwo is positive`() = runTest {
        val viewModel = createViewModel()

        viewModel.setFieldOne("valid")
        viewModel.setFieldTwo(5)

        assertTrue(viewModel.state.first().isValid)
    }

    @Test
    fun `performAction returns null when state is invalid`() = runTest {
        val dependency = FakeSomeDependency()
        val viewModel = createViewModel(dependency)

        val result = viewModel.performAction()

        assertNull(result)
        assertTrue(dependency.actions.isEmpty())
    }

    @Test
    fun `performAction calls dependency and resets state when valid`() = runTest {
        val dependency = FakeSomeDependency()
        val viewModel = createViewModel(dependency)

        viewModel.setFieldOne("test")
        viewModel.setFieldTwo(42)
        viewModel.toggleExpanded()

        val result = viewModel.performAction()

        // Verify action was called
        assertEquals(1, dependency.actions.size)
        assertEquals("test" to 42, dependency.actions[0])

        // Verify state was reset but expansion preserved
        val state = viewModel.state.first()
        assertEquals("", state.fieldOne)
        assertEquals(0, state.fieldTwo)
        assertTrue(state.isExpanded)
    }

    @Test
    fun `reset clears form but preserves expansion`() = runTest {
        val viewModel = createViewModel()

        viewModel.setFieldOne("test")
        viewModel.setFieldTwo(10)
        viewModel.toggleExpanded()

        viewModel.reset()

        val state = viewModel.state.first()
        assertEquals("", state.fieldOne)
        assertEquals(0, state.fieldTwo)
        assertTrue(state.isExpanded) // Preserved
    }
}
```

### Step 3: Update the Composable

Modify the existing composable or create new one to use the ViewModel:

```kotlin
@Composable
fun MyFeaturePanel(
    viewModel: MyFeatureViewModel,
    onActionComplete: (SomeResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = modifier) {
        // Collapsible header
        Row(
            modifier = Modifier.clickable { viewModel.toggleExpanded() }
        ) {
            Text(if (state.isExpanded) "▼" else "▶")
            Text("My Feature")
        }

        if (state.isExpanded) {
            OutlinedTextField(
                value = state.fieldOne,
                onValueChange = { viewModel.setFieldOne(it) },
                label = { Text("Field One") }
            )

            // Number input for fieldTwo...

            Button(
                onClick = {
                    viewModel.performAction()?.let { onActionComplete(it) }
                },
                enabled = state.isValid
            ) {
                Text("Perform Action")
            }
        }
    }
}
```

### Step 4: Wire Up in Main.kt

Add the ViewModel creation and pass it to the composable:

```kotlin
// In GraphEditorApp or appropriate parent composable

// Create ViewModel with dependencies
val myFeatureViewModel = remember {
    MyFeatureViewModel(someDependency)
}

// Use in UI
MyFeaturePanel(
    viewModel = myFeatureViewModel,
    onActionComplete = { result ->
        // Handle the result, e.g., refresh state, show notification
    }
)
```

### Step 5: Run Tests

```bash
# Run the new tests
./gradlew :graphEditor:jvmTest --tests "io.codenode.grapheditor.viewmodel.MyFeatureViewModelTest"

# Verify all tests still pass
./gradlew :graphEditor:jvmTest
```

### Checklist for New ViewModel

- [ ] State data class implements `BaseState`
- [ ] State uses `val` properties only (immutable)
- [ ] State has computed properties for derived values
- [ ] ViewModel exposes `StateFlow<State>` (not MutableStateFlow)
- [ ] Actions are public methods with clear names
- [ ] Actions use `_state.update { it.copy(...) }` pattern
- [ ] Test file created with fake dependencies
- [ ] Tests cover: initial state, all actions, validation, edge cases
- [ ] Composable uses `collectAsState()` to observe state
- [ ] Composable has no business logic (only rendering and action calls)
- [ ] ViewModel does not import or reference other ViewModels directly

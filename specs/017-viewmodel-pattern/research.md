# Research: ViewModel Pattern Migration

**Feature**: 017-viewmodel-pattern
**Date**: 2026-02-16

## Research Summary

This document captures research findings for migrating the graphEditor module from its current state management pattern to a ViewModel-based architecture.

---

## Decision 1: ViewModel Implementation Approach

**Decision**: Use JetBrains Compose Multiplatform ViewModel library (`lifecycle-viewmodel-compose`)

**Rationale**:
- JetBrains provides official multiplatform ViewModel support: `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0-alpha06`
- This is the official Compose Multiplatform ViewModel implementation, aligned with Android's Jetpack ViewModel API
- Provides `viewModel()` composable function for ViewModel scoping and lifecycle management
- StateFlow provides reactive state that integrates well with Compose's `collectAsState()`
- Aligns with JetBrains/Google ecosystem (Apache 2.0 licensed)
- Coroutine-based state management aligns with existing kotlinx-coroutines usage

**Dependency to Add**:
```kotlin
// In graphEditor/build.gradle.kts commonMain dependencies:
implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0-alpha06")
```

**Alternatives Considered**:
| Alternative | Rejected Because |
|-------------|------------------|
| Custom ViewModel pattern | JetBrains official library available; prefer standard solution |
| Molecule library | JetBrains official ViewModel is better supported |
| MutableStateOf in ViewModel | Less testable; harder to observe in tests |
| Redux-like pattern | Over-engineered for current scale |
| Keep GraphState only | Doesn't solve scattered local state problem |

---

## Decision 2: Shared State Coordination

**Decision**: Use SharedStateProvider singleton pattern accessed via CompositionLocal

**Rationale**:
- Existing `GraphState`, `UndoRedoManager`, `PropertyChangeTracker` remain the source of truth
- ViewModels receive shared state via constructor injection
- CompositionLocal provides shared state through Compose tree
- Enables testing ViewModels in isolation with mock shared state

**Alternatives Considered**:
| Alternative | Rejected Because |
|-------------|------------------|
| Global singleton | Harder to test; tight coupling |
| Event bus | Adds complexity; harder to trace state changes |
| Direct ViewModel-to-ViewModel calls | Creates coupling between ViewModels |
| Parameter drilling | Already causing Main.kt bloat |

---

## Decision 3: ViewModel Extraction Order

**Decision**: Extract ViewModels in this order:
1. NodeGeneratorViewModel (simplest, isolated)
2. NodePaletteViewModel (search/filter state)
3. IPPaletteViewModel (selection state)
4. PropertiesPanelViewModel (medium complexity)
5. CanvasInteractionViewModel (most complex)
6. GraphEditorViewModel (orchestration, last)

**Rationale**:
- Start with simplest to establish patterns
- Build confidence before tackling complex canvas state
- Each step is independently testable/deployable
- GraphEditorViewModel last because it depends on all others

---

## Decision 4: Canvas State Migration Strategy

**Decision**: Keep some gesture-related state in FlowGraphCanvas as derived state

**Rationale**:
- Pointer position and gesture detection are tightly coupled to Compose gesture handlers
- Extracting all state to ViewModel adds unnecessary indirection
- Keep transient gesture state (drag offset, pointer position) in composable
- Move business state (selected nodes, connection source) to CanvasInteractionViewModel

**What stays in FlowGraphCanvas**:
- `pointerPosition` (transient gesture data)
- `shiftPressedAtPointerDown` (keyboard modifier state)

**What moves to CanvasInteractionViewModel**:
- `draggingNodeId` (business state)
- `pendingConnection` (business state)
- `selectionBoxBounds` (business state)

---

## Decision 5: Testing Strategy

**Decision**: ViewModels use StateFlow; tests verify state transitions without Compose

**Rationale**:
- StateFlow can be collected in unit tests without Compose
- Business logic tested independently from UI rendering
- Compose UI tests remain for integration testing
- Follows constitution principle II (Test-Driven Development)

**Test Pattern**:
```kotlin
@Test
fun `viewModel updates state correctly`() = runTest {
    val viewModel = NodeGeneratorViewModel(mockRepository)

    viewModel.setName("TestNode")
    viewModel.setInputCount(2)

    assertEquals("TestNode", viewModel.state.value.name)
    assertEquals(2, viewModel.state.value.inputCount)
    assertTrue(viewModel.state.value.isValid)
}
```

---

## Current State Analysis

### Files Requiring Migration

| File | Current State | Target ViewModel |
|------|---------------|------------------|
| NodeGeneratorPanel.kt | 3 local states (dropdowns, expansion) | NodeGeneratorViewModel |
| NodePalette.kt | 2 local states (search, expansion) | NodePaletteViewModel |
| IPPalette.kt | 1 local state (selected type) | IPPaletteViewModel |
| PropertiesPanel.kt | Complex editing state | PropertiesPanelViewModel |
| FlowGraphCanvas.kt | 8+ local states | CanvasInteractionViewModel |
| Main.kt | 12+ top-level states | GraphEditorViewModel |

### State to Remain in state/ Package

| Class | Reason |
|-------|--------|
| GraphState | Foundational; already well-structured |
| UndoRedoManager | Works well; ViewModels call it |
| PropertyChangeTracker | Works well; ViewModels call it |
| ViewSynchronizer | Separate concern (text sync) |
| IPTypeRegistry | Data registry; not state management |
| NodeGeneratorState | Data class; used by ViewModel |

---

## ViewModel Base Pattern

**Recommended ViewModel structure** (using JetBrains lifecycle-viewmodel-compose):

```kotlin
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ExampleViewModel(
    private val graphState: GraphState,
    private val undoRedoManager: UndoRedoManager
) : ViewModel() {
    // Internal mutable state
    private val _state = MutableStateFlow(ExampleState())

    // Public immutable state
    val state: StateFlow<ExampleState> = _state.asStateFlow()

    // Actions (called from UI)
    fun doSomething() {
        _state.update { it.copy(loading = true) }
        // Business logic...
        _state.update { it.copy(loading = false, result = result) }
    }
}

// Immutable state data class
data class ExampleState(
    val loading: Boolean = false,
    val result: String? = null
)
```

**Compose integration** (using viewModel() composable):

```kotlin
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ExampleScreen(
    viewModel: ExampleViewModel = viewModel { ExampleViewModel(graphState, undoRedoManager) }
) {
    val state by viewModel.state.collectAsState()

    // Pure rendering based on state
    if (state.loading) {
        LoadingIndicator()
    } else {
        Content(state.result)
    }
}
```

---

## Migration Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Breaking existing functionality | Comprehensive test coverage before migration |
| Performance regression | StateFlow is efficient; benchmark before/after |
| Partial migration leaving inconsistent state | Complete one ViewModel at a time; each is deployable |
| ViewModel coupling | Use shared state via injection, not direct calls |

---

## References

- [Compose State Documentation](https://developer.android.com/jetpack/compose/state)
- [StateFlow Documentation](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/)
- [MVI Pattern in Compose](https://developer.android.com/topic/architecture/ui-layer/stateholders)

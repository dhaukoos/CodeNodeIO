/*
 * SharedStateProvider - Dependency Injection for ViewModel Pattern
 * Provides shared state to ViewModels via CompositionLocal
 * License: Apache 2.0
 */

package io.codenode.grapheditor.viewmodel

import androidx.compose.runtime.staticCompositionLocalOf
import io.codenode.grapheditor.state.GraphState
import io.codenode.grapheditor.state.UndoRedoManager
import io.codenode.grapheditor.state.PropertyChangeTracker
import io.codenode.grapheditor.state.IPTypeRegistry
import io.codenode.grapheditor.repository.CustomNodeRepository

/**
 * Container for shared state that multiple ViewModels need access to.
 *
 * This class provides dependency injection for ViewModels without requiring
 * direct references between them. ViewModels receive their dependencies through
 * the SharedStateProvider rather than direct constructor injection from Main.kt.
 *
 * ## ViewModel State and Action Contracts
 *
 * All ViewModels in the graphEditor follow these contracts:
 *
 * ### State Contract
 * - State is exposed as `StateFlow<T>` where T implements `BaseState`
 * - State classes are immutable data classes with `val` properties only
 * - State updates use `_state.update { it.copy(...) }` pattern
 * - Computed properties derive values from state (e.g., `isValid`)
 *
 * ### Action Contract
 * - Actions are public methods on the ViewModel
 * - Actions do not return state - they update internal StateFlow
 * - Actions may return domain objects (e.g., `createNode(): CustomNodeDefinition?`)
 * - Actions that modify shared resources use injected dependencies
 *
 * ### Communication Contract
 * - ViewModels do NOT reference each other directly
 * - ViewModels communicate via:
 *   1. SharedStateProvider for accessing shared infrastructure
 *   2. Callbacks passed from parent composables for cross-component events
 *   3. Shared domain objects (GraphState, UndoRedoManager) for data synchronization
 *
 * ## Adding New Shared Dependencies
 *
 * When adding a new shared dependency:
 * 1. Add the property to this data class
 * 2. Update Main.kt to provide the instance
 * 3. Update any ViewModels that need access
 *
 * @property graphState The central graph state holder for the editor.
 *   Used by: CanvasInteractionViewModel, PropertiesPanelViewModel
 *
 * @property undoRedoManager Manager for undo/redo command execution.
 *   Used by: GraphEditorViewModel for undo/redo actions
 *
 * @property propertyChangeTracker Tracker for property changes supporting undo/redo.
 *   Used by: PropertiesPanelViewModel for tracked property updates
 *
 * @property ipTypeRegistry Registry of available InformationPacket types.
 *   Used by: IPPaletteViewModel for type selection
 *
 * @property customNodeRepository Repository for user-created custom node types.
 *   Used by: NodeGeneratorViewModel, NodePaletteViewModel
 */
data class SharedStateProvider(
    val graphState: GraphState,
    val undoRedoManager: UndoRedoManager,
    val propertyChangeTracker: PropertyChangeTracker,
    val ipTypeRegistry: IPTypeRegistry,
    val customNodeRepository: CustomNodeRepository
)

/**
 * CompositionLocal for accessing SharedStateProvider throughout the Compose tree.
 *
 * Usage:
 * ```kotlin
 * // In Main.kt - provide the shared state
 * CompositionLocalProvider(LocalSharedState provides sharedState) {
 *     // App content
 * }
 *
 * // In a ViewModel or composable - access the shared state
 * val sharedState = LocalSharedState.current
 * val graphState = sharedState.graphState
 * ```
 *
 * Throws an error if accessed before being provided in the composition tree.
 */
val LocalSharedState = staticCompositionLocalOf<SharedStateProvider> {
    error("SharedStateProvider not provided. Wrap your composables with CompositionLocalProvider(LocalSharedState provides sharedState) { ... }")
}

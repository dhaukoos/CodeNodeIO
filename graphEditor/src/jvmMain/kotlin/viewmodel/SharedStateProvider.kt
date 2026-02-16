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
 * @property graphState The central graph state holder for the editor
 * @property undoRedoManager Manager for undo/redo command execution
 * @property propertyChangeTracker Tracker for property changes supporting undo/redo
 * @property ipTypeRegistry Registry of available InformationPacket types
 * @property customNodeRepository Repository for user-created custom node types
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

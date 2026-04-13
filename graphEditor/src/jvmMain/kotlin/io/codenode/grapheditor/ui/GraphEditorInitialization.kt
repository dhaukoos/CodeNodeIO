/*
 * GraphEditor State Initialization
 * Extracts core state initialization from GraphEditorApp into a reusable composable.
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.runtime.*
import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.flowgraphinspect.registry.NodeDefinitionRegistry
import io.codenode.flowgraphpersist.state.GraphNodeTemplateRegistry
import io.codenode.flowgraphtypes.registry.IPTypeRegistry
import io.codenode.grapheditor.state.GraphState
import io.codenode.grapheditor.state.PropertyChangeTracker
import io.codenode.grapheditor.state.UndoRedoManager
import io.codenode.grapheditor.state.rememberPropertyChangeTracker
import io.codenode.grapheditor.state.rememberUndoRedoManager
import java.io.File

/**
 * Holds the core shared state for the GraphEditor application.
 *
 * This class contains the most commonly shared state variables that are
 * referenced across multiple parts of the editor. ViewModels, panel expansion
 * states, dialog states, and LaunchedEffects remain in GraphEditorApp.
 */
class GraphEditorState(
    val graphState: GraphState,
    val undoRedoManager: UndoRedoManager,
    val propertyChangeTracker: PropertyChangeTracker,
    val registry: NodeDefinitionRegistry,
    val graphNodeTemplateRegistry: GraphNodeTemplateRegistry,
    val ipTypeRegistry: IPTypeRegistry,
    val projectRoot: File,
    val moduleRootDir: MutableState<File?>,
    val statusMessage: MutableState<String>,
    val registryVersion: MutableState<Int>,
    val ipTypesVersion: MutableState<Int>,
)

/**
 * Creates and remembers the core [GraphEditorState].
 *
 * This composable initializes:
 * - An empty FlowGraph with [GraphState]
 * - [UndoRedoManager] and [PropertyChangeTracker] (via their remember-composables)
 * - [NodeDefinitionRegistry] and [GraphNodeTemplateRegistry]
 * - [IPTypeRegistry] with default types
 * - Project root resolution (from system property or walking up to settings.gradle.kts)
 * - Mutable state holders for moduleRootDir, statusMessage, registryVersion, and ipTypesVersion
 *
 * It does NOT set up LaunchedEffects, ViewModels, dialog state, or panel state.
 */
@Composable
fun rememberGraphEditorState(): GraphEditorState {
    val initialGraph = remember {
        flowGraph(name = "New Graph", version = "1.0.0") {
            // Empty graph
        }
    }
    val graphState = remember { GraphState(initialGraph) }
    val undoRedoManager = rememberUndoRedoManager()
    val propertyChangeTracker = rememberPropertyChangeTracker(undoRedoManager, graphState)

    val projectRoot = remember {
        val explicit = System.getProperty("codenode.project.dir")
        if (explicit != null) {
            File(explicit)
        } else {
            var dir = File(System.getProperty("user.dir"))
            while (dir.parentFile != null && !File(dir, "settings.gradle.kts").exists()) {
                dir = dir.parentFile
            }
            dir
        }
    }

    val registry = remember { NodeDefinitionRegistry() }
    val registryVersion = remember { mutableStateOf(0) }

    val graphNodeTemplateRegistry = remember { GraphNodeTemplateRegistry() }

    val ipTypeRegistry = remember { IPTypeRegistry.withDefaults() }

    val moduleRootDir = remember { mutableStateOf<File?>(null) }
    val statusMessage = remember { mutableStateOf("Ready - Create a new graph or open an existing one") }
    val ipTypesVersion = remember { mutableStateOf(0) }

    return remember(
        graphState,
        undoRedoManager,
        propertyChangeTracker,
        registry,
        graphNodeTemplateRegistry,
        ipTypeRegistry,
        projectRoot,
    ) {
        GraphEditorState(
            graphState = graphState,
            undoRedoManager = undoRedoManager,
            propertyChangeTracker = propertyChangeTracker,
            registry = registry,
            graphNodeTemplateRegistry = graphNodeTemplateRegistry,
            ipTypeRegistry = ipTypeRegistry,
            projectRoot = projectRoot,
            moduleRootDir = moduleRootDir,
            statusMessage = statusMessage,
            registryVersion = registryVersion,
            ipTypesVersion = ipTypesVersion,
        )
    }
}

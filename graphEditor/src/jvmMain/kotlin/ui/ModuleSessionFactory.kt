/*
 * ModuleSessionFactory - Creates RuntimeSession instances for discovered modules
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import io.codenode.circuitsimulator.RuntimeSession
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.fbpdsl.runtime.DynamicPipelineBuilder
import io.codenode.fbpdsl.runtime.DynamicPipelineController
import io.codenode.grapheditor.state.NodeDefinitionRegistry

/**
 * Factory for creating RuntimeSession instances for any discovered module.
 *
 * Uses DynamicPipelineController for all modules — no compile-time dependencies
 * on project module classes. Modules are discovered at runtime via the
 * NodeDefinitionRegistry.
 */
object ModuleSessionFactory {

    /** Registry for looking up compiled CodeNodeDefinitions by name */
    var registry: NodeDefinitionRegistry? = null

    /**
     * Creates a RuntimeSession for the given module.
     *
     * Uses DynamicPipelineController which builds the pipeline from the FlowGraph
     * and CodeNodeDefinition lookup at runtime.
     *
     * @param moduleName The module directory name (e.g., "StopWatch", "WeatherForecast")
     * @param editorFlowGraph The editor's FlowGraph for animation connection mapping
     * @param flowGraphProvider Provider returning the current canvas FlowGraph
     * @return A configured RuntimeSession, or null if the pipeline can't be built
     */
    fun createSession(
        moduleName: String,
        editorFlowGraph: FlowGraph? = null,
        flowGraphProvider: (() -> FlowGraph)? = null
    ): RuntimeSession? {
        val reg = registry ?: return null
        if (editorFlowGraph == null || flowGraphProvider == null) return null

        val lookup: (String) -> io.codenode.fbpdsl.runtime.CodeNodeDefinition? = { name -> reg.getByName(name) }
        if (!DynamicPipelineBuilder.canBuildDynamic(editorFlowGraph, lookup)) return null

        val controller = DynamicPipelineController(
            flowGraphProvider = flowGraphProvider,
            lookup = lookup
        )

        // Use a generic placeholder ViewModel — the runtime preview will use
        // composables discovered from the module's userInterface/ directory
        val viewModel = Any()

        return RuntimeSession(controller, viewModel, editorFlowGraph, flowGraphProvider = flowGraphProvider)
    }
}

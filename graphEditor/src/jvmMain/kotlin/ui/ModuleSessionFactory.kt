/*
 * ModuleSessionFactory - Creates RuntimeSession instances for each module
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import io.codenode.circuitsimulator.RuntimeSession
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.stopwatch.StopWatchViewModel
import io.codenode.stopwatch.generated.StopWatchController
import io.codenode.stopwatch.generated.StopWatchControllerAdapter
import io.codenode.stopwatch.stopWatchFlowGraph
import io.codenode.userprofiles.UserProfilesViewModel
import io.codenode.userprofiles.generated.UserProfilesController
import io.codenode.userprofiles.generated.UserProfilesControllerAdapter
import io.codenode.persistence.UserProfileDao
import io.codenode.persistence.GeoLocationDao
import io.codenode.userprofiles.userProfilesFlowGraph
import io.codenode.geolocations.GeoLocationsViewModel
import io.codenode.geolocations.generated.GeoLocationsController
import io.codenode.geolocations.generated.GeoLocationsControllerAdapter
import io.codenode.geolocations.geoLocationsFlowGraph
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import io.codenode.addresses.AddressesViewModel
import io.codenode.addresses.generated.AddressesController
import io.codenode.addresses.generated.AddressesControllerAdapter
import io.codenode.persistence.AddressDao
import io.codenode.addresses.addressesFlowGraph
import io.codenode.edgeartfilter.EdgeArtFilterViewModel
import io.codenode.edgeartfilter.generated.EdgeArtFilterController
import io.codenode.edgeartfilter.generated.EdgeArtFilterControllerAdapter
import io.codenode.edgeartfilter.generated.EdgeArtFilterControllerInterface
import io.codenode.edgeartfilter.edgeArtFilterFlowGraph
import io.codenode.stopwatch.StopWatchState
import io.codenode.stopwatch.generated.StopWatchControllerInterface
import io.codenode.fbpdsl.runtime.DynamicPipelineBuilder
import io.codenode.fbpdsl.runtime.DynamicPipelineController
import io.codenode.grapheditor.state.NodeDefinitionRegistry

/**
 * Factory for creating module-specific RuntimeSession instances.
 *
 * Each supported module has a dedicated factory method that creates the
 * controller, adapter, viewmodel, and wraps them in a RuntimeSession.
 */
object ModuleSessionFactory : KoinComponent {

    private val userProfileDao: UserProfileDao by inject()
    private val geoLocationDao: GeoLocationDao by inject()
    private val addressDao: AddressDao by inject()

    /** Registry for looking up compiled CodeNodeDefinitions by name */
    var registry: NodeDefinitionRegistry? = null

    /**
     * Creates a RuntimeSession for the given module name.
     *
     * If all canvas nodes have CodeNodeDefinitions in the registry, creates a
     * DynamicPipelineController for dynamic pipeline execution. Otherwise falls
     * back to the module's existing generated Controller/Flow.
     *
     * @param moduleName The module directory name (e.g., "StopWatch", "UserProfiles")
     * @param editorFlowGraph The editor's FlowGraph for animation connection mapping.
     *   This must be the same FlowGraph instance that the Canvas uses to render connections,
     *   so that animation connection IDs match the rendered connections.
     * @param flowGraphProvider Optional provider returning the current canvas FlowGraph.
     *   Used by DynamicPipelineController to re-read the FlowGraph on each start().
     * @return A configured RuntimeSession, or null for unknown modules
     */
    fun createSession(
        moduleName: String,
        editorFlowGraph: FlowGraph? = null,
        flowGraphProvider: (() -> FlowGraph)? = null
    ): RuntimeSession? {
        // Try dynamic pipeline if registry is available and FlowGraph has nodes
        val reg = registry
        if (reg != null && editorFlowGraph != null && flowGraphProvider != null) {
            val lookup: (String) -> io.codenode.fbpdsl.runtime.CodeNodeDefinition? = { name -> reg.getByName(name) }
            if (DynamicPipelineBuilder.canBuildDynamic(editorFlowGraph, lookup)) {
                return createDynamicSession(moduleName, editorFlowGraph, flowGraphProvider, lookup)
            }
        }

        // Fallback to module-specific factory
        return when (moduleName) {
            "StopWatch" -> createStopWatchSession(editorFlowGraph)
            "UserProfiles" -> createUserProfilesSession(editorFlowGraph)
            "GeoLocations" -> createGeoLocationsSession(editorFlowGraph)
            "Addresses" -> createAddressesSession(editorFlowGraph)
            "EdgeArtFilter" -> createEdgeArtFilterSession(editorFlowGraph)
            else -> null
        }
    }

    private fun createStopWatchSession(editorFlowGraph: FlowGraph?): RuntimeSession {
        val controller = StopWatchController(stopWatchFlowGraph)
        val adapter = StopWatchControllerAdapter(controller)
        val viewModel = StopWatchViewModel(adapter)
        return RuntimeSession(controller, viewModel, editorFlowGraph ?: stopWatchFlowGraph)
    }

    private fun createUserProfilesSession(editorFlowGraph: FlowGraph?): RuntimeSession {
        val controller = UserProfilesController(userProfilesFlowGraph)
        controller.start()
        val adapter = UserProfilesControllerAdapter(controller)
        val viewModel = UserProfilesViewModel(adapter, userProfileDao)
        return RuntimeSession(controller, viewModel, editorFlowGraph ?: userProfilesFlowGraph)
    }

    private fun createGeoLocationsSession(editorFlowGraph: FlowGraph?): RuntimeSession {
        val controller = GeoLocationsController(geoLocationsFlowGraph)
        controller.start()
        val adapter = GeoLocationsControllerAdapter(controller)
        val viewModel = GeoLocationsViewModel(adapter, geoLocationDao)
        return RuntimeSession(controller, viewModel, editorFlowGraph ?: geoLocationsFlowGraph)
    }

    private fun createAddressesSession(editorFlowGraph: FlowGraph?): RuntimeSession {
        val controller = AddressesController(addressesFlowGraph)
        controller.start()
        val adapter = AddressesControllerAdapter(controller)
        val viewModel = AddressesViewModel(adapter, addressDao)
        return RuntimeSession(controller, viewModel, editorFlowGraph ?: addressesFlowGraph)
    }

    /**
     * Creates a RuntimeSession using DynamicPipelineController.
     * The controller reads the canvas FlowGraph on each start() call.
     * The ViewModel is still created per-module for module-specific UI state.
     */
    private fun createDynamicSession(
        moduleName: String,
        editorFlowGraph: FlowGraph,
        flowGraphProvider: () -> FlowGraph,
        lookup: (String) -> io.codenode.fbpdsl.runtime.CodeNodeDefinition?
    ): RuntimeSession? {
        // Determine module-specific reset callback
        val onReset: (() -> Unit)? = when (moduleName) {
            "StopWatch" -> { { StopWatchState.reset() } }
            else -> null
        }

        val controller = DynamicPipelineController(
            flowGraphProvider = flowGraphProvider,
            lookup = lookup,
            onReset = onReset
        )

        // Create the module-specific ViewModel using an adapter from ModuleController
        val viewModel: Any = when (moduleName) {
            "StopWatch" -> {
                val adapter = object : StopWatchControllerInterface {
                    override val elapsedSeconds get() = StopWatchState.elapsedSecondsFlow
                    override val elapsedMinutes get() = StopWatchState.elapsedMinutesFlow
                    override val seconds get() = StopWatchState.secondsFlow
                    override val minutes get() = StopWatchState.minutesFlow
                    override val executionState get() = controller.executionState
                    override fun start(): FlowGraph { controller.start(); return flowGraphProvider() }
                    override fun stop(): FlowGraph { controller.stop(); return flowGraphProvider() }
                    override fun reset(): FlowGraph { controller.reset(); return flowGraphProvider() }
                    override fun pause(): FlowGraph { controller.pause(); return flowGraphProvider() }
                    override fun resume(): FlowGraph { controller.resume(); return flowGraphProvider() }
                }
                StopWatchViewModel(adapter)
            }
            "EdgeArtFilter" -> {
                val adapter = object : EdgeArtFilterControllerInterface {
                    override val executionState get() = controller.executionState
                    override fun start() = controller.start()
                    override fun stop() = controller.stop()
                    override fun reset() = controller.reset()
                    override fun pause() = controller.pause()
                    override fun resume() = controller.resume()
                }
                EdgeArtFilterViewModel(adapter)
            }
            else -> return null // Unknown module — can't create ViewModel
        }

        return RuntimeSession(controller, viewModel, editorFlowGraph, flowGraphProvider = flowGraphProvider)
    }

    private fun createEdgeArtFilterSession(editorFlowGraph: FlowGraph?): RuntimeSession {
        val controller = EdgeArtFilterController(edgeArtFilterFlowGraph)
        wireNodeDefinitionLookup(controller)
        val adapter = EdgeArtFilterControllerAdapter(controller)
        val viewModel = EdgeArtFilterViewModel(adapter)
        return RuntimeSession(controller, viewModel, editorFlowGraph ?: edgeArtFilterFlowGraph)
    }

    /**
     * Wires the NodeDefinitionRegistry lookup into a controller so it can
     * resolve node names to CodeNodeDefinitions for dynamic runtime creation.
     */
    private fun wireNodeDefinitionLookup(controller: io.codenode.fbpdsl.runtime.ModuleController) {
        val reg = registry ?: return
        controller.nodeDefinitionLookup = { name -> reg.getByName(name) }
    }
}

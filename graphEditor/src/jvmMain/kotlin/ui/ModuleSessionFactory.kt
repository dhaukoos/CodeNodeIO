/*
 * ModuleSessionFactory - Creates RuntimeSession instances for each module
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import io.codenode.circuitsimulator.RuntimeSession
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.stopwatch.StopWatchViewModel
import io.codenode.stopwatch.StopWatchState
import io.codenode.stopwatch.generated.StopWatchControllerInterface
import io.codenode.userprofiles.UserProfilesViewModel
import io.codenode.userprofiles.UserProfilesState
import io.codenode.userprofiles.generated.UserProfilesControllerInterface
import io.codenode.persistence.UserProfileDao
import io.codenode.persistence.GeoLocationDao
import io.codenode.persistence.AddressDao
import io.codenode.geolocations.GeoLocationsViewModel
import io.codenode.geolocations.GeoLocationsState
import io.codenode.geolocations.generated.GeoLocationsControllerInterface
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import io.codenode.addresses.AddressesViewModel
import io.codenode.addresses.AddressesState
import io.codenode.addresses.generated.AddressesControllerInterface
import io.codenode.edgeartfilter.EdgeArtFilterViewModel
import io.codenode.weatherforecast.WeatherForecastViewModel
import io.codenode.weatherforecast.WeatherForecastState
import io.codenode.weatherforecast.generated.WeatherForecastControllerInterface
import io.codenode.edgeartfilter.generated.EdgeArtFilterController
import io.codenode.edgeartfilter.generated.EdgeArtFilterControllerAdapter
import io.codenode.edgeartfilter.generated.EdgeArtFilterControllerInterface
import io.codenode.edgeartfilter.edgeArtFilterFlowGraph
import io.codenode.fbpdsl.runtime.DynamicPipelineBuilder
import io.codenode.fbpdsl.runtime.DynamicPipelineController
import io.codenode.grapheditor.state.NodeDefinitionRegistry

/**
 * Factory for creating module-specific RuntimeSession instances.
 *
 * Modules with registered CodeNodeDefinitions (StopWatch, UserProfiles,
 * GeoLocations, Addresses) use DynamicPipelineController for runtime
 * pipeline construction. EdgeArtFilter still uses its generated controller
 * as fallback until its migration is complete.
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
     * back to EdgeArtFilter's generated Controller/Flow (the only module that
     * still needs it).
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

        // Fallback for EdgeArtFilter (only module still using generated controller)
        return when (moduleName) {
            "EdgeArtFilter" -> createEdgeArtFilterSession(editorFlowGraph)
            else -> null
        }
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
            "UserProfiles" -> { { UserProfilesState.reset() } }
            "GeoLocations" -> { { GeoLocationsState.reset() } }
            "Addresses" -> { { AddressesState.reset() } }
            "WeatherForecast" -> { { WeatherForecastState.reset() } }
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
            "UserProfiles" -> {
                val adapter = object : UserProfilesControllerInterface {
                    override val save get() = UserProfilesState.saveFlow
                    override val update get() = UserProfilesState.updateFlow
                    override val remove get() = UserProfilesState.removeFlow
                    override val result get() = UserProfilesState.resultFlow
                    override val error get() = UserProfilesState.errorFlow
                    override val executionState get() = controller.executionState
                    override fun start(): FlowGraph { controller.start(); return flowGraphProvider() }
                    override fun stop(): FlowGraph { controller.stop(); return flowGraphProvider() }
                    override fun reset(): FlowGraph { controller.reset(); return flowGraphProvider() }
                    override fun pause(): FlowGraph { controller.pause(); return flowGraphProvider() }
                    override fun resume(): FlowGraph { controller.resume(); return flowGraphProvider() }
                }
                UserProfilesViewModel(adapter, userProfileDao)
            }
            "GeoLocations" -> {
                val adapter = object : GeoLocationsControllerInterface {
                    override val save get() = GeoLocationsState.saveFlow
                    override val update get() = GeoLocationsState.updateFlow
                    override val remove get() = GeoLocationsState.removeFlow
                    override val result get() = GeoLocationsState.resultFlow
                    override val error get() = GeoLocationsState.errorFlow
                    override val executionState get() = controller.executionState
                    override fun start(): FlowGraph { controller.start(); return flowGraphProvider() }
                    override fun stop(): FlowGraph { controller.stop(); return flowGraphProvider() }
                    override fun reset(): FlowGraph { controller.reset(); return flowGraphProvider() }
                    override fun pause(): FlowGraph { controller.pause(); return flowGraphProvider() }
                    override fun resume(): FlowGraph { controller.resume(); return flowGraphProvider() }
                }
                GeoLocationsViewModel(adapter, geoLocationDao)
            }
            "Addresses" -> {
                val adapter = object : AddressesControllerInterface {
                    override val save get() = AddressesState.saveFlow
                    override val update get() = AddressesState.updateFlow
                    override val remove get() = AddressesState.removeFlow
                    override val result get() = AddressesState.resultFlow
                    override val error get() = AddressesState.errorFlow
                    override val executionState get() = controller.executionState
                    override fun start(): FlowGraph { controller.start(); return flowGraphProvider() }
                    override fun stop(): FlowGraph { controller.stop(); return flowGraphProvider() }
                    override fun reset(): FlowGraph { controller.reset(); return flowGraphProvider() }
                    override fun pause(): FlowGraph { controller.pause(); return flowGraphProvider() }
                    override fun resume(): FlowGraph { controller.resume(); return flowGraphProvider() }
                }
                AddressesViewModel(adapter, addressDao)
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
            "WeatherForecast" -> {
                val adapter = object : WeatherForecastControllerInterface {
                    override val executionState get() = controller.executionState
                    override fun start(): FlowGraph { controller.start(); return flowGraphProvider() }
                    override fun stop(): FlowGraph { controller.stop(); return flowGraphProvider() }
                    override fun reset(): FlowGraph { controller.reset(); return flowGraphProvider() }
                    override fun pause(): FlowGraph { controller.pause(); return flowGraphProvider() }
                    override fun resume(): FlowGraph { controller.resume(); return flowGraphProvider() }
                }
                WeatherForecastViewModel(adapter)
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

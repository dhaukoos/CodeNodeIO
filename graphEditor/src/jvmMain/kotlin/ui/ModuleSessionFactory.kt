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
import io.codenode.userprofiles.persistence.UserProfileDao
import io.codenode.userprofiles.userProfilesFlowGraph
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Factory for creating module-specific RuntimeSession instances.
 *
 * Each supported module has a dedicated factory method that creates the
 * controller, adapter, viewmodel, and wraps them in a RuntimeSession.
 */
object ModuleSessionFactory : KoinComponent {

    private val userProfileDao: UserProfileDao by inject()

    /**
     * Creates a RuntimeSession for the given module name.
     *
     * @param moduleName The module directory name (e.g., "StopWatch", "UserProfiles")
     * @param editorFlowGraph The editor's FlowGraph for animation connection mapping.
     *   This must be the same FlowGraph instance that the Canvas uses to render connections,
     *   so that animation connection IDs match the rendered connections.
     * @return A configured RuntimeSession, or null for unknown modules
     */
    fun createSession(moduleName: String, editorFlowGraph: FlowGraph? = null): RuntimeSession? {
        return when (moduleName) {
            "StopWatch" -> createStopWatchSession(editorFlowGraph)
            "UserProfiles" -> createUserProfilesSession(editorFlowGraph)
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
}

/*
 * ModuleSessionFactory - Creates RuntimeSession instances for each module
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import io.codenode.circuitsimulator.RuntimeSession
import io.codenode.stopwatch.StopWatchViewModel
import io.codenode.stopwatch.generated.StopWatchController
import io.codenode.stopwatch.generated.StopWatchControllerAdapter
import io.codenode.stopwatch.stopWatchFlowGraph
import io.codenode.userprofiles.UserProfilesViewModel
import io.codenode.userprofiles.generated.UserProfilesController
import io.codenode.userprofiles.generated.UserProfilesControllerAdapter
import io.codenode.userprofiles.userProfilesFlowGraph

/**
 * Factory for creating module-specific RuntimeSession instances.
 *
 * Each supported module has a dedicated factory method that creates the
 * controller, adapter, viewmodel, and wraps them in a RuntimeSession.
 */
object ModuleSessionFactory {

    /**
     * Creates a RuntimeSession for the given module name.
     *
     * @param moduleName The module directory name (e.g., "StopWatch", "UserProfiles")
     * @return A configured RuntimeSession, or null for unknown modules
     */
    fun createSession(moduleName: String): RuntimeSession? {
        return when (moduleName) {
            "StopWatch" -> createStopWatchSession()
            "UserProfiles" -> createUserProfilesSession()
            else -> null
        }
    }

    private fun createStopWatchSession(): RuntimeSession {
        val controller = StopWatchController(stopWatchFlowGraph)
        val adapter = StopWatchControllerAdapter(controller)
        val viewModel = StopWatchViewModel(adapter)
        return RuntimeSession(controller, viewModel)
    }

    private fun createUserProfilesSession(): RuntimeSession {
        val controller = UserProfilesController(userProfilesFlowGraph)
        controller.start()
        val adapter = UserProfilesControllerAdapter(controller)
        val viewModel = UserProfilesViewModel(adapter)
        return RuntimeSession(controller, viewModel)
    }
}

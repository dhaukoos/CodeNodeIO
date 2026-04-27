/*
 * TestFakeControllerInterface — fixture for ModuleSessionFactoryRegressionTest
 * License: Apache 2.0
 */

package io.codenode.testfake.controller

import io.codenode.fbpdsl.runtime.ModuleController
import kotlinx.coroutines.flow.StateFlow

/**
 * Synthetic per-module ControllerInterface fixture matching the post-collapse
 * shape every regenerated module satisfies: extends [ModuleController], declares
 * exactly the typed state-flow getters that boundary ports expose. Used by
 * `ModuleSessionFactoryRegressionTest` to validate the GraphEditor's reflection-
 * proxy contract without depending on any of the live DemoProject modules.
 */
interface TestFakeControllerInterface : ModuleController {
    val value: StateFlow<Int>
}

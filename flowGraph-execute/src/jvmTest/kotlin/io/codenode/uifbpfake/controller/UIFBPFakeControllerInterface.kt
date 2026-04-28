/*
 * UIFBPFakeControllerInterface — synthetic UI-FBP-shaped controller interface
 * License: Apache 2.0
 */

package io.codenode.uifbpfake.controller

import io.codenode.fbpdsl.runtime.ModuleController
import kotlinx.coroutines.flow.StateFlow

/**
 * Synthetic UI-FBP-shaped ControllerInterface for [UIFBPModuleSessionFactoryTest].
 *
 * Differs from [io.codenode.testfake.controller.TestFakeControllerInterface] in two ways
 * that exercise UI-FBP-specific shape: (a) two sink-input flows (UI-FBP modules typically
 * declare multiple typed Sink ports the UI observes); (b) one of them is nullable
 * (matches the IP-type result-pattern, e.g., `CalculationResults?`). Both reflect the
 * post-085 universal contract — extending `ModuleController` and adding typed
 * `val y: StateFlow<T>` members per Decision 2 of feature 084 spec.
 */
interface UIFBPFakeControllerInterface : ModuleController {
    val results: StateFlow<Int?>
    val displayed: StateFlow<String>
}

/*
 * TestFakeViewModel + TestFakeState — fixtures for ModuleSessionFactoryRegressionTest
 * License: Apache 2.0
 */

package io.codenode.testfake.viewmodel

import io.codenode.fbpdsl.model.FlowExecutionStatus
import io.codenode.testfake.controller.TestFakeControllerInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Synthetic State object satisfying the universal-runtime contract:
 * - Kotlin `object` (provides INSTANCE field for reflective lookup)
 * - lives at `io.codenode.{modulename}.viewmodel.{Module}State` (canonical layout)
 * - exposes `{x}Flow: StateFlow<T>` for every typed getter declared on the interface
 * - provides `fun reset()`
 */
object TestFakeState {
    internal val _value = MutableStateFlow(0)
    val valueFlow: StateFlow<Int> = _value.asStateFlow()

    fun reset() {
        _value.value = 0
    }
}

/**
 * Synthetic ViewModel matching the universal-runtime contract:
 * - public single-arg constructor accepting `{Module}ControllerInterface`
 * - lives at `io.codenode.{modulename}.viewmodel.{Module}ViewModel`
 * - reads typed flows directly from `{Module}State.{x}Flow` (not via the controller)
 *
 * Note: deliberately does NOT extend `androidx.lifecycle.ViewModel` — that
 * dependency isn't on flowGraph-execute's test classpath, and
 * `ModuleSessionFactory.tryCreateViewModel` only requires constructor
 * compatibility, not a specific superclass.
 */
class TestFakeViewModel(private val controller: TestFakeControllerInterface) {
    val value: StateFlow<Int> = TestFakeState.valueFlow

    /**
     * Routes a `getStatus()` call through the GraphEditor's reflection proxy
     * (when constructed via [ModuleSessionFactory.createSession]) so the
     * regression test can verify the T010 `"getStatus" -> controller.getStatus()`
     * proxy update is wired correctly.
     */
    fun statusViaProxy(): FlowExecutionStatus = controller.getStatus()
}

/*
 * UIFBPFakeViewModel + UIFBPFakeState — synthetic UI-FBP fixtures
 *
 * Exercises the post-085 universal contract for the UI-FBP flavor:
 *   - State at io.codenode.{module}.viewmodel.{Module}State (canonical)
 *   - ViewModel constructor takes ({Module}ControllerInterface)
 *   - Sink-input flows on the interface map to {y}Flow fields on State
 *   - emit(...) writes to State mutable fields (UI → State → SourceCodeNode)
 *
 * License: Apache 2.0
 */

package io.codenode.uifbpfake.viewmodel

import io.codenode.fbpdsl.model.FlowExecutionStatus
import io.codenode.uifbpfake.controller.UIFBPFakeControllerInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UIFBPFakeState {
    // Sink-input flows (UI observes)
    internal val _results = MutableStateFlow<Int?>(null)
    val resultsFlow: StateFlow<Int?> = _results.asStateFlow()

    internal val _displayed = MutableStateFlow("")
    val displayedFlow: StateFlow<String> = _displayed.asStateFlow()

    // Source-output mutable fields (the UI writes here via emit(); SourceCodeNode reads them)
    internal val _input = MutableStateFlow(0)
    val inputFlow: StateFlow<Int> = _input.asStateFlow()

    fun reset() {
        _results.value = null
        _displayed.value = ""
        _input.value = 0
    }
}

class UIFBPFakeViewModel(private val controller: UIFBPFakeControllerInterface) {
    val results: StateFlow<Int?> = UIFBPFakeState.resultsFlow
    val displayed: StateFlow<String> = UIFBPFakeState.displayedFlow

    fun emit(input: Int) {
        UIFBPFakeState._input.value = input
    }

    /**
     * Mirror of TestFakeViewModel.statusViaProxy — proves the post-085
     * `getStatus` proxy case routes for UI-FBP-shaped interfaces.
     */
    fun statusViaProxy(): FlowExecutionStatus = controller.getStatus()
}

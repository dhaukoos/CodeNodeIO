/*
 * RecompileViewModel - viewmodel for the per-module recompile toolbar action
 *
 * Owns the idle/compiling state machine; routes invocations through the session-level
 * RecompileSession + a background CoroutineScope so the UI stays responsive while
 * kotlinc runs.
 *
 * License: Apache 2.0
 */

package io.codenode.grapheditor.viewmodel

import io.codenode.flowgraphinspect.compile.CompileUnit
import io.codenode.flowgraphinspect.compile.RecompileResult
import io.codenode.grapheditor.compile.RecompileSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives the toolbar's "Recompile module" affordance.
 *
 * Public state flows:
 *  - [isCompiling]: true while a compile is in-flight (button shows busy state).
 *  - [lastResult]: most-recent [RecompileResult]; null until the first invocation.
 *
 * Concurrency: UI calls [recompile] which launches into [bgScope]; the underlying
 * [RecompileSession] serializes via its own Mutex, so concurrent UI clicks queue
 * rather than overlap.
 */
class RecompileViewModel(
    private val session: RecompileSession,
    private val bgScope: CoroutineScope
) {
    private val _isCompiling = MutableStateFlow(false)
    val isCompiling: StateFlow<Boolean> = _isCompiling.asStateFlow()

    private val _lastResult = MutableStateFlow<RecompileResult?>(null)
    val lastResult: StateFlow<RecompileResult?> = _lastResult.asStateFlow()

    /**
     * Recompile [unit]. Fire-and-forget at the call site; observe outcome via
     * [isCompiling] / [lastResult]. The publisher attached to the session also
     * surfaces the structured result to the error console + status bar.
     */
    fun recompile(unit: CompileUnit) {
        bgScope.launch {
            _isCompiling.value = true
            try {
                val result = session.recompile(unit)
                _lastResult.value = result
            } finally {
                _isCompiling.value = false
            }
        }
    }
}

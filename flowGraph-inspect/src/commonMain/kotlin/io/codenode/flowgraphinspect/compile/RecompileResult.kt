/*
 * RecompileResult - aggregate result surfaced to the GraphEditor UI
 * License: Apache 2.0
 */

package io.codenode.flowgraphinspect.compile

/**
 * Aggregate result of one user-visible recompile attempt. Wraps a [CompileResult] plus
 * session-level metadata (timing, pipeline-quiesce count) so the UI surfaces a
 * structured summary per FR-009.
 *
 * @property compileResult The underlying compile outcome.
 * @property durationMs Wall-clock duration of the recompile in milliseconds.
 * @property pipelinesQuiesced Count of running pipelines that were stopped before this
 *     compile (FR-014). Zero when no pipeline was running.
 * @property warningSummary Optional one-line summary appended to the status feedback
 *     (e.g., "Stopped 1 running pipeline before recompile").
 */
data class RecompileResult(
    val compileResult: CompileResult,
    val durationMs: Long,
    val pipelinesQuiesced: Int = 0,
    val warningSummary: String? = null
) {
    /** True iff the underlying compile succeeded. */
    val success: Boolean get() = compileResult is CompileResult.Success

    /** The unit that was recompiled (delegates to [compileResult]). */
    val unit: CompileUnit get() = compileResult.unit
}

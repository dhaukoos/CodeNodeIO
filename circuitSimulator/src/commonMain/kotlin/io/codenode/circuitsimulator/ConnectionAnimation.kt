/*
 * ConnectionAnimation - Represents an active dot animation on a connection
 * License: Apache 2.0
 */

package io.codenode.circuitsimulator

/**
 * Represents a single dot animation traveling along a connection curve.
 *
 * Created when an IP is emitted from a node's output port.
 * The dot travels from the source port to the target port over [durationMs].
 *
 * @property connectionId The ID of the connection this animation travels along
 * @property startTimeMs System time (ms) when the animation started
 * @property durationMs How long the animation takes to complete (ms)
 */
data class ConnectionAnimation(
    val connectionId: String,
    val startTimeMs: Long,
    val durationMs: Long
) {
    /**
     * Computes the current progress of this animation.
     *
     * @param currentTimeMs Current system time in milliseconds
     * @return Progress value clamped to [0, 1] where 0 = start, 1 = complete
     */
    fun progress(currentTimeMs: Long): Float {
        if (durationMs <= 0) return 1f
        val elapsed = currentTimeMs - startTimeMs
        return (elapsed.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Checks if this animation has completed.
     *
     * @param currentTimeMs Current system time in milliseconds
     * @return true if the animation has reached or passed its end time
     */
    fun isComplete(currentTimeMs: Long): Boolean {
        return progress(currentTimeMs) >= 1f
    }
}

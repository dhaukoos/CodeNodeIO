/*
 * NodeAutoCompileHook - thin abstraction the Node Generator calls after writing a
 * generated CodeNode source file. Lets graphEditor wire its in-process recompile
 * pipeline into NodeGeneratorViewModel without flowGraph-generate depending on
 * graphEditor (the dependency direction is graphEditor → flowGraph-generate).
 *
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.viewmodel

import io.codenode.fbpdsl.model.PlacementLevel
import java.io.File

/**
 * Receives "a new CodeNode source file just landed on disk" notifications from
 * [NodeGeneratorViewModel.generateCodeNode]. Implementations fire-and-forget — the
 * viewmodel does not await completion (the user sees palette refresh asynchronously
 * once the underlying recompile + registry-install finishes).
 *
 * The default [NoOp] implementation skips the auto-compile path entirely; this is the
 * pre-feature-086 behavior (the user must rebuild + relaunch the GraphEditor manually
 * for the new node to appear). The graphEditor module supplies a real implementation
 * that delegates to [io.codenode.grapheditor.compile.RecompileSession.recompileGenerated].
 */
fun interface NodeAutoCompileHook {
    /**
     * Fired once per successful Node Generator invocation, AFTER the source file is
     * written to disk. Implementations should return promptly; long-running work (the
     * actual compile) goes onto a coroutine inside the implementation.
     *
     * @param file The newly-written source file, absolute path.
     * @param tier Placement tier the source was generated for.
     * @param hostModule Gradle module name. Null only when [tier] is UNIVERSAL.
     */
    fun onGenerated(file: File, tier: PlacementLevel, hostModule: String?)

    companion object {
        /** Pre-feature-086 behavior: do nothing. Useful as a default for unit tests. */
        val NoOp: NodeAutoCompileHook = NodeAutoCompileHook { _, _, _ -> }
    }
}

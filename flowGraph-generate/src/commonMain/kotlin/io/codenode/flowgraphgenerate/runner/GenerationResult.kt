/*
 * GenerationResult - Output of code generation pipeline execution
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.runner

data class GenerationResult(
    val generatedFiles: Map<String, String> = emptyMap(),
    val errors: Map<String, String> = emptyMap(),
    val skipped: Set<String> = emptySet()
) {
    val totalGenerated: Int get() = generatedFiles.size
    val totalErrors: Int get() = errors.size
    val totalSkipped: Int get() = skipped.size
    val isSuccess: Boolean get() = errors.isEmpty()
}

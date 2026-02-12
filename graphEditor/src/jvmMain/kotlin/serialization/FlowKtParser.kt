/*
 * FlowKtParser
 * Parses .flow.kt files back to FlowGraph
 * License: Apache 2.0
 */

package io.codenode.grapheditor.serialization

import io.codenode.fbpdsl.model.FlowGraph

/**
 * Result of parsing a .flow.kt file.
 *
 * @property isSuccess Whether parsing succeeded
 * @property graph The parsed FlowGraph (if successful)
 * @property errorMessage Error message (if failed)
 */
data class ParseResult(
    val isSuccess: Boolean,
    val graph: FlowGraph? = null,
    val errorMessage: String? = null
)

/**
 * Parser for .flow.kt compiled Kotlin files.
 *
 * Parses a .flow.kt file back into a FlowGraph model for editing.
 * This enables round-trip serialization: FlowGraph → .flow.kt → FlowGraph
 *
 * T016: Parse .flow.kt to FlowGraph
 * T017: Round-trip equality
 */
class FlowKtParser {

    /**
     * Parses .flow.kt content into a FlowGraph.
     *
     * @param content The .flow.kt file content
     * @return ParseResult with success status and parsed graph
     */
    fun parseFlowKt(content: String): ParseResult {
        // TODO: T023-T026 - Implement FlowKtParser
        // This stub returns failure so tests compile but fail
        return ParseResult(
            isSuccess = false,
            errorMessage = "FlowKtParser not yet implemented"
        )
    }
}

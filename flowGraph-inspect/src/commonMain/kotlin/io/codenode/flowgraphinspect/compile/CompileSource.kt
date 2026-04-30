/*
 * CompileSource - identifies a single source file for the in-process compiler
 * License: Apache 2.0
 */

package io.codenode.flowgraphinspect.compile

import io.codenode.fbpdsl.model.PlacementLevel

/**
 * One source file the compiler will process.
 *
 * @property absolutePath Canonical absolute path on disk.
 * @property tier Placement tier the source belongs to (MODULE / PROJECT / UNIVERSAL).
 * @property hostModuleName Gradle module name. Required for MODULE and PROJECT tiers;
 *     null for UNIVERSAL tier sources (which live outside any module's source tree at
 *     `~/.codenode/nodes/`).
 */
data class CompileSource(
    val absolutePath: String,
    val tier: PlacementLevel,
    val hostModuleName: String?
) {
    init {
        require(absolutePath.isNotBlank()) { "CompileSource.absolutePath must not be blank" }
        when (tier) {
            PlacementLevel.MODULE, PlacementLevel.PROJECT -> require(hostModuleName != null) {
                "CompileSource.hostModuleName is required for tier=$tier"
            }
            PlacementLevel.UNIVERSAL -> {
                // hostModuleName is permitted-null (synthetic compile unit at ~/.codenode/nodes/)
            }
            PlacementLevel.INTERNAL -> require(hostModuleName != null) {
                "CompileSource.hostModuleName is required for tier=INTERNAL"
            }
        }
    }
}

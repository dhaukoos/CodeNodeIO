/*
 * PlacementLevel - Shared enum for filesystem tier placement
 * Used by both Node Generator and IP Generator panels
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

/**
 * Placement level for generated files (nodes and IP types).
 * Determines the filesystem tier where the file is stored.
 *
 * @property displayName Human-readable name shown in dropdowns
 */
enum class PlacementLevel(val displayName: String) {
    MODULE("Module"),
    PROJECT("Project"),
    UNIVERSAL("Universal");

    companion object {
        /**
         * Returns available placement levels based on whether a module is loaded.
         * MODULE is only available when a module is actively loaded.
         */
        fun availableLevels(moduleLoaded: Boolean): List<PlacementLevel> =
            if (moduleLoaded) entries.toList()
            else entries.filter { it != MODULE }
    }
}

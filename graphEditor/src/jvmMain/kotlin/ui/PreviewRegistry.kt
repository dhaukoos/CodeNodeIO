/*
 * PreviewRegistry - Registry for dynamic composable preview dispatch
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

typealias PreviewComposable = @Composable (viewModel: Any, modifier: Modifier) -> Unit

/**
 * Registry that maps composable names to preview rendering functions.
 *
 * Preview providers register their composables at startup, and RuntimePreviewPanel
 * looks up the appropriate renderer by name at display time.
 */
object PreviewRegistry {
    private val registry = mutableMapOf<String, PreviewComposable>()

    fun register(composableName: String, preview: PreviewComposable) {
        registry[composableName] = preview
    }

    fun get(composableName: String): PreviewComposable? = registry[composableName]

    fun hasPreview(composableName: String): Boolean = composableName in registry

    fun registeredNames(): Set<String> = registry.keys.toSet()
}

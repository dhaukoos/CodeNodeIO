/*
 * PreviewRegistry - Registry for dynamic composable preview dispatch
 * License: Apache 2.0
 */

package io.codenode.previewapi

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

typealias PreviewComposable = @Composable (viewModel: Any, modifier: Modifier) -> Unit

/**
 * Registry that maps composable names to preview rendering functions.
 *
 * Preview providers in project modules register their composables at startup,
 * and the graphEditor's RuntimePreviewPanel looks up the appropriate renderer
 * by name at display time.
 *
 * This lives in preview-api (not graphEditor) so project modules can import
 * it without depending on the entire graphEditor.
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

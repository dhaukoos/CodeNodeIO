package io.codenode.edgeartfilter.processingLogic

import io.codenode.edgeartfilter.EdgeArtFilterState
import io.codenode.edgeartfilter.ImageData
import io.codenode.fbpdsl.runtime.ContinuousSinkBlock

/**
 * Sink logic for the ImageViewer node.
 *
 * Receives the final composite ImageData and updates the ViewModel state
 * for UI rendering. Computes `total_ms` from per-node timing metadata.
 * The UI composable observes EdgeArtFilterState.processedImageFlow.
 */
val imageViewerConsume: ContinuousSinkBlock<ImageData> = { imageData ->
    val meta = imageData.metadata
    val totalMs = listOf("grayscale_ms", "edgedetect_ms", "overlay_ms")
        .mapNotNull { meta[it]?.toLongOrNull() }
        .sum()

    val enriched = imageData.copy(
        metadata = meta + ("total_ms" to totalMs.toString())
    )
    EdgeArtFilterState._processedImage.value = enriched
}

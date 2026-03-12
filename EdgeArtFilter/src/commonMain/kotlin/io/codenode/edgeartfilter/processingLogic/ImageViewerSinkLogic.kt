package io.codenode.edgeartfilter.processingLogic

import io.codenode.edgeartfilter.EdgeArtFilterState
import io.codenode.edgeartfilter.ImageData
import io.codenode.fbpdsl.runtime.ContinuousSinkBlock

/**
 * Sink logic for the ImageViewer node.
 *
 * Receives the final composite ImageData and updates the ViewModel state
 * for UI rendering. The UI composable observes EdgeArtFilterState.processedImageFlow.
 */
val imageViewerConsume: ContinuousSinkBlock<ImageData> = { imageData ->
    EdgeArtFilterState._processedImage.value = imageData
}

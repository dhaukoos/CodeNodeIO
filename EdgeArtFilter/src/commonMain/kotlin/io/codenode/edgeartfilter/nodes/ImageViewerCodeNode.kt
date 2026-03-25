/*
 * ImageViewerCodeNode - Self-contained sink node for displaying processed images
 * License: Apache 2.0
 */

package io.codenode.edgeartfilter.nodes

import io.codenode.edgeartfilter.EdgeArtFilterState
import io.codenode.edgeartfilter.iptypes.ImageData
import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec

/**
 * Receives the final composite ImageData and updates the ViewModel state
 * for UI rendering. Computes `total_ms` from per-node timing metadata.
 * The UI composable observes EdgeArtFilterState.processedImageFlow.
 */
object ImageViewerCodeNode : CodeNodeDefinition {
    override val name = "ImageViewer"
    override val category = CodeNodeType.SINK
    override val description = "Displays the processed image in the UI"
    override val inputPorts = listOf(PortSpec("input1", ImageData::class))
    override val outputPorts = emptyList<PortSpec>()

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createContinuousSink<ImageData>(
            name = name,
            consume = { imageData ->
                val meta = imageData.metadata
                val totalMs = listOf("grayscale_ms", "sepia_ms", "edgedetect_ms", "overlay_ms")
                    .mapNotNull { meta[it]?.toLongOrNull() }
                    .sum()

                val enriched = imageData.copy(
                    metadata = meta + ("total_ms" to totalMs.toString())
                )
                EdgeArtFilterState._processedImage.value = enriched
            }
        )
    }
}

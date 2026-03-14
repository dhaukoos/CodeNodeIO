/*
 * ColorOverlayCodeNode - Self-contained processor node for edge-color overlay
 * License: Apache 2.0
 */

package io.codenode.edgeartfilter.nodes

import io.codenode.edgeartfilter.ImageData
import io.codenode.edgeartfilter.createImageBitmapFromPixels
import io.codenode.edgeartfilter.readPixelArray
import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.runtime.NodeCategory
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import kotlin.time.TimeSource

/**
 * Merges the original color image with the edge map to produce
 * a "neon edge" composite:
 *   - If edge brightness > threshold: use bright neon cyan color
 *   - Else: use original pixel (slightly darkened for contrast)
 *
 * Adds `overlay_ms` to metadata.
 */
object ColorOverlayCodeNode : CodeNodeDefinition {
    override val name = "ColorOverlay"
    override val category = NodeCategory.PROCESSOR
    override val description = "Overlays neon cyan edges onto the original image"
    override val inputPorts = listOf(
        PortSpec("input1", ImageData::class),
        PortSpec("input2", ImageData::class)
    )
    override val outputPorts = listOf(PortSpec("output1", ImageData::class))

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createIn2Out1Processor<ImageData, ImageData, ImageData>(
            name = name,
            process = { original, edges ->
                val mark = TimeSource.Monotonic.markNow()

                val origPixels = original.bitmap.readPixelArray()
                val edgePixels = edges.bitmap.readPixelArray()
                val width = original.width
                val height = original.height
                val output = IntArray(width * height)

                val edgeThreshold = 50

                for (i in origPixels.indices) {
                    val edgePixel = edgePixels.getOrElse(i) { 0 }
                    val edgeBrightness = (edgePixel shr 16) and 0xFF

                    if (edgeBrightness > edgeThreshold) {
                        // Neon cyan glow on edges: RGB(0, 255, 255)
                        output[i] = (0xFF shl 24) or (0x00 shl 16) or (0xFF shl 8) or 0xFF
                    } else {
                        // Slightly darken original for contrast
                        val origPixel = origPixels[i]
                        val a = (origPixel shr 24) and 0xFF
                        val r = ((origPixel shr 16) and 0xFF) * 7 / 10
                        val g = ((origPixel shr 8) and 0xFF) * 7 / 10
                        val b = (origPixel and 0xFF) * 7 / 10
                        output[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
                    }
                }

                val elapsed = mark.elapsedNow().inWholeMilliseconds
                val bitmap = createImageBitmapFromPixels(output, width, height)

                val mergedMetadata = original.metadata + edges.metadata + ("overlay_ms" to elapsed.toString())

                ImageData(
                    bitmap = bitmap,
                    width = width,
                    height = height,
                    metadata = mergedMetadata
                )
            }
        )
    }
}

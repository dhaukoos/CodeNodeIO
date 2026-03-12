package io.codenode.edgeartfilter.processingLogic

import io.codenode.edgeartfilter.ImageData
import io.codenode.edgeartfilter.createImageBitmapFromPixels
import io.codenode.edgeartfilter.readPixelArray
import io.codenode.fbpdsl.runtime.In2Out1ProcessBlock

/**
 * Process logic for the ColorOverlay node (2 inputs, 1 output).
 *
 * Merges the original color image with the edge map to produce
 * a "neon edge" composite:
 *   - If edge brightness > threshold: use bright neon cyan color
 *   - Else: use original pixel (slightly darkened for contrast)
 *
 * Adds `overlay_ms` to metadata.
 */
val colorOverlayProcess: In2Out1ProcessBlock<ImageData, ImageData, ImageData> = { original, edges ->
    val startTime = System.currentTimeMillis()

    val origPixels = original.bitmap.readPixelArray()
    val edgePixels = edges.bitmap.readPixelArray()
    val width = original.width
    val height = original.height
    val output = IntArray(width * height)

    val edgeThreshold = 50  // Edge brightness threshold

    for (i in origPixels.indices) {
        val edgePixel = edgePixels.getOrElse(i) { 0 }
        val edgeBrightness = (edgePixel shr 16) and 0xFF  // R channel of edge map

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

    val elapsed = System.currentTimeMillis() - startTime
    val bitmap = createImageBitmapFromPixels(output, width, height)

    // Merge metadata from both inputs
    val mergedMetadata = original.metadata + edges.metadata + ("overlay_ms" to elapsed.toString())

    ImageData(
        bitmap = bitmap,
        width = width,
        height = height,
        metadata = mergedMetadata
    )
}

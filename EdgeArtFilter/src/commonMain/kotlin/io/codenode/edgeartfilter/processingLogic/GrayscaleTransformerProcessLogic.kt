package io.codenode.edgeartfilter.processingLogic

import io.codenode.edgeartfilter.ImageData
import io.codenode.edgeartfilter.createImageBitmapFromPixels
import io.codenode.edgeartfilter.readPixelArray
import io.codenode.fbpdsl.runtime.ContinuousTransformBlock

/**
 * Transform logic for the GrayscaleTransformer node.
 *
 * Converts a color image to grayscale using the luminosity formula:
 *   gray = 0.299 * R + 0.587 * G + 0.114 * B
 *
 * Alpha channel is preserved. Adds `grayscale_ms` to metadata.
 */
val grayscaleTransform: ContinuousTransformBlock<ImageData, ImageData> = { input ->
    val startTime = System.currentTimeMillis()

    val pixels = input.bitmap.readPixelArray()
    val width = input.width
    val height = input.height

    for (i in pixels.indices) {
        val pixel = pixels[i]
        val a = (pixel shr 24) and 0xFF
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF

        val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt().coerceIn(0, 255)

        pixels[i] = (a shl 24) or (gray shl 16) or (gray shl 8) or gray
    }

    val elapsed = System.currentTimeMillis() - startTime
    val bitmap = createImageBitmapFromPixels(pixels, width, height)

    ImageData(
        bitmap = bitmap,
        width = width,
        height = height,
        metadata = input.metadata + ("grayscale_ms" to elapsed.toString())
    )
}

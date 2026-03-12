package io.codenode.edgeartfilter.processingLogic

import io.codenode.edgeartfilter.ImageData
import io.codenode.edgeartfilter.createImageBitmapFromPixels
import io.codenode.edgeartfilter.readPixelArray
import io.codenode.fbpdsl.runtime.ContinuousTransformBlock
import kotlin.math.min
import kotlin.time.TimeSource

/**
 * Transform logic for the SepiaTransformer node.
 *
 * Applies a sepia tone matrix to each pixel:
 *   newR = min(255, 0.393*R + 0.769*G + 0.189*B)
 *   newG = min(255, 0.349*R + 0.686*G + 0.168*B)
 *   newB = min(255, 0.272*R + 0.534*G + 0.131*B)
 *
 * Alpha channel is preserved. Adds `sepia_ms` to metadata.
 * Drop-in replacement for GrayscaleTransformer (same port signature).
 */
val sepiaTransform: ContinuousTransformBlock<ImageData, ImageData> = { input ->
    val mark = TimeSource.Monotonic.markNow()

    val pixels = input.bitmap.readPixelArray()
    val width = input.width
    val height = input.height

    for (i in pixels.indices) {
        val pixel = pixels[i]
        val a = (pixel shr 24) and 0xFF
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF

        val newR = min(255, (0.393 * r + 0.769 * g + 0.189 * b).toInt())
        val newG = min(255, (0.349 * r + 0.686 * g + 0.168 * b).toInt())
        val newB = min(255, (0.272 * r + 0.534 * g + 0.131 * b).toInt())

        pixels[i] = (a shl 24) or (newR shl 16) or (newG shl 8) or newB
    }

    val elapsed = mark.elapsedNow().inWholeMilliseconds
    val bitmap = createImageBitmapFromPixels(pixels, width, height)

    ImageData(
        bitmap = bitmap,
        width = width,
        height = height,
        metadata = input.metadata + ("sepia_ms" to elapsed.toString())
    )
}

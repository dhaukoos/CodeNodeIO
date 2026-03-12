package io.codenode.edgeartfilter.processingLogic

import io.codenode.edgeartfilter.ImageData
import io.codenode.edgeartfilter.createImageBitmapFromPixels
import io.codenode.edgeartfilter.readPixelArray
import io.codenode.fbpdsl.runtime.ContinuousTransformBlock
import kotlinx.coroutines.delay
import kotlin.math.sqrt
import kotlin.time.TimeSource

/**
 * Transform logic for the EdgeDetector node.
 *
 * Applies Sobel 3x3 edge detection convolution:
 *   Gx kernel: [[-1,0,1],[-2,0,2],[-1,0,1]]
 *   Gy kernel: [[-1,-2,-1],[0,0,0],[1,2,1]]
 *   Magnitude: sqrt(Gx^2 + Gy^2), clamped to 0-255
 *
 * Includes configurable simulated delay (default 500ms) for demo purposes.
 * Adds `edgedetect_ms` to metadata.
 */
val edgeDetectorTransform: ContinuousTransformBlock<ImageData, ImageData> = { input ->
    val mark = TimeSource.Monotonic.markNow()

    // Simulated processing delay for demo (shows async UI responsiveness)
    delay(500)

    val pixels = input.bitmap.readPixelArray()
    val width = input.width
    val height = input.height
    val output = IntArray(width * height)

    // Sobel kernels
    val gxKernel = arrayOf(
        intArrayOf(-1, 0, 1),
        intArrayOf(-2, 0, 2),
        intArrayOf(-1, 0, 1)
    )
    val gyKernel = arrayOf(
        intArrayOf(-1, -2, -1),
        intArrayOf(0, 0, 0),
        intArrayOf(1, 2, 1)
    )

    for (y in 1 until height - 1) {
        for (x in 1 until width - 1) {
            var gx = 0
            var gy = 0

            for (ky in -1..1) {
                for (kx in -1..1) {
                    val pixel = pixels[(y + ky) * width + (x + kx)]
                    // Use grayscale intensity (R channel, since input is grayscale)
                    val intensity = (pixel shr 16) and 0xFF

                    gx += intensity * gxKernel[ky + 1][kx + 1]
                    gy += intensity * gyKernel[ky + 1][kx + 1]
                }
            }

            val magnitude = sqrt((gx * gx + gy * gy).toDouble()).toInt().coerceIn(0, 255)
            output[y * width + x] = (0xFF shl 24) or (magnitude shl 16) or (magnitude shl 8) or magnitude
        }
    }

    // Border pixels remain black (0xFF000000)
    for (x in 0 until width) {
        output[x] = 0xFF.shl(24)                           // top row
        output[(height - 1) * width + x] = 0xFF.shl(24)    // bottom row
    }
    for (y in 0 until height) {
        output[y * width] = 0xFF.shl(24)                   // left column
        output[y * width + width - 1] = 0xFF.shl(24)       // right column
    }

    val elapsed = mark.elapsedNow().inWholeMilliseconds
    val bitmap = createImageBitmapFromPixels(output, width, height)

    ImageData(
        bitmap = bitmap,
        width = width,
        height = height,
        metadata = input.metadata + ("edgedetect_ms" to elapsed.toString())
    )
}

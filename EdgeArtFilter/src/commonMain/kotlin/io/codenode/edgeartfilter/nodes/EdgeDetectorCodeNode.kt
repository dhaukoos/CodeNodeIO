/*
 * EdgeDetectorCodeNode - Self-contained transformer node for Sobel edge detection
 * License: Apache 2.0
 */

package io.codenode.edgeartfilter.nodes

import io.codenode.edgeartfilter.iptypes.ImageData
import io.codenode.edgeartfilter.createImageBitmapFromPixels
import io.codenode.edgeartfilter.readPixelArray
import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import kotlinx.coroutines.delay
import kotlin.math.sqrt
import kotlin.time.TimeSource

/**
 * Applies Sobel 3x3 edge detection convolution:
 *   Gx kernel: [[-1,0,1],[-2,0,2],[-1,0,1]]
 *   Gy kernel: [[-1,-2,-1],[0,0,0],[1,2,1]]
 *   Magnitude: sqrt(Gx^2 + Gy^2), clamped to 0-255
 *
 * Includes 500ms simulated delay for demo purposes.
 * Adds `edgedetect_ms` to metadata.
 */
object EdgeDetectorCodeNode : CodeNodeDefinition {
    override val name = "EdgeDetector"
    override val category = CodeNodeType.TRANSFORMER
    override val description = "Detects edges using Sobel 3x3 convolution"
    override val inputPorts = listOf(PortSpec("input1", ImageData::class))
    override val outputPorts = listOf(PortSpec("output1", ImageData::class))

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createContinuousTransformer<ImageData, ImageData>(
            name = name,
            transform = { input ->
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
                                val intensity = (pixel shr 16) and 0xFF

                                gx += intensity * gxKernel[ky + 1][kx + 1]
                                gy += intensity * gyKernel[ky + 1][kx + 1]
                            }
                        }

                        val magnitude = sqrt((gx * gx + gy * gy).toDouble()).toInt().coerceIn(0, 255)
                        output[y * width + x] = (0xFF shl 24) or (magnitude shl 16) or (magnitude shl 8) or magnitude
                    }
                }

                // Border pixels remain black
                for (x in 0 until width) {
                    output[x] = 0xFF.shl(24)
                    output[(height - 1) * width + x] = 0xFF.shl(24)
                }
                for (y in 0 until height) {
                    output[y * width] = 0xFF.shl(24)
                    output[y * width + width - 1] = 0xFF.shl(24)
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
        )
    }
}

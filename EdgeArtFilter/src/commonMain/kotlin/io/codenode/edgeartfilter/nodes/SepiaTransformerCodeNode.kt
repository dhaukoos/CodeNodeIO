/*
 * SepiaTransformerCodeNode - Self-contained transformer node for sepia tone
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
import kotlin.math.min
import kotlin.time.TimeSource

/**
 * Applies a sepia tone matrix to each pixel:
 *   newR = min(255, 0.393*R + 0.769*G + 0.189*B)
 *   newG = min(255, 0.349*R + 0.686*G + 0.168*B)
 *   newB = min(255, 0.272*R + 0.534*G + 0.131*B)
 *
 * Alpha channel is preserved. Adds `sepia_ms` to metadata.
 * Drop-in replacement for GrayscaleTransformer (same port signature).
 */
object SepiaTransformerCodeNode : CodeNodeDefinition {
    override val name = "SepiaTransformer"
    override val category = CodeNodeType.TRANSFORMER
    override val description = "Applies sepia tone color matrix to image"
    override val inputPorts = listOf(PortSpec("input1", ImageData::class))
    override val outputPorts = listOf(PortSpec("output1", ImageData::class))

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createContinuousTransformer<ImageData, ImageData>(
            name = name,
            transform = { input ->
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
        )
    }
}

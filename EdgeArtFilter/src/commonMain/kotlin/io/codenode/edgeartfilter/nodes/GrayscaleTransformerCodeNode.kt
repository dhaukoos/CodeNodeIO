/*
 * GrayscaleTransformerCodeNode - Self-contained transformer node for grayscale conversion
 * License: Apache 2.0
 */

package io.codenode.edgeartfilter.nodes

import io.codenode.edgeartfilter.ImageData
import io.codenode.edgeartfilter.createImageBitmapFromPixels
import io.codenode.edgeartfilter.readPixelArray
import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import kotlin.time.TimeSource

/**
 * Converts a color image to grayscale using the luminosity formula:
 *   gray = 0.299 * R + 0.587 * G + 0.114 * B
 *
 * Alpha channel is preserved. Adds `grayscale_ms` to metadata.
 */
object GrayscaleTransformerCodeNode : CodeNodeDefinition {
    override val name = "GrayscaleTransformer"
    override val category = CodeNodeType.TRANSFORMER
    override val description = "Converts color image to grayscale using luminosity formula"
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

                    val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt().coerceIn(0, 255)

                    pixels[i] = (a shl 24) or (gray shl 16) or (gray shl 8) or gray
                }

                val elapsed = mark.elapsedNow().inWholeMilliseconds
                val bitmap = createImageBitmapFromPixels(pixels, width, height)

                ImageData(
                    bitmap = bitmap,
                    width = width,
                    height = height,
                    metadata = input.metadata + ("grayscale_ms" to elapsed.toString())
                )
            }
        )
    }
}

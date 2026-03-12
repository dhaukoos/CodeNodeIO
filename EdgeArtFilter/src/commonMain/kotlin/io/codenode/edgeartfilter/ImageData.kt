package io.codenode.edgeartfilter

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Primary data packet flowing through the image processing pipeline.
 *
 * Carries image pixel data along with processing metadata that accumulates
 * as the packet flows through nodes (per-node timing, source info, etc.).
 *
 * @param bitmap The image pixel data
 * @param width Image width in pixels
 * @param height Image height in pixels
 * @param metadata Processing metadata (node timings, source info)
 */
data class ImageData(
    val bitmap: ImageBitmap,
    val width: Int,
    val height: Int,
    val metadata: Map<String, String> = emptyMap()
)

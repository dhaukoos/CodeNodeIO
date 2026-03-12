package io.codenode.edgeartfilter

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Creates an ImageBitmap from an ARGB pixel array.
 *
 * Platform-specific implementation:
 * - JVM: Uses BufferedImage.setRGB() + toComposeImageBitmap()
 */
expect fun createImageBitmapFromPixels(pixels: IntArray, width: Int, height: Int): ImageBitmap

/**
 * Reads all pixels from an ImageBitmap into an ARGB IntArray.
 *
 * Each pixel is packed as: (alpha << 24) | (red << 16) | (green << 8) | blue
 */
fun ImageBitmap.readPixelArray(): IntArray {
    val pixels = IntArray(width * height)
    readPixels(pixels)
    return pixels
}

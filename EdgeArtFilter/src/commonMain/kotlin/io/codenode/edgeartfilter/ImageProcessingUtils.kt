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
 * Opens a platform file chooser filtered to PNG images and loads the selected file
 * as an ImageData packet. Returns null if the user cancels.
 */
expect fun pickImageFile(): ImageData?

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

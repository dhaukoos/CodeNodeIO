package io.codenode.edgeartfilter

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.image.BufferedImage

actual fun createImageBitmapFromPixels(pixels: IntArray, width: Int, height: Int): ImageBitmap {
    val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    bufferedImage.setRGB(0, 0, width, height, pixels, 0, width)
    return bufferedImage.toComposeImageBitmap()
}

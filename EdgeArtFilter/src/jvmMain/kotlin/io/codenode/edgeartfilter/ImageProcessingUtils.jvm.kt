package io.codenode.edgeartfilter

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.codenode.edgeartfilter.iptypes.ImageData
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

actual fun createImageBitmapFromPixels(pixels: IntArray, width: Int, height: Int): ImageBitmap {
    val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    bufferedImage.setRGB(0, 0, width, height, pixels, 0, width)
    return bufferedImage.toComposeImageBitmap()
}

actual fun pickImageFile(): ImageData? {
    val chooser = JFileChooser().apply {
        dialogTitle = "Select Image"
        fileFilter = FileNameExtensionFilter("PNG Images", "png")
        isAcceptAllFileFilterUsed = false
    }
    val result = chooser.showOpenDialog(null)
    if (result != JFileChooser.APPROVE_OPTION) return null

    val file = chooser.selectedFile
    val bufferedImage = ImageIO.read(file) ?: return null
    val bitmap = bufferedImage.toComposeImageBitmap()

    return ImageData(
        bitmap = bitmap,
        width = bufferedImage.width,
        height = bufferedImage.height,
        metadata = mapOf("source" to file.absolutePath)
    )
}

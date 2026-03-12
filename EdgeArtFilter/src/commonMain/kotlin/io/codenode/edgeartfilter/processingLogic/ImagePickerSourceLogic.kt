package io.codenode.edgeartfilter.processingLogic

import io.codenode.edgeartfilter.EdgeArtFilterState
import io.codenode.edgeartfilter.ImageData
import io.codenode.fbpdsl.runtime.ProcessResult2
import io.codenode.fbpdsl.runtime.SourceOut2Block
import kotlinx.coroutines.flow.drop

/**
 * Source logic for the ImagePicker node.
 *
 * Emits the same ImageData to two output channels (fan-out):
 *   - outputChannel1 → GrayscaleTransformer (for edge detection path)
 *   - outputChannel2 → ColorOverlay (original image for merging)
 *
 * Trigger: UI sets EdgeArtFilterState._selectedImage via file chooser.
 * If user cancels dialog: no emission, no error.
 */
val imagePickerGenerate: SourceOut2Block<ImageData, ImageData> = { emit ->
    EdgeArtFilterState._selectedImage
        .drop(1) // Skip initial null value
        .collect { imageData ->
            if (imageData != null) {
                emit(ProcessResult2.both(imageData, imageData))
            }
        }
}

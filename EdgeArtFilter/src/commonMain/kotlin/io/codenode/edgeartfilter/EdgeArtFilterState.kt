package io.codenode.edgeartfilter

import io.codenode.edgeartfilter.iptypes.ImageData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared observable state for the EdgeArtFilter module.
 *
 * Source nodes write to this state (e.g., selected image),
 * sink nodes read from processing results, and the ViewModel
 * exposes these flows to the UI.
 */
object EdgeArtFilterState {

    /** Image selected by the user via ImagePicker (source input) */
    internal val _selectedImage = MutableStateFlow<ImageData?>(null)
    val selectedImageFlow: StateFlow<ImageData?> = _selectedImage.asStateFlow()

    /** Processed composite image from the pipeline (sink output) */
    internal val _processedImage = MutableStateFlow<ImageData?>(null)
    val processedImageFlow: StateFlow<ImageData?> = _processedImage.asStateFlow()

    fun reset() {
        _selectedImage.value = null
        _processedImage.value = null
    }
}

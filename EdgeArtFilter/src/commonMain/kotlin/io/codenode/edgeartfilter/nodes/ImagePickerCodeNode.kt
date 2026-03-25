/*
 * ImagePickerCodeNode - Self-contained source node for image selection
 * License: Apache 2.0
 */

package io.codenode.edgeartfilter.nodes

import io.codenode.edgeartfilter.EdgeArtFilterState
import io.codenode.edgeartfilter.iptypes.ImageData
import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.fbpdsl.runtime.ProcessResult2
import kotlinx.coroutines.flow.drop

/**
 * Source node that emits the user-selected image to two outputs (fan-out):
 *   - output1 → edge detection path (e.g., GrayscaleTransformer)
 *   - output2 → original image path (e.g., ColorOverlay)
 *
 * Trigger: UI sets EdgeArtFilterState._selectedImage via file chooser.
 */
object ImagePickerCodeNode : CodeNodeDefinition {
    override val name = "ImagePicker"
    override val category = CodeNodeType.SOURCE
    override val description = "Selects an image file and emits it to two output channels"
    override val inputPorts = emptyList<PortSpec>()
    override val outputPorts = listOf(
        PortSpec("output1", ImageData::class),
        PortSpec("output2", ImageData::class)
    )

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createSourceOut2<ImageData, ImageData>(
            name = name,
            generate = { emit ->
                EdgeArtFilterState._selectedImage
                    .drop(1) // Skip initial null value
                    .collect { imageData ->
                        if (imageData != null) {
                            emit(ProcessResult2.both(imageData, imageData))
                        }
                    }
            }
        )
    }
}

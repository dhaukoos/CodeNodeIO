package io.codenode.edgeartfilter.userInterface

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.codenode.edgeartfilter.EdgeArtFilterViewModel
import io.codenode.edgeartfilter.pickImageFile
import io.codenode.fbpdsl.model.ExecutionState

/**
 * Main composable for the EdgeArtFilter module.
 *
 * Displays the processed image result and provides pipeline controls.
 * The file picker trigger is handled by the ViewModel's selectImage() method,
 * invoked from the platform-specific host (graphEditor RuntimePreview panel).
 */
@Composable
fun EdgeArtFilter(
    viewModel: EdgeArtFilterViewModel,
    modifier: Modifier = Modifier
) {
    val processedImage by viewModel.processedImage.collectAsState()
    val executionState by viewModel.executionState.collectAsState()
    val isRunning = executionState == ExecutionState.RUNNING

    Column(
        modifier = modifier.fillMaxSize().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pipeline status
        Text(
            text = when (executionState) {
                ExecutionState.RUNNING -> "Pipeline Running"
                ExecutionState.PAUSED -> "Pipeline Paused"
                else -> "Pipeline Idle"
            },
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Pick image button
        Button(
            onClick = {
                val imageData = pickImageFile()
                if (imageData != null) {
                    viewModel.selectImage(
                        bitmap = imageData.bitmap,
                        width = imageData.width,
                        height = imageData.height,
                        sourcePath = imageData.metadata["source"] ?: ""
                    )
                }
            },
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text("Pick Image")
        }

        // Processed image display
        val imageData = processedImage
        if (imageData != null) {
            Image(
                bitmap = imageData.bitmap,
                contentDescription = "Processed image",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(4.dp),
                contentScale = ContentScale.Fit
            )

            // Image dimensions
            Text(
                text = "${imageData.width} × ${imageData.height}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            // Empty state
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Select an image to start the pipeline",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

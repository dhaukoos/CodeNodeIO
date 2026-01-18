/*
 * GraphEditor Main Entry Point
 * Compose Desktop visual graph editor for Flow-Based Programming
 * License: Apache 2.0
 */

package io.codenode.grapheditor

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Main composable for the GraphEditor visual canvas
 * Displays a grid-based canvas for creating and editing flow graphs
 */
@Composable
fun GraphEditorCanvas(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CodeNodeIO Graph Editor",
                style = MaterialTheme.typography.h4,
                color = MaterialTheme.colors.onBackground
            )
            Text(
                text = "Visual Flow-Based Programming Editor",
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * Preview of the GraphEditor canvas
 * Used for testing Composable function compilation
 */
@Preview
@Composable
fun GraphEditorCanvasPreview() {
    MaterialTheme {
        GraphEditorCanvas()
    }
}


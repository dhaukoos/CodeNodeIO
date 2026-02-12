/*
 * Main composable entry point for KMPMobileApp
 * Shared UI code for Android and iOS
 * Uses the StopWatch.flow virtual circuit via generated StopWatchController
 */
package io.codenode.mobileapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.codenode.stopwatch.generated.StopWatchController
import io.codenode.stopwatch.stopWatchFlowGraph

/**
 * Main application composable.
 * This is the entry point for the shared UI.
 */
@Composable
fun App() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MainContent()
        }
    }
}

/**
 * Main content composable displaying a greeting and stopwatch.
 * Creates the StopWatchController from the StopWatch module's FlowGraph.
 */
@Composable
fun MainContent() {
    // Create Controller from StopWatch module's flow graph - remember to survive recomposition
    val controller = remember { StopWatchController(stopWatchFlowGraph) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "CodeNodeIO Mobile",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = greet(),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp)
        )
        StopWatch(
            controller = controller,
            modifier = Modifier.padding(top = 32.dp),
            minSize = 400.dp
        )
    }
}

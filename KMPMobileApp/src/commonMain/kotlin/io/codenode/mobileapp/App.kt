/*
 * Main composable entry point for KMPMobileApp
 * Shared UI code for Android and iOS
 * Uses ViewModel pattern to bridge FlowGraph with Compose UI
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
import io.codenode.stopwatchv2.generated.StopWatchV2ControllerAdapter
import io.codenode.stopwatchv2.generated.StopWatchV2ViewModel
import io.codenode.stopwatchv2.generated.StopWatchV2Controller
import io.codenode.stopwatchv2.stopWatchV2FlowGraph
import io.codenode.stopwatchv2.userInterface.StopWatchV2Screen

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
 * Creates the StopWatchViewModel from the StopWatch module's FlowGraph.
 */
@Composable
fun MainContent() {
    // Create Controller from StopWatchV2 module's flow graph
    val controller = remember { StopWatchV2Controller(stopWatchV2FlowGraph) }

    // Create ViewModel wrapping the controller via adapter
    val viewModel = remember { StopWatchV2ViewModel(StopWatchV2ControllerAdapter(controller)) }

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
        StopWatchV2Screen(
            viewModel = viewModel,
            modifier = Modifier.padding(top = 32.dp),
            minSize = 400.dp
        )
    }
}

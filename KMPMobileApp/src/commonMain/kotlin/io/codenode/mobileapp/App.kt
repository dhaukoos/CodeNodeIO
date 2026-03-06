/*
 * Main composable entry point for KMPMobileApp
 * Shared UI code for Android and iOS
 * Uses ViewModel pattern to bridge FlowGraph with Compose UI
 */
package io.codenode.mobileapp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.codenode.stopwatch.generated.StopWatchControllerAdapter
import io.codenode.stopwatch.StopWatchViewModel
import io.codenode.stopwatch.generated.StopWatchController
import io.codenode.stopwatch.stopWatchFlowGraph
import io.codenode.stopwatch.userInterface.StopWatchScreen
import io.codenode.userprofiles.UserProfilesViewModel
import io.codenode.userprofiles.generated.UserProfilesController
import io.codenode.userprofiles.generated.UserProfilesControllerAdapter
import io.codenode.userprofiles.userProfilesFlowGraph
import io.codenode.userprofiles.userInterface.UserProfiles

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
 * Main content composable with bottom navigation for StopWatch and UserProfiles tabs.
 */
@Composable
fun MainContent() {
    // StopWatch setup
    val stopWatchController = remember {
        StopWatchController(stopWatchFlowGraph).also {
            it.setAttenuationDelay(1000)
        }
    }
    val stopWatchViewModel = remember { StopWatchViewModel(StopWatchControllerAdapter(stopWatchController)) }

    // UserProfiles setup
    val userProfilesController = remember {
        UserProfilesController(userProfilesFlowGraph).also {
            it.start()
        }
    }
    val userProfilesViewModel = remember { UserProfilesViewModel(UserProfilesControllerAdapter(userProfilesController)) }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("StopWatch", "UserProfiles")

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(title) },
                        icon = {}
                    )
                }
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            0 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    StopWatchScreen(
                        viewModel = stopWatchViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            1 -> {
                UserProfiles(
                    viewModel = userProfilesViewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
}

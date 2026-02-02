/*
 * iOS MainViewController - Entry point for the iOS app
 */
package io.codenode.mobileapp

import androidx.compose.ui.window.ComposeUIViewController

/**
 * Creates the main UIViewController for iOS.
 * This is called from Swift/Objective-C code to get the Compose UI.
 */
fun MainViewController() = ComposeUIViewController { App() }

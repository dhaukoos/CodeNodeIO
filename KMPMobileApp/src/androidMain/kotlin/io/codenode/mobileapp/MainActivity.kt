/*
 * Android MainActivity - Entry point for the Android app
 */
package io.codenode.mobileapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.codenode.persistence.initializeDatabaseContext

/**
 * Main Activity for the Android application.
 * Sets up the Compose UI with the shared App composable.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        initializeDatabaseContext(application)
        setContent {
            App()
        }
    }
}

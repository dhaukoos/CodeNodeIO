/*
 * GraphEditor Main Entry Point
 * Compose Desktop visual graph editor for Flow-Based Programming
 * License: Apache 2.0
 */

package io.codenode.grapheditor

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.codenode.grapheditor.ui.GraphEditorApp
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.io.File

/**
 * Main entry point for the standalone Graph Editor application
 */
fun main(args: Array<String> = emptyArray()) {
    // Resolve project directory from (in priority order):
    // 1. --project <path> command-line argument
    // 2. CODENODE_PROJECT_DIR environment variable
    // 3. Current working directory (legacy behavior)
    val projectDirArg = args.indexOf("--project").let { idx ->
        if (idx >= 0 && idx + 1 < args.size) args[idx + 1] else null
    }
    val projectDirEnv = System.getenv("CODENODE_PROJECT_DIR")
    val resolvedProjectDir = (projectDirArg ?: projectDirEnv)?.let { File(it).absoluteFile }

    // Store the resolved project directory for downstream use.
    // Do NOT set user.dir — it breaks FileDialog directory navigation.
    if (resolvedProjectDir != null) {
        System.setProperty("codenode.project.dir", resolvedProjectDir.absolutePath)
    }

    // Koin DI is initialized with an empty module set.
    // Project modules register their Koin modules at runtime when loaded.
    startKoin {
        modules(module { })
    }

    application {
    val windowState = rememberWindowState(
        width = 1400.dp,
        height = 900.dp
    )

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "CodeNodeIO Graph Editor - Visual Flow-Based Programming"
    ) {
        GraphEditorApp()
    }
    }
}

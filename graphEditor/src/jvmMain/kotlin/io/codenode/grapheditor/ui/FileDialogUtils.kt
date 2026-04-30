/*
 * FileDialogUtils - Native file dialog utilities for Graph Editor
 * Provides file open and directory chooser dialogs
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/**
 * Result of a file open dialog operation.
 */
data class FileOpenResult(val file: File? = null, val error: String? = null)

/**
 * Show file open dialog. Defaults to filtering for `.flow.kt` files; callers can
 * override the filter (e.g., the UI-FBP UI-file picker accepts plain `.kt`).
 *
 * Uses java.awt.FileDialog for native macOS file picker.
 *
 * @param initialDir Directory to open in
 * @param title Dialog title (default: "Open Flow Graph")
 * @param filenameFilter Predicate that accepts (dir, name) pairs. Default keeps
 *        only `.flow.kt` files.
 */
fun showFileOpenDialog(
    initialDir: File? = null,
    title: String = "Open Flow Graph",
    filenameFilter: (File, String) -> Boolean = { _, name -> name.endsWith(".flow.kt") }
): FileOpenResult {
    val startDir = initialDir
        ?: System.getProperty("codenode.project.dir")?.let { File(it) }
        ?: File(System.getProperty("user.dir"))

    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
    dialog.directory = startDir.absolutePath
    dialog.setFilenameFilter { dir, name -> filenameFilter(dir, name) }
    dialog.isVisible = true

    val dir = dialog.directory ?: return FileOpenResult()
    val fileName = dialog.file ?: return FileOpenResult()
    return FileOpenResult(file = File(dir, fileName))
}

/**
 * Show directory chooser for module compilation output
 *
 * @param title Dialog title (default: "Select Output Directory for KMP Module")
 */
fun showDirectoryChooser(title: String = "Select Output Directory for KMP Module"): File? {
    val startDir = System.getProperty("codenode.project.dir")?.let { File(it) }
        ?: File(System.getProperty("user.dir"))

    // Enable directory selection in the native macOS dialog
    val prevDirProp = System.getProperty("apple.awt.fileDialogForDirectories")
    System.setProperty("apple.awt.fileDialogForDirectories", "true")

    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
    dialog.directory = startDir.absolutePath
    dialog.isVisible = true

    // Restore previous setting
    if (prevDirProp != null) {
        System.setProperty("apple.awt.fileDialogForDirectories", prevDirProp)
    } else {
        System.clearProperty("apple.awt.fileDialogForDirectories")
    }

    val dir = dialog.directory ?: return null
    val fileName = dialog.file ?: return null
    return File(dir, fileName)
}

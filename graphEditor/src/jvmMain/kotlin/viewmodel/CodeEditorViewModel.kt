/*
 * CodeEditorViewModel - State Management for Code Editor
 * Manages file loading, saving, dirty tracking, and editor state
 * License: Apache 2.0
 */

package io.codenode.grapheditor.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Represents a selectable file entry in the file selector dropdown.
 *
 * @property displayName Label shown in the dropdown
 * @property filePath Absolute path to the file on disk
 * @property isFlowGraph True if this is the flowGraph DSL file (read-only)
 * @property associatedNodeId Node ID in the flowGraph this file belongs to (null for flowGraph file)
 */
data class FileEntry(
    val displayName: String,
    val filePath: File,
    val isFlowGraph: Boolean,
    val associatedNodeId: String? = null
)

/**
 * State for the code editor.
 *
 * @property currentFile The file currently loaded (null = no file open)
 * @property originalContent Content as loaded from disk
 * @property editedContent Current content in the editor
 * @property isReadOnly True for flowGraph DSL files, false for CodeNode source files
 * @property errorMessage Error message if file loading failed
 */
data class CodeEditorState(
    val currentFile: File? = null,
    val originalContent: String = "",
    val editedContent: String = "",
    val isReadOnly: Boolean = false,
    val errorMessage: String? = null
) : BaseState {
    val isDirty: Boolean
        get() = editedContent != originalContent

    val hasFile: Boolean
        get() = currentFile != null && errorMessage == null
}

/**
 * ViewModel for the code editor panel.
 * Manages file I/O, dirty tracking, and editor state transitions.
 */
class CodeEditorViewModel {

    private val _state = MutableStateFlow(CodeEditorState())
    val state: StateFlow<CodeEditorState> = _state.asStateFlow()

    /**
     * Loads a file into the editor.
     *
     * @param file The file to load
     * @param readOnly Whether the file should be opened in read-only mode
     */
    fun loadFile(file: File, readOnly: Boolean = false) {
        if (!file.exists()) {
            _state.value = CodeEditorState(
                currentFile = file,
                isReadOnly = readOnly,
                errorMessage = "Source file not found: ${file.absolutePath}"
            )
            return
        }

        try {
            val content = file.readText()
            _state.value = CodeEditorState(
                currentFile = file,
                originalContent = content,
                editedContent = content,
                isReadOnly = readOnly
            )
        } catch (e: Exception) {
            _state.value = CodeEditorState(
                currentFile = file,
                isReadOnly = readOnly,
                errorMessage = "Failed to read file: ${e.message}"
            )
        }
    }

    /**
     * Updates the edited content without saving to disk.
     */
    fun updateContent(newContent: String) {
        _state.value = _state.value.copy(editedContent = newContent)
    }

    /**
     * Saves the current edited content to disk.
     *
     * @return true if save succeeded, false otherwise
     */
    fun save(): Boolean {
        val currentState = _state.value
        val file = currentState.currentFile ?: return false

        if (currentState.isReadOnly) return false

        return try {
            file.writeText(currentState.editedContent)
            _state.value = currentState.copy(
                originalContent = currentState.editedContent,
                errorMessage = null
            )
            true
        } catch (e: Exception) {
            _state.value = currentState.copy(
                errorMessage = "Failed to save file: ${e.message}"
            )
            false
        }
    }

    /**
     * Discards unsaved changes, reverting to the last saved content.
     */
    fun discardChanges() {
        val currentState = _state.value
        _state.value = currentState.copy(
            editedContent = currentState.originalContent
        )
    }

    /**
     * Clears the editor state (no file loaded).
     */
    fun clear() {
        _state.value = CodeEditorState()
    }
}

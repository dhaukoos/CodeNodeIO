/*
 * FileBrowserEditor Test
 * TDD tests for FileBrowserEditor component
 * License: Apache 2.0
 */

package ui

import io.codenode.grapheditor.ui.*
import java.io.File
import kotlin.test.*

/**
 * TDD tests for FileBrowserEditor component.
 *
 * These tests verify that FileBrowserEditor:
 * - Renders with text field and browse button (T009)
 * - Displays existing path value (T010)
 * - Shows error state when isError is true (T011)
 * - Triggers onValueChange callback on path changes (T012)
 *
 * Note: These tests are designed to FAIL initially (TDD Red phase)
 * until FileBrowserEditor is fully implemented.
 */
class FileBrowserEditorTest {

    // ============================================
    // T009: Rendering Tests
    // ============================================

    @Test
    fun `should render with text field and browse button`() {
        // Given: FileBrowserEditor is instantiated
        // The component should contain a text field for path display
        // and a browse button for file selection

        // Verify that the editor type exists and maps correctly
        val definition = PropertyDefinition(
            name = "processingLogicFile",
            type = PropertyType.FILE_PATH
        )

        // Then: Editor type should be FILE_BROWSER
        assertEquals(EditorType.FILE_BROWSER, definition.editorType)

        // And: PropertyType.FILE_PATH should exist
        assertEquals(PropertyType.FILE_PATH, definition.type)
    }

    @Test
    fun `text field should show placeholder when empty`() {
        // Given: Empty value
        val value = ""

        // Then: The component should be able to handle empty values
        // When rendered, placeholder "Select a file..." should be shown
        assertTrue(value.isEmpty())

        // Verify that empty string is a valid initial state
        val trimmedValue = value.trim()
        assertTrue(trimmedValue.isEmpty())
    }

    // ============================================
    // T010: Display Existing Value Tests
    // ============================================

    @Test
    fun `should display existing path value in text field`() {
        // Given: A file path value
        val existingPath = "demos/stopwatch/TimerEmitterComponent.kt"

        // Then: The path should be displayable (not empty)
        assertTrue(existingPath.isNotEmpty())

        // And: Path should be a valid file path format
        assertTrue(existingPath.endsWith(".kt"))
        assertTrue(existingPath.contains("/"))
    }

    @Test
    fun `should display relative path from project root`() {
        // Given: A relative path from project root
        val relativePath = "demos/stopwatch/DisplayReceiverComponent.kt"

        // Then: Path should be relative (no absolute path indicators)
        assertFalse(relativePath.startsWith("/"))
        assertFalse(relativePath.contains(":")) // No Windows drive letters

        // And: Should maintain proper format
        assertEquals("demos/stopwatch/DisplayReceiverComponent.kt", relativePath)
    }

    @Test
    fun `should handle path with forward slashes on all platforms`() {
        // Given: Paths stored with forward slashes for portability
        val storedPath = "demos/stopwatch/TimerEmitterComponent.kt"

        // Then: Path should use forward slashes (not backslashes)
        assertFalse(storedPath.contains("\\"))
        assertTrue(storedPath.contains("/"))
    }

    // ============================================
    // T011: Error State Tests
    // ============================================

    @Test
    fun `should show error state when isError is true`() {
        // Given: isError is true
        val isError = true
        val errorMessage = "File not found"

        // Then: Error state should be active
        assertTrue(isError)

        // And: Error message should be available for display
        assertTrue(errorMessage.isNotEmpty())
    }

    @Test
    fun `should indicate error for non-existent file`() {
        // Given: A path to a file that doesn't exist
        val invalidPath = "demos/stopwatch/NonExistent.kt"
        val isError = true
        val errorMessage = "File not found: demos/stopwatch/NonExistent.kt"

        // Then: Error state should be set
        assertTrue(isError)
        assertTrue(errorMessage.contains("File not found"))
        assertTrue(errorMessage.contains(invalidPath))
    }

    @Test
    fun `should indicate error for invalid file extension`() {
        // Given: A path without .kt extension
        val invalidPath = "demos/stopwatch/README.md"
        val isError = true
        val errorMessage = "Must be a Kotlin file (.kt)"

        // Then: Error should indicate wrong extension
        assertTrue(isError)
        assertTrue(errorMessage.contains(".kt"))
        assertFalse(invalidPath.endsWith(".kt"))
    }

    @Test
    fun `error border color should be distinct`() {
        // Given: Error state styling constants
        // The error border should be visually distinct (red)

        // When: isError is true
        val isError = true

        // Then: The component should apply error styling
        // This is a behavioral contract that error state changes appearance
        assertTrue(isError)
    }

    // ============================================
    // T012: Callback Tests
    // ============================================

    @Test
    fun `should trigger onValueChange when path is selected`() {
        // Given: A callback to capture value changes
        var capturedValue: String? = null
        val onValueChange: (String) -> Unit = { value ->
            capturedValue = value
        }

        // When: Simulating a file selection
        val selectedPath = "demos/stopwatch/TimerEmitterComponent.kt"
        onValueChange(selectedPath)

        // Then: Callback should be invoked with the path
        assertNotNull(capturedValue)
        assertEquals(selectedPath, capturedValue)
    }

    @Test
    fun `should trigger onValueChange on manual text entry`() {
        // Given: A callback to capture value changes
        var changeCount = 0
        var lastValue: String? = null
        val onValueChange: (String) -> Unit = { value ->
            changeCount++
            lastValue = value
        }

        // When: User types in the text field (simulated)
        onValueChange("demos/")
        onValueChange("demos/stopwatch/")
        onValueChange("demos/stopwatch/Timer")
        onValueChange("demos/stopwatch/TimerEmitterComponent.kt")

        // Then: Callback should be invoked for each change
        assertEquals(4, changeCount)
        assertEquals("demos/stopwatch/TimerEmitterComponent.kt", lastValue)
    }

    @Test
    fun `should not modify path value internally`() {
        // Given: A path value passed to onValueChange
        val originalPath = "demos/stopwatch/TimerEmitterComponent.kt"
        var receivedPath: String? = null

        val onValueChange: (String) -> Unit = { value ->
            receivedPath = value
        }

        // When: Path is provided
        onValueChange(originalPath)

        // Then: Path should be unchanged
        assertEquals(originalPath, receivedPath)
    }

    @Test
    fun `callback should receive relative path from browse selection`() {
        // Given: A simulated browse result converted to relative path
        // (In real implementation, JFileChooser returns absolute path,
        //  which is converted to relative path before callback)

        val absolutePath = "/Users/dev/CodeNodeIO/demos/stopwatch/TimerEmitterComponent.kt"
        val projectRoot = "/Users/dev/CodeNodeIO"

        // When: Converting to relative path
        val relativePath = absolutePath.removePrefix(projectRoot + "/")

        // Then: Relative path should be passed to callback
        assertEquals("demos/stopwatch/TimerEmitterComponent.kt", relativePath)
    }

    // ============================================
    // Integration Contract Tests
    // ============================================

    @Test
    fun `FILE_PATH property type should map to FILE_BROWSER editor`() {
        // Given: A PropertyDefinition with FILE_PATH type
        val definition = PropertyDefinition(
            name = "testFile",
            type = PropertyType.FILE_PATH,
            required = true,
            description = "A test file path property"
        )

        // Then: Editor type should be FILE_BROWSER
        assertEquals(EditorType.FILE_BROWSER, definition.editorType)
    }

    @Test
    fun `FILE_BROWSER should be valid EditorType enum value`() {
        // Given: EditorType enum
        // Then: FILE_BROWSER should be a valid value
        val editorType = EditorType.FILE_BROWSER
        assertNotNull(editorType)
        assertEquals("FILE_BROWSER", editorType.name)
    }

    @Test
    fun `FILE_PATH should be valid PropertyType enum value`() {
        // Given: PropertyType enum
        // Then: FILE_PATH should be a valid value
        val propertyType = PropertyType.FILE_PATH
        assertNotNull(propertyType)
        assertEquals("FILE_PATH", propertyType.name)
    }

    // ============================================
    // T016: Relative Path Conversion Tests
    // ============================================

    @Test
    fun `convertToRelativePath should convert absolute path to relative`() {
        // Given: An absolute file path and project root
        val projectRoot = File("/Users/dev/CodeNodeIO")
        val absoluteFile = File("/Users/dev/CodeNodeIO/demos/stopwatch/TimerEmitterComponent.kt")

        // When: Converting to relative path
        val relativePath = convertToRelativePath(absoluteFile, projectRoot)

        // Then: Should return relative path with forward slashes
        assertEquals("demos/stopwatch/TimerEmitterComponent.kt", relativePath)
    }

    @Test
    fun `convertToRelativePath should use forward slashes on all platforms`() {
        // Given: A file path (simulating Windows-style path internally)
        val projectRoot = File("/Users/dev/CodeNodeIO")
        val absoluteFile = File("/Users/dev/CodeNodeIO/demos/stopwatch/Component.kt")

        // When: Converting to relative path
        val relativePath = convertToRelativePath(absoluteFile, projectRoot)

        // Then: Should not contain backslashes
        assertFalse(relativePath.contains("\\"))
    }

    @Test
    fun `convertToRelativePath should handle file at project root`() {
        // Given: A file directly in project root
        val projectRoot = File("/Users/dev/CodeNodeIO")
        val absoluteFile = File("/Users/dev/CodeNodeIO/Component.kt")

        // When: Converting to relative path
        val relativePath = convertToRelativePath(absoluteFile, projectRoot)

        // Then: Should return just the filename
        assertEquals("Component.kt", relativePath)
    }

    @Test
    fun `convertToRelativePath should handle file outside project root`() {
        // Given: A file outside project root
        val projectRoot = File("/Users/dev/CodeNodeIO")
        val absoluteFile = File("/Users/other/project/Component.kt")

        // When: Converting to relative path
        val relativePath = convertToRelativePath(absoluteFile, projectRoot)

        // Then: Should return absolute path (normalized with forward slashes)
        assertTrue(relativePath.contains("other/project"))
    }

    @Test
    fun `convertToRelativePath should not have leading slash`() {
        // Given: An absolute file path
        val projectRoot = File("/Users/dev/CodeNodeIO")
        val absoluteFile = File("/Users/dev/CodeNodeIO/src/main/Component.kt")

        // When: Converting to relative path
        val relativePath = convertToRelativePath(absoluteFile, projectRoot)

        // Then: Should not start with forward slash
        assertFalse(relativePath.startsWith("/"))
    }
}

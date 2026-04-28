/*
 * GenerationFileTreeTest - Tests for file tree model and checkbox toggling
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class GenerationFileTreeTest {

    // Feature 085 (universal-runtime collapse) replaced the trio
    // (RuntimeFlow + RuntimeController + RuntimeControllerAdapter)Generator
    // with a single ModuleRuntimeGenerator emitting controller/{Module}Runtime.kt.
    private val sampleTree = GenerationFileTree(
        folders = listOf(
            FolderNode(
                name = "controller",
                files = listOf(
                    FileNode("ControllerInterface.kt", isSelected = true, generatorId = "RuntimeControllerInterfaceGenerator"),
                    FileNode("Runtime.kt", isSelected = true, generatorId = "ModuleRuntimeGenerator"),
                    FileNode("ViewModelLink.kt", isSelected = true, generatorId = "RuntimeViewModelGenerator")
                ),
                selectionState = TriState.ALL
            ),
            FolderNode(
                name = "flow",
                files = listOf(
                    FileNode("Test.flow.kt", isSelected = true, generatorId = "FlowKtGenerator"),
                    FileNode("Stub.kt", isSelected = true, generatorId = "UserInterfaceStubGenerator")
                ),
                selectionState = TriState.ALL
            )
        )
    )

    @Test
    fun `empty tree has no folders`() {
        val tree = GenerationFileTree.empty()
        assertTrue(tree.folders.isEmpty())
        assertEquals(0, tree.totalFiles)
        assertEquals(0, tree.selectedFiles)
    }

    @Test
    fun `totalFiles counts all files across folders`() {
        assertEquals(5, sampleTree.totalFiles)
    }

    @Test
    fun `selectedFiles counts selected files`() {
        assertEquals(5, sampleTree.selectedFiles)
    }

    @Test
    fun `toggleFolder deselects all children when folder is ALL`() {
        val updated = sampleTree.toggleFolder("controller")
        val folder = updated.folders.first { it.name == "controller" }
        assertEquals(TriState.NONE, folder.selectionState)
        assertTrue(folder.files.all { !it.isSelected })
    }

    @Test
    fun `toggleFolder selects all children when folder is NONE`() {
        val deselected = sampleTree.toggleFolder("controller")
        val reselected = deselected.toggleFolder("controller")
        val folder = reselected.folders.first { it.name == "controller" }
        assertEquals(TriState.ALL, folder.selectionState)
        assertTrue(folder.files.all { it.isSelected })
    }

    @Test
    fun `toggleFolder selects all when folder is PARTIAL`() {
        val partial = sampleTree.toggleFile("controller", "Controller.kt")
        assertEquals(TriState.PARTIAL, partial.folders.first { it.name == "controller" }.selectionState)

        val toggled = partial.toggleFolder("controller")
        val folder = toggled.folders.first { it.name == "controller" }
        assertEquals(TriState.ALL, folder.selectionState)
        assertTrue(folder.files.all { it.isSelected })
    }

    @Test
    fun `toggleFile deselects individual file`() {
        val updated = sampleTree.toggleFile("controller", "Controller.kt")
        val folder = updated.folders.first { it.name == "controller" }
        assertFalse(folder.files.first { it.name == "Controller.kt" }.isSelected)
        assertTrue(folder.files.first { it.name == "ControllerInterface.kt" }.isSelected)
    }

    @Test
    fun `toggleFile updates parent folder to PARTIAL`() {
        val updated = sampleTree.toggleFile("controller", "Controller.kt")
        val folder = updated.folders.first { it.name == "controller" }
        assertEquals(TriState.PARTIAL, folder.selectionState)
    }

    @Test
    fun `toggleFile updates parent to NONE when last file deselected`() {
        var tree = sampleTree
        tree = tree.toggleFile("flow", "Test.flow.kt")
        tree = tree.toggleFile("flow", "TestFlow.kt")
        val folder = tree.folders.first { it.name == "flow" }
        assertEquals(TriState.NONE, folder.selectionState)
    }

    @Test
    fun `toggleFile does not affect other folders`() {
        val updated = sampleTree.toggleFile("controller", "Controller.kt")
        val flowFolder = updated.folders.first { it.name == "flow" }
        assertEquals(TriState.ALL, flowFolder.selectionState)
    }

    @Test
    fun `toggleFolder does not affect other folders`() {
        val updated = sampleTree.toggleFolder("controller")
        val flowFolder = updated.folders.first { it.name == "flow" }
        assertEquals(TriState.ALL, flowFolder.selectionState)
        assertEquals(2, updated.folders.first { it.name == "flow" }.files.count { it.isSelected })
    }

    @Test
    fun `computeSelectionState returns correct state`() {
        assertEquals(TriState.NONE, FolderNode.computeSelectionState(emptyList()))
        assertEquals(TriState.ALL, FolderNode.computeSelectionState(listOf(FileNode("a.kt", true))))
        assertEquals(TriState.NONE, FolderNode.computeSelectionState(listOf(FileNode("a.kt", false))))
        assertEquals(TriState.PARTIAL, FolderNode.computeSelectionState(listOf(
            FileNode("a.kt", true), FileNode("b.kt", false)
        )))
    }
}

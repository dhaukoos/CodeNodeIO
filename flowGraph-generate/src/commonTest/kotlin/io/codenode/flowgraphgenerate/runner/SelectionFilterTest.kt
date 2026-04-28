/*
 * SelectionFilterTest - Tests for file-tree to generator exclusion mapping
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.runner

import io.codenode.flowgraphgenerate.model.FileNode
import io.codenode.flowgraphgenerate.model.FolderNode
import io.codenode.flowgraphgenerate.model.GenerationFileTree
import io.codenode.flowgraphgenerate.model.TriState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class SelectionFilterTest {

    @Test
    fun `empty filter includes all generators`() {
        val filter = SelectionFilter()
        assertTrue(filter.isIncluded("FlowKtGenerator"))
        assertTrue(filter.isIncluded("ModuleRuntimeGenerator"))
    }

    @Test
    fun `filter excludes specified generators`() {
        val filter = SelectionFilter(excludedGeneratorIds = setOf("ModuleRuntimeGenerator"))
        assertFalse(filter.isIncluded("ModuleRuntimeGenerator"))
        assertTrue(filter.isIncluded("FlowKtGenerator"))
    }

    @Test
    fun `fromFileTree excludes deselected files`() {
        val tree = GenerationFileTree(
            folders = listOf(
                FolderNode(
                    name = "controller",
                    files = listOf(
                        FileNode("Controller.kt", isSelected = false, generatorId = "ModuleRuntimeGenerator"),
                        FileNode("ControllerInterface.kt", isSelected = true, generatorId = "RuntimeControllerInterfaceGenerator")
                    ),
                    selectionState = TriState.PARTIAL
                )
            )
        )
        val filter = SelectionFilter.fromFileTree(tree)
        assertFalse(filter.isIncluded("ModuleRuntimeGenerator"))
        assertTrue(filter.isIncluded("RuntimeControllerInterfaceGenerator"))
    }

    @Test
    fun `fromFileTree with all selected produces empty exclusions`() {
        val tree = GenerationFileTree(
            folders = listOf(
                FolderNode(
                    name = "flow",
                    files = listOf(
                        FileNode("Flow.kt", isSelected = true, generatorId = "FlowKtGenerator"),
                        FileNode("Runtime.kt", isSelected = true, generatorId = "ModuleRuntimeGenerator")
                    ),
                    selectionState = TriState.ALL
                )
            )
        )
        val filter = SelectionFilter.fromFileTree(tree)
        assertTrue(filter.excludedGeneratorIds.isEmpty())
    }

    @Test
    fun `fromFileTree with all deselected excludes all`() {
        val tree = GenerationFileTree(
            folders = listOf(
                FolderNode(
                    name = "controller",
                    files = listOf(
                        FileNode("Controller.kt", isSelected = false, generatorId = "ModuleRuntimeGenerator"),
                        FileNode("ControllerInterface.kt", isSelected = false, generatorId = "RuntimeControllerInterfaceGenerator")
                    ),
                    selectionState = TriState.NONE
                )
            )
        )
        val filter = SelectionFilter.fromFileTree(tree)
        assertEquals(2, filter.excludedGeneratorIds.size)
        assertFalse(filter.isIncluded("ModuleRuntimeGenerator"))
        assertFalse(filter.isIncluded("RuntimeControllerInterfaceGenerator"))
    }

    @Test
    fun `fromFileTree ignores files with empty generatorId`() {
        val tree = GenerationFileTree(
            folders = listOf(
                FolderNode(
                    name = "misc",
                    files = listOf(
                        FileNode("readme.txt", isSelected = false, generatorId = "")
                    ),
                    selectionState = TriState.NONE
                )
            )
        )
        val filter = SelectionFilter.fromFileTree(tree)
        assertTrue(filter.excludedGeneratorIds.isEmpty())
    }
}

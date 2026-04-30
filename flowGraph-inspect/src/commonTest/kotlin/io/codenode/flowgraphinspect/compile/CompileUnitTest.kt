/*
 * CompileUnitTest - TDD Red tests for CompileUnit sealed class
 * License: Apache 2.0
 */

package io.codenode.flowgraphinspect.compile

import io.codenode.fbpdsl.model.PlacementLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CompileUnitTest {

    private val sourceA = CompileSource("/tmp/A.kt", PlacementLevel.MODULE, "Demo")
    private val sourceB = CompileSource("/tmp/B.kt", PlacementLevel.MODULE, "Demo")

    @Test
    fun `SingleFile sources contains exactly the wrapped source`() {
        val unit = CompileUnit.SingleFile(sourceA)
        assertEquals(listOf(sourceA), unit.sources)
    }

    @Test
    fun `SingleFile description names the file`() {
        val unit = CompileUnit.SingleFile(sourceA)
        assertTrue(unit.description.contains("A.kt"), "description should mention filename: ${unit.description}")
        assertTrue(unit.description.startsWith("File:"))
    }

    @Test
    fun `Module sources is non-empty by invariant`() {
        assertFailsWith<IllegalArgumentException> {
            CompileUnit.Module(moduleName = "Demo", tier = PlacementLevel.MODULE, sources = emptyList())
        }
    }

    @Test
    fun `Module moduleName must not be blank`() {
        assertFailsWith<IllegalArgumentException> {
            CompileUnit.Module(moduleName = "", tier = PlacementLevel.MODULE, sources = listOf(sourceA))
        }
    }

    @Test
    fun `Module description names the module and tier`() {
        val unit = CompileUnit.Module("Demo", PlacementLevel.MODULE, listOf(sourceA, sourceB))
        assertTrue(unit.description.contains("Demo"))
        assertTrue(unit.description.contains("module", ignoreCase = true))
    }

    @Test
    fun `Module sources composition matches the input list`() {
        val unit = CompileUnit.Module("Demo", PlacementLevel.MODULE, listOf(sourceA, sourceB))
        assertEquals(listOf(sourceA, sourceB), unit.sources)
    }
}

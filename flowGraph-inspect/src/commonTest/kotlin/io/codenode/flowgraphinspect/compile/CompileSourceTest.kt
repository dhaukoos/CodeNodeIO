/*
 * CompileSourceTest - TDD Red tests for CompileSource data class
 * License: Apache 2.0
 */

package io.codenode.flowgraphinspect.compile

import io.codenode.fbpdsl.model.PlacementLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CompileSourceTest {

    @Test
    fun `MODULE tier requires non-null hostModuleName`() {
        assertFailsWith<IllegalArgumentException> {
            CompileSource(absolutePath = "/tmp/X.kt", tier = PlacementLevel.MODULE, hostModuleName = null)
        }
    }

    @Test
    fun `PROJECT tier requires non-null hostModuleName`() {
        assertFailsWith<IllegalArgumentException> {
            CompileSource(absolutePath = "/tmp/X.kt", tier = PlacementLevel.PROJECT, hostModuleName = null)
        }
    }

    @Test
    fun `UNIVERSAL tier permits null hostModuleName`() {
        val source = CompileSource(
            absolutePath = "/Users/me/.codenode/nodes/Foo.kt",
            tier = PlacementLevel.UNIVERSAL,
            hostModuleName = null
        )
        assertEquals(PlacementLevel.UNIVERSAL, source.tier)
        assertEquals(null, source.hostModuleName)
    }

    @Test
    fun `blank absolutePath is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            CompileSource(absolutePath = "", tier = PlacementLevel.MODULE, hostModuleName = "X")
        }
    }

    @Test
    fun `data class equality holds`() {
        val a = CompileSource("/tmp/X.kt", PlacementLevel.MODULE, "TestModule")
        val b = CompileSource("/tmp/X.kt", PlacementLevel.MODULE, "TestModule")
        assertEquals(a, b)
    }
}

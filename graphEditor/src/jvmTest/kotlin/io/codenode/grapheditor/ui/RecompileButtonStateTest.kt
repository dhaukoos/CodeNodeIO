/*
 * RecompileButtonStateTest — TDD tests for T046 (US3): toolbar button state
 *
 * Compose-UI testing requires `androidx.compose.ui:ui-test-junit4` infra that
 * graphEditor doesn't currently set up. We instead extract the button's
 * state-derivation into a pure function and test that surface — it's where the
 * spec-relevant decisions (FR-008 label, FR-006 disabled-while-compiling) live.
 *
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RecompileButtonStateTest {

    @Test
    fun `null moduleName returns null state — button is hidden`() {
        assertNull(recompileButtonState(moduleName = null, isRecompiling = false))
        assertNull(recompileButtonState(moduleName = null, isRecompiling = true))
    }

    @Test
    fun `idle state shows the labeled enabled button`() {
        val s = recompileButtonState(moduleName = "TestModule", isRecompiling = false)
        assertEquals(RecompileButtonState(label = "Recompile module: TestModule", enabled = true), s)
    }

    @Test
    fun `compiling state shows the busy label and is disabled`() {
        val s = recompileButtonState(moduleName = "TestModule", isRecompiling = true)
        assertEquals(RecompileButtonState(label = "Recompiling…", enabled = false), s)
    }

    @Test
    fun `label includes the module name verbatim — FR-008 scope obviousness`() {
        val s = recompileButtonState(moduleName = "MyModule", isRecompiling = false)
        assertEquals("Recompile module: MyModule", s?.label)
    }
}

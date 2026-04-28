/*
 * UIFBPSpecTest - Pins the post-082/083 typed-input shape of UIFBPSpec
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class UIFBPSpecTest {

    @Test
    fun `constructing a spec with all three potentially-distinct identifiers succeeds`() {
        val spec = UIFBPSpec(
            flowGraphPrefix = "Quickstart084Alt",
            composableName = "RenamedDemoUI",
            viewModelTypeName = "Quickstart084AltViewModel",
            packageName = "io.codenode.quickstart084",
            sourceOutputs = emptyList(),
            sinkInputs = emptyList()
        )

        assertEquals("Quickstart084Alt", spec.flowGraphPrefix)
        assertEquals("RenamedDemoUI", spec.composableName)
        assertEquals("io.codenode.quickstart084", spec.packageName)
        assertNotEquals(
            spec.flowGraphPrefix, spec.composableName,
            "post-082/083 the flow-graph prefix and the user-authored composable name " +
                "are independent identifiers; the generator must surface both"
        )
    }

    @Test
    fun `deprecated moduleName resolves to flowGraphPrefix`() {
        val spec = UIFBPSpec(
            flowGraphPrefix = "DemoUI",
            composableName = "DemoUI",
            viewModelTypeName = "DemoUIViewModel",
            packageName = "io.codenode.demo",
            sourceOutputs = emptyList(),
            sinkInputs = emptyList()
        )

        @Suppress("DEPRECATION")
        assertEquals("DemoUI", spec.moduleName)
        @Suppress("DEPRECATION")
        assertEquals(spec.flowGraphPrefix, spec.moduleName,
            "the deprecated alias must map to flowGraphPrefix (not composableName) " +
                "because pre-082/083 callers used moduleName for the file prefix")
    }

    @Test
    fun `ipTypeImports field is unchanged from pre-082-083`() {
        val imports = listOf("io.codenode.iptypes.CalculationResults", "io.codenode.iptypes.CoordsXY")
        val spec = UIFBPSpec(
            flowGraphPrefix = "DemoUI",
            composableName = "DemoUI",
            viewModelTypeName = "DemoUIViewModel",
            packageName = "io.codenode.demo",
            sourceOutputs = emptyList(),
            sinkInputs = emptyList(),
            ipTypeImports = imports
        )
        assertEquals(imports, spec.ipTypeImports)
    }
}

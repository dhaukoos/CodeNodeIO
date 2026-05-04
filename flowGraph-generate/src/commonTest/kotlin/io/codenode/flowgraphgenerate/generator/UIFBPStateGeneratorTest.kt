/*
 * UIFBPStateGeneratorTest — feature 087 / Design B.
 *
 * Pins the post-Design-B shape: `data class {Name}State` with one `val` per
 * sinkInput, default values per the contract's table, and NO MutableStateFlow /
 * asStateFlow / reset() / object-singleton emission. Per-flow-graph runtime
 * state lives in {Name}Runtime's closure; the State data class is the
 * UI-facing SSOT.
 *
 * Test style: substring + structural assertions matching project convention,
 * supplemented by one byte-equality determinism case per generator (SC-005).
 *
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.generator

import io.codenode.flowgraphgenerate.parser.PortInfo
import io.codenode.flowgraphgenerate.parser.UIFBPSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UIFBPStateGeneratorTest {

    private val demoSpec = UIFBPSpec(
        flowGraphPrefix = "DemoUI",
        composableName = "DemoUI",
        viewModelTypeName = "DemoUIViewModel",
        packageName = "io.codenode.demo",
        sourceOutputs = listOf(
            PortInfo("numA", "Double"),
            PortInfo("numB", "Double")
        ),
        sinkInputs = listOf(
            PortInfo("results", "CalculationResults", isNullable = true)
        ),
        ipTypeImports = listOf("io.codenode.demo.iptypes.CalculationResults")
    )

    // Case 1: data class with one val per sinkInput in declared order
    @Test
    fun `generate emits a data class with one val per sinkInput in declared order`() {
        val spec = demoSpec.copy(sinkInputs = listOf(
            PortInfo("foo", "Int"),
            PortInfo("bar", "Boolean")
        ))
        val output = UIFBPStateGenerator().generate(spec)
        assertTrue(output.contains("data class DemoUIState("),
            "Design B: State MUST be a data class, not an object singleton")
        // Property order matches sinkInputs declared order.
        val fooIdx = output.indexOf("val foo")
        val barIdx = output.indexOf("val bar")
        assertTrue(fooIdx in 0 until barIdx,
            "properties MUST appear in spec.sinkInputs declared order")
    }

    // Case 2: correct defaults per primitive type
    @Test
    fun `generate uses correct defaults per primitive type`() {
        val spec = demoSpec.copy(sinkInputs = listOf(
            PortInfo("intF", "Int"),
            PortInfo("longF", "Long"),
            PortInfo("doubleF", "Double"),
            PortInfo("floatF", "Float"),
            PortInfo("boolF", "Boolean"),
            PortInfo("strF", "String")
        ), ipTypeImports = emptyList())
        val output = UIFBPStateGenerator().generate(spec)
        assertTrue(output.contains("val intF: Int = 0"))
        assertTrue(output.contains("val longF: Long = 0L"))
        assertTrue(output.contains("val doubleF: Double = 0.0"))
        assertTrue(output.contains("val floatF: Float = 0.0f"))
        assertTrue(output.contains("val boolF: Boolean = false"))
        assertTrue(output.contains("val strF: String = \"\""))
    }

    // Case 3: nullable IP type → nullable property with null default
    @Test
    fun `generate uses null default for nullable IP types`() {
        val output = UIFBPStateGenerator().generate(demoSpec)
        assertTrue(output.contains("val results: CalculationResults? = null"),
            "nullable port → property emitted as `T?` with default null")
    }

    // Case 4: zero sinkInputs → zero-arg data class
    @Test
    fun `generate on zero sinkInputs emits a zero-arg data class`() {
        val spec = demoSpec.copy(sinkInputs = emptyList(), ipTypeImports = emptyList())
        val output = UIFBPStateGenerator().generate(spec)
        assertTrue(output.contains("data class DemoUIState()"),
            "FR-009: empty-state case emits a zero-arg data class")
    }

    // Case 5: package
    @Test
    fun `generate package matches spec packageName plus viewmodel`() {
        val output = UIFBPStateGenerator().generate(demoSpec)
        assertTrue(output.contains("package io.codenode.demo.viewmodel"))
    }

    // Case 6: class name uses flowGraphPrefix
    @Test
    fun `generate class name uses flowGraphPrefix not composableName`() {
        val divergent = demoSpec.copy(flowGraphPrefix = "AltPrefix")
        val output = UIFBPStateGenerator().generate(divergent)
        assertTrue(output.contains("data class AltPrefixState("),
            "class name MUST come from flowGraphPrefix")
    }

    // Case 7: IP-type imports only when needed
    @Test
    fun `generate emits IP-type imports only when at least one sinkInput needs them`() {
        // demoSpec has CalculationResults sinkInput → import expected
        val output = UIFBPStateGenerator().generate(demoSpec)
        assertTrue(output.contains("import io.codenode.demo.iptypes.CalculationResults"))

        // No sinkInputs referencing the IP type → no import (even if ipTypeImports listed)
        val noNeed = demoSpec.copy(
            sinkInputs = listOf(PortInfo("count", "Int"))
        )
        val output2 = UIFBPStateGenerator().generate(noNeed)
        assertFalse(output2.contains("import io.codenode.demo.iptypes.CalculationResults"),
            "when no sinkInput references the IP type, the import is omitted")
    }

    // Case 8: no StateStore companion (Design B)
    @Test
    fun `generate does NOT emit any StateStore companion (Design B)`() {
        val output = UIFBPStateGenerator().generate(demoSpec)
        assertFalse(output.contains("StateStore"),
            "Design B eliminates the singleton entirely; no StateStore companion is emitted")
    }

    // Case 9: no MutableStateFlow / asStateFlow / reset()
    @Test
    fun `generate does NOT emit MutableStateFlow asStateFlow or reset (those moved to {Name}Runtime)`() {
        val output = UIFBPStateGenerator().generate(demoSpec)
        assertFalse(output.contains("MutableStateFlow"),
            "Design B: per-port flows live on the controller, not on State")
        assertFalse(output.contains("asStateFlow"),
            "Design B: State is a pure data class with no flow exposure")
        assertFalse(output.contains("fun reset()"),
            "Design B: reset semantics live in the Runtime factory's onReset closure")
        assertFalse(output.contains("object DemoUIState"),
            "Design B: State is `data class`, not `object`")
    }

    // Case 10: non-primitive non-nullable IP type → emit nullable + null (per E4 / spec edge case)
    @Test
    fun `generate handles a non-nullable port carrying a non-primitive IP type by emitting nullable with null default`() {
        // Per spec.md Edge Cases L135-138: the generator falls back to nullable + null
        // for any port whose type lacks a no-arg constructor. The generator's
        // default-value table already returns "null" for unknown (non-primitive) types;
        // for symmetry with the runtime State data class, the property type itself
        // becomes nullable too.
        val spec = demoSpec.copy(
            sinkInputs = listOf(PortInfo("calc", "CalculationResults", isNullable = false)),
            ipTypeImports = listOf("io.codenode.demo.iptypes.CalculationResults")
        )
        val output = UIFBPStateGenerator().generate(spec)
        assertTrue(output.contains("val calc: CalculationResults? = null"),
            "non-primitive non-nullable port falls back to nullable + null default — " +
                "authors who need a non-null default override post-generation")
    }

    // Case 11: determinism (SC-005)
    @Test
    fun `generate output is byte-identical across two consecutive calls`() {
        val a = UIFBPStateGenerator().generate(demoSpec)
        val b = UIFBPStateGenerator().generate(demoSpec)
        assertEquals(a, b, "regenerating on an unchanged spec MUST be byte-identical")
    }
}

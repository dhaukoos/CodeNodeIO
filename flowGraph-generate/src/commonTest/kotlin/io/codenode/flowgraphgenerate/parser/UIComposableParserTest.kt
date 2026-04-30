/*
 * UIComposableParserTest - Tests for UI file parsing
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UIComposableParserTest {

    private val parser = UIComposableParser()

    private val demoUIContent = """
        package io.codenode.demo.userInterface

        import androidx.compose.foundation.layout.*
        import androidx.compose.material.*
        import androidx.compose.runtime.*
        import androidx.compose.ui.Modifier
        import io.codenode.demo.DemoUIViewModel
        import io.codenode.demo.iptypes.CalculationResults

        @Composable
        fun DemoUI(
            viewModel: DemoUIViewModel,
            modifier: Modifier = Modifier
        ) {
            var textA by remember { mutableStateOf("") }
            var textB by remember { mutableStateOf("") }
            val results by viewModel.results.collectAsState()

            Column(modifier = modifier.fillMaxSize()) {
                InputSection(
                    onEmit = {
                        val a = textA.toDoubleOrNull()
                        val b = textB.toDoubleOrNull()
                        if (a != null && b != null) {
                            viewModel.emit(a, b)
                        }
                    }
                )
                ResultsSection(results = results)
            }
        }

        @Composable
        private fun ResultsSection(results: CalculationResults?) {
            if (results == null) {
                Text("Enter values")
            } else {
                Text("Sum: ${'$'}{results.sum}")
            }
        }
    """.trimIndent()

    // T001: Extract module name and ViewModel type
    @Test
    fun `extracts module name and ViewModel type from composable signature`() {
        val result = parser.parse(demoUIContent)
        assertTrue(result.isSuccess)
        val spec = result.spec!!
        assertEquals("DemoUI", spec.composableName)
        assertEquals("DemoUI", spec.flowGraphPrefix,
            "without an explicit flowGraphPrefix override, the parser falls back to the " +
                "Composable function name (pre-082/083 convention)")
        assertEquals("DemoUIViewModel", spec.viewModelTypeName)
    }

    // Post-082/083: parse(content, flowGraphPrefix) decouples the file prefix from the
    // user-authored Composable function name (Decision 2 of feature 084 spec).
    @Test
    fun `parse with explicit flowGraphPrefix decouples it from composableName`() {
        val result = parser.parse(demoUIContent, flowGraphPrefix = "AltPrefix")
        assertTrue(result.isSuccess)
        val spec = result.spec!!
        assertEquals("AltPrefix", spec.flowGraphPrefix,
            "explicit override drives the file prefix and PreviewRegistry key")
        assertEquals("DemoUI", spec.composableName,
            "the user-authored Composable function name is independent of the override")
    }

    @Test
    fun `extracts base package from userInterface subpackage`() {
        val result = parser.parse(demoUIContent)
        val spec = result.spec!!
        assertEquals("io.codenode.demo", spec.packageName)
    }

    // T002: Extract Source outputs from emit call
    @Test
    fun `extracts Source outputs from viewModel emit call`() {
        val result = parser.parse(demoUIContent)
        val spec = result.spec!!
        assertEquals(2, spec.sourceOutputs.size)
        assertEquals("a", spec.sourceOutputs[0].name)
        assertEquals("Double", spec.sourceOutputs[0].typeName)
        assertEquals("b", spec.sourceOutputs[1].name)
        assertEquals("Double", spec.sourceOutputs[1].typeName)
    }

    // T003: Extract Sink inputs from collectAsState
    @Test
    fun `extracts Sink inputs from viewModel collectAsState calls`() {
        val result = parser.parse(demoUIContent)
        val spec = result.spec!!
        assertEquals(1, spec.sinkInputs.size)
        assertEquals("results", spec.sinkInputs[0].name)
    }

    @Test
    fun `detects nullable Sink input type`() {
        val result = parser.parse(demoUIContent)
        val spec = result.spec!!
        assertTrue(spec.sinkInputs[0].isNullable)
    }

    @Test
    fun `extracts IP type imports`() {
        val result = parser.parse(demoUIContent)
        val spec = result.spec!!
        assertTrue(spec.ipTypeImports.contains("io.codenode.demo.iptypes.CalculationResults"))
    }

    // T004: Error when no ViewModel parameter
    @Test
    fun `returns error when no ViewModel parameter found`() {
        val noViewModelContent = """
            package io.codenode.demo.userInterface

            import androidx.compose.runtime.Composable

            @Composable
            fun SimpleScreen(modifier: Modifier = Modifier) {
                Text("Hello")
            }
        """.trimIndent()

        val result = parser.parse(noViewModelContent)
        assertFalse(result.isSuccess)
        assertNull(result.spec)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun `returns error when no package declaration found`() {
        val noPackageContent = """
            import androidx.compose.runtime.Composable

            @Composable
            fun DemoUI(viewModel: DemoUIViewModel) { }
        """.trimIndent()

        val result = parser.parse(noPackageContent)
        assertFalse(result.isSuccess)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun `handles multiple collectAsState properties`() {
        val multiSinkContent = """
            package io.codenode.demo.userInterface

            import io.codenode.demo.DemoUIViewModel

            @Composable
            fun MultiSink(viewModel: DemoUIViewModel, modifier: Modifier = Modifier) {
                val results by viewModel.results.collectAsState()
                val status by viewModel.status.collectAsState()
                val error by viewModel.error.collectAsState()
            }
        """.trimIndent()

        val result = parser.parse(multiSinkContent)
        val spec = result.spec!!
        assertEquals(3, spec.sinkInputs.size)
        assertEquals("results", spec.sinkInputs[0].name)
        assertEquals("status", spec.sinkInputs[1].name)
        assertEquals("error", spec.sinkInputs[2].name)
    }

    @Test
    fun `ignores lifecycle methods on viewModel`() {
        val lifecycleContent = """
            package io.codenode.demo.userInterface

            import io.codenode.demo.DemoUIViewModel

            @Composable
            fun LifecycleUI(viewModel: DemoUIViewModel, modifier: Modifier = Modifier) {
                val results by viewModel.results.collectAsState()
                Button(onClick = { viewModel.start() }) { Text("Start") }
                Button(onClick = { viewModel.stop() }) { Text("Stop") }
                Button(onClick = { viewModel.reset() }) { Text("Reset") }
                Button(onClick = { viewModel.emit(42.0) }) { Text("Emit") }
            }
        """.trimIndent()

        val result = parser.parse(lifecycleContent)
        val spec = result.spec!!
        assertEquals(1, spec.sourceOutputs.size)
        assertEquals("42.0", spec.sourceOutputs[0].name)
    }

    // T018: Integration test with actual DemoUI.kt content
    @Test
    fun `parses real DemoUI file with full content`() {
        val realDemoUI = """
package io.codenode.demo.userInterface

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codenode.demo.DemoUIViewModel
import io.codenode.demo.iptypes.CalculationResults

@Composable
fun DemoUI(
    viewModel: DemoUIViewModel,
    modifier: Modifier = Modifier
) {
    var textA by remember { mutableStateOf("") }
    var textB by remember { mutableStateOf("") }
    var errorA by remember { mutableStateOf(false) }
    var errorB by remember { mutableStateOf(false) }
    val results by viewModel.results.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        InputSection(
            textA = textA,
            textB = textB,
            errorA = errorA,
            errorB = errorB,
            onTextAChanged = { textA = it; errorA = false },
            onTextBChanged = { textB = it; errorB = false },
            onEmit = {
                val a = textA.toDoubleOrNull()
                val b = textB.toDoubleOrNull()
                errorA = a == null
                errorB = b == null
                if (a != null && b != null) {
                    viewModel.emit(a, b)
                }
            }
        )

        Divider()

        ResultsSection(results = results)
    }
}

@Composable
private fun ResultsSection(results: CalculationResults?) {
    if (results == null) {
        Text("Enter values and press Emit")
    } else {
        Text("Sum: ${'$'}{results.sum}")
    }
}
        """.trimIndent()

        val result = parser.parse(realDemoUI)
        assertTrue(result.isSuccess, "Parse should succeed: ${result.errorMessage}")
        val spec = result.spec!!

        assertEquals("DemoUI", spec.flowGraphPrefix)
        assertEquals("DemoUIViewModel", spec.viewModelTypeName)
        assertEquals("io.codenode.demo", spec.packageName)

        assertEquals(2, spec.sourceOutputs.size, "Should have 2 source outputs from emit(a, b)")
        assertEquals("a", spec.sourceOutputs[0].name)
        assertEquals("Double", spec.sourceOutputs[0].typeName)
        assertEquals("b", spec.sourceOutputs[1].name)
        assertEquals("Double", spec.sourceOutputs[1].typeName)

        assertEquals(1, spec.sinkInputs.size, "Should have 1 sink input from results.collectAsState()")
        assertEquals("results", spec.sinkInputs[0].name)
        assertTrue(spec.sinkInputs[0].isNullable, "results should be nullable (CalculationResults?)")

        assertTrue(spec.ipTypeImports.contains("io.codenode.demo.iptypes.CalculationResults"))
    }
}

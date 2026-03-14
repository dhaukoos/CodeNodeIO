/*
 * CodeNodeStandaloneTestExample - Demonstrates standalone testing of CodeNodeDefinition
 *
 * This test pattern shows how to test a self-contained node in isolation:
 * 1. Instantiate the CodeNodeDefinition (it's a Kotlin object singleton)
 * 2. Verify metadata (name, category, ports)
 * 3. Call createRuntime() to get a NodeRuntime
 * 4. Wire input/output channels directly
 * 5. Start, feed input, assert on output — no flow graph or controller needed
 *
 * License: Apache 2.0
 */

package io.codenode.nodes

import io.codenode.fbpdsl.runtime.NodeCategory
import io.codenode.fbpdsl.runtime.TransformerRuntime
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class CodeNodeStandaloneTestExample {

    /**
     * Verify that a CodeNodeDefinition's metadata is correct.
     * No runtime, no channels — just check the definition itself.
     */
    @Test
    fun `definition metadata is correct`() {
        val def = Test3TransformerCodeNode

        assertEquals("Test3Transformer", def.name)
        assertEquals(NodeCategory.TRANSFORMER, def.category)
        assertEquals(1, def.inputPorts.size)
        assertEquals(1, def.outputPorts.size)
        assertEquals("input1", def.inputPorts[0].name)
        assertEquals("output1", def.outputPorts[0].name)
    }

    /**
     * Verify that toNodeTypeDefinition() produces a palette-ready entry.
     */
    @Test
    fun `toNodeTypeDefinition produces valid palette entry`() {
        val ntd = Test3TransformerCodeNode.toNodeTypeDefinition()

        assertEquals("Test3Transformer", ntd.name)
        assertEquals(2, ntd.portTemplates.size) // 1 input + 1 output
        assertTrue(ntd.defaultConfiguration.containsKey("_codeNodeDefinition"))
    }

    /**
     * Standalone runtime test: create, wire channels, feed input, assert output.
     * No FlowGraph, no Controller, no RuntimeSession needed.
     */
    @Test
    fun `createRuntime produces working runtime in isolation`() = runTest {
        // 1. Get a runtime from the definition
        val runtime = Test3TransformerCodeNode.createRuntime("test-instance")
        assertNotNull(runtime)

        // 2. Cast to the expected runtime type and wire channels
        val transformer = runtime as TransformerRuntime<*, *>
        @Suppress("UNCHECKED_CAST")
        val typedTransformer = transformer as TransformerRuntime<Any, Any>

        val inputChannel = Channel<Any>(Channel.BUFFERED)
        val outputChannel = Channel<Any>(Channel.BUFFERED)
        typedTransformer.inputChannel = inputChannel
        typedTransformer.outputChannel = outputChannel

        // 3. Collect outputs in background
        val results = mutableListOf<Any>()
        val collectJob = launch {
            for (value in outputChannel) {
                results.add(value)
            }
        }

        // 4. Start the runtime
        typedTransformer.start(this) {}

        // 5. Feed input and verify output
        inputChannel.send("hello")
        inputChannel.send(42)
        advanceUntilIdle()

        // Test3TransformerCodeNode is a pass-through: input -> input
        assertEquals(2, results.size)
        assertEquals("hello", results[0])
        assertEquals(42, results[1])

        // 6. Cleanup
        typedTransformer.stop()
        collectJob.cancel()
    }
}

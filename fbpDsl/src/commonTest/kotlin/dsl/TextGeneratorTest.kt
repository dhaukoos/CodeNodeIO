/*
 * TextGenerator Test
 * Unit tests for FlowGraph to textual DSL generation
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.dsl

import io.codenode.fbpdsl.model.*
import kotlin.test.*

class TextGeneratorTest {

    @Test
    fun `should generate DSL text from simple FlowGraph`() {
        // Given a simple flow graph
        val graph = flowGraph("TestGraph", version = "1.0.0") {
            val gen = codeNode("Generator") {
                output("data", String::class)
            }

            val proc = codeNode("Processor") {
                input("input", String::class)
                output("result", String::class)
            }

            gen.output("data") connect proc.input("input")
        }

        // When generating DSL text
        val dslText = TextGenerator.generate(graph)

        // Then the text should contain graph declaration
        assertTrue(dslText.contains("flowGraph"), "Should contain flowGraph declaration")
        assertTrue(dslText.contains("TestGraph"), "Should contain graph name")
        assertTrue(dslText.contains("1.0.0"), "Should contain version")

        // And should contain node declarations
        assertTrue(dslText.contains("Generator"), "Should contain Generator node")
        assertTrue(dslText.contains("Processor"), "Should contain Processor node")
        assertTrue(dslText.contains("codeNode"), "Should contain codeNode keyword")

        // And should contain port declarations
        assertTrue(dslText.contains("input"), "Should contain input port")
        assertTrue(dslText.contains("output"), "Should contain output ports")

        // And should contain connection
        assertTrue(dslText.contains("connect"), "Should contain connection")
    }

    @Test
    fun `should generate readable formatting with proper indentation`() {
        // Given a graph with nested structure
        val graph = flowGraph("FormattingTest", version = "1.0.0") {
            val node1 = codeNode("Node1") {
                output("out", String::class)
            }
        }

        // When generating DSL text
        val dslText = TextGenerator.generate(graph)

        // Then the text should be properly formatted
        assertTrue(dslText.contains("\n"), "Should contain line breaks")
        assertTrue(dslText.contains("    "), "Should contain indentation")

        // And should be parseable back to FlowGraph (structural check)
        assertTrue(dslText.startsWith("flowGraph"), "Should start with flowGraph")
    }

    @Test
    fun `should include all graph metadata in generated text`() {
        // Given a graph with description and metadata
        val graph = flowGraph(
            name = "MetadataTest",
            version = "2.0.0",
            description = "Test graph with metadata"
        ) {}

        // When generating DSL text
        val dslText = TextGenerator.generate(graph)

        // Then all metadata should be included
        assertTrue(dslText.contains("MetadataTest"), "Should include name")
        assertTrue(dslText.contains("2.0.0"), "Should include version")
        assertTrue(dslText.contains("Test graph with metadata"), "Should include description")
    }

    @Test
    fun `should generate text for complex graph with multiple connections`() {
        // Given a complex graph with multiple nodes and connections
        val graph = flowGraph("ComplexGraph", version = "1.0.0") {
            val gen = codeNode("DataGen") {
                output("data", Any::class)
            }

            val filter = codeNode("Filter") {
                input("input", Any::class)
                output("passed", Any::class)
                output("rejected", Any::class)
            }

            val processor = codeNode("Processor") {
                input("data", Any::class)
                output("result", Any::class)
            }

            gen.output("data") connect filter.input("input")
            filter.output("passed") connect processor.input("data")
        }

        // When generating DSL text
        val dslText = TextGenerator.generate(graph)

        // Then all nodes should be represented
        assertTrue(dslText.contains("DataGen"), "Should contain DataGen node")
        assertTrue(dslText.contains("Filter"), "Should contain Filter node")
        assertTrue(dslText.contains("Processor"), "Should contain Processor node")

        // And all connections should be represented
        val connectCount = dslText.split("connect").size - 1
        assertEquals(2, connectCount, "Should have 2 connections")
    }

    @Test
    fun `should handle empty graph gracefully`() {
        // Given an empty graph
        val graph = flowGraph("EmptyGraph", version = "1.0.0") {}

        // When generating DSL text
        val dslText = TextGenerator.generate(graph)

        // Then should generate minimal valid DSL
        assertTrue(dslText.contains("flowGraph"), "Should contain flowGraph keyword")
        assertTrue(dslText.contains("EmptyGraph"), "Should contain graph name")
        assertFalse(dslText.contains("codeNode"), "Should not contain nodes")
    }

    @Test
    fun `should preserve port type information in generated text`() {
        // Given a graph with typed ports
        val graph = flowGraph("TypedPorts", version = "1.0.0") {
            val typed = codeNode("TypedNode") {
                input("stringInput", String::class)
                output("numberOutput", Int::class)
            }
        }

        // When generating DSL text
        val dslText = TextGenerator.generate(graph)

        // Then type information should be preserved
        assertTrue(dslText.contains("String::class") || dslText.contains("String"),
            "Should preserve String type")
        assertTrue(dslText.contains("Int::class") || dslText.contains("Int"),
            "Should preserve Int type")
    }
}

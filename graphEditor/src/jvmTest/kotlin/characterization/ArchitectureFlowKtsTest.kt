/*
 * Architecture FlowKts Verification Test
 * Verifies the architecture.flow.kt meta-FlowGraph parses correctly
 * and matches MIGRATION.md structure
 * License: Apache 2.0
 */

package characterization

import io.codenode.grapheditor.serialization.FlowKtParser
import java.io.File
import kotlin.test.*

/**
 * Verification test for graphEditor/architecture.flow.kt.
 * Ensures the meta-FlowGraph loads without errors and matches
 * the target architecture documented in MIGRATION.md.
 */
class ArchitectureFlowKtsTest {

    private val parser = FlowKtParser()

    private fun loadArchitectureFile(): String {
        val candidates = listOf(
            File("graphEditor/architecture.flow.kt"),
            File("../graphEditor/architecture.flow.kt"),
            File(System.getProperty("user.dir"), "graphEditor/architecture.flow.kt"),
            File(System.getProperty("user.dir")).parentFile?.let {
                File(it, "graphEditor/architecture.flow.kt")
            }
        ).filterNotNull()

        val file = candidates.firstOrNull { it.exists() }
            ?: fail("architecture.flow.kt not found. Searched: ${candidates.map { it.absolutePath }}")

        return file.readText()
    }

    @Test
    fun `architecture flow kts parses successfully`() {
        val content = loadArchitectureFile()
        val result = parser.parseFlowKt(content)

        assertTrue(result.isSuccess, "Parse failed: ${result.errorMessage}")
        assertNotNull(result.graph, "Parsed FlowGraph should not be null")
    }

    @Test
    fun `architecture flow kts has correct graph name`() {
        val content = loadArchitectureFile()
        val result = parser.parseFlowKt(content)

        assertTrue(result.isSuccess, "Parse failed: ${result.errorMessage}")
        assertEquals("Target Architecture", result.graph!!.name)
    }

    @Test
    fun `architecture flow kts contains all six module nodes`() {
        val content = loadArchitectureFile()
        val result = parser.parseFlowKt(content)

        assertTrue(result.isSuccess, "Parse failed: ${result.errorMessage}")
        val graph = result.graph!!
        val nodeNames = graph.rootNodes.map { it.name }.toSet()

        assertEquals(6, graph.rootNodes.size, "Should have 6 nodes (5 slices + root)")
        assertTrue("flowGraph-inspect" in nodeNames, "Missing flowGraph-inspect node")
        assertTrue("flowGraph-persist" in nodeNames, "Missing flowGraph-persist node")
        assertTrue("flowGraph-compose" in nodeNames, "Missing flowGraph-compose node")
        assertTrue("flowGraph-execute" in nodeNames, "Missing flowGraph-execute node")
        assertTrue("flowGraph-generate" in nodeNames, "Missing flowGraph-generate node")
        assertTrue("graphEditor" in nodeNames, "Missing graphEditor (root) node")
    }

    @Test
    fun `architecture flow kts has connections between modules`() {
        val content = loadArchitectureFile()
        val result = parser.parseFlowKt(content)

        assertTrue(result.isSuccess, "Parse failed: ${result.errorMessage}")
        val graph = result.graph!!

        assertTrue(graph.connections.isNotEmpty(), "Should have inter-module connections")
        assertTrue(graph.connections.size >= 15,
            "Should have at least 15 connections (got ${graph.connections.size})")
    }

    @Test
    fun `inspect node is the most connected source`() {
        val content = loadArchitectureFile()
        val result = parser.parseFlowKt(content)

        assertTrue(result.isSuccess, "Parse failed: ${result.errorMessage}")
        val graph = result.graph!!

        val inspectNode = graph.rootNodes.first { it.name == "flowGraph-inspect" }
        val inspectOutbound = graph.connections.count { it.sourceNodeId == inspectNode.id }

        assertTrue(inspectOutbound >= 8,
            "inspect should have most outbound connections (got $inspectOutbound)")
    }

    @Test
    fun `root node has no outbound connections`() {
        val content = loadArchitectureFile()
        val result = parser.parseFlowKt(content)

        assertTrue(result.isSuccess, "Parse failed: ${result.errorMessage}")
        val graph = result.graph!!

        val rootNode = graph.rootNodes.first { it.name == "graphEditor" }
        val rootOutbound = graph.connections.count { it.sourceNodeId == rootNode.id }

        assertEquals(0, rootOutbound,
            "Root (composition root) should have no outbound connections — it only consumes")
    }

    @Test
    fun `all target platforms are specified`() {
        val content = loadArchitectureFile()
        val result = parser.parseFlowKt(content)

        assertTrue(result.isSuccess, "Parse failed: ${result.errorMessage}")
        val graph = result.graph!!

        assertTrue(graph.targetPlatforms.isNotEmpty(), "Should have target platforms")
    }
}

/*
 * Architecture FlowKts Verification Test
 * Verifies the architecture.flow.kt meta-FlowGraph parses correctly
 * and matches MIGRATION.md structure
 * License: Apache 2.0
 */

package characterization

import io.codenode.flowgraphpersist.serialization.FlowKtParser
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
    fun `architecture flow kt parses successfully`() {
        val content = loadArchitectureFile()
        val result = parser.parseFlowKt(content)

        assertTrue(result.isSuccess, "Parse failed: ${result.errorMessage}")
        assertNotNull(result.graph, "Parsed FlowGraph should not be null")
    }

    @Test
    fun `architecture flow kt has correct graph name`() {
        val content = loadArchitectureFile()
        val result = parser.parseFlowKt(content)

        assertTrue(result.isSuccess, "Parse failed: ${result.errorMessage}")
        assertEquals("Target Architecture", result.graph!!.name)
    }

    @Test
    fun `architecture flow kt contains all eight nodes`() {
        val content = loadArchitectureFile()
        val result = parser.parseFlowKt(content)

        assertTrue(result.isSuccess, "Parse failed: ${result.errorMessage}")
        val graph = result.graph!!
        val nodeNames = graph.rootNodes.map { it.name }.toSet()

        assertEquals(8, graph.rootNodes.size, "Should have 8 nodes (6 slices + source + sink)")
        assertTrue("flowGraph-types" in nodeNames, "Missing flowGraph-types node")
        assertTrue("flowGraph-inspect" in nodeNames, "Missing flowGraph-inspect node")
        assertTrue("flowGraph-persist" in nodeNames, "Missing flowGraph-persist node")
        assertTrue("flowGraph-compose" in nodeNames, "Missing flowGraph-compose node")
        assertTrue("flowGraph-execute" in nodeNames, "Missing flowGraph-execute node")
        assertTrue("flowGraph-generate" in nodeNames, "Missing flowGraph-generate node")
        assertTrue("graphEditor-source" in nodeNames, "Missing graphEditor-source node")
        assertTrue("graphEditor-sink" in nodeNames, "Missing graphEditor-sink node")
    }

    @Test
    fun `architecture flow kt has exactly 20 connections`() {
        val content = loadArchitectureFile()
        val result = parser.parseFlowKt(content)

        assertTrue(result.isSuccess, "Parse failed: ${result.errorMessage}")
        val graph = result.graph!!

        assertEquals(20, graph.connections.size,
            "Should have exactly 20 connections (15 state flows + 5 command flows)")
    }

    @Test
    fun `types and inspect are the two hub source nodes`() {
        val content = loadArchitectureFile()
        val result = parser.parseFlowKt(content)

        assertTrue(result.isSuccess, "Parse failed: ${result.errorMessage}")
        val graph = result.graph!!

        val typesNode = graph.rootNodes.first { it.name == "flowGraph-types" }
        val inspectNode = graph.rootNodes.first { it.name == "flowGraph-inspect" }
        val typesOutbound = graph.connections.count { it.sourceNodeId == typesNode.id }
        val inspectOutbound = graph.connections.count { it.sourceNodeId == inspectNode.id }

        assertEquals(4, typesOutbound,
            "types should have 4 outbound connections (got $typesOutbound)")
        assertEquals(4, inspectOutbound,
            "inspect should have 4 outbound connections (got $inspectOutbound)")
    }

    @Test
    fun `graphEditor-source has only command outputs`() {
        val content = loadArchitectureFile()
        val result = parser.parseFlowKt(content)

        assertTrue(result.isSuccess, "Parse failed: ${result.errorMessage}")
        val graph = result.graph!!

        val sourceNode = graph.rootNodes.first { it.name == "graphEditor-source" }
        val sourceOutbound = graph.connections.count { it.sourceNodeId == sourceNode.id }
        val sourceInbound = graph.connections.count { it.targetNodeId == sourceNode.id }

        assertEquals(5, sourceOutbound,
            "graphEditor-source should have 5 command outputs (got $sourceOutbound)")
        assertEquals(0, sourceInbound,
            "graphEditor-source should have no inbound connections (got $sourceInbound)")
    }

    @Test
    fun `graphEditor-sink has only state inputs`() {
        val content = loadArchitectureFile()
        val result = parser.parseFlowKt(content)

        assertTrue(result.isSuccess, "Parse failed: ${result.errorMessage}")
        val graph = result.graph!!

        val sinkNode = graph.rootNodes.first { it.name == "graphEditor-sink" }
        val sinkOutbound = graph.connections.count { it.sourceNodeId == sinkNode.id }
        val sinkInbound = graph.connections.count { it.targetNodeId == sinkNode.id }

        assertEquals(0, sinkOutbound,
            "graphEditor-sink should have no outbound connections (got $sinkOutbound)")
        assertTrue(sinkInbound >= 7,
            "graphEditor-sink should have at least 7 state inputs (got $sinkInbound)")
    }

    @Test
    fun `all workflow modules receive flowGraphModel from source`() {
        val content = loadArchitectureFile()
        val result = parser.parseFlowKt(content)

        assertTrue(result.isSuccess, "Parse failed: ${result.errorMessage}")
        val graph = result.graph!!

        val sourceNode = graph.rootNodes.first { it.name == "graphEditor-source" }
        val commandTargets = graph.connections
            .filter { it.sourceNodeId == sourceNode.id }
            .map { it.targetNodeId }
            .toSet()

        val modulesNeedingFlowGraph = listOf("flowGraph-compose", "flowGraph-persist",
            "flowGraph-execute", "flowGraph-generate")
        for (moduleName in modulesNeedingFlowGraph) {
            val node = graph.rootNodes.first { it.name == moduleName }
            assertTrue(node.id in commandTargets,
                "$moduleName should receive flowGraphModel from graphEditor-source")
        }
    }

    @Test
    fun `no cycles exist in the connection graph`() {
        val content = loadArchitectureFile()
        val result = parser.parseFlowKt(content)

        assertTrue(result.isSuccess, "Parse failed: ${result.errorMessage}")
        val graph = result.graph!!

        // Build adjacency from source node → target nodes
        val adjacency = mutableMapOf<String, MutableSet<String>>()
        for (conn in graph.connections) {
            adjacency.getOrPut(conn.sourceNodeId) { mutableSetOf() }.add(conn.targetNodeId)
        }

        // DFS cycle detection
        val visited = mutableSetOf<String>()
        val inStack = mutableSetOf<String>()

        fun hasCycle(nodeId: String): Boolean {
            if (nodeId in inStack) return true
            if (nodeId in visited) return false
            visited.add(nodeId)
            inStack.add(nodeId)
            for (neighbor in adjacency[nodeId] ?: emptySet()) {
                if (hasCycle(neighbor)) return true
            }
            inStack.remove(nodeId)
            return false
        }

        val nodeIds = graph.rootNodes.map { it.id }
        val cycleFound = nodeIds.any { hasCycle(it) }
        assertFalse(cycleFound, "Connection graph should be a DAG — no cycles")
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

/*
 * Architecture FlowKts Verification Test
 * Verifies the architecture.flow.kt meta-FlowGraph parses correctly
 * and matches MIGRATION.md structure
 * License: Apache 2.0
 */

package characterization

import io.codenode.flowgraphpersist.serialization.FlowKtParser
import io.codenode.flowgraphtypes.discovery.IPTypeDiscovery
import io.codenode.flowgraphtypes.registry.IPTypeRegistry
import io.codenode.grapheditor.util.resolveConnectionIPTypes
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
    fun `architecture flow kt captures portTypeNameHints for typealias IP types`() {
        val content = loadArchitectureFile()
        val result = parser.parseFlowKt(content)

        assertTrue(result.isSuccess, "Parse failed: ${result.errorMessage}")
        val graph = result.graph!!
        val hints = result.portTypeNameHints

        // Debug: print all hints and all ports
        println("=== portTypeNameHints (${hints.size} entries) ===")
        hints.forEach { (portId, typeName) ->
            println("  $portId -> $typeName")
        }
        println()

        println("=== All nodes and their output ports ===")
        for (node in graph.rootNodes) {
            println("  Node: ${node.name} (id=${node.id})")
            node.outputPorts.forEach { port ->
                println("    output: ${port.name} (id=${port.id}) dataType=${port.dataType.simpleName}")
            }
            node.inputPorts.forEach { port ->
                println("    input: ${port.name} (id=${port.id}) dataType=${port.dataType.simpleName}")
            }
        }
        println()

        println("=== Connections ===")
        graph.connections.forEach { conn ->
            println("  ${conn.sourceNodeId}:${conn.sourcePortId} -> ${conn.targetNodeId}:${conn.targetPortId} [ipTypeId=${conn.ipTypeId}]")
        }

        // Verify hints are populated for typealias types
        assertTrue(hints.isNotEmpty(), "portTypeNameHints should not be empty — typealias IP types should produce hints")

        // Check specific expected hints (typealias types resolve to Any::class → hint stored)
        // Data class types like IPTypeCommand resolve directly → no hint needed
        val expectedHints = mapOf(
            "flowgraph-types_ipTypeMetadata" to "IPTypeMetadata",
            "flowgraph-inspect_nodeDescriptors" to "NodeDescriptors",
            "grapheditor-source_flowGraphModel" to "FlowGraphModel"
        )
        for ((portId, expectedTypeName) in expectedHints) {
            assertEquals(expectedTypeName, hints[portId],
                "Expected hint '$expectedTypeName' for port '$portId', got '${hints[portId]}'")
        }
    }

    @Test
    fun `resolveConnectionIPTypes sets ipTypeId on all non-String connections`() {
        // Simulate the real graph editor flow: discover IP types → parse file → resolve connections
        val projectRoot = findProjectRoot()
        val discovery = IPTypeDiscovery(projectRoot)
        val ipTypeRegistry = IPTypeRegistry()
        val discovered = discovery.discoverAll()
        ipTypeRegistry.registerFromFilesystem(discovered) { meta ->
            discovery.resolveKClass(meta)
        }

        println("=== Registered IP Types (${ipTypeRegistry.getAllTypes().size}) ===")
        ipTypeRegistry.getAllTypes().forEach { ipType ->
            println("  ${ipType.id}: ${ipType.typeName} (payloadType=${ipType.payloadType.simpleName})")
        }

        val parser = FlowKtParser()
        parser.setTypeResolver { typeName ->
            ipTypeRegistry.getByTypeName(typeName)?.payloadType
        }
        val content = loadArchitectureFile()
        val result = parser.parseFlowKt(content)
        assertTrue(result.isSuccess, "Parse failed: ${result.errorMessage}")
        val graph = result.graph!!

        println("\n=== portTypeNameHints (${result.portTypeNameHints.size}) ===")
        result.portTypeNameHints.forEach { (k, v) -> println("  $k -> $v") }

        val resolved = resolveConnectionIPTypes(graph, ipTypeRegistry, result.portTypeNameHints)

        println("\n=== Resolved Connections ===")
        resolved.connections.forEach { conn ->
            val ipTypeName = conn.ipTypeId?.let { ipTypeRegistry.getById(it)?.typeName } ?: "NONE"
            println("  ${conn.sourcePortId} -> ${conn.targetPortId} ipTypeId=${conn.ipTypeId} (${ipTypeName})")
        }

        // 19 of 20 connections should have ipTypeId set
        // The one exception is serializedOutput (String::class per FR-004) which has no custom IP type
        val unresolved = resolved.connections.filter { it.ipTypeId == null }
        assertEquals(1, unresolved.size,
            "Only serializedOutput should be unresolved, but got: " +
            unresolved.map { "${it.sourcePortId} -> ${it.targetPortId}" })
        assertTrue(unresolved.single().sourcePortId.contains("serializedOutput"),
            "Unresolved connection should be serializedOutput")
    }

    private fun findProjectRoot(): File {
        val candidates = listOf(
            File(System.getProperty("user.dir")),
            File(System.getProperty("user.dir")).parentFile
        )
        return candidates.firstOrNull { File(it, "iptypes/src/commonMain/kotlin/io/codenode/iptypes").isDirectory }
            ?: fail("Could not find project root with iptypes directory")
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

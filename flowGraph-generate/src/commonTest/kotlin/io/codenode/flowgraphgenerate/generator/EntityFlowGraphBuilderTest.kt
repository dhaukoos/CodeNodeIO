/*
 * EntityFlowGraphBuilder Test
 * Tests for building FlowGraph instances for entity CRUD modules
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.generator

import kotlin.test.*

class EntityFlowGraphBuilderTest {

    private val builder = EntityFlowGraphBuilder()
    private val spec = EntityModuleSpec.fromIPType(
        ipTypeName = "GeoLocation",
        sourceIPTypeId = "ip_geolocation_test123",
        properties = listOf(
            EntityProperty("latitude", "Double", true),
            EntityProperty("longitude", "Double", true),
            EntityProperty("label", "String", true)
        )
    )

    @Test
    fun `buildFlowGraph creates graph with correct name`() {
        val flowGraph = builder.buildFlowGraph(spec)
        assertEquals("GeoLocations", flowGraph.name)
    }

    @Test
    fun `buildFlowGraph creates graph with 3 nodes`() {
        val flowGraph = builder.buildFlowGraph(spec)
        assertEquals(3, flowGraph.rootNodes.size)
    }

    @Test
    fun `buildFlowGraph creates CUD source node with correct name`() {
        val flowGraph = builder.buildFlowGraph(spec)
        val cudNode = flowGraph.rootNodes.find { it.name == "GeoLocationCUD" }
        assertNotNull(cudNode, "GeoLocationCUD node should exist")
    }

    @Test
    fun `buildFlowGraph creates CUD node with 0 inputs and 3 outputs`() {
        val flowGraph = builder.buildFlowGraph(spec)
        val cudNode = flowGraph.rootNodes.find { it.name == "GeoLocationCUD" }!!
        assertEquals(0, cudNode.inputPorts.size)
        assertEquals(3, cudNode.outputPorts.size)
    }

    @Test
    fun `buildFlowGraph creates CUD node with correct output port names`() {
        val flowGraph = builder.buildFlowGraph(spec)
        val cudNode = flowGraph.rootNodes.find { it.name == "GeoLocationCUD" }!!
        val outputNames = cudNode.outputPorts.map { it.name }
        assertEquals(listOf("save", "update", "remove"), outputNames)
    }

    @Test
    fun `buildFlowGraph creates Repository processor with correct name`() {
        val flowGraph = builder.buildFlowGraph(spec)
        val repoNode = flowGraph.rootNodes.find { it.name == "GeoLocationRepository" }
        assertNotNull(repoNode, "GeoLocationRepository node should exist")
    }

    @Test
    fun `buildFlowGraph creates Repository node with 3 inputs and 2 outputs`() {
        val flowGraph = builder.buildFlowGraph(spec)
        val repoNode = flowGraph.rootNodes.find { it.name == "GeoLocationRepository" }!!
        assertEquals(3, repoNode.inputPorts.size)
        assertEquals(2, repoNode.outputPorts.size)
    }

    @Test
    fun `buildFlowGraph creates Repository node with correct port names`() {
        val flowGraph = builder.buildFlowGraph(spec)
        val repoNode = flowGraph.rootNodes.find { it.name == "GeoLocationRepository" }!!
        val inputNames = repoNode.inputPorts.map { it.name }
        val outputNames = repoNode.outputPorts.map { it.name }
        assertEquals(listOf("save", "update", "remove"), inputNames)
        assertEquals(listOf("result", "error"), outputNames)
    }

    @Test
    fun `buildFlowGraph creates Display sink with correct name`() {
        val flowGraph = builder.buildFlowGraph(spec)
        val displayNode = flowGraph.rootNodes.find { it.name == "GeoLocationsDisplay" }
        assertNotNull(displayNode, "GeoLocationsDisplay node should exist")
    }

    @Test
    fun `buildFlowGraph creates Display node with 2 inputs and 0 outputs`() {
        val flowGraph = builder.buildFlowGraph(spec)
        val displayNode = flowGraph.rootNodes.find { it.name == "GeoLocationsDisplay" }!!
        assertEquals(2, displayNode.inputPorts.size)
        assertEquals(0, displayNode.outputPorts.size)
    }

    @Test
    fun `buildFlowGraph creates Display node with correct input port names`() {
        val flowGraph = builder.buildFlowGraph(spec)
        val displayNode = flowGraph.rootNodes.find { it.name == "GeoLocationsDisplay" }!!
        val inputNames = displayNode.inputPorts.map { it.name }
        assertEquals(listOf("result", "error"), inputNames)
    }

    @Test
    fun `buildFlowGraph creates 5 connections`() {
        val flowGraph = builder.buildFlowGraph(spec)
        assertEquals(5, flowGraph.connections.size)
    }

    @Test
    fun `buildFlowGraph has target platforms`() {
        val flowGraph = builder.buildFlowGraph(spec)
        assertTrue(flowGraph.targetPlatforms.contains(io.codenode.fbpdsl.model.FlowGraph.TargetPlatform.KMP_ANDROID))
        assertTrue(flowGraph.targetPlatforms.contains(io.codenode.fbpdsl.model.FlowGraph.TargetPlatform.KMP_IOS))
    }

    @Test
    fun `buildFlowGraph version is 1_0_0`() {
        val flowGraph = builder.buildFlowGraph(spec)
        assertEquals("1.0.0", flowGraph.version)
    }
}

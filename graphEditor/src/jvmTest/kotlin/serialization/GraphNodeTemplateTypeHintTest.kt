package serialization

import io.codenode.flowgraphpersist.serialization.FlowKtParser
import io.codenode.flowgraphpersist.serialization.GraphNodeTemplateSerializer
import io.codenode.flowgraphpersist.state.GraphNodeTemplateInstantiator
import io.codenode.fbpdsl.model.GraphNode
import java.io.File
import kotlin.test.*

class GraphNodeTemplateTypeHintTest {

    private val templateContent = """
/*
 * GraphNode Template: WeatherAtLocation
 * @GraphNodeTemplate
 * @TemplateName WeatherAtLocation
 * @InputPorts 1
 * @OutputPorts 2
 * @ChildNodes 3
 */

import io.codenode.fbpdsl.dsl.*
import io.codenode.fbpdsl.model.*

val graph = flowGraph("WeatherAtLocation", version = "1.0.0", description = "Grouped from 3 nodes") {
    val weatheratlocation = graphNode("WeatherAtLocation") {
        description = "Grouped from 3 nodes"
        position(600.0, 300.0)

        val child_httpfetcher = codeNode("HttpFetcher") {
            position(50.0, 50.0)
            input("coordinates", Coordinates::class)
            output("response", HttpResponse::class)
            config("_codeNodeClass", "io.codenode.weatherforecast.nodes.HttpFetcherCodeNode")
        }

        val child_jsonparser_1 = codeNode("JsonParser") {
            position(300.0, 50.0)
            input("response", HttpResponse::class)
            output("forecastData", ForecastData::class)
            config("_codeNodeClass", "io.codenode.weatherforecast.nodes.JsonParserCodeNode")
        }

        val child_datamapper_2 = codeNode("DataMapper") {
            position(550.0, 50.0)
            input("forecastData", ForecastData::class)
            output("displayList", ForecastDisplayList::class)
            output("chartData", ChartData::class)
            config("_codeNodeClass", "io.codenode.weatherforecast.nodes.DataMapperCodeNode")
        }

        internalConnection(child_httpfetcher, "response", child_jsonparser_1, "response") withType "ip_httpresponse"
        internalConnection(child_jsonparser_1, "forecastData", child_datamapper_2, "forecastData") withType "ip_forecastdata"

        portMapping("in_httpfetcher_coordinates", "child_httpfetcher", "coordinates")
        portMapping("out_datamapper_displayList", "child_datamapper_2", "displayList")
        portMapping("out_datamapper_chartData", "child_datamapper_2", "chartData")

        exposeInput("in_httpfetcher_coordinates", Any::class, downstream = "node_httpfetcher:httpfetcher_coordinates")
        exposeOutput("out_datamapper_displayList", Any::class, upstream = "node_datamapper:datamapper_displayList")
        exposeOutput("out_datamapper_chartData", Any::class, upstream = "node_datamapper:datamapper_chartData")
    }
}
    """.trimIndent()

    @Test
    fun `parser captures portTypeNameHints for unresolved types`() {
        val parser = FlowKtParser()
        val result = parser.parseFlowKt(templateContent)

        assertTrue(result.isSuccess, "Parse should succeed")

        // Child ports with custom types should have hints
        assertTrue(result.portTypeNameHints.isNotEmpty(), "Should have type name hints")
        assertTrue(result.portTypeNameHints.values.contains("Coordinates"), "Should have Coordinates hint")
        assertTrue(result.portTypeNameHints.values.contains("HttpResponse"), "Should have HttpResponse hint")
        assertTrue(result.portTypeNameHints.values.contains("ForecastData"), "Should have ForecastData hint")
        assertTrue(result.portTypeNameHints.values.contains("ForecastDisplayList"), "Should have ForecastDisplayList hint")
        assertTrue(result.portTypeNameHints.values.contains("ChartData"), "Should have ChartData hint")
    }

    @Test
    fun `loadTemplate enriches GraphNode with type hints in config`() {
        // Write template to temp file
        val tempFile = File.createTempFile("graphnode_template_", ".flow.kts")
        tempFile.deleteOnExit()
        tempFile.writeText(templateContent)

        val graphNode = GraphNodeTemplateSerializer.loadTemplate(tempFile)
        assertNotNull(graphNode, "GraphNode should be loaded")

        // Check type hints are stored in config
        val typeHints = graphNode.configuration.filter { it.key.startsWith("_portTypeHint_") }
        assertTrue(typeHints.isNotEmpty(), "Should have type hints in config")
        assertEquals("Coordinates", typeHints["_portTypeHint_in_httpfetcher_coordinates"])
        assertEquals("ForecastDisplayList", typeHints["_portTypeHint_out_datamapper_displayList"])
        assertEquals("ChartData", typeHints["_portTypeHint_out_datamapper_chartData"])
    }

    @Test
    fun `instantiated GraphNode preserves type hints keyed by port name`() {
        val tempFile = File.createTempFile("graphnode_template_", ".flow.kts")
        tempFile.deleteOnExit()
        tempFile.writeText(templateContent)

        val template = GraphNodeTemplateSerializer.loadTemplate(tempFile)
        assertNotNull(template)

        // Deep copy with new IDs (simulates instantiation)
        val instantiated = GraphNodeTemplateInstantiator.deepCopyWithNewIds(template)

        // Port IDs should be different from template
        assertNotEquals(template.id, instantiated.id)

        // But config hints keyed by port NAME should still be present
        val typeHints = instantiated.configuration.filter { it.key.startsWith("_portTypeHint_") }

        // Port names don't change, so hint keys should still match
        val exposedPortNames = (instantiated.inputPorts + instantiated.outputPorts).map { it.name }

        // Build the same map the UI would build
        val portTypeMap = mutableMapOf<String, String>()
        for (port in instantiated.inputPorts + instantiated.outputPorts) {
            instantiated.configuration["_portTypeHint_${port.name}"]?.let { typeName ->
                portTypeMap[port.id] = typeName
            }
        }

        // All exposed ports should have type hints resolved via port name lookup
        assertEquals(3, portTypeMap.size, "All 3 exposed ports should have type hints: $portTypeMap")
        assertTrue(portTypeMap.values.contains("Coordinates"), "Should contain Coordinates")
        assertTrue(portTypeMap.values.contains("ForecastDisplayList"), "Should contain ForecastDisplayList")
        assertTrue(portTypeMap.values.contains("ChartData"), "Should contain ChartData")
    }

    @Test
    fun `instantiated GraphNode has consistent port IDs between child nodes and internal connections`() {
        val tempFile = File.createTempFile("graphnode_template_", ".flow.kts")
        tempFile.deleteOnExit()
        tempFile.writeText(templateContent)

        val template = GraphNodeTemplateSerializer.loadTemplate(tempFile)
        assertNotNull(template)

        val instantiated = GraphNodeTemplateInstantiator.deepCopyWithNewIds(template)

        // Collect all actual port IDs from child nodes
        val childPortIds = mutableSetOf<String>()
        for (child in instantiated.childNodes) {
            for (port in child.inputPorts + child.outputPorts) {
                childPortIds.add(port.id)
            }
        }

        // Every connection source/target port ID must exist in child node ports
        for (conn in instantiated.internalConnections) {
            assertTrue(
                conn.sourcePortId in childPortIds,
                "Connection source port '${conn.sourcePortId}' not found in child ports: $childPortIds"
            )
            assertTrue(
                conn.targetPortId in childPortIds,
                "Connection target port '${conn.targetPortId}' not found in child ports: $childPortIds"
            )
        }

        // Should have 2 internal connections (HttpFetcher→JsonParser, JsonParser→DataMapper)
        assertEquals(2, instantiated.internalConnections.size, "Should have 2 internal connections")

        // Should have 3 child nodes
        assertEquals(3, instantiated.childNodes.size, "Should have 3 child nodes")
    }

    @Test
    fun `boundary colors resolvable via type hints for edge ports not on internal connections`() {
        val tempFile = File.createTempFile("graphnode_template_", ".flow.kts")
        tempFile.deleteOnExit()
        tempFile.writeText(templateContent)

        val template = GraphNodeTemplateSerializer.loadTemplate(tempFile)
        assertNotNull(template)

        val gn = GraphNodeTemplateInstantiator.deepCopyWithNewIds(template)

        // Edge ports (coordinates, displayList, chartData) are NOT on internal connections
        // but they should have _portTypeHint_ entries that can resolve to IPTypeRegistry types
        for (port in gn.inputPorts + gn.outputPorts) {
            val hint = gn.configuration["_portTypeHint_${port.name}"]
            assertNotNull(hint, "Port '${port.name}' should have a type hint in config")
        }

        // Verify the hints match expected type names
        assertEquals("Coordinates", gn.configuration["_portTypeHint_in_httpfetcher_coordinates"])
        assertEquals("ForecastDisplayList", gn.configuration["_portTypeHint_out_datamapper_displayList"])
        assertEquals("ChartData", gn.configuration["_portTypeHint_out_datamapper_chartData"])
    }
}

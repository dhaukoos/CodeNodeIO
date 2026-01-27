/*
 * Property Code Generation Integration Test
 * Tests that property changes are correctly reflected in generated code
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.integration

import io.codenode.fbpdsl.model.*
import io.codenode.kotlincompiler.generator.*
import kotlin.test.*

/**
 * TDD integration tests verifying that node property configurations
 * are correctly reflected in generated KMP code.
 *
 * These tests verify the full pipeline:
 * 1. Create a FlowGraph with CodeNodes that have property configurations
 * 2. Generate KMP code using KotlinCodeGenerator
 * 3. Verify the generated code includes configuration properties
 *
 * Note: These tests are designed to FAIL initially (TDD Red phase)
 * until ConfigAwareGenerator is implemented (T090).
 */
class PropertyCodeGenTest {

    private val generator = KotlinCodeGenerator()

    // ============================================
    // Basic Property Code Generation Tests
    // ============================================

    @Test
    fun `should generate code with simple string configuration properties`() {
        // Given a node with string configuration properties
        val node = CodeNode(
            id = "node_http_1",
            name = "HttpClient",
            codeNodeType = CodeNodeType.API_ENDPOINT,
            position = Node.Position(100.0, 100.0),
            inputPorts = listOf(
                PortFactory.input<String>("request", "node_http_1", required = true)
            ),
            outputPorts = listOf(
                PortFactory.output<String>("response", "node_http_1")
            ),
            configuration = mapOf(
                "baseUrl" to "https://api.example.com",
                "authHeader" to "Bearer token123"
            )
        )

        // When generating component code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then generated code should include configuration properties
        assertTrue(
            generatedCode.contains("baseUrl"),
            "Generated code should include baseUrl property"
        )
        assertTrue(
            generatedCode.contains("https://api.example.com"),
            "Generated code should include baseUrl value"
        )
        assertTrue(
            generatedCode.contains("authHeader"),
            "Generated code should include authHeader property"
        )
    }

    @Test
    fun `should generate code with numeric configuration properties`() {
        // Given a node with numeric configuration
        val node = CodeNode(
            id = "node_retry_1",
            name = "RetryHandler",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(100.0, 100.0),
            inputPorts = listOf(
                PortFactory.input<Any>("input", "node_retry_1")
            ),
            outputPorts = listOf(
                PortFactory.output<Any>("output", "node_retry_1")
            ),
            configuration = mapOf(
                "maxRetries" to "3",
                "retryDelayMs" to "1000",
                "backoffMultiplier" to "2.0"
            )
        )

        // When generating component code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then generated code should include numeric properties
        assertTrue(
            generatedCode.contains("maxRetries"),
            "Generated code should include maxRetries property"
        )
        assertTrue(
            generatedCode.contains("retryDelayMs"),
            "Generated code should include retryDelayMs property"
        )

        // Verify values are present (may be as String or converted to numeric)
        assertTrue(
            generatedCode.contains("3") || generatedCode.contains("\"3\""),
            "Generated code should include maxRetries value"
        )
    }

    @Test
    fun `should generate code with boolean configuration properties`() {
        // Given a node with boolean configuration
        val node = CodeNode(
            id = "node_cache_1",
            name = "CacheManager",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(100.0, 100.0),
            inputPorts = listOf(
                PortFactory.input<Any>("data", "node_cache_1")
            ),
            outputPorts = listOf(
                PortFactory.output<Any>("cached", "node_cache_1")
            ),
            configuration = mapOf(
                "enabled" to "true",
                "persistToDisk" to "false",
                "useCompression" to "true"
            )
        )

        // When generating component code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then generated code should include boolean properties
        assertTrue(
            generatedCode.contains("enabled"),
            "Generated code should include enabled property"
        )
        assertTrue(
            generatedCode.contains("persistToDisk"),
            "Generated code should include persistToDisk property"
        )
    }

    // ============================================
    // Configuration in Processing Logic Tests
    // ============================================

    @Test
    fun `should use configuration values in generated process function`() {
        // Given a validator node with validation rules configuration
        val node = CodeNode(
            id = "node_validator_1",
            name = "InputValidator",
            codeNodeType = CodeNodeType.VALIDATOR,
            position = Node.Position(100.0, 100.0),
            inputPorts = listOf(
                PortFactory.input<String>("input", "node_validator_1", required = true)
            ),
            outputPorts = listOf(
                PortFactory.output<String>("valid", "node_validator_1"),
                PortFactory.output<String>("invalid", "node_validator_1")
            ),
            configuration = mapOf(
                "minLength" to "5",
                "maxLength" to "100",
                "pattern" to "^[a-zA-Z0-9]+$"
            )
        )

        // When generating component code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then configuration should be accessible in generated code
        assertTrue(
            generatedCode.contains("minLength"),
            "Generated code should reference minLength"
        )
        assertTrue(
            generatedCode.contains("maxLength"),
            "Generated code should reference maxLength"
        )

        // And a processing function should exist (validate for validators)
        assertTrue(
            generatedCode.contains("fun validate") || generatedCode.contains("fun process"),
            "Generated code should include validate or process function"
        )
    }

    @Test
    fun `should generate typed configuration accessors`() {
        // Given a node with typed configuration
        val node = CodeNode(
            id = "node_transform_1",
            name = "DataTransformer",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(100.0, 100.0),
            inputPorts = listOf(
                PortFactory.input<Any>("input", "node_transform_1")
            ),
            outputPorts = listOf(
                PortFactory.output<Any>("output", "node_transform_1")
            ),
            configuration = mapOf(
                "timeout" to "30",
                "multiplier" to "1.5",
                "debug" to "true"
            )
        )

        // When generating component code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then should have properties (will verify correct types in implementation)
        assertTrue(generatedCode.contains("timeout"))
        assertTrue(generatedCode.contains("multiplier"))
        assertTrue(generatedCode.contains("debug"))
    }

    // ============================================
    // Full Pipeline Integration Tests
    // ============================================

    @Test
    fun `should generate complete project with configured nodes`() {
        // Given a flow graph with configured nodes
        val graph = createConfiguredFlowGraph()

        // When generating complete project
        val project = generator.generateProject(graph)

        // Then project should have expected files
        assertTrue(project.fileCount() >= 2, "Should generate at least 2 files")

        // And all files should be valid
        project.files.forEach { fileSpec ->
            val code = fileSpec.toString()
            assertTrue(code.isNotBlank(), "File ${fileSpec.name} should not be empty")
        }
    }

    @Test
    fun `should preserve configuration when generating flow orchestrator`() {
        // Given a flow graph with configured nodes
        val graph = createConfiguredFlowGraph()

        // When generating flow code
        val flowSpec = generator.generateFlowCode(graph)
        val flowCode = flowSpec.toString()

        // Then flow code should be generated
        assertTrue(flowCode.isNotBlank(), "Flow orchestrator should be generated")

        // And should reference the component classes
        assertTrue(
            flowCode.contains("DataSource") || flowCode.contains("dataSource"),
            "Flow orchestrator should reference DataSource"
        )
    }

    @Test
    fun `should handle empty configuration gracefully`() {
        // Given a node with no configuration
        val node = CodeNode(
            id = "node_simple_1",
            name = "SimpleNode",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(100.0, 100.0),
            inputPorts = listOf(
                PortFactory.input<Any>("input", "node_simple_1")
            ),
            outputPorts = listOf(
                PortFactory.output<Any>("output", "node_simple_1")
            ),
            configuration = emptyMap()
        )

        // When generating component code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then should generate valid code without configuration properties
        assertTrue(generatedCode.contains("class Simplenode") || generatedCode.contains("class SimpleNode"))
        assertTrue(generatedCode.contains("fun transform") || generatedCode.contains("fun process"))
    }

    @Test
    fun `should handle special characters in configuration values`() {
        // Given a node with special characters in config
        val node = CodeNode(
            id = "node_special_1",
            name = "SpecialConfig",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(100.0, 100.0),
            inputPorts = listOf(
                PortFactory.input<Any>("input", "node_special_1")
            ),
            outputPorts = listOf(
                PortFactory.output<Any>("output", "node_special_1")
            ),
            configuration = mapOf(
                "queryTemplate" to "SELECT * FROM users WHERE name = '\$name'",
                "regex" to "^[a-z]+\\d{3}$",
                "jsonTemplate" to "{\"key\": \"value\"}"
            )
        )

        // When generating component code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then should handle escaping properly
        assertTrue(
            generatedCode.contains("queryTemplate"),
            "Should include queryTemplate property"
        )
        // The actual escaping will be handled by KotlinPoet
    }

    // ============================================
    // Property Type Template Tests
    // ============================================

    @Test
    fun `should generate filter node with configuration-based predicate`() {
        // Given a filter node with filter rules
        val node = CodeNode(
            id = "node_filter_1",
            name = "DataFilter",
            codeNodeType = CodeNodeType.FILTER,
            position = Node.Position(100.0, 100.0),
            inputPorts = listOf(
                PortFactory.input<Any>("input", "node_filter_1")
            ),
            outputPorts = listOf(
                PortFactory.output<Any>("passed", "node_filter_1"),
                PortFactory.output<Any>("rejected", "node_filter_1")
            ),
            configuration = mapOf(
                "filterField" to "status",
                "filterValue" to "active",
                "filterOperator" to "equals"
            )
        )

        // When generating component code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then should include filter configuration
        assertTrue(generatedCode.contains("filterField"))
        assertTrue(generatedCode.contains("filterValue"))
    }

    @Test
    fun `should generate transformer node with transformation rules`() {
        // Given a transformer with mapping configuration
        val node = CodeNode(
            id = "node_mapper_1",
            name = "DataMapper",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(100.0, 100.0),
            inputPorts = listOf(
                PortFactory.input<Any>("input", "node_mapper_1")
            ),
            outputPorts = listOf(
                PortFactory.output<Any>("output", "node_mapper_1")
            ),
            configuration = mapOf(
                "sourceField" to "firstName",
                "targetField" to "name",
                "transformType" to "uppercase"
            )
        )

        // When generating component code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then should include transformation config
        assertTrue(generatedCode.contains("sourceField"))
        assertTrue(generatedCode.contains("targetField"))
        assertTrue(generatedCode.contains("transformType"))
    }

    // ============================================
    // Helper Functions
    // ============================================

    private fun createConfiguredFlowGraph(): FlowGraph {
        val dataSourceNode = CodeNode(
            id = "node_source_1",
            name = "DataSource",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 100.0),
            outputPorts = listOf(
                PortFactory.output<Any>("data", "node_source_1")
            ),
            configuration = mapOf(
                "pollInterval" to "5000",
                "maxItems" to "100"
            )
        )

        val processorNode = CodeNode(
            id = "node_processor_1",
            name = "DataProcessor",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(300.0, 100.0),
            inputPorts = listOf(
                PortFactory.input<Any>("input", "node_processor_1")
            ),
            outputPorts = listOf(
                PortFactory.output<Any>("output", "node_processor_1")
            ),
            configuration = mapOf(
                "format" to "json",
                "validate" to "true"
            )
        )

        val sinkNode = CodeNode(
            id = "node_sink_1",
            name = "DataSink",
            codeNodeType = CodeNodeType.SINK,
            position = Node.Position(500.0, 100.0),
            inputPorts = listOf(
                PortFactory.input<Any>("input", "node_sink_1")
            ),
            configuration = mapOf(
                "logLevel" to "INFO",
                "batchSize" to "10"
            )
        )

        val sourceToProcessor = Connection(
            id = "conn_1",
            sourceNodeId = dataSourceNode.id,
            sourcePortId = dataSourceNode.outputPorts.first().id,
            targetNodeId = processorNode.id,
            targetPortId = processorNode.inputPorts.first().id
        )

        val processorToSink = Connection(
            id = "conn_2",
            sourceNodeId = processorNode.id,
            sourcePortId = processorNode.outputPorts.first().id,
            targetNodeId = sinkNode.id,
            targetPortId = sinkNode.inputPorts.first().id
        )

        return FlowGraph(
            id = "graph_test_1",
            name = "ConfiguredTestGraph",
            version = "1.0.0",
            rootNodes = listOf(dataSourceNode, processorNode, sinkNode),
            connections = listOf(sourceToProcessor, processorToSink)
        )
    }
}

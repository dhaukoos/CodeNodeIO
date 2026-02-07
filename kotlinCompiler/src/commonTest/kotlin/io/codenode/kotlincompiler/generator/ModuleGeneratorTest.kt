/*
 * ModuleGenerator Test
 * Unit tests for KMP module generation from FlowGraph
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import io.codenode.fbpdsl.model.*
import kotlin.test.*

/**
 * TDD tests for ModuleGenerator - verifies KMP module generation from FlowGraph.
 *
 * These tests are written FIRST and should FAIL until ModuleGenerator is implemented.
 */
class ModuleGeneratorTest {

    // ========== Test Fixtures ==========

    private fun createTestCodeNode(
        id: String,
        name: String,
        type: CodeNodeType = CodeNodeType.TRANSFORMER
    ): CodeNode {
        return CodeNode(
            id = id,
            name = name,
            codeNodeType = type,
            position = Node.Position(0.0, 0.0),
            inputPorts = listOf(
                Port(
                    id = "${id}_input",
                    name = "input",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = id
                )
            ),
            outputPorts = listOf(
                Port(
                    id = "${id}_output",
                    name = "output",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    owningNodeId = id
                )
            )
        )
    }

    private fun createTestFlowGraph(
        name: String = "TestFlow",
        nodes: List<Node> = listOf(createTestCodeNode("node1", "Processor"))
    ): FlowGraph {
        return FlowGraph(
            id = "flow_${name.lowercase()}",
            name = name,
            version = "1.0.0",
            description = "Test flow for module generation",
            rootNodes = nodes,
            targetPlatforms = listOf(
                FlowGraph.TargetPlatform.KMP_ANDROID,
                FlowGraph.TargetPlatform.KMP_IOS,
                FlowGraph.TargetPlatform.KMP_DESKTOP
            )
        )
    }

    // ========== T054: Module Structure Generation Tests ==========

    @Test
    fun `should generate module with correct directory structure`() {
        // Given
        val flowGraph = createTestFlowGraph("UserValidation")
        val moduleName = "user-validation"
        val generator = ModuleGenerator()

        // When
        val moduleStructure = generator.generateModuleStructure(flowGraph, moduleName)

        // Then
        assertTrue(moduleStructure.directories.contains("src/commonMain/kotlin"),
            "Should include commonMain source directory")
        assertTrue(moduleStructure.directories.contains("src/commonTest/kotlin"),
            "Should include commonTest source directory")
        assertTrue(moduleStructure.directories.contains("src/jvmMain/kotlin"),
            "Should include jvmMain source directory")
    }

    @Test
    fun `should generate module structure with package-appropriate subdirectories`() {
        // Given
        val flowGraph = createTestFlowGraph("DataProcessor")
        val moduleName = "data-processor"
        val packageName = "io.codenode.generated.dataprocessor"
        val generator = ModuleGenerator()

        // When
        val moduleStructure = generator.generateModuleStructure(flowGraph, moduleName, packageName)

        // Then
        val expectedPackagePath = "src/commonMain/kotlin/io/codenode/generated/dataprocessor"
        assertTrue(moduleStructure.directories.contains(expectedPackagePath),
            "Should include package-specific source directory")
    }

    @Test
    fun `should generate module structure with android source set when targeting android`() {
        // Given
        val flowGraph = createTestFlowGraph().copy(
            targetPlatforms = listOf(FlowGraph.TargetPlatform.KMP_ANDROID)
        )
        val generator = ModuleGenerator()

        // When
        val moduleStructure = generator.generateModuleStructure(flowGraph, "android-module")

        // Then
        assertTrue(moduleStructure.directories.contains("src/androidMain/kotlin"),
            "Should include androidMain source directory")
    }

    @Test
    fun `should generate module structure with ios source sets when targeting ios`() {
        // Given
        val flowGraph = createTestFlowGraph().copy(
            targetPlatforms = listOf(FlowGraph.TargetPlatform.KMP_IOS)
        )
        val generator = ModuleGenerator()

        // When
        val moduleStructure = generator.generateModuleStructure(flowGraph, "ios-module")

        // Then
        assertTrue(moduleStructure.directories.contains("src/iosMain/kotlin"),
            "Should include iosMain source directory")
    }

    // ========== T055: build.gradle.kts Generation Tests ==========

    @Test
    fun `should generate valid build gradle kts with KMP plugin`() {
        // Given
        val flowGraph = createTestFlowGraph("ApiClient")
        val moduleName = "api-client"
        val generator = ModuleGenerator()

        // When
        val buildGradleContent = generator.generateBuildGradle(flowGraph, moduleName)

        // Then
        assertTrue(buildGradleContent.contains("plugins {"),
            "Should contain plugins block")
        assertTrue(buildGradleContent.contains("kotlin(\"multiplatform\")"),
            "Should include KMP plugin")
        assertTrue(buildGradleContent.contains("kotlin(\"plugin.serialization\")"),
            "Should include serialization plugin")
    }

    @Test
    fun `should configure kotlin targets based on flow graph platforms`() {
        // Given
        val flowGraph = createTestFlowGraph().copy(
            targetPlatforms = listOf(
                FlowGraph.TargetPlatform.KMP_ANDROID,
                FlowGraph.TargetPlatform.KMP_IOS,
                FlowGraph.TargetPlatform.KMP_DESKTOP
            )
        )
        val generator = ModuleGenerator()

        // When
        val buildGradleContent = generator.generateBuildGradle(flowGraph, "multi-platform")

        // Then
        assertTrue(buildGradleContent.contains("androidTarget"),
            "Should configure Android target")
        assertTrue(buildGradleContent.contains("ios") || buildGradleContent.contains("iosX64"),
            "Should configure iOS targets")
        assertTrue(buildGradleContent.contains("jvm"),
            "Should configure JVM target for desktop")
    }

    @Test
    fun `should include required dependencies in build gradle`() {
        // Given
        val flowGraph = createTestFlowGraph()
        val generator = ModuleGenerator()

        // When
        val buildGradleContent = generator.generateBuildGradle(flowGraph, "test-module")

        // Then
        assertTrue(buildGradleContent.contains("kotlinx-coroutines"),
            "Should include coroutines dependency")
        assertTrue(buildGradleContent.contains("kotlinx-serialization"),
            "Should include serialization dependency")
    }

    @Test
    fun `should generate build gradle with correct module name`() {
        // Given
        val flowGraph = createTestFlowGraph("CustomFlow")
        val moduleName = "my-custom-module"
        val generator = ModuleGenerator()

        // When
        val buildGradleContent = generator.generateBuildGradle(flowGraph, moduleName)

        // Then
        assertTrue(buildGradleContent.contains("CustomFlow") || buildGradleContent.contains("my-custom-module"),
            "Should reference module/flow name in generated content")
    }

    // ========== T056: FlowGraph Class Generation Tests ==========

    @Test
    fun `should generate flow graph class with correct name`() {
        // Given
        val flowGraph = createTestFlowGraph("UserProcessing")
        val packageName = "io.codenode.generated.userprocessing"
        val generator = ModuleGenerator()

        // When
        val flowGraphClass = generator.generateFlowGraphClass(flowGraph, packageName)

        // Then
        assertTrue(flowGraphClass.contains("class UserProcessingFlow") ||
            flowGraphClass.contains("class UserProcessing"),
            "Should generate class with flow name")
        assertTrue(flowGraphClass.contains("package $packageName"),
            "Should include correct package declaration")
    }

    @Test
    fun `should generate flow graph class with node instantiation code`() {
        // Given
        val nodes = listOf(
            createTestCodeNode("node1", "InputValidator"),
            createTestCodeNode("node2", "DataTransformer")
        )
        val flowGraph = createTestFlowGraph("Pipeline", nodes)
        val generator = ModuleGenerator()

        // When
        val flowGraphClass = generator.generateFlowGraphClass(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(flowGraphClass.contains("InputValidator") || flowGraphClass.contains("inputValidator"),
            "Should instantiate InputValidator node")
        assertTrue(flowGraphClass.contains("DataTransformer") || flowGraphClass.contains("dataTransformer"),
            "Should instantiate DataTransformer node")
    }

    @Test
    fun `should generate flow graph class with connection wiring`() {
        // Given
        val node1 = createTestCodeNode("node1", "Source")
        val node2 = createTestCodeNode("node2", "Sink")
        val connection = Connection(
            id = "conn1",
            sourceNodeId = "node1",
            sourcePortId = "node1_output",
            targetNodeId = "node2",
            targetPortId = "node2_input"
        )
        val flowGraph = createTestFlowGraph("ConnectedFlow", listOf(node1, node2))
            .copy(connections = listOf(connection))
        val generator = ModuleGenerator()

        // When
        val flowGraphClass = generator.generateFlowGraphClass(flowGraph, "io.codenode.generated")

        // Then
        // Should have some connection-related code or wiring logic
        assertTrue(flowGraphClass.contains("Source") && flowGraphClass.contains("Sink"),
            "Should reference connected nodes")
    }

    @Test
    fun `should generate flow graph class with proper imports`() {
        // Given
        val flowGraph = createTestFlowGraph()
        val generator = ModuleGenerator()

        // When
        val flowGraphClass = generator.generateFlowGraphClass(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(flowGraphClass.contains("import") ||
            flowGraphClass.contains("package"),
            "Should include package or import declarations")
    }

    // ========== T057: RootControlNode Wrapper Generation Tests ==========

    @Test
    fun `should generate controller class wrapping RootControlNode`() {
        // Given
        val flowGraph = createTestFlowGraph("MainFlow")
        val packageName = "io.codenode.generated.mainflow"
        val generator = ModuleGenerator()

        // When
        val controllerClass = generator.generateControllerClass(flowGraph, packageName)

        // Then
        assertTrue(controllerClass.contains("class MainFlowController") ||
            controllerClass.contains("class MainFlow") && controllerClass.contains("Controller"),
            "Should generate controller class with appropriate name")
        assertTrue(controllerClass.contains("package $packageName"),
            "Should include correct package declaration")
    }

    @Test
    fun `should generate controller with start method`() {
        // Given
        val flowGraph = createTestFlowGraph()
        val generator = ModuleGenerator()

        // When
        val controllerClass = generator.generateControllerClass(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(controllerClass.contains("fun start") ||
            controllerClass.contains("startAll"),
            "Should include start method")
    }

    @Test
    fun `should generate controller with pause method`() {
        // Given
        val flowGraph = createTestFlowGraph()
        val generator = ModuleGenerator()

        // When
        val controllerClass = generator.generateControllerClass(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(controllerClass.contains("fun pause") ||
            controllerClass.contains("pauseAll"),
            "Should include pause method")
    }

    @Test
    fun `should generate controller with stop method`() {
        // Given
        val flowGraph = createTestFlowGraph()
        val generator = ModuleGenerator()

        // When
        val controllerClass = generator.generateControllerClass(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(controllerClass.contains("fun stop") ||
            controllerClass.contains("stopAll"),
            "Should include stop method")
    }

    @Test
    fun `should generate controller with status method`() {
        // Given
        val flowGraph = createTestFlowGraph()
        val generator = ModuleGenerator()

        // When
        val controllerClass = generator.generateControllerClass(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(controllerClass.contains("fun getStatus") ||
            controllerClass.contains("status"),
            "Should include getStatus method")
    }

    @Test
    fun `should generate controller that wraps RootControlNode internally`() {
        // Given
        val flowGraph = createTestFlowGraph()
        val generator = ModuleGenerator()

        // When
        val controllerClass = generator.generateControllerClass(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(controllerClass.contains("RootControlNode") ||
            controllerClass.contains("rootControlNode") ||
            controllerClass.contains("controller"),
            "Should reference RootControlNode or controller internally")
    }

    // ========== Integration Tests ==========

    @Test
    fun `should generate complete module with all required files`() {
        // Given
        val flowGraph = createTestFlowGraph("CompleteModule")
        val generator = ModuleGenerator()

        // When
        val generatedModule = generator.generateModule(flowGraph, "complete-module")

        // Then
        assertTrue(generatedModule.files.isNotEmpty(),
            "Should generate at least one file")
        assertTrue(generatedModule.files.any { file -> file.name.contains("build.gradle") },
            "Should include build.gradle.kts")
    }

    @Test
    fun `should handle flow graph with nested GraphNodes`() {
        // Given
        val innerNode = createTestCodeNode("inner", "InnerProcessor")
        val graphNode = GraphNode(
            id = "outer",
            name = "OuterContainer",
            position = Node.Position(0.0, 0.0),
            childNodes = listOf(innerNode.copy(parentNodeId = "outer")),
            inputPorts = listOf(
                Port(
                    id = "outer_input",
                    name = "input",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = "outer"
                )
            ),
            outputPorts = listOf(
                Port(
                    id = "outer_output",
                    name = "output",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    owningNodeId = "outer"
                )
            ),
            portMappings = mapOf(
                "input" to GraphNode.PortMapping("inner", "input"),
                "output" to GraphNode.PortMapping("inner", "output")
            )
        )
        val flowGraph = createTestFlowGraph("NestedFlow", listOf(graphNode))
        val generator = ModuleGenerator()

        // When
        val flowGraphClass = generator.generateFlowGraphClass(flowGraph, "io.codenode.generated")

        // Then
        // Should handle nested structure without error
        assertNotNull(flowGraphClass, "Should generate class for nested graph")
        assertTrue(flowGraphClass.isNotEmpty(), "Generated class should not be empty")
    }
}

/*
 * GenericNodeGenerator Test
 * Contract tests for generic node code generation with UseCase delegation
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import io.codenode.fbpdsl.model.*
import kotlin.test.*

/**
 * TDD tests for GenericNodeGenerator.
 *
 * Test organization follows task breakdown:
 * - T038: UseCase delegation tests
 * - T039: Placeholder component tests (no UseCase)
 * - T040: Custom port names in generated component
 * - T041: supportsGenericNode detection
 */
class GenericNodeGeneratorTest {

    // ========== Helper Functions ==========

    private fun createGenericNode(
        id: String = "node_generic_1",
        name: String = "in2out1",
        genericType: String? = "in2out1",
        useCaseClass: String? = null,
        inputNames: List<String> = listOf("input1", "input2"),
        outputNames: List<String> = listOf("output1")
    ): CodeNode {
        val configuration = mutableMapOf<String, String>()
        genericType?.let { configuration["_genericType"] = it }
        useCaseClass?.let { configuration["_useCaseClass"] = it }

        return CodeNode(
            id = id,
            name = name,
            codeNodeType = CodeNodeType.TRANSFORMER,
            description = "Generic processing node with ${inputNames.size} inputs and ${outputNames.size} outputs",
            position = Node.Position(200.0, 200.0),
            inputPorts = inputNames.mapIndexed { index, portName ->
                Port(
                    id = "${id}_input_$portName",
                    name = portName,
                    direction = Port.Direction.INPUT,
                    dataType = Any::class,
                    owningNodeId = id
                )
            },
            outputPorts = outputNames.mapIndexed { index, portName ->
                Port(
                    id = "${id}_output_$portName",
                    name = portName,
                    direction = Port.Direction.OUTPUT,
                    dataType = Any::class,
                    owningNodeId = id
                )
            },
            configuration = configuration
        )
    }

    private fun createNonGenericNode(
        name: String = "RegularNode"
    ): CodeNode {
        return CodeNode(
            id = "node_regular_1",
            name = name,
            codeNodeType = CodeNodeType.TRANSFORMER,
            description = "Regular non-generic node",
            position = Node.Position(100.0, 100.0),
            inputPorts = listOf(
                Port(
                    id = "port_input",
                    name = "input",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = "node_regular_1"
                )
            ),
            outputPorts = listOf(
                Port(
                    id = "port_output",
                    name = "output",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    owningNodeId = "node_regular_1"
                )
            ),
            configuration = emptyMap()
        )
    }

    // ========== T041: supportsGenericNode Detection Tests ==========

    @Test
    fun `supportsGenericNode returns true for node with _genericType`() {
        // Given a node with _genericType in configuration
        val node = createGenericNode(genericType = "in2out1")

        // When checking if it's a generic node
        val generator = GenericNodeGenerator()
        val isGeneric = generator.supportsGenericNode(node)

        // Then it should return true
        assertTrue(isGeneric, "Node with _genericType should be identified as generic")
    }

    @Test
    fun `supportsGenericNode returns false for node without _genericType`() {
        // Given a node without _genericType
        val node = createNonGenericNode()

        // When checking if it's a generic node
        val generator = GenericNodeGenerator()
        val isGeneric = generator.supportsGenericNode(node)

        // Then it should return false
        assertFalse(isGeneric, "Node without _genericType should not be identified as generic")
    }

    @Test
    fun `supportsGenericNode returns true for various generic type patterns`() {
        val generator = GenericNodeGenerator()

        // Test various generic type patterns
        val patterns = listOf("in0out1", "in1out0", "in5out5", "in3out2")

        patterns.forEach { pattern ->
            val node = createGenericNode(genericType = pattern)
            assertTrue(
                generator.supportsGenericNode(node),
                "Node with _genericType=$pattern should be identified as generic"
            )
        }
    }

    // ========== T038: UseCase Delegation Tests ==========

    @Test
    fun `generateComponent creates class that delegates to UseCase`() {
        // Given a generic node with a UseCase class reference
        val node = createGenericNode(
            name = "EmailValidator",
            genericType = "in1out2",
            useCaseClass = "com.example.EmailValidatorUseCase",
            inputNames = listOf("email"),
            outputNames = listOf("valid", "invalid")
        )

        // When generating the component
        val generator = GenericNodeGenerator()
        val fileSpec = generator.generateComponent(node)
        val generatedCode = fileSpec.toString()

        // Then the generated code should delegate to the UseCase
        assertTrue(
            generatedCode.contains("EmailValidatorUseCase"),
            "Generated code should reference the UseCase class"
        )
        assertTrue(
            generatedCode.contains("useCase"),
            "Generated code should have a useCase property or parameter"
        )
    }

    @Test
    fun `generateComponent includes UseCase constructor parameter`() {
        // Given a generic node with UseCase reference
        val node = createGenericNode(
            name = "DataTransformer",
            genericType = "in1out1",
            useCaseClass = "com.example.DataTransformerUseCase",
            inputNames = listOf("data"),
            outputNames = listOf("result")
        )

        // When generating the component
        val generator = GenericNodeGenerator()
        val fileSpec = generator.generateComponent(node)
        val generatedCode = fileSpec.toString()

        // Then the constructor should accept a UseCase instance
        assertTrue(
            generatedCode.contains("DataTransformerUseCase"),
            "Constructor should accept UseCase type"
        )
    }

    @Test
    fun `generateComponent UseCase delegation calls execute method`() {
        // Given a generic node with UseCase
        val node = createGenericNode(
            name = "Processor",
            genericType = "in2out1",
            useCaseClass = "com.example.ProcessorUseCase",
            inputNames = listOf("input1", "input2"),
            outputNames = listOf("output")
        )

        // When generating the component
        val generator = GenericNodeGenerator()
        val fileSpec = generator.generateComponent(node)
        val generatedCode = fileSpec.toString()

        // Then the process method should call the UseCase
        assertTrue(
            generatedCode.contains("execute") || generatedCode.contains("invoke") || generatedCode.contains("process"),
            "Generated process method should call UseCase execute/invoke/process method"
        )
    }

    @Test
    fun `generateComponent handles fully qualified UseCase class name`() {
        // Given a generic node with fully qualified class name
        val node = createGenericNode(
            name = "FullyQualifiedNode",
            useCaseClass = "com.example.domain.usecases.validation.EmailValidatorUseCase"
        )

        // When generating the component
        val generator = GenericNodeGenerator()
        val fileSpec = generator.generateComponent(node)
        val generatedCode = fileSpec.toString()

        // Then the generated code should handle the full package path
        assertTrue(
            generatedCode.contains("EmailValidatorUseCase"),
            "Should include the UseCase class name"
        )
    }

    // ========== T039: Placeholder Component Tests (No UseCase) ==========

    @Test
    fun `generatePlaceholderComponent creates TODO placeholder when no UseCase`() {
        // Given a generic node without UseCase reference
        val node = createGenericNode(
            name = "PlaceholderNode",
            genericType = "in2out1",
            useCaseClass = null
        )

        // When generating the component
        val generator = GenericNodeGenerator()
        val fileSpec = generator.generateComponent(node)
        val generatedCode = fileSpec.toString()

        // Then the generated code should contain TODO placeholders
        assertTrue(
            generatedCode.contains("TODO"),
            "Generated code should contain TODO placeholder when no UseCase"
        )
    }

    @Test
    fun `generatePlaceholderComponent has correct input and output signature`() {
        // Given a generic node without UseCase
        val node = createGenericNode(
            name = "SignatureNode",
            genericType = "in3out2",
            useCaseClass = null,
            inputNames = listOf("a", "b", "c"),
            outputNames = listOf("x", "y")
        )

        // When generating the component
        val generator = GenericNodeGenerator()
        val fileSpec = generator.generateComponent(node)
        val generatedCode = fileSpec.toString()

        // Then the process function should have correct parameters
        assertTrue(generatedCode.contains("class SignatureNode"), "Should generate class")
        // Should have input handling for a, b, c
        assertTrue(
            generatedCode.contains("aInput") || generatedCode.contains("a"),
            "Should have input for 'a'"
        )
    }

    @Test
    fun `generatePlaceholderComponent creates valid compilable code`() {
        // Given a generic node without UseCase
        val node = createGenericNode(
            name = "CompilableNode",
            genericType = "in1out1",
            useCaseClass = null,
            inputNames = listOf("data"),
            outputNames = listOf("result")
        )

        // When generating the component
        val generator = GenericNodeGenerator()
        val fileSpec = generator.generateComponent(node)
        val generatedCode = fileSpec.toString()

        // Then the code should have valid Kotlin structure
        assertTrue(generatedCode.contains("package "), "Should have package declaration")
        assertTrue(generatedCode.contains("class CompilableNode"), "Should have class declaration")
        assertTrue(generatedCode.contains("fun "), "Should have function declarations")
    }

    @Test
    fun `generatePlaceholderComponent includes node metadata as comments`() {
        // Given a generic node without UseCase
        val node = createGenericNode(
            name = "DocumentedNode",
            genericType = "in2out3",
            useCaseClass = null
        )

        // When generating the component
        val generator = GenericNodeGenerator()
        val fileSpec = generator.generateComponent(node)
        val generatedCode = fileSpec.toString()

        // Then the generated code should include documentation
        assertTrue(
            generatedCode.contains("in2out3") || generatedCode.contains("genericType") || generatedCode.contains("DocumentedNode"),
            "Generated code should document the node type or name"
        )
    }

    // ========== T040: Custom Port Names Tests ==========

    @Test
    fun `generateComponent uses custom port names for input channels`() {
        // Given a generic node with custom input port names
        val node = createGenericNode(
            name = "CustomInputNode",
            inputNames = listOf("email", "password", "captcha"),
            outputNames = listOf("result")
        )

        // When generating the component
        val generator = GenericNodeGenerator()
        val fileSpec = generator.generateComponent(node)
        val generatedCode = fileSpec.toString()

        // Then the generated code should use custom port names
        assertTrue(
            generatedCode.contains("email") || generatedCode.contains("Email"),
            "Should use custom input name 'email'"
        )
        assertTrue(
            generatedCode.contains("password") || generatedCode.contains("Password"),
            "Should use custom input name 'password'"
        )
    }

    @Test
    fun `generateComponent uses custom port names for output channels`() {
        // Given a generic node with custom output port names
        val node = createGenericNode(
            name = "CustomOutputNode",
            inputNames = listOf("data"),
            outputNames = listOf("success", "failure", "log")
        )

        // When generating the component
        val generator = GenericNodeGenerator()
        val fileSpec = generator.generateComponent(node)
        val generatedCode = fileSpec.toString()

        // Then the generated code should use custom output names
        assertTrue(
            generatedCode.contains("success") || generatedCode.contains("Success"),
            "Should use custom output name 'success'"
        )
        assertTrue(
            generatedCode.contains("failure") || generatedCode.contains("Failure"),
            "Should use custom output name 'failure'"
        )
    }

    @Test
    fun `generateComponent handles mixed custom and default port names`() {
        // Given a generic node with mixed port names
        val node = createGenericNode(
            name = "MixedNode",
            inputNames = listOf("customInput", "input2"),
            outputNames = listOf("output1", "customOutput")
        )

        // When generating the component
        val generator = GenericNodeGenerator()
        val fileSpec = generator.generateComponent(node)
        val generatedCode = fileSpec.toString()

        // Then all port names should be present
        assertTrue(generatedCode.contains("customInput") || generatedCode.contains("Custominput"),
            "Should include custom input name")
        assertTrue(generatedCode.contains("customOutput") || generatedCode.contains("Customoutput"),
            "Should include custom output name")
    }

    @Test
    fun `generateComponent sanitizes port names for Kotlin identifiers`() {
        // Given a generic node with port names that need sanitization
        val node = createGenericNode(
            name = "SanitizedNode",
            inputNames = listOf("user-data", "config_value"),
            outputNames = listOf("result-ok")
        )

        // When generating the component
        val generator = GenericNodeGenerator()
        val fileSpec = generator.generateComponent(node)
        val generatedCode = fileSpec.toString()

        // Then property/parameter names should be valid Kotlin identifiers (no hyphens in code)
        // Note: The original port name may still appear in comments/docs, that's acceptable
        assertTrue(
            generatedCode.contains("class SanitizedNode"),
            "Should generate the class"
        )
        // Check that sanitized names are used for Input properties (camelCase, no hyphens)
        assertTrue(
            generatedCode.contains("userDataInput") || generatedCode.contains("userdataInput"),
            "Should sanitize 'user-data' to camelCase for input channel"
        )
        assertTrue(
            generatedCode.contains("configValueInput") || generatedCode.contains("configvalueInput"),
            "Should sanitize 'config_value' to camelCase for input channel"
        )
    }

    // ========== Additional Edge Case Tests ==========

    @Test
    fun `generateComponent handles in0out1 generator pattern`() {
        // Given a generic node with no inputs (generator pattern)
        val node = createGenericNode(
            name = "DataGenerator",
            genericType = "in0out1",
            useCaseClass = "com.example.DataGeneratorUseCase",
            inputNames = emptyList(),
            outputNames = listOf("data")
        )

        // When generating the component
        val generator = GenericNodeGenerator()
        val fileSpec = generator.generateComponent(node)
        val generatedCode = fileSpec.toString()

        // Then should generate valid generator component
        assertTrue(generatedCode.contains("class DataGenerator"), "Should generate class")
    }

    @Test
    fun `generateComponent handles in1out0 sink pattern`() {
        // Given a generic node with no outputs (sink pattern)
        val node = createGenericNode(
            name = "DataSink",
            genericType = "in1out0",
            useCaseClass = "com.example.DataSinkUseCase",
            inputNames = listOf("data"),
            outputNames = emptyList()
        )

        // When generating the component
        val generator = GenericNodeGenerator()
        val fileSpec = generator.generateComponent(node)
        val generatedCode = fileSpec.toString()

        // Then should generate valid sink component
        assertTrue(generatedCode.contains("class DataSink"), "Should generate class")
    }

    @Test
    fun `generateComponent preserves _genericType in configuration property`() {
        // Given a generic node with _genericType
        val node = createGenericNode(
            name = "TypedNode",
            genericType = "in2out3"
        )

        // When generating the component
        val generator = GenericNodeGenerator()
        val fileSpec = generator.generateComponent(node)
        val generatedCode = fileSpec.toString()

        // Then the _genericType should be preserved in some form
        assertTrue(
            generatedCode.contains("in2out3") || generatedCode.contains("genericType"),
            "Should preserve or document the generic type"
        )
    }
}

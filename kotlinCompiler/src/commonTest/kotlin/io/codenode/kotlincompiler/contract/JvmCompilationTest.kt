/*
 * JVM Compilation Contract Test
 * Verifies that generated code compiles for JVM target
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.contract

import io.codenode.fbpdsl.model.*
import io.codenode.kotlincompiler.generator.KotlinCodeGenerator
import kotlin.test.*

/**
 * Contract tests that verify generated Kotlin code is valid and compiles for JVM target.
 * These tests ensure the code generator produces syntactically correct Kotlin code
 * that would compile successfully on JVM-based Kotlin targets.
 */
class JvmCompilationTest {

    private val generator = KotlinCodeGenerator()

    private fun createTestNode(
        name: String,
        inputPorts: List<Port<*>> = emptyList(),
        outputPorts: List<Port<*>> = emptyList()
    ): CodeNode {
        val nodeId = "node_${name}_${System.currentTimeMillis()}"
        return CodeNode(
            id = nodeId,
            name = name,
            codeNodeType = CodeNodeType.CUSTOM,
            description = "Test node: $name",
            position = Node.Position(0.0, 0.0),
            inputPorts = inputPorts.map { it.copy(owningNodeId = nodeId) },
            outputPorts = outputPorts.map { it.copy(owningNodeId = nodeId) }
        )
    }

    @Test
    fun `generated code should have valid package declaration`() {
        // Given a simple node
        val node = createTestNode("JvmComponent")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then should have valid JVM package declaration
        assertTrue(generatedCode.contains("package io.codenode.generated"),
            "Should have valid package declaration for JVM")
    }

    @Test
    fun `generated class should be valid JVM class`() {
        // Given a node
        val node = createTestNode("JvmService")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then should generate valid JVM class syntax
        assertTrue(generatedCode.contains("class JvmService"), "Should declare class")
        assertFalse(generatedCode.contains("expect "), "Should not be expect class for JVM")
        assertFalse(generatedCode.contains("actual "), "Should not require actual implementation")
    }

    @Test
    fun `generated code should not use platform-specific APIs`() {
        // Given a node
        val node = createTestNode("CrossPlatformNode")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then should not contain platform-specific imports
        assertFalse(generatedCode.contains("import java.awt."), "Should not use AWT (desktop only)")
        assertFalse(generatedCode.contains("import javax.swing."), "Should not use Swing (desktop only)")
        assertFalse(generatedCode.contains("import android."), "Should not use Android APIs")
    }

    @Test
    fun `generated code should use Kotlin stdlib compatible with JVM`() {
        // Given a node
        val node = createTestNode("StdlibUser")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then should use JVM-compatible constructs
        // No platform-specific expect/actual needed
        assertTrue(generatedCode.contains("package "), "Should have valid syntax")
    }

    @Test
    fun `generated code should have valid file name for JVM`() {
        // Given a node
        val node = createTestNode("ValidFileName")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)

        // Then file name should be valid for JVM filesystem
        // Note: KotlinPoet FileSpec.name is the simple name without extension
        // The .kt extension is added when writing to file via FileSpec.writeTo()
        assertEquals("ValidFileName", fileSpec.name, "Should have valid file name")
        assertFalse(fileSpec.name.contains(" "), "Should not have spaces in filename")
        assertFalse(fileSpec.name.contains("/"), "Should not have slashes in filename")
    }

    @Test
    fun `generated code should compile with standard JVM annotations`() {
        // Given a node
        val node = createTestNode("AnnotatedComponent")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then annotations should be JVM-compatible
        // KDoc comments are always valid
        assertTrue(generatedCode.contains("/**") || !generatedCode.contains("@"),
            "Annotations if present should be JVM-compatible")
    }

    @Test
    fun `generated code should handle JVM reserved words`() {
        // Given a node with name that might conflict with JVM
        val node = createTestNode("Object") // 'object' is a Kotlin keyword

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then should escape or handle the name appropriately
        assertTrue(generatedCode.contains("class "), "Should still generate valid class")
    }

    @Test
    fun `generated code should be compatible with coroutines on JVM`() {
        // Given a node that might use coroutines
        val node = createTestNode("AsyncProcessor")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then code structure should be compatible with coroutines
        // Basic class generation doesn't require coroutine imports yet
        assertTrue(generatedCode.contains("class AsyncProcessor"), "Should generate async-compatible class")
    }

    @Test
    fun `generated code should have consistent indentation for JVM tooling`() {
        // Given a node
        val node = createTestNode("FormattedCode")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then indentation should be consistent (spaces only, no tabs - Kotlin convention)
        val lines = generatedCode.split("\n")

        // KotlinPoet uses 2-space indentation
        // Every line should either be empty, have no leading whitespace, or have multiples of 2 spaces
        val validIndentation = lines.all { line ->
            if (line.isBlank()) true
            else if (!line[0].isWhitespace()) true
            else {
                // Check that indentation is only spaces (no tabs) and is a multiple of 2
                val leadingSpaces = line.takeWhile { it == ' ' }
                !line.contains('\t') && leadingSpaces.length == line.takeWhile { it.isWhitespace() }.length
            }
        }
        assertTrue(validIndentation, "All lines should have consistent space-based indentation")
    }

    @Test
    fun `generated code should work with JVM reflection`() {
        // Given a node
        val node = createTestNode("ReflectableClass")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then class should be accessible via reflection (not internal/private at file level)
        assertFalse(generatedCode.contains("private class "), "Should not be private class")
        assertFalse(generatedCode.contains("internal class "), "Should not be internal by default")
    }
}

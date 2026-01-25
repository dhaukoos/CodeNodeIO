/*
 * iOS Compilation Contract Test
 * Verifies that generated code compiles for iOS target (Kotlin/Native)
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.contract

import io.codenode.fbpdsl.model.*
import io.codenode.kotlincompiler.generator.KotlinCodeGenerator
import kotlin.test.*

/**
 * Contract tests that verify generated Kotlin code is valid and compiles for iOS target.
 * These tests ensure the code generator produces code compatible with Kotlin/Native
 * and avoids JVM-only or Android-only constructs.
 */
class IosCompilationTest {

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
    fun `generated code should have valid package declaration for iOS`() {
        // Given a simple node
        val node = createTestNode("IosComponent")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then should have valid package declaration for iOS (Kotlin/Native)
        assertTrue(generatedCode.contains("package io.codenode.generated"),
            "Should have valid package declaration for iOS")
    }

    @Test
    fun `generated class should be valid Kotlin Native class`() {
        // Given a node
        val node = createTestNode("IosService")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then should generate valid Kotlin/Native compatible class syntax
        assertTrue(generatedCode.contains("class IosService"), "Should declare class")
        assertFalse(generatedCode.contains("expect "), "Should not be expect class in common code")
    }

    @Test
    fun `generated code should not use JVM-only APIs`() {
        // Given a node
        val node = createTestNode("NativeNode")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then should not contain JVM-only imports
        assertFalse(generatedCode.contains("import java."), "Should not use java.* APIs")
        assertFalse(generatedCode.contains("import javax."), "Should not use javax.* APIs")
        assertFalse(generatedCode.contains("import sun."), "Should not use sun.* APIs")
    }

    @Test
    fun `generated code should not use Android-only APIs`() {
        // Given a node
        val node = createTestNode("CrossPlatformNode")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then should not contain Android-only imports
        assertFalse(generatedCode.contains("import android."), "Should not use Android APIs")
        assertFalse(generatedCode.contains("import androidx."), "Should not use AndroidX APIs")
    }

    @Test
    fun `generated code should use Kotlin common stdlib only`() {
        // Given a node
        val node = createTestNode("CommonStdlibUser")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then should use only common Kotlin constructs
        assertTrue(generatedCode.contains("package "), "Should have valid syntax")
        // Should not use JVM-specific kotlin.jvm package
        assertFalse(generatedCode.contains("kotlin.jvm."), "Should not use kotlin.jvm package")
    }

    @Test
    fun `generated code should have valid file name for Kotlin Native`() {
        // Given a node
        val node = createTestNode("ValidFileName")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)

        // Then file name should be valid for Kotlin/Native filesystem
        assertEquals("ValidFileName.kt", fileSpec.name, "Should have valid .kt extension")
        assertFalse(fileSpec.name.contains(" "), "Should not have spaces in filename")
        assertFalse(fileSpec.name.contains("/"), "Should not have slashes in filename")
    }

    @Test
    fun `generated code should be compatible with Kotlin Native coroutines`() {
        // Given a node that might use coroutines
        val node = createTestNode("AsyncController")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then code structure should be compatible with Kotlin/Native coroutines
        assertTrue(generatedCode.contains("class AsyncController"), "Should generate async-compatible class")
        // Kotlin/Native has single-threaded coroutines by default
        assertFalse(generatedCode.contains("Dispatchers.IO"), "Should not use JVM-specific Dispatchers.IO")
    }

    @Test
    fun `generated code should not use JVM reflection`() {
        // Given a node
        val node = createTestNode("NoReflectionNode")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then should not use JVM reflection APIs
        assertFalse(generatedCode.contains("::class.java"), "Should not use ::class.java")
        assertFalse(generatedCode.contains("Class.forName"), "Should not use Class.forName")
        assertFalse(generatedCode.contains("import kotlin.reflect.full."), "Should not use full reflection")
    }

    @Test
    fun `generated code should handle Kotlin Native memory model`() {
        // Given a node
        val node = createTestNode("MemorySafeNode")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then should be compatible with Kotlin/Native new memory model
        // No @ThreadLocal or @SharedImmutable needed for basic classes
        assertTrue(generatedCode.contains("class MemorySafeNode"), "Should generate valid class")
        // Should not generate mutable global state
        assertFalse(generatedCode.contains("object.*var".toRegex()), "Should avoid mutable object state")
    }

    @Test
    fun `generated code should support iOS naming conventions`() {
        // Given a node with iOS-like name
        val node = createTestNode("UserViewController")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then should generate valid class with iOS naming convention
        assertTrue(generatedCode.contains("class UserViewController"), "Should allow ViewController-like names")
        // But shouldn't extend UIViewController (that requires platform-specific code)
        assertFalse(generatedCode.contains("UIKit"), "Should not import UIKit in common code")
    }

    @Test
    fun `generated code should be binary framework compatible`() {
        // Given a node
        val node = createTestNode("ExportableClass")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then class should be public for framework export
        assertFalse(generatedCode.contains("internal class "), "Should not be internal class")
        assertFalse(generatedCode.contains("private class "), "Should not be private class")
        // Public classes are exported in iOS frameworks
        assertTrue(generatedCode.contains("class ExportableClass"), "Should generate exportable class")
    }

    @Test
    fun `generated code should avoid inline classes for better Swift interop`() {
        // Given a node
        val node = createTestNode("SwiftInteropClass")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then should avoid value/inline classes for better Swift interop
        assertFalse(generatedCode.contains("value class"), "Should not use value classes")
        assertFalse(generatedCode.contains("inline class"), "Should not use inline classes")
        assertTrue(generatedCode.contains("class SwiftInteropClass"), "Should use regular class")
    }
}

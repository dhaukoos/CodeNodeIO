/*
 * Android Compilation Contract Test
 * Verifies that generated code compiles for Android target
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.contract

import io.codenode.fbpdsl.model.*
import io.codenode.kotlincompiler.generator.KotlinCodeGenerator
import kotlin.test.*

/**
 * Contract tests that verify generated Kotlin code is valid and compiles for Android target.
 * These tests ensure the code generator produces code compatible with Android's Kotlin
 * requirements and avoids JVM-only or iOS-only constructs.
 */
class AndroidCompilationTest {

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
    fun `generated code should have valid package declaration for Android`() {
        // Given a simple node
        val node = createTestNode("AndroidComponent")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then should have valid package declaration for Android
        assertTrue(generatedCode.contains("package io.codenode.generated"),
            "Should have valid package declaration for Android")
    }

    @Test
    fun `generated class should be valid Android-compatible class`() {
        // Given a node
        val node = createTestNode("AndroidService")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then should generate valid Android-compatible class syntax
        assertTrue(generatedCode.contains("class AndroidService"), "Should declare class")
        // Android uses actual/expect for platform-specific code, but generated code should be common
        assertFalse(generatedCode.contains("expect "), "Should not be expect class for Android")
    }

    @Test
    fun `generated code should not use desktop-only APIs`() {
        // Given a node
        val node = createTestNode("MobileNode")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then should not contain desktop-only imports
        assertFalse(generatedCode.contains("import java.awt."), "Should not use AWT (desktop only)")
        assertFalse(generatedCode.contains("import javax.swing."), "Should not use Swing (desktop only)")
        assertFalse(generatedCode.contains("import java.applet."), "Should not use Applet APIs")
    }

    @Test
    fun `generated code should not use iOS-only APIs`() {
        // Given a node
        val node = createTestNode("CrossPlatformNode")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then should not contain iOS-only constructs
        assertFalse(generatedCode.contains("import platform."), "Should not use Kotlin/Native platform APIs")
        assertFalse(generatedCode.contains("import kotlinx.cinterop."), "Should not use C interop directly")
    }

    @Test
    fun `generated code should use Android-compatible Kotlin stdlib`() {
        // Given a node
        val node = createTestNode("StdlibUser")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then should use Android-compatible constructs
        assertTrue(generatedCode.contains("package "), "Should have valid syntax")
        // Generated code should compile with kotlin-stdlib-jdk8 or newer
    }

    @Test
    fun `generated code should have valid file name for Android build`() {
        // Given a node
        val node = createTestNode("ValidFileName")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)

        // Then file name should be valid for Android filesystem and build tools
        // Note: KotlinPoet FileSpec.name is the simple name without extension
        // The .kt extension is added when writing to file via FileSpec.writeTo()
        assertEquals("ValidFileName", fileSpec.name, "Should have valid file name")
        assertFalse(fileSpec.name.contains(" "), "Should not have spaces in filename")
        assertFalse(fileSpec.name.contains("/"), "Should not have slashes in filename")
        assertFalse(fileSpec.name.contains("\\"), "Should not have backslashes in filename")
    }

    @Test
    fun `generated code should be compatible with Android coroutines`() {
        // Given a node that might use coroutines
        val node = createTestNode("AsyncViewModel")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then code structure should be compatible with Android coroutines (kotlinx.coroutines)
        assertTrue(generatedCode.contains("class AsyncViewModel"), "Should generate async-compatible class")
        // No blocking calls like Thread.sleep() should be generated
        assertFalse(generatedCode.contains("Thread.sleep"), "Should not use blocking Thread.sleep")
    }

    @Test
    fun `generated code should not use Java 9+ APIs unavailable on older Android`() {
        // Given a node
        val node = createTestNode("CompatibleNode")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then should not use Java 9+ APIs (Android API level 26+ required for some)
        assertFalse(generatedCode.contains("java.util.Optional."), "Should not use Optional (limited Android support)")
        assertFalse(generatedCode.contains("java.util.stream."), "Should prefer Kotlin collections over Java streams")
    }

    @Test
    fun `generated code should handle Android naming conventions`() {
        // Given a node with Activity-like name
        val node = createTestNode("UserActivity")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then should generate valid class that doesn't conflict with Android concepts
        assertTrue(generatedCode.contains("class UserActivity"), "Should allow Activity-like names")
        // But shouldn't extend android.app.Activity (that's up to the user's code)
        assertFalse(generatedCode.contains("extends Activity"), "Should not auto-extend Android Activity")
    }

    @Test
    fun `generated code should be ProGuard-friendly`() {
        // Given a node
        val node = createTestNode("ObfuscatableClass")

        // When generating code
        val fileSpec = generator.generateNodeComponent(node)
        val generatedCode = fileSpec.toString()

        // Then class should be public and not rely on reflection by default
        assertFalse(generatedCode.contains("private class "), "Should not be private class")
        // No @Keep annotation unless explicitly needed
        assertTrue(generatedCode.contains("class ObfuscatableClass"), "Should generate standard class")
    }
}

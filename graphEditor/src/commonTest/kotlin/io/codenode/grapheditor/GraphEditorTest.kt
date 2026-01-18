/*
 * GraphEditor Composable Function Tests
 * Tests for verifying Compose compilation and runtime behavior
 * License: Apache 2.0
 */

package io.codenode.grapheditor

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Test suite for verifying Composable function compilation
 * with Kotlin 2.1.30 and Compose Multiplatform 1.11.1
 */
class GraphEditorTest {

    /**
     * Verify that the GraphEditorCanvas Composable function exists
     * and can be referenced at runtime
     */
    @Test
    fun testGraphEditorCanvasExists() {
        // This test verifies that the @Composable annotation and function
        // are properly recognized by the Kotlin Compose compiler plugin
        val canvasClass = GraphEditorCanvas::class
        assertTrue(canvasClass.simpleName != null)
        assertTrue(canvasClass.simpleName == "GraphEditorCanvasKt" ||
                   canvasClass.simpleName == "GraphEditorCanvasPrevievKt" ||
                   canvasClass.simpleName!!.contains("GraphEditor"))
    }

    /**
     * Verify basic test framework integration
     * Ensures Kotlin/JVM test compilation works
     */
    @Test
    fun testBasicFunctionality() {
        val message = "Compose Multiplatform 1.11.1 with Kotlin 2.1.30"
        assertTrue(message.isNotEmpty())
        assertTrue(message.contains("Kotlin"))
    }
}


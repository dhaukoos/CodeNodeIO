# Quickstart: StopWatch Virtual Circuit Refactor

**Feature**: 011-stopwatch-refactor
**Date**: 2026-02-12

## Prerequisites

- Kotlin 2.1.21
- Gradle 8.x
- Existing codebase with graphEditor and KMPMobileApp modules

## Implementation Steps

### Step 1: Create RequiredPropertyValidator

**File**: `graphEditor/src/jvmMain/kotlin/compilation/RequiredPropertyValidator.kt`

```kotlin
package io.codenode.grapheditor.compilation

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.FlowGraph

data class PropertyValidationError(
    val nodeId: String,
    val nodeName: String,
    val propertyName: String,
    val reason: String = "required for code generation"
)

data class PropertyValidationResult(
    val success: Boolean,
    val errors: List<PropertyValidationError> = emptyList()
) {
    fun toErrorMessage(): String {
        if (success) return ""
        return errors.groupBy { it.nodeId }.entries.joinToString("\n") { (_, nodeErrors) ->
            val nodeName = nodeErrors.first().nodeName
            val properties = nodeErrors.map { it.propertyName }.joinToString(", ")
            "Node '$nodeName' missing required properties: $properties"
        }
    }
}

class RequiredPropertyValidator {

    private val requiredSpecs = mapOf(
        CodeNodeType.GENERIC to setOf("_useCaseClass", "_genericType")
    )

    fun validate(flowGraph: FlowGraph): PropertyValidationResult {
        val errors = mutableListOf<PropertyValidationError>()

        flowGraph.getAllCodeNodes().forEach { node ->
            val required = requiredSpecs[node.codeNodeType] ?: emptySet()
            required.forEach { propertyName ->
                val value = node.configuration[propertyName]
                if (value.isNullOrBlank()) {
                    errors.add(PropertyValidationError(
                        nodeId = node.id,
                        nodeName = node.name,
                        propertyName = propertyName
                    ))
                }
            }
        }

        return PropertyValidationResult(
            success = errors.isEmpty(),
            errors = errors
        )
    }

    fun getRequiredProperties(nodeType: CodeNodeType): Set<String> {
        return requiredSpecs[nodeType] ?: emptySet()
    }
}
```

### Step 2: Integrate with CompilationService

**File**: `graphEditor/src/jvmMain/kotlin/compilation/CompilationService.kt`

**Modification**: Add property validation after FlowGraph.validate()

```kotlin
class CompilationService {

    private val moduleGenerator = ModuleGenerator()
    private val propertyValidator = RequiredPropertyValidator()  // ADD

    fun compileToModule(
        flowGraph: FlowGraph,
        outputDir: File,
        moduleName: String = flowGraph.name.lowercase().replace(" ", "-"),
        packageName: String = ModuleGenerator.DEFAULT_PACKAGE
    ): CompilationResult {
        return try {
            // Validate flow graph structure
            val validation = flowGraph.validate()
            if (!validation.success) {
                return CompilationResult(
                    success = false,
                    errorMessage = "Flow graph validation failed: ${validation.errors.joinToString(", ")}"
                )
            }

            // ADD: Validate required properties
            val propertyValidation = propertyValidator.validate(flowGraph)
            if (!propertyValidation.success) {
                return CompilationResult(
                    success = false,
                    errorMessage = "Required properties missing:\n${propertyValidation.toErrorMessage()}"
                )
            }

            // Generate module (existing code)
            val generatedModule = moduleGenerator.generateModule(flowGraph, moduleName, packageName)
            // ... rest unchanged
        }
    }
}
```

### Step 3: Enhance PropertiesPanel for Required Fields

**File**: `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt`

**Modification 1**: Show `_useCaseClass` for GENERIC nodes (around line 553)

```kotlin
// In PropertiesContent composable
// Replace:
val configProperties = state.properties.filterKeys { !it.startsWith("_") }

// With:
val configProperties = state.properties.filterKeys { key ->
    // Show _useCaseClass for GENERIC nodes, hide other internal properties
    if (state.isGenericNode && key == "_useCaseClass") true
    else !key.startsWith("_")
}
```

**Modification 2**: Add required indicator for `_useCaseClass` (new PropertyDefinition)

```kotlin
// When displaying _useCaseClass for GENERIC nodes
if (state.isGenericNode && state.properties.containsKey("_useCaseClass")) {
    PropertyEditorRow(
        definition = PropertyDefinition(
            name = "Use Case Class",
            type = PropertyType.STRING,
            required = true,
            description = "Fully qualified class implementing ProcessingLogic"
        ),
        value = state.properties["_useCaseClass"] ?: "",
        error = if ((state.properties["_useCaseClass"] ?: "").isBlank())
            "Use Case Class is required" else null,
        onValueChange = { onPropertyChange("_useCaseClass", it) }
    )
    Spacer(modifier = Modifier.height(8.dp))
}
```

### Step 4: Remove Redundant Code from KMPMobileApp

**File**: `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/App.kt`

**Before**:
```kotlin
import io.codenode.fbpdsl.model.*
import io.codenode.stopwatch.generated.StopWatchController

fun createStopWatchFlowGraph(): FlowGraph {
    // 85 lines of redundant FlowGraph definition
}

@Composable
fun MainContent() {
    val flowGraph = remember { createStopWatchFlowGraph() }
    val controller = remember(flowGraph) { StopWatchController(flowGraph) }
    // ...
}
```

**After**:
```kotlin
import io.codenode.stopwatch.stopWatchFlowGraph
import io.codenode.stopwatch.generated.StopWatchController

// DELETE: createStopWatchFlowGraph() function entirely

@Composable
fun MainContent() {
    val controller = remember { StopWatchController(stopWatchFlowGraph) }
    // ...
}
```

### Step 5: Write Tests

**File**: `graphEditor/src/jvmTest/kotlin/compilation/RequiredPropertyValidatorTest.kt`

```kotlin
package io.codenode.grapheditor.compilation

import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.Node
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RequiredPropertyValidatorTest {

    private val validator = RequiredPropertyValidator()

    @Test
    fun `validate returns success when all required properties present`() {
        val graph = flowGraph("Test", version = "1.0.0") {
            codeNode("Timer", nodeType = "GENERIC") {
                config("_useCaseClass", "com.example.Timer")
                config("_genericType", "in0out1")
            }
        }

        val result = validator.validate(graph)

        assertTrue(result.success)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate returns error for GENERIC node missing _useCaseClass`() {
        val node = CodeNode(
            id = "timer",
            name = "Timer",
            codeNodeType = CodeNodeType.GENERIC,
            position = Node.Position(0.0, 0.0),
            configuration = mapOf("_genericType" to "in0out1")
        )
        val graph = flowGraph("Test", version = "1.0.0") {}
            .withNodes(listOf(node))

        val result = validator.validate(graph)

        assertFalse(result.success)
        assertEquals(1, result.errors.size)
        assertEquals("_useCaseClass", result.errors[0].propertyName)
    }

    @Test
    fun `validate ignores non-GENERIC node types`() {
        val node = CodeNode(
            id = "api",
            name = "APICall",
            codeNodeType = CodeNodeType.API_ENDPOINT,
            position = Node.Position(0.0, 0.0),
            configuration = emptyMap()  // No properties
        )
        val graph = flowGraph("Test", version = "1.0.0") {}
            .withNodes(listOf(node))

        val result = validator.validate(graph)

        assertTrue(result.success)
    }
}
```

## Verification Steps

1. **Run tests**:
   ```bash
   ./gradlew :graphEditor:jvmTest
   ```

2. **Test compilation validation**:
   ```bash
   ./gradlew :graphEditor:run
   # Create GENERIC node, leave _useCaseClass empty, click Compile
   # Expected: Error message listing missing properties
   ```

3. **Test KMPMobileApp builds**:
   ```bash
   ./gradlew :KMPMobileApp:build
   ```

4. **Run mobile app**:
   ```bash
   ./gradlew :KMPMobileApp:run
   # Verify stopwatch still functions correctly
   ```

## Common Issues

### Issue: "Unresolved reference: stopWatchFlowGraph"

**Solution**: Ensure StopWatch module dependency is declared in KMPMobileApp:

```kotlin
// KMPMobileApp/build.gradle.kts
commonMain.dependencies {
    implementation(project(":StopWatch"))
}
```

### Issue: Validation passes but code generation fails

**Solution**: Check that `_useCaseClass` value is a valid fully qualified class name that exists in the classpath.

### Issue: Properties panel doesn't show _useCaseClass

**Solution**: Verify the node's `codeNodeType` is `GENERIC` and the `isGenericNode` check in PropertiesPanelState is working.

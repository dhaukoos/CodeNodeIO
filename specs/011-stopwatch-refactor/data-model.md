# Data Model: StopWatch Virtual Circuit Refactor

**Feature**: 011-stopwatch-refactor
**Date**: 2026-02-12

## Overview

This document defines the data structures for compile-time required property validation.

---

## Core Entities

### PropertyValidationError

Represents a single missing required property on a node.

```kotlin
data class PropertyValidationError(
    val nodeId: String,
    val nodeName: String,
    val propertyName: String,
    val reason: String = "required property not defined"
)
```

**Fields**:
| Field | Type | Description |
|-------|------|-------------|
| nodeId | String | Unique identifier of the node |
| nodeName | String | Human-readable name for error messages |
| propertyName | String | Name of the missing property |
| reason | String | Why this property is required |

**Example**:
```kotlin
PropertyValidationError(
    nodeId = "timer-emitter",
    nodeName = "TimerEmitter",
    propertyName = "_useCaseClass",
    reason = "required for code generation"
)
```

---

### PropertyValidationResult

Result of validating all nodes in a FlowGraph for required properties.

```kotlin
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
```

**Fields**:
| Field | Type | Description |
|-------|------|-------------|
| success | Boolean | True if all required properties are defined |
| errors | List<PropertyValidationError> | All validation errors found |

**Methods**:
| Method | Returns | Description |
|--------|---------|-------------|
| toErrorMessage() | String | Formatted error message for UI display |

---

### RequiredPropertySpec

Defines which properties are required for a node type.

```kotlin
data class RequiredPropertySpec(
    val nodeType: CodeNodeType,
    val requiredProperties: Set<String>
)
```

**Fields**:
| Field | Type | Description |
|-------|------|-------------|
| nodeType | CodeNodeType | The node type this spec applies to |
| requiredProperties | Set<String> | Property names that must be defined |

**Registry** (compile-time constant):
```kotlin
val REQUIRED_PROPERTY_SPECS = mapOf(
    CodeNodeType.GENERIC to setOf("_useCaseClass", "_genericType")
)
```

---

## Relationships

```
FlowGraph
    └── rootNodes: List<Node>
            └── CodeNode
                    ├── codeNodeType: CodeNodeType
                    └── configuration: Map<String, String>
                            └── validates against → RequiredPropertySpec

PropertyValidationResult
    └── errors: List<PropertyValidationError>
            └── references → CodeNode (by nodeId, nodeName)
```

---

## State Transitions

### Compilation State Machine

```
┌─────────────────┐
│  GRAPH_LOADED   │
└────────┬────────┘
         │ User clicks "Compile"
         ▼
┌─────────────────┐
│STRUCTURE_VALID? │─── No ──► [Show FlowGraph validation errors]
└────────┬────────┘
         │ Yes
         ▼
┌─────────────────┐
│PROPERTIES_VALID?│─── No ──► [Show PropertyValidationResult errors]
└────────┬────────┘
         │ Yes
         ▼
┌─────────────────┐
│  COMPILE_OK     │──► [Generate module, write files]
└─────────────────┘
```

---

## Validation Rules

### GENERIC Node Type

| Property | Required | Validation |
|----------|----------|------------|
| `_useCaseClass` | Yes | Non-blank string |
| `_genericType` | Yes | Non-blank string |
| `speedAttenuation` | No | Optional numeric string |

### Other Node Types

No required properties enforced by this feature. Existing validation in NodeTypeDefinition.configurationSchema applies.

---

## Integration with Existing Models

### CompilationResult (existing, extended)

```kotlin
data class CompilationResult(
    val success: Boolean,
    val outputPath: String? = null,
    val generatedModule: GeneratedModule? = null,
    val fileCount: Int = 0,
    val errorMessage: String? = null,
    // NEW: Distinguish validation type
    val validationType: ValidationType? = null
)

enum class ValidationType {
    STRUCTURE,    // FlowGraph.validate() failed
    PROPERTIES    // Property validation failed
}
```

---

## Example Scenarios

### Scenario 1: Valid Graph

**Input**: FlowGraph with GENERIC node having both `_useCaseClass` and `_genericType`

```kotlin
CodeNode(
    id = "timer",
    name = "TimerEmitter",
    codeNodeType = CodeNodeType.GENERIC,
    configuration = mapOf(
        "_useCaseClass" to "io.codenode.usecases.Timer",
        "_genericType" to "in0out2"
    )
)
```

**Output**: `PropertyValidationResult(success = true, errors = emptyList())`

### Scenario 2: Missing Required Property

**Input**: FlowGraph with GENERIC node missing `_useCaseClass`

```kotlin
CodeNode(
    id = "timer",
    name = "TimerEmitter",
    codeNodeType = CodeNodeType.GENERIC,
    configuration = mapOf(
        "_genericType" to "in0out2"
    )
)
```

**Output**:
```kotlin
PropertyValidationResult(
    success = false,
    errors = listOf(
        PropertyValidationError(
            nodeId = "timer",
            nodeName = "TimerEmitter",
            propertyName = "_useCaseClass",
            reason = "required for code generation"
        )
    )
)
```

**Error Message**: "Node 'TimerEmitter' missing required properties: _useCaseClass"

### Scenario 3: Multiple Nodes Missing Properties

**Input**: FlowGraph with two GENERIC nodes, both missing `_useCaseClass`

**Output**:
```kotlin
PropertyValidationResult(
    success = false,
    errors = listOf(
        PropertyValidationError("timer", "TimerEmitter", "_useCaseClass", ...),
        PropertyValidationError("display", "DisplayReceiver", "_useCaseClass", ...)
    )
)
```

**Error Message**:
```
Node 'TimerEmitter' missing required properties: _useCaseClass
Node 'DisplayReceiver' missing required properties: _useCaseClass
```

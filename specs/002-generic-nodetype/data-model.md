# Data Model: Generic NodeType Definition

**Feature Branch**: `002-generic-nodetype`
**Date**: 2026-01-28

## Overview

This document defines the data model extensions required to support Generic NodeType Definitions in the CodeNodeIO FBP system.

## Entities

### 1. NodeCategory (Extended Enum)

Extends the existing `NodeTypeDefinition.NodeCategory` enum with a new value.

```kotlin
enum class NodeCategory {
    UI_COMPONENT,
    SERVICE,
    TRANSFORMER,
    VALIDATOR,
    API_ENDPOINT,
    DATABASE,
    GENERIC          // NEW
}
```

**Purpose**: Categorizes node types for palette organization and determines default CodeNodeType mapping.

### 2. GenericNodeConfiguration (New Data Class)

Holds the configurable properties specific to generic nodes.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `numInputs` | Int | Yes | Number of input ports (0-5) |
| `numOutputs` | Int | Yes | Number of output ports (0-5) |
| `customName` | String? | No | User-defined display name, overrides default |
| `iconResource` | String? | No | Path to custom icon/image resource |
| `inputNames` | List<String> | No | Custom names for input ports |
| `outputNames` | List<String> | No | Custom names for output ports |
| `useCaseClassName` | String? | No | Fully-qualified class name for processing logic |

**Constraints**:
- `numInputs` must be in range [0, 5]
- `numOutputs` must be in range [0, 5]
- `inputNames.size` must equal `numInputs` if provided
- `outputNames.size` must equal `numOutputs` if provided

**Validation Rules**:
```kotlin
fun validate(): ValidationResult {
    val errors = mutableListOf<String>()

    if (numInputs !in 0..5) {
        errors.add("numInputs must be between 0 and 5, was: $numInputs")
    }
    if (numOutputs !in 0..5) {
        errors.add("numOutputs must be between 0 and 5, was: $numOutputs")
    }
    if (inputNames != null && inputNames.size != numInputs) {
        errors.add("inputNames size (${inputNames.size}) must match numInputs ($numInputs)")
    }
    if (outputNames != null && outputNames.size != numOutputs) {
        errors.add("outputNames size (${outputNames.size}) must match numOutputs ($numOutputs)")
    }

    return ValidationResult(errors.isEmpty(), errors)
}
```

### 3. GenericNodeTypeFactory (New Factory Object)

Factory for creating NodeTypeDefinition instances for generic nodes.

**Primary Function Signature**:
```kotlin
fun createGenericNodeType(
    numInputs: Int,
    numOutputs: Int,
    customName: String? = null,
    customDescription: String? = null,
    iconResource: String? = null,
    useCaseClassName: String? = null,
    inputNames: List<String>? = null,
    outputNames: List<String>? = null
): NodeTypeDefinition
```

**Return Value Structure**:
- `id`: `"generic_in${numInputs}_out${numOutputs}"` (or custom if name provided)
- `name`: `"in${numInputs}out${numOutputs}"` (or custom if provided)
- `category`: `NodeCategory.GENERIC`
- `description`: Generated or custom description
- `portTemplates`: Generated based on input/output counts
- `defaultConfiguration`: Contains GenericNodeConfiguration as JSON
- `configurationSchema`: JSON Schema for validation
- `codeTemplates`: Templates for KMP code generation

### 4. PortTemplate Configuration

Port templates generated for generic nodes follow a consistent pattern.

**Input Ports**:
| Index | Default Name | Data Type | Required |
|-------|--------------|-----------|----------|
| 0 | `input1` | `Any` | `false` |
| 1 | `input2` | `Any` | `false` |
| ... | ... | ... | ... |
| n-1 | `input{n}` | `Any` | `false` |

**Output Ports**:
| Index | Default Name | Data Type | Required |
|-------|--------------|-----------|----------|
| 0 | `output1` | `Any` | `false` |
| 1 | `output2` | `Any` | `false` |
| ... | ... | ... | ... |
| n-1 | `output{n}` | `Any` | `false` |

**Data Type Decision**: Using `Any::class` for maximum flexibility. Type safety is enforced at connection time via port compatibility validation.

## Relationships

```
┌─────────────────────────────┐
│   GenericNodeConfiguration  │
│  (configuration data class) │
└──────────────┬──────────────┘
               │ serialized to
               ▼
┌─────────────────────────────┐
│     NodeTypeDefinition      │
│   (existing model class)    │
│                             │
│ - category: GENERIC         │
│ - defaultConfiguration:     │
│   {GenericNodeConfig JSON}  │
└──────────────┬──────────────┘
               │ creates (at drag-drop)
               ▼
┌─────────────────────────────┐
│         CodeNode            │
│   (existing node class)     │
│                             │
│ - type: TRANSFORMER         │
│ - configuration: {...}      │
│ - ports: [input1, output1]  │
└──────────────┬──────────────┘
               │ generates (at code gen)
               ▼
┌─────────────────────────────┐
│    Generated Component      │
│   (KMP Kotlin class)        │
└─────────────────────────────┘
```

## State Transitions

Generic nodes follow the standard node lifecycle:

```
[NodeTypeDefinition in Palette]
        │
        │ User drags to canvas
        ▼
[CodeNode created with ports]
        │
        │ User configures properties
        ▼
[CodeNode with custom config]
        │
        │ User saves graph
        ▼
[Serialized to .flow.kts]
        │
        │ User generates code
        ▼
[Component class generated]
```

## Serialization Format

Generic nodes serialize as standard CodeNodes with additional metadata.

**DSL Format Example** (in .flow.kts):
```kotlin
codeNode("validateEmail") {
    type = CodeNodeType.TRANSFORMER
    position(200f, 150f)

    // Generic node metadata
    metadata("_genericType", "in2out1")
    metadata("_useCaseClass", "com.example.validators.EmailValidator")
    metadata("_iconResource", "icons/email-check.svg")

    // Ports (possibly with custom names)
    inputPort("emailAddress", Any::class)
    inputPort("validationRules", Any::class)
    outputPort("validationResult", Any::class)

    // Additional configuration
    config("timeout", "5000")
}
```

**Metadata Key Convention**:
- Keys prefixed with `_` are reserved for generic node infrastructure
- `_genericType`: Original generic type (e.g., "in2out1")
- `_useCaseClass`: Fully-qualified UseCase class name
- `_iconResource`: Custom icon resource path

## Code Generation Output

**With UseCase Reference**:
```kotlin
/**
 * Generated component for: validateEmail
 * Based on generic type: in2out1
 * UseCase: com.example.validators.EmailValidator
 */
class ValidateEmailComponent(
    private val useCase: EmailValidator
) : FlowComponent {

    val emailAddress = Channel<Any>()
    val validationRules = Channel<Any>()
    val validationResult = Channel<Any>()

    override suspend fun process() {
        val input1 = emailAddress.receive()
        val input2 = validationRules.receive()
        val result = useCase.execute(input1, input2)
        validationResult.send(result)
    }
}
```

**Without UseCase Reference**:
```kotlin
/**
 * Generated component for: genericNode1
 * Based on generic type: in1out2
 * TODO: Implement processing logic
 */
class GenericNode1Component : FlowComponent {

    val input1 = Channel<Any>()
    val output1 = Channel<Any>()
    val output2 = Channel<Any>()

    override suspend fun process() {
        val input = input1.receive()
        // TODO: Implement processing logic
        // Route output to appropriate port:
        // output1.send(result1)
        // output2.send(result2)
        throw NotImplementedError("Processing logic not implemented for generic node")
    }
}
```

## Validation Rules Summary

| Rule | Validation Point | Error Message |
|------|------------------|---------------|
| Input count range | Factory creation | "numInputs must be between 0 and 5" |
| Output count range | Factory creation | "numOutputs must be between 0 and 5" |
| Input names count | Factory creation | "inputNames size must match numInputs" |
| Output names count | Factory creation | "outputNames size must match numOutputs" |
| UseCase class format | Code generation | "UseCase class must be fully-qualified" |
| Port name uniqueness | Inherited from NodeTypeDefinition | "Port templates must have unique names" |

## Migration

No migration required. Generic nodes are additive to the existing system:
- Existing graphs remain unchanged
- Existing NodeTypeDefinitions unaffected
- New GENERIC category automatically appears in palette
- No database schema changes (file-based storage)

# Contract: GenericNodeTypeFactory API

**Module**: `fbpDsl`
**Package**: `io.codenode.fbpdsl.factory`
**Version**: 1.0.0

## Overview

Defines the contract for the GenericNodeTypeFactory, which creates NodeTypeDefinition instances for generic nodes with configurable input/output counts.

## Factory Function

### `createGenericNodeType`

Creates a NodeTypeDefinition for a generic node with the specified configuration.

**Signature**:
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

**Parameters**:

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `numInputs` | `Int` | Yes | - | Number of input ports (0-5) |
| `numOutputs` | `Int` | Yes | - | Number of output ports (0-5) |
| `customName` | `String?` | No | `null` | Display name; defaults to "in{M}out{N}" |
| `customDescription` | `String?` | No | `null` | Node description; generated if null |
| `iconResource` | `String?` | No | `null` | Path to custom icon resource |
| `useCaseClassName` | `String?` | No | `null` | Fully-qualified UseCase class name |
| `inputNames` | `List<String>?` | No | `null` | Custom input port names |
| `outputNames` | `List<String>?` | No | `null` | Custom output port names |

**Returns**: `NodeTypeDefinition` with:
- `id`: `"generic_in{M}_out{N}"` or derived from customName
- `name`: customName or `"in{M}out{N}"`
- `category`: `NodeCategory.GENERIC`
- `description`: customDescription or auto-generated
- `portTemplates`: Generated input/output port templates
- `defaultConfiguration`: JSON-encoded configuration
- `codeTemplates`: KMP code generation template

**Throws**:
- `IllegalArgumentException` if `numInputs` not in [0, 5]
- `IllegalArgumentException` if `numOutputs` not in [0, 5]
- `IllegalArgumentException` if `inputNames` provided but size != `numInputs`
- `IllegalArgumentException` if `outputNames` provided but size != `numOutputs`

## Convenience Functions

### `getAllGenericNodeTypes`

Returns all 36 standard generic node type definitions.

**Signature**:
```kotlin
fun getAllGenericNodeTypes(): List<NodeTypeDefinition>
```

**Returns**: List of 36 NodeTypeDefinitions for all (0-5) × (0-5) combinations.

**Performance**: First call creates all instances; subsequent calls return cached list.

### `getCommonGenericNodeTypes`

Returns commonly-used generic node types for a simplified palette.

**Signature**:
```kotlin
fun getCommonGenericNodeTypes(): List<NodeTypeDefinition>
```

**Returns**: List containing:
- `in0out1` (Generator/Source)
- `in1out0` (Sink)
- `in1out1` (Simple Transformer)
- `in1out2` (Splitter)
- `in2out1` (Merger)

## Test Cases

### Creation Tests

| Test Case | Input | Expected Output |
|-----------|-------|-----------------|
| Valid minimum | `(0, 0)` | NodeTypeDefinition with 0 ports |
| Valid maximum | `(5, 5)` | NodeTypeDefinition with 10 ports |
| Valid mixed | `(2, 3)` | NodeTypeDefinition with 5 ports |
| Invalid negative inputs | `(-1, 0)` | `IllegalArgumentException` |
| Invalid negative outputs | `(0, -1)` | `IllegalArgumentException` |
| Invalid over-limit inputs | `(6, 0)` | `IllegalArgumentException` |
| Invalid over-limit outputs | `(0, 6)` | `IllegalArgumentException` |

### Naming Tests

| Test Case | Input | Expected `name` |
|-----------|-------|-----------------|
| Default name | `(1, 2)` | `"in1out2"` |
| Custom name | `(1, 2, customName="MyNode")` | `"MyNode"` |
| Zero inputs | `(0, 1)` | `"in0out1"` |
| Zero outputs | `(1, 0)` | `"in1out0"` |
| Both zero | `(0, 0)` | `"in0out0"` |

### Port Tests

| Test Case | Input | Expected Ports |
|-----------|-------|----------------|
| Single input | `(1, 0)` | `["input1"]` inputs, `[]` outputs |
| Single output | `(0, 1)` | `[]` inputs, `["output1"]` outputs |
| Multiple inputs | `(3, 0, inputNames=["a","b","c"])` | `["a","b","c"]` inputs |
| Custom output names | `(0, 2, outputNames=["result","error"])` | `["result","error"]` outputs |
| Mismatched names count | `(2, 0, inputNames=["only_one"])` | `IllegalArgumentException` |

### Configuration Tests

| Test Case | Verification |
|-----------|--------------|
| UseCase included | `defaultConfiguration` contains `useCaseClassName` |
| Icon included | `defaultConfiguration` contains `iconResource` |
| All props serialized | Configuration is valid JSON |
| Schema generated | `configurationSchema` is valid JSON Schema |

## Example Usage

```kotlin
// Basic usage
val transformer = createGenericNodeType(1, 1)
// name: "in1out1", category: GENERIC, 1 input port, 1 output port

// With custom names
val emailValidator = createGenericNodeType(
    numInputs = 2,
    numOutputs = 1,
    customName = "EmailValidator",
    customDescription = "Validates email addresses against rules",
    inputNames = listOf("email", "rules"),
    outputNames = listOf("isValid"),
    useCaseClassName = "com.example.EmailValidatorUseCase"
)

// Get all types for palette
val allTypes = getAllGenericNodeTypes()
assertEquals(36, allTypes.size)

// Get common types for quick access
val commonTypes = getCommonGenericNodeTypes()
assertEquals(5, commonTypes.size)
```

## Compatibility

- **Backward Compatible**: Yes - additive changes only
- **Breaking Changes**: None
- **Deprecations**: None

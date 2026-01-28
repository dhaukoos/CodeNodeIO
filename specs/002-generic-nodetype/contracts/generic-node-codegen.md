# Contract: Generic Node Code Generation

**Module**: `kotlinCompiler`
**Package**: `io.codenode.kotlincompiler.generator`
**Version**: 1.0.0

## Overview

Defines the contract for generating Kotlin Multiplatform code from CodeNodes created from generic NodeTypeDefinitions.

## Generator Interface

### `GenericNodeGenerator`

Generates KMP component classes from generic nodes.

**Signature**:
```kotlin
class GenericNodeGenerator {
    fun generateComponent(node: CodeNode): FileSpec
    fun generatePlaceholderComponent(node: CodeNode): FileSpec
    fun supportsGenericNode(node: CodeNode): Boolean
}
```

## Methods

### `generateComponent`

Generates a complete component class for a generic node with UseCase reference.

**Preconditions**:
- `node.configuration` contains `_useCaseClass` key
- UseCase class name is fully-qualified

**Postconditions**:
- Returns FileSpec with compilable Kotlin component
- Component delegates to UseCase for processing
- Input/output channels match node's port configuration

**Generated Code Pattern**:
```kotlin
package io.codenode.generated.components

import kotlinx.coroutines.channels.Channel
import {useCaseClass}

/**
 * Generated component: {nodeName}
 * Generic type: {genericType}
 * UseCase: {useCaseClass}
 */
class {NodeName}Component(
    private val useCase: {UseCaseClass}
) {
    // Input channels
    val {inputName1} = Channel<Any>(Channel.RENDEZVOUS)
    val {inputName2} = Channel<Any>(Channel.RENDEZVOUS)
    ...

    // Output channels
    val {outputName1} = Channel<Any>(Channel.RENDEZVOUS)
    ...

    suspend fun process() {
        // Receive inputs
        val input1 = {inputName1}.receive()
        val input2 = {inputName2}.receive()
        ...

        // Execute use case
        val result = useCase.execute(input1, input2, ...)

        // Send output
        {outputName1}.send(result)
    }
}
```

### `generatePlaceholderComponent`

Generates a placeholder component for nodes without UseCase reference.

**Preconditions**:
- `node.configuration` does NOT contain `_useCaseClass` key OR value is blank

**Postconditions**:
- Returns FileSpec with compilable Kotlin component
- Contains TODO comments for implementation
- Throws `NotImplementedError` in process method

**Generated Code Pattern**:
```kotlin
package io.codenode.generated.components

import kotlinx.coroutines.channels.Channel

/**
 * Generated placeholder component: {nodeName}
 * Generic type: {genericType}
 *
 * TODO: Either:
 * 1. Implement processing logic directly in this class
 * 2. Configure a UseCase class reference in the flow graph
 */
class {NodeName}Component {
    // Input channels
    val {inputName1} = Channel<Any>(Channel.RENDEZVOUS)
    ...

    // Output channels
    val {outputName1} = Channel<Any>(Channel.RENDEZVOUS)
    ...

    suspend fun process() {
        // Receive inputs
        val input1 = {inputName1}.receive()
        ...

        // TODO: Implement processing logic here
        // Example: val result = transform(input1)
        // {outputName1}.send(result)

        throw NotImplementedError(
            "Processing logic not implemented for node '{nodeName}'. " +
            "Configure a UseCase class or implement logic here."
        )
    }
}
```

### `supportsGenericNode`

Checks if a CodeNode originated from a generic NodeTypeDefinition.

**Detection Criteria**:
- `node.configuration` contains `_genericType` key
- OR `node.metadata` contains pattern matching "in[0-5]out[0-5]"

**Returns**: `true` if node is generic, `false` otherwise

## Test Cases

### Component Generation Tests

| Test Case | Node Config | Expected Output |
|-----------|-------------|-----------------|
| With UseCase (in1out1) | `{_useCaseClass: "com.ex.MyUseCase"}` | Component with UseCase delegation |
| Without UseCase (in1out1) | `{}` | Placeholder component with TODO |
| Multi-input (in3out1) | `{_useCaseClass: "..."}` | 3 input channels, 1 output |
| Multi-output (in1out3) | `{_useCaseClass: "..."}` | 1 input channel, 3 outputs |
| No inputs (in0out1) | `{_useCaseClass: "..."}` | Generator pattern (no receive) |
| No outputs (in1out0) | `{_useCaseClass: "..."}` | Sink pattern (no send) |
| Custom port names | `{_useCaseClass: "...", ports...}` | Uses custom port names |

### Validation Tests

| Test Case | Expected Behavior |
|-----------|-------------------|
| Invalid UseCase class format | Throws `CodeGenerationException` |
| Empty port names | Uses defaults (input1, output1) |
| Special characters in node name | Sanitized to valid Kotlin identifier |

### Compilation Contract Tests

| Test Case | Verification |
|-----------|--------------|
| Generated code compiles | `kotlinc` succeeds without errors |
| Component is instantiable | Can create instance at runtime |
| Channels are accessible | Public channel properties exist |
| Process is suspending | `process()` has suspend modifier |

## Error Handling

### `CodeGenerationException`

Thrown when code generation fails.

```kotlin
class CodeGenerationException(
    val node: CodeNode,
    val reason: String,
    cause: Throwable? = null
) : Exception("Failed to generate code for node '${node.name}': $reason", cause)
```

**Common Error Reasons**:
- `"UseCase class name is not fully-qualified"`
- `"UseCase class name contains invalid characters"`
- `"Node name cannot be converted to valid identifier"`

## Integration Points

### With KotlinCodeGenerator

The GenericNodeGenerator integrates with the main KotlinCodeGenerator:

```kotlin
// In KotlinCodeGenerator.generateProject()
for (node in flowGraph.getAllCodeNodes()) {
    val generator = when {
        genericNodeGenerator.supportsGenericNode(node) -> genericNodeGenerator
        // ... other node type checks
        else -> defaultGenerator
    }
    val fileSpec = generator.generateComponent(node)
    generatedFiles.add(fileSpec)
}
```

### With ComponentGenerator

Generic nodes use the same `ComponentGenerator` output format for consistency:
- Same package structure: `io.codenode.generated.components`
- Same channel patterns
- Same `process()` method signature

## Example Generated Files

### With UseCase (EmailValidator)

**Input**: CodeNode named "validateEmail" with `_useCaseClass = "com.example.EmailValidator"`, 2 inputs, 1 output

**Output** (`ValidateEmailComponent.kt`):
```kotlin
package io.codenode.generated.components

import kotlinx.coroutines.channels.Channel
import com.example.EmailValidator

class ValidateEmailComponent(
    private val useCase: EmailValidator
) {
    val email = Channel<Any>(Channel.RENDEZVOUS)
    val rules = Channel<Any>(Channel.RENDEZVOUS)
    val isValid = Channel<Any>(Channel.RENDEZVOUS)

    suspend fun process() {
        val emailValue = email.receive()
        val rulesValue = rules.receive()
        val result = useCase.execute(emailValue, rulesValue)
        isValid.send(result)
    }
}
```

### Without UseCase (Placeholder)

**Input**: CodeNode named "genericProcessor" with no UseCase, 1 input, 2 outputs

**Output** (`GenericProcessorComponent.kt`):
```kotlin
package io.codenode.generated.components

import kotlinx.coroutines.channels.Channel

/**
 * TODO: Implement processing logic or configure a UseCase class
 */
class GenericProcessorComponent {
    val input1 = Channel<Any>(Channel.RENDEZVOUS)
    val output1 = Channel<Any>(Channel.RENDEZVOUS)
    val output2 = Channel<Any>(Channel.RENDEZVOUS)

    suspend fun process() {
        val inputValue = input1.receive()

        // TODO: Implement your logic here
        // Route to appropriate output:
        // output1.send(result1)
        // output2.send(result2)

        throw NotImplementedError(
            "Processing logic not implemented for node 'genericProcessor'"
        )
    }
}
```

# Quickstart: Generic NodeType Definition

**Feature Branch**: `002-generic-nodetype`
**Date**: 2026-01-28

## Overview

This guide shows how to use Generic NodeType Definitions in CodeNodeIO to create flexible processing nodes with configurable inputs and outputs.

## Quick Start

### 1. Using Generic Nodes in the Visual Editor

1. Open the CodeNodeIO Graph Editor
2. In the Node Palette, expand the **Generic** category
3. Drag a generic node type (e.g., `in1out1`, `in2out1`) onto the canvas
4. Connect ports to other nodes as usual

### 2. Configuring Generic Nodes

1. Select the generic node on the canvas
2. Open the Properties Panel
3. Configure:
   - **Display Name**: Give the node a meaningful name (e.g., "EmailValidator")
   - **Port Names**: Rename ports to describe their purpose (e.g., "email", "result")
   - **UseCase Class**: Specify a fully-qualified class name for processing logic
   - **Icon** (optional): Select a custom icon

### 3. Generating Code

When you generate code from a flow graph containing generic nodes:

- **With UseCase**: Generated component delegates to your UseCase class
- **Without UseCase**: Generated component has placeholder TODO comments

## Examples

### Example 1: Simple Transformer (in1out1)

**Use Case**: Transform input data to output

```
┌─────────────────┐
│   in1out1       │
│ ┌──────────────┐│
│ │ input1    ●──┼┼──→ output
│ └──────────────┘│
│ ┌──────────────┐│
│ │ output1   ●──┼┼──→ input
│ └──────────────┘│
└─────────────────┘
```

**Configuration**:
- Display Name: `UppercaseText`
- Input Port: `text`
- Output Port: `uppercased`
- UseCase: `com.example.UppercaseUseCase`

### Example 2: Splitter (in1out2)

**Use Case**: Route input to one of two outputs based on condition

```
┌─────────────────┐
│   in1out2       │       ┌──→ success path
│ ┌──────────────┐│       │
│ │ input1    ●──┼┼──→ ───┤
│ └──────────────┘│       │
│ ┌──────────────┐│       └──→ error path
│ │ output1   ●──┼┼──→
│ └──────────────┘│
│ ┌──────────────┐│
│ │ output2   ●──┼┼──→
│ └──────────────┘│
└─────────────────┘
```

**Configuration**:
- Display Name: `ValidateInput`
- Input Port: `data`
- Output Ports: `valid`, `invalid`
- UseCase: `com.example.InputValidatorUseCase`

### Example 3: Merger (in2out1)

**Use Case**: Combine two inputs into one output

```
data1 ───┐
         │  ┌─────────────────┐
         └──┼──● input1       │
            │ ┌──────────────┐│
            │ │ output1   ●──┼┼──→ combined
data2 ─────┼──● input2       │
            │ └──────────────┘│
            └─────────────────┘
```

**Configuration**:
- Display Name: `CombineUserData`
- Input Ports: `profile`, `preferences`
- Output Port: `userData`
- UseCase: `com.example.CombineUserDataUseCase`

### Example 4: Generator (in0out1)

**Use Case**: Source node that produces data without input

```
┌─────────────────┐
│   in0out1       │
│ ┌──────────────┐│
│ │ output1   ●──┼┼──→ stream of events
│ └──────────────┘│
└─────────────────┘
```

**Configuration**:
- Display Name: `EventSource`
- Output Port: `events`
- UseCase: `com.example.EventGeneratorUseCase`

### Example 5: Sink (in1out0)

**Use Case**: Terminal node that consumes data without output

```
events ───┐
          │  ┌─────────────────┐
          └──┼──● input1       │
             │ (no outputs)    │
             └─────────────────┘
```

**Configuration**:
- Display Name: `Logger`
- Input Port: `logEntry`
- UseCase: `com.example.LoggingUseCase`

## Programmatic Usage

### Creating Generic NodeTypeDefinitions

```kotlin
import io.codenode.fbpdsl.factory.GenericNodeTypeFactory.createGenericNodeType

// Basic usage - creates "in2out1" with default port names
val merger = createGenericNodeType(numInputs = 2, numOutputs = 1)

// With custom configuration
val emailValidator = createGenericNodeType(
    numInputs = 2,
    numOutputs = 1,
    customName = "EmailValidator",
    customDescription = "Validates email addresses",
    inputNames = listOf("email", "rules"),
    outputNames = listOf("isValid"),
    useCaseClassName = "com.example.EmailValidatorUseCase"
)
```

### Getting All Generic Types for Palette

```kotlin
import io.codenode.fbpdsl.factory.GenericNodeTypeFactory.getAllGenericNodeTypes
import io.codenode.fbpdsl.factory.GenericNodeTypeFactory.getCommonGenericNodeTypes

// All 36 combinations (0-5 inputs × 0-5 outputs)
val allTypes = getAllGenericNodeTypes()

// Common subset (in0out1, in1out0, in1out1, in1out2, in2out1)
val commonTypes = getCommonGenericNodeTypes()
```

### Detecting Generic Nodes

```kotlin
// Check if a CodeNode came from a generic NodeTypeDefinition
fun isGenericNode(node: CodeNode): Boolean {
    return node.configuration["_genericType"] != null
}

// Get the original generic type
fun getGenericType(node: CodeNode): String? {
    return node.configuration["_genericType"]
}
```

## Generated Code Examples

### With UseCase Reference

When you configure a UseCase class, the generated component delegates to it:

```kotlin
// Source flow graph:
// EmailValidator (in2out1) with UseCase = com.example.EmailValidatorUseCase

// Generated: EmailValidatorComponent.kt
class EmailValidatorComponent(
    private val useCase: EmailValidatorUseCase
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

### Without UseCase Reference

Without a UseCase, you get a placeholder:

```kotlin
// Generated: GenericNode1Component.kt
class GenericNode1Component {
    val input1 = Channel<Any>(Channel.RENDEZVOUS)
    val output1 = Channel<Any>(Channel.RENDEZVOUS)

    suspend fun process() {
        val inputValue = input1.receive()

        // TODO: Implement processing logic
        // output1.send(transformedValue)

        throw NotImplementedError("Configure UseCase or implement here")
    }
}
```

## Best Practices

### 1. Use Meaningful Names

Instead of keeping default names like `in2out1`, rename your generic nodes to describe their purpose:
- `UserDataMerger` instead of `in2out1`
- `EventLogger` instead of `in1out0`
- `ResultSplitter` instead of `in1out2`

### 2. Use Descriptive Port Names

Rename ports from defaults (`input1`, `output1`) to meaningful names:
- `email`, `validationRules`, `isValid`
- `userData`, `formattedOutput`

### 3. Provide UseCase Classes for Production

For production code generation, always specify UseCase classes:
- Enables generated components to have real functionality
- Keeps business logic separate from flow graph structure
- Makes components testable

### 4. Choose the Right Generic Type

| Scenario | Generic Type | Example |
|----------|--------------|---------|
| Transform data | `in1out1` | Format dates, convert units |
| Validate/filter | `in1out2` | Route valid/invalid |
| Combine data | `in2out1` | Merge profiles |
| Fan-out | `in1outN` | Broadcast to multiple handlers |
| Generate | `in0out1` | Timer, event source |
| Consume | `in1out0` | Logger, database writer |

## Troubleshooting

### Generic Category Not Showing in Palette

Ensure generic node types are added to the node type list:
```kotlin
val nodeTypes = createSampleNodeTypes() + getAllGenericNodeTypes()
```

### Code Generation Fails for Generic Nodes

Check that:
1. UseCase class name is fully-qualified (e.g., `com.example.MyUseCase`)
2. UseCase class exists in your project
3. UseCase has an `execute()` method with matching parameters

### Port Names Not Persisting

Verify you're setting custom port names at NodeTypeDefinition creation time:
```kotlin
createGenericNodeType(
    numInputs = 2,
    numOutputs = 1,
    inputNames = listOf("a", "b"),  // Must match numInputs
    outputNames = listOf("result")   // Must match numOutputs
)
```

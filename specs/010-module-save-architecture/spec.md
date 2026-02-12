# Feature Specification: Module Save Architecture

**Feature Branch**: `010-module-save-architecture`
**Created**: 2026-02-12
**Status**: Draft
**Input**: Refactor the FlowGraph save/compile workflow so that module structure is created at save time (not compile time), ProcessingLogic stub files are scaffolded for the user to implement, and the FlowGraph definition becomes a compiled .flow.kt Kotlin source file rather than an interpreted .flow.kts script.

## Problem Statement

The current architecture has several issues:

1. **ProcessingLogic location ambiguity** - User-created ProcessingLogic files live outside the generated module, causing import/compilation issues
2. **Script vs Source** - The .flow.kts files are Kotlin scripts requiring a custom deserializer, missing IDE support (autocomplete, refactoring, compile-time errors)
3. **Late module creation** - Module structure is only created at compile time, but ProcessingLogic implementations need a home before that

## Proposed Architecture

### Two-Phase Workflow

**Phase 1: Save** - Creates the module skeleton
- Module directory with build.gradle.kts
- `{GraphName}.flow.kt` - Compiled Kotlin DSL defining the FlowGraph
- Stub ProcessingLogic files for each CodeNode (user fills in implementation)

**Phase 2: Compile** - Generates executable code
- Factory function `create{GraphName}FlowGraph()` that instantiates the FlowGraph with ProcessingLogic
- Flow orchestration code (connection wiring, lifecycle management)

### Key Changes

1. **.flow.kt replaces .flow.kts** - FlowGraph definition is compiled Kotlin, not interpreted script
2. **Module created on Save** - Not deferred to compile time
3. **ProcessingLogic stubs generated** - User implements them in the module
4. **No custom deserializer needed** - Standard Kotlin compilation

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Save Creates Module Structure (Priority: P1)

A flow graph designer creates a new FlowGraph in the graphEditor with two CodeNodes (TimerEmitter and DisplayReceiver). When they click Save and specify a location, the system creates a complete KMP module directory containing:
- `build.gradle.kts` with KMP configuration
- `src/commonMain/kotlin/{package}/StopWatch.flow.kt` defining the graph
- `src/commonMain/kotlin/{package}/TimerEmitterComponent.kt` stub
- `src/commonMain/kotlin/{package}/DisplayReceiverComponent.kt` stub

The designer can immediately open these stub files in their IDE and implement the ProcessingLogic.

**Why this priority**: This is the foundational change. All other features depend on the module existing with proper structure before compile.

**Independent Test**: Save a FlowGraph and verify the complete module structure is created with all expected files.

**Acceptance Scenarios**:

1. **Given** a new FlowGraph with CodeNodes, **When** the user saves it, **Then** a module directory is created with build.gradle.kts.
2. **Given** a FlowGraph with N CodeNodes, **When** saved, **Then** N ProcessingLogic stub files are generated, one per CodeNode.
3. **Given** the module is created, **When** opened in an IDE, **Then** it compiles (stubs have valid syntax) and provides full IDE support.

---

### User Story 2 - FlowGraph as Compiled Kotlin (.flow.kt) (Priority: P1)

The FlowGraph definition is saved as a `.flow.kt` file that is actual Kotlin source code, not a script. The file uses the same DSL syntax but is compiled by the Kotlin compiler. This provides IDE autocomplete, syntax highlighting, refactoring support, and compile-time error checking. The graphEditor can read this file to reconstruct the visual graph.

**Why this priority**: Equal to US1 because the .flow.kt format is core to the new architecture. It eliminates the need for FlowGraphDeserializer and enables IDE tooling.

**Independent Test**: Save a FlowGraph, verify the .flow.kt file is valid Kotlin that compiles, and verify the graphEditor can reopen it.

**Acceptance Scenarios**:

1. **Given** a FlowGraph is saved, **When** the .flow.kt file is examined, **Then** it contains valid Kotlin code using the FlowGraph DSL.
2. **Given** a .flow.kt file exists, **When** opened in IntelliJ/Android Studio, **Then** full IDE support is available (autocomplete, error highlighting, refactoring).
3. **Given** a .flow.kt file exists, **When** opened in graphEditor, **Then** the visual graph is reconstructed correctly.

---

### User Story 3 - ProcessingLogic Stub Generation (Priority: P2)

When a module is created, each CodeNode gets a corresponding ProcessingLogic stub file. The stub implements the `ProcessingLogic` interface with placeholder implementation. The stub includes:
- Class name derived from node name (e.g., `TimerEmitter` → `TimerEmitterComponent`)
- `invoke()` method with input/output parameters matching the node's ports
- KDoc describing the expected behavior based on node type (GENERATOR, TRANSFORMER, SINK)

**Why this priority**: Depends on US1 (module creation). Provides the scaffolding users need to implement their logic.

**Independent Test**: Save a FlowGraph with various node types and verify appropriate stubs are generated with correct signatures.

**Acceptance Scenarios**:

1. **Given** a CodeNode named "DataProcessor", **When** the module is saved, **Then** a `DataProcessorComponent.kt` stub file is created implementing ProcessingLogic.
2. **Given** a CodeNode with input ports (a: Int, b: String), **When** the stub is generated, **Then** the invoke() method signature reflects these inputs.
3. **Given** a GENERATOR node type, **When** the stub is generated, **Then** the KDoc indicates this node should emit data without requiring inputs.

---

### User Story 4 - Compile Generates Factory Function (Priority: P2)

After the user has implemented their ProcessingLogic classes, running compile generates the factory function `create{GraphName}FlowGraph()`. This function instantiates each CodeNode with its ProcessingLogic implementation, creates all port definitions, wires connections, and returns the complete FlowGraph.

**Why this priority**: Depends on US1-3 being complete. This is the code generation payoff.

**Independent Test**: Implement ProcessingLogic stubs, run compile, verify factory function is generated that creates valid FlowGraph.

**Acceptance Scenarios**:

1. **Given** all ProcessingLogic classes are implemented, **When** compile runs, **Then** a factory function is generated in the module.
2. **Given** the factory function exists, **When** called, **Then** it returns a FlowGraph with all nodes having their ProcessingLogic instances.
3. **Given** an app depends on the module, **When** it imports and calls the factory function, **Then** the app compiles and the FlowGraph is fully functional.

---

### User Story 5 - Incremental Save (Re-save Existing Module) (Priority: P3)

When a user modifies an existing FlowGraph and saves, the system updates the module intelligently:
- Updates the .flow.kt file with new graph structure
- Generates stubs for NEW nodes only (doesn't overwrite existing implementations)
- Warns if nodes were removed (their ProcessingLogic files are now orphaned)

**Why this priority**: Important for real-world usage but depends on basic save working first.

**Independent Test**: Modify an existing FlowGraph (add/remove nodes), save, verify only new stubs created and existing implementations preserved.

**Acceptance Scenarios**:

1. **Given** an existing module with implemented ProcessingLogic, **When** the FlowGraph is modified and re-saved, **Then** existing implementations are not overwritten.
2. **Given** a new node is added to an existing FlowGraph, **When** saved, **Then** only the new node gets a stub file.
3. **Given** a node is removed from a FlowGraph, **When** saved, **Then** a warning indicates the orphaned ProcessingLogic file.

---

## Technical Design

### Module Structure (Created on Save)

```
{ModuleName}/
├── build.gradle.kts          # KMP build configuration
├── settings.gradle.kts       # Module settings
└── src/
    └── commonMain/
        └── kotlin/
            └── {package}/
                ├── {GraphName}.flow.kt           # FlowGraph definition (compiled DSL)
                ├── {Node1Name}Component.kt       # ProcessingLogic stub
                ├── {Node2Name}Component.kt       # ProcessingLogic stub
                └── ...
```

### .flow.kt File Format

```kotlin
package io.codenode.generated.stopwatch

import io.codenode.fbpdsl.dsl.*

/**
 * StopWatch FlowGraph Definition
 *
 * This file defines the structure of the StopWatch virtual circuit.
 * Edit this file to modify nodes, ports, and connections.
 * Implement ProcessingLogic in the companion Component files.
 */
val stopWatchGraph = flowGraph("StopWatch", version = "1.0.0") {

    val timerEmitter = codeNode("TimerEmitter", nodeType = GENERATOR) {
        position(100.0, 100.0)
        output("elapsedSeconds", Int::class)
        output("elapsedMinutes", Int::class)
        config("speedAttenuation", "1000")
        processingLogic<TimerEmitterComponent>()
    }

    val displayReceiver = codeNode("DisplayReceiver", nodeType = SINK) {
        position(400.0, 100.0)
        input("seconds", Int::class)
        input("minutes", Int::class)
        processingLogic<DisplayReceiverComponent>()
    }

    timerEmitter.output("elapsedSeconds") connect displayReceiver.input("seconds")
    timerEmitter.output("elapsedMinutes") connect displayReceiver.input("minutes")
}
```

### ProcessingLogic Stub Template

```kotlin
package io.codenode.generated.stopwatch

import io.codenode.fbpdsl.model.InformationPacket
import io.codenode.fbpdsl.model.ProcessingLogic

/**
 * ProcessingLogic implementation for TimerEmitter node.
 *
 * Node Type: GENERATOR
 * Outputs: elapsedSeconds (Int), elapsedMinutes (Int)
 *
 * TODO: Implement the timer logic that emits elapsed time values.
 */
class TimerEmitterComponent : ProcessingLogic {

    override suspend fun invoke(
        inputs: Map<String, InformationPacket<*>>
    ): Map<String, InformationPacket<*>> {
        // TODO: Implement processing logic
        throw NotImplementedError("Implement TimerEmitterComponent processing logic")
    }
}
```

### Files Generated at Compile Time

After user implements ProcessingLogic:

```
{ModuleName}/
└── src/
    └── commonMain/
        └── kotlin/
            └── {package}/
                ├── {GraphName}Flow.kt            # Generated: Flow orchestrator class
                ├── {GraphName}Controller.kt      # Generated: Lifecycle controller
                └── {GraphName}Factory.kt         # Generated: createXXXFlowGraph() function
```

---

## Migration Path

### From 009-stopwatch-codegen-refactor

1. Existing `demos/stopwatch/StopWatch.flow.kts` → Convert to `StopWatch/src/.../StopWatch.flow.kt`
2. Existing `demos/stopwatch/TimerEmitterComponent.kt` → Move to `StopWatch/src/.../`
3. Existing `demos/stopwatch/DisplayReceiverComponent.kt` → Move to `StopWatch/src/.../`
4. Remove FlowGraphDeserializer dependency for .flow.kts parsing
5. Update graphEditor Save to create module structure
6. Update graphEditor Open to read .flow.kt files

---

## Edge Cases

- **What if user manually edits .flow.kt with invalid syntax?**
  - Kotlin compiler catches errors; graphEditor shows parse error on open

- **What if ProcessingLogic class name doesn't match expected pattern?**
  - Compile validation warns about missing/mismatched ProcessingLogic references

- **What if module already exists at save location?**
  - Prompt user: overwrite, merge (incremental), or cancel

- **What if user deletes a ProcessingLogic file but node still references it?**
  - Compile fails with clear error message about missing class

---

## Out of Scope

- IDE plugin integration (future spec)
- Version control integration for .flow.kt files
- Visual diff for FlowGraph changes
- Multi-module FlowGraph dependencies

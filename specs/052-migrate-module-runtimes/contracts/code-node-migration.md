# Contract: CodeNode Migration Pattern

**Feature**: 052-migrate-module-runtimes
**Date**: 2026-03-16

## CodeNodeDefinition Contract

Each migrated node MUST implement the `CodeNodeDefinition` interface as a Kotlin `object` singleton.

### Required Properties

```kotlin
object ExampleCodeNode : CodeNodeDefinition {
    override val name: String           // Matches the node name in .flow.kt DSL
    override val category: NodeCategory // SOURCE, PROCESSOR, or SINK
    override val description: String    // Human-readable tooltip text
    override val inputPorts: List<PortSpec>   // Input port specs (empty for sources)
    override val outputPorts: List<PortSpec>  // Output port specs (empty for sinks)

    override fun createRuntime(name: String): NodeRuntime {
        // Return the exact same runtime type that the generated Flow uses
    }
}
```

### Port Name Convention

Port names in `PortSpec` MUST match the port names defined in the module's `.flow.kt` DSL file. The DynamicPipelineBuilder uses index-based matching, but names should remain consistent for readability and palette display.

### createRuntime() Contract

The `createRuntime()` method MUST:
1. Use the same `CodeNodeFactory` method that the generated Flow currently uses
2. Embed the processing logic directly (no external tick function reference)
3. Reference the module's State singleton for input/output state management
4. For entity modules: access DAO via the module's Persistence KoinComponent
5. Return a runtime that produces identical observable behavior to the generated code

### File Location

```
{Module}/src/commonMain/kotlin/io/codenode/{module}/nodes/{NodeName}CodeNode.kt
```

### ServiceLoader Registration

Each module MUST have a ServiceLoader file at:
```
{Module}/src/jvmMain/resources/META-INF/services/io.codenode.fbpdsl.runtime.CodeNodeDefinition
```

Content: one fully-qualified class name per line for each CodeNodeDefinition in the module.

## StopWatch Node Contracts

### TimerEmitterCodeNode

```kotlin
object TimerEmitterCodeNode : CodeNodeDefinition {
    override val name = "TimerEmitter"
    override val category = NodeCategory.SOURCE
    override val inputPorts = emptyList<PortSpec>()
    override val outputPorts = listOf(
        PortSpec("elapsedSeconds", Int::class),
        PortSpec("elapsedMinutes", Int::class)
    )

    override fun createRuntime(name: String): NodeRuntime {
        // Returns SourceOut2Runtime<Int, Int>
        // Combines StopWatchState._elapsedSeconds and _elapsedMinutes
        // Emits ProcessResult2.both() on state changes (drop(1))
    }
}
```

### TimeIncrementerCodeNode

```kotlin
object TimeIncrementerCodeNode : CodeNodeDefinition {
    override val name = "TimeIncrementer"
    override val category = NodeCategory.PROCESSOR
    override val inputPorts = listOf(
        PortSpec("elapsedSeconds", Int::class),
        PortSpec("elapsedMinutes", Int::class)
    )
    override val outputPorts = listOf(
        PortSpec("seconds", Int::class),
        PortSpec("minutes", Int::class)
    )

    override fun createRuntime(name: String): NodeRuntime {
        // Returns In2Out2Runtime<Int, Int, Int, Int>
        // Embeds timeIncrementerTick logic:
        //   increment seconds, roll over at 60, update state
    }
}
```

### DisplayReceiverCodeNode

```kotlin
object DisplayReceiverCodeNode : CodeNodeDefinition {
    override val name = "DisplayReceiver"
    override val category = NodeCategory.SINK
    override val inputPorts = listOf(
        PortSpec("seconds", Int::class),
        PortSpec("minutes", Int::class)
    )
    override val outputPorts = emptyList<PortSpec>()

    override fun createRuntime(name: String): NodeRuntime {
        // Returns SinkIn2AnyRuntime<Int, Int> with initialValue1=0, initialValue2=0
        // Updates StopWatchState._seconds and _minutes
    }
}
```

## Entity Module Node Contracts

All three entity modules (UserProfiles, GeoLocations, Addresses) follow identical contracts with module-specific types.

### CUD Source Contract

```kotlin
object {Entity}CUDCodeNode : CodeNodeDefinition {
    override val name = "{Entity}CUD"  // e.g., "UserProfileCUD"
    override val category = NodeCategory.SOURCE
    override val inputPorts = emptyList<PortSpec>()
    override val outputPorts = listOf(
        PortSpec("save", Any::class),
        PortSpec("update", Any::class),
        PortSpec("remove", Any::class)
    )

    override fun createRuntime(name: String): NodeRuntime {
        // Returns SourceOut3Runtime<Any, Any, Any>
        // Collects from {Module}State._save, _update, _remove with .drop(1)
        // Emits ProcessResult3 selectively (one non-null at a time)
        // Resets the state value to null after emission
    }
}
```

### Repository Processor Contract

```kotlin
object {Entity}RepositoryCodeNode : CodeNodeDefinition {
    override val name = "{Entity}Repository"  // e.g., "UserProfileRepository"
    override val category = NodeCategory.PROCESSOR
    override val inputPorts = listOf(
        PortSpec("save", Any::class),
        PortSpec("update", Any::class),
        PortSpec("remove", Any::class)
    )
    override val outputPorts = listOf(
        PortSpec("result", Any::class),
        PortSpec("error", Any::class)
    )

    override fun createRuntime(name: String): NodeRuntime {
        // Returns In3AnyOut2Runtime<Any, Any, Any, Any, Any>
        // initialValue1/2/3 = Unit
        // Embeds repository tick logic with identity tracking:
        //   var lastSaveRef/lastUpdateRef/lastRemoveRef in closure
        //   Accesses DAO via {Module}Persistence.dao (Koin)
        //   Returns ProcessResult2.first() for success, .second() for error
    }
}
```

### Display Sink Contract

```kotlin
object {Entity}DisplayCodeNode : CodeNodeDefinition {
    override val name = "{Entity}Display"  // e.g., "UserProfilesDisplay"
    override val category = NodeCategory.SINK
    override val inputPorts = listOf(
        PortSpec("result", Any::class),
        PortSpec("error", Any::class)
    )
    override val outputPorts = emptyList<PortSpec>()

    override fun createRuntime(name: String): NodeRuntime {
        // Returns SinkIn2Runtime<Any, Any>
        // Updates {Module}State._result and _error
    }
}
```

## ModuleSessionFactory Contract

The factory MUST support dynamic sessions for all migrated modules:

```kotlin
// For each module, when canBuildDynamic() returns true:
1. Create DynamicPipelineController with flowGraphProvider and lookup
2. Create adapter implementing {Module}ControllerInterface
3. Create {Module}ViewModel with adapter (+ DAO for entity modules)
4. Return RuntimeSession with controller, viewModel, flowGraph, flowGraphProvider
```

Entity modules NO LONGER need pre-starting (controller.start() before RuntimeSession) because:
- DynamicPipelineController defers pipeline construction to start()
- Source nodes use .drop(1) and wait for user actions
- RuntimeSession.start() triggers the pipeline when the user presses Start

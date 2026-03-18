# Research: CodeNode-Driven Flow Runtime

## R1: How Generated Flow Files Currently Create Runtimes

**Decision**: Three patterns exist in the current generated Flow files, all of which need to be replaced with `CodeNodeDefinition.createRuntime()`.

**Current patterns**:

1. **Processor nodes** import a `*Tick` function from `processingLogic/` and pass it to the factory:
   ```kotlin
   import io.codenode.stopwatch.processingLogic.timeIncrementerTick
   val timeIncrementer = CodeNodeFactory.createIn2Out2Processor<Int, Int, Int, Int>(
       name = "TimeIncrementer",
       process = timeIncrementerTick
   )
   ```

2. **Source/Sink nodes (entity modules)** call factory functions from CUD/Display stub files:
   ```kotlin
   import io.codenode.userprofiles.createUserProfileCUD
   val userProfileCUD = createUserProfileCUD()
   ```

3. **Source/Sink nodes (StopWatch)** have inline lambda logic directly in the generated Flow:
   ```kotlin
   val timerEmitter = CodeNodeFactory.createSourceOut2<Int, Int>(
       name = "TimerEmitter",
       generate = { emit -> combine(...).drop(1).collect { emit(it) } }
   )
   ```

**Replacement**: All three patterns become:
```kotlin
val timeIncrementer = TimerEmitterCodeNode.createRuntime("TimerEmitter")
```

**Rationale**: CodeNodeDefinition.createRuntime() encapsulates all factory method selection, type parameters, and processing logic. The generated Flow no longer needs to know any of these details.

## R2: How to Resolve CodeNodeDefinition Objects in Generated Flow Files

**Decision**: Import CodeNodeDefinition objects directly by their fully-qualified class name.

**Rationale**: CodeNode objects are Kotlin `object` singletons in `commonMain` source sets. They can be imported directly — no registry lookup needed at compile time. The generated Flow file imports each CodeNode object and calls `createRuntime()` on it.

**Example**:
```kotlin
import io.codenode.stopwatch.nodes.TimerEmitterCodeNode
import io.codenode.stopwatch.nodes.TimeIncrementerCodeNode
import io.codenode.stopwatch.nodes.DisplayReceiverCodeNode

class StopWatchFlow {
    internal val timerEmitter = TimerEmitterCodeNode.createRuntime("TimerEmitter")
    internal val timeIncrementer = TimeIncrementerCodeNode.createRuntime("TimeIncrementer")
    internal val displayReceiver = DisplayReceiverCodeNode.createRuntime("DisplayReceiver")
    // ... lifecycle and wiring unchanged
}
```

**Alternatives considered**:
- *Registry lookup at runtime* — Rejected. Adds unnecessary runtime complexity and dependency on a registry being initialized. Direct imports are simpler and type-safe.
- *Passing CodeNodeDefinitions as constructor parameters* — Rejected. Would change the Controller's constructor signature, cascading changes to Controller, Adapter, and all call sites.

## R3: Impact on Channel Wiring in Generated Flow Files

**Decision**: Channel wiring code in the generated Flow files remains unchanged.

**Rationale**: `CodeNodeDefinition.createRuntime()` returns a `NodeRuntime` — the same base type that the current factory methods return. The wiring code accesses `inputChannel1`, `outputChannel1`, etc. on the concrete runtime types. Since `createRuntime()` returns the same concrete runtime classes (e.g., `In2Out2Runtime`, `SourceOut2Runtime`), the wiring code works identically.

**Caveat**: The generated Flow file currently uses typed runtime variables (e.g., `In2Out2Runtime<Int, Int, Int, Int>`). With `createRuntime()`, the return type is `NodeRuntime` (base type). The wiring code needs to cast or the variable type needs to be the concrete type. Since the wiring uses property access on specific runtime types, we need to cast the result of `createRuntime()`.

**Solution**: Cast at assignment time:
```kotlin
internal val timeIncrementer = TimeIncrementerCodeNode.createRuntime("TimeIncrementer") as In2Out2Runtime<*, *, *, *>
```

Or better — since channel wiring accesses properties that exist on `NodeRuntime` subclasses, use the same pattern DynamicPipelineBuilder uses: access channels via reflection-style helpers or direct property access on the runtime's known type.

**Simplest approach**: The generated Flow can import the CodeNode and call `createRuntime()`, then cast to the expected runtime type for channel access. This is safe because the CodeNode's `createRuntime()` always returns the same concrete type.

## R4: Impact on RuntimeFlowGenerator

**Decision**: Add a CodeNode-aware generation path to RuntimeFlowGenerator.

**Current behavior**: RuntimeFlowGenerator generates runtime instances by:
1. Determining factory method from port counts (via RuntimeTypeResolver)
2. Generating type parameters from port types
3. Importing and referencing processingLogic tick functions for processors
4. Importing and calling CUD/Display factory functions for entity stubs
5. Generating inline lambdas for source/sink nodes

**New behavior**: When the generator is told a node has a CodeNodeDefinition:
1. Import the CodeNode object instead of processingLogic/stub functions
2. Generate `val nodeName = CodeNodeObject.createRuntime("NodeName")`
3. Skip all factory method selection, type parameter generation, and lambda generation for that node

**How the generator knows**: The generator receives the CodeNode's definition class name (e.g., `io.codenode.stopwatch.nodes.TimerEmitterCodeNode`) as metadata. This can be provided via:
- A new field on the FlowGraph node's configuration map (e.g., `_codeNodeClass`)
- Or passed as a parameter to the generator alongside the FlowGraph

**Backward compatibility**: Nodes without CodeNodeDefinitions continue to use the existing generation path (FR-003).

## R5: Files That Become Dead Code After This Change

**Decision**: Delete the following files that are superseded by CodeNode objects.

**ProcessingLogic files** (1 file — StopWatch is the only module with a processor tick):
- `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/processingLogic/TimeIncrementerProcessLogic.kt`

**CUD/Display stub files** (6 files — each entity module has one of each):
- `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/UserProfileCUD.kt`
- `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/UserProfilesDisplay.kt`
- `GeoLocations/src/commonMain/kotlin/io/codenode/geolocations/GeoLocationCUD.kt`
- `GeoLocations/src/commonMain/kotlin/io/codenode/geolocations/GeoLocationsDisplay.kt`
- `Addresses/src/commonMain/kotlin/io/codenode/addresses/AddressCUD.kt`
- `Addresses/src/commonMain/kotlin/io/codenode/addresses/AddressesDisplay.kt`

**Total**: 7 files deleted.

**Note**: The `processingLogic/` directory in StopWatch becomes empty after deleting the single file, so the directory itself should be removed.

## R6: KMPMobileApp Impact

**Decision**: The KMPMobileApp requires no code changes.

**Rationale**: KMPMobileApp uses the generated Controller (which delegates to the generated Flow). The Controller's public API doesn't change — only the Flow's internal implementation changes from processingLogic imports to CodeNode imports. Since the CodeNode objects are in `commonMain`, they're accessible from all KMP targets (Android, iOS, JVM).

**Risk**: The KMPMobileApp's `build.gradle.kts` must have module dependencies that transitively include the `nodes/` packages. Since the CodeNode files are in the same module (e.g., `StopWatch`), this is already satisfied.

## R7: Observable State Delegation in Generated Flow

**Decision**: Observable state delegation (e.g., `val secondsFlow = StopWatchState.secondsFlow`) remains unchanged.

**Rationale**: The state delegation in generated Flow files reads from the module State object's public StateFlow properties. This has nothing to do with how runtimes are created — it's a separate concern for exposing state to the Controller/ViewModel layer. No change needed.

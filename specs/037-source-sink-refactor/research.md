# Research: Modify Source and Sink Nodes

**Feature Branch**: `037-source-sink-refactor`
**Date**: 2026-03-04

## R1: Scope of Generator-to-Source Rename

### Decision
Rename `CodeNodeType.GENERATOR` to `CodeNodeType.SOURCE` and all associated runtime classes, factory methods, type aliases, and references across the codebase. This is a clean rename with no backward compatibility shims.

### Rationale
The term "generator" implies autonomous data production (timed loops, polling). The new architecture has data entering the flow graph from UI interactions via ViewModel binding, making "source" the accurate descriptor — a passive entry point for externally-produced data.

### Alternatives Considered
- **Keep GENERATOR name**: Rejected — creates conceptual mismatch when generators no longer generate autonomously.
- **Add SOURCE as alias, deprecate GENERATOR**: Rejected — codebase is under active development; clean rename avoids deprecated code clutter.

## R2: Runtime Class Naming Convention

### Decision
Use `{Role}{Direction}{Count}Runtime` pattern:
- `SourceRuntime<T>` (was `GeneratorRuntime<T>`)
- `SourceOut2Runtime<U, V>` (was `Out2GeneratorRuntime<U, V>`)
- `SourceOut3Runtime<U, V, W>` (was `Out3GeneratorRuntime<U, V, W>`)
- `SinkIn2Runtime<A, B>` (was `In2SinkRuntime<A, B>`)
- `SinkIn3Runtime<A, B, C>` (was `In3SinkRuntime<A, B, C>`)
- `SinkIn2AnyRuntime<A, B>` (was `In2AnySinkRuntime<A, B>`)
- `SinkIn3AnyRuntime<A, B, C>` (was `In3AnySinkRuntime<A, B, C>`)
- `SinkRuntime<T>` (unchanged — already follows pattern)

### Rationale
Consistent `{Role}{Direction}{Count}` pattern makes the API self-documenting. Role comes first for alphabetical grouping (all Source* together, all Sink* together).

### Alternatives Considered
- `Out2SourceRuntime` (preserving old prefix pattern): Rejected — inconsistent; role should come first.

## R3: Source Node Behavioral Change — No Tick Loop

### Decision
Source nodes no longer contain a timed while loop with a tick function. Instead:
1. `SourceRuntime` retains its `outputChannel` but the generate block is driven externally (via ViewModel)
2. The code generator (`RuntimeFlowGenerator`) creates source nodes using `createContinuousSource` with a generate block that receives values pushed from the ViewModel/Controller
3. The Controller exposes emit methods for each source node, called by the ViewModel in response to UI events
4. No `createTimedSource` factory method is needed (timed variants removed for source nodes)
5. No `ProcessingLogic` stub is generated for source nodes

### Rationale
Feature 034 established that data flows from UI → ViewModel → Controller → Flow. Source nodes are passive conduits for UI-driven data, not autonomous producers. Tick functions are unnecessary when the UI drives data production.

### Alternatives Considered
- Keep timed source as an option alongside UI-driven source: Rejected — adds complexity; timer behavior can be implemented as a transformer or in the UI layer.

## R4: Sink Node Stub Removal

### Decision
Sink nodes no longer generate a `ProcessingLogic` stub file. The consume block in the generated `{Name}Flow.kt` only updates observable state (`{FlowName}State._property.value = value`). No user tick function is called.

### Rationale
Feature 034 already established that sink input ports drive observable state in the ViewModel. The tick function in sink stubs is redundant — the only purpose of a sink is to bridge flow data into ViewModel-observable state.

### Alternatives Considered
- Keep sink stubs for custom side-effect logic: Rejected — side effects should be handled by transformer nodes upstream of the sink, keeping sinks as pure state bridges.

## R5: Factory Method Naming

### Decision
Rename factory methods to match runtime class naming:

| Old Method | New Method |
|-----------|-----------|
| `createContinuousGenerator<T>` | `createContinuousSource<T>` |
| `createTimedGenerator<T>` | *(removed — no timed sources)* |
| `createOut2Generator<U, V>` | `createSourceOut2<U, V>` |
| `createOut3Generator<U, V, W>` | `createSourceOut3<U, V, W>` |
| `createTimedOut2Generator<U, V>` | *(removed — no timed sources)* |
| `createTimedOut3Generator<U, V, W>` | *(removed — no timed sources)* |
| `createContinuousSink<T>` | `createContinuousSink<T>` *(unchanged)* |
| `createIn2Sink<A, B>` | `createSinkIn2<A, B>` |
| `createIn3Sink<A, B, C>` | `createSinkIn3<A, B, C>` |
| `createIn2AnySink<A, B>` | `createSinkIn2Any<A, B>` |
| `createIn3AnySink<A, B, C>` | `createSinkIn3Any<A, B, C>` |
| `createTimedSink<T>` | *(removed — no timed sinks)* |
| `createTimedIn2Sink<A, B>` | *(removed — no timed sinks)* |
| `createTimedIn3Sink<A, B, C>` | *(removed — no timed sinks)* |

### Rationale
Timed factory methods are artifacts of the autonomous tick pattern. With source nodes driven by UI and sink nodes as pure state bridges, timed variants are unnecessary. Continuous variants remain for manual/test usage.

## R6: Type Alias Naming

### Decision
Rename type aliases to match new conventions:

| Old Alias | New Alias |
|----------|----------|
| `ContinuousGeneratorBlock<T>` | `ContinuousSourceBlock<T>` |
| `GeneratorTickBlock<T>` | *(removed — no timed sources)* |
| `Out2GeneratorBlock<U, V>` | `SourceOut2Block<U, V>` |
| `Out2TickBlock<U, V>` | *(removed)* |
| `Out3GeneratorBlock<U, V, W>` | `SourceOut3Block<U, V, W>` |
| `Out3TickBlock<U, V, W>` | *(removed)* |
| `ContinuousSinkBlock<T>` | `ContinuousSinkBlock<T>` *(unchanged)* |
| `SinkTickBlock<T>` | *(removed — no timed sinks)* |
| `In2SinkBlock<A, B>` | `SinkIn2Block<A, B>` |
| `In2SinkTickBlock<A, B>` | *(removed)* |
| `In3SinkBlock<A, B, C>` | `SinkIn3Block<A, B, C>` |
| `In3SinkTickBlock<A, B, C>` | *(removed)* |
| `In2AnySinkBlock<A, B>` | `SinkIn2AnyBlock<A, B>` |
| `In2AnySinkTickBlock<A, B>` | *(removed)* |
| `In3AnySinkBlock<A, B, C>` | `SinkIn3AnyBlock<A, B, C>` |
| `In3AnySinkTickBlock<A, B, C>` | *(removed)* |

## R7: Files Requiring Changes

### Source Code Files (Active Code)

**fbpDsl module — Runtime classes (rename files):**
1. `GeneratorRuntime.kt` → `SourceRuntime.kt`
2. `Out2GeneratorRuntime.kt` → `SourceOut2Runtime.kt`
3. `Out3GeneratorRuntime.kt` → `SourceOut3Runtime.kt`
4. `In2SinkRuntime.kt` → `SinkIn2Runtime.kt`
5. `In3SinkRuntime.kt` → `SinkIn3Runtime.kt`
6. `In2AnySinkRuntime.kt` → `SinkIn2AnyRuntime.kt`
7. `In3AnySinkRuntime.kt` → `SinkIn3AnyRuntime.kt`

**fbpDsl module — Other files (edit in place):**
8. `CodeNode.kt` — Rename `GENERATOR` to `SOURCE` in enum
9. `ContinuousTypes.kt` — Rename type aliases
10. `CodeNodeFactory.kt` — Rename factory methods, remove timed variants

**fbpDsl test files:**
11. `TypedNodeRuntimeTest.kt`
12. `PauseResumeTest.kt`
13. `IndependentControlTest.kt`
14. `ContinuousFactoryTest.kt`
15. `AnyInputRuntimeTest.kt`
16. `RuntimeRegistryTest.kt`
17. `RuntimeRegistrationTest.kt`
18. `NodeRuntimeTest.kt`
19. `StopWatchFlowGraphTest.kt`
20. `StopWatchSerializationTest.kt`

**kotlinCompiler module — Code generators (edit in place):**
21. `RuntimeTypeResolver.kt`
22. `ProcessingLogicStubGenerator.kt`
23. `RuntimeFlowGenerator.kt`
24. `FlowKtGenerator.kt`
25. `ComponentGenerator.kt`
26. `FlowGraphFactoryGenerator.kt`
27. `GeneratorTemplate.kt` → `SourceTemplate.kt`

**kotlinCompiler test files:**
28. `RuntimeTypeResolverTest.kt`
29. `RuntimeFlowGeneratorTest.kt`
30. `ProcessingLogicStubGeneratorTest.kt`
31. `RuntimeViewModelGeneratorTest.kt`
32. `RuntimeControllerGeneratorTest.kt`
33. `RuntimeControllerInterfaceGeneratorTest.kt`
34. `RuntimeControllerAdapterGeneratorTest.kt`
35. `FlowKtGeneratorTest.kt`
36. `ConnectionWiringResolverTest.kt`
37. `ObservableStateResolverTest.kt`
38. `FlowGraphFactoryGeneratorTest.kt`
39. `ChannelCapacityTest.kt`
40. `ViewModelGeneratorTest.kt`
41. `StopWatchModuleGeneratorTest.kt`
42. `PropertyCodeGenTest.kt`

**graphEditor module:**
43. `NodeRenderer.kt`
44. `DragAndDropHandler.kt`
45. `Main.kt`
46. `FlowGraphSerializer.kt`
47. `ModuleSaveService.kt`
48. `ModuleSaveServiceTest.kt`
49. `RequiredPropertyValidatorTest.kt`
50. `CompilationValidatorTest.kt`

**Generated module files (existing ProcessLogic stubs to be aware of):**
51. `StopWatch/processingLogic/TimerEmitterProcessLogic.kt` — References GeneratorTickBlock
52. `StopWatch/processingLogic/DisplayReceiverProcessLogic.kt` — References SinkTickBlock
53. `StopWatchV2/processingLogic/TimerEmitterProcessLogic.kt`
54. `StopWatchV2/processingLogic/DisplayReceiverProcessLogic.kt`
55. `UserProfiles/processingLogic/` — Various stubs
56. `RepositoryPattern/processingLogic/` — Various stubs

**Other:**
57. `KMPMobileApp/.../StopWatchIntegrationTest.kt`
58. `StopWatchOriginal/.../TimerEmitterComponent.kt`
59. `StopWatchOriginal/.../DisplayReceiverComponent.kt`
60. `kotlinCompiler/.../RegenerateStopWatch.kt`

### Documentation Files (specs/)
Specs are historical documentation. They should NOT be modified as part of this feature — they document the state of the system at the time each feature was implemented.

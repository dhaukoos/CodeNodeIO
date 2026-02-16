# Implementation Plan: Typed NodeRuntime Stubs

**Branch**: `015-typed-node-runtime` | **Date**: 2026-02-15 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/015-typed-node-runtime/spec.md`

## Summary

Generate typed NodeRuntime stubs for FBP nodes with 0-3 inputs and 0-3 outputs. This extends the existing `NodeRuntime`, `GeneratorRuntime`, `SinkRuntime`, `TransformerRuntime`, and `FilterRuntime` infrastructure from feature 014 to support multi-input/multi-output nodes with compile-time type safety. Factory methods will create typed processor functions (e.g., `createIn2Out1Processor`) and `ProcessResult` data classes will enable nullable multi-output return values.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0
**Storage**: N/A (in-memory FlowGraph models)
**Testing**: runTest with virtual time (advanceTimeBy, advanceUntilIdle)
**Target Platform**: JVM, iOS, Android, Desktop, Web (KMP common)
**Project Type**: Single KMP library project (fbpDsl module)
**Performance Goals**: <1ms node creation, <10μs per message processing overhead
**Constraints**: Compile-time type safety, no runtime reflection, graceful channel closure handling
**Scale/Scope**: 15 valid node configurations (0-3 inputs × 0-3 outputs, excluding 0×0)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| **I. Code Quality First** | ✅ PASS | Public APIs will have explicit type annotations and docstrings. Factory methods under 50 lines. |
| **II. Test-Driven Development** | ✅ PASS | Feature 014 established TDD pattern with runTest/advanceTimeBy. Will continue for this feature. |
| **III. User Experience Consistency** | ✅ PASS | N/A - Library code, no UI components. |
| **IV. Performance Requirements** | ✅ PASS | Channel-based processing with configurable buffer capacity. No O(n²) algorithms. |
| **V. Observability & Debugging** | ✅ PASS | Node names enable identification. ExecutionState tracking inherited from NodeRuntime. |
| **Licensing & IP** | ✅ PASS | Using only Apache 2.0 licensed kotlinx-coroutines. No new dependencies. |

## Project Structure

### Documentation (this feature)

```text
specs/015-typed-node-runtime/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
fbpDsl/
├── src/
│   ├── commonMain/kotlin/io/codenode/fbpdsl/
│   │   ├── model/
│   │   │   └── CodeNodeFactory.kt          # Add typed processor factory methods
│   │   └── runtime/
│   │       ├── NodeRuntime.kt              # Base class (existing)
│   │       ├── GeneratorRuntime.kt         # 0 inputs (existing, extends)
│   │       ├── SinkRuntime.kt              # 0 outputs (existing, extends)
│   │       ├── TransformerRuntime.kt       # 1 input, 1 output (existing)
│   │       ├── FilterRuntime.kt            # 1 input, 1 output predicate (existing)
│   │       ├── ContinuousTypes.kt          # Type aliases (existing, extend)
│   │       ├── ProcessResult.kt            # NEW: ProcessResult<U,V,W> data classes
│   │       └── MultiPortRuntime.kt         # NEW: Multi-input/multi-output runtimes
│   └── commonTest/kotlin/io/codenode/fbpdsl/
│       └── runtime/
│           ├── ContinuousFactoryTest.kt    # Existing tests (extend)
│           └── TypedNodeRuntimeTest.kt     # NEW: Multi-port runtime tests
```

**Structure Decision**: Extend the existing fbpDsl module structure. New files for ProcessResult and MultiPortRuntime, extensions to existing CodeNodeFactory and ContinuousTypes.

## Complexity Tracking

> No violations requiring justification. This feature extends existing patterns.

## Research Summary

### Key Decisions from Feature 014

1. **Runtime vs Model Separation**: `CodeNode` is pure serializable model, `NodeRuntime<T>` owns lifecycle
2. **Specialized Runtimes**: `GeneratorRuntime`, `SinkRuntime`, `TransformerRuntime`, `FilterRuntime` extend `NodeRuntime`
3. **Channel Wiring**: Manual channel assignment (`runtime.inputChannel = channel`)
4. **Factory Pattern**: `CodeNodeFactory.createContinuousGenerator<T>` returns typed runtime

### New Patterns for Feature 015

1. **ProcessResult Types**: Nullable fields for selective output
   ```kotlin
   data class ProcessResult2<U, V>(val out1: U?, val out2: V?)
   data class ProcessResult3<U, V, W>(val out1: U?, val out2: V?, val out3: W?)
   ```

2. **Multi-Input Runtime**: Wait for all inputs synchronously before processing
   ```kotlin
   class In2Out1Runtime<A, B, R>(
       codeNode: CodeNode,
       private val process: suspend (A, B) -> R
   ) : NodeRuntime<A>(codeNode) {
       var inputChannel2: ReceiveChannel<B>? = null
       // Receive from both, then invoke process
   }
   ```

3. **Factory Method Naming**: `createIn{N}Out{M}Processor<types...>`
   - `createIn1Out1Processor<A, R>` (alias for createContinuousTransformer)
   - `createIn2Out1Processor<A, B, R>`
   - `createIn1Out2Processor<A, U, V>` returns ProcessResult2

### Scope: Valid Configurations (15 total)

| Inputs | Outputs | Factory Method | Notes |
|--------|---------|----------------|-------|
| 0 | 1 | `createOut1Generator` | Alias for createContinuousGenerator |
| 0 | 2 | `createOut2Generator` | Returns ProcessResult2 |
| 0 | 3 | `createOut3Generator` | Returns ProcessResult3 |
| 1 | 0 | `createIn1Sink` | Alias for createContinuousSink |
| 1 | 1 | `createIn1Out1Processor` | Alias for createContinuousTransformer |
| 1 | 2 | `createIn1Out2Processor` | Returns ProcessResult2 |
| 1 | 3 | `createIn1Out3Processor` | Returns ProcessResult3 |
| 2 | 0 | `createIn2Sink` | 2 input channels |
| 2 | 1 | `createIn2Out1Processor` | 2 input channels |
| 2 | 2 | `createIn2Out2Processor` | Returns ProcessResult2 |
| 2 | 3 | `createIn2Out3Processor` | Returns ProcessResult3 |
| 3 | 0 | `createIn3Sink` | 3 input channels |
| 3 | 1 | `createIn3Out1Processor` | 3 input channels |
| 3 | 2 | `createIn3Out2Processor` | Returns ProcessResult2 |
| 3 | 3 | `createIn3Out3Processor` | Returns ProcessResult3 |

**Invalid**: 0 inputs AND 0 outputs (rejected by factory)

## Implementation Strategy

### Phase 0: ProcessResult Types (Priority: Foundation)
Create ProcessResult data classes with nullable fields and destructuring support.

### Phase 1: Multi-Input Runtimes (Priority: P1)
Create runtime classes for 2-input and 3-input nodes with synchronous receive pattern.

### Phase 2: Multi-Output Runtimes (Priority: P2)
Extend runtimes to support multiple typed output channels with ProcessResult dispatch.

### Phase 3: Factory Methods (Priority: P1-P2)
Add factory methods to CodeNodeFactory for all 15 valid configurations.

### Phase 4: Integration (Priority: P3)
Test integration with existing StopWatch components and flow graphs.

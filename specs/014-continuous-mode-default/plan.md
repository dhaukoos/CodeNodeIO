# Implementation Plan: Continuous Mode as Default

**Branch**: `014-continuous-mode-default` | **Date**: 2026-02-13 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/014-continuous-mode-default/spec.md`

## Summary

Refactor CodeNodeFactory to create continuous-mode nodes by default, where nodes run in coroutine loops that process channels rather than single invocations. This leverages the `nodeControlJob` and lifecycle methods from feature 013 to provide unified lifecycle management for all factory-created nodes.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0
**Storage**: N/A (in-memory FlowGraph models)
**Testing**: kotlin.test + JUnit5 + kotlinx-coroutines-test (runTest, advanceTimeBy)
**Target Platform**: JVM + iOS (via KMP)
**Project Type**: Multi-module KMP library (fbpDsl core, StopWatch example)
**Performance Goals**: Channel operations < 1ms overhead, no memory leaks under sustained load
**Constraints**: Must work with virtual time testing, graceful shutdown within 100ms
**Scale/Scope**: Supporting flows with 10-50 nodes, 1000+ messages/second throughput

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | ✅ PASS | Factory methods use clear naming, type-safe generics |
| II. Test-Driven Development | ✅ PASS | Tests written for each factory method before implementation |
| III. User Experience Consistency | ✅ PASS | API follows existing factory patterns |
| IV. Performance Requirements | ✅ PASS | Channel overhead benchmarked, backpressure prevents memory issues |
| V. Observability & Debugging | ✅ PASS | Execution state observable via CodeNode.executionState |
| Licensing & IP | ✅ PASS | All dependencies Apache 2.0 compatible |

**Gate Result**: PASS - No violations requiring justification

## Project Structure

### Documentation (this feature)

```text
specs/014-continuous-mode-default/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (via /speckit.tasks)
```

### Source Code (repository root)

```text
fbpDsl/
├── src/
│   ├── commonMain/kotlin/io/codenode/fbpdsl/
│   │   ├── model/
│   │   │   ├── CodeNode.kt              # MODIFIED: Remove lifecycle methods (moved to runtime)
│   │   │   └── CodeNodeFactory.kt       # MODIFIED: Add continuous factory methods
│   │   ├── runtime/                     # NEW: Runtime execution package
│   │   │   ├── NodeRuntime.kt           # NEW: Execution wrapper with lifecycle
│   │   │   └── ContinuousTypes.kt       # NEW: Type aliases for continuous blocks
│   │   └── usecase/
│   │       └── TypedUseCases.kt         # Existing base classes
│   └── commonTest/kotlin/io/codenode/fbpdsl/
│       ├── model/
│       │   └── CodeNodeLifecycleTest.kt # RENAMED → NodeRuntimeTest.kt
│       └── runtime/
│           ├── NodeRuntimeTest.kt       # MOVED: Tests for lifecycle (from CodeNodeLifecycleTest)
│           └── ContinuousFactoryTest.kt # NEW: Tests for continuous factory
│
StopWatch/
├── src/
│   └── commonMain/kotlin/io/codenode/stopwatch/
│       └── usecases/
│           ├── TimerEmitterComponent.kt      # MODIFIED: codeNode → nodeRuntime
│           └── DisplayReceiverComponent.kt   # MODIFIED: codeNode → nodeRuntime
└── src/
    └── commonTest/kotlin/io/codenode/stopwatch/
        └── usecases/
            └── ChannelIntegrationTest.kt     # Validates with NodeRuntime
```

**Structure Decision**:
1. Create new `runtime/` package for execution concerns
2. Move lifecycle from CodeNode to NodeRuntime
3. Refactor StopWatch components to use NodeRuntime
4. Rename/relocate CodeNodeLifecycleTest to NodeRuntimeTest

## Key Design Decisions

### D1: NodeRuntime as Execution Wrapper

**Decision**: Create `NodeRuntime` class that owns all runtime state and lifecycle methods.

**Rationale**:
- Keeps CodeNode as a pure serializable model (no `@Transient` hacks)
- Centralizes lifecycle control (start/stop/pause/resume) in one place
- "Runtime" better describes the class than "Wiring"

### D2: Move Lifecycle from CodeNode to NodeRuntime

**Decision**: Relocate feature 013's lifecycle additions from CodeNode to NodeRuntime.

**Rationale**:
- Feature 013 added `nodeControlJob`, `start()`, `stop()`, `pause()`, `resume()` to CodeNode
- These are runtime concerns, not model properties
- CodeNode data class shouldn't have `@Transient var` properties
- Clean separation: CodeNode = schema, NodeRuntime = execution

**Moved to NodeRuntime**:
- `nodeControlJob: Job?`
- `executionState: ExecutionState` (mutable runtime copy)
- `start(scope, processingBlock)`
- `stop()`
- `pause()`
- `resume()`
- `isRunning()`, `isPaused()`, `isIdle()`

### D3: Channel Storage Location

**Decision**: Channels stored on NodeRuntime, not in CodeNode data class.

**Rationale**: CodeNode is serializable; channels are runtime-only. Separation maintains clean serialization.

### D4: Backward Compatibility Strategy

**Decision**: Keep existing factory methods, add new `createContinuous*` methods alongside.

**Rationale**: Non-breaking change allows gradual migration. Deprecate old methods in future release.

### D5: Default Channel Buffer Size

**Decision**: Use `Channel.BUFFERED` (64 elements) as default.

**Rationale**: Provides reasonable backpressure without excessive memory. Configurable per-node.

## Complexity Tracking

> No Constitution violations requiring justification.

| Item | Complexity | Mitigation |
|------|------------|------------|
| Continuous loop semantics | Medium | Well-documented patterns, comprehensive tests |
| Channel wiring | Medium | NodeWiring class isolates complexity |
| Backward compatibility | Low | Additive API changes only |

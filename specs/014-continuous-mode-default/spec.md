# Feature Specification: Continuous Mode as Default

**Feature ID**: 014-continuous-mode-default
**Status**: Draft
**Created**: 2026-02-13
**Dependencies**: 013-node-control-extraction (nodeControlJob, start/stop/pause/resume)

## Problem Statement

The current CodeNodeFactory creates nodes with a **single-invocation** execution model where `ProcessingLogic` is called once per input packet. However, real FBP components typically run as **continuous loops** that:

1. Listen on input channels (or generate on a schedule)
2. Process data as it arrives
3. Emit to output channels
4. Continue until stopped

The StopWatch components (TimerEmitterComponent, DisplayReceiverComponent) had to implement their own lifecycle management outside the factory because the factory doesn't support continuous execution. Feature 013 extracted this lifecycle control into CodeNode, but the factory still creates single-invocation nodes.

## User Stories

### US1: Continuous Generator Nodes (Priority: P1)
**As a** flow developer
**I want** to create generator nodes that emit continuously
**So that** I can build timer, sensor, and event-source components using the factory

**Acceptance Criteria**:
- Factory creates generators that run in a continuous loop
- Generator receives scope and emit function for controlled output
- Generator respects `isActive` check for graceful shutdown
- Generator uses CodeNode's nodeControlJob for lifecycle

### US2: Continuous Sink Nodes (Priority: P1)
**As a** flow developer
**I want** to create sink nodes that collect from channels continuously
**So that** I can build display, logger, and consumer components using the factory

**Acceptance Criteria**:
- Factory creates sinks that iterate over input channels
- Sink handles channel closure gracefully
- Sink uses CodeNode's nodeControlJob for lifecycle
- Sink supports typed channels with backpressure

### US3: Continuous Transformer Nodes (Priority: P2)
**As a** flow developer
**I want** to create transformer nodes that process streams continuously
**So that** I can build real-time data processing pipelines

**Acceptance Criteria**:
- Factory creates transformers that read from input channels and write to output channels
- Transformer processes items as they arrive (not batched)
- Transformer respects pause/resume for flow control

### US4: Channel-Based Communication (Priority: P1)
**As a** flow developer
**I want** nodes to communicate via typed channels by default
**So that** I get backpressure, type safety, and FBP semantics automatically

**Acceptance Criteria**:
- Nodes have typed input/output channels instead of just ports
- Channel capacity is configurable (default: buffered)
- Channel closure propagates through the flow graph
- Existing InformationPacket model works with channels

### US5: Backward Compatibility (Priority: P2)
**As a** developer with existing flows
**I want** single-invocation mode to remain available
**So that** my existing code continues to work

**Acceptance Criteria**:
- Factory retains methods for single-invocation nodes (renamed or flagged)
- Existing ProcessingLogic implementations work unchanged
- Migration path documented for upgrading to continuous mode

## Functional Requirements

### FR1: Continuous Execution Model
- Nodes run in coroutine loops managed by `nodeControlJob`
- Loops check `isActive` and execution state for graceful shutdown
- Pause/resume supported via execution state checks in loop

### FR2: Channel Integration
- Input channels: `ReceiveChannel<T>` for typed input
- Output channels: `SendChannel<T>` for typed output
- Channel wiring handled by FlowGraph orchestration
- Support for fan-out (one output to multiple inputs) and fan-in (multiple inputs to one output)

### FR3: Factory Method Signatures

**Generator**:
```
createGenerator<T>(name, tick: suspend (emit: suspend (T) -> Unit) -> Unit)
```

**Sink**:
```
createSink<T>(name, process: suspend (T) -> Unit)
```

**Transformer**:
```
createTransformer<TIn, TOut>(name, transform: suspend (TIn) -> TOut)
```

### FR4: Lifecycle Delegation
- All factory-created nodes use CodeNode's lifecycle methods
- `start(scope)` begins the continuous loop
- `stop()` cancels the loop gracefully
- `pause()/resume()` control loop execution

## Non-Functional Requirements

### NFR1: Performance
- Channel operations should not introduce significant overhead
- Backpressure should prevent memory exhaustion under load

### NFR2: Testability
- Continuous nodes testable with virtual time (runTest, advanceTimeBy)
- Channel-based communication mockable for unit tests

### NFR3: Type Safety
- Channel types enforced at compile time
- No unchecked casts in generated factory code

## Success Criteria

1. TimerEmitterComponent can be replaced with `CodeNodeFactory.createGenerator<TimerOutput>()`
2. DisplayReceiverComponent can be replaced with `CodeNodeFactory.createSink<TimerOutput>()`
3. ChannelIntegrationTest passes with factory-created nodes
4. All existing tests continue to pass (backward compatibility)
5. New factory methods documented with examples

## Out of Scope

- Distributed channel communication (network transport)
- Persistent channels (durable messaging)
- Complex routing patterns (content-based routing, scatter-gather)
- Back-pressure strategies beyond buffered channels

## Open Questions

1. Should continuous mode completely replace single-invocation, or coexist?
2. What should the default channel buffer size be?
3. How should errors in continuous loops be handled (retry, skip, fail-fast)?
4. Should channels be part of the CodeNode data class or separate wiring?

## Dependencies

- **013-node-control-extraction**: Provides lifecycle methods - these will be RELOCATED from CodeNode to NodeRuntime
- **012-channel-connections**: Provides typed channel infrastructure

## Refactoring Scope

This feature includes relocating feature 013's lifecycle additions:

| Item | From (013) | To (014) |
|------|------------|----------|
| `nodeControlJob: Job?` | CodeNode | NodeRuntime |
| `start(scope, block)` | CodeNode | NodeRuntime |
| `stop()` | CodeNode | NodeRuntime |
| `pause()` | CodeNode | NodeRuntime |
| `resume()` | CodeNode | NodeRuntime |
| `executionState` (mutable) | CodeNode (via copy) | NodeRuntime (direct) |

**Rationale**: CodeNode should be a pure serializable model. Runtime execution state belongs in NodeRuntime.

## Risks

1. **Breaking changes**: Existing factory usage may need updates
2. **Complexity**: Continuous loops are harder to reason about than single invocations
3. **Resource leaks**: Improper shutdown could leave orphaned coroutines

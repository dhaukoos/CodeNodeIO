# Research: Node Control Extraction

**Feature Branch**: `013-node-control-extraction`
**Date**: 2026-02-13
**Spec**: [spec.md](./spec.md)

## Summary

This feature extracts common node lifecycle control (job management, execution state, start/stop/pause/resume) from individual component implementations (TimerEmitterComponent, DisplayReceiverComponent) into the base CodeNode class.

## Technical Findings

### Current Architecture Analysis

**CodeNode (fbpDsl/.../model/CodeNode.kt)**:
- Is a `data class` with `@Serializable` annotation
- Has `executionState: ExecutionState = ExecutionState.IDLE` property
- Has `@Transient val processingLogic: ProcessingLogic? = null`
- Has helper methods: `isRunning()`, `isPaused()`, `isError()`, `isIdle()`, `canAcceptPackets()`
- Has `withExecutionState(newState, propagate)` method returning new CodeNode
- Has `process(inputs)` suspend function that invokes processingLogic
- **Missing**: `nodeControlJob`, `start(scope)`, `stop()` methods

**TimerEmitterComponent (StopWatch/.../usecases/TimerEmitterComponent.kt)**:
- Has `var executionState: ExecutionState = ExecutionState.IDLE`
- Has `private var timerJob: Job? = null`
- Has `suspend fun start(scope: CoroutineScope)` that:
  - Cancels existing timerJob
  - Launches new coroutine in provided scope
  - Runs while `isActive && executionState == RUNNING`
- Has `fun stop()` that:
  - Sets `executionState = ExecutionState.IDLE`
  - Cancels timerJob
  - Sets timerJob to null

**DisplayReceiverComponent (StopWatch/.../usecases/DisplayReceiverComponent.kt)**:
- Has `private var collectionJob: Job? = null`
- Has `suspend fun start(scope: CoroutineScope)` that:
  - Cancels existing collectionJob
  - Launches new coroutine in provided scope for channel iteration
- Has `fun stop()` that:
  - Cancels collectionJob
  - Sets collectionJob to null

### Key Design Decision: Data Class vs Mutable State

**Challenge**: CodeNode is a `data class` with immutable semantics, but Job management requires mutable state.

**Options Considered**:

1. **Add @Transient var nodeControlJob: Job?** to CodeNode
   - Pros: Simple, direct extraction from components
   - Cons: Breaks data class immutability pattern

2. **Create NodeController wrapper class**
   - Pros: Maintains CodeNode immutability
   - Cons: Adds another layer of indirection

3. **Use @Transient var with clear documentation**
   - Pros: Pragmatic, follows existing pattern (coroutineHandle already exists)
   - Cons: Mixed paradigm (data class with mutable runtime state)

**Decision**: Option 3 - Use `@Transient var nodeControlJob: Job? = null`

**Rationale**:
- CodeNode already has `@Transient` fields (`inputPorts`, `outputPorts`, `processingLogic`)
- The `coroutineHandle: String?` property already exists for runtime reference
- Job is a runtime construct that cannot be serialized anyway
- The pattern is consistent with existing architecture

### Processing Logic Customization

Components implement `ProcessingLogic` interface but also need custom processing loops:
- TimerEmitter: delay-based tick loop
- DisplayReceiver: channel iteration loop

**Solution**: CodeNode.start() provides the job management skeleton, but the actual processing loop is defined by the component via a callback or delegation pattern.

### Pause/Resume Implementation

**Challenge**: Pause needs to suspend processing without cancelling the job.

**Options Considered**:

1. **Use Mutex or Semaphore**
   - Pros: True coroutine suspension
   - Cons: More complex, requires coordination

2. **Polling with delay in processing loop**
   - Pros: Simple, processing loop checks `isPaused()` and waits
   - Cons: Slight delay in pause response (polling interval)

3. **Use StateFlow for pause signal**
   - Pros: Reactive, immediate response
   - Cons: Requires flow collection in processing loop

**Decision**: Option 2 - Polling with delay

**Rationale**:
- Simple to implement and understand
- Processing loops already check `isRunning()`, adding `isPaused()` check is natural
- Small polling delay (50ms) is acceptable for stopwatch use case
- No additional synchronization primitives needed

### Existing Test Coverage

- **TimerEmitterComponentTest**: 4 tests covering:
  - Incrementing seconds emission
  - Minute rollover at 60 seconds
  - Stop emitting when state changes
  - Initial values

- **DisplayReceiverComponentTest**: 3 tests covering:
  - State updates on receive
  - Initial values
  - Continuous updates

- **ChannelIntegrationTest**: End-to-end channel tests

**Critical**: All existing tests must pass without modification to test expectations.

## Dependencies

- **kotlinx.coroutines**: Already in use (Job, CoroutineScope, launch)
- **ExecutionState**: Existing enum, sufficient for lifecycle management
- No new dependencies required

## Resolved Clarifications

| Question | Resolution |
|----------|------------|
| Can CodeNode have mutable Job state? | Yes, via @Transient var, consistent with existing pattern |
| Should start() be suspend function? | Yes, consistent with component implementations |
| How do components customize processing? | Via ProcessingLogic and custom start() override/delegation |
| Does ExecutionState need extension? | No, existing states (IDLE, RUNNING, PAUSED, ERROR) are sufficient |
| How does pause() suspend without cancelling? | Processing loop polls isPaused() and delays until resumed |
| Should pause/resume be suspend functions? | No, they only update state; processing loop handles suspension |

## Risk Assessment

| Risk | Mitigation |
|------|------------|
| Breaking existing tests | TDD approach - run tests after each change |
| Changing public API | Internal refactoring only, no external API changes |
| Data class semantics | Document @Transient Job as runtime state |

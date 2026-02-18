# Research: Refactor TimerEmitterComponent

**Feature**: 020-refactor-timer-emitter
**Date**: 2026-02-18

## R1: How to Abstract the Timed Generation Loop

**Decision**: Add a `tickIntervalMs` parameter to `Out2GeneratorRuntime` and implement an internal timed loop when provided. The runtime's `start()` method wraps the user's tick function in a while-loop with pause hooks, delay management, and state checks.

**Rationale**: The `Out2GeneratorRuntime` already owns the execution loop (it launches the coroutine and calls the generator block). Adding an optional tick interval moves the while-loop + delay + pause logic from user code into the runtime, where it belongs. This avoids creating a new class hierarchy while keeping the existing `Out2GeneratorBlock` signature for non-timed generators.

**Alternatives Considered**:
- **New TimedOut2GeneratorRuntime subclass**: Would add a class to the hierarchy for each arity (TimedGeneratorRuntime, TimedOut2GeneratorRuntime, TimedOut3GeneratorRuntime). Rejected: class proliferation for a single parameter.
- **Wrapper/decorator pattern**: Would wrap Out2GeneratorRuntime and intercept start(). Rejected: adds indirection without simplifying the user API.
- **Tick function as a separate type alias**: A `TimedOut2GeneratorBlock<U, V>` that receives no `emit` parameter and returns `ProcessResult2<U, V>`. Rejected initially but reconsidered - this is actually the cleanest approach as it makes the contract explicit: the tick function is called once per interval and returns the value to emit. **Revised decision: use this approach.**

**Final Decision**: Introduce a `Out2TickBlock<U, V>` type alias: `suspend () -> ProcessResult2<U, V>` — a simple function that returns what to emit. The runtime calls it at each tick interval, handles pause/resume, and emits the result. A new factory method `createTimedOut2Generator()` creates this variant.

## R2: What Lifecycle Boilerplate Can Be Eliminated from Components

**Decision**: Components should delegate all lifecycle properties and methods through a thin wrapper pattern, or expose the runtime directly. The refactored TimerEmitterComponent will:
1. Not contain any `ExecutionState` references in its business logic
2. Delegate `executionState`, `registry`, `start()`, `stop()`, and channel properties to the runtime
3. Only contain: StateFlow declarations, the tick function, and `reset()` logic

**Rationale**: Analysis shows TimerEmitterComponent is 63% boilerplate (property delegation, lifecycle forwarding). The tick function (incrementer + StateFlow updates) is the only unique business logic. The component's generator block currently duplicates the pause hook that `Out2GeneratorRuntime.emit()` already provides.

**Alternatives Considered**:
- **Eliminate component entirely, use runtime directly**: Rejected. Components provide domain-specific naming (elapsedSecondsFlow, reset()) that runtimes can't provide.
- **Generate components from metadata**: Future possibility but out of scope for this refactoring.

## R3: Impact on Existing Tests

**Decision**: Existing test assertions should continue to pass. Test setup may need minor updates to use the new factory method (`createTimedOut2Generator` instead of `createOut2Generator` with manual timing loop).

**Rationale**: The refactoring changes internal structure, not observable behavior. Tests that verify tick timing, pause/resume, reset, and channel flow should all pass with the new implementation.

**Test files affected**:
- `TimerEmitterComponentTest.kt`: May need setup changes if component API changes
- `ChannelIntegrationTest.kt`: May need wiring order updates (already done for the bug fix)
- `ContinuousFactoryTest.kt`: Unaffected (tests single-output GeneratorRuntime)
- `TypedNodeRuntimeTest.kt`: Unaffected (tests runtime classes directly)

## R4: DisplayReceiverComponent Cleanup

**Decision**: DisplayReceiverComponent's consumer block is already minimal (4 lines). The refactoring will remove commented-out code and ensure the delegation pattern is consistent with TimerEmitterComponent, but no structural changes are needed to the consumer logic.

**Rationale**: The sink runtime (`In2SinkRuntime`) already handles the receive loop, pause hooks, and channel management. The component only needs its consumer block and StateFlow declarations.

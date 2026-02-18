# Data Model: Refactor TimerEmitterComponent

**Feature**: 020-refactor-timer-emitter
**Date**: 2026-02-18

## Entities

### Out2TickBlock<U, V> (New Type Alias)

A tick function that is called once per interval and returns the values to emit.

- **Signature**: `suspend () -> ProcessResult2<U, V>`
- **Relationship**: Used by `Out2GeneratorRuntime` when tick interval is configured
- **Constraints**: Must return a non-null ProcessResult2; null fields within the result cause selective sending (existing behavior)

### Out2GeneratorRuntime<U, V> (Modified)

Extended to support an optional timed tick mode alongside the existing generator block mode.

- **New field**: `tickIntervalMs: Long` — tick interval in milliseconds (0 = no internal loop, use generator block as-is)
- **New field**: `tickBlock: Out2TickBlock<U, V>?` — the user's per-tick function (alternative to `generate` block)
- **Behavior change**: When `tickIntervalMs > 0` and `tickBlock` is set, `start()` runs an internal while-loop with delay, pause hooks, and state management, calling `tickBlock()` each iteration and emitting the result
- **Backward compatibility**: When `tickIntervalMs == 0` or `tickBlock` is null, behavior is identical to current implementation (user-provided generator block with manual loop)

### CodeNodeFactory (Modified)

Extended with a new factory method for timed generators.

- **New method**: `createTimedOut2Generator<U, V>(name, tickIntervalMs, tick, ...)` — creates an Out2GeneratorRuntime configured for timed tick mode
- **Existing methods**: `createOut2Generator` remains unchanged for backward compatibility

### TimerEmitterComponent (Refactored)

Simplified to contain only domain-specific logic.

- **Retained**: `_elapsedSeconds: MutableStateFlow<Int>`, `_elapsedMinutes: MutableStateFlow<Int>`, `elapsedSecondsFlow`, `elapsedMinutesFlow`
- **Retained**: `incrementer()` function (seconds/minutes rollover logic)
- **Retained**: `reset()` method (resets StateFlows to zero and stops runtime)
- **Removed**: Generator block with manual while-loop, pause hooks, state checks
- **Replaced with**: A tick function that calls `incrementer()`, updates StateFlows, and returns the ProcessResult2
- **Delegation**: `executionState`, `registry`, `codeNode`, `outputChannel1`, `outputChannel2`, `start()`, `stop()` delegate to the runtime

### DisplayReceiverComponent (Cleaned Up)

- **Removed**: Commented-out code blocks
- **Retained**: Consumer block, StateFlow declarations, property delegation
- **No structural changes**: Already follows the target pattern

## State Transitions

No changes to execution state machine. The runtime's existing states (IDLE -> RUNNING <-> PAUSED -> IDLE) remain unchanged.

The only behavioral change is WHERE the state checking happens:
- **Before**: Component's generator block checks `executionState` manually
- **After**: Runtime's internal timed loop checks `executionState` automatically

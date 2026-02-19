# Contract: Channel Naming Convention

**Feature**: 021-refactor-noderuntime-base
**Date**: 2026-02-19

## Input Channel Naming

| Channels | Property Names |
|----------|---------------|
| 1 input  | `inputChannel` |
| 2 inputs | `inputChannel1`, `inputChannel2` |
| 3 inputs | `inputChannel1`, `inputChannel2`, `inputChannel3` |

## Output Channel Naming

| Channels | Property Names                                       |
|----------|------------------------------------------------------|
| 1 output (Generator) | `outputChannel` (no number suffix)                   |
| 1 output (Transformer) | `outputChannel` (no number suffix)            |
| 1 output (Processor) | `outputChannel` (no number suffix)             |
| 1 output (Filter) | `outputChannel` (no number suffix)                   |
| 2 outputs | `outputChannel1`, `outputChannel2`                   |
| 3 outputs | `outputChannel1`, `outputChannel2`, `outputChannel3` |

## Base Class Contract

`NodeRuntime` provides lifecycle management ONLY:
- `executionState`: IDLE, RUNNING, PAUSED, ERROR
- `nodeControlJob`: Coroutine job reference
- `start(scope, processingBlock)`: Launches processing coroutine
- `stop()`: Cancels job, sets IDLE
- `pause()` / `resume()`: State transitions
- `registry`: Optional RuntimeRegistry for centralized control

`NodeRuntime` does NOT provide:
- Channel properties (moved to subclasses)
- Generic type parameters (subclasses define their own)
- Channel lifecycle management (subclasses handle in their `start()` overrides)

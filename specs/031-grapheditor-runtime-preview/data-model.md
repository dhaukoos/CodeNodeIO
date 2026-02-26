# Data Model: GraphEditor Runtime Preview

**Feature**: 031-grapheditor-runtime-preview
**Date**: 2026-02-25

## Entities

### RuntimeSession

Represents an active or idle runtime preview session within the graphEditor.

| Field | Type | Description |
|-------|------|-------------|
| executionState | ExecutionState | Current state: IDLE, RUNNING, PAUSED |
| attenuationDelayMs | Long | Added delay in ms per tick cycle (0 = nominal speed, max 5000) |
| controller | StopWatchControllerInterface? | Active controller (null when no module loaded) |
| viewModel | StopWatchViewModel? | Active view model for UI binding (null when no module loaded) |

**State Transitions**:
```
IDLE → RUNNING (Start)
RUNNING → PAUSED (Pause)
PAUSED → RUNNING (Resume)
RUNNING → IDLE (Stop)
PAUSED → IDLE (Stop)
RUNNING → IDLE (Graph edited while running)
```

**Validation Rules**:
- `attenuationDelayMs` must be in range [0, 5000]
- Start is only valid from IDLE state
- Pause is only valid from RUNNING state
- Resume is only valid from PAUSED state
- Stop is valid from RUNNING or PAUSED states

### NodeRuntime (modified)

Existing entity in fbpDsl. Adding one new field.

| New Field | Type | Description |
|-----------|------|-------------|
| attenuationDelayMs | Long? | When non-null, replaces tickIntervalMs as the delay in timed generator loops. Default: null (use tickIntervalMs) |

**Impact**: This field is added to the base `NodeRuntime` class. Timed generator loops use `delay(attenuationDelayMs ?: tickIntervalMs)`. When null (default), existing behavior is unchanged. When set to 0, generators run with no delay. All other runtime types ignore it.

## Relationships

```
RuntimeSession 1--1 StopWatchController
RuntimeSession 1--1 StopWatchViewModel
StopWatchController 1--1 StopWatchFlow
StopWatchFlow 1--* NodeRuntime (timerEmitter, displayReceiver)
NodeRuntime has attenuationDelayMs
```

## State Flow Diagram

```
GraphEditor ←→ RuntimePreviewPanel (Compose UI)
                    ↓
             RuntimeSession (circuitSimulator)
                    ↓
          StopWatchViewModel → StopWatchController → StopWatchFlow
                                                         ↓
                                              TimerEmitter (Out2GeneratorRuntime)
                                              DisplayReceiver (In2SinkRuntime)
                                                         ↓
                                              StateProperties → StateFlows → UI Preview
```

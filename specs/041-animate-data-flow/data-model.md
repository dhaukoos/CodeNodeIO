# Data Model: Animate Data Flow

**Feature**: 041-animate-data-flow
**Date**: 2026-03-06

## Entities

### EmissionEvent

Represents a single IP emission from a runtime node's output port. Created by the `onEmit` callback in `NodeRuntime` and consumed by `DataFlowAnimationController` to create animations.

| Field | Type | Description |
|-------|------|-------------|
| nodeId | String | ID of the CodeNode that emitted the IP |
| portIndex | Int | Zero-based index of the output port that emitted |

**Lifecycle**: Transient — created on emission, consumed immediately by the animation controller.

### ConnectionAnimation

Represents an active dot animation traveling along a connection curve. Created by `DataFlowAnimationController` when an emission event maps to a connection.

| Field | Type | Description |
|-------|------|-------------|
| connectionId | String | ID of the Connection being animated |
| startTimeMs | Long | System time (ms) when the animation started |
| durationMs | Long | Total duration of the animation (80% of attenuationMs) |

**Lifecycle**: Created on emission → active while `elapsed < durationMs` → removed when complete.

**Derived Properties**:
- `progress: Float` = `(currentTimeMs - startTimeMs) / durationMs` clamped to [0, 1]
- Position on curve: Computed by the graphEditor using `cubicBezier(P0, P1, P2, P3, progress)`

### AnimationState

The aggregate state exposed to the rendering layer via `StateFlow`.

| Field | Type | Description |
|-------|------|-------------|
| activeAnimations | List\<ConnectionAnimation\> | Currently active dot animations |

**Lifecycle**: Continuously updated by `DataFlowAnimationController`. Cleared on stop, frozen on pause, resumed on unpause.

## Relationships

```text
EmissionEvent ─(maps to)─> Connection(s) ─(creates)─> ConnectionAnimation(s)
                              │
                              ▼
                     FlowGraph.connections
                   (lookup by sourceNodeId + sourcePortId)
```

### Mapping: EmissionEvent → Connection(s)

When a node emits on output port index N:
1. Find the CodeNode by `nodeId` from the FlowGraph
2. Get the port ID at index N from `codeNode.outputPorts[portIndex]`
3. Find all connections where `sourceNodeId == nodeId && sourcePortId == portId`
4. Create a `ConnectionAnimation` for each matching connection

**Note**: A single output port may have multiple connections (fan-out), so one emission can create multiple animations.

## State Transitions

### DataFlowAnimationController States

```text
                 ┌──────────────────────┐
                 │                      │
    start()      ▼      pause()         │  resume()
  ────────> [OBSERVING] ────────> [PAUSED] ────────┘
                 │
    stop()       │
                 ▼
            [INACTIVE]
```

- **INACTIVE**: No emissions observed, no animations running. Initial state.
- **OBSERVING**: Actively receiving emission events and managing animations.
- **PAUSED**: Emission events are ignored, existing animations freeze (progress stops advancing).

### ConnectionAnimation Lifecycle

```text
  EmissionEvent ──> [ACTIVE] ──(progress >= 1.0)──> [COMPLETE] ──> removed
                       │
              stop() ──┘──> immediately removed
```

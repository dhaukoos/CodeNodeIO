# Data Model: Migrate Module Runtimes

**Feature**: 052-migrate-module-runtimes
**Date**: 2026-03-16

## Entities

### CodeNodeDefinition (existing interface — no changes)

The `CodeNodeDefinition` interface from feature 050 is used as-is. Each migrated node implements this interface.

| Field | Type | Description |
|-------|------|-------------|
| name | String | Unique node name for registry lookup |
| category | NodeCategory | SOURCE, TRANSFORMER, PROCESSOR, or SINK |
| description | String? | Human-readable description |
| inputPorts | List\<PortSpec\> | Input port specifications |
| outputPorts | List\<PortSpec\> | Output port specifications |
| createRuntime(name) | NodeRuntime | Factory method returning configured runtime |

### StopWatch Nodes

#### TimerEmitterCodeNode

| Property | Value |
|----------|-------|
| name | "TimerEmitter" |
| category | SOURCE |
| inputPorts | [] (empty) |
| outputPorts | [PortSpec("elapsedSeconds", Int::class), PortSpec("elapsedMinutes", Int::class)] |
| runtime | SourceOut2Runtime\<Int, Int\> |
| state access | StopWatchState._elapsedSeconds, StopWatchState._elapsedMinutes |

#### TimeIncrementerCodeNode

| Property | Value |
|----------|-------|
| name | "TimeIncrementer" |
| category | PROCESSOR |
| inputPorts | [PortSpec("elapsedSeconds", Int::class), PortSpec("elapsedMinutes", Int::class)] |
| outputPorts | [PortSpec("seconds", Int::class), PortSpec("minutes", Int::class)] |
| runtime | In2Out2Runtime\<Int, Int, Int, Int\> |
| state access | StopWatchState._elapsedSeconds, StopWatchState._elapsedMinutes |

#### DisplayReceiverCodeNode

| Property | Value |
|----------|-------|
| name | "DisplayReceiver" |
| category | SINK |
| inputPorts | [PortSpec("seconds", Int::class), PortSpec("minutes", Int::class)] |
| outputPorts | [] (empty) |
| runtime | SinkIn2AnyRuntime\<Int, Int\> |
| initial values | 0, 0 |
| state access | StopWatchState._seconds, StopWatchState._minutes |

### Entity Module Nodes (UserProfiles / GeoLocations / Addresses)

All three entity modules share an identical 3-node architecture. The table below uses UserProfiles as the example; GeoLocations and Addresses follow the same pattern with their respective entity types and state objects.

#### UserProfileCUDCodeNode

| Property | Value |
|----------|-------|
| name | "UserProfileCUD" |
| category | SOURCE |
| inputPorts | [] (empty) |
| outputPorts | [PortSpec("save", Any::class), PortSpec("update", Any::class), PortSpec("remove", Any::class)] |
| runtime | SourceOut3Runtime\<Any, Any, Any\> |
| state access | UserProfilesState._save, ._update, ._remove |

#### UserProfileRepositoryCodeNode

| Property | Value |
|----------|-------|
| name | "UserProfileRepository" |
| category | PROCESSOR |
| inputPorts | [PortSpec("save", Any::class), PortSpec("update", Any::class), PortSpec("remove", Any::class)] |
| outputPorts | [PortSpec("result", Any::class), PortSpec("error", Any::class)] |
| runtime | In3AnyOut2Runtime\<Any, Any, Any, Any, Any\> |
| initial values | Unit, Unit, Unit |
| DAO access | UserProfilesPersistence.dao (Koin) |

#### UserProfilesDisplayCodeNode

| Property | Value |
|----------|-------|
| name | "UserProfilesDisplay" |
| category | SINK |
| inputPorts | [PortSpec("result", Any::class), PortSpec("error", Any::class)] |
| outputPorts | [] (empty) |
| runtime | SinkIn2Runtime\<Any, Any\> |
| state access | UserProfilesState._result, ._error |

## Relationships

```text
┌──────────────┐     ┌──────────────────┐     ┌─────────────────┐
│ TimerEmitter │────▶│ TimeIncrementer  │────▶│ DisplayReceiver │
│  (Source)    │     │  (Processor)     │     │  (Sink)         │
│  0→2 ports   │     │  2→2 ports       │     │  2→0 ports      │
└──────────────┘     └──────────────────┘     └─────────────────┘

┌──────────────┐     ┌──────────────────┐     ┌─────────────────┐
│ EntityCUD    │────▶│ EntityRepository │────▶│ EntityDisplay   │
│  (Source)    │     │  (Processor)     │     │  (Sink)         │
│  0→3 ports   │     │  3→2 ports       │     │  2→0 ports      │
└──────────────┘     └──────────────────┘     └─────────────────┘
```

## State Transitions

No new state transitions. All nodes use the existing `ExecutionState` lifecycle: `IDLE → RUNNING → PAUSED → RUNNING → IDLE`.

The DynamicPipelineController manages these transitions identically to how it manages EdgeArtFilter nodes — no module-specific lifecycle changes needed.

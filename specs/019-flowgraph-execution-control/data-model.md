# Data Model: Unified FlowGraph Execution Control

**Feature**: 019-flowgraph-execution-control
**Date**: 2026-02-17

## Entities

### RuntimeRegistry (NEW)

Tracks active NodeRuntime instances for a flow, enabling centralized state control.

**Properties**:

| Property | Type | Description |
|----------|------|-------------|
| runtimes | Map<String, NodeRuntime<*>> | Active runtimes keyed by codeNode.id |

**Methods**:

| Method | Parameters | Return | Description |
|--------|------------|--------|-------------|
| register | runtime: NodeRuntime<*> | Unit | Add runtime to registry |
| unregister | runtime: NodeRuntime<*> | Unit | Remove runtime from registry |
| pauseAll | - | Unit | Call pause() on all registered runtimes |
| resumeAll | - | Unit | Call resume() on all registered runtimes |
| stopAll | - | Unit | Call stop() on all runtimes, clear registry |
| count | - | Int | Number of registered runtimes |

**Thread Safety**: Uses ConcurrentHashMap for thread-safe access from coroutines.

### RootControlNode (MODIFIED)

Extended to support RuntimeRegistry integration and resumeAll().

**New Properties**:

| Property | Type | Description |
|----------|------|-------------|
| registry | RuntimeRegistry? | Optional registry for runtime state propagation |

**New Methods**:

| Method | Parameters | Return | Description |
|--------|------------|--------|-------------|
| resumeAll | - | FlowGraph | Set all nodes to RUNNING, call registry.resumeAll() |

**Modified Methods**:

| Method | Change |
|--------|--------|
| pauseAll | Also calls registry?.pauseAll() |
| stopAll | Also calls registry?.stopAll() |
| startAll | Also calls registry?.resumeAll() (for consistency) |

**Factory Method Update**:
```kotlin
companion object {
    fun createFor(
        flowGraph: FlowGraph,
        name: String,
        registry: RuntimeRegistry? = null  // New optional parameter
    ): RootControlNode
}
```

### NodeRuntime (MODIFIED)

Extended to support registry registration on lifecycle events.

**New Properties**:

| Property | Type | Description |
|----------|------|-------------|
| registry | RuntimeRegistry? | Optional registry to register with |

**Modified Methods**:

| Method | Change |
|--------|--------|
| start | Register with registry if provided |
| stop | Unregister from registry if provided |

### ExecutionState (UNCHANGED)

Existing enum with all needed values.

| Value | Description |
|-------|-------------|
| IDLE | Node not currently processing; ready to start |
| RUNNING | Node actively processing InformationPackets |
| PAUSED | Node execution paused; processing loop waiting |
| ERROR | Node encountered an error and stopped execution |

### StopWatchControllerInterface (MODIFIED)

Extended with pause/resume methods.

**New Methods**:

| Method | Return | Description |
|--------|--------|-------------|
| pause() | FlowGraph | Pause the stopwatch |
| resume() | FlowGraph | Resume the stopwatch |

### StopWatchViewModel (MODIFIED)

Extended with pause/resume actions.

**New Methods**:

| Method | Return | Description |
|--------|--------|-------------|
| pause() | FlowGraph | Delegates to controller.pause() |
| resume() | FlowGraph | Delegates to controller.resume() |

## Relationships

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                           Compose UI Layer                               │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  StopWatch Composable                                             │   │
│  │  - Start/Pause/Resume/Stop/Reset buttons                         │   │
│  │  - Derives visibility from executionState                        │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                  │                                       │
│                                  │ observes                              │
│                                  ▼                                       │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  StopWatchViewModel                                               │   │
│  │  - start() / pause() / resume() / stop() / reset()               │   │
│  │  - executionState: StateFlow<ExecutionState>                     │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                  │                                       │
│                                  │ delegates                             │
│                                  ▼                                       │
└─────────────────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────────────────┐
│                         Controller Layer                                 │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  StopWatchController                                              │   │
│  │  - Uses RootControlNode for state management                     │   │
│  │  - Owns RuntimeRegistry                                          │   │
│  │  - Coordinates model + runtime state                             │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                  │                                       │
│                    ┌─────────────┴─────────────┐                        │
│                    ▼                           ▼                        │
│  ┌──────────────────────────┐   ┌──────────────────────────┐           │
│  │  RootControlNode         │   │  RuntimeRegistry          │           │
│  │  - startAll()            │──▶│  - register(runtime)      │           │
│  │  - pauseAll()            │   │  - unregister(runtime)    │           │
│  │  - resumeAll()           │   │  - pauseAll()             │           │
│  │  - stopAll()             │   │  - resumeAll()            │           │
│  │  - Updates FlowGraph     │   │  - stopAll()              │           │
│  └──────────────────────────┘   └──────────────────────────┘           │
│                                              │                          │
│                                              │ calls pause()/resume()   │
│                                              ▼                          │
└─────────────────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────────────────┐
│                          Runtime Layer                                   │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  NodeRuntime Instances                                            │   │
│  │  ┌─────────────────────┐    ┌─────────────────────┐              │   │
│  │  │ TimerEmitter        │───▶│ DisplayReceiver     │              │   │
│  │  │ (GeneratorRuntime)  │    │ (SinkRuntime)       │              │   │
│  │  │ - Pause hook in loop│    │ - Pause hook in loop│              │   │
│  │  │ - Registers on start│    │ - Registers on start│              │   │
│  │  └─────────────────────┘    └─────────────────────┘              │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

## State Transitions

```text
                    ┌─────────┐
                    │  IDLE   │◀────────────────┐
                    └────┬────┘                 │
                         │                      │
                    start()                  stop()
                         │                      │
                         ▼                      │
                    ┌─────────┐            ┌────┴────┐
         ┌─────────▶│ RUNNING │────────────│  IDLE   │
         │          └────┬────┘            └─────────┘
         │               │
      resume()       pause()
         │               │
         │               ▼
         │          ┌─────────┐
         └──────────│ PAUSED  │
                    └────┬────┘
                         │
                      stop()
                         │
                         ▼
                    ┌─────────┐
                    │  IDLE   │
                    └─────────┘
```

## Validation Rules

| Rule | Entity | Description |
|------|--------|-------------|
| Valid state transition | RuntimeRegistry | pause() only valid when RUNNING; resume() only valid when PAUSED |
| Thread safety | RuntimeRegistry | All operations must be thread-safe |
| Cleanup on stop | RuntimeRegistry | stopAll() clears all registrations |
| Registry optional | RootControlNode | Must work without registry for backward compatibility |
| Pause hook present | All *Runtime | Every processing loop must check executionState |

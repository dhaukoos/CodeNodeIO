# Data Model: StopWatch Virtual Circuit Demo

**Feature**: 008-stopwatch-virtual-circuit
**Date**: 2026-02-08

## Overview

This document defines the data entities, relationships, and interfaces for the StopWatch virtual circuit. The model bridges the FBP graph representation with the Compose UI layer.

---

## Entity Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                        StopWatchFlowGraph                           │
│  (FlowGraph container for the virtual circuit)                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────────────┐           ┌──────────────────────┐       │
│  │    TimerEmitter      │           │   DisplayReceiver    │       │
│  │    (CodeNode)        │           │     (CodeNode)       │       │
│  ├──────────────────────┤           ├──────────────────────┤       │
│  │ Inputs: (none)       │           │ Inputs:              │       │
│  │                      │           │  - seconds: Int      │       │
│  │ Outputs:             │           │  - minutes: Int      │       │
│  │  - elapsedSeconds ○──┼───────────┼──○ seconds           │       │
│  │  - elapsedMinutes ○──┼───────────┼──○ minutes           │       │
│  └──────────────────────┘           └──────────────────────┘       │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              │ wrapped by
                              ▼
                    ┌──────────────────────┐
                    │  StopWatchController │
                    │  (RootControlNode)   │
                    ├──────────────────────┤
                    │ + start()            │
                    │ + stop()             │
                    │ + pause()            │
                    │ + reset()            │
                    │ + getStatus()        │
                    │ + isRunning: Boolean │
                    │ + bindToLifecycle()  │
                    └──────────────────────┘
```

---

## Entities

### 1. StopWatchFlowGraph

**Description**: The top-level container representing the virtual circuit.

**Attributes**:
| Field | Type | Description |
|-------|------|-------------|
| id | String | Unique identifier (UUID) |
| name | String | "StopWatch" |
| version | String | Semantic version (e.g., "1.0.0") |
| description | String | "Virtual circuit demo for stopwatch functionality" |
| rootNodes | List<Node> | Contains TimerEmitter and DisplayReceiver |
| connections | List<Connection> | Two connections linking outputs to inputs |
| metadata | Map<String, String> | Author, creation date, etc. |
| targetPlatforms | Set<TargetPlatform> | ANDROID, IOS, DESKTOP |

**Validation Rules**:
- name is non-empty
- version follows semantic versioning (X.Y.Z)
- All connections reference valid port IDs
- No orphaned nodes (every node connected or explicitly marked standalone)

---

### 2. TimerEmitter (CodeNode)

**Description**: Generator node that emits time values at regular intervals.

**Attributes**:
| Field | Type | Description |
|-------|------|-------------|
| id | String | Unique identifier |
| name | String | "TimerEmitter" |
| type | NodeType | GENERATOR |
| inputPorts | List<Port> | Empty list (no inputs) |
| outputPorts | List<Port> | [elapsedSeconds, elapsedMinutes] |
| executionState | ExecutionState | Current state (IDLE, RUNNING, PAUSED, ERROR) |
| controlConfig | ControlConfig | speedAttenuation=1000, etc. |

**Output Ports**:
| Port ID | Name | Type | Description |
|---------|------|------|-------------|
| elapsedSeconds | Elapsed Seconds | Int | Seconds counter (0-59) |
| elapsedMinutes | Elapsed Minutes | Int | Minutes counter (0-∞) |

**UseCase Behavior**:
```kotlin
suspend fun process() {
    var seconds = 0
    var minutes = 0

    while (executionState == ExecutionState.RUNNING) {
        delay(controlConfig.speedAttenuation)
        seconds += 1
        if (seconds >= 60) {
            seconds = 0
            minutes += 1
        }
        emit("elapsedSeconds", seconds)
        emit("elapsedMinutes", minutes)
    }
}
```

---

### 3. DisplayReceiver (CodeNode)

**Description**: Sink node that receives time values and triggers UI rendering.

**Attributes**:
| Field | Type | Description |
|-------|------|-------------|
| id | String | Unique identifier |
| name | String | "DisplayReceiver" |
| type | NodeType | SINK |
| inputPorts | List<Port> | [seconds, minutes] |
| outputPorts | List<Port> | Empty list (no outputs) |
| executionState | ExecutionState | Current state |
| controlConfig | ControlConfig | Default configuration |

**Input Ports**:
| Port ID | Name | Type | Description |
|---------|------|------|-------------|
| seconds | Seconds | Int | Current seconds value |
| minutes | Minutes | Int | Current minutes value |

**UseCase Behavior**:
```kotlin
suspend fun process(seconds: Int, minutes: Int) {
    // Updates Compose state which triggers StopWatchFace recomposition
    updateDisplayState(seconds, minutes)
}
```

---

### 4. Connection (elapsedSeconds)

**Description**: Links TimerEmitter.elapsedSeconds to DisplayReceiver.seconds.

**Attributes**:
| Field | Type | Value |
|-------|------|-------|
| id | String | "conn_seconds" |
| sourceNodeId | String | TimerEmitter.id |
| sourcePortId | String | "elapsedSeconds" |
| targetNodeId | String | DisplayReceiver.id |
| targetPortId | String | "seconds" |
| channelCapacity | Int | 1 (latest value only) |

---

### 5. Connection (elapsedMinutes)

**Description**: Links TimerEmitter.elapsedMinutes to DisplayReceiver.minutes.

**Attributes**:
| Field | Type | Value |
|-------|------|-------|
| id | String | "conn_minutes" |
| sourceNodeId | String | TimerEmitter.id |
| sourcePortId | String | "elapsedMinutes" |
| targetNodeId | String | DisplayReceiver.id |
| targetPortId | String | "minutes" |
| channelCapacity | Int | 1 (latest value only) |

---

### 6. StopWatchController

**Description**: Runtime wrapper around RootControlNode, providing the execution control interface for the UI layer.

**Attributes**:
| Field | Type | Description |
|-------|------|-------------|
| rootControlNode | RootControlNode | Underlying control node |
| flowGraph | FlowGraph | Reference to StopWatchFlowGraph |

**Interface**:
```kotlin
interface StopWatchController {
    val isRunning: Boolean
    val executionState: ExecutionState
    val elapsedSeconds: StateFlow<Int>
    val elapsedMinutes: StateFlow<Int>

    fun start()
    fun stop()
    fun pause()
    fun reset()
    fun getStatus(): FlowExecutionStatus

    /**
     * Binds this controller to an Android/KMP Lifecycle.
     * When the lifecycle pauses, the controller pauses (if running).
     * When the lifecycle resumes, the controller resumes (if it was running before pause).
     * When the lifecycle is destroyed, the controller stops.
     *
     * @param lifecycle The Lifecycle to bind to (typically from LocalLifecycleOwner)
     */
    fun bindToLifecycle(lifecycle: Lifecycle)
}
```

**State Derivation**:
```kotlin
val isRunning: Boolean
    get() = executionState == ExecutionState.RUNNING
```

---

## Relationships

| Relationship | Cardinality | Description |
|--------------|-------------|-------------|
| FlowGraph → Node | 1:N | FlowGraph contains multiple nodes |
| Node → Port | 1:N | Each node has multiple ports |
| Connection → Port | 2:1 | Each connection links exactly 2 ports |
| RootControlNode → FlowGraph | 1:1 | Controller wraps exactly one FlowGraph |

---

## State Transitions

### ExecutionState Lifecycle

```
     ┌────────────────────────────────────────┐
     │                                        │
     ▼                                        │
  ┌──────┐    start()     ┌─────────┐         │
  │ IDLE │ ─────────────► │ RUNNING │         │
  └──────┘                └─────────┘         │
     ▲                        │ │             │
     │         stop()         │ │             │
     └────────────────────────┘ │             │
                                │ pause()     │
                                ▼             │
                          ┌────────┐          │
                          │ PAUSED │──────────┘
                          └────────┘  resume()
                              │
                              │ stop()
                              ▼
                          ┌──────┐
                          │ IDLE │
                          └──────┘
```

### Reset Operation

Reset is a composite operation:
1. Call `stop()` → transitions to IDLE
2. Reset elapsedSeconds to 0
3. Reset elapsedMinutes to 0

### Why ExecutionState (not Android Lifecycle)

ExecutionState is intentionally separate from Android/KMP Lifecycle because:

| Concern | ExecutionState | Android Lifecycle |
|---------|---------------|-------------------|
| Purpose | FBP processing control | UI visibility/activity |
| ERROR state | ✓ Built-in | ✗ Not available |
| Control direction | Explicit (user calls) | System-driven |
| Portability | Any platform | Android/iOS only |

The `bindToLifecycle()` utility bridges the two when needed, allowing:
- fbpDsl to remain UI-agnostic (usable in headless/server contexts)
- Optional lifecycle integration for mobile apps
- User control over whether lifecycle events affect execution

See [research.md](research.md#7-executionstate-vs-android-lifecycle) for detailed analysis.

---

## Serialization Format (.flow.kts)

Example DSL for the StopWatch FlowGraph:

```kotlin
flowGraph("StopWatch") {
    version = "1.0.0"
    description = "Virtual circuit demo for stopwatch functionality"

    codeNode("TimerEmitter") {
        type = NodeType.GENERATOR
        outputPort("elapsedSeconds", Int::class)
        outputPort("elapsedMinutes", Int::class)
        controlConfig {
            speedAttenuation = 1000L
        }
    }

    codeNode("DisplayReceiver") {
        type = NodeType.SINK
        inputPort("seconds", Int::class)
        inputPort("minutes", Int::class)
    }

    connect("TimerEmitter.elapsedSeconds", "DisplayReceiver.seconds")
    connect("TimerEmitter.elapsedMinutes", "DisplayReceiver.minutes")

    targetPlatforms(ANDROID, IOS, DESKTOP)
}
```

---

## Generated Code Interfaces

### StopWatchFlowGraph.kt

```kotlin
class StopWatchFlowGraph {
    val timerEmitter: TimerEmitterComponent
    val displayReceiver: DisplayReceiverComponent
    val connections: List<Connection>

    fun toFlowGraph(): FlowGraph
}
```

### StopWatchController.kt

```kotlin
class StopWatchController(
    private val flowGraph: StopWatchFlowGraph
) {
    private val rootControlNode = RootControlNode.createFor(
        flowGraph.toFlowGraph(),
        name = "StopWatchController"
    )

    // Track state for lifecycle binding
    private var wasRunningBeforePause = false

    val isRunning: Boolean
        get() = rootControlNode.flowGraph
            .rootNodes.first()
            .executionState == ExecutionState.RUNNING

    fun start() { /* calls rootControlNode.startAll() */ }
    fun stop() { /* calls rootControlNode.stopAll() */ }
    fun pause() { /* calls rootControlNode.pauseAll() */ }
    fun getStatus() = rootControlNode.getStatus()

    /**
     * Binds this controller to an Android/KMP Lifecycle.
     * Automatically pauses when app goes to background and resumes when returning.
     */
    fun bindToLifecycle(lifecycle: Lifecycle) {
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    wasRunningBeforePause = isRunning
                    if (isRunning) pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (wasRunningBeforePause) start()
                }
                Lifecycle.Event.ON_DESTROY -> stop()
                else -> {}
            }
        })
    }
}
```

**Required Dependency** (in generated module's build.gradle.kts):
```kotlin
commonMain.dependencies {
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.9.6")
}
```

---

## Validation Rules Summary

| Entity | Rule | Error Message |
|--------|------|---------------|
| FlowGraph | name non-empty | "FlowGraph name cannot be empty" |
| FlowGraph | version semantic | "Version must follow X.Y.Z format" |
| Connection | ports exist | "Source/target port not found" |
| Connection | types compatible | "Port types incompatible: {source} → {target}" |
| Port | dataType not null | "Port must have a data type" |
| CodeNode | at least one port | "CodeNode must have at least one input or output port" |

# Data Model: StopWatch ViewModel Pattern

**Feature**: 018-stopwatch-viewmodel
**Date**: 2026-02-16

## Entities

### StopWatchViewModel

The ViewModel class that bridges FlowGraph domain logic (StopWatchController) and Compose UI.

**Properties (State)**:

| Property | Type | Description |
|----------|------|-------------|
| elapsedSeconds | StateFlow<Int> | Current elapsed seconds (0-59), delegated from controller |
| elapsedMinutes | StateFlow<Int> | Current elapsed minutes, delegated from controller |
| executionState | StateFlow<ExecutionState> | Current state (IDLE, RUNNING, PAUSED), delegated from controller |
| isRunning | Boolean | Computed: true when executionState == RUNNING |

**Actions (Methods)**:

| Method | Return | Description |
|--------|--------|-------------|
| start() | FlowGraph | Start the stopwatch, delegates to controller.start() |
| stop() | FlowGraph | Stop the stopwatch, delegates to controller.stop() |
| reset() | FlowGraph | Reset elapsed time to 0, delegates to controller.reset() |

**Dependencies**:

| Dependency | Type | Injected Via |
|------------|------|--------------|
| controller | StopWatchController | Constructor |

### ExecutionState (Existing)

Enum from fbpDsl representing flow execution state.

| Value | Description |
|-------|-------------|
| IDLE | Stopwatch is stopped, not counting |
| RUNNING | Stopwatch is actively counting |
| PAUSED | Stopwatch is paused (time preserved) |

### StopWatchController (Existing - Not Modified)

Generated controller that manages the FlowGraph execution.

**Properties Used by ViewModel**:

| Property | Type | Description |
|----------|------|-------------|
| elapsedSeconds | StateFlow<Int> | Observable seconds counter |
| elapsedMinutes | StateFlow<Int> | Observable minutes counter |
| executionState | StateFlow<ExecutionState> | Observable execution state |

**Methods Used by ViewModel**:

| Method | Description |
|--------|-------------|
| start() | Start all nodes in the flow |
| stop() | Stop all nodes in the flow |
| reset() | Reset the flow to initial state |

## Relationships

```text
┌─────────────────────────────────────────────────────────────────┐
│                         Compose UI Layer                        │
│  ┌──────────────────┐    ┌──────────────────────────────────┐  │
│  │  StopWatchFace   │    │          StopWatch               │  │
│  │  (Pure Render)   │◄───│  collectAsState() from ViewModel │  │
│  └──────────────────┘    └──────────────────────────────────┘  │
│                                        │                        │
│                                        │ observes               │
│                                        ▼                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                   StopWatchViewModel                      │  │
│  │  - elapsedSeconds: StateFlow<Int>                        │  │
│  │  - elapsedMinutes: StateFlow<Int>                        │  │
│  │  - executionState: StateFlow<ExecutionState>             │  │
│  │  - start() / stop() / reset()                            │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                        │                        │
│                                        │ delegates              │
│                                        ▼                        │
└─────────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────────┐
│                       Domain Logic Layer                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                 StopWatchController                       │  │
│  │  (Generated from StopWatch.flow)                         │  │
│  │  - Manages FlowGraph execution                           │  │
│  │  - Provides StateFlow for elapsed time                   │  │
│  │  - Handles lifecycle binding                             │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                        │                        │
│                                        │ controls               │
│                                        ▼                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                   StopWatchFlow                           │  │
│  │  - TimerEmitter → DisplayReceiver                        │  │
│  │  - Channel-based FBP execution                           │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## State Flow

```text
User Action        ViewModel           Controller          FlowGraph
     │                  │                   │                  │
     │  clicks Start    │                   │                  │
     │─────────────────►│                   │                  │
     │                  │  start()          │                  │
     │                  │──────────────────►│                  │
     │                  │                   │  start nodes     │
     │                  │                   │─────────────────►│
     │                  │                   │                  │
     │                  │                   │◄─────────────────│
     │                  │                   │  state changes   │
     │                  │◄──────────────────│                  │
     │                  │  StateFlow emit   │                  │
     │◄─────────────────│                   │                  │
     │  UI recomposes   │                   │                  │
```

## Validation Rules

| Rule | Entity | Description |
|------|--------|-------------|
| Non-negative time | StopWatchViewModel | elapsedSeconds and elapsedMinutes are always >= 0 |
| State consistency | StopWatchViewModel | executionState reflects actual FlowGraph state |
| Late subscription | StopWatchViewModel | StateFlow provides current value immediately on collect |

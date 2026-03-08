# Quickstart: Debuggable Data Runtime Preview

**Feature**: 044-debuggable-data-preview
**Date**: 2026-03-07

## Overview

This feature adds the ability to inspect the most recent data value on any connection in a flow graph while the runtime is paused with data flow animation enabled.

## Key Files

| File | Change |
|------|--------|
| `circuitSimulator/.../DataFlowDebugger.kt` | NEW: Stores per-connection transit snapshots |
| `circuitSimulator/.../RuntimeSession.kt` | Wire debugger into start/stop/animation lifecycle |
| `graphEditor/.../ui/PropertiesPanel.kt` | Display snapshot value for selected connection when paused |
| `graphEditor/.../Main.kt` | Pass debugger state to PropertiesPanel |
| `fbpDsl/.../runtime/NodeRuntime.kt` | Add value-capturing emission callback |
| Various runtime files | Update emission sites to pass value to new callback |

## Verification

### StopWatch Module
1. Run app, open StopWatch module
2. Set attenuation to 1000ms, enable "Animate Data Flow"
3. Press Start, wait several ticks
4. Press Pause
5. Click on the connection between TimerEmitter and TimeIncrementer (seconds channel)
6. Observe: Properties panel shows connection properties AND the most recent seconds value (e.g., "42")
7. Click on the minutes channel connection
8. Observe: Properties panel shows the most recent minutes value (e.g., "0")

### UserProfiles Module
1. Run app, open UserProfiles module
2. Set attenuation to 1000ms, enable "Animate Data Flow"
3. Press Start, press Add to save a profile
4. Press Pause
5. Click on a connection from UserProfileCUD to UserProfileRepository
6. Observe: Properties panel shows the most recent entity or Unit value

### No Debug Mode
1. Run any module with "Animate Data Flow" disabled
2. Pause and click on a connection
3. Observe: No snapshot data is displayed (only standard connection properties)

## Build & Run

```bash
./gradlew :graphEditor:run
```

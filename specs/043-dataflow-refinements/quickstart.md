# Quickstart: Module DataFlow Refinements

**Feature**: 043-dataflow-refinements
**Date**: 2026-03-07

## Overview

This feature refines data flow in two modules to eliminate unnecessary channel emissions:
1. **UserProfiles**: Only the triggered action's channel emits (not all three)
2. **StopWatch**: Minutes output only emits when the value changes

## Key Files

| File | Change |
|------|--------|
| `StopWatch/.../processingLogic/TimeIncrementerProcessLogic.kt` | Use `ProcessResult2.first()` when minutes unchanged |
| `UserProfiles/.../generated/UserProfilesFlow.kt` | Replace `combine()` source with individual collectors; change processor to `In3AnyOut2` |
| `UserProfiles/.../generated/UserProfilesController.kt` | Update `start()` for new flow pattern |
| `UserProfiles/.../processingLogic/UserProfileRepositoryProcessLogic.kt` | Adapt process block for any-input semantics |

## Verification

### StopWatch
1. Run app, open StopWatch module
2. Set attenuation to 1000ms, enable "Animate Data Flow"
3. Press Start
4. Observe: seconds connection shows dots every tick; minutes connection shows dot only at 59→0 rollover

### UserProfiles
1. Run app, open UserProfiles module
2. Set attenuation to 1000ms, enable "Animate Data Flow"
3. Press Start, then press Add
4. Observe: only 1 connection (save/add) shows an animated dot, not all 3

## Build & Run

```bash
./gradlew :StopWatch:compileKotlinJvm :UserProfiles:compileKotlinJvm
./gradlew :graphEditor:run
```

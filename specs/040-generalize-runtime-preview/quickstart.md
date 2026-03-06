# Quickstart: Generalize Runtime Preview

**Feature**: 040-generalize-runtime-preview
**Date**: 2026-03-06

## Prerequisites

- Existing graphEditor with RuntimePreviewPanel
- StopWatch module with working preview
- UserProfiles module (from feature 039)
- fbpDsl runtime library

## Implementation Order

1. **ModuleController interface** — Define in fbpDsl, the shared runtime library
2. **Generated controllers implement ModuleController** — Update StopWatchController and UserProfilesController
3. **Refactor RuntimeSession** — Accept ModuleController + viewModel instead of hardcoding StopWatch
4. **PreviewRegistry** — Create registry for dynamic preview dispatch
5. **Refactor StopWatchPreviewProvider** — Register with PreviewRegistry instead of being called directly
6. **Create UserProfilesPreviewProvider** — Register UserProfiles composable previews
7. **Refactor RuntimePreviewPanel** — Replace hardcoded `when` with registry lookup
8. **Module session factories** — Create factory functions in graphEditor for each module
9. **Wire Main.kt** — Use factory to create RuntimeSession based on loaded module

## Verification

```bash
# Build all affected modules
./gradlew :fbpDsl:compileKotlinJvm
./gradlew :circuitSimulator:compileKotlinJvm
./gradlew :StopWatch:compileKotlinJvm
./gradlew :UserProfiles:compileKotlinJvm
./gradlew :graphEditor:run
```

Manual test flow:
1. Launch graphEditor → load StopWatch module → open Runtime Preview panel
2. Verify StopWatch preview works identically to before (clock face ticks, controls work)
3. Select "StopWatchScreen" from dropdown → verify full screen preview renders
4. Load UserProfiles module → verify Runtime Preview panel initializes for UserProfiles
5. Select "UserProfiles" from dropdown → verify UserProfiles screen renders
6. Start runtime → verify execution controls work (start/stop/pause/resume)
7. Adjust attenuation slider → verify it takes effect
8. Switch back to StopWatch module → verify clean transition, StopWatch preview works

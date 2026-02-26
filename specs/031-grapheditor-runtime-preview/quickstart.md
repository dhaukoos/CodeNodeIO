# Quickstart: GraphEditor Runtime Preview

**Feature**: 031-grapheditor-runtime-preview
**Date**: 2026-02-25

## Verification Scenarios

### Scenario 1: Start and Stop StopWatch
1. Launch the graphEditor application
2. Load the StopWatch module (or ensure it is the active module)
3. Expand the runtime preview panel on the right side
4. Verify the controls show: Start button enabled, Stop/Pause/Resume disabled
5. Press **Start**
6. Verify: execution state shows "Running", the StopWatch face appears in preview, seconds hand moves
7. Wait 5 seconds — verify the digital display reads approximately "00:05"
8. Press **Stop**
9. Verify: execution state shows "Idle", the display resets to "00:00", Start button re-enabled

### Scenario 2: Pause and Resume
1. Start the StopWatch (per Scenario 1)
2. Wait until the display shows "00:10"
3. Press **Pause**
4. Verify: state shows "Paused", the clock freezes at ~00:10, seconds hand stops
5. Wait 3 seconds — verify the display still shows "00:10" (not advancing)
6. Press **Resume**
7. Verify: state shows "Running", the clock continues from "00:10"
8. Stop the StopWatch

### Scenario 3: Speed Attenuation
1. Start the StopWatch at 0ms attenuation (no delay — ticks as fast as the loop can run)
2. Observe the seconds hand advancing rapidly (no artificial delay between ticks)
3. Increase attenuation to **1000ms** using the slider
4. Observe the seconds hand now ticks at 1-second intervals (attenuationDelayMs is the entire delay)
5. Increase attenuation to **3000ms**
6. Observe the seconds hand now ticks at 3-second intervals
7. Decrease attenuation back to **0ms**
8. Observe the seconds hand returns to rapid ticking (no delay)
9. Stop the StopWatch

### Scenario 4: Minute Rollover
1. Start the StopWatch at 0ms attenuation
2. Wait until the display shows "00:58" (or set a lower attenuation for faster testing)
3. Observe the seconds count through 59 → 00 and the minutes increment to "01:00"
4. Verify the minutes hand has moved on the clock face
5. Stop the StopWatch

### Scenario 5: Edit While Running
1. Start the StopWatch and let it run for a few seconds
2. Modify the flow graph (e.g., move a node on the canvas)
3. Verify: execution stops automatically, state returns to "Idle", preview resets

### Scenario 6: Preview Panel Toggle
1. Verify the runtime preview panel can be collapsed (hidden)
2. Verify the canvas expands to fill the freed space
3. Verify the runtime preview panel can be expanded (shown) again
4. Verify no execution state is lost during collapse/expand while idle

### Scenario 7: No UI Composables
1. Load a module that has no userInterface composables (if available)
2. Expand the runtime preview panel
3. Verify: controls are visible but the preview area shows "No preview available"
4. Verify: Start/Stop still function (execution state changes even without preview)

## Build & Run

```bash
# Compile all affected modules
./gradlew :fbpDsl:compileKotlinJvm
./gradlew :circuitSimulator:compileKotlinJvm
./gradlew :graphEditor:compileKotlinJvm

# Run tests
./gradlew :fbpDsl:allTests
./gradlew :circuitSimulator:jvmTest
./gradlew :graphEditor:jvmTest

# Launch the graphEditor
./gradlew :graphEditor:run
```

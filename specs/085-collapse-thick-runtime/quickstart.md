# Quickstart: Verifying the Universal-Runtime Collapse

**Date**: 2026-04-27
**Feature**: [spec.md](./spec.md)

This document is the manual verification path for feature 085. The collapse touches three layers (fbpDsl, flowGraph-generate, all five reference modules + KMPMobileApp), so verification is organized by layer and by the two consumer paths (Runtime Preview vs production deployment).

Five verification scenarios:

1. **VS-A** — `fbpDsl` enhancement (`getStatus()`) works in isolation.
2. **VS-B** — All five reference modules regenerate cleanly to the new shape and produce `{Module}Runtime.kt`.
3. **VS-C** — GraphEditor Runtime Preview behavior is unchanged for every reference module.
4. **VS-D** — KMPMobileApp builds and runs against the collapsed modules; its existing tests pass.
5. **VS-E** — The deprecated generators are gone from the codebase.

---

## Pre-flight

```bash
# From CodeNodeIO repo root
git checkout 085-collapse-thick-runtime
./gradlew :fbpDsl:jvmTest                          # Unit tests for getStatus() must pass
./gradlew :flowGraph-generate:jvmTest              # Generator tests must pass
./gradlew :flowGraph-execute:jvmTest               # Runtime Preview regression tests must pass

# Confirm DemoProject is at a compatible head
ls /Users/dhaukoos/CodeNodeIO-DemoProject/{StopWatch,Addresses,UserProfiles,EdgeArtFilter,WeatherForecast,KMPMobileApp}
```

---

## VS-A — `getStatus()` works on `DynamicPipelineController`

### A1. Inspect the new method signature

```bash
grep -n "fun getStatus" /Users/dhaukoos/CodeNodeIO/fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/{ModuleController,DynamicPipelineController,DynamicPipeline}.kt
```

Expected: each of the three files declares `getStatus(): FlowExecutionStatus`. `ModuleController` declares it as an interface method; `DynamicPipelineController` overrides; `DynamicPipeline` provides the implementation that wraps `RootControlNode.getStatus()`.

### A2. Run the new unit test

```bash
cd /Users/dhaukoos/CodeNodeIO
./gradlew :fbpDsl:jvmTest --tests '*DynamicPipelineControllerTest*'
```

Expected: a test like `getStatus_returns_idle_before_start`, `getStatus_reflects_running_after_start`, `getStatus_reflects_paused_after_pause`, `getStatus_returns_idle_after_stop` all pass.

---

## VS-B — Reference modules regenerate to the new shape

### B1. Regenerate StopWatch

Trigger module regeneration for StopWatch via the GraphEditor (or whichever entry point `ModuleGenerator` exposes). Confirm the structured summary reports:

- `StopWatchRuntime.kt` (at `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/`) — **CREATED**
- `controller/StopWatchControllerInterface.kt` — **UPDATED** (now extends `ModuleController`; body shrunk to just typed state-flow getters)
- `controller/StopWatchController.kt` — **DELETED**
- `controller/StopWatchControllerAdapter.kt` — **DELETED**
- `flow/StopWatchFlow.kt` — **DELETED** (the user-authored `StopWatch.flow.kt` is **not** touched)
- All other files (`viewmodel/StopWatchViewModel.kt`, `viewmodel/StopWatchState.kt`, `nodes/*`, `userInterface/*`, `jvmMain/.../StopWatchPreviewProvider.kt`) — **UNCHANGED**

### B2. Verify file contents

```bash
ls /Users/dhaukoos/CodeNodeIO-DemoProject/StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/
# Expect: StopWatchRuntime.kt + the unchanged subdirs (controller/, flow/, nodes/, userInterface/, viewmodel/)
# Expect controller/ to contain ONLY StopWatchControllerInterface.kt
# Expect flow/ to contain ONLY StopWatch.flow.kt (user-authored DSL)

cat /Users/dhaukoos/CodeNodeIO-DemoProject/StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/StopWatchRuntime.kt
# Expect ~30-35 lines matching the template in data-model.md §4
```

### B3. Build the module

```bash
cd /Users/dhaukoos/CodeNodeIO-DemoProject
./gradlew :StopWatch:compileKotlinJvm
```

Expected: clean compile, no errors.

### B4. Repeat for the other four reference modules

Repeat B1–B3 for Addresses, UserProfiles, EdgeArtFilter, and WeatherForecast. For WeatherForecast (which had no thick stack to begin with), the only changes are the addition of `WeatherForecastRuntime.kt` and the modification of `WeatherForecastControllerInterface.kt` (adding `: ModuleController` and the typed state-flow getters that the regeneration newly emits).

### B5. Confirm cumulative line-count reduction

```bash
# Pre-collapse: ~1,400 lines across 12 files (4 Controller + 4 Adapter + 4 Flow runtime).
# Post-collapse: 5 × ~30-line {Module}Runtime.kt = ~150 lines.
# Net reduction: ~1,250 lines of generated boilerplate.

find /Users/dhaukoos/CodeNodeIO-DemoProject/{StopWatch,Addresses,UserProfiles,EdgeArtFilter,WeatherForecast}/src/commonMain -type f -name "*Controller.kt" -o -name "*ControllerAdapter.kt" -o -name "*Flow.kt" 2>/dev/null | grep -v ControllerInterface | grep -v ".flow.kt"
# Expect: empty output (the only matches before were the eliminated trio)
```

This confirms SC-001.

---

## VS-C — Runtime Preview behavior unchanged

### C1. Open each reference module in the GraphEditor

For each of the five reference modules, in turn:

1. Open the module via the Module Dropdown.
2. Open its `.flow.kt` graph.
3. Click "Runtime Preview".
4. Confirm:
   - The UI renders inside the Runtime Preview panel.
   - Runtime status reports the module is loadable.
   - Click Start. Behavior matches pre-collapse:
     - **StopWatch**: timer advances (seconds/minutes); Pause stops advance; Resume continues.
     - **Addresses**: empty list initially; (with the test app's add/update/remove flow) entries appear.
     - **UserProfiles**: empty list; same add/update/remove behavior.
     - **EdgeArtFilter**: image-processing pipeline produces output frames.
     - **WeatherForecast**: trigger source emits; HTTP fetch / parse / display chain produces forecast data.
   - Click Pause / Resume / Stop / Reset — each behaves as it did pre-collapse.

### C2. Verify dataflow animation, attenuation, observers (regression checks)

For at least StopWatch and Addresses:

1. Enable dataflow animation. Confirm animations appear on connections during data flow.
2. Adjust the attenuation slider. Confirm node emissions slow down accordingly.
3. Inspect the data preview panel. Confirm per-port values are observable and update with each emission.

### C3. Run the new regression test

```bash
cd /Users/dhaukoos/CodeNodeIO
./gradlew :flowGraph-execute:jvmTest --tests '*ModuleSessionFactoryRegressionTest*'
```

Expected: the test exercises Runtime Preview's session-creation path against each of the five regenerated modules and asserts that `createSession` returns a non-null `RuntimeSession` whose ViewModel is correctly cast-able to `{Module}ViewModel`.

This satisfies SC-005 and US3.

---

## VS-D — KMPMobileApp parity (production-app path)

### D1. Inspect the migrated instantiation sites

```bash
grep -n "createStopWatchRuntime\|createUserProfilesRuntime\|StopWatchController(\|UserProfilesController(\|StopWatchControllerAdapter\|UserProfilesControllerAdapter" /Users/dhaukoos/CodeNodeIO-DemoProject/KMPMobileApp/src
```

Expected:
- Matches: `createStopWatchRuntime(stopWatchFlowGraph)` and `createUserProfilesRuntime(userProfilesFlowGraph)` in `App.kt`, and `createStopWatchRuntime(flowGraph)` in `StopWatchIntegrationTest.kt` (~6 sites).
- NO matches: `StopWatchController(`, `UserProfilesController(`, `StopWatchControllerAdapter`, `UserProfilesControllerAdapter` (the old import surface is gone).

### D2. Build KMPMobileApp

```bash
cd /Users/dhaukoos/CodeNodeIO-DemoProject
./gradlew :KMPMobileApp:assembleDebug
```

Expected: clean build for the Android target. (iOS / desktop targets, if configured, build cleanly too.)

### D3. Run KMPMobileApp's existing integration tests

```bash
./gradlew :KMPMobileApp:testDebugUnitTest --tests '*StopWatchIntegrationTest*'
```

Expected: every test passes. Particular attention to `controller_getStatus_returns_FlowExecutionStatus` (line 369-378 of the test) — this is the test that validated the `getStatus()` parity gap and proves the universal runtime carries it.

### D4. Manual end-to-end check

Launch KMPMobileApp on an Android emulator or device. Exercise both tabs:

1. **StopWatch tab**: Start the timer; confirm seconds/minutes advance; pause; resume; stop; reset.
2. **UserProfiles tab**: View the empty list; add a profile; confirm it appears; edit it; confirm the change; delete it; confirm it's removed.

Behavior MUST match pre-collapse exactly.

This satisfies SC-004 and US2.

### D5. Production-app integration template (SC-008)

Use this template as the full instantiation pattern for any of the five reference modules
(StopWatch, Addresses, UserProfiles, EdgeArtFilter, WeatherForecast). A new developer
should be able to write a working production-app integration in under 30 minutes from
this template alone.

**Template** — replace `<Module>` with the module name (e.g., `StopWatch`) throughout:

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.codenode.<module>.controller.create<Module>Runtime
import io.codenode.<module>.flow.<module>FlowGraph         // .flow.kt's val export
import io.codenode.<module>.viewmodel.<Module>ViewModel
import io.codenode.<module>.userInterface.<Module>Screen   // or {Module}() for a non-screen UI

@Composable
fun <Module>Tab() {
    // 1. Build the typed ControllerInterface from the FlowGraph DSL constant.
    //    The factory returns an object that delegates ModuleController by the
    //    underlying DynamicPipelineController — start/stop/pause/resume/reset
    //    all "just work."
    val controller = remember { create<Module>Runtime(<module>FlowGraph) }

    // 2. Wrap the controller in the typed ViewModel. (For DAO-bearing modules
    //    — Addresses, UserProfiles — pass the DAO as the second argument:
    //    `<Module>ViewModel(controller, <Persistence>.dao)`.)
    val viewModel = remember { <Module>ViewModel(controller) }

    // 3. Render the module's UI; the composable observes ViewModel state flows
    //    via `collectAsState()` internally. No bespoke wiring needed.
    <Module>Screen(viewModel = viewModel)
}
```

**Concrete example — StopWatch** (mirrors `KMPMobileApp/.../mobileapp/App.kt`):

```kotlin
import io.codenode.stopwatch.controller.createStopWatchRuntime
import io.codenode.stopwatch.flow.stopWatchFlowGraph
import io.codenode.stopwatch.viewmodel.StopWatchViewModel
import io.codenode.stopwatch.userInterface.StopWatchScreen

@Composable
fun StopWatchTab() {
    val controller = remember { createStopWatchRuntime(stopWatchFlowGraph) }
    val viewModel = remember { StopWatchViewModel(controller) }
    StopWatchScreen(viewModel = viewModel)
}
```

**Concrete example — UserProfiles (DAO-bearing module)**:

```kotlin
import io.codenode.userprofiles.controller.createUserProfilesRuntime
import io.codenode.userprofiles.flow.userProfilesFlowGraph
import io.codenode.userprofiles.persistence.UserProfilesPersistence
import io.codenode.userprofiles.viewmodel.UserProfilesViewModel
import io.codenode.userprofiles.userInterface.UserProfiles

@Composable
fun UserProfilesTab() {
    val controller = remember { createUserProfilesRuntime(userProfilesFlowGraph) }
    val viewModel  = remember { UserProfilesViewModel(controller, UserProfilesPersistence.dao) }
    UserProfiles(viewModel = viewModel)
}
```

**Why this template is sufficient** — the universal-runtime collapse means there is only
one wiring pattern across all five modules. The `create<Module>Runtime(...)` factory
encapsulates `DynamicPipelineController` construction, the per-module NodeRegistry
lookup, and the State-object wiring. Production consumers never see the runtime
plumbing. Adding a sixth module is purely a regeneration step plus the three lines
above.

**Build dependencies** — the consuming app's Gradle module needs:

```kotlin
dependencies {
    implementation(project(":<Module>"))     // brings the typed ControllerInterface,
                                              // ViewModel, and the create<Module>Runtime
                                              // factory into the consumer's classpath
    implementation(project(":fbpDsl"))        // ModuleController, FlowExecutionStatus
    // For DAO-bearing modules, also: implementation(project(":persistence"))
}
```

No additional `flowGraph-execute` or GraphEditor dependency is needed — `DynamicPipelineController`
lives in `fbpDsl`, which the module already pulls in.

---

## VS-E — Deprecated generators are removed

### E1. Confirm absence in source

```bash
find /Users/dhaukoos/CodeNodeIO/flowGraph-generate/src -name 'RuntimeControllerGenerator.kt' -o -name 'RuntimeControllerAdapterGenerator.kt' -o -name 'RuntimeFlowGenerator.kt'
```

Expected: empty output.

### E2. Confirm absence of references

```bash
grep -rn "RuntimeControllerGenerator\|RuntimeControllerAdapterGenerator\|RuntimeFlowGenerator" /Users/dhaukoos/CodeNodeIO/flowGraph-generate/src --include='*.kt'
```

Expected: no matches outside of clearly-archival material (e.g., a CHANGELOG comment if maintained). Live source code (orchestrator, generator suite, tests) MUST be free of references.

### E3. Confirm the orchestrator no longer invokes them

```bash
grep -n "ModuleRuntimeGenerator\|RuntimeControllerInterfaceGenerator" /Users/dhaukoos/CodeNodeIO/flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/ModuleGenerator.kt
```

Expected: matches for `ModuleRuntimeGenerator` (newly invoked) and `RuntimeControllerInterfaceGenerator` (kept). NO matches for the deprecated three.

This satisfies SC-007 and US5.

---

## VS-F — Hand-edit safety check (FR-013)

### F1. Synthetic test of the safety check

For one module (StopWatch), create a fake "hand-edited" Controller file that lacks the `Generated by` marker:

```bash
cd /Users/dhaukoos/CodeNodeIO-DemoProject/StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/controller
echo "// custom user code, no generator marker" > StopWatchControllerLegacyHandEdit.kt
```

(Note: this is a synthetic file with a non-target name; replace with a target-name file scenario by temporarily renaming `StopWatchRuntime.kt` to `StopWatchController.kt` and stripping its header.)

Re-run regeneration. Expected: the orchestrator MUST refuse to delete the unmarked file at the target path, MUST emit a structured warning naming the file and the reason ("marker absent"), AND MUST NOT silently overwrite or delete it.

After verifying, restore the file to its expected state.

---

## Failure-mode reference

| Symptom | Likely cause | Remedy |
|---|---|---|
| `:KMPMobileApp:assembleDebug` fails with "unresolved reference: StopWatchController" | KMPMobileApp's instantiation sites weren't updated atomically with module regeneration | Re-run the migration; ensure App.kt and StopWatchIntegrationTest.kt are in the same change set as the StopWatch regeneration |
| `:StopWatch:compileKotlinJvm` fails with "missing import io.codenode.fbpdsl.runtime.ModuleController" | `RuntimeControllerInterfaceGenerator` modification not applied | Verify the generator emits the import + `: ModuleController` clause |
| Runtime Preview shows "Preview not available" for a module | `PreviewProvider` discovery failed (unrelated to this feature) | Out of scope — pre-existing issue; check `jvmMain/.../{Module}PreviewProvider.kt` exists |
| `controller_getStatus_returns_FlowExecutionStatus` test fails | `getStatus()` not added to `DynamicPipelineController`, OR added but `DynamicPipeline` doesn't retain a `RootControlNode` reference | Verify VS-A1 first; check `DynamicPipeline.kt` retains `RootControlNode` as a class field (not local) |
| Runtime Preview misbehaves: `getStatus()` called via reflection returns null and crashes | The `ModuleSessionFactory.createControllerProxy` one-line update for `getStatus` was missed | Verify line 108-150 of `ModuleSessionFactory.kt` has explicit `"getStatus" -> controller.getStatus()` case |
| Re-running module regeneration deletes a file the user expected to keep | Safety check regressed | Verify VS-F passes; check the orchestrator's deletion path includes the marker-comment inspection |
| `setAttenuationDelay(...)` called on the typed `{Module}ControllerInterface` does nothing | The interface didn't extend `ModuleController` (Kotlin delegation isn't reaching the controller) | Verify the regenerated `{Module}ControllerInterface.kt` contains `: ModuleController` |
| WeatherForecast in Runtime Preview suddenly broken (was working pre-collapse) | The regeneration added typed state-flow getters that don't have matching `xxxFlow` fields on `WeatherForecastState` | Verify the State object's field names match the interface's getters; this should be inherent if both are generated consistently |

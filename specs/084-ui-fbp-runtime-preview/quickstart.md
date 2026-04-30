# Quickstart: Verifying UI-FBP Runtime Preview + Deployable Module (post-085)

**Date**: 2026-04-28
**Feature**: [spec.md](./spec.md)

This document is the manual verification path for feature 084 against the post-085 universal-runtime surface. Per the post-clarification spec (Session 2026-04-28), the feature targets BOTH scenarios:

- **Scenario R** — Runtime Preview in the GraphEditor (in-process, reflection-driven).
- **Scenario D** — Deployable to a production app that imports the module without the GraphEditor (using feature 085's `create{FlowGraph}Runtime(flowGraph)` factory).

Three verification scenarios:

1. **VS-A** — Migrate the existing `TestModule` (in `CodeNodeIO-DemoProject/`), bring it up to the post-085 contract, and verify Runtime Preview works against `DemoUI.kt`.
2. **VS-B** — Generate a fresh module from a brand-new qualifying UI file in a feature-085-scaffolded module and verify Runtime Preview works without any manual file edits.
3. **VS-C** — Confirm the generated module is consumable from a production app via the same `create{FlowGraph}Runtime(flowGraph)` template documented in feature 085's `quickstart.md` VS-D5 — UI-FBP modules and entity modules share one consumer pattern.

---

## Pre-flight

```bash
# From CodeNodeIO repo root
git checkout 084-ui-fbp-runtime-preview
./gradlew :flowGraph-generate:check                  # Generator unit + integration tests must pass
./gradlew :flowGraph-execute:jvmTest                 # Runtime contract tests (incl. ModuleSessionFactoryRegressionTest from 085) must pass

# Confirm DemoProject is checked out at a compatible head
ls /Users/dhaukoos/CodeNodeIO-DemoProject/TestModule              # Must exist
ls /Users/dhaukoos/CodeNodeIO-DemoProject/StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/controller/StopWatchRuntime.kt   # Reference shape
```

---

## VS-A — Migrate and verify TestModule (Scenario R)

### A1. One-time legacy migration (covers FR-009 + FR-010)

The existing `TestModule` was scaffolded **before** feature 085's `ModuleGenerator` shape. It lacks the `jvm()` target and `preview-api` dependency UI-FBP requires (Decision 5: UI-FBP detects this and refuses), AND it carries a duplicated `saved/` package from an older generator iteration (Decision 6).

**Step 1 — Build wiring**: Open `CodeNodeIO-DemoProject/TestModule/build.gradle.kts`. Verify the `kotlin { ... }` block contains `jvm { ... }`. If absent, add it (mirroring StopWatch's shape — see `CodeNodeIO-DemoProject/StopWatch/build.gradle.kts:17-65`):

```kotlin
kotlin {
    androidTarget()
    jvm()                               // ← ADD if missing

    sourceSets {
        // ... existing common sets ...
        val jvmMain by getting {
            dependencies {
                implementation("io.codenode:preview-api")   // ← ADD if missing
            }
        }
    }
}
```

**Step 2 — Legacy `saved/` cleanup**:

```bash
cd /Users/dhaukoos/CodeNodeIO-DemoProject/TestModule
rm -rf src/commonMain/kotlin/io/codenode/demo/saved
```

**Step 3 — Import-path fix in `userInterface/DemoUI.kt`**:

```kotlin
// In src/commonMain/kotlin/io/codenode/demo/userInterface/DemoUI.kt
// Replace:
import io.codenode.demo.saved.DemoUIViewModel
// With:
import io.codenode.demo.viewmodel.DemoUIViewModel
```

Uncomment the live `viewModel.emit(a, b)` call inside `DemoUI.kt` (it is currently commented out — pre-feature debt).

**Step 4 — Verify the migration before re-running UI-FBP**: `./gradlew :TestModule:compileKotlinJvm` — expect a clean compile (or a single failure pointing at the missing `DemoUIControllerInterface`, which UI-FBP will create in A2). The `jvm()` and `preview-api` entries are validated by trying to build the JVM target.

### A2. Re-run UI-FBP code generation

From the GraphEditor:

1. Open `CodeNodeIO-DemoProject` via the workspace dropdown (feature 083). Select `TestModule`.
2. Trigger the UI-FBP code-generation entry point. Per FR-014/FR-015 (post-clarification), the entry point is the GraphEditor's UI-FBP path with **two file selectors**: pick `flow/DemoUI.flow.kt` as the flow graph, and `userInterface/DemoUI.kt` as the qualifying UI file.
3. Click Generate. Confirm the structured summary (`UIFBPSaveResult`) lists every file in the post-085 set:
   - `viewmodel/DemoUIState.kt` — UPDATED (path migration to `viewmodel/`; flow-graph prefix)
   - `viewmodel/DemoUIViewModel.kt` — UPDATED (constructor takes `(DemoUIControllerInterface)`; flows from State)
   - `nodes/DemoUISourceCodeNode.kt` — UNCHANGED or UPDATED (depending on prefix-derivation diff)
   - `nodes/DemoUISinkCodeNode.kt` — UNCHANGED or UPDATED
   - `controller/DemoUIControllerInterface.kt` — **CREATED** (extends `ModuleController`)
   - `controller/DemoUIRuntime.kt` — **CREATED** (factory `createDemoUIRuntime(flowGraph)`)
   - `userInterface/DemoUIPreviewProvider.kt` (jvmMain) — **CREATED** (registers under `"DemoUI"`; calls `DemoUI(viewModel = vm, ...)`)
   - `flow/DemoUI.flow.kt` — UNCHANGED (mode = UNCHANGED in `FlowKtMergeReport`) IF the existing graph still matches the Source/Sink port shape; otherwise UPDATED with `portsAdded`/`portsRemoved`/`connectionsDropped` populated.
   - **Build script NOT touched** — UI-FBP detected the post-A1-migration `jvm()` + `preview-api` and proceeded; if either were missing, generation would have refused with a clear error.

### A3. Build the module (covers Scenario R + Scenario D pre-conditions)

```bash
cd /Users/dhaukoos/CodeNodeIO-DemoProject
./gradlew :TestModule:compileKotlinJvm
```

Expected: clean compile, no errors. If errors mention duplicate symbols, a `saved/` artifact survived A1 — re-run A1 step 2.

A clean compile here covers both Scenario R (Runtime Preview can load the module's classes) and Scenario D's pre-condition (the deployable artifacts compile). VS-C runs the production-app instantiation.

### A4. Open Runtime Preview against DemoUI (Scenario R)

1. With `TestModule` selected and `DemoUI.flow.kt` open in the GraphEditor, click "Runtime Preview" (right panel toggle).
2. From the composable dropdown, select `DemoUI` (it should be selected by default — `flowGraphName == "DemoUI"` matches the `PreviewRegistry` key).
3. Confirm:
   - The DemoUI calculator UI renders inside the panel.
   - The runtime status indicator shows the module is RUNNING (or IDLE before Start).
   - No "No runtime available for this module" or "Preview not available" message is shown.
   - The reflection proxy in `ModuleSessionFactory.createControllerProxy` is loaded: setting a breakpoint in its `InvocationHandler` and clicking Start should hit the `start` case (not the `getStatus` case unless explicitly polled).

### A5. End-to-end flow check (US2 from the spec, Scenario R)

1. In the open `DemoUI.flow.kt` graph, drag a passthrough/transform between `DemoUISource` outputs and `DemoUISink` inputs (e.g., wire `a → results.sum`, `b → results.difference`). Save.
2. Click Start in the Runtime Preview controls.
3. Enter values in the A and B fields and click Emit.
4. Observe the Results section update with the computed values within one second (SC-003).
5. Click Pause; further Emit clicks should stop propagating. Click Resume; propagation resumes.
6. Click Stop; the runtime returns to IDLE without leaking coroutines (verifiable indirectly: subsequent Start works).

### A6. Re-generation idempotency check (US4, FR-011)

1. Without changing `DemoUI.kt`, re-run UI-FBP code generation against the same `{flow graph, UI file}` pair.
2. Confirm the `UIFBPSaveResult` lists every file as UNCHANGED and `flowKtMerge.mode == UNCHANGED`.
3. Confirm Runtime Preview still works (re-open if necessary).

### A7. Re-generation with UI signature change (US4, FR-012)

1. Edit `DemoUI.kt` to add a third numeric input parameter, e.g., `c: Double` to the `viewModel.emit(...)` call site (and to the ViewModel's `emit` signature — which UI-FBP regenerates).
2. Re-run UI-FBP. Confirm the `UIFBPSaveResult` reports:
   - `viewmodel/DemoUIState.kt`, `viewmodel/DemoUIViewModel.kt`, `nodes/DemoUISourceCodeNode.kt` — UPDATED.
   - `controller/DemoUIControllerInterface.kt` — UNCHANGED (the interface only carries sink-input flows; adding a Source input doesn't affect it). If `c` was added as a sink-input instead of a source-output, the interface IS UPDATED with a new `val c: StateFlow<Double>` member.
   - `controller/DemoUIRuntime.kt` — UPDATED if a new `override val` is needed in its anonymous object expression; otherwise UNCHANGED.
   - `userInterface/DemoUIPreviewProvider.kt` — UNCHANGED (it doesn't depend on port shape).
   - `flow/DemoUI.flow.kt` — UPDATED with `portsAdded = [{nodeName: "DemoUISource", portName: "c", typeName: "Double"}]` and `userNodesPreserved` reflecting the count of business-logic CodeNodes.
3. Compile and verify Runtime Preview re-loads successfully.

### A8. Hand-written conflict check (FR-016)

1. Manually edit `controller/DemoUIControllerInterface.kt` and remove the `Generated by CodeNodeIO` marker comment block.
2. Re-run UI-FBP.
3. Confirm the `UIFBPSaveResult` reports the file as `SKIPPED_CONFLICT` with a `reason` naming the missing marker, and that the file is not overwritten.
4. Re-add the marker comment and re-run; the file is now UPDATED (or UNCHANGED if content already matches).

### A9. Unscaffolded-host refusal check (FR-009 post-clarification)

1. Save `TestModule/build.gradle.kts` to a backup. Edit it to remove the `jvm()` target (or the `preview-api` dependency).
2. Re-run UI-FBP.
3. Confirm `UIFBPSaveResult.success == false` and `errorMessage` names the missing piece(s) and points at this VS-A1 migration.
4. Confirm zero file changes happened (no partial output).
5. Restore the backup of `build.gradle.kts`.

---

## VS-B — Green-field generation in a feature-085-scaffolded module (Scenario R + Scenario D pre-conditions)

### B1. Create a new module via feature 085's Generate Module path

From the GraphEditor, use the Module dropdown → "Create New Module..." (post-feature-083). Name it `Quickstart084`, Target Platforms = "Desktop (JVM)" + "Android". Wait for module scaffolding to complete. Verify the generated `Quickstart084/build.gradle.kts` already contains both `jvm()` and `implementation("io.codenode:preview-api")` (feature 085's `ModuleGenerator` emits both for every new module).

### B2. Drop a qualifying UI file in

Create `Quickstart084/src/commonMain/kotlin/io/codenode/quickstart084/userInterface/Quickstart084.kt`:

```kotlin
package io.codenode.quickstart084.userInterface

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.codenode.quickstart084.viewmodel.Quickstart084ViewModel

@Composable
fun Quickstart084(
    viewModel: Quickstart084ViewModel,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("") }
    val echoed by viewModel.echo.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = input, onValueChange = { input = it }, label = { Text("input") })
        Button(onClick = { viewModel.emit(input) }) { Text("Emit") }
        Divider()
        Text("Echoed: ${echoed ?: "(none yet)"}")
    }
}
```

Also create a bootstrap `Quickstart084/src/commonMain/kotlin/io/codenode/quickstart084/flow/Quickstart084.flow.kt` (or let UI-FBP create one in B3). The `Quickstart084ViewModel` import will not yet resolve — UI-FBP generates it in B3.

### B3. Run UI-FBP code generation against `{Quickstart084.flow.kt, Quickstart084.kt}`

From the GraphEditor (with `Quickstart084` selected), trigger UI-FBP. Pick the `.flow.kt` and the UI file via the file selectors. Confirm `UIFBPSaveResult` lists every file in the post-085 set as CREATED:

- `viewmodel/Quickstart084State.kt`
- `viewmodel/Quickstart084ViewModel.kt`
- `nodes/Quickstart084SourceCodeNode.kt`
- `nodes/Quickstart084SinkCodeNode.kt`
- `controller/Quickstart084ControllerInterface.kt`
- `controller/Quickstart084Runtime.kt`
- `userInterface/Quickstart084PreviewProvider.kt` (jvmMain)
- `flow/Quickstart084.flow.kt` (bootstrap, mode = CREATED)
- **Build script NOT touched** — feature 085's scaffolding already provided what UI-FBP needs.

### B4. Wire a passthrough and verify (Scenario R)

1. Open `Quickstart084.flow.kt` and connect `Quickstart084Source.input` → `Quickstart084Sink.echo` directly (or via a passthrough node). Save.
2. Build: `./gradlew :Quickstart084:compileKotlinJvm` — expect clean compile (SC-002).
3. Open Runtime Preview, click Start, type into the input field, click Emit.
4. The "Echoed:" line MUST update to show the typed value within one second.

### B5. Multiple flow graphs in one module (post-082/083)

This step exercises the explicit-pair input model (FR-014/FR-015 post-clarification).

1. In the same `Quickstart084` module, add a second flow graph file: `flow/Quickstart084Alt.flow.kt`.
2. Add a second qualifying UI file: `userInterface/Quickstart084Alt.kt` (with a `@Composable fun Quickstart084Alt(viewModel: Quickstart084AltViewModel, ...)` declaration).
3. Run UI-FBP a second time, picking the new `{Quickstart084Alt.flow.kt, Quickstart084Alt.kt}` pair.
4. Confirm UI-FBP emits a parallel post-085 set with `Quickstart084Alt` prefixes (e.g., `controller/Quickstart084AltControllerInterface.kt`, `controller/Quickstart084AltRuntime.kt`, `userInterface/Quickstart084AltPreviewProvider.kt`) — non-colliding with the original `Quickstart084` artifacts.
5. Build the module: `./gradlew :Quickstart084:compileKotlinJvm` — expect clean compile.
6. Open Runtime Preview; the composable dropdown should now contain both `Quickstart084` and `Quickstart084Alt`. Switching between them MUST work without errors.

---

## VS-C — Deployable parity check via feature 085's VS-D5 template (Scenario D)

This scenario verifies that the generated module is consumable from a production app via the same `create{FlowGraph}Runtime(flowGraph)` factory pattern entity modules use. SC-008 was retired during clarification; this scenario remains as Scenario D evidence and references feature 085's already-canonical template.

### C1. Read the canonical template

Read feature 085's `quickstart.md` VS-D5 ("Production-app integration template"):

```bash
cat /Users/dhaukoos/CodeNodeIO/specs/085-collapse-thick-runtime/quickstart.md | grep -A 60 "D5\."
```

It documents the generic 3-line template and concrete examples for StopWatch and UserProfiles (DAO-bearing).

### C2. Apply the template to a UI-FBP module

Write a tiny one-file JVM main inside a sibling `quickstart-app` (create the directory or reuse an existing test app):

```kotlin
package io.codenode.quickstartapp

import androidx.compose.runtime.remember
import io.codenode.quickstart084.controller.createQuickstart084Runtime
import io.codenode.quickstart084.flow.quickstart084FlowGraph         // .flow.kt's exported FlowGraph constant
import io.codenode.quickstart084.viewmodel.Quickstart084ViewModel
import io.codenode.quickstart084.userInterface.Quickstart084          // user-authored Composable

@androidx.compose.runtime.Composable
fun Quickstart084Tab() {
    val controller = remember { createQuickstart084Runtime(quickstart084FlowGraph) }
    val viewModel  = remember { Quickstart084ViewModel(controller) }
    Quickstart084(viewModel = viewModel)
}
```

This is **identical in shape** to feature 085's StopWatch example — proving UI-FBP modules require no UI-FBP-specific consumer code paths.

### C3. Compile and run

Build the consuming app's Gradle module and confirm it compiles. (A full smoke run requires a Compose Desktop host; the compile-time check is sufficient for SC-002 / Scenario D evidence in CI.) For an interactive run, host the `Quickstart084Tab()` composable inside a Compose Desktop window and exercise the UI as in B4.

If C2 and C3 succeed for a UI-FBP module without any UI-FBP-specific consumer logic, Scenario D is verified.

---

## Failure-mode reference

| Symptom | Likely cause | Remedy |
|---|---|---|
| UI-FBP refuses with "host module is unscaffolded" | Missing `jvm()` target or `preview-api` dependency | Run VS-A1 (legacy migration) |
| "No runtime available for this module" | `ControllerInterface` not on classpath; module's `jvm()` target missing or not yet compiled | Re-run B3 (or A2/A3); confirm `build.gradle.kts` has `jvm()` + `preview-api` |
| "Preview not available for: {Name}" | `PreviewProvider` discovery failed; check the file lives in `src/jvmMain/kotlin/.../userInterface/{Name}PreviewProvider.kt` | Confirm A2 / B3 generated it; check `gradlew :Module:jvmMainClasses` succeeded |
| `ClassCastException` when emitting in Runtime Preview | ViewModel constructor doesn't match interface, OR `ControllerInterface` not on classpath causing `Any()` fallback | Recompile module; confirm interface FQCN matches `io.codenode.{module}.controller.{FlowGraph}ControllerInterface` |
| `NullPointerException` reading `echoed`/`results`/etc. | A `val y: StateFlow<T>` in the interface has no corresponding `yFlow` field on `{FlowGraph}State` | Inspect generated State; field names must match interface getter names (post-Decision 2 prefix derivation) |
| Duplicate-symbol compile error | Legacy `saved/` or base-package `State.kt`/`ViewModel.kt` survived migration | Re-run VS-A1 step 2, OR re-run UI-FBP with `UIFBPSaveOptions(deleteLegacyLocations = true)` |
| `controller/{FlowGraph}Runtime.kt` doesn't compile (`override val y` references missing State field) | `UIFBPStateGenerator` and `RuntimeControllerInterfaceGenerator` disagreed on a port name or its `Flow` suffix | Inspect generated State and ControllerInterface; field names must align (this should be inherent if both generators consume the same `UIFBPSpec`) |
| Production-app `main()` (VS-C2) compiles but `controller.start()` throws at runtime | `{FlowGraph}NodeRegistry::lookup` doesn't resolve a node in the FlowGraph; CodeNode runtime not on classpath | Confirm Source/Sink CodeNodes are generated AND the module's `nodes/` directory was scanned by the build |
| PreviewRegistry shows the module under the wrong key | `PreviewProviderGenerator` registered under composable name instead of flow-graph prefix (regression of Decision 2) | Inspect generated `{FlowGraph}PreviewProvider.kt`; the `register("…")` call must use the flow-graph prefix |
| PreviewRegistry's lambda calls a function that doesn't exist | `PreviewProviderGenerator` invoked the flow-graph prefix as the function name when the user-authored composable name diverges (regression of Decision 2) | Inspect generated PreviewProvider; the function call inside the lambda must use the user-authored Composable name |

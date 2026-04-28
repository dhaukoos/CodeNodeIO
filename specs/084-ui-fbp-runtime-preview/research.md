# Phase 0 Research: UI-FBP Runtime Preview Gap Analysis (post-085)

**Date**: 2026-04-28
**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Cross-check**: [CROSS-CHECK-085.md](./CROSS-CHECK-085.md)

## Purpose

This research consolidates the post-085 runtime contract that any UI-FBP-generated module must satisfy in order for (a) the GraphEditor's Runtime Preview panel to load, render, and execute it, AND (b) a production app to consume it without the GraphEditor. The previous (pre-085) iteration of this document targeted the now-deleted thick stack (`{Module}Controller.kt` + `{Module}ControllerAdapter.kt` + `{Module}Flow.kt` runtime); that scope is fully retired by feature 085's universal-runtime collapse. This document replaces it with the post-collapse decisions.

All decisions below are grounded in source files referenced by absolute path; line numbers are accurate as of the branch starting point (post-085 main, 2026-04-28).

There are no `NEEDS CLARIFICATION` items remaining — all five `Session 2026-04-28` clarifications in the spec resolve every open dimension introduced by feature 085's landing.

---

## Decision 1: Ride the universal runtime collapse — UI-FBP emits `{FlowGraph}ControllerInterface.kt` (extending `ModuleController`) + `{FlowGraph}Runtime.kt` (factory) + `{FlowGraph}PreviewProvider.kt`, identical to the shape every other module now produces

**Rationale**: Feature 085 collapsed the per-module thick runtime trio (Flow + Controller + ControllerAdapter) into a single `controller/{Module}Runtime.kt` factory function `create{Module}Runtime(flowGraph): {Module}ControllerInterface`. The factory constructs a `DynamicPipelineController`, looks up node definitions via a per-module registry, and returns an anonymous `object : {Module}ControllerInterface, ModuleController by controller { override val y = {Module}State.yFlow ... }`. Every reference module (StopWatch, Addresses, UserProfiles, EdgeArtFilter, WeatherForecast) carries this shape, and so does every new module created via feature 085's Generate Module path. UI-FBP modules MUST share this shape so they are deployable via the same `create{Module}Runtime(flowGraph)` consumer pattern documented in `specs/085-collapse-thick-runtime/quickstart.md` VS-D5.

**Generator reuse plan** (no new generator code required for the controller/runtime tier):

- `RuntimeControllerInterfaceGenerator` (commonMain, owned by 085) emits `interface X : ModuleController { val y: StateFlow<T> ... }` from a `FlowGraph` plus the controller package path. **Reused as-is.**
- `ModuleRuntimeGenerator` (commonMain, owned by 085) emits `controller/{FlowGraph}Runtime.kt` from a `FlowGraph` plus the package set. **Reused as-is.**
- Both are already wired into `GenerationPath.UI_FBP` in `CodeGenerationRunner` (verified at `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/runner/CodeGenerationRunner.kt`).

**UIFBPSpec → FlowGraph translation**: UI-FBP today builds a Source CodeNode (whose outputs mirror the Composable's input parameters) and a Sink CodeNode (whose inputs mirror the ViewModel's `StateFlow` properties). Feeding `RuntimeControllerInterfaceGenerator` and `ModuleRuntimeGenerator` requires a `FlowGraph` model with those two nodes. This translation is small (~20–30 lines inside `UIFBPInterfaceGenerator`) and replaces the abandoned pre-085 `UIFBPSpecAdapter` (which would have translated `UIFBPSpec` into the now-deleted `RuntimeControllerSpec`/`RuntimeFlowSpec` inputs).

**Alternatives considered**:

- *Build a UI-FBP-specific Runtime/Interface generator pair.* Rejected — duplicates what 085 already emits, drifts from entity modules, and re-introduces per-path divergence the universal collapse explicitly retired.
- *Skip the controller/runtime tier (UI-FBP modules in Runtime Preview only, not deployable).* Rejected per spec — FR-005 (post-clarification) mandates both Runtime-Preview-loadable AND deployable. Skipping deployability would re-create the WeatherForecast gap that 085 closed.

**Source references**:

- `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/RuntimeControllerInterfaceGenerator.kt` (post-085 — emits `: ModuleController`)
- `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/ModuleRuntimeGenerator.kt`
- `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/runner/CodeGenerationRunner.kt` (UI_FBP path)
- `CodeNodeIO-DemoProject/StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/controller/StopWatchRuntime.kt` (canonical post-085 reference shape)
- `specs/085-collapse-thick-runtime/quickstart.md` VS-D5 (production-app integration template)

---

## Decision 2: `UIFBPSpec` gains a `composableName: String` field separate from `moduleName`/flowGraphPrefix; `PreviewProviderGenerator` takes that as a separate input so the registry key (flow-graph prefix) decouples from the Composable function call (user-authored name)

**Rationale**: Per the 2026-04-28 clarification (Q3), the post-082/083 naming model has three potentially-distinct identifiers:

| Identifier | Source | Used for |
|---|---|---|
| Flow graph prefix | The user-selected `.flow.kt` file's filename minus `.flow.kt` | Generated file prefix; `PreviewRegistry` key (matches `RuntimePreviewPanel`'s lookup); ViewModel/State/ControllerInterface/Runtime class names |
| Module name | The host module's directory / Gradle project name | Package path (`io.codenode.{module-lowercase}.*`). Independent of the flow graph prefix because a module may host multiple flow graphs |
| Composable function name | The qualifying UI source file's `@Composable fun X(viewModel: ...)` declaration | The function the `PreviewProvider` actually invokes inside its `register { ... }` lambda. The user authored this and it may not coincide with either of the above |

Today's `UIFBPSpec.moduleName` field conflates "flow graph prefix", "Composable function name", and historically the module name itself. Splitting the spec into typed fields removes the ambiguity:

```kotlin
data class UIFBPSpec(
    val flowGraphPrefix: String,        // For file/class prefixes + PreviewRegistry key
    val composableName: String,         // For the Composable function call inside PreviewProvider (was conflated with moduleName)
    val packageName: String,            // For on-disk path translation
    val sourceOutputs: List<PortInfo>,  // Source CodeNode outputs
    val sinkInputs: List<PortInfo>,     // Sink CodeNode inputs
    val ipTypeImports: List<String>,    // FQCNs for generated import lines
    // moduleName retained for backward compat, derived from packageName, no longer drives generation
)
```

**Inherited 085 `PreviewProviderGenerator` change**: today the generator emits `PreviewRegistry.register("{moduleName}") { … {moduleName}(viewModel = vm, modifier = modifier) }` where `moduleName = flowGraph.name.pascalCase()`. The first occurrence (registry key) is correct as-is — the flow graph name is what `RuntimePreviewPanel` looks up. The second occurrence (Composable function call) is wrong when the Composable name diverges. The fix is to add a `composableName: String` parameter to `PreviewProviderGenerator.generate(...)`, default to `flowGraph.name.pascalCase()` for backward compat, and emit:

```kotlin
PreviewRegistry.register("$flowGraphPrefix") { viewModel, modifier ->
    val vm = viewModel as ${flowGraphPrefix}ViewModel
    $composableName(viewModel = vm, modifier = modifier)
}
```

Existing 085 callers (every entity module currently using the convention `{Module}() composable name == {Module}State` etc.) get the no-divergence default and continue to work. UI-FBP callers pass the parser-extracted `composableName`.

**Alternatives considered**:

- *Have UI-FBP emit its own `UIFBPPreviewProviderGenerator` that ignores the inherited 085 generator.* Rejected — duplicates ~30 lines of trivial registration code and drifts the two paths. Adding one parameter to the shared generator is strictly less code AND keeps every module's preview-registration shape identical.
- *Force the user to rename the Composable to match the flow graph prefix.* Rejected — violates the post-082/083 design that allows multiple flow graphs per module (potentially each with their own UI file using arbitrary user-authored Composable names). Also infringes user authorial control.
- *Auto-rename inside the generator.* Rejected — silent rewriting of user-authored source is a usability disaster and breaks the user's other call sites.

**Source references**:

- `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/PreviewProviderGenerator.kt` (085 — to be extended)
- `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/parser/UIFBPSpec.kt` (current shape — to be extended)
- `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/parser/UIComposableParser.kt` (already extracts the function name; just needs to populate the new field)
- `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/RuntimePreviewPanel.kt` (uses `flowGraphName` as the registry-key lookup)

---

## Decision 3: `UIFBPViewModelGenerator` emits a constructor `({FlowGraph}ControllerInterface)` and reads flows directly from `{FlowGraph}State` — matching the post-085 entity-module ViewModel shape

**Rationale**: `flowGraph-execute/src/jvmMain/kotlin/io/codenode/flowgraphexecute/ModuleSessionFactory.tryCreateViewModel` attempts ViewModel construction with one of: `(ControllerInterface)` or `(ControllerInterface, Dao)`. If neither matches, it falls back to returning `RuntimeSession(controller, Any(), …)`, and the cast `viewModel as {FlowGraph}ViewModel` inside the `PreviewProvider` throws `ClassCastException` at runtime. So the generated ViewModel **must** declare a public single-arg constructor accepting the typed ControllerInterface.

Today's UI-FBP-generated ViewModel (`TestModule/.../viewmodel/DemoUIViewModel.kt`) has a no-arg constructor and writes directly to `DemoUIState` mutable fields. Required redesign (mirrors `WeatherForecastViewModel` and `AddressesViewModel`):

```kotlin
package io.codenode.{module}.viewmodel

import androidx.lifecycle.ViewModel
import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.{module}.controller.{FlowGraph}ControllerInterface
// + IP type imports

class {FlowGraph}ViewModel(
    private val controller: {FlowGraph}ControllerInterface
) : ViewModel() {
    val {y}: StateFlow<{T}> = {FlowGraph}State.{y}Flow
    val executionState: StateFlow<ExecutionState> = controller.executionState

    fun emit({sourceOutputs as parameters}) {
        {FlowGraph}State._{a}.value = {a}
    }

    // Control methods come via ModuleController-by-controller delegation;
    // the ViewModel can simply re-expose them or call them inline.
}
```

The `emit(...)` body continues to write directly to `{FlowGraph}State._x` mutable fields (preserves today's UI-write semantics). The Source CodeNode's runtime body observes those fields and emits into the FBP graph. Sink-input flows are read from `{FlowGraph}State.{y}Flow` (not via `controller.{y}`) — matches WeatherForecast/Addresses precedent. Same code works under both runtime paths because both mutate the same `{FlowGraph}State` singleton.

**Control method delegation**: post-085, `{FlowGraph}ControllerInterface` extends `ModuleController`, so `start/stop/pause/resume/reset/getStatus/setAttenuationDelay/setEmissionObserver/setValueObserver` are inherited. The ViewModel's `controller: {FlowGraph}ControllerInterface` field can call them directly without redeclaration.

**Alternatives considered**:

- *Keep the no-arg constructor.* Rejected — fails `ModuleSessionFactory.tryCreateViewModel`'s reflection lookup; PreviewProvider cast throws.
- *Read flows through the controller (`val y = controller.y`) instead of from State.* Rejected — diverges from WeatherForecast/Addresses precedent; adds an unnecessary indirection (the controller's flow accessors ultimately resolve to State fields anyway).
- *Use a property setter for the controller.* Rejected — diverges from convention; `ModuleSessionFactory` only matches constructors.

**Source references**:

- `flowGraph-execute/src/jvmMain/kotlin/io/codenode/flowgraphexecute/ModuleSessionFactory.kt` (`tryCreateViewModel` reflection lookup)
- `CodeNodeIO-DemoProject/WeatherForecast/src/commonMain/kotlin/io/codenode/weatherforecast/viewmodel/WeatherForecastViewModel.kt` (reference shape — minimal)
- `CodeNodeIO-DemoProject/Addresses/src/commonMain/kotlin/io/codenode/addresses/viewmodel/AddressesViewModel.kt` (reference shape — with DAO)
- `CodeNodeIO-DemoProject/TestModule/src/commonMain/kotlin/io/codenode/demo/viewmodel/DemoUIViewModel.kt` (current shape — to be revised)

---

## Decision 4: `UIFBPStateGenerator` emits to `viewmodel/` subpackage; `{FlowGraph}State` lives at `io.codenode.{module}.viewmodel.{FlowGraph}State`

**Rationale**: `ModuleSessionFactory` looks up the State at `io.codenode.{module}.viewmodel.{FlowGraph}State` first, with `io.codenode.{module}.{FlowGraph}State` as a fallback. Today's UI-FBP generator emits to the base package directly; entity modules (and post-085 expectation) emit to `viewmodel/`. Aligning UI-FBP avoids the fallback path and matches the canonical layout.

Combined with Decision 6 (legacy `saved/` cleanup), the result is a single canonical State location: `commonMain/kotlin/{packagePath}/viewmodel/{FlowGraph}State.kt`.

**Alternatives considered**: Emit to base package (current behavior). Rejected — accumulates legacy debt; relies on the fallback FQCN.

---

## Decision 5: Build wiring (`jvm()` target + `preview-api` dep) is fully delegated to feature 085's `ModuleGenerator` scaffolding; UI-FBP detects an unscaffolded host and refuses with an actionable error rather than mutating `build.gradle.kts`

**Rationale**: Per the 2026-04-28 clarification (Q4), `ModuleGenerator` (feature 085's module-scaffolding generator) already emits `jvm() { ... }` and `implementation("io.codenode:preview-api")` in `jvmMain` for every module created via the Generate Module path (verified at `flowGraph-generate/.../ModuleGenerator.kt:170, 329, 331`). Modules created by 085's scaffolding have the right wiring out of the box; UI-FBP runs against them with no edits.

For pre-scaffolding-era modules (the only known case is `TestModule`), the user runs a one-time migration documented in `quickstart.md` VS-A1 — adding the `jvm()` block and the `preview-api` dependency by hand (or with the help of a small shell script — see VS-A1). UI-FBP MUST detect a missing `jvm()` target or `preview-api` dep before emitting any output and fail fast with an actionable error pointing at VS-A1.

**Detection heuristic** (in `UIFBPSaveService`):

1. Read host module's `build.gradle.kts` as text.
2. Confirm the file contains `jvm` followed by `{` (allowing whitespace/newlines) — covers `jvm()`, `jvm {`, `jvm(IR)`, `jvm("desktop") { }`.
3. Confirm the file contains `io.codenode:preview-api` (string match — the simplest robust check).
4. If either is missing, return `UIFBPSaveResult(success = false, errorMessage = "host module is unscaffolded; run quickstart.md VS-A1 migration first: missing {jvm()|preview-api dep}")`.

**Alternatives considered**:

- *Auto-insert the missing entries.* Rejected — silent mutation of user-authored build scripts. Risks breaking heavily customized configurations.
- *Run UI-FBP unconditionally and let the compile fail.* Rejected — failure mode is opaque; the user has to dig through compile errors to discover the cause. Failing at the generator surface with a precise message is the correct UX.

**Source references**:

- `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/ModuleGenerator.kt` (085's scaffolding generator — already emits the right entries)
- `CodeNodeIO-DemoProject/StopWatch/build.gradle.kts:65` (`implementation("io.codenode:preview-api")`)
- `CodeNodeIO-DemoProject/TestModule/build.gradle.kts` (legacy — needs the one-time migration)

---

## Decision 6: Remove the legacy `saved/` package; UI-FBP regenerates only into `viewmodel/`. Migration is opt-in via `UIFBPSaveOptions(deleteLegacyLocations = true)`, default off

**Rationale**: `TestModule/src/commonMain/kotlin/io/codenode/demo/saved/` contains a parallel pair of `DemoUIState.kt` and `DemoUIViewModel.kt` that conflict with the same files in `viewmodel/`. The `userInterface/DemoUI.kt` currently imports from `saved/`. Today's UI-FBP runs to base package; tomorrow it emits to `viewmodel/` (Decision 4). Three alternatives for handling the duplicate:

1. **Always delete legacy locations** when re-generating. Risks deleting user-edited files.
2. **Leave them in place.** Fails SC-002 (clean compile) due to duplicate symbol declarations and breaks the `ModuleSessionFactory` State lookup.
3. **Opt-in via `UIFBPSaveOptions(deleteLegacyLocations = true)`, default off.** ✓ Caller (UI-FBP UI) decides; `quickstart.md` VS-A1 documents the one-time TestModule migration as the canonical opt-in trigger.

The opt-in posture composes with the FR-016 hand-edit safety check: even when `deleteLegacyLocations = true`, files lacking the `Generated by CodeNodeIO` marker are NOT deleted (they're surfaced via `SKIPPED_CONFLICT` in the structured `UIFBPSaveResult`).

**Alternatives considered** — all enumerated above; the opt-in default-off variant is the lowest-risk choice.

**Source references**:

- `CodeNodeIO-DemoProject/TestModule/src/commonMain/kotlin/io/codenode/demo/saved/` (legacy duplicate)
- `flowGraph-generate/src/jvmMain/kotlin/io/codenode/flowgraphgenerate/runner/GenerationFileWriter.kt` (post-085 reference for marker-comment detection — `carriesGeneratorMarker`)

---

## Decision 7: Re-generation preserves user-added content of `.flow.kt` by merging at the FlowGraph DSL level via `FlowKtParser` + `FlowGraphSerializer`, never by text rewriting

**Rationale**: FR-011/FR-012 require that re-running UI-FBP not destroy user-added CodeNodes and connections. The mechanism:

1. If `.flow.kt` does not exist, emit the bootstrap (today's behavior, preserved).
2. If `.flow.kt` exists, parse it via `flowGraph-persist/FlowKtParser` into a `FlowGraph` model. Compute the new Source/Sink CodeNode port sets from the post-clarification `UIFBPSpec`. Update existing CodeNodes in-place: add new ports, remove ports the user no longer has in the UI, drop only those connections that reference removed ports. Re-serialize via `flowGraph-persist/FlowGraphSerializer`.
3. Surface a structured `FlowKtMergeReport` (mode = CREATED/UPDATED/UNCHANGED/PARSE_FAILED_SKIPPED, plus port-level diff and dropped-connection list).

If parsing fails (the user manually edited `.flow.kt` into a non-parseable shape), the generator MUST NOT overwrite — emit an error and direct the user to fix or remove the file (FR-016 spirit applied to graph contents). Mode `PARSE_FAILED_SKIPPED` records this.

**Alternatives considered**:

- *Always overwrite with bootstrap.* Rejected — destroys user work; fails FR-011/FR-012.
- *Append-only (never modify existing CodeNodes).* Rejected — won't propagate UI-signature changes; user would need to manually edit Source/Sink ports.
- *Diff at the text level.* Rejected — fragile; `.flow.kt` is structured DSL and round-trips cleanly through the existing parser/serializer.

**Source references**:

- `flowGraph-persist/src/jvmMain/kotlin/io/codenode/flowgraphpersist/serialization/FlowKtParser.kt`
- `flowGraph-persist/src/jvmMain/kotlin/io/codenode/flowgraphpersist/serialization/FlowGraphSerializer.kt` (or its commonMain analog)

---

## Decision 8: All new generators stay in `commonMain` of `flowGraph-generate`; the filesystem-touching `UIFBPSaveService` lives in `jvmMain`

**Rationale**: Existing UI-FBP generators are all in `commonMain` (verified). They produce `String` content from typed input and have no I/O, which keeps them pure and unit-testable in `commonTest` without filesystem fixtures. The merge logic for `.flow.kt` (Decision 7), the `build.gradle.kts` detect-and-refuse (Decision 5), legacy `saved/` cleanup (Decision 6), and structured-result emission are I/O- and project-aware and live in a new `UIFBPSaveService` in `jvmMain`. This service composes the pure generators, calls the `flowGraph-persist` parser/serializer (also commonMain but invoked from jvm), writes outputs, and emits `UIFBPSaveResult`. Tests for `UIFBPSaveService` live in `jvmTest` against tmpdir fixtures.

This is the same `commonMain pure / jvmMain orchestrator` discipline established by feature 085's `GenerationFileWriter` and respected throughout `flowGraph-generate`.

**Alternatives considered**:

- *Put everything in `jvmMain`.* Rejected — breaks KMP-first principle.
- *Put I/O in `commonMain` via `okio`.* Rejected — adds a dependency for marginal benefit; the only consumer is the desktop GraphEditor.

---

## Decision 9: UI-FBP takes its inputs as an explicit `{flow graph (.flow.kt), qualifying UI file}` pair from the user via GraphEditor file selectors, mirroring feature 085's Generate Module pattern; no implicit module scanning

**Rationale**: Per the 2026-04-28 clarification (Q5), features 082/083 made multiple flow graphs per module the norm. A given module can host any number of `.flow.kt` files and any number of qualifying UI files. Implicit scanning to "discover" the right pair would either (a) refuse with a confusing error when more than one of either kind exists, or (b) silently pick a first match and surprise the user. Both fail.

The clean pattern: the user explicitly selects both inputs in the GraphEditor's file selectors (the same pattern already in production use for feature 085's Generate Module path's `.flow.kt` selector). UI-FBP's caller (the GraphEditor's UI-FBP code-generation entry point) supplies both as explicit arguments; the generator does no scanning and refuses to operate without both.

This also retires the pre-clarification FR-017 ("refuse on more than one qualifying UI file") — that was a workaround for the ambiguity the explicit-pair model removes structurally.

**Alternatives considered**:

- *Implicit scan, refuse on ambiguity.* Rejected — confusing error UX; doesn't compose with the multi-flow-graph-per-module reality.
- *Implicit scan, silent first-match.* Rejected — surprising; violates principle of least astonishment.
- *Caller supplies pair OR scan if there's exactly one of each.* Rejected — adds branching for marginal benefit; explicit-pair is the only path that scales to the post-082/083 model.

**Source references**:

- `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/CodeGeneratorPanel.kt` (085's Generate Module file selector — pattern to mirror)
- `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/viewmodel/CodeGeneratorViewModel.kt` (`selectFlowGraphFile` already wired for Generate Module; UI-FBP path will reuse the pattern)

---

## Summary: Closing-the-Gap Checklist (post-085)

| Artifact | Today (UI-FBP) | Target | Owner |
|---|---|---|---|
| `{FlowGraph}State.kt` | Emitted to base package; conflicts with `viewmodel/` copy in TestModule | Emit to `viewmodel/`; prefix from flow-graph (not module) | `UIFBPStateGenerator` (path + prefix change) |
| `{FlowGraph}ViewModel.kt` | No-arg constructor; flows from State directly | `({FlowGraph}ControllerInterface)` constructor; flows from State; control methods through controller; prefix from flow-graph | `UIFBPViewModelGenerator` (signature + prefix change) |
| `{FlowGraph}SourceCodeNode.kt` | Emitted to `nodes/` | `nodes/`; prefix from flow-graph | `UIFBPSourceCodeNodeGenerator` (prefix change minimal) |
| `{FlowGraph}SinkCodeNode.kt` | Emitted to `nodes/` | `nodes/`; prefix from flow-graph | `UIFBPSinkCodeNodeGenerator` (prefix change minimal) |
| `{FlowGraph}ControllerInterface.kt` | **Not generated** | `controller/`; extends `ModuleController` | **REUSE** `RuntimeControllerInterfaceGenerator` (085-owned, no UI-FBP-specific generator needed) |
| `{FlowGraph}Runtime.kt` | **Not generated** | `controller/`; factory `create{FlowGraph}Runtime(flowGraph)` | **REUSE** `ModuleRuntimeGenerator` (085-owned) |
| `{FlowGraph}PreviewProvider.kt` | **Not generated** | `jvmMain/.../userInterface/`; registers under flow-graph prefix; calls user-authored Composable name | **EXTEND** `PreviewProviderGenerator` (085-owned) — add `composableName` parameter |
| `.flow.kt` | Optional bootstrap | Parse-and-merge if present; bootstrap if absent | `UIFBPSaveService` (new) |
| `build.gradle.kts` | Untouched | Detect-and-refuse if missing `jvm()` or `preview-api`; do NOT mutate | `UIFBPSaveService` (new) — detection only |
| Legacy `saved/` package | Present in TestModule | One-time delete (opt-in via `UIFBPSaveOptions(deleteLegacyLocations = true)`) + import-path fix in `userInterface/DemoUI.kt` | `quickstart.md` VS-A1 |

**No NEEDS CLARIFICATION items remain.** All technical-context fields in the plan are populated from inspected source. Spec Clarifications Sessions 2026-04-26, 2026-04-27, and 2026-04-28 have resolved every open dimension.

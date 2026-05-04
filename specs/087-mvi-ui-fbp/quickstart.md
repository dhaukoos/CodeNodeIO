# Phase 1 Quickstart: MVI Pattern for UI-FBP Interface Generation

This document defines the manual-verification scenarios for feature 087.
Acceptance scenarios from the spec map onto these vertical slices.

## Prerequisites

- Branch `087-mvi-ui-fbp` checked out.
- Local `CodeNodeIO-DemoProject` clone at `/Users/dhaukoos/CodeNodeIO-DemoProject/`
  (TestModule contains the DemoUI flow graph used as the P2 migration target).
- GraphEditor builds and launches: `./gradlew :graphEditor:run` succeeds.

## VS-A: Generator change verified in isolation (US1)

### VS-A1 — RED phase

Run the modified + new fixture-based generator tests; they should fail (no
implementation yet) under Design B:

```sh
./gradlew :flowGraph-generate:commonTest --tests "io.codenode.flowgraphgenerate.generator.UIFBPStateGeneratorTest"
./gradlew :flowGraph-generate:commonTest --tests "io.codenode.flowgraphgenerate.generator.UIFBPEventGeneratorTest"
./gradlew :flowGraph-generate:commonTest --tests "io.codenode.flowgraphgenerate.generator.UIFBPViewModelGeneratorTest"
./gradlew :flowGraph-generate:commonTest --tests "io.codenode.flowgraphgenerate.generator.UIFBPSinkCodeNodeGeneratorTest"
./gradlew :flowGraph-generate:commonTest --tests "io.codenode.flowgraphgenerate.generator.UIFBPSourceCodeNodeGeneratorTest"
./gradlew :flowGraph-generate:commonTest --tests "io.codenode.flowgraphgenerate.generator.UIFBPControllerInterfaceTest"
./gradlew :flowGraph-generate:commonTest --tests "io.codenode.flowgraphgenerate.generator.UIFBPRuntimeFactoryTest"
```

Expected: tests compile, all NEW assertions FAIL (Constitution §II RED).

### VS-A2 — GREEN phase

After implementing the generator changes:

```sh
./gradlew :flowGraph-generate:check
```

Expected: BUILD SUCCESSFUL. All fixture tests pass.

### VS-A3 — Determinism check (SC-005)

Run a synthetic generate twice; the outputs MUST be byte-identical:

```sh
./gradlew :flowGraph-generate:commonTest --tests "*ViewModelGeneratorTest.generate is byte-identical*"
./gradlew :flowGraph-generate:commonTest --tests "*StateGeneratorTest.generate*byte-identical*"
./gradlew :flowGraph-generate:commonTest --tests "*EventGeneratorTest.generate*byte-identical*"
```

## VS-B: DemoUI module migrated end-to-end (US2)

### VS-B1 — Regenerate

In the GraphEditor, open the DemoUI flow graph
(`TestModule/src/commonMain/kotlin/io/codenode/testmodule/flow/DemoUI.flow.kt`),
trigger UI-FBP code generation. Verify on-disk (Design B):

- `TestModule/.../viewmodel/DemoUIState.kt` is now a
  `data class DemoUIState(val results: CalculationResults? = null)`.
- `TestModule/.../viewmodel/DemoUIEvent.kt` is a NEW file
  (`sealed interface DemoUIEvent { data class UpdateNumA(...) ; data class UpdateNumB(...) }`).
- `TestModule/.../viewmodel/DemoUIViewModel.kt` exposes
  `state: StateFlow<DemoUIState>` + `fun onEvent(DemoUIEvent)`; `onEvent`
  branches call `controller.emitNumA(...)` / `controller.emitNumB(...)`.
- `TestModule/.../controller/DemoUIControllerInterface.kt` adds
  `fun emitNumA(value: Double)` + `fun emitNumB(value: Double)` (additive).
- `TestModule/.../controller/DemoUIRuntime.kt` declares per-flow-graph
  `MutableStateFlow<CalculationResults?>` (sink) + `MutableSharedFlow<Double>`
  (source ports) and wires them via `DemoUISinkCodeNode.withReporters(...)`
  / `DemoUISourceCodeNode.withSources(...)`.
- `TestModule/.../nodes/DemoUISinkCodeNode.kt` is the `object` definition
  with default no-op `createRuntime` + `withReporters(vararg)` wrapper.
- `TestModule/.../nodes/DemoUISourceCodeNode.kt` is the `object` definition
  with default never-emit `createRuntime` + `withSources(vararg)` wrapper.
- **NO file named `DemoUIStateStore.kt`** — Design B eliminates the
  singleton; the prior `DemoUIState.kt` (singleton object) is REPLACED, not
  preserved under a new name.

### VS-B2 — Update the hand-written DemoUI Screen

Edit
`TestModule/src/commonMain/kotlin/io/codenode/testmodule/userInterface/DemoUI.kt`
to use the new two-parameter signature:

```kotlin
@Composable
fun DemoUI(state: DemoUIState, onEvent: (DemoUIEvent) -> Unit) {
    // … render based on state.results
    // … on-button-tap: onEvent(DemoUIEvent.UpdateNumA(123.0))
}
```

Update DemoUI's preview/root binding (the host module's own composable that
wires the ViewModel to the Screen) to collect `viewModel.state` and pass
`viewModel::onEvent`. Remove the now-unused `DemoUIAction.kt` and
`DemoUIStateMVI.kt` placeholder files.

### VS-B3 — Compile + Runtime Preview

```sh
./gradlew :TestModule:check
```

Expected: BUILD SUCCESSFUL. Then launch the GraphEditor:

```sh
./gradlew :graphEditor:run
```

Open the DemoUI flow graph, hit Runtime Preview. Expected:

- The DemoUI screen renders identically to the pre-migration baseline.
- Every documented interaction (button tap, displayed result update)
  produces the same observable behaviour.
- The execution state indicator (start/stop/pause) works exactly as before.

## VS-C: P3 — second DemoProject UI module migrated with no generator changes (SC-004)

### VS-C1 — Pick a P3 candidate

Candidates (UI-FBP-shaped modules, identified by `find` over
`/Users/dhaukoos/CodeNodeIO-DemoProject`): WeatherForecast, EdgeArtFilter,
UserProfiles, Addresses, StopWatch.

### VS-C2 — Regenerate + update Screen

Same procedure as VS-B but on the chosen module. Critically: the GENERATOR
must be unchanged from the P2 migration. Any per-module branching in the
generator is a SC-004 failure.

### VS-C3 — Verify

```sh
./gradlew :{ChosenModule}:check
./gradlew :graphEditor:run
```

Expected: module compiles, Runtime Preview renders + behaves identically.

## VS-D: Atomicity on generator failure (FR-010)

### VS-D1

Force a generator failure (e.g., supply a malformed UIFBPSpec via a unit
test). Verify:

- The on-disk file set in the host module's `viewmodel/` package is the
  PRIOR generation's output, untouched.
- No partial files are written.
- The generator's return value is `UIFBPGenerateResult(success = false,
  errorMessage = ...)`.

## VS-E: Adding a new intent post-migration (SC-003)

### VS-E1

After P2 migration, add one new sourceOutput port to DemoUI's flow graph.
Regenerate. Then verify the change footprint is ≤ 3 file edits in the host
module (state property addition, event case addition, ViewModel onEvent
branch addition — all auto-generated; the only hand-edit is the DemoUI
Screen call site that raises the new event). Count the diff with
`git diff --stat`.

## VS-F: Multi-instance state isolation (Design B SSOT guarantee)

### VS-F1

Construct two `DemoUIViewModel` instances in a test, each with its own
`create{Name}Runtime(flowGraph)` controller. Drive `onEvent` on one and
verify the other's `state` is unaffected. Drive a sink-port emission on
one controller's flow and verify only the corresponding ViewModel's
`_state` updates. This proves Design B's test-isolation guarantee — a
property the prior singleton design could not satisfy.

```sh
./gradlew :flowGraph-generate:commonTest --tests "*MultiInstanceIsolationTest*"
```

## Exit criteria

- VS-A1 → A3: generator-side TDD complete; tests green at HEAD.
- VS-B1 → B3: DemoUI migrated; Runtime Preview parity verified by hand.
- VS-C1 → C3: at least one second module migrated with zero generator
  changes between P2 and P3.
- VS-D1: atomicity verified.
- VS-E1: SC-003 budget verified.
- VS-F1: multi-instance state isolation verified (Design B SSOT
  guarantee).

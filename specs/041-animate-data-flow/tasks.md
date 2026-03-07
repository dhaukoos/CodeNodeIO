# Tasks: Animate Data Flow

**Input**: Design documents from `/specs/041-animate-data-flow/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/animation-controller-api.md, quickstart.md

**Tests**: Not explicitly requested — test tasks omitted.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story. US1 (Toggle + Animation) and US2 (Threshold Gating) are combined since the toggle and threshold are tightly coupled UI elements.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Foundational (Emission Callback Infrastructure)

**Purpose**: Add the `onEmit` callback to `NodeRuntime` and instrument all runtime classes to invoke it on IP emission. Add `setEmissionObserver` to `ModuleController` interface and implement it in generated controllers. This BLOCKS all user story work.

**CRITICAL**: No animation work can begin until this phase is complete.

- [ ] T001 Add `onEmit` callback property to `NodeRuntime` in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/NodeRuntime.kt` — add `var onEmit: ((String, Int) -> Unit)? = null` property. This optional callback is invoked when the node emits an IP on an output port, receiving `(nodeId, portIndex)`.
- [ ] T002 Add `setEmissionObserver(observer: ((String, Int) -> Unit)?)` to `ModuleController` interface in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/ModuleController.kt` — add the method signature to the interface. When called with a non-null observer, all runtimes in the module should have their `onEmit` set to that observer. When called with `null`, all `onEmit` callbacks are cleared.
- [ ] T003 [P] Instrument single-output runtime classes with `onEmit` invocation — add `onEmit?.invoke(codeNode.id, 0)` immediately after each successful `send()` call in these files under `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/`: `SourceRuntime.kt` (after `outputChannel?.send(value)`, line ~81), `TransformerRuntime.kt` (after `outChannel.send(transformed)`, line ~91), `FilterRuntime.kt` (after the output send), `In2Out1Runtime.kt` (after the single output send), `In3Out1Runtime.kt` (after the single output send), `In2AnyOut1Runtime.kt` (after the single output send), `In3AnyOut1Runtime.kt` (after the single output send). Total: 7 files, one `onEmit` call each.
- [ ] T004 [P] Instrument multi-output runtime classes with `onEmit` invocation — add `onEmit?.invoke(codeNode.id, portIndex)` immediately after each successful `send()` call in these files under `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/`: `SourceOut2Runtime.kt` (portIndex 0 and 1), `SourceOut3Runtime.kt` (portIndex 0, 1, 2), `In1Out2Runtime.kt` (portIndex 0, 1), `In1Out3Runtime.kt` (portIndex 0, 1, 2), `In2Out2Runtime.kt` (portIndex 0, 1), `In2Out3Runtime.kt` (portIndex 0, 1, 2), `In3Out2Runtime.kt` (portIndex 0, 1), `In3Out3Runtime.kt` (portIndex 0, 1, 2), `In2AnyOut2Runtime.kt` (portIndex 0, 1), `In2AnyOut3Runtime.kt` (portIndex 0, 1, 2), `In3AnyOut2Runtime.kt` (portIndex 0, 1), `In3AnyOut3Runtime.kt` (portIndex 0, 1, 2). Total: 12 files.
- [ ] T005 [P] Implement `setEmissionObserver` override in `StopWatchController` in `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/generated/StopWatchController.kt` — add a property `private var emissionObserver: ((String, Int) -> Unit)? = null` and implement `override fun setEmissionObserver(observer: ((String, Int) -> Unit)?)` that stores the observer and iterates over all runtime instances in the flow (accessible via the flow class properties) to set each runtime's `onEmit = observer`. Also update the `start()` method to re-apply the stored observer after starting runtimes (since runtimes may be recreated on restart).
- [ ] T006 [P] Implement `setEmissionObserver` override in `UserProfilesController` in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/generated/UserProfilesController.kt` — same pattern as T005: add the `emissionObserver` property, implement the override to iterate all runtimes in the flow, and re-apply observer on start().

**Checkpoint**: `./gradlew :fbpDsl:compileKotlinJvm :StopWatch:compileKotlinJvm :UserProfiles:compileKotlinJvm` compiles without errors.

---

## Phase 2: User Story 1 + 2 — Toggle, Dot Animation, and Threshold Gating (Priority: P1)

**Goal**: User can enable an "Animate Data Flow" toggle (gated by 500ms attenuation threshold) and see animated dots traveling along connection curves when IPs are emitted during runtime execution. Toggle is automatically disabled/deactivated when attenuation is below threshold.

**Independent Test**: Load StopWatch module → set attenuation to 1000ms → enable "Animate Data Flow" toggle → click Start → observe dots traveling along connection curves. Set attenuation below 500ms → toggle is disabled. Stop execution → dots clear immediately.

### Implementation for User Stories 1 + 2

- [ ] T007 [US1] Create `ConnectionAnimation` data class in `circuitSimulator/src/commonMain/kotlin/io/codenode/circuitsimulator/ConnectionAnimation.kt` — define `data class ConnectionAnimation(val connectionId: String, val startTimeMs: Long, val durationMs: Long)` with methods `fun progress(currentTimeMs: Long): Float` (returns elapsed/duration clamped to [0,1]) and `fun isComplete(currentTimeMs: Long): Boolean` (returns progress >= 1.0).
- [ ] T008 [US1] Create `DataFlowAnimationController` in `circuitSimulator/src/commonMain/kotlin/io/codenode/circuitsimulator/DataFlowAnimationController.kt` — class with: `activeAnimations: StateFlow<List<ConnectionAnimation>>` exposing current animations; `fun createEmissionObserver(flowGraph: FlowGraph, attenuationMs: () -> Long): (String, Int) -> Unit` that returns a callback which maps (nodeId, portIndex) to connection IDs via FlowGraph lookup (find connections where `sourceNodeId == nodeId` and source port matches the output port at `portIndex`) and adds new `ConnectionAnimation` entries with `durationMs = (attenuationMs() * 0.8).toLong()`; `fun startFrameLoop(scope: CoroutineScope)` that launches a coroutine updating the animation list every ~16ms to remove completed animations; `fun stopFrameLoop()` to cancel the frame loop; `fun pause()` to record pause timestamp and freeze progress; `fun resume()` to adjust startTimeMs of all active animations by the paused duration; `fun clear()` to remove all animations immediately. Import `FlowGraph` from `io.codenode.fbpdsl.model`.
- [ ] T009 [US1] Extend `RuntimeSession` in `circuitSimulator/src/commonMain/kotlin/io/codenode/circuitsimulator/RuntimeSession.kt` — add: `val animationController = DataFlowAnimationController()`; `private val _animateDataFlow = MutableStateFlow(false)` and `val animateDataFlow: StateFlow<Boolean>`; `val animationAttenuationThreshold: Long = 500L`; `fun setAnimateDataFlow(enabled: Boolean)` that only activates if `_attenuationDelayMs.value >= animationAttenuationThreshold`, creates emission observer via `animationController.createEmissionObserver()` using a stored `FlowGraph` reference, and calls `controller.setEmissionObserver()`. Modify `start()` to start animation frame loop and wire observer if `animateDataFlow` is true, using the FlowGraph returned by `controller.start()`. Modify `stop()` to call `animationController.clear()`, `animationController.stopFrameLoop()`, and `controller.setEmissionObserver(null)`. Modify `pause()` to call `animationController.pause()`. Modify `resume()` to call `animationController.resume()`. Modify `setAttenuation()` to auto-deactivate animation if new value < threshold (call `setAnimateDataFlow(false)`). Add a `CoroutineScope` parameter or create an internal scope for the frame loop.
- [ ] T010 [US1] Add "Animate Data Flow" toggle to `RuntimePreviewPanel` in `graphEditor/src/jvmMain/kotlin/ui/RuntimePreviewPanel.kt` — in the Speed Attenuation section, after the range labels Row (0ms / 2000ms) and before the Divider (line ~258), add a Row with `Text("Animate Data Flow", fontSize = 11.sp)` on the left and a `Switch(checked = animateDataFlow, onCheckedChange = { onAnimateDataFlowChanged(it) }, enabled = runtimeSession != null && attenuationMs >= 500L)` on the right. Add new parameters to the composable function: `animateDataFlow: Boolean = false` and `onAnimateDataFlowChanged: (Boolean) -> Unit = {}`. When attenuation is below 500ms and the switch would be disabled, add a subtle hint text below: `Text("Requires ≥500ms attenuation", fontSize = 9.sp, color = Color.Gray)`.
- [ ] T011 [US1] Add animation dot rendering to `FlowGraphCanvas` in `graphEditor/src/jvmMain/kotlin/ui/FlowGraphCanvas.kt` — add `activeAnimations: List<ConnectionAnimation> = emptyList()` parameter to both `FlowGraphCanvas` and `FlowGraphCanvasWithViewModel` composables. Inside the Canvas draw block, after drawing connections (line ~502) and before drawing nodes (line ~547), add a new rendering pass: for each `ConnectionAnimation` in `activeAnimations`, find the matching connection by `connectionId`, compute source/target port screen positions (same logic as `drawConnection`), compute Bezier control points (same as `drawBezierConnection`), interpolate position using the existing `cubicBezier()` function (line ~1019) with `t = animation.progress(currentTimeMs)`, and draw a filled circle at the interpolated position with radius `6f * scale` and color matching the connection (use `connectionColors[connectionId]` or default `Color(0xFF424242)`). Add a `LaunchedEffect` that triggers continuous recomposition while `activeAnimations` is non-empty using `withFrameMillis` to read current time into a state variable.
- [ ] T012 [US1] Wire animation state in `Main.kt` in `graphEditor/src/jvmMain/kotlin/Main.kt` — collect `runtimeSession?.animateDataFlow?.collectAsState()` and `runtimeSession?.animationController?.activeAnimations?.collectAsState()`. Pass `animateDataFlow` and `onAnimateDataFlowChanged = { runtimeSession?.setAnimateDataFlow(it) }` to `RuntimePreviewPanel`. Pass `activeAnimations` list to `FlowGraphCanvas` (or `FlowGraphCanvasWithViewModel`). Ensure animation state is collected as `emptyList()` when `runtimeSession` is null.

**Checkpoint**: StopWatch animation works: set attenuation ≥500ms, enable toggle, start execution, observe dots. Toggle disables below 500ms. Stop clears all dots. Pause freezes dots, resume continues them.

---

## Phase 3: User Story 3 — Module-Agnostic Animation (Priority: P2)

**Goal**: The animation system works generically for any loaded flow graph — dots animate along UserProfiles connection curves with the same visual behavior as StopWatch.

**Independent Test**: Load UserProfiles module → set attenuation to 1000ms → enable "Animate Data Flow" → start execution → observe dots animating along UserProfiles' connection curves with identical visual behavior to StopWatch.

### Implementation for User Story 3

- [ ] T013 [US3] Verify module-agnostic animation for UserProfiles — load UserProfiles in graphEditor, set attenuation to 1000ms, enable animation, start execution, and confirm dots animate along UserProfiles' connection curves. If any module-specific issues arise (e.g., connection lookup failures, missing runtime instrumentation), fix them. The architecture from US1 should handle this generically via FlowGraph connection lookup, but verify edge cases like multi-output nodes and fan-out connections.

**Checkpoint**: Both StopWatch and UserProfiles modules animate correctly with identical visual behavior.

---

## Phase 4: Polish & Cross-Cutting Concerns

**Purpose**: Edge case handling, module switch cleanup, and end-to-end validation.

- [ ] T014 Ensure module switch clears animations in `graphEditor/src/jvmMain/kotlin/Main.kt` — verify that the existing `DisposableEffect(runtimeSession)` (which calls `stop()` on the old session) also clears animation state and stops the frame loop. If the `runtimeSession` changes via `remember(moduleRootDir)`, the old session's `stop()` should call `animationController.clear()` and `animationController.stopFrameLoop()`. Verify no stale animation dots persist after switching modules.
- [ ] T015 Run quickstart.md end-to-end validation — follow the manual test flow in `specs/041-animate-data-flow/quickstart.md`: execute all 8 verification steps (V1-V8) covering toggle visibility, threshold gating, dot animation, pause/resume, stop clearing, toggle-off during execution, UserProfiles animation, and module switch cleanup.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: No dependencies — can start immediately. BLOCKS all user story work.
- **US1+US2 (Phase 2)**: Depends on Phase 1 completion (emission callbacks must be wired before animation can work)
- **US3 (Phase 3)**: Depends on Phase 2 (needs working animation to verify across modules)
- **Polish (Phase 4)**: Depends on Phases 2 and 3

### User Story Dependencies

- **US1+US2 (P1)**: Can start after Foundational — core animation + threshold gating
- **US3 (P2)**: Depends on US1+US2 — verifies generic behavior established by US1

### Within Each Phase

- T001 (onEmit property) and T002 (setEmissionObserver interface) must complete before T003/T004/T005/T006
- T003 and T004 can run in parallel (different runtime files)
- T005 and T006 can run in parallel (different controller files)
- T003/T004 and T005/T006 can run in parallel (different modules)
- T007 (ConnectionAnimation) must complete before T008 (DataFlowAnimationController)
- T008 must complete before T009 (RuntimeSession extensions)
- T009 must complete before T010 (UI toggle) and T011 (dot rendering)
- T010 and T011 can run in parallel (different files)
- T012 (Main.kt wiring) depends on T009, T010, and T011

### Parallel Opportunities

- T003 and T004 can run in parallel (different runtime files, same pattern)
- T005 and T006 can run in parallel (different controller files, same pattern)
- T010 and T011 can run in parallel (different UI files)

---

## Implementation Strategy

### MVP First (Phase 1 → Phase 2)

1. Complete Phase 1: Emission callback infrastructure
2. Complete Phase 2: US1+US2 — toggle, animation, threshold gating
3. **STOP and VALIDATE**: StopWatch animation works end-to-end
4. Demo if ready

### Incremental Delivery

1. Foundational → Emission callbacks wired, controllers instrumented
2. Add US1+US2 → Toggle, dots, threshold gating → Demo (MVP!)
3. Add US3 → Verify UserProfiles animation → Demo (full feature)
4. Polish → Edge cases, module switch cleanup, full validation

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- US1 and US2 are combined because the toggle (US1) and threshold gating (US2) are inseparable UI elements — the toggle inherently requires threshold logic
- US3 is mostly validation since the architecture is designed generically from the start
- The `onEmit` callback in each runtime class is a single line addition after each `send()` call
- StopWatch and UserProfiles controllers are in `generated/` directories (gitignored) — use `git add -f` when committing
- The `cubicBezier()` function already exists in FlowGraphCanvas.kt (line ~1019) — reuse it for dot position interpolation
- Commit after each task or logical group

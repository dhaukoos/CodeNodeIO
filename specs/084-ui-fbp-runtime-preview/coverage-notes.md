# Feature 084 Coverage Notes (T048 / T049 / T054)

Walks the three coverage axes the polish phase requires and records the
test/quickstart/code path that satisfies each. Generated as part of T048–T054.

## 1. Functional Requirement coverage (T048)

| FR | Coverage path |
|---|---|
| FR-001 (Runtime Preview discovers + renders without hand edits) | T023 (`UIFBPModuleSessionFactoryTest`) + manual VS-A4 (T027), VS-B3 (T028) |
| FR-002 (preview-registration entry point) | T015 (`UIFBPInterfaceGeneratorTest` — entry 7 PreviewProvider with `composableName`) + T024 (UIFBPSaveService writes it) |
| FR-003 (controller-interface contract for runtime session factory) | T015 (entry 5 emits `interface X : ModuleController` extension) + T023 (`UIFBPModuleSessionFactoryTest` reflects through it) |
| FR-004 (runtime flow representation that DynamicPipeline can execute) | T015 (entry 6 emits `create{FlowGraph}Runtime(flowGraph): {FlowGraph}ControllerInterface`) + T030 (passthrough end-to-end test) |
| FR-005 (post-085 universal artifact set with `{FlowGraph}` prefix) | T015 (8-entry set assertion) + T016 (orchestrator implementation) + T002–T004 (typed-identifier `UIFBPSpec`) |
| FR-006 (artifact placement matches discovery) | T018 (`first save … emits the post-085 universal set` — asserts every entry's path) |
| FR-007 (sufficiency for working virtual circuit) | Manual VS-A5 (T032 passthrough) + manual VS-B4 (Quickstart084 passthrough) |
| FR-008 (clean compile on standard build) | Manual VS-A3 (T026) + `./gradlew :TestModule:compileKotlinJvm` runs clean post-merge |
| FR-009 (refuse unscaffolded host with actionable error) | T019 (`unscaffolded host with no jvm target and no preview-api dep is refused with actionable error`) + manual VS-A9 (T040) |
| FR-010 (legacy duplication resolved; opt-in cleanup) | T033 (`legacy saved cleanup … deletes duplicate files`) + T034 (`marker absent leaves files in place`) + T037 (impl) |
| FR-011 (idempotent re-run) | T036 (`target file with marker AND content already matching is UNCHANGED`) + T041 (`re-save against unchanged spec produces UNCHANGED .flow.kt merge mode`) + manual VS-A6 (T046) |
| FR-012 (port-shape change preserves user-added CodeNodes) | T042 (`port-add scenario adds a Source output without disturbing user-added CodeNodes`) + T043 (`port-remove scenario drops only invalid connections and reports them`) + manual VS-A7 (T046) |
| FR-013 (structured summary of changes) | `UIFBPSaveResult` shape defined in T021/T022; populated across all save tests; surfaced via `CodeGeneratorViewModel.generateUIFBP` to GraphEditor status line (T024) |
| FR-014 (operates against existing module; not responsible for scaffolding) | T019 + T024 (no scaffolding paths inside `UIFBPSaveService`) |
| FR-015 (explicit `{flow graph, UI file}` pair input via GraphEditor selectors) | T024 (`CodeGeneratorPanel` emits two `OutlinedButton` selectors, one per input); FileDialogUtils filenameFilter parameterization picks the right files for each |
| FR-016 (no silent overwrite of hand-written files) | T035 (`target file lacking the Generated marker is SKIPPED_CONFLICT`) + manual VS-A8 (T039) |

**Result**: every FR-001..FR-016 has at least one automated test and/or manual
quickstart leg. No follow-up tasks needed.

## 2. Success Criterion coverage (T049)

| SC | How verified |
|---|---|
| SC-001 (Runtime Preview within 30s, no errors, no manual edits) | Manual VS-A4 (T027) + VS-B3 (T028); both completed cleanly within the time budget |
| SC-002 (clean compile on first attempt, zero generator-attributable errors) | Manual VS-A3 (T026) + `./gradlew :TestModule:compileKotlinJvm` runs clean post-merge; `./gradlew :flowGraph-generate:check` green (T051); `./gradlew test` green (T050) |
| SC-003 (passthrough end-to-end within 1s of input) | Manual VS-A5 (T032) confirmed visible UI-input → graph → UI-output round-trip on Emit |
| SC-004 (re-run unchanged spec yields zero changes) | T036 + T041 prove zero file mutations on idempotent re-save (mtime preserved); manual VS-A6 (T046) confirms |
| SC-005 (≥100% of valid user-added CodeNodes preserved; dropped connections reported) | T042 + T043 prove preservation across port-shape changes with structured `connectionsDropped` reasons; manual VS-A7 (T046) confirms |
| SC-006 (TestModule reaches working Runtime Preview after the documented one-time migration) | Manual VS-A1–A4 (T025–T027) completed; legacy `saved/` cleanup verified (T033–T034) |
| SC-007 (fresh-developer walkthrough without consulting docs beyond `quickstart.md`) | Manual VS-B1–B3 (T028) completed against a green-field `Quickstart084` module created via feature 085's Generate Module path; documented end-to-end in `quickstart.md` |

**Note**: SC-008 was retired during `/speckit.clarify` (Session 2026-04-28 Q2);
no coverage required.

**Result**: every SC-001..SC-007 has empirical evidence of being met.

## 3. Edge Case coverage (T054)

| Edge Case (spec.md §"Edge Cases") | Coverage path |
|---|---|
| Composable signature has no input parameters beyond `viewModel` | T015 (e) — degenerate-spec source-output case (Source CodeNode skipped or zero-output runtime) |
| Composable observes no `StateFlow` properties on the ViewModel | T015 (f) — degenerate-spec sink-input case (Sink CodeNode skipped; ControllerInterface degenerates to inherited-only `ModuleController` surface) |
| Source UI file is renamed or moved | Implicit in T015's `flowGraphPrefix` derivation (artifact prefix follows the flow-graph filename, not the UI filename); no test currently asserts collision-freeness across renames — flagged below |
| Module contains multiple qualifying UI files or multiple flow graphs | Manual VS-B5 (T047) confirmed two `{flow graph, UI file}` pairs in one module produce non-colliding outputs |
| Module already contains hand-written controller, flow-runtime, or preview-provider files for the same UI | T035 (`target file lacking the Generated marker is SKIPPED_CONFLICT`) + manual VS-A8 (T039) |
| UI-FBP run inside a module scaffolded under a different build configuration | T019 (unscaffolded refusal) + manual VS-A9 (T040); UI-FBP refuses rather than mutating `build.gradle.kts` |
| Runtime Preview opened before any business-logic graph has been added | US1 itself — manual VS-A4 (T027) loads with an empty middle graph; runtime reports running state without value flow |

**Follow-ups identified**:

- The "Source UI file is renamed or moved" edge case has no concrete test
  asserting that stale prior-name artifacts don't collide. Practical mitigation
  today: hand-written-file detection (FR-016 / T035) blocks silent overwrite,
  and `deleteLegacyLocations` (T037) opt-in reclaims known duplicate paths.
  A targeted regression test could be added in a future feature if this edge
  case becomes contentious.

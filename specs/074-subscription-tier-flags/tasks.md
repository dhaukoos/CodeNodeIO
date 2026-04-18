# Tasks: Subscription Tier Feature Flags

**Input**: Design documents from `/specs/074-subscription-tier-flags/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, quickstart.md

**Tests**: Unit tests are included for the foundational tier model and save separation (core infrastructure changes).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: Create the SubscriptionTier model, FeatureGate interface, and LocalFeatureGate implementation — the infrastructure all user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Tests

- [X] T001 [P] Add test: SubscriptionTier.hasAccess() returns correct results for all tier combinations (FREE < PRO < SIM) in `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/SubscriptionTierTest.kt`
- [X] T002 [P] Add test: LocalFeatureGate reads tier from properties file, defaults to FREE when file missing, and updates StateFlow on setTier() in `fbpDsl/src/jvmTest/kotlin/io/codenode/fbpdsl/subscription/LocalFeatureGateTest.kt`

### Implementation

- [X] T003 Create `SubscriptionTier` enum (FREE, PRO, SIM) with `hasAccess(required)` method and `FeatureGate` interface with `currentTier: StateFlow<SubscriptionTier>`, `canGenerate()`, `canSimulate()`, `canUseRepositoryNodes()` in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/SubscriptionTier.kt`
- [X] T004 Create `LocalFeatureGate` implementation that reads `~/.codenode/config.properties` key `subscription.tier`, defaults to FREE, exposes `setTier()` for runtime updates in `fbpDsl/src/jvmMain/kotlin/io/codenode/fbpdsl/subscription/LocalFeatureGate.kt`
- [X] T005 Run tests: `./gradlew :fbpDsl:jvmTest` to verify tier model and LocalFeatureGate

**Checkpoint**: SubscriptionTier + FeatureGate infrastructure is ready. All user stories can now proceed.

---

## Phase 2: User Story 1 — Separate Save from Code Generation (Priority: P1) 🎯 MVP

**Goal**: Save writes only the `.flow.kt` file. A new "Generate Module" action produces the full module scaffolding. Both actions work independently.

**Independent Test**: Click Save → only `.flow.kt` written. Click "Generate Module" → full module scaffolding produced. Verify by inspecting the output directory after each action.

### Implementation

- [X] T006 [US1] Extract `saveFlowKtOnly(flowGraph, outputDir, portTypeNames)` method from `ModuleSaveService.saveModule()` that writes only the `.flow.kt` file using `FlowGraphSerializer.serialize()` in `flowGraph-generate/src/jvmMain/kotlin/io/codenode/flowgraphgenerate/save/ModuleSaveService.kt`
- [X] T007 [US1] Modify Save action in `GraphEditorDialogs.kt` to call `saveFlowKtOnly()` instead of `saveModule()` — Save now writes only the `.flow.kt` file in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphEditorDialogs.kt`
- [X] T008 [US1] Add "Generate Module" button to the toolbar next to Save in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/TopToolbar.kt`
- [X] T009 [US1] Wire "Generate Module" action in `GraphEditorApp.kt` — on click, call `ModuleSaveService.saveModule()` using `lastSaveDir` as default output (prompt if not set) in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphEditorApp.kt`
- [X] T010 [US1] Compile and run tests: `./gradlew :flowGraph-generate:compileKotlinJvm :graphEditor:compileKotlinJvm :graphEditor:jvmTest`

**Checkpoint**: Save and Generate are independent actions. Save writes only `.flow.kt`. Generate produces full module. User Story 1 is independently testable.

---

## Phase 3: User Story 2 — Feature Flag Infrastructure (Priority: P2)

**Goal**: Wire FeatureGate into the graph editor. Gate "Generate Module" behind Pro tier. Free-tier users see a clear upgrade message.

**Independent Test**: Set tier to FREE → "Generate Module" is disabled with upgrade message. Set to PRO → "Generate Module" works. Set to SIM → everything works.

### Implementation

- [X] T011 [US2] Instantiate `LocalFeatureGate` in `GraphEditorApp.kt` and pass it as a parameter to child composables (TopToolbar, RuntimePreviewPanel, NodePalette) in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphEditorApp.kt`
- [X] T012 [US2] Gate "Generate Module" button in `TopToolbar.kt` — disabled with tooltip "Upgrade to Pro to generate modules" when `featureGate.canGenerate()` is false in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/TopToolbar.kt`
- [X] T013 [US2] Show upgrade prompt in `GraphEditorDialogs.kt` when a Free-tier user attempts to trigger code generation (guard the Generate action callback) in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphEditorDialogs.kt`
- [X] T014 [US2] Compile and run tests: `./gradlew :graphEditor:compileKotlinJvm :graphEditor:jvmTest`

**Checkpoint**: Feature flag infrastructure is wired. Code generation is gated behind Pro tier. Free-tier users see clear upgrade messages.

---

## Phase 4: User Story 3 — Gate Runtime Preview Behind Sim Tier (Priority: P3)

**Goal**: Runtime Preview panel shows an upgrade prompt for Free and Pro tier users. Sim tier gets full simulation controls.

**Independent Test**: Set tier to PRO → Runtime Preview panel shows "Requires Sim tier" upgrade prompt. Set to SIM → full preview controls available.

### Implementation

- [X] T015 [US3] Accept `featureGate` parameter in `RuntimePreviewPanel` composable in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/RuntimePreviewPanel.kt`
- [X] T016 [US3] When `featureGate.canSimulate()` is false, replace panel content with an upgrade prompt (e.g., "Circuit simulation requires the Sim tier. Upgrade to unlock Runtime Preview.") while keeping the `CollapsiblePanel` wrapper functional in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/RuntimePreviewPanel.kt`
- [X] T017 [US3] Compile and run tests: `./gradlew :graphEditor:compileKotlinJvm :graphEditor:jvmTest`

**Checkpoint**: Runtime Preview is gated behind Sim tier. Free and Pro users see upgrade prompt. Sim users have full simulation.

---

## Phase 5: User Story 4 — Gate Entity/Repository Nodes Behind Pro Tier (Priority: P4)

**Goal**: Entity/repository nodes (NodeTypeDefinition category = "Repository") are hidden from the node palette for Free-tier users. All other nodes and all IP types remain available at all tiers.

**Independent Test**: Set tier to FREE → node palette shows all standard and custom nodes but not entity/repository nodes. Set to PRO → entity/repository nodes appear. IP type palette is unaffected at any tier.

### Implementation

- [X] T018 [US4] Accept `featureGate` parameter in the node palette composable — filtering applied at `GraphEditorLayout.kt` call site before passing `nodeTypes` to `NodePalette`
- [X] T019 [US4] Filter out nodes with category `DATABASE` when `featureGate.canUseRepositoryNodes()` is false in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphEditorLayout.kt`
- [ ] T020 [US4] When a graph containing entity/repository nodes is opened on Free tier, mark those nodes visually as restricted (e.g., dimmed with lock icon or border) and prevent editing/connecting in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphEditorApp.kt`
- [X] T021 [US4] Compile and run tests: `./gradlew :graphEditor:compileKotlinJvm :graphEditor:jvmTest`

**Checkpoint**: Entity/repository nodes are gated behind Pro. All other nodes and IP types available at all tiers.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Full verification across all quickstart scenarios and cross-tier edge cases

- [ ] T022 Run full test suite: `./gradlew :fbpDsl:jvmTest :flowGraph-generate:jvmTest :graphEditor:jvmTest`
- [ ] T023 Run quickstart.md verification scenarios VS1–VS11
- [ ] T024 Verify cross-tier graph opening: save a graph with entity/repository nodes on Pro tier, open on Free tier, confirm nodes are visible but restricted
- [ ] T025 Verify tier change without restart: change `subscription.tier` in `~/.codenode/config.properties` and confirm the UI updates reactively

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: No dependencies — can start immediately. BLOCKS all user stories.
- **User Story 1 (Phase 2)**: Depends on Foundational — separates Save from Generate
- **User Story 2 (Phase 3)**: Depends on Foundational + User Story 1 — gates Generate behind Pro tier (needs Generate button from US1)
- **User Story 3 (Phase 4)**: Depends on Foundational — gates Runtime Preview (independent of US1/US2)
- **User Story 4 (Phase 5)**: Depends on Foundational — gates node palette (independent of US1/US2/US3)
- **Polish (Phase 6)**: Depends on all user stories being complete

### Within Each Phase

- Tests (T001–T002) before implementation (T003–T004), then verification (T005)
- US1: ModuleSaveService extraction (T006) before UI changes (T007–T009)
- US2: Wiring (T011) before gating (T012–T013)
- US3 and US4: Can proceed in parallel after Foundational phase

### Parallel Opportunities

```text
# Foundational phase: tests and implementation can overlap
T001, T002  (tests in parallel)
T003, T004  (impl in parallel — different files)

# After Foundational complete:
# US3 and US4 can run in parallel (independent features):
T015-T017 (Runtime Preview gating)
T018-T021 (Node palette gating)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Foundational (T001–T005)
2. Complete Phase 2: User Story 1 (T006–T010)
3. **STOP and VALIDATE**: Save writes only `.flow.kt`, Generate produces full module
4. This is independently valuable even without tier gating

### Incremental Delivery

1. Foundational → SubscriptionTier + FeatureGate ready
2. User Story 1 → Save/Generate separated (MVP!)
3. User Story 2 → Code generation gated behind Pro
4. User Story 3 → Runtime Preview gated behind Sim
5. User Story 4 → Entity nodes gated behind Pro
6. Polish → Full verification pass

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- US3 (Runtime Preview) and US4 (Node Palette) are independent and can be implemented in any order or in parallel
- US2 depends on US1 (needs the "Generate Module" button to gate)
- IP types are NOT gated — available at all tiers per clarification session 2026-04-17
- `serializedOutput` port remains String — not affected by tier gating
- Commit after each phase completion

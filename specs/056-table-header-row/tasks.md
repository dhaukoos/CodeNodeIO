# Tasks: Table Header Row for Entity List Views

**Input**: Design documents from `/specs/056-table-header-row/`
**Prerequisites**: plan.md, spec.md, research.md

**Tests**: Not explicitly requested — no test tasks included.

**Organization**: Tasks grouped by user story. US1 refactors existing modules (visible impact first), US2 updates the code generator, US3 is covered by US1's per-module refactoring.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: No setup needed — all target files already exist. This feature modifies existing code only.

*(No tasks)*

---

## Phase 2: Foundational (Code Generator Updates)

**Purpose**: Update the code generator so it produces the HeaderRow pattern for all future modules. This must be done first so that US3 (existing module refactoring) follows the same pattern the generator will produce.

- [X] T001 Update `generateListView()` to emit a `{Entity}HeaderRow` composable definition and invoke it above the list/empty-state Box in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/EntityUIGenerator.kt`
- [X] T002 Update `generateRowView()` to remove `"${prop.name}: "` label prefixes from all Text values, display values only, use `"—"` for nullable nulls, and display `"Yes"`/`"No"` for booleans without label in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/EntityUIGenerator.kt`

**Checkpoint**: Code generator now produces HeaderRow + value-only rows for any new entity module.

---

## Phase 3: User Story 1 + User Story 3 — Refactor Existing Modules (Priority: P1/P2)

**Goal**: Add HeaderRow composable to all three existing entity modules and remove inline labels from data rows. US1 (header row visible in list views) and US3 (refactor existing modules) are delivered together since they affect the same files.

**Independent Test**: Launch graphEditor, open runtime preview for each module. Verify header row with column names appears at top. Verify data rows show values only (no "Label: value" format). Verify header stays visible while scrolling.

### Addresses Module

- [X] T003 [P] [US1] Add `AddressHeaderRow` composable and invoke it above the list Box in `Addresses/src/commonMain/kotlin/io/codenode/addresses/userInterface/Addresses.kt`
- [X] T004 [P] [US1] Remove `"Street: "`, `"City: "`, `"State: "`, `"Zip: "` label prefixes from Text values in `Addresses/src/commonMain/kotlin/io/codenode/addresses/userInterface/AddressRow.kt`

### GeoLocations Module

- [X] T005 [P] [US1] Add `GeoLocationHeaderRow` composable and invoke it above the list Box in `GeoLocations/src/commonMain/kotlin/io/codenode/geolocations/userInterface/GeoLocations.kt`
- [X] T006 [P] [US1] Remove `"name: "`, `"lat: "`, `"lon: "` label prefixes from Text values in `GeoLocations/src/commonMain/kotlin/io/codenode/geolocations/userInterface/GeoLocationRow.kt`

### UserProfiles Module

- [X] T007 [P] [US1] Add `UserProfileHeaderRow` composable and invoke it above the list Box in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/userInterface/UserProfiles.kt`
- [X] T008 [P] [US1] Remove `"Age: "` label prefix from UserProfileRow, keep name as-is (already no label), change `"Active"/"Inactive"` display to `"Yes"/"No"` for consistency in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/userInterface/UserProfiles.kt`

**Checkpoint**: All three existing modules display header row + value-only data rows in runtime preview.

---

## Phase 4: User Story 2 — Verify Code Generator Output (Priority: P2)

**Goal**: Validate the code generator (updated in Phase 2) produces correct output for new modules.

**Independent Test**: Generate a new Repository Module from an IP Type with 3+ properties. Verify generated `{entity}s.kt` contains HeaderRow composable. Verify generated `{entity}Row.kt` has value-only display.

- [ ] T009 [US2] Manually verify code generator output by reviewing `EntityUIGenerator.generateListView()` and `EntityUIGenerator.generateRowView()` output for a sample 3-property entity spec — confirm HeaderRow composable present and row values have no inline labels

**Checkpoint**: Code generator confirmed to produce correct HeaderRow pattern for new modules.

---

## Phase 5: Polish & Cross-Cutting Concerns

- [ ] T010 Build the project to verify all changes compile in `graphEditor` module via `./gradlew :graphEditor:build`
- [ ] T011 Run runtime preview for each existing module (Addresses, GeoLocations, UserProfiles) to verify visual alignment between header and data row columns

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 2)**: No dependencies — can start immediately
- **US1+US3 (Phase 3)**: Can start after Phase 2 (to follow the generator's pattern), but technically independent
- **US2 (Phase 4)**: Depends on Phase 2 completion
- **Polish (Phase 5)**: Depends on all prior phases

### Within Phase 3

All six module tasks (T003-T008) are marked [P] — they modify different files and can run in parallel. Within each module, the HeaderRow addition and Row label removal are in separate files and can also run in parallel.

### Parallel Opportunities

```text
# All module refactors can run in parallel:
T003 + T004 (Addresses)
T005 + T006 (GeoLocations)
T007 + T008 (UserProfiles)
```

---

## Implementation Strategy

### MVP First (Phase 2 + Phase 3)

1. Update code generator (T001, T002)
2. Refactor all three existing modules (T003-T008) in parallel
3. **STOP and VALIDATE**: Runtime preview all three modules
4. Complete polish (T010-T011)

### Incremental Delivery

1. Phase 2: Generator updates → future modules are covered
2. Phase 3: Refactor existing modules → all current modules follow new pattern
3. Phase 4: Verify generator output
4. Phase 5: Full build + visual verification

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- UserProfiles module has UserProfileRow in the same file as UserProfiles — both T007 and T008 modify the same file and must run sequentially
- The HeaderRow composable uses identical layout modifiers (weight/padding) as data rows to guarantee column alignment
- Commit after each phase completion

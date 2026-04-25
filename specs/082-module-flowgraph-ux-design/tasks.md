# Tasks: Module & FlowGraph UX Design

**Input**: Design documents from `/specs/082-module-flowgraph-ux-design/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, quickstart.md

**Tests**: None — this is a documentation-only feature. Verification is review-based.

**Organization**: Tasks are grouped by user story to enable independent review.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

## Phase 1: User Story 1 — Module Properties Dialog Redesign (Priority: P1) 🎯 MVP

**Goal**: Document the redesigned Module Properties dialog with all fields, validation rules, button states, and mockup description.

**Independent Test**: Read the dialog spec — all fields, validation, and states are unambiguous.

### Implementation

- [X] T001 [US1] Write Module Properties dialog specification with field definitions, validation rules, button states, and create/edit modes in `specs/082-module-flowgraph-ux-design/ux-design.md`
- [X] T002 [US1] Write ASCII mockups for both create-new and edit-existing modes of Module Properties dialog in `specs/082-module-flowgraph-ux-design/ux-design.md`

**Checkpoint**: Module Properties dialog fully specified and reviewable.

---

## Phase 2: User Story 2 — New/Open/Save UI Variations (Priority: P2)

**Goal**: Document 3 UI variations for New/Open/Save with complete user flows, pros/cons, and a recommendation.

**Independent Test**: Each variation has New/Open/Save flows described, pros/cons are specific, recommendation is justified.

### Implementation

- [X] T003 [US2] Write Variation A (Context Bar) with user flows, ASCII mockup, and pros/cons table in `ux-design.md`
- [X] T004 [US2] Write Variation B (Toolbar Dropdown) with user flows, ASCII mockup, and pros/cons table in `ux-design.md`
- [X] T005 [US2] Write Variation C (Workspace — Recommended) with user flows, ASCII mockup, and pros/cons table in `ux-design.md`
- [X] T006 [US2] Write 7-criterion comparison table and recommendation rationale in `ux-design.md`

**Checkpoint**: All 3 variations documented with clear recommendation.

---

## Phase 3: User Story 3 — Module/FlowGraph Relationship Model (Priority: P3)

**Goal**: Document the conceptual relationship model with definitions, operations, and edge cases.

**Independent Test**: A developer can answer relationship questions (many-to-one, operations per level, edge cases) by reading the document.

### Implementation

- [X] T007 [US3] Write entity definitions for Module and FlowGraph in `ux-design.md` Section 3.1
- [X] T008 [US3] Write relationship model with diagram, cardinality, and constraints in `ux-design.md` Section 3.2
- [X] T009 [US3] Write operations table (8 operations across module and flowGraph levels) in `ux-design.md` Section 3.3
- [X] T010 [US3] Write edge case behavior table (6 scenarios with defined behaviors) in `ux-design.md` Section 3.4

**Checkpoint**: Relationship model fully documented and edge cases addressed.

---

## Phase 4: Polish & Consolidation

**Purpose**: Consolidate all sections into a cohesive design document and add migration notes.

- [X] T011 Write migration notes: current→new behavior table, 9-item implementation scope, backward compatibility in `ux-design.md` Section 4
- [X] T012 Review `ux-design.md` for completeness — all FR-001 through FR-010 addressed across 4 sections
- [X] T013 Run quickstart.md verification scenarios VS1–VS5 — all pass

---

## Dependencies & Execution Order

### Phase Dependencies

- **User Story 1 (Phase 1)**: No dependencies — dialog design standalone.
- **User Story 2 (Phase 2)**: No dependencies — variations standalone. Can run in parallel with US1.
- **User Story 3 (Phase 3)**: No dependencies — model standalone. Can run in parallel with US1/US2.
- **Polish (Phase 4)**: Depends on all user stories — consolidation requires all sections.

### Parallel Opportunities

```text
# All 3 user stories are independent documentation tasks:
US1 (T001-T002), US2 (T003-T006), US3 (T007-T010) — can all run in parallel

# Within US2, variations are independent:
T003 (Variation A), T004 (Variation B), T005 (Variation C)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: User Story 1 (T001–T002) — Module Properties dialog
2. **STOP and VALIDATE**: Dialog spec is reviewable and unambiguous

### Incremental Delivery

1. User Story 1 → Module Properties dialog design (MVP!)
2. User Story 2 → 3 UI variations with recommendation
3. User Story 3 → Relationship model documentation
4. Polish → Consolidated ux-design.md with migration notes

---

## Notes

- All tasks write to the same output file: `ux-design.md` — each task adds a section
- No code implementation — all deliverables are documentation
- The recommended variation (C — Workspace) can be changed based on user review
- Commit after each phase

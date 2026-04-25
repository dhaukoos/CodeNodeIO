# Feature Specification: Module & FlowGraph UX Design

**Feature Branch**: `082-module-flowgraph-ux-design`
**Created**: 2026-04-25
**Status**: Draft
**Input**: User description: "Refine how New, Open, and Save buttons work — establish many-to-one relationship between flowGraphs and modules, redesign Module Properties dialog, propose UI variations for New/Open/Save."

**Scope**: This feature delivers design documentation and UI mockup descriptions only. No code implementation — that is deferred to a subsequent feature.

## Context: Current State

Today there is an implied one-to-one relationship between a flowGraph and the module that contains it:
- "New" creates a blank flowGraph named "New Graph" with no module context
- "Open" browses for a `.flow.kt` file and infers the module from its directory
- "Save" writes the `.flow.kt` to the module's `flow/` directory (or a flat file if no module exists)
- "FlowGraph Properties" dialog sets the graph name and target platforms — but these are actually module-level concerns

The reality should be **many-to-one**: a module can contain multiple flowGraphs in its `flow/` directory. A module must exist before a flowGraph can be saved within it.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Module Properties Dialog Redesign (Priority: P1)

The current "FlowGraph Properties" dialog is renamed to "Module Properties". It allows the user to view and edit module-level settings: module name (minimum 3 characters) and target platforms (at least 1 required). A "Create Module" button triggers module scaffolding when the criteria are met — it presents a directory chooser, then creates the module directory structure via ModuleScaffoldingGenerator. The name field starts empty (not "New Graph"). This dialog is the canonical place to create new modules and view/edit module settings.

**Why this priority**: The module must exist before flowGraphs can be saved. This is the foundational UX change.

**Independent Test**: Open Module Properties. Enter "StopWatch" as name, select Android + iOS platforms. Click "Create Module". Select a directory. Verify the module scaffolding is created.

**Acceptance Scenarios**:

1. **Given** the user opens the properties dialog, **When** it appears, **Then** it is titled "Module Properties" (not "FlowGraph Properties")
2. **Given** the Module Properties dialog, **When** the name field is empty or fewer than 3 characters, **Then** the "Create Module" button is disabled
3. **Given** a valid name and at least 1 target platform selected, **When** the user clicks "Create Module", **Then** a directory chooser appears and the module scaffolding is created at the selected location
4. **Given** a module is already loaded, **When** the user opens Module Properties, **Then** the name and platforms are pre-filled from the current module and "Create Module" is replaced by the module path display

---

### User Story 2 - Propose New/Open/Save UI Variations (Priority: P2)

The designer proposes 2-3 variations for how the New, Open, and Save toolbar buttons should work in a many-flowGraph-per-module world. Each variation addresses: how the user names a new flowGraph, how they choose which module it belongs to, how they open a specific flowGraph from a module, and how Save knows where to write. Each variation includes pros, cons, and mockup descriptions.

**Why this priority**: The UI design for New/Open/Save must be decided before implementation. Multiple reasonable approaches exist and the tradeoffs need to be evaluated.

**Independent Test**: The design document contains 2-3 clearly described variations with pros/cons tables, mockup descriptions, and a recommended approach.

**Acceptance Scenarios**:

1. **Given** the design document, **When** reading any variation, **Then** it describes the complete user flow for New, Open, and Save including how the flowGraph name and module context are established
2. **Given** each variation, **When** evaluating it, **Then** the pros and cons are specific and actionable (not vague)
3. **Given** the variations, **When** comparing them, **Then** a recommended approach is identified with clear reasoning

---

### User Story 3 - Document Module/FlowGraph Relationship Model (Priority: P3)

A conceptual document describes the many-to-one relationship between flowGraphs and modules. It defines: what a "module" is (directory + scaffolding + gradle + flow/ + controller/ + etc.), what a "flowGraph" is (a .flow.kt file within a module's flow/ directory), how they relate (module contains 1+ flowGraphs), and what operations apply to each level (module: create, configure platforms; flowGraph: new, open, save, generate).

**Why this priority**: The relationship model is the foundation for all UX decisions. Without a clear model, the UI variations in US2 lack a shared conceptual basis.

**Independent Test**: A developer reading the document can answer: "Can a module have multiple flowGraphs?", "Can a flowGraph exist without a module?", "What happens when I save a flowGraph — where does it go?"

**Acceptance Scenarios**:

1. **Given** the relationship document, **When** a developer reads it, **Then** they understand the many-to-one model and can predict the behavior of New, Open, Save, and Generate for any scenario
2. **Given** the document, **When** it describes edge cases (empty module, orphan flowGraph, module with no flowGraphs), **Then** the expected behavior is defined for each

---

### Edge Cases

- What happens when a user tries to save a flowGraph with no module loaded? The system prompts them to create a module first via Module Properties, or select an existing module directory.
- What happens when a module contains zero flowGraphs? The module is valid — it simply has no flow definitions yet. The graph editor shows a blank canvas.
- What happens when a user opens a `.flow.kt` from outside any module? The system loads the flowGraph but without module context — limited functionality (no code generation, no Runtime Preview).
- What happens when two flowGraphs in the same module have the same name? This should be prevented — flowGraph names must be unique within a module.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST rename "FlowGraph Properties" to "Module Properties" in all UI elements
- **FR-002**: The Module Properties dialog MUST have a name field that starts empty (not "New Graph") with a minimum of 3 characters required
- **FR-003**: The Module Properties dialog MUST require at least 1 target platform selected before module creation is allowed
- **FR-004**: The Module Properties dialog MUST include a "Create Module" button that triggers module scaffolding via directory chooser when name and platform criteria are met
- **FR-005**: The design document MUST propose 2-3 UI variations for how New, Open, and Save should work with many-to-one flowGraph/module relationship
- **FR-006**: Each UI variation MUST describe the complete user flow for New (name + module selection), Open (file browsing + module inference), and Save (target location determination)
- **FR-007**: Each variation MUST include specific pros and cons and a mockup description
- **FR-008**: The design MUST include a recommended variation with clear reasoning
- **FR-009**: The conceptual document MUST define the module/flowGraph relationship model including: what constitutes each entity, how they relate, what operations apply to each
- **FR-010**: The design documents MUST address edge cases: no module loaded, empty module, orphan flowGraph, duplicate names

### Key Entities

- **Module**: A KMP directory structure containing scaffolding (gradle files), source directories (flow/, controller/, viewmodel/, etc.), and one or more flowGraph definitions. Created via Module Properties dialog.
- **FlowGraph**: A `.flow.kt` file within a module's `flow/` directory. Defines nodes, connections, and port types. Multiple flowGraphs can exist within a single module. Created via "New", opened via "Open", persisted via "Save".

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The Module Properties dialog design is documented with all fields, validation rules, and button states clearly specified
- **SC-002**: 2-3 UI variations for New/Open/Save are documented with pros/cons and a recommendation
- **SC-003**: The module/flowGraph relationship model is documented with definitions, relationships, and edge case behaviors
- **SC-004**: A developer unfamiliar with the project can read the design documents and understand the proposed UX without ambiguity
- **SC-005**: The recommended UI variation addresses all identified edge cases

## Assumptions

- This feature delivers documentation only — no code changes, no UI implementation
- The subsequent feature will implement the chosen UI variation and migrate existing functionality
- Module scaffolding uses the existing ModuleScaffoldingGenerator (feature 078)
- The existing FlowGraph DSL format (.flow.kt) remains unchanged
- The folder hierarchy from feature 077 (flow/, controller/, viewmodel/, etc.) is assumed
- The "Save" button continues to save only the flowGraph (.flow.kt) — code generation remains in the Code Generator panel

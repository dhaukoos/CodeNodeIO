# Feature Specification: Unified Configurable Code Generation

**Feature Branch**: `076-codegen-decomposition`
**Created**: 2026-04-21
**Status**: Draft
**Input**: User description: "Unified, configurable Code Generation Migration — dependency analysis of code generation functionality, logical file grouping, Code Generator UI panel prototype, and migration plan."

## Context: Current "Generate Module" Extent

The "Generate Module" toolbar button currently produces a complete KMP module scaffolding from a flow graph:

**Module structure files**: `build.gradle.kts`, `settings.gradle.kts`
**Flow definition**: `{Name}.flow.kt`
**Runtime controller files** (in `generated/`): `{Name}Flow.kt`, `{Name}Controller.kt`, `{Name}ControllerInterface.kt`, `{Name}ControllerAdapter.kt`
**ViewModel**: `{Name}ViewModel.kt` (selective regeneration of Module Properties section)
**UI stub**: `userInterface/{Name}.kt` (write-once)
**Persistence** (if repository nodes present): Entity, DAO, Repository, AppDatabase, DatabaseModule

"Generate Repository Module" additionally creates: converter files, 3 CodeNode definitions (CUD, Repository, Display), 3 UI views (list, add/update, row), persistence wiring, and Koin DI integration.

"Generate UI-FBP" creates: ViewModel, State, Source CodeNode, Sink CodeNode, and a bootstrap `.flow.kt`.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Code Generation Dependency Analysis (Priority: P1)

A developer examines the code generation system and produces a complete dependency map of all generators, the files they produce, their inputs, and their inter-dependencies. This analysis identifies which generators can operate independently and which require outputs from other generators as inputs. Notably, a "module scaffolding" component must be extracted that creates the KMP module structure (gradle files, source directories) given a Module name defined in the top bar settings — this component is a prerequisite for all other file generation components. The deliverable is a dependency analysis document and a proposed logical file grouping that refines the current flat module structure into organized subdirectories (e.g., controller files grouped under `/controller`, flow files under `/flow`).

**Why this priority**: The dependency analysis is the foundation for all subsequent decomposition. Without understanding which generators depend on which, the system cannot be broken into configurable, independently-selectable units.

**Independent Test**: The dependency analysis document identifies every generator class, its inputs, its outputs (generated files), and directed dependency edges. The proposed folder hierarchy is validated by mapping every file from an existing generated module (e.g., Addresses) into the new structure.

**Acceptance Scenarios**:

1. **Given** the current code generation codebase, **When** the analysis is complete, **Then** every generator class is catalogued with its inputs, outputs (files produced), and dependencies on other generators
2. **Given** the analysis document, **When** a developer reads it, **Then** they can trace the dependency chain from any single generated file back to its required inputs and prerequisite generators
3. **Given** the existing flat module structure (e.g., Addresses module), **When** the proposed folder hierarchy is applied, **Then** every file maps to a logical subdirectory (e.g., controller/, flow/, nodes/, userInterface/, persistence/)
4. **Given** the dependency map, **When** identifying independent generation units, **Then** each unit can be described as a potential CodeNode or GraphNode in a future code-generation flow graph
5. **Given** the dependency analysis, **When** reviewing the module scaffolding component, **Then** it is identified as a prerequisite for all other generation components, accepting a Module name (from the top bar settings) as its primary input

---

### User Story 2 - Code Generator UI Panel Prototype (Priority: P2)

A developer opens the graph editor and sees a new "Code Generator" panel on the left side, following the visual pattern of the Node Generator and IP Generator panels. The panel is feature-gated under the Pro tier. In the top section, the user selects a generation path via dropdown: "Generate Module", "Repository", or "UI-FBP". For the "Generate Module" path, the primary input is the existing flow graph as configured in the graph editor UI together with the Module name defined in the top bar settings — this generates an end-to-end vertical slice of MVVM with a stub for the user interface file. For the "Repository" path, a second dropdown lists available custom IP Types as the primary input. For the "UI-FBP" path, a file chooser selects the Compose UI source file. Below the input selection, a file-tree GUI displays checkboxes for each file that could be generated, organized under the proposed folder hierarchy. Folder-level checkboxes select/deselect all children. The "Generate Repository Module" / "Remove Repository Module" buttons are relocated from the IP Type Properties panel to this Code Generator panel.

**Why this priority**: The UI prototype validates the design concept and provides a tangible interface for future code generation wiring. It also consolidates code generation actions that are currently scattered across different panels into a single, discoverable location.

**Independent Test**: Open the graph editor on Pro tier. The Code Generator panel appears on the left. Select "Generate Module" path — the file tree populates with the expected files for the current flow graph. Select "Repository" path, pick an IP Type — the file tree populates with repository module files grouped by folder. Select "UI-FBP" path, pick a UI file — the file tree shows ViewModel, State, Source, Sink grouped by folder. Folder checkboxes toggle all children. The panel is not visible on Free tier.

**Acceptance Scenarios**:

1. **Given** a user is on Pro tier, **When** they view the left panel area, **Then** a "Code Generator" collapsible panel is visible alongside the Node Generator and IP Generator panels
2. **Given** a user is on Free tier, **When** they view the left panel area, **Then** the Code Generator panel is not visible
3. **Given** the user selects "Generate Module" path, **When** the file tree populates, **Then** it shows all files that would be generated for the current flow graph (module scaffolding, flow definition, runtime controllers, ViewModel, UI stub), organized under subdirectories
4. **Given** the user selects "Repository" path and picks a custom IP Type, **When** the file tree populates, **Then** it shows all files that would be generated for a repository module, organized under subdirectories (controller/, flow/, nodes/, userInterface/, persistence/)
5. **Given** the user selects "UI-FBP" path and picks a Compose UI file, **When** the file tree populates, **Then** it shows the ViewModel, State, Source CodeNode, and Sink CodeNode files organized under their respective subdirectories
5. **Given** the file tree is displayed, **When** the user clicks a folder checkbox, **Then** all child file checkboxes toggle to match the folder's state
6. **Given** the file tree is displayed, **When** the user deselects individual file checkboxes, **Then** the parent folder checkbox updates to reflect partial selection (indeterminate state)
7. **Given** the "Generate Repository Module" button previously in the IP Type Properties panel, **When** this feature is complete, **Then** the button has been relocated to the Code Generator panel and removed from the IP Type Properties panel

---

### User Story 3 - Migration Plan Document (Priority: P3)

A migration plan document describes the sequence of future feature steps needed to evolve from the current monolithic code generation design to the new configurable, flow-graph-based code generation system. Each step is scoped as an independent feature that can be specified, planned, and implemented on its own. The plan identifies which components become CodeNodes, which become GraphNodes, and how the file-tree selection UI will wire into actual generation logic.

**Why this priority**: The migration plan provides the roadmap for the multi-feature evolution. Without it, subsequent features risk making design decisions in isolation that conflict with the overall architecture direction.

**Independent Test**: The migration plan lists a numbered sequence of future features, each with a one-paragraph scope description, its prerequisites (which prior features must be complete), and the specific generation components it addresses. A developer reading the plan can understand the full evolution path.

**Acceptance Scenarios**:

1. **Given** the dependency analysis from US1, **When** the migration plan is written, **Then** each future feature step addresses a coherent subset of the dependency graph
2. **Given** the migration plan, **When** a developer reads any single step, **Then** they understand what that step delivers, what it depends on, and what it enables for subsequent steps
3. **Given** the migration plan, **When** all steps are complete, **Then** the code generation system has evolved from monolithic generator methods into a configurable flow graph of CodeNodes where individual file generation can be selected or deselected

---

### Edge Cases

- What happens when the user switches between "Repository" and "UI-FBP" paths in the Code Generator panel? The file tree clears and repopulates with the file structure appropriate for the newly selected path.
- What happens when no IP Types exist for the Repository path? The dropdown is empty with a message indicating no custom IP Types are available.
- What happens when the user has no saved UI files for the UI-FBP path? The file chooser opens normally; if no file is selected, the file tree remains empty.
- What happens when the Code Generator panel is visible but no generation path is selected? The file tree section shows a placeholder message: "Select a generation path to see available files."

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST produce a dependency analysis document cataloguing every code generator, its inputs, outputs (generated files), and dependencies on other generators
- **FR-002**: The dependency analysis MUST cover both the "Generate Module" / "Generate Repository Module" path and the "Generate UI-FBP" path
- **FR-003**: The system MUST propose a refined folder hierarchy that groups related generated files into logical subdirectories (e.g., controller files in `/controller`, flow files in `/flow`)
- **FR-004**: The system MUST provide a "Code Generator" side panel in the graph editor, following the visual pattern of the Node Generator and IP Generator panels
- **FR-005**: The Code Generator panel MUST be feature-gated under the Pro tier — not visible to Free tier users
- **FR-006**: The Code Generator panel MUST offer a path selector with three options: "Generate Module", "Repository", and "UI-FBP"
- **FR-006a**: For the "Generate Module" path, the primary input is the current flow graph and Module name from the top bar settings, generating an end-to-end MVVM vertical slice with a UI stub
- **FR-007**: For the Repository path, the panel MUST present a dropdown of available custom IP Types as the primary input
- **FR-008**: For the UI-FBP path, the panel MUST provide a mechanism to select a Compose UI source file as the primary input
- **FR-009**: The panel MUST display a file-tree GUI showing checkboxes for each file that could be generated, organized under the proposed folder hierarchy
- **FR-010**: Folder-level checkboxes MUST select or deselect all child file checkboxes
- **FR-011**: Individual file checkbox changes MUST update the parent folder checkbox to reflect selection state (all, none, or partial)
- **FR-012**: The "Generate Repository Module" and "Remove Repository Module" actions MUST be relocated from the IP Type Properties panel to the Code Generator panel
- **FR-013**: The UI interaction (input selection, file tree population, checkbox toggling) MUST be fully functional, even though code generation is not yet wired to the new panel in this feature
- **FR-014**: The system MUST produce a migration plan document describing the sequence of future features needed to complete the evolution to configurable, flow-graph-based code generation

### Key Entities

- **GenerationPath**: The selected code generation approach (Generate Module, Repository, or UI-FBP), which determines the set of files available for generation.
- **GenerationFileTree**: A hierarchical model of folders and files representing the complete set of outputs for a given generation path and primary input, with selection state per node.
- **DependencyMap**: A directed graph of generator components, their inputs, outputs, and inter-dependencies, used to determine which generation units can operate independently.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The dependency analysis covers 100% of generator classes in the code generation system — no generator is missing from the map
- **SC-002**: Every file produced by "Generate Module", "Generate Repository Module", and "Generate UI-FBP" is mapped to a proposed subdirectory in the refined folder hierarchy
- **SC-003**: The Code Generator panel is visible and interactive on Pro tier, with path selection, input selection, and file-tree checkbox toggling all functional
- **SC-004**: The "Generate Repository Module" / "Remove Repository Module" buttons are no longer in the IP Type Properties panel and are exclusively in the Code Generator panel
- **SC-005**: The migration plan identifies at least 3 distinct future feature steps, each with clear scope, prerequisites, and deliverables
- **SC-006**: A developer unfamiliar with the codebase can read the dependency analysis and migration plan and understand the full code generation architecture and evolution path within 30 minutes

## Clarifications

### Session 2026-04-21

- Q: Should the dependency analysis identify module scaffolding as a distinct prerequisite component? → A: Yes. A "module scaffolding" component must be extracted that creates the KMP module structure given a Module name from the top bar settings. This is a prerequisite for all other file generation components.
- Q: Are there only two generation paths (Repository, UI-FBP)? → A: No. A third path — "Generate Module" — takes the current flow graph and Module name as input and generates an end-to-end MVVM vertical slice with a UI stub. The Code Generator panel offers all three paths.

## Assumptions

- This feature does not wire the Code Generator panel to actual code generation logic — the panel is a functional UI prototype with input selection and file-tree display, but pressing "Generate" does not produce files via the new panel (existing toolbar buttons continue to work as before)
- The dependency analysis and migration plan are delivered as documentation artifacts within the feature's specs directory, not as code
- The proposed folder hierarchy is a recommendation that will be implemented in a subsequent feature — this feature does not move any existing generated files
- The Code Generator panel follows the same collapsible panel pattern as Node Generator and IP Generator, positioned on the left side of the graph editor
- The file-tree checkbox model supports three states: selected (all children), deselected (no children), and indeterminate (some children selected)
- Feature 064 (vertical slice refactor) established the precedent for analysis/documentation features — this feature follows that pattern

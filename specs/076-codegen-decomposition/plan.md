# Implementation Plan: Unified Configurable Code Generation

**Branch**: `076-codegen-decomposition` | **Date**: 2026-04-21 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/076-codegen-decomposition/spec.md`

## Summary

Perform dependency analysis of the code generation system, prototype a new Code Generator UI panel with three generation paths (Generate Module, Repository, UI-FBP), a file-tree checkbox GUI for configurable file selection, and produce a migration plan for evolving from monolithic generation to a flow-graph-based configurable system. Deliverables: dependency analysis document, Code Generator panel (functional UI prototype), and migration plan document.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3, kotlinx-coroutines 1.8.0
**Primary Dependencies**: graphEditor (UI panel), flowGraph-generate (generator analysis), fbpDsl (FeatureGate for tier gating)
**Storage**: N/A — documentation artifacts + UI panel code
**Testing**: `./gradlew :graphEditor:compileKotlinJvm :graphEditor:jvmTest`
**Target Platform**: KMP Desktop (Compose Desktop 1.7.3)
**Project Type**: Existing KMP multi-module project
**Performance Goals**: N/A — UI panel, no runtime code generation
**Constraints**: Panel is a UI prototype only — no wiring to actual generation logic. Documentation artifacts live in specs directory.
**Scale/Scope**: 2 documentation artifacts, 1 new Compose panel + ViewModel, ~5 modified files for panel integration and button relocation

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Panel follows established patterns (NodeGeneratorPanel, IPGeneratorPanel). Single-responsibility ViewModel. |
| II. Test-Driven Development | PASS | Panel state management testable via ViewModel unit tests. File-tree model has clear input/output contracts. |
| III. User Experience Consistency | PASS | Follows exact CollapsiblePanel LEFT pattern with 220dp width, dropdowns, action buttons matching existing panels. |
| IV. Performance Requirements | N/A | UI panel with static data model. |
| V. Observability & Debugging | PASS | Panel state observable via StateFlow. |
| Licensing & IP | PASS | No new external dependencies. |

## Project Structure

### Documentation (this feature)

```text
specs/076-codegen-decomposition/
├── spec.md
├── plan.md              # This file
├── research.md
├── quickstart.md
├── dependency-analysis.md   # US1 deliverable
├── migration-plan.md        # US3 deliverable
├── checklists/
│   └── requirements.md
└── tasks.md
```

### Source Code (repository root)

```text
graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/
├── ui/
│   ├── CodeGeneratorPanel.kt           # NEW — Code Generator panel composable
│   ├── GraphEditorLayout.kt            # MODIFIED — add CodeGenerator to left column
│   ├── GraphEditorApp.kt               # MODIFIED — wire panel state + FeatureGate
│   └── PropertiesPanel.kt              # MODIFIED — remove repository buttons
└── viewmodel/
    └── CodeGeneratorViewModel.kt        # NEW — panel state management

flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/
└── model/
    └── GenerationFileTree.kt            # NEW — file tree data model
```

**Structure Decision**: Panel code follows existing graphEditor patterns. The `GenerationFileTree` model lives in flowGraph-generate since it models generation output structure and can be reused when wiring is eventually implemented.

## Code Generator Panel Design

### Panel Layout (following NodeGeneratorPanel pattern)
- `CollapsiblePanel(side = PanelSide.LEFT)`, 220.dp width
- Header: "Code Generator" (bold 16sp)
- Top section: Path dropdown ("Generate Module", "Repository", "UI-FBP")
- Input section (varies by path):
  - Generate Module: Reads from current FlowGraph name (displayed as label, not editable here)
  - Repository: Dropdown of custom IP Types
  - UI-FBP: File chooser button → shows selected filename
- File tree section: Scrollable checkbox tree
- Action buttons: "Generate" (disabled in this feature), and for Repository path: "Create Repository Module" / "Remove Repository Module"

### CodeGeneratorViewModel State
```kotlin
data class CodeGeneratorPanelState(
    val selectedPath: GenerationPath = GenerationPath.GENERATE_MODULE,
    val selectedIPTypeId: String? = null,
    val selectedUIFilePath: String? = null,
    val fileTree: GenerationFileTree = GenerationFileTree.empty(),
    val isExpanded: Boolean = false
)

enum class GenerationPath { GENERATE_MODULE, REPOSITORY, UI_FBP }
```

### GenerationFileTree Data Model
```kotlin
data class GenerationFileTree(
    val folders: List<FolderNode>
) {
    companion object { fun empty(): GenerationFileTree }
}

data class FolderNode(
    val name: String,
    val files: List<FileNode>,
    val selectionState: TriState  // ALL, NONE, PARTIAL
)

data class FileNode(
    val name: String,
    val isSelected: Boolean,
    val generatorId: String  // maps to generator class for future wiring
)

enum class TriState { ALL, NONE, PARTIAL }
```

### File Tree Population Per Path

The ViewModel builds the file tree based on the selected path and input:

**Generate Module**: Uses the current FlowGraph to determine files (controller count, whether persistence is needed)
**Repository**: Uses the selected IP Type to determine entity name → file names
**UI-FBP**: Uses the selected UI file → parses with UIComposableParser → file names from UIFBPSpec

### Button Relocation

Remove from `PropertiesPanel.kt` (IPTypePropertiesPanel section):
- `onCreateRepositoryModule` callback and its Button
- `onRemoveRepositoryModule` callback and its Button

Add to `CodeGeneratorPanel.kt`:
- Same buttons, shown only when path = Repository and an IP Type is selected
- Callbacks wired through the same `GraphEditorLayout.kt` → `ModuleSaveService` path

## Dependency Analysis Document Design

Delivered as `dependency-analysis.md` in the specs directory. Contains:

1. **Generator Catalogue**: Table of all 22 generators with inputs, outputs, dependencies
2. **Dependency Graph**: ASCII or Mermaid diagram showing directed edges
3. **Module Scaffolding Component**: Identified as root prerequisite
4. **Independent Units**: Groups of generators that can operate standalone
5. **Proposed Folder Hierarchy**: Current → proposed mapping for each file
6. **CodeNode/GraphNode Mapping**: Which generators could become CodeNodes vs GraphNodes

## Migration Plan Document Design

Delivered as `migration-plan.md` in the specs directory. Contains a numbered sequence of future features:

1. **Folder Hierarchy Migration**: Move existing generated files to new subdirectory structure
2. **Module Scaffolding Extraction**: Extract directory/gradle creation as standalone component
3. **Generator CodeNode Wrappers**: Wrap individual generators as CodeNodes
4. **Code Generation FlowGraph**: Create a flow graph that orchestrates generation
5. **Wire Code Generator Panel**: Connect panel selections to the generation flow graph
6. (Additional steps as identified by the dependency analysis)

## Complexity Tracking

No constitution violations. No complexity justification needed.

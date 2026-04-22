# Research: Unified Configurable Code Generation

**Feature**: 076-codegen-decomposition
**Date**: 2026-04-21

## R1: Complete Generator Catalogue

**Decision**: Document all 22 generator classes and 2 orchestrators, mapping inputs → outputs → dependencies.

### Individual Generators (flowGraph-generate/generator/)

| Generator | Output File(s) | Inputs | Dependencies |
|-----------|---------------|--------|--------------|
| ModuleGenerator | `build.gradle.kts` | FlowGraph, moduleName | None |
| FlowKtGenerator | `{Name}.flow.kt` | FlowGraph, packageName, ipTypeNames | None |
| RuntimeFlowGenerator | `generated/{Name}Flow.kt` | FlowGraph, packageName | None |
| RuntimeControllerGenerator | `generated/{Name}Controller.kt` | FlowGraph, packageName | None |
| RuntimeControllerInterfaceGenerator | `generated/{Name}ControllerInterface.kt` | FlowGraph, packageName | None |
| RuntimeControllerAdapterGenerator | `generated/{Name}ControllerAdapter.kt` | FlowGraph, packageName | None |
| RuntimeViewModelGenerator | `{Name}ViewModel.kt` | FlowGraph, packageName, existing content | ObservableStateResolver |
| UserInterfaceStubGenerator | `userInterface/{Name}.kt` | FlowGraph, packageName | None (write-once) |
| RepositoryCodeGenerator | Entity, DAO, Repository, BaseDao, AppDatabase, DatabaseModule, platform files | FlowGraph, ipTypeProperties | None |
| EntityCUDCodeNodeGenerator | `nodes/{Entity}CUDCodeNode.kt` | entityName, pluralName, ipTypeId | None |
| EntityRepositoryCodeNodeGenerator | `nodes/{Entity}RepositoryCodeNode.kt` | entityName, pluralName, ipTypeId | None |
| EntityDisplayCodeNodeGenerator | `nodes/{Plural}DisplayCodeNode.kt` | pluralName, packageName | None |
| EntityConverterGenerator | `{Entity}Converters.kt` | entityName, properties | None |
| EntityViewModelGenerator | `{Plural}ViewModel.kt` | pluralName, packageName | None |
| EntityPersistenceModuleGenerator | `{Plural}Persistence.kt` | pluralName, entityName | None |
| EntityUIGenerator | 3 UI files (list, add/update, row) | entityName, properties | None |
| UIFBPStateGenerator | `{Name}State.kt` | UIFBPSpec | None |
| UIFBPViewModelGenerator | `{Name}ViewModel.kt` | UIFBPSpec | None |
| UIFBPSourceCodeNodeGenerator | `nodes/{Name}SourceCodeNode.kt` | UIFBPSpec | None |
| UIFBPSinkCodeNodeGenerator | `nodes/{Name}SinkCodeNode.kt` | UIFBPSpec | None |

### Orchestrators

| Orchestrator | Method | Generators Called (in order) |
|-------------|--------|------------------------------|
| ModuleSaveService | saveModule() | ModuleGenerator → FlowKtGenerator → Runtime{Flow,Controller,Interface,Adapter} → RuntimeViewModelGenerator → UserInterfaceStubGenerator → RepositoryCodeGenerator |
| ModuleSaveService | saveEntityModule() | EntityModuleGenerator → EntityCUD/Repository/Display CodeNodes → EntityConverter → EntityViewModel → EntityPersistence → EntityUI → Runtime files → RepositoryCodeGenerator |
| UIFBPInterfaceGenerator | generateAll() | UIFBPState → UIFBPViewModel → UIFBPSource → UIFBPSink + optional FlowKtGenerator |

### Support Classes

| Class | Role |
|-------|------|
| ObservableStateResolver | Analyzes FlowGraph source/sink ports → StateFlow property declarations |
| UIComposableParser | Parses Compose UI file → UIFBPSpec |

## R2: Proposed Folder Hierarchy

**Decision**: Refine the flat module layout into logical subdirectories.

**Current layout** (e.g., Addresses):
```
Addresses/
├── build.gradle.kts
├── settings.gradle.kts
└── src/commonMain/kotlin/.../addresses/
    ├── Addresses.flow.kt
    ├── AddressesViewModel.kt
    ├── AddressesPersistence.kt
    ├── AddressConverters.kt
    ├── generated/
    │   ├── AddressesFlow.kt
    │   ├── AddressesController.kt
    │   ├── AddressesControllerInterface.kt
    │   └── AddressesControllerAdapter.kt
    ├── nodes/
    │   ├── AddressCUDCodeNode.kt
    │   ├── AddressRepositoryCodeNode.kt
    │   └── AddressesDisplayCodeNode.kt
    └── userInterface/
        ├── Addresses.kt
        ├── AddUpdateAddress.kt
        └── AddressRow.kt
```

**Proposed layout**:
```
Addresses/
├── build.gradle.kts
├── settings.gradle.kts
└── src/commonMain/kotlin/.../addresses/
    ├── flow/
    │   ├── Addresses.flow.kt
    │   └── AddressesFlow.kt
    ├── controller/
    │   ├── AddressesController.kt
    │   ├── AddressesControllerInterface.kt
    │   └── AddressesControllerAdapter.kt
    ├── viewmodel/
    │   └── AddressesViewModel.kt
    ├── nodes/
    │   ├── AddressCUDCodeNode.kt
    │   ├── AddressRepositoryCodeNode.kt
    │   └── AddressesDisplayCodeNode.kt
    ├── userInterface/
    │   ├── Addresses.kt
    │   ├── AddUpdateAddress.kt
    │   └── AddressRow.kt
    ├── persistence/
    │   ├── AddressesPersistence.kt
    │   └── AddressConverters.kt
    └── iptypes/
        └── (module-level IP types if any)
```

**Key changes**:
- `generated/` renamed to split across `flow/` and `controller/`
- ViewModel moves from root to `viewmodel/`
- Persistence files move from root to `persistence/`
- Converter files move from root to `persistence/`

## R3: Existing Panel Pattern

**Decision**: Follow the exact pattern of NodeGeneratorPanel and IPGeneratorPanel.

**Pattern**:
- Composable function: `fun CodeGeneratorPanel(viewModel: CodeGeneratorViewModel, isExpanded: Boolean, onToggle: () -> Unit, ...)`
- Wrapped in `CollapsiblePanel(isExpanded, onToggle, side = PanelSide.LEFT)`
- Fixed width: `260.dp`, `fillMaxHeight()`, background `Color(0xFFFAFAFA)`, border
- Header: title text (bold 16sp)
- Top section: `ExposedDropdownMenuBox` for path/input selection
- Content section: scrollable area below dropdown
- Action buttons at bottom

**Layout position**: In `GraphEditorLayout.kt`, add the panel in the left column alongside NodeGeneratorPanel and IPGeneratorPanel, controlled by `isCodeGeneratorExpanded` state.

## R4: File Tree Checkbox Model

**Decision**: Use a tree data model with three-state checkboxes (checked, unchecked, indeterminate).

**Data model**:
```
GenerationFileTree
├── folders: List<FolderNode>
│   ├── name: String (e.g., "controller")
│   ├── selectionState: TriState (ALL, NONE, PARTIAL)
│   └── files: List<FileNode>
│       ├── name: String (e.g., "AddressesController.kt")
│       ├── isSelected: Boolean
│       └── generatorId: String (maps to generator class)
```

**Behavior**: Folder checkbox toggles all children. Individual file deselection sets parent to PARTIAL. This is a display model only — no code generation wiring in this feature.

## R5: Module Scaffolding as Prerequisite

**Decision**: In the dependency analysis, identify "Module Scaffolding" as a distinct root component that all other generators depend on.

**Module Scaffolding** creates:
- Module directory structure
- `build.gradle.kts` (via ModuleGenerator)
- `settings.gradle.kts`
- Source directory tree (`src/commonMain/kotlin/...`, `src/jvmMain/kotlin/...`)

**Input**: Module name (from top bar FlowGraph properties)
**Dependencies**: None — this is the root prerequisite
**Dependents**: All other generators require the directory structure to exist

## R6: Generation Path → File Tree Mapping

**Decision**: Each generation path produces a deterministic file tree based on the primary input.

**Generate Module** path (input: FlowGraph + Module name):
- `flow/{Name}.flow.kt`
- `flow/{Name}Flow.kt`
- `controller/{Name}Controller.kt`, `ControllerInterface.kt`, `ControllerAdapter.kt`
- `viewmodel/{Name}ViewModel.kt`
- `userInterface/{Name}.kt`
- (optional persistence/ if repo nodes present)

**Repository** path (input: IP Type):
- All of "Generate Module" plus:
- `nodes/{Entity}CUDCodeNode.kt`, `RepositoryCodeNode.kt`, `DisplayCodeNode.kt`
- `persistence/{Entity}Converters.kt`, `{Plural}Persistence.kt`
- `userInterface/AddUpdate{Entity}.kt`, `{Entity}Row.kt`

**UI-FBP** path (input: UI file):
- `viewmodel/{Name}ViewModel.kt`, `{Name}State.kt`
- `controller/{Name}Controller.kt`, `{Name}ControllerInterface.kt`, `{Name}ControllerAdapter.kt`
- `flow/{Name}Flow.kt`
- `nodes/{Name}SourceCodeNode.kt`, `{Name}SinkCodeNode.kt`
- `flow/{Name}.flow.kt` (bootstrap)

# Research: Folder Hierarchy Migration

**Feature**: 077-folder-hierarchy-migration
**Date**: 2026-04-22

## R1: Generator Output Path Changes

**Decision**: Update the package/path constants in `ModuleSaveService` to use new subdirectory names instead of `generated/`.

**Current paths** (ModuleSaveService lines 62-64):
- `GENERATED_SUBPACKAGE = "generated"` → files go to `{basePackage}.generated/`
- `USER_INTERFACE_SUBPACKAGE = "userInterface"` → stays the same
- `PERSISTENCE_SUBPACKAGE = "persistence"` → stays the same (already correct)

**New subpackages needed**:
- `FLOW_SUBPACKAGE = "flow"` → for .flow.kt and Flow.kt
- `CONTROLLER_SUBPACKAGE = "controller"` → for Controller, ControllerInterface, ControllerAdapter
- `VIEWMODEL_SUBPACKAGE = "viewmodel"` → for ViewModel.kt

**Changes required**:
1. Replace `GENERATED_SUBPACKAGE` with `FLOW_SUBPACKAGE` and `CONTROLLER_SUBPACKAGE`
2. Move .flow.kt from base package to `flow/` subpackage
3. Move ViewModel.kt from base package to `viewmodel/` subpackage
4. Move Flow.kt from `generated/` to `flow/`
5. Move Controller*.kt from `generated/` to `controller/`

## R2: Import Reference Updates

**Decision**: Each generator must produce correct package declarations and import statements for the new layout.

**Key cross-references**:
- Controller imports Flow → `import {basePackage}.flow.{Name}Flow`
- ControllerAdapter imports ControllerInterface → `import {basePackage}.controller.{Name}ControllerInterface`
- ViewModel imports ControllerInterface → `import {basePackage}.controller.{Name}ControllerInterface`
- ViewModel imports ControllerAdapter → `import {basePackage}.controller.{Name}ControllerAdapter`
- Flow imports ViewModel state → `import {basePackage}.viewmodel.{Name}ViewModel` (if referenced)
- UI stub imports ViewModel → `import {basePackage}.viewmodel.{Name}ViewModel`

**Generator files to update**:
- `RuntimeFlowGenerator.generate()` → package declaration changes from `generated` to `flow`
- `RuntimeControllerGenerator.generate()` → package from `generated` to `controller`, import Flow from `flow`
- `RuntimeControllerInterfaceGenerator.generate()` → package from `generated` to `controller`
- `RuntimeControllerAdapterGenerator.generate()` → package from `generated` to `controller`, import Interface from `controller`
- `RuntimeViewModelGenerator.generate()` → package from base to `viewmodel`, import from `controller`
- `UserInterfaceStubGenerator.generate()` → import ViewModel from `viewmodel` instead of base
- `FlowKtGenerator.generateFlowKt()` → package from base to `flow` (if package is embedded)

## R3: ModuleSessionFactory Runtime Resolution

**Decision**: Update the class loading pattern in `ModuleSessionFactory` to use new package paths.

**Current** (line 84-95):
```
${modulePackage}.generated.${moduleName}ControllerInterface
${modulePackage}.${moduleName}ViewModel
```

**New**:
```
${modulePackage}.controller.${moduleName}ControllerInterface
${modulePackage}.viewmodel.${moduleName}ViewModel
```

**Backward compatibility**: For FR-010 (dual layout support during transition), try the new path first, fall back to old path.

## R4: Scanner Path Updates

**Decision**: The existing scanners for `nodes/` and `userInterface/` already use directory name matching (`it.name == "nodes"`) which works regardless of depth. No change needed for those. Need to verify `flow/` and `controller/` are not scanned (they contain generated files, not user-discoverable nodes).

**GraphEditorApp.kt scanning** (line 110-121):
- `nodes/` → already scanned by name match — no change needed
- `userInterface/` → already scanned by name match — no change needed
- `flow/`, `controller/`, `viewmodel/` → NOT scanned (correct — no user-discoverable items there)

**DynamicPreviewDiscovery**:
- Scans `userInterface/` for `*PreviewProvider.kt` files — no change needed (path unchanged)

## R5: Demo Project Migration Strategy

**Decision**: Migrate each module by moving files and updating package/import declarations. Do NOT re-generate — preserve user-authored code.

**Migration steps per module**:
1. Create new subdirectories: `flow/`, `controller/`, `viewmodel/`
2. Move .flow.kt from root to `flow/`
3. Move Flow.kt from `generated/` to `flow/`
4. Move Controller*.kt from `generated/` to `controller/`
5. Move ViewModel.kt from root to `viewmodel/`
6. Move Persistence.kt, Converters.kt from root to `persistence/` (if applicable)
7. Update package declarations in moved files
8. Update import statements in ALL files that reference moved files
9. Delete empty `generated/` directory
10. Compile and verify

**Modules to migrate**: StopWatch, UserProfiles, Addresses, EdgeArtFilter, WeatherForecast, TestModule

## R6: EntityModuleGenerator Path Updates

**Decision**: Update `EntityModuleGenerator.generateModule()` to use the new subpackages for output paths.

**Current** (line 68): Converters go to base package → move to `persistence/`
**Current** (line 75): .flow.kt goes to base package → move to `flow/`
**Current** (line 98-108): Runtime files go to `generated/` → split to `flow/` and `controller/`
**Current** (line 111-118): UI files go to `userInterface/` → no change

All path construction in `EntityModuleGenerator` follows `ModuleSaveService` patterns, so updating the subpackage constants propagates correctly.

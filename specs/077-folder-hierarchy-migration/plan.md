# Implementation Plan: Folder Hierarchy Migration

**Branch**: `077-folder-hierarchy-migration` | **Date**: 2026-04-22 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/077-folder-hierarchy-migration/spec.md`

## Summary

Migrate code generation output from the current flat layout (with `generated/` subdirectory) to an organized hierarchy (flow/, controller/, viewmodel/, persistence/). Update all generators to write to new paths, update import references, migrate 5+ demo project modules, and update runtime scanning to locate classes in the new packages.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3
**Primary Dependencies**: flowGraph-generate (generators, ModuleSaveService, EntityModuleGenerator), flowGraph-inspect (NodeDefinitionRegistry), flowGraph-execute (ModuleSessionFactory), graphEditor (scanning, DynamicPreviewDiscovery)
**Storage**: Filesystem — generated .kt source files in KMP module structures
**Testing**: `./gradlew :flowGraph-generate:jvmTest :graphEditor:jvmTest` + Demo Project module compilation
**Target Platform**: KMP Desktop
**Project Type**: Existing KMP multi-module project + Demo Project
**Performance Goals**: N/A — file generation, not runtime
**Constraints**: Must preserve user-authored code. Must support dual layout during transition (FR-010). Demo Project modules must compile after migration.
**Scale/Scope**: ~10 generator files updated, 6 demo modules migrated, ~3 scanner/resolver files updated

## Constitution Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Organized hierarchy improves readability. Single-responsibility folders. |
| II. Test-Driven Development | PASS | Each generator change verified by compilation of demo modules. |
| III. User Experience Consistency | PASS | No user-facing changes — internal reorganization only. |
| IV. Performance Requirements | N/A | File generation, not runtime. |
| V. Observability & Debugging | PASS | No change to runtime behavior. |
| Licensing & IP | PASS | No new dependencies. |

## Project Structure

### Source Code (CodeNodeIO repository)

```text
flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/
├── save/
│   └── ModuleSaveService.kt                 # MODIFIED — subpackage constants, path construction
├── generator/
│   ├── RuntimeFlowGenerator.kt              # MODIFIED — package declaration: generated → flow
│   ├── RuntimeControllerGenerator.kt        # MODIFIED — package: generated → controller, imports
│   ├── RuntimeControllerInterfaceGenerator.kt # MODIFIED — package: generated → controller
│   ├── RuntimeControllerAdapterGenerator.kt # MODIFIED — package: generated → controller, imports
│   ├── RuntimeViewModelGenerator.kt         # MODIFIED — package: base → viewmodel, imports
│   ├── UserInterfaceStubGenerator.kt        # MODIFIED — import ViewModel from viewmodel
│   ├── FlowKtGenerator.kt                  # MODIFIED — package: base → flow (if applicable)
│   ├── EntityModuleGenerator.kt             # MODIFIED — output paths for entity files
│   ├── EntityConverterGenerator.kt          # MODIFIED — package: base → persistence
│   └── EntityPersistenceGenerator.kt        # MODIFIED — verify package is persistence
└── model/
    └── GenerationFileTreeBuilder.kt         # ALREADY UPDATED — uses new paths

flowGraph-execute/src/jvmMain/kotlin/io/codenode/flowgraphexecute/
└── ModuleSessionFactory.kt                  # MODIFIED — class loading: generated → controller/viewmodel

graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/
└── ui/
    └── GraphEditorApp.kt                    # VERIFY — scanning already uses name-based matching
```

### Demo Project (CodeNodeIO-DemoProject)

```text
StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/
    generated/ → split into flow/ and controller/
    StopWatch.flow.kt → flow/
    StopWatchViewModel.kt → viewmodel/

UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/
    generated/ → split into flow/ and controller/
    UserProfiles.flow.kt → flow/
    UserProfilesViewModel.kt → viewmodel/
    UserProfilesPersistence.kt → persistence/ (already there or move)

Addresses/src/commonMain/kotlin/io/codenode/addresses/
    (same pattern)

EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/
    (same pattern — no persistence)

WeatherForecast/src/commonMain/kotlin/io/codenode/weatherforecast/
    (same pattern — no persistence)

TestModule/src/commonMain/kotlin/io/codenode/demo/
    (same pattern — UI-FBP layout)
```

## Generator Path Change Design

### ModuleSaveService Constants
```
Current:                          New:
GENERATED_SUBPACKAGE = "generated" → removed
                                    FLOW_SUBPACKAGE = "flow"
                                    CONTROLLER_SUBPACKAGE = "controller"
                                    VIEWMODEL_SUBPACKAGE = "viewmodel"
USER_INTERFACE_SUBPACKAGE = "userInterface" → unchanged
PERSISTENCE_SUBPACKAGE = "persistence"     → unchanged
```

### File → Subdirectory Mapping

| File | Current Location | New Location |
|------|-----------------|--------------|
| {Name}.flow.kt | base package | flow/ |
| {Name}Flow.kt | generated/ | flow/ |
| {Name}Controller.kt | generated/ | controller/ |
| {Name}ControllerInterface.kt | generated/ | controller/ |
| {Name}ControllerAdapter.kt | generated/ | controller/ |
| {Name}ViewModel.kt | base package | viewmodel/ |
| {Name}.kt (UI stub) | userInterface/ | userInterface/ (unchanged) |
| {Name}Persistence.kt | base package | persistence/ |
| {Entity}Converters.kt | base package | persistence/ |

### Import Cross-References

Each generator must produce imports referencing the correct new packages:
- Controller imports Flow: `{basePackage}.flow.{Name}Flow`
- ControllerAdapter imports Interface: `{basePackage}.controller.{Name}ControllerInterface`
- ViewModel imports Interface: `{basePackage}.controller.{Name}ControllerInterface`
- ViewModel imports Adapter: `{basePackage}.controller.{Name}ControllerAdapter`
- UI stub imports ViewModel: `{basePackage}.viewmodel.{Name}ViewModel`

### ModuleSessionFactory Dual-Layout Resolution

For backward compatibility (FR-010), try new paths first, fall back to old:
```
1. Try: ${modulePackage}.controller.${Name}ControllerInterface
2. Fallback: ${modulePackage}.generated.${Name}ControllerInterface

1. Try: ${modulePackage}.viewmodel.${Name}ViewModel
2. Fallback: ${modulePackage}.${Name}ViewModel
```

## Demo Module Migration Strategy

For each of the 6 modules:
1. Create new subdirectories (flow/, controller/, viewmodel/, persistence/ if needed)
2. Move files to new locations
3. Update package declarations in moved files
4. Update import statements in ALL files referencing moved files
5. Delete empty `generated/` directory
6. Compile to verify

User-authored files (UI composables, custom CodeNodes) are NOT moved — only their imports are updated if they reference moved generated files.

## Complexity Tracking

No constitution violations. No complexity justification needed.

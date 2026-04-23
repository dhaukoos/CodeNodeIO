# Implementation Plan: Module Scaffolding Extraction

**Branch**: `078-extract-module-scaffolding` | **Date**: 2026-04-22 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/078-extract-module-scaffolding/spec.md`

## Summary

Extract module directory creation and Gradle file generation from `ModuleSaveService` into a standalone `ModuleScaffoldingGenerator`. Refactor `saveModule()` and `saveEntityModule()` to delegate to it. Pure refactoring — behavior-preserving with zero regressions.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: flowGraph-generate (ModuleSaveService, ModuleGenerator)
**Storage**: Filesystem — generated KMP module directories
**Testing**: `./gradlew :flowGraph-generate:jvmTest`
**Target Platform**: KMP Desktop
**Project Type**: Existing KMP multi-module project
**Constraints**: Behavior-preserving refactoring. All existing tests must pass without modification.
**Scale/Scope**: 1 new class, 1 modified class, ~10 new unit tests

## Constitution Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Single-responsibility extraction. Removes embedded logic from orchestrator. |
| II. Test-Driven Development | PASS | New unit tests for scaffolding generator. Existing tests verify no regression. |
| III. User Experience Consistency | PASS | No user-facing changes — pure internal refactoring. |
| IV. Performance Requirements | N/A | File generation. |
| V. Observability & Debugging | PASS | No behavioral change. |
| Licensing & IP | PASS | No new dependencies. |

## Project Structure

```text
flowGraph-generate/src/
├── jvmMain/kotlin/io/codenode/flowgraphgenerate/
│   ├── save/
│   │   ├── ModuleSaveService.kt                    # MODIFIED — delegate to scaffolding generator
│   │   └── ModuleScaffoldingGenerator.kt            # NEW — standalone scaffolding component
│   └── generator/
│       └── ModuleGenerator.kt                       # UNCHANGED — still generates build.gradle.kts content
└── jvmTest/kotlin/io/codenode/flowgraphgenerate/
    └── save/
        ├── ModuleSaveServiceTest.kt                 # UNCHANGED — existing tests verify no regression
        └── ModuleScaffoldingGeneratorTest.kt        # NEW — unit tests for scaffolding in isolation
```

## ModuleScaffoldingGenerator Design

### Class

```kotlin
class ModuleScaffoldingGenerator(
    private val moduleGenerator: ModuleGenerator = ModuleGenerator()
) {
    fun generate(
        moduleName: String,
        outputDir: File,
        targetPlatforms: List<FlowGraph.TargetPlatform> = emptyList(),
        packagePrefix: String = ModuleSaveService.DEFAULT_PACKAGE_PREFIX,
        isEntityModule: Boolean = false
    ): ScaffoldingResult
}

data class ScaffoldingResult(
    val moduleDir: File,
    val basePackage: String,
    val flowPackage: String,
    val controllerPackage: String,
    val viewModelPackage: String,
    val userInterfacePackage: String,
    val filesCreated: List<String>
)
```

### What it does (in order)

1. Compute `basePackage` from `packagePrefix` + `moduleName`
2. Compute subpackages (flow, controller, viewmodel, userInterface)
3. Create module directory
4. Create source directory structure for each package (commonMain, jvmMain, commonTest, platform-specific)
5. Write `build.gradle.kts` (via `ModuleGenerator.generateBuildGradle()`) — write-once
6. Write `settings.gradle.kts` (moved from `ModuleSaveService.generateSettingsGradle()`) — write-once
7. Return `ScaffoldingResult` with all paths

### What ModuleSaveService becomes

```
Before:
  saveModule() {
    1. compute packages
    2. create dirs          ← extracted
    3. write gradle files   ← extracted
    4. write .flow.kt
    5. generate runtime files
    6. generate viewmodel
    7. generate UI stub
    8. generate persistence
  }

After:
  saveModule() {
    1. scaffoldingGenerator.generate(...)  ← delegated
    2. write .flow.kt
    3. generate runtime files
    4. generate viewmodel
    5. generate UI stub
    6. generate persistence
  }
```

## Complexity Tracking

No constitution violations. No complexity justification needed.

# Research: Module Scaffolding Extraction

**Feature**: 078-extract-module-scaffolding
**Date**: 2026-04-22

## R1: Scaffolding Code to Extract

**Decision**: Extract directory creation, Gradle file generation, and settings file generation from `ModuleSaveService` into `ModuleScaffoldingGenerator`.

**Code to extract from ModuleSaveService**:
1. Module directory creation (line 182-185): `File(outputDir, effectiveModuleName).mkdirs()`
2. Source directory structure creation (line 192-196): 5 calls to `createDirectoryStructure()` for base, flow, controller, viewmodel, userInterface packages
3. `createDirectoryStructure()` method (line 942-976): Creates commonMain, commonTest, jvmMain, jvmTest, and platform-specific source sets based on target platforms
4. Gradle file generation (line 198-210): `writeFileIfNew` for build.gradle.kts (via `moduleGenerator.generateBuildGradle()`) and settings.gradle.kts (via `generateSettingsGradle()`)
5. `generateSettingsGradle()` method (line 1280+): Inline settings.gradle.kts generation

**What stays in ModuleSaveService**:
- `enrichWithCodeNodeMetadata()` — pre-processing of FlowGraph
- `deriveModuleName()` — name derivation logic
- Package name computation — `$DEFAULT_PACKAGE_PREFIX.${effectiveModuleName.lowercase()}`
- All content generator calls (flow, controller, viewmodel, UI, persistence)
- File write tracking (filesCreated, filesOverwritten, filesDeleted)

## R2: ModuleScaffoldingGenerator API Design

**Decision**: The generator takes module name, output directory, target platforms, and optionally a package prefix. Returns a result containing the module directory and list of created files.

**Signature**:
```kotlin
class ModuleScaffoldingGenerator {
    fun generate(
        moduleName: String,
        outputDir: File,
        targetPlatforms: List<FlowGraph.TargetPlatform> = emptyList(),
        packagePrefix: String = "io.codenode"
    ): ScaffoldingResult
}

data class ScaffoldingResult(
    val moduleDir: File,
    val basePackage: String,
    val filesCreated: List<String>
)
```

**Rationale**: The target platforms parameter replaces the FlowGraph dependency — the scaffolding generator needs to know which platform-specific source sets to create and how to configure build.gradle.kts, but doesn't need the full FlowGraph.

## R3: Nodes and IPTypes Directories

**Decision**: The scaffolding generator also creates the `nodes/` and `iptypes/` subdirectories alongside flow/, controller/, viewmodel/, userInterface/. This ensures all standard module subdirectories exist before any generator runs.

**Rationale**: The `nodes/` directory is part of the standard module structure and is expected by NodeDefinitionRegistry scanning. The `iptypes/` directory is expected by IPTypeDiscovery for module-level IP type definitions. Creating both as part of scaffolding is cleaner than having individual generators create them ad-hoc.

## R4: Integration with saveEntityModule

**Decision**: `saveEntityModule()` also delegates to `ModuleScaffoldingGenerator` for its directory and Gradle creation. The entity module uses the same scaffolding with `isEntityModule = true` passed to `generateBuildGradle()`.

**Current code** (saveEntityModule line 320-327):
```kotlin
createDirectoryStructure(moduleDir, spec.basePackage, output.flowGraph)
createDirectoryStructure(moduleDir, "${spec.basePackage}.$FLOW_SUBPACKAGE", output.flowGraph)
createDirectoryStructure(moduleDir, "${spec.basePackage}.$CONTROLLER_SUBPACKAGE", output.flowGraph)
createDirectoryStructure(moduleDir, "${spec.basePackage}.$VIEWMODEL_SUBPACKAGE", output.flowGraph)
createDirectoryStructure(moduleDir, "${spec.basePackage}.$USER_INTERFACE_SUBPACKAGE", output.flowGraph)
```

This is identical to `saveModule()` scaffolding — the only difference is `isEntityModule = true` in the build.gradle.kts generation. The scaffolding generator accepts this as a parameter.

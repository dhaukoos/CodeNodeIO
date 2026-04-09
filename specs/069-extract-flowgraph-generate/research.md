# Research: Extract flowGraph-generate Module

**Feature**: 069-extract-flowgraph-generate
**Date**: 2026-04-09

## R1: Runtime Types for Sub-FlowGraph Decomposition

**Decision**: Use In2AnyOut1Runtime + In3AnyOut1Runtime for the two-node sub-graph.

**Rationale**: Both exist in fbpDsl with factory methods:
- `CodeNodeFactory.createIn2AnyOut1Processor()` (line 910)
- `CodeNodeFactory.createIn3AnyOut1Processor()` (line 1057)

Both support anyInput mode — fires when any single input receives data.

**Alternatives Considered**:
- In4AnyOut1Runtime: Doesn't exist, would require fbpDsl changes, exceeds 3-input max convention
- Single In3Out1 + external passthrough: More complex wiring, less clean

## R2: kotlinCompiler Dependencies

**Decision**: The new module's build.gradle.kts mirrors kotlinCompiler's deps plus the 4 extracted modules.

**Current kotlinCompiler dependencies**:
- `project(":fbpDsl")` — core FBP types
- `libs.kotlinPoet` — KotlinPoet for code generation
- `libs.coroutines.core` — kotlinx-coroutines
- `libs.serialization.json` — kotlinx-serialization
- `kotlin("compiler-embeddable")` — JVM-only, Kotlin compilation support

**Additional dependencies needed** (from graphEditor generate-bucket files):
- `project(":flowGraph-types")` — IPTypeRegistry used by IPGeneratorViewModel
- `project(":flowGraph-persist")` — serialization used by CompilationService
- `project(":flowGraph-inspect")` — NodeDefinitionRegistry used by NodeGeneratorViewModel

## R3: Consumer Analysis

**Consumers of kotlinCompiler** (modules with `project(":kotlinCompiler")` dependency):
1. `graphEditor/build.gradle.kts` — primary consumer
2. `idePlugin/build.gradle.kts` — secondary consumer

**Consumers of graphEditor generate-bucket files** (internal imports):
1. `graphEditor/src/jvmMain/kotlin/ui/IPGeneratorPanel.kt` → imports IPGeneratorViewModel
2. `graphEditor/src/jvmMain/kotlin/ui/NodeGeneratorPanel.kt` → imports NodeGeneratorViewModel
3. `graphEditor/src/jvmMain/kotlin/Main.kt` → may import CompilationService, ModuleSaveService

These UI panels stay in graphEditor and will import from `io.codenode.flowgraphgenerate`.

## R4: Source Set Placement Analysis

**kotlinCompiler source sets**:
- commonMain: 30 files (generators, templates, validator) — all platform-independent KotlinPoet code
- commonTest: 23 files — platform-independent tests
- jvmMain: 2 files (RegenerateStopWatch.kt, GenerateGeoLocationModule.kt) — **DEAD CODE**, not extracted
- jvmTest: 2 files (characterization tests) — JVM-specific

The 2 jvmMain files are one-off code generation scripts that target StopWatch/ and GeoLocations/ directories. These modules live in CodeNodeIO-DemoProject but the scripts were missed during the feature 060 repo split. They must be moved to CodeNodeIO-DemoProject before the kotlinCompiler module is deleted.

**graphEditor generate-bucket files** (all in jvmMain):
- `viewmodel/IPGeneratorViewModel.kt` — uses ViewModel, IPTypeRegistry
- `viewmodel/NodeGeneratorViewModel.kt` — uses ViewModel, NodeDefinitionRegistry
- `compilation/CompilationService.kt` — uses file system APIs
- `compilation/CompilationValidator.kt` — validation logic
- `compilation/RequiredPropertyValidator.kt` — validation logic
- `save/ModuleSaveService.kt` — uses file system APIs for module generation

**Decision**: Preserve source set placement. commonMain stays commonMain, jvmMain stays jvmMain. The 2 jvmMain tool scripts are moved to CodeNodeIO-DemoProject (not extracted into flowGraph-generate). The graphEditor files go to jvmMain (they use JVM ViewModel and filesystem APIs).

## R5: Established Extraction Pattern (from features 065-068)

All four prior extractions follow an identical 8-phase Strangler Fig pattern:

1. **Setup**: Create module directory, build.gradle.kts, settings.gradle.kts entry
2. **Copy**: Copy files to new module, update package declarations
3. **TDD Tests**: Write failing tests for CodeNode port contract
4. **CodeNode**: Implement CodeNodeDefinition with appropriate runtime
5. **Migrate Consumers**: Update imports in graphEditor, idePlugin, etc.
6. **Remove Originals**: Delete source files from old locations
7. **Architecture Wiring**: Update architecture.flow.kt with child codeNode + port mappings
8. **Verification**: Run full test suite, verify compilation, validate architecture

Each phase produces a separate git commit for traceability.

## R6: Architecture.flow.kt Internal Connections

**Decision**: The generate graphNode will use the DSL's existing connection syntax for internal wiring between child codeNodes.

**Pattern from existing graphNodes** (e.g., flowGraph-execute):
```kotlin
val generate = graphNode("flowGraph-generate") {
    // Exposed ports (external interface)
    exposeInput("flowGraphModel", String::class)
    exposeInput("serializedOutput", String::class)
    exposeInput("nodeDescriptors", String::class)
    exposeInput("ipTypeMetadata", String::class)
    exposeOutput("generatedOutput", String::class)

    // Child CodeNode 1: aggregator
    val aggregator = codeNode("GenerateContextAggregator", nodeType = "TRANSFORMER") {
        input("flowGraphModel", String::class)
        input("serializedOutput", String::class)
        output("generationContext", String::class)
    }

    // Child CodeNode 2: generator
    val generator = codeNode("FlowGraphGenerate", nodeType = "TRANSFORMER") {
        input("generationContext", String::class)
        input("nodeDescriptors", String::class)
        input("ipTypeMetadata", String::class)
        output("generatedOutput", String::class)
    }

    // Port mappings: external → child
    portMapping("flowGraphModel", "generateContextAggregator", "flowGraphModel")
    portMapping("serializedOutput", "generateContextAggregator", "serializedOutput")
    portMapping("nodeDescriptors", "flowGraphGenerate", "nodeDescriptors")
    portMapping("ipTypeMetadata", "flowGraphGenerate", "ipTypeMetadata")
    portMapping("generatedOutput", "flowGraphGenerate", "generatedOutput")

    // Internal connection: aggregator → generator
    // (expressed via port mappings or internal connection DSL)
}
```

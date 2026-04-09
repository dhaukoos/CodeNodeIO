# Implementation Plan: Extract flowGraph-generate Module

**Branch**: `069-extract-flowgraph-generate` | **Date**: 2026-04-09 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/069-extract-flowgraph-generate/spec.md`

## Summary

Extract all code generation files (67 from kotlinCompiler + 6 from graphEditor) into a new `flowGraph-generate` KMP module using the Strangler Fig pattern. The module boundary is expressed as two CodeNodeDefinitions forming a sub-FlowGraph: GenerateContextAggregatorCodeNode (In2AnyOut1, combining flowGraphModel + serializedOutput → generationContext) and FlowGraphGenerateCodeNode (In3AnyOut1, taking generationContext + nodeDescriptors + ipTypeMetadata → generatedOutput). This decomposition stays within the 3-input CodeNode maximum. The kotlinCompiler module is fully absorbed and deleted. Architecture.flow.kt is updated with the two-node sub-graph wiring.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: fbpDsl (core FBP domain model, CodeNodeFactory, In2AnyOut1Runtime, In3AnyOut1Runtime), flowGraph-types (IPTypeRegistry), flowGraph-persist (FlowGraphSerializer, FlowKtParser), flowGraph-inspect (NodeDefinitionRegistry), KotlinPoet (code generation), kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, kotlin-compiler-embeddable (JVM only)
**Storage**: N/A — generates source code files to filesystem
**Testing**: kotlin.test (commonTest), JUnit5 (jvmTest), characterization tests (jvmTest)
**Target Platform**: KMP Desktop (JVM), KMP iOS, KMP Android
**Project Type**: KMP multi-module (Gradle composite)
**Performance Goals**: N/A (code generation is batch, not latency-sensitive)
**Constraints**: Must not break any existing tests; Strangler Fig pattern ensures incremental migration
**Scale/Scope**: 65 kotlinCompiler files + 6 graphEditor files = 71 files total moving to new module (2 kotlinCompiler jvmMain tool scripts excluded — dead code from feature 060 demo split)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Module extraction improves single-responsibility; CodeNode boundary is a clean public interface |
| II. Test-Driven Development | PASS | TDD for both CodeNodes; characterization tests migrate intact; all existing tests must pass |
| III. User Experience Consistency | N/A | No user-facing UI changes (UI panels stay in graphEditor) |
| IV. Performance Requirements | N/A | Code generation is batch; no runtime performance impact |
| V. Observability & Debugging | PASS | CodeNode ports provide observable data flow boundaries |
| Licensing & IP | PASS | All dependencies are Apache 2.0 / MIT (KotlinPoet, kotlin-compiler-embeddable are Apache 2.0) |

**Gate Result**: PASS — no violations.

## Project Structure

### Documentation (this feature)

```text
specs/069-extract-flowgraph-generate/
├── spec.md
├── plan.md              # This file
├── research.md          # Phase 0 output
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
flowGraph-generate/
├── build.gradle.kts
└── src/
    ├── commonMain/kotlin/io/codenode/flowgraphgenerate/
    │   ├── generator/           # 22 files from kotlinCompiler/generator/
    │   │   ├── KotlinCodeGenerator.kt
    │   │   ├── ModuleGenerator.kt
    │   │   ├── FlowKtGenerator.kt
    │   │   ├── FlowGenerator.kt
    │   │   ├── FlowGraphFactoryGenerator.kt
    │   │   ├── ComponentGenerator.kt
    │   │   ├── GenericNodeGenerator.kt
    │   │   ├── RuntimeFlowGenerator.kt
    │   │   ├── RuntimeViewModelGenerator.kt
    │   │   ├── RuntimeTypeResolver.kt
    │   │   ├── RuntimeControllerGenerator.kt
    │   │   ├── RuntimeControllerInterfaceGenerator.kt
    │   │   ├── RuntimeControllerAdapterGenerator.kt
    │   │   ├── ConnectionWiringResolver.kt
    │   │   ├── ObservableStateResolver.kt
    │   │   ├── ConfigAwareGenerator.kt
    │   │   ├── BuildScriptGenerator.kt
    │   │   ├── UserInterfaceStubGenerator.kt
    │   │   ├── RepositoryCodeGenerator.kt
    │   │   ├── EntityModuleGenerator.kt
    │   │   ├── EntityModuleSpec.kt
    │   │   ├── EntityFlowGraphBuilder.kt
    │   │   ├── EntityPersistenceGenerator.kt
    │   │   ├── EntityUIGenerator.kt
    │   │   ├── EntityCUDCodeNodeGenerator.kt
    │   │   ├── EntityDisplayCodeNodeGenerator.kt
    │   │   └── EntityRepositoryCodeNodeGenerator.kt
    │   ├── templates/           # 8 files from kotlinCompiler/templates/
    │   │   ├── NodeTemplate.kt
    │   │   ├── SourceTemplate.kt
    │   │   ├── SinkTemplate.kt
    │   │   ├── TransformerTemplate.kt
    │   │   ├── FilterTemplate.kt
    │   │   ├── SplitterTemplate.kt
    │   │   ├── MergerTemplate.kt
    │   │   └── ValidatorTemplate.kt
    │   ├── validator/           # 1 file from kotlinCompiler/validator/
    │   │   └── LicenseValidator.kt
    │   ├── compilation/         # 3 files from graphEditor/compilation/
    │   │   ├── CompilationService.kt
    │   │   ├── CompilationValidator.kt
    │   │   └── RequiredPropertyValidator.kt
    │   ├── viewmodel/           # 2 files from graphEditor/viewmodel/
    │   │   ├── IPGeneratorViewModel.kt
    │   │   └── NodeGeneratorViewModel.kt
    │   └── save/                # 1 file from graphEditor/save/
    │       └── ModuleSaveService.kt
    ├── commonTest/kotlin/io/codenode/flowgraphgenerate/
    │   ├── generator/           # 18 test files from kotlinCompiler
    │   ├── contract/            # 3 test files (Jvm/Ios/AndroidCompilationTest)
    │   ├── integration/         # 1 test file (PropertyCodeGenTest)
    │   └── validator/           # 1 test file (LicenseValidationTest)
    ├── jvmMain/kotlin/io/codenode/flowgraphgenerate/
    │   └── node/                # CodeNode definitions
    │       ├── GenerateContextAggregatorCodeNode.kt
    │       └── FlowGraphGenerateCodeNode.kt
    └── jvmTest/kotlin/io/codenode/flowgraphgenerate/
        ├── characterization/    # 2 files from kotlinCompiler/jvmTest
        │   ├── CodeGenerationCharacterizationTest.kt
        │   └── FlowKtGeneratorCharacterizationTest.kt
        └── node/                # TDD tests for CodeNodes
            ├── GenerateContextAggregatorCodeNodeTest.kt
            └── FlowGraphGenerateCodeNodeTest.kt
```

**Structure Decision**: Follows the established KMP module pattern from features 065-068. Source files preserve their original subdirectory structure (generator/, templates/, validator/, compilation/, viewmodel/, save/) under the new `io.codenode.flowgraphgenerate` package. CodeNode definitions go in `jvmMain/node/` (JVM-specific factory APIs). The 6 graphEditor files are placed in commonMain unless they have JVM-specific dependencies (to be verified during implementation).

## Research Decisions

### R1: Runtime Types for Sub-FlowGraph Decomposition

**Decision**: Use In2AnyOut1Runtime for GenerateContextAggregator and In3AnyOut1Runtime for FlowGraphGenerate.

**Rationale**: Both runtime types exist in fbpDsl with corresponding factory methods (CodeNodeFactory.createIn2AnyOut1Processor at line 910, CodeNodeFactory.createIn3AnyOut1Processor at line 1057). The anyInput mode ensures each CodeNode fires when any input arrives, matching the established pattern from features 065-068.

**Alternatives Considered**: Creating a new In4AnyOut1Runtime was rejected because it would require changes to fbpDsl and doesn't follow the existing 3-input maximum convention. The two-node decomposition is cleaner and demonstrates the composability of the FBP approach.

### R2: Package Naming

**Decision**: Use `io.codenode.flowgraphgenerate` as the base package.

**Rationale**: Follows the established pattern: flowGraph-types → `flowgraphtypes`, flowGraph-persist → `flowgraphpersist`, flowGraph-inspect → `flowgraphinspect`, flowGraph-execute → `flowgraphexecute`.

### R3: Source Set Placement

**Decision**: kotlinCompiler's commonMain files stay in commonMain, jvmMain files stay in jvmMain. The 6 graphEditor files are currently all in jvmMain — they should be checked for JVM-specific APIs during implementation and placed accordingly.

**Rationale**: Preserve the existing source set structure from kotlinCompiler. The 6 graphEditor files use JVM filesystem APIs and ViewModel patterns — likely jvmMain candidates.

### R4: kotlinCompiler Module Deletion

**Decision**: Delete the entire kotlinCompiler module directory after all consumers are migrated, and remove its entry from settings.gradle.kts. Two jvmMain tool scripts (RegenerateStopWatch.kt, GenerateGeoLocationModule.kt) are NOT extracted into flowGraph-generate — they belong with the DemoProject and must be moved to CodeNodeIO-DemoProject before the kotlinCompiler module is deleted.

**Rationale**: Unlike previous extractions (which extracted a subset of files from a surviving module), this extraction absorbs kotlinCompiler wholesale. The two tool scripts reference StopWatch/ and GeoLocations/ directories that live in CodeNodeIO-DemoProject. They were missed during the feature 060 repo split and need to be relocated there (not deleted).

### R5: graphEditor Dependency Update

**Decision**: Replace `project(":kotlinCompiler")` with `project(":flowGraph-generate")` in graphEditor/build.gradle.kts. Also update idePlugin/build.gradle.kts similarly.

**Rationale**: graphEditor and idePlugin are the only consumers of kotlinCompiler. After extraction, they depend on flowGraph-generate instead.

### R6: GenerateContextAggregator Purpose

**Decision**: The aggregator combines flowGraphModel (the graph to generate code for) and serializedOutput (the .flow.kts serialized form from persist) into a single generationContext string. This is a natural pairing — both represent "what to generate."

**Rationale**: The second node (FlowGraphGenerate) then receives this combined context plus the two metadata inputs (nodeDescriptors, ipTypeMetadata) that describe "how to generate." This 2+3 split aligns with the data flow semantics in architecture.flow.kt.

### R7: Internal Connection in architecture.flow.kt

**Decision**: The generate graphNode will have an internal connection from GenerateContextAggregator.output("generationContext") to FlowGraphGenerate.input("generationContext"), expressed using the existing DSL connection syntax within the graphNode block.

**Rationale**: This follows the established pattern where graphNodes can contain multiple child codeNodes with internal wiring, as documented in the fbpDsl DSL specification.

## Complexity Tracking

No constitution violations to justify.

# KotlinCompiler Architecture Audit

**Feature**: 064-vertical-slice-refactor
**Date**: 2026-04-02
**Total Source Files**: 38 (27 commonMain/generator + 8 commonMain/templates + 1 commonMain/validator + 2 jvmMain)

## Responsibility Buckets

Six buckets classify every kotlinCompiler source file by its primary user-facing workflow:

| Bucket | Target Module | Scope |
|--------|--------------|-------|
| **compose** | flowGraph-compose | Building a flow graph interactively: adding nodes from the palette, connecting ports, validating connections (port type checking, cycle detection), configuring node properties. Owns graph mutation logic — the path from user gesture to valid FlowGraph. |
| **persist** | flowGraph-persist | Saving and loading flow graphs: serialization to `.flow.kts` DSL, deserialization back to in-memory FlowGraph, filesystem I/O, reconciling deserialized state with editor state. Owns the round-trip workflow between memory and disk. |
| **execute** | flowGraph-execute | Running a flow graph and observing results: dynamic runtime pipeline, coroutine channel orchestration, execution control (start/stop/pause/resume/step), data flow animation state, runtime preview. Owns everything from "press Play" to "see results." |
| **generate** | flowGraph-generate | Producing deployable code from a graph: module save workflow, CodeNode definition codegen, runtime file generation, build configuration output. Owns the path from FlowGraph to generated source files on disk. |
| **inspect** | flowGraph-inspect | Understanding available components: node palette, IP type registry, filesystem scanner for node discovery, CodeNode source text editor, IP type file generation, debuggable data preview. Owns discovery and examination of what's available to compose with. |
| **root** | graphEditor (stays) | Composition root: Compose UI composables that render the editor, ViewModels that wire slices together, DI/wiring, application entry point. No business logic — only presentation and orchestration. |

## Assignment Methodology

Each file is assigned to exactly one bucket using a multi-signal approach, evaluated in priority order:

1. **Primary type operated on**: What domain object does this file's core logic manipulate? FlowGraph structure → compose. File I/O → persist. Coroutine channels/execution state → execute. Generated source code → generate. Node definitions/registry → inspect.

2. **Import analysis**: The set of imports reveals domain affinity:
   - `io.codenode.fbpdsl.serialization.*` → persist
   - `io.codenode.fbpdsl.runtime.*` → execute
   - `io.codenode.kotlincompiler.*` → generate
   - Registry/discovery types → inspect
   - `io.codenode.fbpdsl.model.FlowGraph`, `Node`, `Connection` (mutations) → compose

3. **Cross-reference density**: If a file has cross-references to multiple buckets, it belongs to the bucket with the highest affinity (most references). Ties are broken by which workflow the file is *called from* most often.

## File Audit

### generator/ (27 files)

| File | Bucket | Primary Responsibility | Cross-Module Dependencies |
|------|--------|----------------------|--------------------------|
| generator/BuildScriptGenerator.kt | generate | Generates build.gradle.kts files for KMP projects with dependency configuration | fbpDsl (FlowGraph model) |
| generator/ComponentGenerator.kt | generate | Generates Kotlin component classes with input/output channels and process functions | fbpDsl (CodeNode, CodeNodeType, Port); KotlinPoet; NodeTemplateRegistry |
| generator/ConfigAwareGenerator.kt | generate | Adds configuration properties to generated node components | fbpDsl (CodeNode); KotlinPoet |
| generator/ConnectionWiringResolver.kt | generate | Resolves FlowGraph connections to channel property assignments (WiringStatement data class) | fbpDsl (CodeNode, FlowGraph) |
| generator/EntityCUDCodeNodeGenerator.kt | generate | Generates {Entity}CUDCodeNode source node (3 outputs: save/update/remove) implementing CodeNodeDefinition | fbpDsl (CodeNodeFactory, CodeNodeDefinition, NodeRuntime, PortSpec, ProcessResult3) |
| generator/EntityDisplayCodeNodeGenerator.kt | generate | Generates {Entity}DisplayCodeNode sink node (2 string inputs) implementing CodeNodeDefinition | fbpDsl (CodeNodeFactory, CodeNodeDefinition, NodeRuntime, PortSpec) |
| generator/EntityFlowGraphBuilder.kt | generate | Builds FlowGraph programmatically for entity CRUD modules (CUD → Repository → Display pattern) | fbpDsl (flowGraph DSL, withType DSL, FlowGraph) |
| generator/EntityModuleGenerator.kt | generate | Orchestrates full entity module generation coordinating all sub-generators | Delegates to EntityFlowGraphBuilder, FlowKtGenerator, EntityCUDCodeNodeGenerator, EntityRepositoryCodeNodeGenerator, EntityDisplayCodeNodeGenerator, EntityPersistenceGenerator, EntityUIGenerator, RuntimeViewModelGenerator, RuntimeFlowGenerator, RuntimeControllerGenerator |
| generator/EntityModuleSpec.kt | generate | Data class specification for entity module generation with naming variants and properties | None |
| generator/EntityPersistenceGenerator.kt | generate | Generates {Entity}sPersistence.kt Koin module for DI of Repository and DAO | None (Koin DI framework) |
| generator/EntityRepositoryCodeNodeGenerator.kt | generate | Generates {Entity}RepositoryCodeNode transformer node implementing CodeNodeDefinition | fbpDsl (CodeNodeFactory, CodeNodeDefinition, NodeRuntime, PortSpec) |
| generator/EntityUIGenerator.kt | generate | Generates three UI Compose files (list view, add/update dialog, row display) for entity modules | Compose (Foundation, Material3) |
| generator/FlowGenerator.kt | generate | Generates channel wiring and flow orchestrator classes for coroutine-based execution | fbpDsl (CodeNode, Connection, FlowGraph); KotlinPoet |
| generator/FlowGraphFactoryGenerator.kt | generate | Generates factory functions for FlowGraph instantiation with tick function references | fbpDsl (CodeNode, FlowGraph, Connection) |
| generator/FlowKtGenerator.kt | generate | Generates .flow.kt compiled Kotlin files serializing FlowGraph using FBP DSL syntax | fbpDsl (FlowGraph, CodeNode, Connection, Port) |
| generator/GenericNodeGenerator.kt | generate | Generates Kotlin components from generic nodes with configurable inputs/outputs | fbpDsl (CodeNode, Port); KotlinPoet |
| generator/KotlinCodeGenerator.kt | generate | Orchestrates complete KMP code generation pipeline from FlowGraph | fbpDsl (FlowGraph, CodeNode, Connection); delegates to ComponentGenerator, FlowGenerator |
| generator/ModuleGenerator.kt | generate | Orchestrates complete KMP module generation with directory structure and build config | fbpDsl (FlowGraph, CodeNode); KotlinReflect |
| generator/ObservableStateResolver.kt | generate | Extracts observable state properties (StateFlow-backed) from boundary ports | fbpDsl (FlowGraph, CodeNode, Port) |
| generator/RepositoryCodeGenerator.kt | generate | Generates Room persistence layer code (Entity, DAO, Repository, BaseDao, Database) | None (defines EntityProperty, EntityInfo data classes) |
| generator/RuntimeControllerAdapterGenerator.kt | generate | Generates {Name}ControllerAdapter wrapping the Controller implementation | fbpDsl (FlowGraph); uses ObservableStateResolver |
| generator/RuntimeControllerGenerator.kt | generate | Generates {Name}Controller class with execution control and observable state exposure | fbpDsl (CodeNode, FlowGraph); uses ObservableStateResolver |
| generator/RuntimeControllerInterfaceGenerator.kt | generate | Generates {Name}ControllerInterface declaring control methods and StateFlow properties | fbpDsl (FlowGraph); uses ObservableStateResolver |
| generator/RuntimeFlowGenerator.kt | generate | Generates {Name}Flow.kt with runtime instantiation via CodeNodeFactory or CodeNodeDefinition | fbpDsl (CodeNode, FlowGraph); uses ObservableStateResolver, ConnectionWiringResolver, RuntimeTypeResolver |
| generator/RuntimeTypeResolver.kt | generate | Maps node port counts to CodeNodeFactory method names and runtime class types | fbpDsl (CodeNode) |
| generator/RuntimeViewModelGenerator.kt | generate | Generates {Name}ViewModel.kt stub with module-level State object using marker-delineated sections | fbpDsl (CodeNode, FlowGraph); uses ObservableStateResolver |
| generator/UserInterfaceStubGenerator.kt | generate | Generates Composable stub files for userInterface/ directories (write-once pattern) | fbpDsl (FlowGraph) |

### templates/ (8 files)

| File | Bucket | Primary Responsibility | Cross-Module Dependencies |
|------|--------|----------------------|--------------------------|
| templates/NodeTemplate.kt | generate | Interface contract and registry for node-type-specific code generation templates | fbpDsl (CodeNode, CodeNodeType); KotlinPoet (TypeSpec, ClassName) |
| templates/SourceTemplate.kt | generate | Generates Kotlin class code for SOURCE nodes (MutableSharedFlow, coroutine loop) | fbpDsl (CodeNode, CodeNodeType); KotlinPoet; ConfigAwareGenerator |
| templates/FilterTemplate.kt | generate | Generates Kotlin class code for FILTER nodes (input/output flows, predicate logic) | fbpDsl (CodeNode, CodeNodeType); KotlinPoet; ConfigAwareGenerator |
| templates/MergerTemplate.kt | generate | Generates Kotlin class code for MERGER nodes (multiple inputs merged to single output) | fbpDsl (CodeNode, CodeNodeType); KotlinPoet; ConfigAwareGenerator |
| templates/SinkTemplate.kt | generate | Generates Kotlin class code for SINK nodes (consume-only endpoints) | fbpDsl (CodeNode, CodeNodeType); KotlinPoet; ConfigAwareGenerator |
| templates/SplitterTemplate.kt | generate | Generates Kotlin class code for SPLITTER nodes (single input routed to multiple outputs) | fbpDsl (CodeNode, CodeNodeType); KotlinPoet; ConfigAwareGenerator |
| templates/TransformerTemplate.kt | generate | Generates Kotlin class code for TRANSFORMER nodes (transform input to output) | fbpDsl (CodeNode, CodeNodeType); KotlinPoet; ConfigAwareGenerator |
| templates/ValidatorTemplate.kt | generate | Generates Kotlin class code for VALIDATOR nodes (route to valid/invalid outputs) | fbpDsl (CodeNode, CodeNodeType); KotlinPoet; ConfigAwareGenerator |

### validator/ (1 file)

| File | Bucket | Primary Responsibility | Cross-Module Dependencies |
|------|--------|----------------------|--------------------------|
| validator/LicenseValidator.kt | generate | Validates generated code and dependencies against GPL/LGPL/AGPL license restrictions | None (validation logic only) |

### jvmMain/ (2 files)

| File | Bucket | Primary Responsibility | Cross-Module Dependencies |
|------|--------|----------------------|--------------------------|
| tools/RegenerateStopWatch.kt | generate | JVM tool that regenerates StopWatch module files by creating FlowGraph and invoking ModuleGenerator | fbpDsl (FlowGraph, CodeNode, Port, CodeNodeType); ModuleGenerator |
| GenerateGeoLocationModule.kt | generate | JVM tool that generates complete GeoLocation entity module by invoking EntityModuleGenerator | EntityModuleGenerator, EntityModuleSpec, ModuleGenerator |

## Seam Matrix

### Internal Seams (within kotlinCompiler)

All files are in the **generate** bucket, so all internal dependencies are intra-bucket. Key delegation patterns:

| Source File | Target File | Type | Notes |
|------------|------------|------|-------|
| KotlinCodeGenerator.kt | ComponentGenerator.kt | Function call | Delegates component generation |
| KotlinCodeGenerator.kt | FlowGenerator.kt | Function call | Delegates flow orchestrator generation |
| ComponentGenerator.kt | NodeTemplate.kt | Function call | Uses NodeTemplateRegistry for type-specific generation |
| EntityModuleGenerator.kt | EntityFlowGraphBuilder.kt | Function call | Builds FlowGraph for entity module |
| EntityModuleGenerator.kt | FlowKtGenerator.kt | Function call | Generates .flow.kt for entity module |
| EntityModuleGenerator.kt | EntityCUDCodeNodeGenerator.kt | Function call | Generates CUD node source |
| EntityModuleGenerator.kt | EntityRepositoryCodeNodeGenerator.kt | Function call | Generates repository node source |
| EntityModuleGenerator.kt | EntityDisplayCodeNodeGenerator.kt | Function call | Generates display node source |
| EntityModuleGenerator.kt | EntityPersistenceGenerator.kt | Function call | Generates Koin persistence module |
| EntityModuleGenerator.kt | EntityUIGenerator.kt | Function call | Generates Compose UI files |
| EntityModuleGenerator.kt | RuntimeViewModelGenerator.kt | Function call | Generates ViewModel stub |
| EntityModuleGenerator.kt | RuntimeFlowGenerator.kt | Function call | Generates runtime flow |
| EntityModuleGenerator.kt | RuntimeControllerGenerator.kt | Function call | Generates controller |
| RuntimeFlowGenerator.kt | ObservableStateResolver.kt | Function call | Resolves observable state ports |
| RuntimeFlowGenerator.kt | ConnectionWiringResolver.kt | Function call | Resolves channel assignments |
| RuntimeFlowGenerator.kt | RuntimeTypeResolver.kt | Function call | Maps port counts to factory methods |
| RuntimeControllerGenerator.kt | ObservableStateResolver.kt | Function call | Resolves observable state ports |
| RuntimeControllerAdapterGenerator.kt | ObservableStateResolver.kt | Function call | Resolves observable state ports |
| RuntimeControllerInterfaceGenerator.kt | ObservableStateResolver.kt | Function call | Resolves observable state ports |
| RuntimeViewModelGenerator.kt | ObservableStateResolver.kt | Function call | Resolves observable state ports |
| All SourceTemplate, FilterTemplate, etc. | ConfigAwareGenerator.kt | Function call | Adds config properties to generated classes |
| RegenerateStopWatch.kt | ModuleGenerator.kt | Function call | Invokes full module generation |
| GenerateGeoLocationModule.kt | EntityModuleGenerator.kt | Function call | Invokes entity module generation |

### Cross-Module Seams

| Source File | Target Module | Type | Dependency |
|------------|--------------|------|-----------|
| Nearly all generator files | fbpDsl | Type reference | FlowGraph, CodeNode, Port, Connection, CodeNodeType, InformationPacketType |
| Entity*CodeNodeGenerator files | fbpDsl | Type reference | CodeNodeFactory, CodeNodeDefinition, NodeRuntime, PortSpec, ProcessResult* |
| ComponentGenerator.kt | fbpDsl | Type reference | Uses NodeTemplateRegistry (from templates/) |
| FlowKtGenerator.kt | fbpDsl | Type reference | FlowGraph DSL builder types |

**Note**: kotlinCompiler has **no inbound dependencies from graphEditor or circuitSimulator**. It is consumed by graphEditor (via CompilationService/ModuleSaveService) but does not import from graphEditor. This confirms kotlinCompiler maps cleanly to flowGraph-generate as a provider module.

### Seam Count by Boundary

| Boundary | Count | Notes |
|----------|-------|-------|
| generate→fbpDsl (type reference) | 36 | Nearly every file imports fbpDsl model types — these are shared vocabulary, not extraction seams |
| (internal generate→generate) | 23 | Internal delegation chain; stays within flowGraph-generate |
| **Total cross-module** | **0** | No cross-module seams to graphEditor or circuitSimulator |

**Inbound seams** (from graphEditor into kotlinCompiler — documented in graphEditor/ARCHITECTURE.md):
- graphEditor/save/ModuleSaveService.kt → kotlinCompiler generators (generate→generate, stays within flowGraph-generate)
- graphEditor/compilation/CompilationService.kt → kotlinCompiler validators (generate→generate, stays within flowGraph-generate)

## Summary

### Files Per Bucket

| Bucket | Count | Percentage |
|--------|-------|-----------|
| **generate** | 38 | 100% |
| **Total** | **38** | **100%** |

### Key Observations

1. **kotlinCompiler is a pure generate module** — Every single file maps to the generate bucket. This confirms the plan's assessment that kotlinCompiler dissolves entirely into flowGraph-generate.

2. **No cross-bucket complexity** — Unlike graphEditor (which spans all six buckets), kotlinCompiler is monolithic in its responsibility. Extraction to flowGraph-generate is a clean module move, not a decomposition.

3. **Entity generation is the largest sub-concern** — 12 of 38 files (32%) are entity-module-specific generators. This aligns with research.md's identification of "Entity scaffolding (~2000 lines)" as a future extraction candidate within generate.

4. **fbpDsl is the only external dependency** — All files import fbpDsl model types (FlowGraph, CodeNode, Port, etc.) as shared vocabulary. No imports from graphEditor or circuitSimulator.

5. **Two JVM-specific tool files** — RegenerateStopWatch.kt and GenerateGeoLocationModule.kt are CLI-invocable tools, not library code. They orchestrate generators and could become integration tests or standalone utilities.

6. **ObservableStateResolver is a high-traffic internal hub** — Referenced by 5 generators (RuntimeFlow, RuntimeController, RuntimeControllerAdapter, RuntimeControllerInterface, RuntimeViewModel). A candidate for interface extraction if the runtime generation sub-concern grows.

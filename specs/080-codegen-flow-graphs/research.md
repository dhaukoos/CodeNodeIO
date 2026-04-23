# Research: Code Generation Flow Graphs

**Feature**: 080-codegen-flow-graphs
**Date**: 2026-04-23

## R1: Flow Graph Structure

**Decision**: Each generation flow graph uses the standard .flow.kt DSL. A Source node provides the input spec (GenerationConfig for module-level, EntityModuleSpec for entity, UIFBPSpec for UI-FBP). The Generator CodeNodes fan out from the source, each receiving the spec and producing a file content string. A Sink node collects all outputs into a GenerationResult.

**Pattern**:
```
Source(config) ──→ FlowKtGeneratorNode ──→ Sink(results)
               ├→ RuntimeFlowGeneratorNode ──→
               ├→ RuntimeControllerGeneratorNode ──→
               ├→ RuntimeControllerInterfaceGeneratorNode ──→
               ├→ RuntimeControllerAdapterGeneratorNode ──→
               ├→ RuntimeViewModelGeneratorNode ──→
               └→ UserInterfaceStubGeneratorNode ──→
```

**Rationale**: Fan-out from a single source mirrors the current `saveModule()` pattern where one FlowGraph input drives all generators. The sink collects results for the runner to return.

## R2: CodeGenerationRunner Design

**Decision**: The runner uses the existing FBP runtime (DynamicPipelineController) to execute the generation flow graph. The Source node emits the GenerationConfig, the Generator CodeNodes run as TRANSFORMER nodes in the pipeline (with parallelism from the fan-out topology), and the Sink node collects the generated file contents into a GenerationResult.

**Key insight**: Eating our own dogfood — the tool's own code generation uses the same FBP runtime it provides to users. The fan-out topology naturally parallelizes independent generators (e.g., FlowKtGenerator and RuntimeControllerGenerator can execute concurrently on separate coroutines), demonstrating the performance benefit of the FBP approach.

**Rationale**: The fbpDsl runtime abstracts away the complexity of channels, coroutines, and pipeline orchestration — that is the product's core value proposition. Using a custom synchronous runner would contradict this and miss the opportunity to validate the FBP runtime with a real internal use case. The existing DynamicPipelineController already handles flow graph execution, node instantiation, and channel wiring.

**Alternatives considered**:
- Custom synchronous runner: Rejected — contradicts eat-our-own-dogfood principle, imposes control-flow bias on a naturally parallel fan-out topology, and misses the opportunity to validate the FBP runtime.
- Direct method calls (current approach): Being replaced — the flow graph adds configurability, visual editing, and parallel execution.

## R3: SelectionFilter Mapping

**Decision**: The `SelectionFilter` maps `generatorId` strings (from `FileNode.generatorId` in `GenerationFileTree`) to enabled/disabled state. The runner checks each Generator CodeNode's name against the filter before execution.

```
data class SelectionFilter(
    val excludedGeneratorIds: Set<String> = emptySet()
)
```

**Mapping from file tree**: When a `FileNode` is deselected (`isSelected = false`), its `generatorId` is added to the `excludedGeneratorIds` set. When a folder is deselected, all its children's `generatorId` values are added.

## R4: GenerationResult Design

**Decision**: The result is a simple map with error tracking.

```
data class GenerationResult(
    val generatedFiles: Map<String, String>,  // generatorId → content
    val errors: Map<String, String> = emptyMap(),  // generatorId → error message
    val skipped: Set<String> = emptySet()  // generatorIds excluded by filter
)
```

## R5: Flow Graph File Placement

**Decision**: The flow graphs live in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/flow/` following the eat-our-own-dogfood principle and the folder hierarchy from feature 077.

**Files**:
- `flowGraph-generate/.../flow/GenerateModule.flow.kt`
- `flowGraph-generate/.../flow/GenerateRepository.flow.kt`
- `flowGraph-generate/.../flow/GenerateUIFBP.flow.kt`

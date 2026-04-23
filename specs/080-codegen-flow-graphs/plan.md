# Implementation Plan: Code Generation Flow Graphs

**Branch**: `080-codegen-flow-graphs` | **Date**: 2026-04-23 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/080-codegen-flow-graphs/spec.md`

## Summary

Create 3 generation flow graphs expressing code generation as compositions of Generator CodeNodes, plus a CodeGenerationRunner that executes them via the FBP runtime (DynamicPipelineController) with selective generation support and natural parallelism from the fan-out topology.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: flowGraph-generate (Generator CodeNode wrappers, GenerationConfig), fbpDsl (FlowGraph DSL for .flow.kt files)
**Storage**: N/A — in-memory content production
**Testing**: `./gradlew :flowGraph-generate:jvmTest`
**Constraints**: Runner produces content only — no file writing. Flow graphs are declarative manifests, not runtime pipelines.
**Scale/Scope**: 3 flow graph files, 1 runner class, 1 selection filter, ~15 tests

## Constitution Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Flow graphs are declarative. Runner is simple orchestrator. |
| II. Test-Driven Development | PASS | Runner verified by comparison to ModuleSaveService output. |
| III. User Experience Consistency | PASS | Flow graphs viewable in existing graph editor. |
| IV. Performance Requirements | PASS | Under 5 seconds for any path. |
| V. Observability & Debugging | PASS | GenerationResult tracks successes, errors, and skipped. |
| Licensing & IP | PASS | No new dependencies. |

## Project Structure

```text
flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/
├── flow/                                    # NEW — generation flow graphs
│   ├── GenerateModule.flow.kt
│   ├── GenerateRepository.flow.kt
│   └── GenerateUIFBP.flow.kt
├── runner/                                  # NEW — execution engine
│   ├── CodeGenerationRunner.kt
│   ├── GenerationResult.kt
│   └── SelectionFilter.kt
└── nodes/                                   # EXISTING — Generator CodeNode wrappers
    └── (15 wrapper objects)

flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/
└── runner/
    └── CodeGenerationRunnerTest.kt          # NEW — runner tests
```

## CodeGenerationRunner Design

The runner uses the existing FBP runtime (DynamicPipelineController) to execute generation flow graphs — eating our own dogfood.

```kotlin
class CodeGenerationRunner {
    fun execute(
        generationPath: GenerationPath,
        config: GenerationConfig,
        selectionFilter: SelectionFilter = SelectionFilter()
    ): GenerationResult
}
```

### Execution Flow

1. Load the generation flow graph for the given path
2. Remove excluded Generator CodeNodes based on selectionFilter
3. Build a DynamicPipelineController from the filtered flow graph
4. Emit the GenerationConfig through the Source node
5. The fan-out topology routes the config to all Generator CodeNodes in parallel
6. Each Generator CodeNode transforms the config into generated file content
7. The Sink node collects all outputs into a GenerationResult
8. Return the result (generated files + errors + skipped)

### Why FBP Runtime?

- **Eat our own dogfood**: The tool's code generation uses the same runtime it provides to users
- **Natural parallelism**: The fan-out topology runs independent generators concurrently — 7 generators that each take ~100ms complete in ~100ms total, not 700ms
- **Complexity abstracted**: The fbpDsl runtime handles channels, coroutines, and pipeline orchestration — that abstraction is the product's core value proposition
- **Validation**: Running the FBP runtime for an internal use case validates it with real workloads

## Flow Graph Design

Each flow graph uses the standard .flow.kt DSL:

```kotlin
val generateModuleFlowGraph = flowGraph("GenerateModule", version = "1.0.0") {
    val source = codeNode("ConfigSource", nodeType = "SOURCE") {
        output("config", Any::class)
    }
    val flowKtGen = codeNode("FlowKtGenerator", nodeType = "TRANSFORMER") { ... }
    val runtimeFlowGen = codeNode("RuntimeFlowGenerator", nodeType = "TRANSFORMER") { ... }
    // ... 5 more generators
    val sink = codeNode("ResultCollector", nodeType = "SINK") { ... }

    source.output("config") connect flowKtGen.input("config")
    // ... fan-out connections
}
```

The flow graphs are primarily for **visual representation** in the graph editor — showing the generation pipeline. The actual execution uses the runner's direct invocation approach.

## SelectionFilter Design

```kotlin
data class SelectionFilter(
    val excludedGeneratorIds: Set<String> = emptySet()
) {
    fun isIncluded(generatorId: String): Boolean = generatorId !in excludedGeneratorIds

    companion object {
        fun fromFileTree(fileTree: GenerationFileTree): SelectionFilter {
            val excluded = fileTree.folders.flatMap { folder ->
                folder.files.filter { !it.isSelected }.map { it.generatorId }
            }.toSet()
            return SelectionFilter(excludedGeneratorIds = excluded)
        }
    }
}
```

## Complexity Tracking

No constitution violations. No complexity justification needed.

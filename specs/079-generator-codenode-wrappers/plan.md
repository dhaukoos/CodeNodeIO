# Implementation Plan: Generator CodeNode Wrappers

**Branch**: `079-generator-codenode-wrappers` | **Date**: 2026-04-23 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/079-generator-codenode-wrappers/spec.md`

## Summary

Wrap all 15 generators from the dependency analysis as CodeNode definitions with typed input/output ports, enabling them to participate in flow graphs. Each wrapper is a thin `object : CodeNodeDefinition` delegate using TRANSFORMER category. Wrappers are discoverable via the node palette.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: fbpDsl (CodeNodeDefinition, CodeNodeType, PortSpec, NodeRuntime), flowGraph-generate (existing generators)
**Storage**: N/A — pure code, no persistence
**Testing**: `./gradlew :flowGraph-generate:jvmTest`
**Target Platform**: KMP (commonMain)
**Constraints**: Existing generators remain unchanged. Wrappers are purely additive.
**Scale/Scope**: 15 new wrapper objects, 1 new subdirectory, ~15 unit tests

## Constitution Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Thin wrappers follow single-responsibility. Consistent naming convention. |
| II. Test-Driven Development | PASS | Each wrapper verified by unit test comparing output to direct generator call. |
| III. User Experience Consistency | PASS | Wrappers appear in palette like any other CodeNode. |
| IV. Performance Requirements | N/A | Code generation, not runtime. |
| V. Observability & Debugging | PASS | No behavioral change. |
| Licensing & IP | PASS | No new dependencies. |

## Project Structure

```text
flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/
├── nodes/                                         # NEW directory
│   ├── FlowKtGeneratorNode.kt                      # NEW — 7 module-level wrappers
│   ├── RuntimeFlowGeneratorNode.kt
│   ├── RuntimeControllerGeneratorNode.kt
│   ├── RuntimeControllerInterfaceGeneratorNode.kt
│   ├── RuntimeControllerAdapterGeneratorNode.kt
│   ├── RuntimeViewModelGeneratorNode.kt
│   ├── UserInterfaceStubGeneratorNode.kt
│   ├── EntityCUDGeneratorNode.kt                    # NEW — 4 entity wrappers
│   ├── EntityRepositoryGeneratorNode.kt
│   ├── EntityDisplayGeneratorNode.kt
│   ├── EntityPersistenceGeneratorNode.kt
│   ├── UIFBPStateGeneratorNode.kt                   # NEW — 4 UI-FBP wrappers
│   ├── UIFBPViewModelGeneratorNode.kt
│   ├── UIFBPSourceGeneratorNode.kt
│   └── UIFBPSinkGeneratorNode.kt
└── generator/                                       # UNCHANGED — existing generators

flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/
└── nodes/
    └── GeneratorCodeNodeTest.kt                     # NEW — unit tests for all wrappers
```

## Wrapper Pattern

Each wrapper follows this template:

```kotlin
object FlowKtGeneratorNode : CodeNodeDefinition {
    override val name = "FlowKtGenerator"
    override val category = CodeNodeType.TRANSFORMER
    override val description = "Generates .flow.kt file from FlowGraph"
    override val inputPorts = listOf(PortSpec("flowGraph", Any::class))
    override val outputPorts = listOf(PortSpec("content", String::class))

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createContinuousTransformer<Any, String>(
            name = name,
            transform = { input ->
                val flowGraph = input as FlowGraph
                FlowKtGenerator().generateFlowKt(flowGraph, ...)
            }
        )
    }
}
```

### Input Port Types by Generator Group

| Group | Input Port Name | Input Type (cast from Any) |
|-------|----------------|--------------------------|
| Module-level (7) | "flowGraph" | FlowGraph |
| Entity (4) | "entitySpec" | EntityModuleSpec |
| UI-FBP (4) | "uiFBPSpec" | UIFBPSpec |

### Output Port

All wrappers: `PortSpec("content", String::class)` — the generated file content.

## Complexity Tracking

No constitution violations. No complexity justification needed.

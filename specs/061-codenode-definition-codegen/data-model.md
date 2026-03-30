# Data Model: Generate CodeNodeDefinition-Based Repository Modules

**Feature**: 061-codenode-definition-codegen
**Date**: 2026-03-30

## Entities

### Generated Node Files (per entity module)

| File | Pattern | Category | Ports |
|------|---------|----------|-------|
| `{Entity}CUDCodeNode.kt` | CodeNodeDefinition (SOURCE) | SOURCE | 0 in, 3 out (save, update, remove) — all IP type |
| `{Entity}RepositoryCodeNode.kt` | CodeNodeDefinition (TRANSFORMER) | TRANSFORMER | 3 in (save, update, remove) — IP type; 2 out (result, error) — String |
| `{PluralName}DisplayCodeNode.kt` | CodeNodeDefinition (SINK) | SINK | 2 in (result, error) — String; 0 out |

### EntityFlowGraphBuilder Node Configuration

| Config Key | CUD Node | Repository Node | Display Node |
|-----------|----------|-----------------|--------------|
| `_codeNodeClass` | `io.codenode.{module}.nodes.{Entity}CUDCodeNode` | `io.codenode.{module}.nodes.{Entity}RepositoryCodeNode` | `io.codenode.{module}.nodes.{PluralName}DisplayCodeNode` |
| `_genericType` | `in0out3` | `in3anyout2` | `in2out0` |
| `_cudSource` | `"true"` | — | — |
| `_repository` | — | `"true"` | — |
| `_display` | — | — | `"true"` |

Note: `_codeNodeDefinition` is no longer used — all nodes are CodeNodeDefinition by default. Legacy tick-function support has been removed.

### Generator Classes (new)

| Generator | Replaces | Output |
|-----------|----------|--------|
| `EntityCUDCodeNodeGenerator` | `EntityCUDGenerator` | `{Entity}CUDCodeNode.kt` — object implementing CodeNodeDefinition |
| `EntityRepositoryCodeNodeGenerator` | (tick function pattern) | `{Entity}RepositoryCodeNode.kt` — object implementing CodeNodeDefinition |
| `EntityDisplayCodeNodeGenerator` | `EntityDisplayGenerator` | `{PluralName}DisplayCodeNode.kt` — object implementing CodeNodeDefinition |

## Relationships

```text
EntityModuleGenerator (orchestrator)
    ├── EntityCUDCodeNodeGenerator.generate(spec) → {Entity}CUDCodeNode.kt
    ├── EntityRepositoryCodeNodeGenerator.generate(spec) → {Entity}RepositoryCodeNode.kt
    ├── EntityDisplayCodeNodeGenerator.generate(spec) → {PluralName}DisplayCodeNode.kt
    ├── EntityFlowGraphBuilder.buildFlowGraph(spec) → FlowGraph with _codeNodeClass configs
    ├── FlowKtGenerator.generateFlowKt(flowGraph) → {PluralName}.flow.kt
    ├── RuntimeFlowGenerator.generate(flowGraph) → {PluralName}Flow.kt (no tick functions)
    └── (existing generators: ViewModel, Persistence, UI, etc.)

RuntimeFlowGenerator
    ├── Detects _codeNodeClass on all nodes → codeNodeClassNodes path
    ├── Imports: import io.codenode.{module}.nodes.{NodeName}CodeNode
    ├── Runtime: val xxx = {NodeName}CodeNode.createRuntime("{NodeName}")
    └── No tick function imports or references
```

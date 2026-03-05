# Quickstart: Modify Source and Sink Nodes

**Feature Branch**: `037-source-sink-refactor`
**Date**: 2026-03-04

## Overview

This feature renames "generator" nodes to "source" nodes, changes source nodes from autonomous tick-based data producers to ViewModel-driven data entry points, removes processing logic stubs for both source and sink nodes, and renames multi-input/output runtime classes to follow a consistent `{Role}{Direction}{Count}` naming convention.

## Key Concepts

### Source Node (was Generator)
- **Before**: Autonomous producer with timed while loop calling a tick function
- **After**: Passive entry point for UI-driven data, emitted via ViewModel binding
- **No processing logic stub** — data comes from the UI layer

### Sink Node
- **Before**: Consumer with optional tick function from processing logic stub
- **After**: Pure state bridge — binds input channel to ViewModel observable state
- **No processing logic stub** — only updates `{FlowName}State` properties

### Naming Convention: `{Role}{Direction}{Count}Runtime`
```
SourceRuntime<T>          — 1-output source
SourceOut2Runtime<U, V>   — 2-output source
SourceOut3Runtime<U, V, W> — 3-output source
SinkRuntime<T>            — 1-input sink (unchanged)
SinkIn2Runtime<A, B>      — 2-input sink
SinkIn3Runtime<A, B, C>   — 3-input sink
SinkIn2AnyRuntime<A, B>   — 2-input any-trigger sink
SinkIn3AnyRuntime<A, B, C> — 3-input any-trigger sink
```

## Build & Test

```bash
# Run all tests (both modules affected)
./gradlew :fbpDsl:jvmTest :kotlinCompiler:jvmTest :graphEditor:jvmTest

# Compile graphEditor (verifies all references resolve)
./gradlew :graphEditor:compileKotlinJvm

# Full build
./gradlew build
```

## Development Sequence

1. **Rename enum** (`GENERATOR` → `SOURCE` in `CodeNodeType`)
2. **Rename runtime class files** (7 files renamed)
3. **Rename type aliases** in `ContinuousTypes.kt`
4. **Rename factory methods** in `CodeNodeFactory.kt` (remove timed variants)
5. **Update code generators** (`RuntimeTypeResolver`, `RuntimeFlowGenerator`, `ProcessingLogicStubGenerator`)
6. **Update graph editor** (`NodeRenderer`, `DragAndDropHandler`, `FlowGraphSerializer`)
7. **Update all tests** (30+ test files)
8. **Update existing modules** (`StopWatch`, `StopWatchV2`, `UserProfiles`, `RepositoryPattern`)

## Migration Notes

- Existing `.flow.kt` files contain `nodeType = "GENERATOR"` — the deserializer must handle both `"GENERATOR"` (legacy) and `"SOURCE"` (new) values for backward compatibility
- Existing processing logic stubs for generator/sink nodes in generated modules become orphaned — they should be deleted or will be cleaned up on next module re-save
- The `StopWatchOriginal` module contains hand-written components that reference the old names — these need manual updates

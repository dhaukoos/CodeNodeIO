# CodeNodeIO Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-01-13

## Active Technologies
- Kotlin 2.1.21 (existing project configuration) (002-generic-nodetype)
- Kotlin 2.1.21 (KMP), Compose Desktop 1.7.3 + Compose Desktop (UI), kotlinx-coroutines, kotlinx-serialization (005-graphnode-creation)
- .flow.kts files (DSL serialization format) (005-graphnode-creation)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3, kotlinx-serialization, kotlinx-coroutines (006-passthru-port-segments)
- .flow.kts DSL files (text-based serialization) (006-passthru-port-segments)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + kotlinx-serialization, kotlinx-coroutines, Compose Desktop 1.7.3 (graphEditor) (007-node-execution-control)
- .flow.kts files (text-based DSL serialization) (007-node-execution-control)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, Compose Multiplatform 1.7.3, KotlinPoe (008-stopwatch-virtual-circuit)
- .flow.kts files (DSL serialization for FlowGraph persistence) (008-stopwatch-virtual-circuit)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3, kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0 (011-stopwatch-refactor)
- N/A (in-memory FlowGraph models) (011-stopwatch-refactor)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, KotlinPoet (code generation) (012-channel-connections)
- N/A (in-memory FlowGraph models, generated code) (012-channel-connections)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3, kotlinx-serialization 1.6.0 (016-node-generator)
- JSON file via kotlinx-serialization for CustomNodeRepository persistence (016-node-generator)
- Kotlin 2.1.21 (Kotlin Multiplatform) + Compose Desktop 1.7.3, kotlinx-coroutines 1.8.0 (017-viewmodel-pattern)
- FileCustomNodeRepository (JSON persistence for custom nodes) (017-viewmodel-pattern)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Multiplatform 1.7.3, kotlinx-coroutines 1.8.0, JetBrains lifecycle-viewmodel-compose 2.8.0 (018-stopwatch-viewmodel)
- N/A (in-memory FlowGraph state) (018-stopwatch-viewmodel)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Multiplatform 1.7.3, kotlinx-coroutines 1.8.0, lifecycle-viewmodel-compose 2.8.0 (019-flowgraph-execution-control)
- Kotlin 2.1.21 (Kotlin Multiplatform) + kotlinx-coroutines 1.8.0, Compose Multiplatform 1.7.3, lifecycle-viewmodel-compose 2.8.0 (020-refactor-timer-emitter)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + kotlinx-coroutines 1.8.0 (channels, coroutine scopes) (021-refactor-noderuntime-base)
- N/A (in-memory runtime state) (021-refactor-noderuntime-base)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Multiplatform 1.7.3, kotlinx-coroutines 1.8.0, lifecycle-viewmodel-compose 2.8.0, lifecycle-runtime-compose 2.8.0 (022-generate-flowgraph-viewmodel)
- N/A (in-memory FlowGraph models, generated source code) (022-generate-flowgraph-viewmodel)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3 + kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0 (023-port-type-selector)
- .flow.kts files (DSL text-based serialization via FlowGraphSerializer) (023-port-type-selector)
- N/A (code deletion only) (024-remove-single-invocation)
- N/A (in-memory factory methods only) (025-timed-factory-methods)
- N/A (in-memory models, generated source files) (026-processing-logic-stubs)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, Compose Multiplatform 1.7.3, lifecycle-viewmodel-compose 2.8.0 (027-generate-runtime-files)
- N/A (generates source code files to filesystem) (027-generate-runtime-files)

- Kotlin 1.9+, Go 1.21+ (001-ide-plugin-platform)

## Project Structure

```text
src/
tests/
```

## Commands

# Add commands for Kotlin 1.9+, Go 1.21+

## Code Style

Kotlin 1.9+, Go 1.21+: Follow standard conventions

## Recent Changes
- 027-generate-runtime-files: Added Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, Compose Multiplatform 1.7.3, lifecycle-viewmodel-compose 2.8.0
- 026-processing-logic-stubs: Added Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, KotlinPoet (code generation)
- 025-timed-factory-methods: Added Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0


<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->

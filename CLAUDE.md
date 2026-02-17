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
- 018-stopwatch-viewmodel: Added Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Multiplatform 1.7.3, kotlinx-coroutines 1.8.0, JetBrains lifecycle-viewmodel-compose 2.8.0
- 017-viewmodel-pattern: Added Kotlin 2.1.21 (Kotlin Multiplatform) + Compose Desktop 1.7.3, kotlinx-coroutines 1.8.0
- 016-node-generator: Added Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3, kotlinx-serialization 1.6.0


<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->

# Research: Self-Contained CodeNode Definition

**Feature**: 050-self-contained-codenode | **Date**: 2026-03-13

## R1: Self-Contained Node Class Structure

**Decision**: A self-contained node is a Kotlin object (singleton) implementing a `CodeNodeDefinition` interface. The object bundles metadata (name, category, ports) and a processing logic lambda in a single file.

**Rationale**: An `object` declaration (vs. `class`) is the right choice because:
- Node definitions are stateless singletons — there's one "SepiaTransformer" definition, not multiple
- Objects are easily discoverable via classpath scanning or explicit registration
- Objects can hold `val` properties for metadata and lambdas for processing logic
- They don't need constructors, making the generated code simpler

**Alternatives considered**:
- `class` with companion factory: More boilerplate, no benefit since definitions are stateless
- Top-level `val` (current pattern): Works for processing logic but can't bundle metadata — the root cause of the current fragmentation
- Annotation-based discovery: Would require annotation processing (KSP), adding build complexity

**Example structure**:
```kotlin
object SepiaTransformerCodeNode : CodeNodeDefinition {
    override val name = "SepiaTransformer"
    override val category = NodeCategory.TRANSFORMER
    override val inputPorts = listOf(PortSpec("image", ImageData::class))
    override val outputPorts = listOf(PortSpec("result", ImageData::class))

    override val processBlock: ContinuousTransformBlock<ImageData, ImageData> = { input ->
        // sepia transformation logic
    }
}
```

## R2: Node File Location Strategy (Three Levels)

**Decision**: Support three placement levels with a clear directory convention at each:

| Level | Path | Gradle Module | Compiled |
|-------|------|---------------|----------|
| Module | `{Module}/src/commonMain/kotlin/io/codenode/{module}/nodes/` | Existing module | Yes |
| Project | `nodes/src/commonMain/kotlin/io/codenode/nodes/` | New `nodes` module | Yes |
| Universal | `~/.codenode/nodes/` | None | No (template only) |

**Rationale**: Module and Project levels are in the Gradle source tree and get compiled normally. Universal level stores portable node templates that must be copied into a project to be executable — but the graph editor can still display them in the palette by parsing file metadata.

**Alternatives considered**:
- Single shared module only: Simpler, but doesn't allow module-scoped nodes
- Dynamic classloading for Universal: Too complex, fragile across KMP targets
- Kotlin scripting (`.kts`) for Universal: KMP scripting support is immature

## R3: Node Discovery Mechanism

**Decision**: Use a `NodeDefinitionRegistry` that discovers nodes from three sources on startup:
1. **Compiled nodes**: Classpath scanning of classes implementing `CodeNodeDefinition` — covers Module and Project levels
2. **Universal templates**: File-system scan of `~/.codenode/nodes/*.kt` — parses metadata comments for palette display only
3. **Legacy nodes**: Existing `CustomNodeDefinition` entries from `~/.codenode/custom-nodes.json` — backward compatibility

**Rationale**: Classpath scanning is the most reliable for compiled code. File parsing for Universal nodes provides palette visibility without requiring compilation. Legacy support ensures existing workflows continue.

**Alternatives considered**:
- Manual registration in Main.kt: The current approach — exactly what this feature eliminates
- Gradle build plugin generating a manifest: Works but adds build complexity and slows iteration
- ServiceLoader (META-INF/services): Standard JVM pattern, but doesn't work well with KMP commonMain

## R4: Runtime Resolution Pattern

**Decision**: Replace hardcoded processing logic references in generated flow classes with a registry lookup. When the runtime preview starts, the flow graph's node names are resolved against `NodeDefinitionRegistry` to get the appropriate processing logic and factory method.

**Rationale**: This is the minimal change that enables hot-swap. The flow class no longer needs `import sepiaTransform` — it looks up the processing block by node name at runtime.

**Alternatives considered**:
- Regenerate flow class on every graph change: Works but requires recompilation
- Reflection-based instantiation: Fragile, poor error messages
- Code generation at flow save time: The current approach — breaks on node swap

## R5: CodeNodeDefinition Interface Design

**Decision**: The interface provides both metadata for the palette AND a factory method to create the appropriate `NodeRuntime` instance. This keeps the interface simple while supporting all runtime types.

**Rationale**: Rather than exposing raw processing blocks (which vary wildly by runtime type), the node definition provides a `createRuntime(name)` method that returns the correct `NodeRuntime` subclass with the block already embedded. The registry calls this when wiring the flow.

**Alternatives considered**:
- Exposing raw block + runtime type enum: Requires the caller to dispatch to the right factory method
- Abstract class with generics: Generic type erasure makes this awkward for multi-type processors

## R6: Processing Logic Placeholder

**Decision**: Generated node files include a pass-through processing logic that forwards input to output unchanged (for transformers/processors) or emits a default value (for sources). This ensures the file compiles immediately.

**Rationale**: Users expect to generate, compile, test, then customize. A compilation error on generation would break this workflow.

**Alternatives considered**:
- TODO() placeholder: Compiles but crashes at runtime
- Empty lambda: Doesn't compile for non-Unit return types
- Comment-only stub: Doesn't compile

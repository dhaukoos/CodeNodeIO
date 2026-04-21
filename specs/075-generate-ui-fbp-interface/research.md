# Research: Generate UI-FBP Interface

**Feature**: 075-generate-ui-fbp-interface
**Date**: 2026-04-19

## R1: UI File Parsing Strategy

**Decision**: Use regex-based parsing of the Compose UI `.kt` file to infer the ViewModel interface entirely from the UI file's usage patterns. The ViewModel file does not exist yet — it is one of the four files this generator creates. The UI file is the sole input.

**Rationale**: The developer writes the UI composable first, referencing a ViewModel that doesn't exist yet (it will show as unresolved in the IDE). The UI file's usage of that ViewModel — method calls and property accesses — defines the contract. The generator reads those usage patterns and produces all four interface files: ViewModel, State, Source CodeNode, and Sink CodeNode. `viewModel.emit(...)` calls reveal Source ports, and `viewModel.{prop}.collectAsState()` calls reveal Sink ports.

**Parsing approach**:
1. Find the `@Composable fun {Name}(viewModel: {Name}ViewModel, ...)` signature → derive module name
2. Find `viewModel.{methodName}(...)` calls (excluding `collectAsState`) → extract method names and parameter types as Source outputs
3. Find `viewModel.{propertyName}.collectAsState()` calls → extract property names and their types as Sink inputs
4. Resolve IP types from the module's `/iptypes` directory by matching type names

**Alternatives considered**:
- Kotlin compiler API for full AST parsing: Rejected — heavy dependency, slow startup, overkill for convention-based patterns.
- Require a separate interface definition file: Rejected — adds a manual step, defeats the automation goal.

## R2: CodeNode Source File Generation

**Decision**: Generate a CodeNode Source `object` following the existing pattern: `{Name}SourceCodeNode : CodeNodeDefinition` with `category = CodeNodeType.SOURCE`, `inputPorts = emptyList()`, and output ports matching the ViewModel's emit function parameters.

**Pattern** (from ImagePickerCodeNode):
```kotlin
object {Name}SourceCodeNode : CodeNodeDefinition {
    override val name = "{Name}Source"
    override val category = CodeNodeType.SOURCE
    override val description = "Emits UI input values into the processing graph"
    override val inputPorts = emptyList<PortSpec>()
    override val outputPorts = listOf(
        PortSpec("numA", Double::class),
        PortSpec("numB", Double::class)
    )
    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createSourceOut2<Double, Double>(
            name = name,
            generate = { emit ->
                // Collect from State flows when UI emits values
            }
        )
    }
}
```

**Factory method selection**: Based on output port count — `createContinuousGenerator` (1 output), `createSourceOut2` (2 outputs), `createSourceOut3` (3 outputs).

**File placement**: `{Module}/src/commonMain/kotlin/io/codenode/{modulename}/nodes/{Name}SourceCodeNode.kt`

## R3: CodeNode Sink File Generation

**Decision**: Generate a CodeNode Sink `object` following the existing pattern: `{Name}SinkCodeNode : CodeNodeDefinition` with `category = CodeNodeType.SINK`, `outputPorts = emptyList()`, and input ports matching the ViewModel's StateFlow properties.

**Pattern** (from UserProfilesDisplayCodeNode):
```kotlin
object {Name}SinkCodeNode : CodeNodeDefinition {
    override val name = "{Name}Sink"
    override val category = CodeNodeType.SINK
    override val description = "Receives processed results for UI display"
    override val inputPorts = listOf(
        PortSpec("results", CalculationResults::class)
    )
    override val outputPorts = emptyList<PortSpec>()
    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createContinuousSink<CalculationResults>(
            name = name,
            consume = { value ->
                {Name}State._results.value = value
            }
        )
    }
}
```

**Factory method selection**: Based on input port count — `createContinuousSink` (1 input), `createSinkIn2` (2 inputs), `createSinkIn3` (3 inputs).

**File placement**: `{Module}/src/commonMain/kotlin/io/codenode/{modulename}/nodes/{Name}SinkCodeNode.kt`

## R4: ViewModel Generation Pattern

**Decision**: Generate `{Name}ViewModel` extending `androidx.lifecycle.ViewModel`. The ViewModel delegates to the State object for all observable state, and exposes methods that write to the State's internal MutableStateFlows.

**Pattern** (from DemoUIViewModel):
- Expose `StateFlow` properties from State object (read-only wrappers of Sink data)
- Expose `emit(...)` style functions that write Source data to State object
- Include `reset()` method that delegates to `{Name}State.reset()`

**Key**: The ViewModel does NOT contain business logic — that's the flowgraph's job. The ViewModel is purely a bridge between UI calls and State flows.

## R5: State Object Generation Pattern

**Decision**: Generate `{Name}State` as a Kotlin `object` with MutableStateFlow/StateFlow pairs for every boundary value — both Source outputs (data UI sends) and Sink inputs (data graph returns).

**Pattern** (from DemoUIState):
- Internal `_propertyName` as `MutableStateFlow<T>` — written by Source nodes and Sink nodes
- Public `propertyNameFlow` as `StateFlow<T>` — read by ViewModel
- `reset()` function that clears all flows to default values

## R6: Generator Module Placement

**Decision**: Place the new generator class in `flowGraph-generate` alongside existing generators. Create `UIFBPInterfaceGenerator` in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/`.

**Rationale**: All code generation logic lives in `flowGraph-generate`. The existing generators (RuntimeViewModelGenerator, EntityDisplayCodeNodeGenerator, etc.) establish the pattern. The UI parser can also live here since it's part of the generation pipeline.

**Alternatives considered**:
- New module: Rejected — overkill for a generator that follows existing patterns.
- In graphEditor: Rejected — generation is not a UI concern.

## R7: Triggering from the Graph Editor

**Decision**: Add a "Generate UI-FBP" button to the toolbar (alongside "Save" and "Generate Module"). The button opens a file chooser to select a UI `.kt` file, then runs the generator.

**Rationale**: Follows the same pattern as "Generate Module" — toolbar button → file selection → code generation → status message. The developer needs to point at the UI file since the graph editor doesn't inherently know which file to process.

**Alternative workflow**: The button could also work contextually — if the currently loaded module has a `/userInterface/` directory with exactly one composable file, auto-detect it. But explicit file selection is safer for v1.

## R8: Node Discovery

**Decision**: Generated CodeNode files are discovered automatically by `NodeDefinitionRegistry.scanDirectory()` which scans module `nodes/` directories for `.kt` files containing `CodeNodeDefinition`. No additional registration step is needed.

**Rationale**: The existing discovery mechanism handles this. When the graph editor loads a module directory, it scans for CodeNode definitions. The generated files follow the same pattern and are immediately visible.

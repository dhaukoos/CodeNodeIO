# Research: Persist CodeNode Metadata Through Save Pipeline

## R1: How to Inject _codeNodeClass into CodeNodeDefinition's defaultConfiguration

**Decision**: Add `_codeNodeClass` to `defaultConfiguration` in `CodeNodeDefinition.toNodeTypeDefinition()` using `this::class.qualifiedName`.

**Rationale**: `CodeNodeDefinition.toNodeTypeDefinition()` (at `fbpDsl/.../runtime/CodeNodeDefinition.kt:97-144`) already sets `_genericType` and `_codeNodeDefinition` in `defaultConfiguration` (lines 139-142). Adding `_codeNodeClass` follows the same pattern. Kotlin's `this::class.qualifiedName` returns the fully-qualified class name (e.g., `io.codenode.stopwatch.nodes.TimerEmitterCodeNode`) which is exactly what `RuntimeFlowGenerator` checks for.

**Current defaultConfiguration** (line 139-142):
```kotlin
defaultConfiguration = mapOf(
    "_genericType" to "in${inputPorts.size}out${outputPorts.size}",
    "_codeNodeDefinition" to "true"
)
```

**New defaultConfiguration**:
```kotlin
defaultConfiguration = mapOf(
    "_genericType" to "in${inputPorts.size}out${outputPorts.size}",
    "_codeNodeDefinition" to "true",
    "_codeNodeClass" to (this::class.qualifiedName ?: "")
)
```

**Alternatives considered**:
- *Registry lookup at generation time* — Rejected. The generator doesn't have access to the registry; metadata must be on the node itself.
- *Separate metadata field on CodeNode model* — Rejected. The `configuration` map already serves this purpose and is the convention throughout the codebase.

## R2: FlowKtGenerator _-Prefix Filtering

**Decision**: Whitelist specific `_`-prefixed keys that must be preserved in the `.flow.kt` file, rather than removing the filter entirely.

**Current filter** (at `kotlinCompiler/.../generator/FlowKtGenerator.kt:166-169`):
```kotlin
node.configuration.filter { !it.key.startsWith("_") }.forEach { (key, value) ->
    builder.appendLine("${innerIndent}config(\"${escapeString(key)}\", \"${escapeString(value)}\")")
}
```

**New approach**: Preserve keys that are needed for round-tripping:
```kotlin
val preservedInternalKeys = setOf("_codeNodeClass", "_genericType", "_codeNodeDefinition")
node.configuration.filter { !it.key.startsWith("_") || it.key in preservedInternalKeys }.forEach { ... }
```

**Rationale**: The `_`-prefix filter was added to prevent transient internal state from leaking into the serialized `.flow.kt`. However, `_codeNodeClass`, `_genericType`, and `_codeNodeDefinition` are essential for the code generation pipeline and must survive serialization. Other `_`-prefixed keys (like `_cudSource`, `_display`, `_repository`, `_sourceIPTypeId`, `_sourceIPTypeName`) are legacy and will be removed with the legacy cleanup.

**The DSL supports this**: `FlowGraphDsl.kt:232-234` defines a `config(key, value)` function that stores arbitrary string key-value pairs in the node's configuration map. These are read back on deserialization.

**Alternatives considered**:
- *Remove all _ filtering* — Rejected. Some internal keys were genuinely transient.
- *Use a separate annotation mechanism* — Rejected. Overengineered for a simple key-value store.

## R3: DragAndDropHandler.createNodeFromType() Configuration Loss

**Decision**: Pass `nodeType.defaultConfiguration` to the created CodeNode instead of `emptyMap()`.

**Current code** (at `graphEditor/.../ui/DragAndDropHandler.kt:259-272`):
```kotlin
val codeNode = CodeNode(
    id = ...,
    name = nodeType.name,
    ...
    configuration = emptyMap()  // line 267 — ALL metadata lost
)
```

**Fix**: Change to:
```kotlin
configuration = nodeType.defaultConfiguration
```

**Rationale**: The `NodeTypeDefinition` already carries `defaultConfiguration` (set by `CodeNodeDefinition.toNodeTypeDefinition()`). The click-to-place path (Main.kt:894) already passes `configuration = nodeType.defaultConfiguration`. The drag-and-drop path was simply an oversight.

**Note**: The click-to-place path at Main.kt:894 already works correctly — it uses `nodeType.defaultConfiguration`.

## R4: FileCustomNodeRepository and CustomNodeDefinition Usage Scope

**Decision**: Remove FileCustomNodeRepository, CustomNodeDefinition, CustomNodeRepository, and all references. Clean up NodeDefinitionRegistry to use only compiled CodeNodeDefinitions.

**Files to delete**:
- `graphEditor/src/jvmMain/kotlin/repository/FileCustomNodeRepository.kt`
- `graphEditor/src/jvmMain/kotlin/repository/CustomNodeDefinition.kt`
- `graphEditor/src/jvmMain/kotlin/repository/CustomNodeRepository.kt`

**Files to modify**:
- `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/NodeDefinitionRegistry.kt` — Remove `customNodeRepository` constructor parameter (line 45-46), `legacyNodes` property (line 55), `discoverLegacyNodes()` method (line 251-255), and the legacy merge block in `getAllForPalette()` (lines 106-113).
- `graphEditor/src/jvmMain/kotlin/Main.kt` — Remove: `customNodeRepository` initialization (lines 345-346), `customNodes` state (line 358-369), passing `customNodeRepository` to `NodeDefinitionRegistry` and `NodeGeneratorViewModel`, the `onNodeCreated` callback refreshing `customNodes` (line 838), and `customNodes` parameters throughout composable calls.
- `graphEditor/src/jvmMain/kotlin/viewmodel/NodeGeneratorViewModel.kt` — Remove `customNodeRepository` constructor parameter and `createNode()` method.
- `graphEditor/src/jvmMain/kotlin/ui/NodeGeneratorPanel.kt` — Remove the legacy "Create" button and `onCreate` callback (line 60-67).

**Risk**: The `~/.codenode/custom-nodes.json` file will no longer be read. The 2 test artifacts (MyIn2AnyOut1, CUDoperations) will be lost. This is acceptable per clarification.

## R5: ProcessingLogicStubGenerator in the Save Pipeline

**Decision**: Remove ProcessingLogicStubGenerator from the save pipeline. With all nodes having CodeNodeDefinitions, processingLogic stubs are no longer needed.

**Current behavior**: `ModuleSaveService` (line 72) instantiates `ProcessingLogicStubGenerator`. During save (lines 1025-1051), for each processor node (input+output ports), it checks `shouldGenerateStub()` which returns `true` if the node has a `_genericType` config but is NOT a source-only, sink-only, CUD source, or display sink. If true, it generates or preserves a `{NodeName}ProcessLogic.kt` file.

**With CodeNode-driven generation**: Nodes using `createRuntime()` don't need processingLogic stubs — the logic is inside the CodeNodeDefinition. The generator should skip stub generation entirely for nodes with `_codeNodeClass`.

**Simplest approach**: In `ModuleSaveService`, skip processingLogic stub generation for nodes that have `_codeNodeClass` in their configuration. Since all nodes will have this after the fix, the stub generator becomes a no-op and can be removed entirely.

## R6: NodeGeneratorViewModel createNode() vs generateCodeNode()

**Decision**: Remove `createNode()` and the legacy "Create" button. Only `generateCodeNode()` remains.

**`createNode()`** (line 167-180): Creates a `CustomNodeDefinition` and saves it to `FileCustomNodeRepository`. This is the legacy path that produces JSON entries in `~/.codenode/custom-nodes.json`.

**`generateCodeNode()`** (line 188-248): Generates a `.kt` file implementing `CodeNodeDefinition`. This is the modern path that produces compiled, self-contained node objects.

**UI impact**: `NodeGeneratorPanel.kt` (lines 60-67) has two buttons:
- "Create" → calls `createNode()` (legacy)
- "Generate CodeNode" → calls `generateCodeNode()` (modern)

After removal, only "Generate CodeNode" remains. The `onCreate` callback chain in Main.kt (line 836-838) that refreshes the `customNodes` state can also be removed.

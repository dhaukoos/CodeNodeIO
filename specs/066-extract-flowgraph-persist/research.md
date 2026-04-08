# Research: flowGraph-persist Module Extraction

## R1: Which Files Belong in flowGraph-persist?

**Decision**: Extract 6 core serialization/template files, not 8. Exclude ViewSynchronizer and TextualView.

**Rationale**: The original description listed 8 files including `ViewSynchronizer.kt` and `TextualView.kt`. Analysis reveals:
- `ViewSynchronizer` takes `GraphState` as a constructor parameter — GraphState is a 1711-line graphEditor-internal class. Including ViewSynchronizer would require either a circular dependency (persist → graphEditor) or extracting GraphState too (massive scope creep).
- `TextualView` is a Compose UI composable with heavy Material UI imports — it's a presentation concern, not persistence.
- Both files belong in the flowGraph-compose slice (bidirectional sync between visual and textual representations), not persist.

**Files to extract (6)**:
1. `serialization/FlowGraphSerializer.kt` (543 lines, JVM — java.io.File)
2. `serialization/FlowKtParser.kt` (764 lines, pure Kotlin)
3. `serialization/GraphNodeTemplateSerializer.kt` (206 lines, JVM — java.io.File)
4. `model/GraphNodeTemplateMeta.kt` (33 lines, pure Kotlin)
5. `io/codenode/grapheditor/state/GraphNodeTemplateRegistry.kt` (231 lines, JVM — java.io.File)
6. `io/codenode/grapheditor/state/GraphNodeTemplateInstantiator.kt` (279 lines, JVM — java.io.File)

**Alternatives considered**:
- Include all 8: Rejected — ViewSynchronizer creates circular dependency on GraphState
- Include ViewSynchronizer with interface extraction: Rejected — over-engineering, ViewSynchronizer belongs in compose slice
- Extract only serialization/ files (3): Rejected — template files are persistence concerns that belong with their serializer

## R2: Runtime Type for CodeNode

**Decision**: Use `In2AnyOut3Runtime<String, String, String, String, String>` with `anyInput = true`.

**Rationale**: The persist CodeNode has 2 inputs (flowGraphModel, ipTypeMetadata) and 3 outputs (serializedOutput, loadedFlowGraph, graphNodeTemplates). Using `anyInput` mode because:
- ipTypeMetadata updates arrive independently from flowGraphModel
- When ipTypeMetadata changes, the CodeNode should cache it for use in future serialization operations
- When flowGraphModel arrives with a serialize command, it uses the cached ipTypeMetadata
- This matches the pattern established by FlowGraphTypesCodeNode in feature 065

**Alternatives considered**:
- `In2Out3Runtime` (requires ALL inputs): Rejected — would block serialization until ipTypeMetadata arrives, even when not needed for every operation

## R3: KMP Source Set Split

**Decision**: 1 file in commonMain (GraphNodeTemplateMeta), 5 files in jvmMain, CodeNode + tests in jvmMain.

**Rationale**:
- `GraphNodeTemplateMeta.kt` is a pure data class with no JVM dependencies — safe for commonMain
- `FlowKtParser.kt` is pure Kotlin but uses `kotlin.reflect.KClass` extensively — KMP-compatible, could go in commonMain. However, keeping it in jvmMain alongside its consumers simplifies the initial extraction (can revisit later).
- All other files use `java.io.File` — must be in jvmMain
- CodeNode wraps JVM-dependent code — must be in jvmMain

**Alternatives considered**:
- Put FlowKtParser in commonMain: Possible but unnecessary complexity for this extraction — defer to a future feature

## R4: Port Command Protocol

**Decision**: flowGraphModel input carries JSON commands with an `action` field. ipTypeMetadata input carries cached type data.

**Rationale**: Following the same pattern as FlowGraphTypesCodeNode:
- `flowGraphModel` input: JSON with `action` (serialize, deserialize, saveTemplate, loadTemplate, deleteTemplate, listTemplates) and payload data
- `ipTypeMetadata` input: cached IP type registry state (same format as FlowGraphTypesCodeNode output)
- `serializedOutput` output: .flow.kt text (serialize result)
- `loadedFlowGraph` output: serialized FlowGraph data (deserialize result)
- `graphNodeTemplates` output: serialized template metadata list (template CRUD results)

## R5: Dependencies

**Decision**: flowGraph-persist depends on `:fbpDsl` and `:flowGraph-types`.

**Rationale**:
- `:fbpDsl` — FlowGraph, Node, CodeNode, GraphNode, Connection, Port, PlacementLevel, CodeNodeDefinition, runtime types
- `:flowGraph-types` — IPTypeRegistry (needed by GraphNodeTemplateSerializer and GraphNodeTemplateInstantiator for template loading with IP type resolution)
- No dependency on `:graphEditor` — consumers in graphEditor depend on flowGraph-persist, not the reverse

## R6: Data-Oriented Port Naming

**Decision**: Use data-oriented names matching architecture.flow.kt: flowGraphModel, ipTypeMetadata, serializedOutput, loadedFlowGraph, graphNodeTemplates.

**Rationale**: Feature 064 R6 established data-oriented naming over service-oriented naming. These names describe the data shape flowing through ports, not the service performing the operation. Already defined in architecture.flow.kt.

## R7: Internal Cross-References

**Decision**: All 6 files cross-reference each other and will move together. No dangling references.

**Analysis**:
- FlowGraphSerializer ← used by GraphNodeTemplateSerializer
- FlowKtParser ← used by GraphNodeTemplateSerializer
- GraphNodeTemplateSerializer ← used by GraphNodeTemplateRegistry, GraphNodeTemplateInstantiator
- GraphNodeTemplateMeta ← used by GraphNodeTemplateSerializer, GraphNodeTemplateRegistry, GraphNodeTemplateInstantiator
- GraphNodeTemplateRegistry and GraphNodeTemplateInstantiator are leaf consumers

All internal references are satisfied within the 6-file set.

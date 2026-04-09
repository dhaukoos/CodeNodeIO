# Quickstart: Extract flowGraph-compose Module

## Verification Scenarios

### 1. Module Compiles Independently

```bash
./gradlew :flowGraph-compose:build
```

Expected: BUILD SUCCESSFUL, all tests pass.

### 2. Full Project Compiles

```bash
./gradlew check
```

Expected: All existing tests pass. Zero regressions.

### 3. No Stale References

```bash
# Verify no graphEditor files still reference old package paths for moved files
grep -r "io.codenode.grapheditor.viewmodel.CanvasInteractionViewModel" graphEditor/
grep -r "io.codenode.grapheditor.viewmodel.PropertiesPanelViewModel" graphEditor/
grep -r "io.codenode.grapheditor.state.NodeGeneratorState" graphEditor/
grep -r "io.codenode.grapheditor.state.ViewSynchronizer" graphEditor/
```

Expected: No matches (all references updated to `io.codenode.flowgraphcompose`).

### 4. Original Files Removed

```bash
# Verify files no longer exist in graphEditor
ls graphEditor/src/jvmMain/kotlin/viewmodel/CanvasInteractionViewModel.kt
ls graphEditor/src/jvmMain/kotlin/viewmodel/PropertiesPanelViewModel.kt
ls graphEditor/src/jvmMain/kotlin/state/NodeGeneratorState.kt
ls graphEditor/src/jvmMain/kotlin/state/ViewSynchronizer.kt
```

Expected: All return "No such file or directory".

### 5. CodeNode Port Contract

Run the TDD tests:

```bash
./gradlew :flowGraph-compose:jvmTest --tests "*.FlowGraphComposeCodeNodeTest"
```

Expected: Tests verify 3 input ports (flowGraphModel, nodeDescriptors, ipTypeMetadata), 1 output port (graphState), anyInput=true, and basic data flow.

### 6. Architecture Wiring

Verify the compose graphNode in architecture.flow.kt has:
- One child codeNode: "FlowGraphCompose" (TRANSFORMER, 3 inputs, 1 output)
- Port mappings for all 3 inputs and 1 output
- All 20 external connections intact

### 7. Characterization Tests

```bash
./gradlew :graphEditor:jvmTest --tests "*.GraphDataOpsCharacterizationTest"
./gradlew :graphEditor:jvmTest --tests "*.ViewModelCharacterizationTest"
```

Expected: Both pass without modification.

### 8. Runtime Preview

Launch the graph editor from the demo project and verify:
- Canvas interactions work (drag, select, connect)
- Properties panel editing works
- Undo/redo works
- Edit button appears for FlowGraphCompose CodeNode in Properties panel

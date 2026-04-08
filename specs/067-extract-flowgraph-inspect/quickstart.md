# Quickstart: flowGraph-inspect Module Extraction

**Feature**: 067-extract-flowgraph-inspect
**Purpose**: Validation scenarios to verify the extraction is correct at each phase

## Scenario 1: Empty Module Compiles

**When**: Phase 1 (Setup) is complete
**Run**: `./gradlew :flowGraph-inspect:compileKotlinJvm`
**Expected**: BUILD SUCCESSFUL — empty module compiles with zero errors

## Scenario 2: Copied Files Compile in New Module

**When**: Phase 2 (File Extraction) is complete
**Run**: `./gradlew :flowGraph-inspect:compileKotlinJvm`
**Expected**: BUILD SUCCESSFUL — all 7 copied files resolve imports from fbpDsl, flowGraph-types, flowGraph-persist

## Scenario 3: Both Modules Coexist (Strangler Fig)

**When**: Phase 2 (File Extraction) is complete
**Run**: `./gradlew :graphEditor:compileKotlinJvm :flowGraph-inspect:compileKotlinJvm`
**Expected**: BUILD SUCCESSFUL — both modules compile independently with original files still in graphEditor

## Scenario 4: No graphEditor Dependency in flowGraph-inspect

**When**: Phase 2 (File Extraction) is complete
**Run**: `grep -r "graphEditor" flowGraph-inspect/build.gradle.kts`
**Expected**: No matches — flowGraph-inspect depends only on fbpDsl, flowGraph-types, flowGraph-persist

## Scenario 5: TDD Tests Compile and Fail

**When**: Phase 3 (TDD Tests) is complete, before CodeNode implementation
**Run**: `./gradlew :flowGraph-inspect:jvmTest`
**Expected**: Tests compile but FAIL (no FlowGraphInspectCodeNode implementation yet)

## Scenario 6: CodeNode Tests Pass

**When**: Phase 4 (CodeNode Implementation) is complete
**Run**: `./gradlew :flowGraph-inspect:jvmTest`
**Expected**: BUILD SUCCESSFUL — all CodeNode TDD tests pass

## Scenario 7: Call Sites Migrated — All Tests Pass

**When**: Phase 5 (Call Site Migration) is complete
**Run**: `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest`
**Expected**: BUILD SUCCESSFUL — all existing tests pass with updated imports

## Scenario 8: No Old Imports Remain

**When**: Phase 5 (Call Site Migration) is complete
**Run**: Search graphEditor source files (excluding dead originals) for old package imports:
- `io.codenode.grapheditor.state.NodeDefinitionRegistry`
- `io.codenode.grapheditor.viewmodel.CodeEditorViewModel`
- `io.codenode.grapheditor.viewmodel.IPPaletteViewModel`
- `io.codenode.grapheditor.viewmodel.GraphNodePaletteViewModel`
- `io.codenode.grapheditor.viewmodel.NodePaletteViewModel`
- `io.codenode.grapheditor.ui.ComposableDiscovery`
- `io.codenode.grapheditor.ui.DynamicPreviewDiscovery`
**Expected**: Zero matches in live files (only the dead originals still have old packages)

## Scenario 9: Originals Removed — All Tests Pass

**When**: Phase 6 (Remove Originals) is complete
**Run**: `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest :flowGraph-inspect:jvmTest`
**Expected**: BUILD SUCCESSFUL — all tests pass across all modules. The 7 original files no longer exist in graphEditor.

## Scenario 10: Compose UI Files Unchanged

**When**: Phase 6 (Remove Originals) is complete
**Verify**: These 5 files still exist in graphEditor unchanged:
- `graphEditor/src/jvmMain/kotlin/ui/CodeEditor.kt`
- `graphEditor/src/jvmMain/kotlin/ui/ColorEditor.kt`
- `graphEditor/src/jvmMain/kotlin/ui/IPPalette.kt`
- `graphEditor/src/jvmMain/kotlin/ui/NodePalette.kt`
- `graphEditor/src/jvmMain/kotlin/ui/SyntaxHighlighter.kt`
**Expected**: All 5 files present and unmodified

## Scenario 11: Architecture Test Passes

**When**: Phase 7 (Architecture Wiring) is complete
**Run**: `./gradlew :graphEditor:jvmTest --tests "characterization.ArchitectureFlowKtsTest"`
**Expected**: BUILD SUCCESSFUL — architecture tests pass with FlowGraphInspect child CodeNode in the flowGraph-inspect GraphNode

## Scenario 12: No Circular Dependencies

**When**: Phase 8 (Verification) is complete
**Run**: `./gradlew :flowGraph-inspect:dependencies`
**Expected**: Only `:fbpDsl`, `:flowGraph-types`, and `:flowGraph-persist` appear as project dependencies. No `:graphEditor`.

## Scenario 13: Full Test Suite Green

**When**: Phase 8 (Verification) is complete
**Run**: `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest :flowGraph-types:jvmTest :flowGraph-persist:jvmTest :flowGraph-inspect:jvmTest`
**Expected**: BUILD SUCCESSFUL — zero regressions across all modules

## Scenario 14: ViewModelCharacterizationTest Passes

**When**: Phase 8 (Verification) is complete
**Run**: `./gradlew :graphEditor:jvmTest --tests "characterization.ViewModelCharacterizationTest"`
**Expected**: BUILD SUCCESSFUL — palette and registry state characterization tests pass

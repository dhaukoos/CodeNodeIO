# Quickstart: flowGraph-execute Module Extraction

**Feature**: 068-extract-flowgraph-execute
**Purpose**: Validation scenarios to verify the extraction is correct at each phase

## Scenario 1: Empty Module Compiles

**When**: Phase 1 (Setup) is complete
**Run**: `./gradlew :flowGraph-execute:compileKotlinJvm`
**Expected**: BUILD SUCCESSFUL — empty module compiles with zero errors

## Scenario 2: Copied Files Compile in New Module

**When**: Phase 2 (File Extraction) is complete
**Run**: `./gradlew :flowGraph-execute:compileKotlinJvm`
**Expected**: BUILD SUCCESSFUL — all 6 copied files resolve imports from fbpDsl and flowGraph-inspect

## Scenario 3: Both Modules Coexist (Strangler Fig)

**When**: Phase 2 (File Extraction) is complete
**Run**: `./gradlew :graphEditor:compileKotlinJvm :circuitSimulator:compileKotlinJvm :flowGraph-execute:compileKotlinJvm`
**Expected**: BUILD SUCCESSFUL — all three modules compile independently with original files still in source modules

## Scenario 4: No graphEditor or circuitSimulator Dependency in flowGraph-execute

**When**: Phase 2 (File Extraction) is complete
**Run**: `grep -E "graphEditor|circuitSimulator" flowGraph-execute/build.gradle.kts`
**Expected**: No matches — flowGraph-execute depends only on fbpDsl and flowGraph-inspect

## Scenario 5: TDD Tests Compile and Fail

**When**: Phase 3 (TDD Tests) is complete, before CodeNode implementation
**Run**: `./gradlew :flowGraph-execute:jvmTest`
**Expected**: Tests compile but FAIL (no FlowGraphExecuteCodeNode implementation yet)

## Scenario 6: CodeNode Tests Pass

**When**: Phase 4 (CodeNode Implementation) is complete
**Run**: `./gradlew :flowGraph-execute:jvmTest`
**Expected**: BUILD SUCCESSFUL — all CodeNode TDD tests pass

## Scenario 7: Call Sites Migrated — All Tests Pass

**When**: Phase 5 (Call Site Migration) is complete
**Run**: `./gradlew :graphEditor:jvmTest :circuitSimulator:jvmTest`
**Expected**: BUILD SUCCESSFUL — all existing tests pass with updated imports

## Scenario 8: No Old Imports Remain

**When**: Phase 5 (Call Site Migration) is complete
**Run**: Search graphEditor source files (excluding dead originals) for old package imports:
- `io.codenode.circuitsimulator.RuntimeSession`
- `io.codenode.circuitsimulator.ConnectionAnimation`
- `io.codenode.circuitsimulator.DataFlowAnimationController`
- `io.codenode.circuitsimulator.DataFlowDebugger`
**Expected**: Zero matches in live files (only the dead originals still have old packages)

## Scenario 9: Originals Removed — All Tests Pass

**When**: Phase 6 (Remove Originals) is complete
**Run**: `./gradlew :graphEditor:jvmTest :flowGraph-execute:jvmTest :flowGraph-inspect:jvmTest :flowGraph-types:jvmTest :flowGraph-persist:jvmTest`
**Expected**: BUILD SUCCESSFUL — all tests pass. The 6 original files no longer exist in their source modules.

## Scenario 10: RuntimePreviewPanel Unchanged

**When**: Phase 6 (Remove Originals) is complete
**Verify**: This file still exists in graphEditor unchanged:
- `graphEditor/src/jvmMain/kotlin/ui/RuntimePreviewPanel.kt`
**Expected**: File present and unmodified (stays as Compose UI concern)

## Scenario 11: Architecture Test Passes

**When**: Phase 7 (Architecture Wiring) is complete
**Run**: `./gradlew :graphEditor:jvmTest --tests "io.codenode.grapheditor.characterization.ArchitectureFlowKtsTest"`
**Expected**: BUILD SUCCESSFUL — architecture tests pass with FlowGraphExecute child CodeNode in the flowGraph-execute GraphNode

## Scenario 12: No Circular Dependencies

**When**: Phase 8 (Verification) is complete
**Run**: `./gradlew :flowGraph-execute:dependencies`
**Expected**: Only `:fbpDsl` and `:flowGraph-inspect` appear as project dependencies. No `:graphEditor`, no `:circuitSimulator`.

## Scenario 13: Full Test Suite Green

**When**: Phase 8 (Verification) is complete
**Run**: `./gradlew :graphEditor:jvmTest :flowGraph-types:jvmTest :flowGraph-persist:jvmTest :flowGraph-inspect:jvmTest :flowGraph-execute:jvmTest`
**Expected**: BUILD SUCCESSFUL — zero regressions across all modules

## Scenario 14: RuntimeSessionCharacterizationTest Passes

**When**: Phase 8 (Verification) is complete
**Run**: `./gradlew :flowGraph-execute:jvmTest --tests "io.codenode.flowgraphexecute.characterization.RuntimeSessionCharacterizationTest"`
**Expected**: BUILD SUCCESSFUL — runtime session characterization tests pass in their new home

## Scenario 15: idePlugin Build Succeeds

**When**: Phase 8 (Verification) is complete
**Run**: `./gradlew :idePlugin:compileKotlinJvm`
**Expected**: BUILD SUCCESSFUL — idePlugin compiles with flowGraph-execute replacing circuitSimulator dependency

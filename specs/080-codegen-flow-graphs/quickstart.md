# Quickstart Verification: Code Generation Flow Graphs

**Feature**: 080-codegen-flow-graphs
**Date**: 2026-04-23

## Verification Scenarios

### VS1: GenerateModule Flow Graph Loads

**Steps**: Open `GenerateModule.flow.kt` in the graph editor.
**Expected**: 7 Generator CodeNodes visible with correct connections from source.

### VS2: GenerateRepository Flow Graph Loads

**Steps**: Open `GenerateRepository.flow.kt` in the graph editor.
**Expected**: 11+ Generator CodeNodes visible (7 module + 4 entity).

### VS3: GenerateUIFBP Flow Graph Loads

**Steps**: Open `GenerateUIFBP.flow.kt` in the graph editor.
**Expected**: 8+ Generator CodeNodes visible (shared + 4 UI-FBP).

### VS4: Runner Produces Correct Output

**Steps**: Run `./gradlew :flowGraph-generate:jvmTest --tests "*CodeGenerationRunner*execute with GENERATE_MODULE*"`
**Expected**: Test passes — 7 non-empty content entries in GenerationResult.
**Correlating test**: `CodeGenerationRunnerTest.execute with GENERATE_MODULE produces 7 entries`

### VS5: Selective Execution

**Steps**: Run `./gradlew :flowGraph-generate:jvmTest --tests "*CodeGenerationRunner*selection filter*"`
**Expected**: Test passes — 6 entries in output, Controller absent, listed in skipped.
**Correlating test**: `CodeGenerationRunnerTest.execute with selection filter excludes specified generators`

### VS6: Full Selection Matches ModuleSaveService

**Steps**: Run `./gradlew :flowGraph-generate:jvmTest --tests "*CodeGenerationRunner*package*"`
**Expected**: Tests pass — generated content contains correct package declarations matching ModuleSaveService output patterns.
**Correlating tests**: `FlowKtGenerator output contains package declaration`, `RuntimeControllerGenerator output contains controller package`

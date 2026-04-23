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

**Steps**: Execute runner with GenerateModule flow graph and test FlowGraph.
**Expected**: 7 non-empty content entries in GenerationResult.

### VS5: Selective Execution

**Steps**: Exclude "RuntimeControllerGenerator" via SelectionFilter. Execute runner.
**Expected**: 6 entries in output, Controller content absent, "RuntimeControllerGenerator" in skipped set.

### VS6: Full Selection Matches ModuleSaveService

**Steps**: Compare runner output to ModuleSaveService output for same input.
**Expected**: Identical generated content for each file.

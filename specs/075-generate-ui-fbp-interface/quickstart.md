# Quickstart Verification: Generate UI-FBP Interface

**Feature**: 075-generate-ui-fbp-interface
**Date**: 2026-04-19

## Prerequisites

- Branch `075-generate-ui-fbp-interface` checked out
- Gradle builds successfully: `./gradlew :flowGraph-generate:compileKotlinJvm :graphEditor:compileKotlinJvm`
- DemoProject's TestModule exists with DemoUI.kt

## Verification Scenarios

### VS1: Compilation and Test Baseline

**Steps**:
1. Run `./gradlew :flowGraph-generate:jvmTest` (generator tests)
2. Run `./gradlew :graphEditor:jvmTest` (editor tests)
3. Verify all tests pass

**Expected**: Zero test failures across both test suites.

### VS2: Parse DemoUI ViewModel Interface

**Steps**:
1. Point the UI parser at `TestModule/src/commonMain/kotlin/io/codenode/demo/userInterface/DemoUI.kt`
2. Verify extracted interface

**Expected**:
- Module name: "DemoUI"
- ViewModel type: "DemoUIViewModel"
- Source outputs: `numA: Double`, `numB: Double` (from `viewModel.emit(a, b)` call)
- Sink inputs: `results: CalculationResults` (from `viewModel.results.collectAsState()`)

### VS3: Generate All Four Files

**Steps**:
1. Trigger "Generate UI-FBP" on DemoUI.kt
2. Inspect the output directory

**Expected**: Four files generated:
- `DemoUIViewModel.kt` — extends ViewModel, delegates to DemoUIState, has `emit(numA, numB)` and `reset()`
- `DemoUIState.kt` — object with `_numA`, `_numB` MutableStateFlows (Source data) and `_results` MutableStateFlow (Sink data)
- `DemoUISourceCodeNode.kt` — Source with 2 output ports (numA: Double, numB: Double)
- `DemoUISinkCodeNode.kt` — Sink with 1 input port (results: CalculationResults)

### VS4: Generated Files Compile

**Steps**:
1. After generating, run `./gradlew :TestModule:compileDebugKotlinAndroid`
2. Verify no compile errors

**Expected**: All generated files compile alongside the existing DemoUI.kt and CalculationResults IP type.

### VS5: Compare with Hand-Written Files

**Steps**:
1. Compare generated `DemoUIViewModel.kt` with the hand-written version
2. Compare generated `DemoUIState.kt` with the hand-written version
3. Verify structural equivalence

**Expected**: Generated files match the structural patterns of the hand-written prototypes. Differences in formatting or comments are acceptable; the API surface (methods, properties, types) must match.

### VS6: CodeNode Discovery

**Steps**:
1. Launch the graph editor with the TestModule loaded
2. Open the node palette
3. Search for "DemoUI"

**Expected**: "DemoUISource" and "DemoUISink" CodeNodes appear in the palette with correct port definitions.

### VS7: Toolbar Integration

**Steps**:
1. Launch the graph editor
2. Verify "Generate UI-FBP" button exists in the toolbar
3. Click it and select DemoUI.kt via file chooser
4. Verify status message shows generation results

**Expected**: Button is visible, file chooser opens for .kt files, generation completes with a success message listing the four generated files.

### VS8: Edge Case — No ViewModel Parameter

**Steps**:
1. Create a simple Compose file without a ViewModel parameter
2. Trigger "Generate UI-FBP" on it

**Expected**: Error message indicating the file has no ViewModel parameter.

### VS9: Edge Case — Overwrite Existing Files

**Steps**:
1. Run "Generate UI-FBP" on DemoUI.kt (first time)
2. Run it again (second time)

**Expected**: Files are regenerated without errors. Existing generated files are overwritten.

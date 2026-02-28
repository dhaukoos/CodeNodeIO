# Quickstart: Refine the ViewModel Binding

**Feature**: 034-refine-viewmodel-binding
**Date**: 2026-02-28

## Overview

Refactor the code-generated ViewModel from a thin wrapper in the `generated/` folder into a stub file at the base package level containing a consolidated Module State object. Eliminate per-node StateProperties files. Validate by renaming the existing StopWatch to StopWatchV2 and producing a refactored StopWatch module from the same .flow.kt definition.

## Files to Modify

### Phase 1: Rename StopWatch → StopWatchV2

1. **`StopWatch/` directory** → Copy/rename to `StopWatchV2/`
   - Update all package declarations from `io.codenode.stopwatch` to `io.codenode.stopwatchv2`
   - Rename all class/object/val prefixes from `StopWatch` to `StopWatchV2`
   - Rename flow graph val from `stopWatchFlowGraph` to `stopWatchV2FlowGraph`

2. **`settings.gradle.kts`** (root)
   - Change `include(":StopWatch")` to `include(":StopWatchV2")`
   - Add back `include(":StopWatch")` for the new module

3. **`KMPMobileApp/build.gradle.kts`**
   - Change dependency from `:StopWatch` to `:StopWatchV2`

4. **`KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/App.kt`**
   - Update imports from `io.codenode.stopwatch.*` to `io.codenode.stopwatchv2.*`
   - Update class references: `StopWatchController` → `StopWatchV2Controller`, etc.

### Phase 2: Refactor Code Generators

5. **`kotlinCompiler/.../generator/RuntimeViewModelGenerator.kt`** → Refactor to **ViewModelStubGenerator**
   - Generate `{ModuleName}State` object with MutableStateFlow/StateFlow pairs from sink input ports
   - Delineate Module Properties section with `// ===== MODULE PROPERTIES START =====` and `// ===== MODULE PROPERTIES END =====` markers
   - Support selective regeneration: on re-save, extract user code outside markers, regenerate Module Properties, reassemble
   - Generate `{ModuleName}ViewModel` class with state delegation and control methods
   - Output to base package (not `generated/`)

6. **`kotlinCompiler/.../generator/RuntimeFlowGenerator.kt`**
   - Replace `statePropertiesPackage: String?` parameter with `viewModelPackage: String`
   - Import `{ModuleName}State` from viewModelPackage
   - Delegate StateFlow from `{ModuleName}State.{portName}Flow`
   - Update sink consume blocks: `{ModuleName}State._{portName}.value = value`
   - Update reset: `{ModuleName}State.reset()`

7. **`kotlinCompiler/.../generator/ProcessingLogicStubGenerator.kt`**
   - Remove `statePropertiesPackage` parameter
   - Remove StateProperties import generation from stubs

8. **`kotlinCompiler/.../generator/StatePropertiesGenerator.kt`** → DELETE

9. **`graphEditor/.../save/ModuleSaveService.kt`**
   - Remove stateProperties directory creation
   - Remove StateProperties file generation loop
   - Remove stateProperties orphan cleanup
   - Move ViewModel generation from `generated/` to base package
   - Treat ViewModel as stub (preserve existing, like processingLogic)

### Phase 3: Update Tests

10. **`kotlinCompiler/.../generator/StatePropertiesGeneratorTest.kt`** → DELETE

11. **`kotlinCompiler/.../generator/RuntimeFlowGeneratorTest.kt`**
    - Update StateProperties delegation tests to use `{ModuleName}State` pattern
    - Remove `statePropertiesPackage` parameter references

12. **`kotlinCompiler/.../generator/ProcessingLogicStubGeneratorTest.kt`**
    - Remove StateProperties import test cases

13. **`kotlinCompiler/.../generator/RuntimeViewModelGeneratorTest.kt`**
    - Update for new ViewModel stub structure (Module State object + class)

14. **`graphEditor/.../save/ModuleSaveServiceTest.kt`**
    - Remove StateProperties file assertions
    - Add ViewModel stub preservation assertions
    - Update file count expectations

### Phase 4: Create New StopWatch Module

15. **`StopWatch/`** — New module generated with refactored code generation
    - Same .flow.kt definition as StopWatchV2
    - ViewModel stub with `StopWatchState` object in base package
    - No `stateProperties/` folder
    - Fresh processingLogic stubs (user fills in tick logic)

16. **`KMPMobileApp/`** — Switch from StopWatchV2 to new StopWatch
    - Update imports back to `io.codenode.stopwatch.*`
    - Update class references to new StopWatch classes

## Build & Verify

```bash
# After Phase 1 (rename)
./gradlew :StopWatchV2:compileKotlinJvm :KMPMobileApp:compileKotlinJvm

# After Phase 2 (generator refactoring)
./gradlew :kotlinCompiler:compileKotlinJvm

# After Phase 3 (test updates)
./gradlew :kotlinCompiler:allTests :graphEditor:jvmTest

# After Phase 4 (new StopWatch)
./gradlew :StopWatch:compileKotlinJvm :KMPMobileApp:compileKotlinJvm
```

## Manual Test

1. Build and run KMPMobileApp with StopWatchV2 → verify stopwatch works (baseline)
2. Run code generation to produce new StopWatch module
3. Fill in StopWatch processingLogic stubs with tick logic
4. Switch KMPMobileApp to new StopWatch module
5. Build and run → verify identical stopwatch behavior
6. Verify no `stateProperties/` folder in new StopWatch
7. Verify ViewModel is in base package (not `generated/`)
8. Add a custom method to ViewModel class (outside markers)
9. Re-run code generation → verify Module Properties section is regenerated, custom method is preserved

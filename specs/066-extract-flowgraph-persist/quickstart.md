# Quickstart: flowGraph-persist Module Extraction Validation

## Scenario 1: Module Compiles Independently

```bash
./gradlew :flowGraph-persist:compileKotlinJvm
```

**Expected**: BUILD SUCCESSFUL. Zero errors.

## Scenario 2: No Circular Dependencies

```bash
./gradlew :flowGraph-persist:dependencies --configuration commonMainImplementationDependenciesMetadata
```

**Expected**: Only `:fbpDsl` and `:flowGraph-types` appear as project dependencies. No `:graphEditor`.

## Scenario 3: CodeNode TDD Tests Pass

```bash
./gradlew :flowGraph-persist:jvmTest
```

**Expected**: All CodeNode tests pass — port signatures, runtime type, data flow, command processing, boundary conditions.

## Scenario 4: Serialization Roundtrip Preserved

```bash
./gradlew :graphEditor:jvmTest --tests "characterization.SerializationRoundTripCharacterizationTest"
```

**Expected**: All serialization roundtrip tests pass. Behavior unchanged.

## Scenario 5: FlowKtGenerator Unaffected

```bash
./gradlew :kotlinCompiler:jvmTest --tests "characterization.FlowKtGeneratorCharacterizationTest"
```

**Expected**: FlowKtGenerator tests pass. This code lives in generate, not persist.

## Scenario 6: Full Test Suite — Zero Regressions

```bash
./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest :flowGraph-types:jvmTest :flowGraph-persist:jvmTest
```

**Expected**: BUILD SUCCESSFUL across all 5 modules.

## Scenario 7: No Old Imports Remain in graphEditor

After call site migration, search for old package imports:

```bash
grep -r "io.codenode.grapheditor.serialization\|io.codenode.grapheditor.state.GraphNodeTemplate" graphEditor/src/ --include="*.kt" | grep -v "/serialization/" | grep -v "GraphNodeTemplate"
```

**Expected**: Zero matches (excluding the dead original files themselves).

## Scenario 8: Architecture Test Passes

```bash
./gradlew :graphEditor:jvmTest --tests "characterization.ArchitectureFlowKtsTest"
```

**Expected**: All architecture tests pass. flowGraph-persist GraphNode contains child codeNode with port mappings.

## Scenario 9: Strangler Fig Commit Sequence

```bash
git log --oneline --reverse <first-commit>..HEAD
```

**Expected**: Commits follow sequence: module creation → file copy → TDD tests → CodeNode implementation → call site migration → original removal → architecture wiring.

## Scenario 10: Dependency Direction

```bash
grep "graphEditor" flowGraph-persist/build.gradle.kts
```

**Expected**: No matches. Dependency flows: graphEditor → flowGraph-persist → flowGraph-types → fbpDsl.

# Quickstart: Extract flowGraph-types Module

**Feature**: 065-extract-flowgraph-types
**Date**: 2026-04-05

## Prerequisites

- Feature 064 (Phase A) is complete — characterization tests, ARCHITECTURE.md audits, MIGRATION.md, and architecture.flow.kt are all in place
- CodeNodeIO builds and all existing tests pass (`./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest`)
- Access to graphEditor source tree (9 IP type files to extract)

---

## Scenario 1: Verify PlacementLevel Prerequisite Move (US1 - P1)

### Steps

1. Check that `PlacementLevel.kt` exists in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/`
2. Check that `PlacementLevel.kt` no longer exists in `graphEditor/src/jvmMain/kotlin/model/`
3. Run `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest`

### Expected Result

- PlacementLevel.kt is in fbpDsl with package `io.codenode.fbpdsl.model`
- All existing tests pass
- No file with the same name remains in graphEditor

---

## Scenario 2: Verify Module Builds Independently (US1 - P1)

### Steps

1. Run `./gradlew :flowGraph-types:build`
2. Inspect the module's `build.gradle.kts` dependencies section
3. Verify the module produces a JVM artifact

### Expected Result

- The module compiles successfully
- The only project dependency is `:fbpDsl`
- No dependencies on `:graphEditor`, `:kotlinCompiler`, or `:circuitSimulator`
- The JVM artifact is produced (classes directory is non-empty)

---

## Scenario 3: Verify All 9 Files Have Moved (US1 - P1)

### Steps

1. List files in `flowGraph-types/src/commonMain/kotlin/io/codenode/flowgraphtypes/`
2. List files in `flowGraph-types/src/jvmMain/kotlin/io/codenode/flowgraphtypes/`
3. Search for the 9 original files in `graphEditor/src/jvmMain/kotlin/`

### Expected Result

- commonMain contains: IPProperty.kt, IPPropertyMeta.kt, IPTypeFileMeta.kt, SerializableIPType.kt, IPTypeRegistry.kt
- jvmMain contains: IPTypeDiscovery.kt, FileIPTypeRepository.kt, IPTypeMigration.kt, IPTypeFileGenerator.kt
- None of these 9 files exist in graphEditor anymore
- Total: 9 files in flowGraph-types, 0 copies remaining in graphEditor

---

## Scenario 4: Verify Data Flow Contracts (US2 - P1)

### Steps

1. Search for `import io.codenode.grapheditor.state.IPTypeRegistry` in the 6 call site files
2. Search for `import io.codenode.grapheditor.repository.FileIPTypeRepository` in the 6 call site files
3. Search for `import io.codenode.grapheditor.state.IPTypeDiscovery` in the 6 call site files
4. Verify that read-only call sites (GraphState, GraphNodeTemplateSerializer, PropertiesPanel) query locally-held data
5. Verify that mutating call sites (IPGeneratorViewModel, IPPaletteViewModel) send commands to the CodeNode's input port

### Expected Result

- Zero imports of IPTypeRegistry, IPTypeDiscovery, or FileIPTypeRepository remain in any call site file
- No service interface files exist (no `api/` directory with `*Service.kt` files)
- Read-only consumers hold `ipTypeMetadata` as local data and query it directly
- Mutating consumers send serialized commands to the `ipTypeCommands` channel
- SharedStateProvider holds current `ipTypeMetadata` data, not an IPTypeRegistry instance

---

## Scenario 6: Verify No Test Regressions (US1, US2 - P1)

### Steps

1. Run `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest`
2. Check all characterization test classes pass
3. Check all pre-existing unit tests pass

### Expected Result

- All tests pass — zero regressions
- Characterization tests: GraphDataOpsCharacterizationTest, RuntimeExecutionCharacterizationTest, ViewModelCharacterizationTest, SerializationRoundTripCharacterizationTest, CodeGenerationCharacterizationTest, FlowKtGeneratorCharacterizationTest, RuntimeSessionCharacterizationTest all pass
- No test source files needed modification (imports may change for test files that directly reference extracted classes)

---

## Scenario 7: Verify CodeNode Port Contract (US3, US4 - P1)

### Steps

1. Run `./gradlew :flowGraph-types:jvmTest`
2. Review test results for FlowGraphTypesCodeNodeTest

### Expected Result

- Tests verify the CodeNode has exactly 3 input ports (`filesystemPaths`, `classpathEntries`, `ipTypeCommands`) and 1 output port (`ipTypeMetadata`)
- Tests verify the CodeNode uses `anyInput` mode — re-emits on any input change
- Tests verify data flows through channels correctly: providing filesystem paths and classpath entries produces IP type metadata output
- Tests verify mutation commands (register, unregister) through `ipTypeCommands` produce updated metadata output
- Tests verify boundary conditions (empty inputs, invalid paths, malformed commands) are handled gracefully
- All CodeNode tests were written before the implementation (TDD order verified by git history)

---

## Scenario 8: Verify Architecture FlowGraph Wiring (US5 - P1)

### Steps

1. Open `graphEditor/architecture.flow.kt`
2. Locate the flowGraph-types node
3. Verify the new `ipTypeCommands` input port and connection from graphEditor-source
4. Run `./gradlew :graphEditor:jvmTest --tests "characterization.ArchitectureFlowKtsTest"`

### Expected Result

- The flowGraph-types GraphNode container is populated with the FlowGraphTypesCodeNode
- The flowGraph-types node has 3 input ports: `filesystemPaths`, `classpathEntries`, `ipTypeCommands`
- All 4 outbound connections remain intact: ipTypeMetadata → compose, persist, generate, rootSink
- A new inbound connection exists: graphEditor-source → ipTypeCommands
- ArchitectureFlowKtsTest tests pass (updated for 20 connections):
  - `architecture flow kt parses successfully`
  - `architecture flow kt has correct graph name`
  - `architecture flow kt contains all eight nodes`
  - `architecture flow kt has exactly 20 connections` (updated from 19)
  - `types and inspect are the two hub source nodes`
  - `graphEditor-source has only command outputs`
  - `graphEditor-sink has only state inputs`
  - `all workflow modules receive flowGraphModel from source`
  - `no cycles exist in the connection graph`
  - `all target platforms are specified`

---

## Scenario 9: Verify Cyclic Dependency Elimination (US6 - P2)

### Steps

1. Inspect `flowGraph-types/build.gradle.kts` — verify no dependency on `:graphEditor`
2. Inspect `graphEditor/build.gradle.kts` — verify dependency on `:flowGraph-types`
3. Verify the dependency direction: graphEditor → flowGraph-types → fbpDsl

### Expected Result

- flowGraph-types depends only on fbpDsl (one-way)
- graphEditor depends on flowGraph-types (one-way)
- No circular dependency exists
- The former inspect↔persist and inspect↔generate cycles (via IP type files) are eliminated because the shared IP type files now live in an independent module

---

## Scenario 10: Verify Strangler Fig Pattern (US7 - P2)

### Steps

1. Review git history for the extraction sequence
2. Verify tests passed at each intermediate commit

### Expected Result

- Git history shows the Strangler Fig sequence: prerequisite move → module creation → file copy → interface creation → call site update → old file removal → CodeNode TDD → CodeNode implementation → architecture wiring
- Each commit represents a safe intermediate state
- No commit breaks the test suite

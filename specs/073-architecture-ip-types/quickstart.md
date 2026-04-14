# Quickstart Verification: Architecture IP Types

**Feature**: 073-architecture-ip-types
**Date**: 2026-04-14 (revised)

## Prerequisites

- Branch `073-architecture-ip-types` checked out
- Gradle builds successfully: `./gradlew :graphEditor:compileKotlinJvm`

## Verification Scenarios

### VS1: Compilation and Test Baseline

**Steps**:
1. Run `./gradlew :flowGraph-types:jvmTest` (discovery system tests)
2. Run `./gradlew :graphEditor:jvmTest` (editor tests)
3. Verify all tests pass

**Expected**: Zero test failures across both test suites.

### VS2: Typealias Parsing Unit Test

**Steps**:
1. Run `./gradlew :flowGraph-types:jvmTest --tests "*IPTypeDiscovery*"`
2. Verify tests include typealias parsing cases

**Expected**: IPTypeDiscovery correctly parses both `data class` and `typealias` IP type files. Existing data class IP types continue to be discovered without changes.

### VS3: IP Type Files Exist

**Steps**:
1. Count files: `find iptypes/src -name "*.kt" -path "*/iptypes/*" | wc -l`
2. Verify each file contains `@IPType` metadata header
3. Verify typealias files reference actual domain classes with correct imports
4. Verify data class files (FilesystemPath, ClasspathEntry, etc.) have meaningful properties

**Expected**: 14 IP type files total (10 in commonMain, 4 in jvmMain). Each has proper `@IPType`, `@TypeName`, `@TypeId`, and `@Color` metadata.

### VS4: IP Type Discovery

**Steps**:
1. Launch the graph editor: `./gradlew :graphEditor:run`
2. Open the IP type palette
3. Verify all 14 architecture IP types appear in the list

**Expected**: NodeDescriptors, IPTypeMetadata, FlowGraphModel, LoadedFlowGraph, GraphNodeTemplates, RuntimeExecutionState, DataFlowAnimations, DebugSnapshots, EditorGraphState, GeneratedOutput, GenerationContext, FilesystemPath, ClasspathEntry, and IPTypeCommand are all visible in the palette with distinct colors.

### VS5: Architecture Flow Graph Types

**Steps**:
1. Open `graphEditor/architecture.flow.kt` in the graph editor
2. Inspect the flowGraph-inspect node's "nodeDescriptors" output port
3. Inspect the flowGraph-types node's "ipTypeMetadata" output port
4. Inspect the flowGraph-persist node's "serializedOutput" output port
5. Check connection colors across all 20 connections

**Expected**:
- nodeDescriptors shows type "NodeDescriptors" (not "String")
- ipTypeMetadata shows type "IPTypeMetadata" (not "String")
- serializedOutput shows type "String" (unchanged)
- All connections are color-coded by their domain IP type

### VS6: Save and Re-Open Persistence

**Steps**:
1. Open architecture.flow.kt in the graph editor
2. Save the file (Ctrl+S or toolbar Save)
3. Close and re-open the file
4. Verify all IP type assignments are preserved

**Expected**: All port types persist correctly through save/load cycles. No types revert to String.

### VS7: File Format Validation

**Steps**:
1. Verify no duplicate TypeId values:
   ```
   grep -rh "@TypeId" iptypes/src/ | sort | uniq -d
   ```
2. Verify typealias files contain `typealias` keyword:
   ```
   grep -l "typealias" iptypes/src/commonMain/kotlin/io/codenode/iptypes/*.kt
   grep -l "typealias" iptypes/src/jvmMain/kotlin/io/codenode/iptypes/*.kt
   ```
3. Verify data class files contain `data class` keyword:
   ```
   grep -l "data class" iptypes/src/commonMain/kotlin/io/codenode/iptypes/*.kt
   ```

**Expected**: No duplicate TypeIds. Typealias files correctly reference domain classes. Data class files have meaningful properties.

### VS8: Backward Compatibility

**Steps**:
1. Verify existing IP types (UserProfile, Address, etc. in CodeNodeIO-DemoProject) still load correctly
2. Open any existing .flow.kt file that uses String-typed ports
3. Verify it loads and displays without errors

**Expected**: No regressions. All existing IP types and flow graphs continue to work identically.

# Quickstart Verification: Module Scaffolding Extraction

**Feature**: 078-extract-module-scaffolding
**Date**: 2026-04-22

## Prerequisites

- Branch `078-extract-module-scaffolding` checked out
- Feature 077 (folder hierarchy migration) merged

## Verification Scenarios

### VS1: Scaffolding Generator Unit Tests

**Steps**:
1. Run `./gradlew :flowGraph-generate:jvmTest --tests "*ModuleScaffoldingGenerator*"`

**Expected**: All scaffolding generator tests pass — directory creation, Gradle files, platform-specific source sets.

### VS2: Existing Tests Still Pass

**Steps**:
1. Run `./gradlew :flowGraph-generate:jvmTest`

**Expected**: 100% of existing ModuleSaveServiceTest tests pass — zero regressions.

### VS3: Generate Module via Graph Editor

**Steps**:
1. Launch graph editor
2. Create a flow graph
3. Click "Generate Module"
4. Inspect output

**Expected**: Identical output to pre-refactoring — same files, same structure (flow/, controller/, viewmodel/, userInterface/).

### VS4: Generate Repository Module

**Steps**:
1. Create an IP Type
2. Generate Repository Module
3. Inspect output

**Expected**: Identical output — same entity module files, same directory structure.

### VS5: Scaffolding Without FlowGraph

**Steps**:
1. Call `ModuleScaffoldingGenerator.generate("TestScaffold", tempDir, listOf(KMP_ANDROID, KMP_IOS))` in a test
2. Verify output

**Expected**: Module directory created with commonMain, androidMain, iosMain source sets. build.gradle.kts configures Android and iOS targets. No FlowGraph needed.

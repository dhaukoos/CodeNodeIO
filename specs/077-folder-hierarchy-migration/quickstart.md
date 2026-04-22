# Quickstart Verification: Folder Hierarchy Migration

**Feature**: 077-folder-hierarchy-migration
**Date**: 2026-04-22

## Prerequisites

- Branch `077-folder-hierarchy-migration` checked out
- Demo Project at `../CodeNodeIO-DemoProject`

## Verification Scenarios

### VS1: New Module Uses New Layout

**Steps**:
1. Launch `./gradlew :graphEditor:run`
2. Create a new flow graph, add nodes
3. Click "Generate Module", select output directory
4. Inspect the generated module structure

**Expected**: Files in flow/, controller/, viewmodel/, userInterface/. No `generated/` directory.

### VS2: Generated Files Compile

**Steps**:
1. After VS1, compile the new module
2. Verify no import errors

**Expected**: All generated files compile with correct cross-package imports.

### VS3: StopWatch Migration

**Steps**:
1. Verify StopWatch module has new layout (flow/, controller/, viewmodel/)
2. Run `cd CodeNodeIO-DemoProject && ./gradlew :StopWatch:compileKotlinJvm`

**Expected**: Compiles with zero errors.

### VS4: UserProfiles Migration

**Steps**:
1. Run `cd CodeNodeIO-DemoProject && ./gradlew :UserProfiles:compileKotlinJvm`

**Expected**: Compiles with zero errors.

### VS5: Addresses Migration

**Steps**:
1. Run `cd CodeNodeIO-DemoProject && ./gradlew :Addresses:compileKotlinJvm`

**Expected**: Compiles with zero errors.

### VS6: EdgeArtFilter Migration

**Steps**:
1. Run `cd CodeNodeIO-DemoProject && ./gradlew :EdgeArtFilter:compileKotlinJvm`

**Expected**: Compiles with zero errors.

### VS7: WeatherForecast Migration

**Steps**:
1. Run `cd CodeNodeIO-DemoProject && ./gradlew :WeatherForecast:compileKotlinJvm`

**Expected**: Compiles with zero errors.

### VS8: Graph Editor Loads Migrated Modules

**Steps**:
1. Launch graph editor from demo project
2. Open each module's .flow.kt
3. Verify nodes load in palette

**Expected**: All flow graphs load. All CodeNodes discoverable.

### VS9: Runtime Preview Works

**Steps**:
1. Open a migrated module with Runtime Preview
2. Start execution

**Expected**: Preview discovers composable and executes without errors.

### VS10: Repository Module Generation

**Steps**:
1. Create a new IP Type
2. Use Code Generator panel → Repository path → Generate
3. Inspect output

**Expected**: All repository module files in the new layout (flow/, controller/, viewmodel/, nodes/, userInterface/, persistence/).

### VS11: User Code Preserved

**Steps**:
1. Compare user-authored UI files (e.g., StopWatchScreen.kt) before and after migration
2. Verify byte-identical

**Expected**: No user-authored code modified.

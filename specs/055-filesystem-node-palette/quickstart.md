# Quickstart Validation: Filesystem-Driven Node Palette

**Feature**: 055-filesystem-node-palette
**Date**: 2026-03-19

## Prerequisites

- graphEditor compiles and launches successfully
- At least one module with CodeNodeDefinition files exists (e.g., StopWatch, EdgeArtFilter)

## Validation Steps

### Step 1: Verify CodeNodeType Enum Reduction

**Action**: Check that `CodeNodeType` has exactly 9 values.

**Expected**: The enum in `CodeNode.kt` contains: SOURCE, SINK, TRANSFORMER, FILTER, SPLITTER, MERGER, VALIDATOR, API_ENDPOINT, DATABASE. CUSTOM and GENERIC are absent.

**Verify**: `grep -c "^\s*[A-Z_]*(" fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNode.kt` returns 9 enum entries.

### Step 2: Verify Removed Enums

**Action**: Confirm the two removed enums no longer exist in the codebase.

**Expected**:
- No `enum class NodeCategory` in `CodeNodeDefinition.kt`
- No `enum class NodeCategory` nested in `NodeTypeDefinition.kt`
- No imports of `NodeCategory` anywhere in production code
- No imports of `NodeTypeDefinition.NodeCategory` anywhere in production code

**Verify**:
```bash
grep -r "NodeCategory" --include="*.kt" fbpDsl/src/commonMain/ graphEditor/src/jvmMain/ StopWatch/ UserProfiles/ GeoLocations/ Addresses/ EdgeArtFilter/ nodes/ | grep -v "CodeNodeType" | grep -v "test" | grep -v "Test"
```
Should return zero results.

### Step 3: Verify Palette Shows Only Filesystem Nodes

**Action**: Launch the graphEditor. Inspect the Node Palette.

**Expected**:
- No hardcoded sample nodes (Data Source, Transform, Filter, API Call, Database Query) appear
- Only nodes discovered from Module, Project, and Universal directories appear
- Each node is grouped under its CodeNodeType category header

### Step 4: Verify Palette Shows Only Populated Categories

**Action**: With StopWatch and EdgeArtFilter modules available, launch the graphEditor.

**Expected**:
- Only category headers for types that have at least one discovered node are shown
- Empty categories (e.g., SPLITTER, MERGER, API_ENDPOINT, DATABASE, VALIDATOR) are hidden if no nodes of that type exist
- Category headers display human-readable names (e.g., "Source", "Transformer", "Api Endpoint")

### Step 5: Verify Node Generator Shows All 9 Types

**Action**: Open the Node Generator panel. Click the category dropdown.

**Expected**:
- All 9 CodeNodeType values are listed: Source, Sink, Transformer, Filter, Splitter, Merger, Validator, Api Endpoint, Database
- All 9 are always visible regardless of which types have existing nodes

### Step 6: Verify Node Generation → Palette Flow

**Action**: In the Node Generator, create a new node with type "Filter", name "TestFilter", placement "Project". Generate it.

**Expected**:
- The node file is created on disk
- The Node Palette immediately shows a "Filter" category section (if it wasn't shown before)
- The new "TestFilter" node appears under the "Filter" category
- No restart required

### Step 7: Verify Filesystem Removal → Palette Sync

**Action**: Delete the TestFilter CodeNode file from the Project directory. Relaunch the graphEditor.

**Expected**:
- The "TestFilter" node no longer appears in the palette
- If no other Filter nodes exist, the "Filter" category section is hidden

### Step 8: Verify Backward-Compatible Deserialization

**Action**: Load an existing .flow.kts file that contains `nodeType = "GENERIC"` (e.g., from GeoLocations or Addresses).

**Expected**:
- The file deserializes without errors
- The node's CodeNodeType falls back to TRANSFORMER (not a crash or missing enum)

### Step 9: Verify All Tests Pass

**Action**: Run the full test suite.

```bash
./gradlew :fbpDsl:jvmTest :graphEditor:jvmTest
```

**Expected**: All tests pass. No references to removed enums in test code.

### Step 10: Verify CodeNodeDefinition Implementations

**Action**: Check all module CodeNode files use `CodeNodeType` instead of `NodeCategory`.

**Expected**:
- All files have `import io.codenode.fbpdsl.model.CodeNodeType` (not NodeCategory)
- All files have `override val category = CodeNodeType.SOURCE` (or SINK, TRANSFORMER, etc.)
- No files reference `NodeCategory` or `PROCESSOR`

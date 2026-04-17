# Quickstart Verification: Subscription Tier Feature Flags

**Feature**: 074-subscription-tier-flags
**Date**: 2026-04-17

## Prerequisites

- Branch `074-subscription-tier-flags` checked out
- Gradle builds successfully: `./gradlew :graphEditor:compileKotlinJvm`

## Verification Scenarios

### VS1: Compilation and Test Baseline

**Steps**:
1. Run `./gradlew :fbpDsl:jvmTest` (tier model tests)
2. Run `./gradlew :graphEditor:jvmTest` (editor tests)
3. Run `./gradlew :flowGraph-generate:jvmTest` (generate tests)
4. Verify all tests pass

**Expected**: Zero test failures across all test suites.

### VS2: Save-Only (No Code Generation)

**Steps**:
1. Launch the graph editor: `./gradlew :graphEditor:run`
2. Create a simple flow graph with a generator and a sink
3. Click Save and select an output directory
4. Inspect the output directory

**Expected**: Only the `.flow.kt` file is written. No `build.gradle.kts`, no runtime controller files, no ViewModel, no UI stubs are generated.

### VS3: Generate Module (Separate Action)

**Steps**:
1. With the same flow graph from VS2, click "Generate Module"
2. Inspect the output directory

**Expected**: Full module scaffolding is produced — `build.gradle.kts`, `.flow.kt` (updated), 4 runtime files, ViewModel stub, UI stub. All files are created in the standard KMP module structure.

### VS4: Free Tier — Code Generation Gated

**Steps**:
1. Ensure `~/.codenode/config.properties` has `subscription.tier=FREE` (or remove the file to test default)
2. Launch the graph editor
3. Create a flow graph and Save it
4. Attempt to click "Generate Module"

**Expected**: Save succeeds (`.flow.kt` written). "Generate Module" button is disabled with a tooltip or message indicating it requires Pro tier.

### VS5: Free Tier — Runtime Preview Gated

**Steps**:
1. With Free tier configured, expand the Runtime Preview panel
2. Observe the panel state

**Expected**: The panel shows an upgrade prompt indicating Runtime Preview requires Sim tier. Start/Stop, Pause/Resume, attenuation, and animation controls are not accessible.

### VS6: Free Tier — Node Palette Filtering

**Steps**:
1. With Free tier configured, open the node palette
2. Browse all categories

**Expected**: All standard built-in node configurations (generators, transformers, filters, sinks at all arities) and all custom (user-defined) nodes are visible. Entity/repository nodes are NOT visible.

### VS7: Free Tier — IP Type Palette (No Filtering)

**Steps**:
1. With Free tier configured, open the IP type palette
2. Browse available types

**Expected**: All IP types are shown — built-in (Any, Int, Double, Boolean, String), custom, architecture, and module-level. No tier-based filtering on IP types.

### VS8: Pro Tier — Full Access Except Simulation

**Steps**:
1. Set `subscription.tier=PRO` in `~/.codenode/config.properties`
2. Restart (or verify runtime tier change) the graph editor
3. Verify "Generate Module" is enabled and works
4. Verify all node types and IP types are visible
5. Verify Runtime Preview is still gated

**Expected**: Code generation works. All nodes and IP types are available. Runtime Preview still shows upgrade prompt (Sim required).

### VS9: Sim Tier — Everything Unlocked

**Steps**:
1. Set `subscription.tier=SIM` in `~/.codenode/config.properties`
2. Restart (or verify runtime tier change) the graph editor
3. Verify all features are available

**Expected**: Code generation, all nodes/IP types, and Runtime Preview are all fully functional.

### VS10: Cross-Tier Graph Opening

**Steps**:
1. On Pro tier, create a graph with entity/repository nodes and custom IP types
2. Save the graph
3. Switch to Free tier
4. Open the saved graph

**Expected**: The graph loads and displays completely. Advanced nodes are visible but marked as restricted. The user can view the graph but cannot add new advanced nodes.

### VS11: Backward Compatibility

**Steps**:
1. Open any existing .flow.kt file from a demo project module
2. Verify it loads without errors
3. Save it (should write only .flow.kt)
4. Generate Module (should produce full scaffolding as before)

**Expected**: No regressions. Existing workflow of Save + Generate (now as two steps) produces identical output to the previous monolithic Save.

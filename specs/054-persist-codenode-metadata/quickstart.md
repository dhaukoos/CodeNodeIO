# Quickstart: Persist CodeNode Metadata Through Save Pipeline

## Step 1: Verify Node Placement Retains CodeNode Identity (US2)

1. Launch graphEditor
2. Create a new module or open an existing one (e.g., StopWatch)
3. Open the node palette — all nodes should come from CodeNodeDefinitions
4. Place a node on the canvas (click or drag-and-drop)
5. Inspect the node's configuration — it should contain `_codeNodeClass` with the fully-qualified class name

## Step 2: Verify Save Produces CodeNode-Driven Flow (US1)

1. Open StopWatch module in graphEditor
2. Save the module (Save button)
3. Open the regenerated `StopWatchFlow.kt`
4. Confirm it imports CodeNode objects (e.g., `TimerEmitterCodeNode`)
5. Confirm it uses `XxxCodeNode.createRuntime("NodeName")` pattern
6. Confirm it does NOT import processingLogic tick functions
7. Confirm it does NOT use CodeNodeFactory calls

## Step 3: Verify Re-Save Preserves CodeNode Metadata (US1)

1. Close and re-open the StopWatch module
2. Save again without changes
3. Open the regenerated `StopWatchFlow.kt`
4. Confirm it still uses the CodeNode pattern — metadata survived the round-trip

## Step 4: Verify All 5 Modules (US1)

Repeat Step 2 for: UserProfiles, GeoLocations, Addresses, EdgeArtFilter.

## Step 5: Verify Legacy Infrastructure Removed (US3)

1. Confirm no "Create" button exists in the Node Generator panel (only "Generate CodeNode")
2. Confirm the graphEditor launches without errors even though `~/.codenode/custom-nodes.json` is no longer read
3. Search the source code — zero references to `CustomNodeDefinition`, `FileCustomNodeRepository`, or `CustomNodeRepository` in active code

## Step 6: Build Verification

1. Run `./gradlew :graphEditor:compileKotlinJvm` — must succeed
2. Run `./gradlew :kotlinCompiler:jvmTest` — must succeed
3. Run `./gradlew :fbpDsl:jvmTest` — no new test failures beyond pre-existing ContinuousFactoryTest failures

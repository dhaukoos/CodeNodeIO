# Quickstart: CodeNode-Driven Flow Runtime

## Step 1: Verify StopWatch Generated Flow (US1 - MVP)

1. Open the generated `StopWatchFlow.kt`
2. Confirm it imports `TimerEmitterCodeNode`, `TimeIncrementerCodeNode`, `DisplayReceiverCodeNode` from `io.codenode.stopwatch.nodes`
3. Confirm it does NOT import anything from `io.codenode.stopwatch.processingLogic`
4. Confirm each runtime is created via `XxxCodeNode.createRuntime("NodeName")`
5. Run KMPMobileApp on Android emulator
6. Navigate to StopWatch
7. Press Start — timer should tick (seconds increment, minutes roll at 60)
8. Press Pause — timer stops, press Resume — timer continues
9. Press Stop — timer resets to 0:00

## Step 2: Verify Entity Module Generated Flows (US1)

For each of UserProfiles, GeoLocations, Addresses:

1. Open the generated `{Module}Flow.kt`
2. Confirm it imports CodeNode objects from `io.codenode.{module}.nodes`
3. Confirm it does NOT import `processingLogic` tick functions
4. Confirm it does NOT import `create{Entity}CUD()` or `create{Entity}sDisplay()` stub functions
5. Confirm each runtime is created via `XxxCodeNode.createRuntime("NodeName")`

## Step 3: Verify Dead Code Removed (US3)

1. Confirm no `processingLogic/` directory exists in StopWatch module
2. Confirm no `UserProfileCUD.kt`, `UserProfilesDisplay.kt` files exist in UserProfiles module
3. Confirm no `GeoLocationCUD.kt`, `GeoLocationsDisplay.kt` files exist in GeoLocations module
4. Confirm no `AddressCUD.kt`, `AddressesDisplay.kt` files exist in Addresses module
5. Confirm no `.flow.kt` file contains `import ...processingLogic.*`

## Step 4: Verify graphEditor Dynamic Pipeline (Regression)

1. Launch graphEditor
2. Load StopWatch module
3. Set Attenuation to 1000ms, enable Animated Data Flow
4. Press Start on Runtime Preview — timer should tick with animation
5. Repeat for UserProfiles, GeoLocations, Addresses — CRUD operations should work

## Step 5: Build Verification

1. Run `./gradlew :graphEditor:compileKotlinJvm` — must succeed
2. Run `./gradlew :KMPMobileApp:compileKotlinAndroid` — must succeed (or equivalent Android compile task)
3. Run `./gradlew :fbpDsl:jvmTest` — no new test failures beyond pre-existing ContinuousFactoryTest failures

## Step 6: RuntimeFlowGenerator Verification (US2)

1. Open RuntimeFlowGenerator source
2. Confirm it has a code path for CodeNode-driven generation
3. Confirm it falls back to processingLogic pattern for nodes without CodeNodeDefinitions

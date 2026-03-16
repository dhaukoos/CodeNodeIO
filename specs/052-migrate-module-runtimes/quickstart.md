# Quickstart: Migrate Module Runtimes

**Feature**: 052-migrate-module-runtimes
**Date**: 2026-03-16

## Prerequisites

- Feature 050 (self-contained CodeNode) complete and merged
- Feature 051 (dynamic runtime pipeline) complete and merged
- EdgeArtFilter running via DynamicPipelineController (baseline reference)

## Step 1: Verify Baseline — EdgeArtFilter Still Works

1. Launch the application: `./gradlew :graphEditor:run`
2. Load **EdgeArtFilter** module
3. Press **Start** → Pick Image → verify processed image appears
4. Enable speed attenuation (slider > 200ms) → enable **Animate Data Flow** → verify dot animations
5. **Stop** → Swap GrayscaleTransformer for SepiaTransformer → rewire → **Start** → verify different output
6. Confirm: EdgeArtFilter uses DynamicPipelineController (dynamic pipeline path)

## Step 2: StopWatch Migration Verification

1. Load **StopWatch** module
2. Press **Start** → verify timer ticks (seconds increment, minutes roll over at 60)
3. Press **Pause** → verify timer freezes → press **Resume** → verify timer continues from paused value
4. Press **Stop** → verify timer returns to idle → press **Start** → verify timer resets to 0:00
5. Enable speed attenuation → enable Animate Data Flow → verify dot animations on connections
6. Confirm: StopWatch uses DynamicPipelineController (not fallback generated controller)

## Step 3: UserProfiles Migration Verification

1. Load **UserProfiles** module
2. Press **Start**
3. Add a new profile → verify it appears in the display list and is persisted to database
4. Update an existing profile → verify the display refreshes with updated data
5. Remove a profile → verify it disappears from the display list
6. Attempt an invalid operation → verify error message appears without crashing the pipeline
7. Enable speed attenuation → enable Animate Data Flow → verify dot animations
8. Confirm: UserProfiles uses DynamicPipelineController

## Step 4: GeoLocations Migration Verification

1. Load **GeoLocations** module
2. Press **Start**
3. Add a new location → verify it appears in the display and is persisted
4. Update a location → verify the display refreshes
5. Remove a location → verify it disappears from the display
6. Enable speed attenuation → enable Animate Data Flow → verify dot animations
7. Confirm: GeoLocations uses DynamicPipelineController

## Step 5: Addresses Migration Verification

1. Load **Addresses** module
2. Press **Start**
3. Add a new address → verify it appears in the display and is persisted
4. Update an address → verify the display refreshes
5. Remove an address → verify it disappears from the display
6. Enable speed attenuation → enable Animate Data Flow → verify dot animations
7. Confirm: Addresses uses DynamicPipelineController

## Step 6: Cross-Module Integration

1. Load **StopWatch** → Start → verify ticking
2. Switch to **UserProfiles** → Start → verify previous module stops cleanly, new module runs
3. Switch to **GeoLocations** → Start → perform CRUD → verify works
4. Switch to **Addresses** → Start → perform CRUD → verify works
5. Switch to **EdgeArtFilter** → Start → verify dynamic pipeline still works
6. Cycle through all 5 modules — no regressions, no crashes

## Step 7: Build Verification

```bash
./gradlew :fbpDsl:jvmTest
./gradlew :graphEditor:run
```

All existing tests must pass with no new failures.

## Expected Node Registration (SC-003)

After migration, the NodeDefinitionRegistry should contain all 12 new nodes plus the existing EdgeArtFilter nodes:

| Module | Node | Category |
|--------|------|----------|
| StopWatch | TimerEmitter | SOURCE |
| StopWatch | TimeIncrementer | PROCESSOR |
| StopWatch | DisplayReceiver | SINK |
| UserProfiles | UserProfileCUD | SOURCE |
| UserProfiles | UserProfileRepository | PROCESSOR |
| UserProfiles | UserProfilesDisplay | SINK |
| GeoLocations | GeoLocationCUD | SOURCE |
| GeoLocations | GeoLocationRepository | PROCESSOR |
| GeoLocations | GeoLocationsDisplay | SINK |
| Addresses | AddressCUD | SOURCE |
| Addresses | AddressRepository | PROCESSOR |
| Addresses | AddressesDisplay | SINK |

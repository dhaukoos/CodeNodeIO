# Implementation Plan: CodeNode-Driven Flow Runtime

**Branch**: `053-codenode-flow-runtime` | **Date**: 2026-03-17 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/053-codenode-flow-runtime/spec.md`

## Summary

Restructure generated Flow files to create node runtimes from CodeNodeDefinition objects instead of importing processingLogic tick functions and CUD/Display factory functions. This makes CodeNodes the single source of truth for node behavior, eliminating the parallel processingLogic and stub files. The RuntimeFlowGenerator in kotlinCompiler is updated to produce CodeNode-driven Flows for modules with full CodeNodeDefinition coverage.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: kotlinx-coroutines 1.8.0, Compose Multiplatform 1.7.3, Room 2.8.4 (KMP), Koin 4.0.0
**Storage**: Room (KMP) with BundledSQLiteDriver for entity modules; N/A for StopWatch
**Testing**: kotlinx-coroutines-test, kotlin.test (commonTest)
**Target Platform**: JVM (graphEditor), Android, iOS (KMPMobileApp)
**Project Type**: KMP multi-module
**Constraints**: CodeNodeDefinition objects must be in commonMain for cross-platform access

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Licensing | PASS | No new dependencies. All existing deps are Apache 2.0 / MIT |
| Code Quality | PASS | Eliminates code duplication (single source of truth). Improves maintainability |
| TDD | N/A | Spec does not request new tests. Existing tests must pass (SC-005) |
| Type Safety | PASS | CodeNodeDefinition.createRuntime() returns typed NodeRuntime |
| Security | N/A | No user input, no network, no secrets |

## Project Structure

### Documentation (this feature)

```text
specs/053-codenode-flow-runtime/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
# Files MODIFIED (generated Flow files restructured):
StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/generated/StopWatchFlow.kt
UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/generated/UserProfilesFlow.kt
GeoLocations/src/commonMain/kotlin/io/codenode/geolocations/generated/GeoLocationsFlow.kt
Addresses/src/commonMain/kotlin/io/codenode/addresses/generated/AddressesFlow.kt

# Files MODIFIED (unused imports removed):
StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/StopWatch.flow.kt
UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/UserProfiles.flow.kt
GeoLocations/src/commonMain/kotlin/io/codenode/geolocations/GeoLocations.flow.kt
Addresses/src/commonMain/kotlin/io/codenode/addresses/Addresses.flow.kt

# Files MODIFIED (generator updated):
kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RuntimeFlowGenerator.kt

# Files DELETED (processingLogic — logic now in CodeNodes):
StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/processingLogic/TimeIncrementerProcessLogic.kt

# Files DELETED (CUD/Display stubs — logic now in CodeNodes):
UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/UserProfileCUD.kt
UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/UserProfilesDisplay.kt
GeoLocations/src/commonMain/kotlin/io/codenode/geolocations/GeoLocationCUD.kt
GeoLocations/src/commonMain/kotlin/io/codenode/geolocations/GeoLocationsDisplay.kt
Addresses/src/commonMain/kotlin/io/codenode/addresses/AddressCUD.kt
Addresses/src/commonMain/kotlin/io/codenode/addresses/AddressesDisplay.kt

# Files NOT MODIFIED (CodeNode definitions — already complete):
StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/nodes/*.kt
UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/nodes/*.kt
GeoLocations/src/commonMain/kotlin/io/codenode/geolocations/nodes/*.kt
Addresses/src/commonMain/kotlin/io/codenode/addresses/nodes/*.kt
```

**Structure Decision**: This is a refactoring of existing generated files. No new directories or modules are created. The `processingLogic/` directories and CUD/Display stub files are deleted once the generated Flow files no longer depend on them.

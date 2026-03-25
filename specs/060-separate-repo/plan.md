# Implementation Plan: Separate Project Repository from Tool Repository

**Branch**: `060-separate-repo` | **Date**: 2026-03-25 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/060-separate-repo/spec.md`

## Summary

Split the CodeNodeIO monorepo into two repositories: the tool (graphEditor, fbpDsl, kotlinCompiler, circuitSimulator) stays in `CodeNodeIO`, and the project modules (Addresses, EdgeArtFilter, GeoLocations, StopWatch, UserProfiles, WeatherForecast, KMPMobileApp, persistence, nodes/, iptypes/) move to a new `CodeNodeIO-DemoProject` repository. The separation requires removing 63 hardcoded imports across 8 graphEditor files and converting them to runtime discovery. The new repository uses Gradle composite builds to depend on fbpDsl during the interim period before it's published.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3
**Primary Dependencies**: Gradle 8.13 (build system), git filter-repo (history extraction), Koin 4.0.0 (DI)
**Storage**: Git repositories (GitHub), Gradle composite builds
**Testing**: Existing module tests in both repositories
**Target Platform**: JVM Desktop (graphEditor), KMP (project modules), Android/iOS (KMPMobileApp)
**Project Type**: Multi-repository, multi-module KMP project
**Performance Goals**: N/A (repository restructuring, no runtime changes)
**Constraints**: Must preserve git history for all moved modules; both repos must build independently
**Scale/Scope**: ~10 modules moved, ~8 files refactored in graphEditor, 2 new build configurations

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Separation improves modularity and enforces clean boundaries between tool and project. |
| II. Test-Driven Development | PASS | Existing tests preserved in both repositories; both must pass independently. |
| III. User Experience Consistency | PASS | graphEditor functions identically after separation when pointed at the project directory. |
| IV. Performance Requirements | N/A | No runtime changes — this is a repository structure change. |
| V. Observability & Debugging | PASS | Clear error messages when project directory is not configured. |
| Licensing | PASS | No new dependencies. git filter-repo is MIT licensed. |

**Post-Phase 1 re-check**: All gates still pass.

## Project Structure

### Documentation (this feature)

```text
specs/060-separate-repo/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── module-discovery.md
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code — Tool Repository (CodeNodeIO) after separation

```text
CodeNodeIO/
├── fbpDsl/                    # Core DSL library
├── graphEditor/               # Visual editor (JVM Desktop)
│   └── src/jvmMain/kotlin/
│       ├── Main.kt            # Refactored: runtime discovery, no project module imports
│       ├── ui/
│       │   └── ModuleSessionFactory.kt  # Refactored: discovery-based session creation
│       └── state/
│           ├── IPTypeDiscovery.kt       # Already discovery-based
│           └── NodeDefinitionRegistry.kt # Already discovery-based
├── kotlinCompiler/            # Code generation engine
├── circuitSimulator/          # Runtime animation engine
├── goCompiler/                # Go code generation
├── idePlugin/                 # IDE integration
├── .specify/                  # Speckit tooling
└── specs/                     # Feature specifications
```

### Source Code — Project Repository (CodeNodeIO-DemoProject) after separation

```text
CodeNodeIO-DemoProject/
├── Addresses/                 # CRUD addresses module
├── EdgeArtFilter/             # Image processing pipeline module
├── GeoLocations/              # CRUD geo locations module
├── StopWatch/                 # Timer demo module
├── UserProfiles/              # CRUD user profiles module
├── WeatherForecast/           # Weather API demo module
├── KMPMobileApp/              # Kotlin Multiplatform mobile app
├── persistence/               # Shared Room database module
├── nodes/                     # Project-level shared nodes
├── iptypes/                   # Project-level shared IP types
├── settings.gradle.kts        # New: includes all project modules + composite build for fbpDsl
├── build.gradle.kts           # New: shared build config
└── README.md                  # Setup instructions
```

**Structure Decision**: Two separate Git repositories, each with independent Gradle build configurations. The project repository uses Gradle `includeBuild("../CodeNodeIO")` for fbpDsl during the interim period, switchable to a Maven coordinate when fbpDsl is published.

## Complexity Tracking

No constitution violations. The split follows standard multi-repository patterns.

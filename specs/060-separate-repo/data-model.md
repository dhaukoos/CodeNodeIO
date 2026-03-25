# Data Model: Separate Project Repository from Tool Repository

**Feature**: 060-separate-repo
**Date**: 2026-03-25

## Entities

### Tool Repository (CodeNodeIO)

Post-separation contents:

| Module | Purpose | Dependencies |
|--------|---------|-------------|
| fbpDsl | Flow-based programming DSL library | kotlinx-coroutines, kotlinx-serialization |
| graphEditor | Visual flow graph editor (JVM Desktop) | fbpDsl, circuitSimulator, kotlinCompiler |
| kotlinCompiler | Kotlin code generation engine | fbpDsl, KotlinPoet |
| circuitSimulator | Runtime animation engine | fbpDsl, Compose |
| goCompiler | Go code generation (if used) | fbpDsl |
| idePlugin | IDE integration plugin | fbpDsl |

### Project Repository (CodeNodeIO-DemoProject)

Post-separation contents:

| Module | Purpose | Dependencies |
|--------|---------|-------------|
| KMPMobileApp | Kotlin Multiplatform mobile app | fbpDsl, StopWatch, UserProfiles, persistence |
| StopWatch | Timer demo module | fbpDsl |
| UserProfiles | CRUD user profiles module | fbpDsl, persistence |
| GeoLocations | CRUD geo locations module | fbpDsl, persistence |
| Addresses | CRUD addresses module | fbpDsl, persistence |
| EdgeArtFilter | Image processing pipeline module | fbpDsl |
| WeatherForecast | Weather API demo module | fbpDsl |
| persistence | Shared Room database module | Room, SQLite |
| nodes/ | Project-level shared node files | fbpDsl |
| iptypes/ | Project-level shared IP type files | (no compile dependency) |

## Relationships

```text
Tool Repository                    Project Repository
===============                    ==================
graphEditor ──depends──> fbpDsl    KMPMobileApp ──depends──> fbpDsl (composite build)
kotlinCompiler ──depends──> fbpDsl StopWatch ──depends──> fbpDsl
circuitSimulator ──> fbpDsl        UserProfiles ──depends──> fbpDsl, persistence
                                   GeoLocations ──depends──> fbpDsl, persistence
                                   Addresses ──depends──> fbpDsl, persistence
                                   EdgeArtFilter ──depends──> fbpDsl
                                   WeatherForecast ──depends──> fbpDsl
                                   persistence ──depends──> Room, SQLite

graphEditor ──discovers-at-runtime──> Project modules (via classpath scanning)
graphEditor ──reads──> Project .flow.kt files (via FlowKtParser)
graphEditor ──writes──> Project module directories (via code generation)
```

## Files to Remove from Tool Repository

After separation, these directories/files are removed from the tool repo:

- `Addresses/`
- `EdgeArtFilter/`
- `GeoLocations/`
- `StopWatch/`
- `UserProfiles/`
- `WeatherForecast/`
- `KMPMobileApp/`
- `persistence/`
- `nodes/`
- `iptypes/`
- Their entries in `settings.gradle.kts`
- Their `project(":...")` dependencies in `graphEditor/build.gradle.kts`

## Files to Modify in Tool Repository

| File | Change |
|------|--------|
| `settings.gradle.kts` | Remove module includes for all project modules |
| `graphEditor/build.gradle.kts` | Remove `project(":...")` dependencies for project modules |
| `Main.kt` | Remove all 27 hardcoded project module imports; rely on runtime discovery |
| `ModuleSessionFactory.kt` | Remove all 24 hardcoded project module imports; convert to discovery-based |
| 6 PreviewProvider files | Move to project repo or remove (preview registration becomes module-side) |

# Implementation Plan: Weather Forecast Demo

**Branch**: `058-weather-forecast-demo` | **Date**: 2026-03-23 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/058-weather-forecast-demo/spec.md`

## Summary

Create a WeatherForecast module that demonstrates a data-fetching FBP pipeline: TriggerSource → HttpFetcher → JsonParser → DataMapper → [ListDisplay, ChartDisplay]. Uses Ktor Client (KMP) for HTTPS GET requests to the Open-Meteo API (no API key required), kotlinx-serialization for JSON parsing, and Compose Canvas for chart rendering. Defines 5 custom IP Types for distinct data streams with color-coded connections.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: Compose Desktop 1.7.3, kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, lifecycle-viewmodel-compose 2.8.0, Ktor Client 3.1.1 (ktor-client-core, ktor-client-cio, ktor-client-content-negotiation, ktor-serialization-kotlinx-json)
**Storage**: N/A (in-memory state only)
**Testing**: Kotlin Multiplatform tests (commonTest), JVM tests
**Target Platform**: JVM Desktop (preview/debug target); fully KMP-portable
**Project Type**: KMP module (same pattern as StopWatch, EdgeArtFilter)
**Performance Goals**: Forecast data displayed within 10 seconds of trigger
**Constraints**: No API key required, KMP-only dependencies (no java.net, no JVM-only libraries)
**Scale/Scope**: 5 CodeNode definitions, 5 custom IP Types, 1 ViewModel, 1 PreviewProvider

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Licensing (Apache 2.0 / MIT / BSD-3) | ✅ PASS | Ktor Client: Apache 2.0. kotlinx-serialization: Apache 2.0. All existing deps: Apache 2.0. |
| No GPL/LGPL in KMP common/native | ✅ PASS | No GPL/LGPL dependencies introduced. |
| Type Safety (public interfaces typed) | ✅ PASS | All IP Types have explicit property definitions. All node inputs/outputs typed via CodeNodeDefinition. |
| Code Quality (single responsibility) | ✅ PASS | Each node has a single responsibility (trigger, fetch, parse, map, display). |
| Security (input validation at boundaries) | ✅ PASS | HTTP response status checked. JSON parsing handles malformed input. Coordinates validated by API. |

**Post-Phase 1 Re-check**: All gates still PASS. Ktor Client is Apache 2.0, fully KMP-compatible.

## Project Structure

### Documentation (this feature)

```text
specs/058-weather-forecast-demo/
├── plan.md              # This file
├── research.md          # Phase 0 output — 8 decisions
├── data-model.md        # Phase 1 output — IP Types, data classes, state model
├── quickstart.md        # Phase 1 output — 6 test scenarios
└── tasks.md             # Phase 2 output (via /speckit.tasks)
```

### Source Code (repository root)

```text
WeatherForecast/
├── build.gradle.kts                        # Module build with Ktor Client dependencies
└── src/
    └── commonMain/
        └── kotlin/
            └── io/
                └── codenode/
                    └── weatherforecast/
                        ├── WeatherForecast.flow.kt          # FlowGraph definition (node + connection declarations)
                        ├── WeatherForecastState.kt           # Singleton state object with MutableStateFlow properties
                        ├── WeatherForecastViewModel.kt       # ViewModel observing state for Compose UI
                        ├── WeatherForecastPreviewProvider.kt # PreviewProvider for RuntimePreview integration
                        ├── models/
                        │   ├── ForecastEntry.kt              # Display entry data class
                        │   ├── ChartData.kt                  # Chart-ready data class
                        │   └── OpenMeteoResponse.kt          # @Serializable response models for Open-Meteo JSON
                        ├── nodes/
                        │   ├── TriggerSourceCodeNode.kt      # SOURCE: emits Coordinates on manual trigger
                        │   ├── HttpFetcherCodeNode.kt        # TRANSFORMER: Ktor Client HTTPS GET
                        │   ├── JsonParserCodeNode.kt         # TRANSFORMER: kotlinx-serialization JSON parse
                        │   ├── DataMapperCodeNode.kt         # TRANSFORMER (1-in 2-out): fan-out to list + chart
                        │   └── ForecastDisplayCodeNode.kt    # SINK (2-in anyInput): updates state
                        ├── processingLogic/
                        │   ├── TriggerSourceProcessingLogic.kt
                        │   ├── HttpFetcherProcessingLogic.kt
                        │   ├── JsonParserProcessingLogic.kt
                        │   ├── DataMapperProcessingLogic.kt
                        │   └── ForecastDisplayProcessingLogic.kt
                        └── userInterface/
                            ├── WeatherForecastUI.kt          # Main composable (list + chart layout)
                            └── ForecastChart.kt              # Compose Canvas line chart

# Modified existing files:
graphEditor/src/jvmMain/kotlin/Main.kt       # Register WeatherForecast nodes, IP Types, scanDirectory
settings.gradle.kts                           # Include WeatherForecast module
graphEditor/build.gradle.kts                  # Add WeatherForecast module dependency
```

**Structure Decision**: Single KMP module (`WeatherForecast/`) following the established pattern of StopWatch and EdgeArtFilter. The `commonMain` source set ensures full KMP portability. Ktor Client with CIO engine for JVM/Desktop; platform-specific engines can be swapped for other targets.

## Complexity Tracking

No constitution violations to justify. All gates pass cleanly.

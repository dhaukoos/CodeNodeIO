# Research: Weather Forecast Demo

**Feature**: 058-weather-forecast-demo
**Date**: 2026-03-23

---

## Decision 1: HTTP Client Library

**Decision**: Use Ktor Client (KMP) for HTTPS GET requests. All modules must be fully KMP-compliant — JVM Desktop is only the preview/debug target, not the deployment boundary. Dependencies: `ktor-client-core`, `ktor-client-cio` (JVM engine), `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`. License: Apache 2.0 (OK).

**Rationale**: The project's implicit goal is that all modules are fully KMP-portable (Android, iOS, Desktop, Web). Using `java.net.HttpURLConnection` would lock the HTTP Client node to JVM only, violating this principle. Ktor Client is the standard KMP HTTP library from JetBrains, provides coroutine-native async I/O, and integrates with kotlinx-serialization for JSON content negotiation. The CIO engine works on JVM/Desktop; platform-specific engines (Darwin, JS) can be swapped in later.

**Alternatives rejected**:
- **java.net.HttpURLConnection**: JVM-only, breaks KMP portability.
- **OkHttp**: JVM-only, same portability issue.
- **java.net.URI + HttpClient (Java 11+)**: JVM-only, not available on all KMP targets.

---

## Decision 2: JSON Parsing Approach

**Decision**: Use `kotlinx-serialization-json` (already a project dependency) for parsing the Open-Meteo response.

**Rationale**: The project already uses kotlinx-serialization 1.6.0 across all modules. Parsing the weather JSON response with `@Serializable` data classes provides type safety and is consistent with existing patterns. No new dependencies needed.

**Alternatives considered**:
- **Manual string parsing**: Error-prone, no type safety, hard to maintain.
- **Gson/Moshi**: JVM-only alternatives, would add unnecessary dependencies. License: Apache 2.0 (OK).

---

## Decision 3: Chart Rendering

**Decision**: Use Compose Canvas (`drawLine`, `drawCircle`, `drawText`) to render a simple line chart, following the same pattern used by StopWatch's analog clock.

**Rationale**: The project already uses Compose Canvas for custom drawing (StopWatch clock face). A simple line chart (7 data points, labeled axes) is straightforward to implement with `Canvas` composable. No charting library needed.

**Alternatives considered**:
- **External charting library** (e.g., compose-charts, YCharts): Would add dependencies and complexity. Most KMP-compatible options are immature. License concerns would need evaluation.
- **ASCII/Text chart**: Too basic for a visual demo.

---

## Decision 4: Module Structure

**Decision**: Create a `WeatherForecast/` top-level module following the exact pattern of StopWatch — `commonMain` source set with state object, CodeNodeDefinition nodes, ViewModel, and Compose UI.

**Rationale**: All existing demo modules (StopWatch, UserProfiles, GeoLocations, Addresses, EdgeArtFilter) follow this exact pattern. Consistency reduces cognitive load and ensures compatibility with existing graphEditor infrastructure (PreviewProvider, RuntimePreview, node registration).

---

## Decision 5: State Management

**Decision**: Use a `WeatherForecastState` singleton object with `MutableStateFlow` properties, matching the `StopWatchState` pattern.

**Rationale**: The StopWatch pattern of `object State` with `MutableStateFlow` properties is the established convention for module state. Source nodes emit from state, sink nodes update state, and the ViewModel observes state for UI rendering.

State properties:
- `_forecastEntries: MutableStateFlow<List<ForecastEntry>>` — parsed forecast data for list display
- `_chartData: MutableStateFlow<ChartData?>` — chart-ready data (labels + values)
- `_errorMessage: MutableStateFlow<String?>` — error state for display
- `_isLoading: MutableStateFlow<Boolean>` — loading indicator
- `_latitude: MutableStateFlow<Double>` — configurable location (default: 40.16)
- `_longitude: MutableStateFlow<Double>` — configurable location (default: -105.10)

---

## Decision 6: Node Pipeline Design

**Decision**: 5 CodeNode definitions with the following pipeline:

```
TriggerSource → HttpFetcher → JsonParser → DataMapper → [ListDisplay, ChartDisplay]
```

**Node definitions**:

1. **TriggerSourceCodeNode** (SOURCE, 0 in, 1 out)
   - Outputs: `coordinates: Any` (Coordinates IP Type)
   - Reads lat/lon from WeatherForecastState, emits on manual trigger signal

2. **HttpFetcherCodeNode** (TRANSFORMER, 1 in, 1 out)
   - Input: `coordinates: Any` (Coordinates)
   - Output: `response: Any` (HttpResponse)
   - Constructs Open-Meteo URL from coordinates, performs HTTPS GET

3. **JsonParserCodeNode** (TRANSFORMER, 1 in, 1 out)
   - Input: `response: Any` (HttpResponse)
   - Output: `forecastData: Any` (ForecastData)
   - Parses JSON into structured forecast data

4. **DataMapperCodeNode** (TRANSFORMER, 1 in, 2 out — In1Out2Runtime)
   - Input: `forecastData: Any` (ForecastData)
   - Output 1: `displayList: Any` (ForecastDisplayList)
   - Output 2: `chartData: Any` (ForecastChartData)
   - Formats data for list view and chart view

5. **ForecastDisplayCodeNode** (SINK, 2 in, 0 out — In2SinkRuntime with anyInput)
   - Input 1: `displayList: Any` (ForecastDisplayList)
   - Input 2: `chartData: Any` (ForecastChartData)
   - Updates WeatherForecastState with display data

**Rationale**: This pipeline mirrors the user's original description. Using In1Out2Runtime for the DataMapper demonstrates fan-out. The single sink with `anyInput: true` collects both display streams, matching the entity module pattern (e.g., UserProfilesDisplayCodeNode).

---

## Decision 7: Open-Meteo API Details

**Decision**: Use the Open-Meteo forecast endpoint with daily aggregation.

**API URL pattern**:
```
https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}&daily=temperature_2m_max,temperature_2m_min&timezone=auto
```

**Response structure** (relevant fields):
```json
{
  "daily": {
    "time": ["2026-03-23", "2026-03-24", ...],
    "temperature_2m_max": [15.2, 18.1, ...],
    "temperature_2m_min": [3.4, 5.6, ...]
  },
  "daily_units": {
    "temperature_2m_max": "°C",
    "temperature_2m_min": "°C"
  }
}
```

**Rationale**: Open-Meteo is free, requires no API key, returns clean JSON, and provides exactly the data needed (7-day daily highs/lows). The `timezone=auto` parameter ensures dates align with the location.

---

## Decision 8: Custom IP Type Definitions

**Decision**: Register 5 custom IP Types in Main.kt alongside existing IP type registration.

| IP Type | ID | Color | Properties |
|---------|----|-------|------------|
| Coordinates | ip_coordinates | Teal (0, 150, 136) | latitude: Double, longitude: Double |
| HttpResponse | ip_httpresponse | Orange (255, 152, 0) | statusCode: Int, body: String |
| ForecastData | ip_forecastdata | Blue (33, 150, 243) | dates: String, maxTemps: String, minTemps: String |
| ForecastDisplayList | ip_forecastdisplaylist | Green (76, 175, 80) | entries: String |
| ForecastChartData | ip_forecastchartdata | Purple (156, 39, 176) | labels: String, values: String |

**Rationale**: Each IP Type gets a distinct color from the existing palette, making the flow visually informative on the canvas. Properties are typed as String where arrays are serialized, since the IP Type property system uses primitive type references.

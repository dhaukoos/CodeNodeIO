# Tasks: Weather Forecast Demo

**Input**: Design documents from `/specs/058-weather-forecast-demo/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization, module structure, and Ktor Client dependencies

- [X] T001 Create WeatherForecast module directory structure and `WeatherForecast/build.gradle.kts` with KMP config, Ktor Client dependencies (ktor-client-core, ktor-client-cio, ktor-client-content-negotiation, ktor-serialization-kotlinx-json), Compose, kotlinx-serialization, and lifecycle-viewmodel-compose — following StopWatch/build.gradle.kts pattern
- [X] T002 Add `include(":WeatherForecast")` to `settings.gradle.kts` and add `implementation(project(":WeatherForecast"))` dependency to `graphEditor/build.gradle.kts`
- [X] T003 [P] Create `WeatherForecastState.kt` singleton object with MutableStateFlow properties (forecastEntries, chartData, errorMessage, isLoading, latitude, longitude) at `WeatherForecast/src/commonMain/kotlin/io/codenode/weatherforecast/WeatherForecastState.kt`
- [X] T004 [P] Create data model classes: `ForecastEntry.kt` and `ChartData.kt` at `WeatherForecast/src/commonMain/kotlin/io/codenode/weatherforecast/models/`
- [X] T005 [P] Create `OpenMeteoResponse.kt` with `@Serializable` data classes for the Open-Meteo JSON response at `WeatherForecast/src/commonMain/kotlin/io/codenode/weatherforecast/models/OpenMeteoResponse.kt`

**Checkpoint**: Module compiles, state and models ready for node implementation

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Register custom IP Types and scan WeatherForecast directory in Main.kt

**CRITICAL**: No user story work can begin until IP Types are registered

- [X] T006 Register 5 custom IP Types in `graphEditor/src/jvmMain/kotlin/Main.kt`: Coordinates (Teal), HttpResponse (Orange), ForecastData (Blue), ForecastDisplayList (Green), ForecastChartData (Purple) — each with appropriate properties per data-model.md
- [X] T007 Add WeatherForecast node directory scanning via `registry.scanDirectory()` call in `graphEditor/src/jvmMain/kotlin/Main.kt` for `WeatherForecast/src/commonMain/kotlin/io/codenode/weatherforecast/nodes`

**Checkpoint**: IP Types visible in palette, module directory scanned — node implementation can begin

---

## Phase 3: User Story 1 — Fetch and Display Weekly Weather Forecast (Priority: P1) MVP

**Goal**: Complete pipeline from trigger through HTTP fetch, JSON parse, data mapping, to list display

**Independent Test**: Open WeatherForecast module, trigger execution, verify 7-day temperature list appears within 10 seconds

### Implementation for User Story 1

- [ ] T008 [P] [US1] Create `TriggerSourceCodeNode.kt` (SOURCE, 0-in 1-out) at `WeatherForecast/src/commonMain/kotlin/io/codenode/weatherforecast/nodes/` — reads lat/lon from WeatherForecastState, emits Coordinates as `Any` using `CodeNodeFactory.createContinuousSource`. Output port: `coordinates`
- [ ] T009 [P] [US1] Create `HttpFetcherCodeNode.kt` (TRANSFORMER, 1-in 1-out) at `WeatherForecast/src/commonMain/kotlin/io/codenode/weatherforecast/nodes/` — receives Coordinates, constructs Open-Meteo URL, performs HTTPS GET using Ktor Client, outputs HttpResponse as `Any` using `CodeNodeFactory.createContinuousTransformer`. Input: `coordinates`, Output: `response`
- [ ] T010 [P] [US1] Create `JsonParserCodeNode.kt` (TRANSFORMER, 1-in 1-out) at `WeatherForecast/src/commonMain/kotlin/io/codenode/weatherforecast/nodes/` — receives HttpResponse, parses JSON body using kotlinx-serialization into ForecastData, outputs as `Any` using `CodeNodeFactory.createContinuousTransformer`. Input: `response`, Output: `forecastData`
- [ ] T011 [US1] Create `DataMapperCodeNode.kt` (TRANSFORMER, 1-in 2-out) at `WeatherForecast/src/commonMain/kotlin/io/codenode/weatherforecast/nodes/` — receives ForecastData, maps to ForecastDisplayList (output1) and ForecastChartData (output2) using `CodeNodeFactory.createIn1Out2Processor` with `ProcessResult2.both()`. Input: `forecastData`, Outputs: `displayList`, `chartData`
- [ ] T012 [US1] Create `ForecastDisplayCodeNode.kt` (SINK, 2-in 0-out with anyInput) at `WeatherForecast/src/commonMain/kotlin/io/codenode/weatherforecast/nodes/` — receives ForecastDisplayList and ForecastChartData, updates WeatherForecastState using `CodeNodeFactory.createSinkIn2Any`. Inputs: `displayList`, `chartData`
- [ ] T013 [US1] Create `WeatherForecast.flow.kt` FlowGraph definition at `WeatherForecast/src/commonMain/kotlin/io/codenode/weatherforecast/` — defines all 5 nodes, connects TriggerSource→HttpFetcher→JsonParser→DataMapper→ForecastDisplay with custom IP type IDs on each connection
- [ ] T014 [US1] Create `WeatherForecastViewModel.kt` at `WeatherForecast/src/commonMain/kotlin/io/codenode/weatherforecast/` — exposes forecastEntries, errorMessage, isLoading state flows; provides start/stop/reset/refresh methods
- [ ] T015 [US1] Create `WeatherForecastUI.kt` main composable at `WeatherForecast/src/commonMain/kotlin/io/codenode/weatherforecast/userInterface/` — renders forecast list (scrollable Column of ForecastEntry items showing date, high, low, unit), loading indicator, error message, and Refresh button
- [ ] T016 [US1] Create `WeatherForecastPreviewProvider.kt` at `WeatherForecast/src/commonMain/kotlin/io/codenode/weatherforecast/` — implements PreviewProvider interface following StopWatch pattern, registers with RuntimePreview
- [ ] T017 [US1] Register WeatherForecast nodes and PreviewProvider in `graphEditor/src/jvmMain/kotlin/Main.kt` — import and register all 5 CodeNode definitions and call `WeatherForecastPreviewProvider.register()`

**Checkpoint**: Full pipeline works — trigger fetches weather data and displays 7-day forecast list

---

## Phase 4: User Story 2 — Visualize Forecast as Temperature Chart (Priority: P2)

**Goal**: Render a line chart of daily high temperatures using Compose Canvas

**Independent Test**: Trigger execution, verify line chart renders 7 data points with labeled axes

### Implementation for User Story 2

- [ ] T018 [US2] Create `ForecastChart.kt` Compose Canvas line chart at `WeatherForecast/src/commonMain/kotlin/io/codenode/weatherforecast/userInterface/` — draws line chart using Canvas composable (drawLine, drawCircle, drawText) with X-axis date labels, Y-axis temperature values, and unit label. Receives ChartData from ViewModel
- [ ] T019 [US2] Integrate chart into `WeatherForecastUI.kt` — add ForecastChart composable below or alongside the list view, fed by chartData StateFlow from ViewModel

**Checkpoint**: Both list and chart display consistent data from the same fetch

---

## Phase 5: User Story 3 — Configurable Location for Forecast (Priority: P3)

**Goal**: Allow user to change latitude/longitude via node properties and re-fetch

**Independent Test**: Change coordinates in TriggerSource properties panel, trigger refresh, verify new location's forecast displays

### Implementation for User Story 3

- [ ] T020 [US3] Update `TriggerSourceCodeNode.kt` to expose latitude and longitude as configurable node properties (using the properties panel pattern from existing nodes) at `WeatherForecast/src/commonMain/kotlin/io/codenode/weatherforecast/nodes/TriggerSourceCodeNode.kt`
- [ ] T021 [US3] Add latitude/longitude input fields to `WeatherForecastUI.kt` preview — allow manual entry of coordinates with "Apply" action that updates WeatherForecastState latitude/longitude before next refresh

**Checkpoint**: Users can change location and see forecast for different cities

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Error handling, build verification, and quickstart validation

- [ ] T022 [P] Add network error handling to `HttpFetcherCodeNode.kt` — catch Ktor exceptions, set errorMessage in WeatherForecastState, handle non-200 status codes gracefully
- [ ] T023 [P] Add JSON parsing error handling to `JsonParserCodeNode.kt` — catch SerializationException, set errorMessage in WeatherForecastState
- [ ] T024 Verify build compiles with `./gradlew :WeatherForecast:build :graphEditor:build`
- [ ] T025 Run quickstart.md scenarios 1-6 manually in graphEditor to validate end-to-end functionality

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on T001-T002 (module exists in build) — BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Phase 1 + Phase 2 completion
- **User Story 2 (Phase 4)**: Depends on Phase 3 (needs ChartData flowing from DataMapper)
- **User Story 3 (Phase 5)**: Depends on Phase 3 (needs TriggerSource node working)
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Phase 2 — no dependencies on other stories
- **User Story 2 (P2)**: Depends on US1 (chart data comes from DataMapper already built in US1; ForecastDisplayCodeNode already receives chartData)
- **User Story 3 (P3)**: Depends on US1 (needs TriggerSource node to modify its properties)

### Within User Story 1

- T008, T009, T010 can run in parallel (independent node files)
- T011 (DataMapper) depends on understanding T008-T010 output types
- T012 (ForecastDisplay) depends on T011 output types
- T013 (FlowGraph) depends on T008-T012 (all nodes must exist)
- T014 (ViewModel) depends on T003 (state)
- T015 (UI) depends on T014 (ViewModel)
- T016 (PreviewProvider) depends on T013-T015
- T017 (Main.kt registration) depends on T008-T012, T016

### Parallel Opportunities

```bash
# Phase 1 parallel tasks (after T001-T002):
T003: WeatherForecastState.kt
T004: ForecastEntry.kt + ChartData.kt
T005: OpenMeteoResponse.kt

# Phase 3 parallel node creation:
T008: TriggerSourceCodeNode.kt
T009: HttpFetcherCodeNode.kt
T010: JsonParserCodeNode.kt

# Phase 6 parallel polish:
T022: HTTP error handling
T023: JSON error handling
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T005)
2. Complete Phase 2: Foundational (T006-T007)
3. Complete Phase 3: User Story 1 (T008-T017)
4. **STOP and VALIDATE**: Trigger flow, verify 7-day forecast list displays
5. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → Module ready
2. Add User Story 1 → Fetch + list display works (MVP!)
3. Add User Story 2 → Chart visualization added
4. Add User Story 3 → Location configurable
5. Polish → Error handling hardened, quickstart validated

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- All HTTP calls use Ktor Client (KMP) — never java.net
- IP Types use `Any` as the port type in CodeNodeDefinition with casting at runtime
- The `generated/` directory files (Controller, ControllerInterface, Flow) will be auto-generated after FlowGraph compilation — do not manually create
- processingLogic/ files are optional — processing logic is inline in CodeNodeDefinition.createRuntime()

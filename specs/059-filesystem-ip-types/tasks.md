# Tasks: Filesystem-Based IP Types

**Input**: Design documents from `/specs/059-filesystem-ip-types/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Extract shared enum and create data model classes needed across all user stories

- [X] T001 Extract `PlacementLevel` enum from `NodeGeneratorViewModel.kt` into shared file at `graphEditor/src/jvmMain/kotlin/model/PlacementLevel.kt` with values MODULE, PROJECT, UNIVERSAL (each with `displayName: String`). Include `availableLevels(moduleLoaded: Boolean)` helper.
- [X] T002 Update `NodeGeneratorViewModel.kt` at `graphEditor/src/jvmMain/kotlin/viewmodel/NodeGeneratorViewModel.kt` to import `PlacementLevel` from the new shared location and remove the inline enum definition.
- [X] T003 Create `IPTypeFileMeta` data class at `graphEditor/src/jvmMain/kotlin/model/IPTypeFileMeta.kt` with fields: typeName, typeId, color (IPColor), properties (List<IPPropertyMeta>), filePath, tier (PlacementLevel), packageName, className.
- [X] T004 Create `IPPropertyMeta` data class at `graphEditor/src/jvmMain/kotlin/model/IPPropertyMeta.kt` with fields: name, kotlinType (String), typeId (String), isRequired (Boolean).
- [X] T005 Add `filePath: String?` and `tier: PlacementLevel?` fields to `CustomIPTypeDefinition` at `graphEditor/src/jvmMain/kotlin/model/IPProperty.kt` (where CustomIPTypeDefinition is defined).
- [X] T006 Verify build compiles with `./gradlew :graphEditor:build`

**Checkpoint**: Shared data models ready — all user stories can reference PlacementLevel and IPTypeFileMeta

---

## Phase 2: Foundational — Migrate Existing Modules to iptypes/ (FR-014)

**Purpose**: Move existing IP type data classes from `models/` to `iptypes/` directories and update all references. This MUST complete before filesystem discovery can work.

- [X] T007 Create `iptypes/` directory in WeatherForecast module at `WeatherForecast/src/commonMain/kotlin/io/codenode/weatherforecast/iptypes/`
- [X] T008 Move IP type data classes from `WeatherForecast/.../models/` to `WeatherForecast/.../iptypes/`: `Coordinates.kt`, `HttpResponse.kt`, `ForecastData.kt`, `ForecastDisplayList.kt`, `ChartData.kt`, `ForecastEntry.kt`. Update package declarations from `io.codenode.weatherforecast.models` to `io.codenode.weatherforecast.iptypes`.
- [X] T009 Update all import references in WeatherForecast module — every file that imports from `io.codenode.weatherforecast.models` must be updated to `io.codenode.weatherforecast.iptypes`. Affected files include: all 5 node files in `nodes/`, `WeatherForecastState.kt`, `WeatherForecastViewModel.kt`, `WeatherForecast.flow.kt`, `WeatherForecast.kt` (UI), `ForecastChart.kt`. Non-IP-type files like `OpenMeteoResponse.kt` stay in `models/`.
- [X] T010 Check if EdgeArtFilter has IP types in `models/` that need migration — move `ImageData.kt` from `EdgeArtFilter/.../models/` (if it exists there) to `EdgeArtFilter/.../iptypes/` and update imports in all EdgeArtFilter node files.
- [X] T011 Add `@IPType` metadata comment headers to all migrated IP type files. Each file gets a comment block with `@IPType`, `@TypeName`, `@TypeId` (e.g., `ip_coordinates`), and `@Color` (use the colors currently defined in Main.kt registration or default palette colors).
- [X] T012 Verify build compiles with `./gradlew :WeatherForecast:build :EdgeArtFilter:build :graphEditor:build`

**Checkpoint**: All modules use `iptypes/` convention, IP type files have discovery metadata headers

---

## Phase 3: User Story 1 — IP Types Discovered from Filesystem (Priority: P1) MVP

**Goal**: graphEditor discovers IP types from `.kt` files on the filesystem at three tiers and populates the palette from discovered types.

**Independent Test**: Launch graphEditor with IP type files in Module, Project, and Universal directories. Verify types appear in palette. Delete a file, relaunch, verify type is gone.

### Implementation for User Story 1

- [X] T013 [US1] Create `IPTypeDiscovery` class at `graphEditor/src/jvmMain/kotlin/state/IPTypeDiscovery.kt` implementing the `parseIPTypeFile(filePath: String): IPTypeFileMeta?` method — regex-based extraction of `@IPType` marker, `@TypeName`, `@TypeId`, `@Color` from comment headers, plus `data class` field parsing and `package` declaration extraction (per contracts/ip-type-discovery.md).
- [X] T014 [US1] Add `discoverAll()` method to `IPTypeDiscovery` — scans three tier directories (Module `iptypes/`, Project `iptypes/src/commonMain/kotlin/io/codenode/iptypes/`, Universal `~/.codenode/iptypes/`) for `.kt` files, calls `parseIPTypeFile()` on each, deduplicates by typeName with tier precedence (Module > Project > Universal), returns `List<IPTypeFileMeta>`.
- [X] T015 [US1] Add `scanDirectory(dir: File, tier: PlacementLevel): List<IPTypeFileMeta>` helper to `IPTypeDiscovery` — lists `.kt` files in directory, parses each, assigns tier, skips unparseable files with warning log.
- [X] T016 [US1] Modify `IPTypeRegistry` at `graphEditor/src/jvmMain/kotlin/state/IPTypeRegistry.kt` — add `registerFromFilesystem(discovered: List<IPTypeFileMeta>)` method that converts each `IPTypeFileMeta` to `InformationPacketType` + `CustomIPTypeDefinition` (with filePath and tier) and registers them. Keep built-in base types (Int, Double, String, Boolean, Any) unconditionally.
- [X] T017 [US1] Modify `Main.kt` at `graphEditor/src/jvmMain/kotlin/Main.kt` — replace the hardcoded WeatherForecast IP type registration block (lines ~284-349) with a call to `IPTypeDiscovery.discoverAll()` followed by `IPTypeRegistry.registerFromFilesystem()`. Keep the `IPTypeRegistry.withDefaults()` call for built-in types. Add Module-level scan directories for all loaded modules.
- [X] T018 [US1] Verify build and launch: `./gradlew :graphEditor:build` — then manually verify the IP Type Palette shows discovered types from WeatherForecast module's `iptypes/` directory.

**Checkpoint**: IP types are filesystem-driven. Palette matches what's on disk. Removing a file removes the type on next launch.

---

## Phase 4: User Story 2 — IP Generator Level Dropdown (Priority: P2)

**Goal**: IP Generator panel gains a Level dropdown (matching Node Generator design). Creating a type generates a `.kt` file at the selected tier and immediately registers it.

**Independent Test**: Open IP Generator, select each level, create a type, verify `.kt` file appears in correct directory and type shows in palette.

### Implementation for User Story 2

- [X] T019 [US2] Create `IPTypeFileGenerator` class at `graphEditor/src/jvmMain/kotlin/state/IPTypeFileGenerator.kt` — implements `generateIPTypeFile(definition: CustomIPTypeDefinition, level: PlacementLevel, activeModulePath: String?): String` that generates the `.kt` file content (data class with `@IPType` header, correct package per level) and writes it to the resolved directory. Returns absolute file path.
- [X] T020 [US2] Add `resolveOutputDirectory(level: PlacementLevel, activeModulePath: String?): File` to `IPTypeFileGenerator` — maps MODULE to `{modulePath}/src/commonMain/kotlin/io/codenode/{moduleName}/iptypes/`, PROJECT to `{projectRoot}/iptypes/src/commonMain/kotlin/io/codenode/iptypes/`, UNIVERSAL to `~/.codenode/iptypes/`. Creates directory if it doesn't exist.
- [X] T021 [US2] Add `mapKotlinType(typeId: String): String` helper to `IPTypeFileGenerator` — maps IP type IDs (ip_string, ip_int, ip_double, ip_boolean) to Kotlin type names (String, Int, Double, Boolean). Falls back to "Any" for custom composite types.
- [X] T022 [US2] Modify `IPGeneratorPanelState` in `graphEditor/src/jvmMain/kotlin/viewmodel/IPGeneratorViewModel.kt` — add `selectedLevel: PlacementLevel` field (default: PROJECT) and `availableLevels: List<PlacementLevel>` computed property that excludes MODULE when no module is loaded.
- [X] T023 [US2] Modify `IPGeneratorViewModel.createType()` in `graphEditor/src/jvmMain/kotlin/viewmodel/IPGeneratorViewModel.kt` — after creating the `CustomIPTypeDefinition`, call `IPTypeFileGenerator.generateIPTypeFile()` with the selected level. Remove the call to `FileIPTypeRepository.add()` (no longer needed). After file generation, call `IPTypeDiscovery.parseIPTypeFile()` on the generated file and register the result in the registry for immediate palette update.
- [X] T024 [US2] Modify `IPGeneratorPanel.kt` at `graphEditor/src/jvmMain/kotlin/ui/IPGeneratorPanel.kt` — add a Level dropdown (DropdownMenu with PlacementLevel entries) matching the visual design of the Node Generator panel's level dropdown. Wire it to `IPGeneratorPanelState.selectedLevel`. Disable MODULE when `availableLevels` doesn't include it.
- [X] T025 [US2] Verify build and test: `./gradlew :graphEditor:build` — then manually test creating IP types at Project and Universal levels, verify files appear in correct directories.

**Checkpoint**: Users can create IP types at any level. Files are generated on disk. Types appear in palette immediately.

---

## Phase 5: User Story 3 — Concrete KClass References (Priority: P3)

**Goal**: Compiled IP types (Module + Project) carry their real KClass in the registry instead of `Any::class`.

**Independent Test**: Launch graphEditor, inspect IPTypeRegistry for a WeatherForecast type (e.g., Coordinates). Verify `payloadType` is `Coordinates::class`, not `Any::class`.

### Implementation for User Story 3

- [X] T026 [US3] Add `resolveKClass(meta: IPTypeFileMeta): KClass<*>` method to `IPTypeDiscovery` at `graphEditor/src/jvmMain/kotlin/state/IPTypeDiscovery.kt` — attempts `Class.forName(meta.className).kotlin` via reflection. Returns the actual KClass on success, falls back to `Any::class` on `ClassNotFoundException`. Only attempts resolution for MODULE and PROJECT tiers (UNIVERSAL always returns `Any::class`).
- [X] T027 [US3] Modify `registerFromFilesystem()` in `IPTypeRegistry` — when converting `IPTypeFileMeta` to `InformationPacketType`, call `IPTypeDiscovery.resolveKClass(meta)` to set `payloadType` to the real KClass for compiled types instead of hardcoding `Any::class`.
- [X] T028 [US3] Verify KClass resolution works end-to-end: build with `./gradlew :graphEditor:build`, launch graphEditor, confirm that WeatherForecast IP types (Coordinates, HttpResponse, ForecastData, etc.) show `payloadType` as their actual KClass in the registry (can add a temporary debug log or breakpoint).

**Checkpoint**: Compiled types carry real KClass references. Type safety is enforced at the registry level.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Legacy migration, cleanup, and end-to-end validation

- [ ] T029 Create `IPTypeMigration` class at `graphEditor/src/jvmMain/kotlin/repository/IPTypeMigration.kt` — implements `migrateIfNeeded()` per contracts/ip-type-discovery.md: checks for `~/.codenode/custom-ip-types.json`, if it exists and `~/.codenode/iptypes/` is empty, reads JSON entries via `FileIPTypeRepository.load()`, generates `.kt` files in Universal via `IPTypeFileGenerator`, renames JSON to `.bak`.
- [ ] T030 Call `IPTypeMigration.migrateIfNeeded()` in `Main.kt` before the filesystem discovery call, so migrated types are picked up by the first scan.
- [ ] T031 [P] Remove or deprecate `FileIPTypeRepository` at `graphEditor/src/jvmMain/kotlin/repository/FileIPTypeRepository.kt` — mark as `@Deprecated` with message pointing to filesystem discovery. Remove direct usage from `IPGeneratorViewModel` (replaced by file generation in T023).
- [ ] T032 [P] Remove hardcoded IP type registrations from `Main.kt` — delete the entire block that manually registers WeatherForecast IP types (ip_coordinates, ip_httpresponse, ip_forecastdata, ip_forecastdisplaylist, ip_forecastchartdata). These are now discovered from filesystem.
- [ ] T033 Verify full build with `./gradlew :WeatherForecast:build :EdgeArtFilter:build :graphEditor:build`
- [ ] T034 Run quickstart.md scenarios 1-8 manually in graphEditor to validate end-to-end functionality

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 (needs PlacementLevel enum) — BLOCKS all user stories
- **US1 (Phase 3)**: Depends on Phase 2 (needs `iptypes/` directories and `@IPType` headers in place)
- **US2 (Phase 4)**: Depends on US1 (needs `IPTypeDiscovery` for immediate registration after file generation)
- **US3 (Phase 5)**: Depends on US1 (needs `IPTypeDiscovery` and `registerFromFilesystem()`)
- **Polish (Phase 6)**: Depends on US1 + US2 (needs file generator for migration, discovery for validation)

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) — no dependencies on other stories
- **User Story 2 (P2)**: Depends on US1 — uses `IPTypeDiscovery` for immediate registration
- **User Story 3 (P3)**: Depends on US1 — extends `IPTypeDiscovery` with KClass resolution

### Within Each User Story

- Core classes before integration
- Discovery/parsing before registration
- Registration before Main.kt wiring
- Build verification at end of each phase

### Parallel Opportunities

- T003 and T004 (IPTypeFileMeta and IPPropertyMeta) can run in parallel
- T031 and T032 (deprecate FileIPTypeRepository and remove hardcoded registrations) can run in parallel
- US2 and US3 can proceed in parallel after US1 completes (independent concerns: file generation vs KClass resolution)

---

## Parallel Example: User Story 1

```bash
# T013, T014, T015 are sequential (each builds on previous)
# T016 can start after T013 (needs IPTypeFileMeta model only)
# T017 depends on T014 + T016 (needs both discovery and registry)
```

## Parallel Example: Phase 6

```bash
# Launch cleanup tasks together:
Task T031: "Deprecate FileIPTypeRepository"
Task T032: "Remove hardcoded IP type registrations from Main.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (extract PlacementLevel, create data models)
2. Complete Phase 2: Foundational (migrate modules to `iptypes/`, add `@IPType` headers)
3. Complete Phase 3: User Story 1 (filesystem discovery + registry integration)
4. **STOP and VALIDATE**: Verify IP Type Palette is filesystem-driven
5. This alone delivers the core value — types-as-files, palette-from-filesystem

### Incremental Delivery

1. Setup + Foundational → Shared infrastructure ready
2. Add US1 → Filesystem-driven palette (MVP!)
3. Add US2 → Users can create types at any level via IP Generator
4. Add US3 → Compiled types carry real KClass for type safety
5. Polish → Migration, cleanup, full validation

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Non-IP-type files (e.g., `OpenMeteoResponse.kt`) remain in `models/` — only true IP types move to `iptypes/`
- The `@IPType` header format follows research.md Decision 2
- Base types (Int, Double, String, Boolean, Any) never need files — they're always built-in

# Tasks: Separate Project Repository from Tool Repository

**Input**: Design documents from `/specs/060-separate-repo/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Prerequisites)

**Purpose**: Ensure the monorepo is in a clean state before splitting

- [ ] T001 Merge or close all open feature branches that span both tool and project code — verify with `git branch -a | grep -v main`
- [ ] T002 Ensure working tree is clean: `git status` shows no uncommitted changes
- [ ] T003 Install `git filter-repo` if not already available: `pip install git-filter-repo` or `brew install git-filter-repo`
- [ ] T004 Create the `CodeNodeIO-DemoProject` repository on GitHub (empty, no README/license/gitignore initialization)

**Checkpoint**: Clean monorepo state and empty target repository ready

---

## Phase 2: Foundational — Extract Project Repository with History

**Purpose**: Create the new repository with all project modules and preserved git history. This MUST complete before tool repository cleanup.

- [ ] T005 Clone a fresh copy of CodeNodeIO to a temporary working directory for extraction: `git clone CodeNodeIO CodeNodeIO-DemoProject-extract`
- [ ] T006 Run `git filter-repo` on the extract clone to keep only project module directories: `git filter-repo --path Addresses/ --path EdgeArtFilter/ --path GeoLocations/ --path StopWatch/ --path UserProfiles/ --path WeatherForecast/ --path KMPMobileApp/ --path persistence/ --path nodes/ --path iptypes/`
- [ ] T007 Verify git history is preserved in the extracted repo: `git log --oneline -- WeatherForecast/` should show the full commit history for that module
- [ ] T008 Add the `CodeNodeIO-DemoProject` GitHub repository as remote: `git remote add origin <github-url>` and push: `git push -u origin main`
- [ ] T009 Verify the push by cloning `CodeNodeIO-DemoProject` fresh from GitHub and confirming all 10 directories (Addresses, EdgeArtFilter, GeoLocations, StopWatch, UserProfiles, WeatherForecast, KMPMobileApp, persistence, nodes, iptypes) are present with history

**Checkpoint**: New repository exists on GitHub with all project modules and preserved git history

---

## Phase 3: User Story 1 — Move Project Modules to New Repository (Priority: P1) MVP

**Goal**: The new project repository builds independently with its own Gradle configuration.

**Independent Test**: Clone CodeNodeIO-DemoProject fresh, configure fbpDsl composite build, run `./gradlew build`.

### Implementation for User Story 1

- [ ] T010 [US1] Create `settings.gradle.kts` in CodeNodeIO-DemoProject root — include all project modules: Addresses, EdgeArtFilter, GeoLocations, StopWatch, UserProfiles, WeatherForecast, KMPMobileApp, persistence, nodes. Add `includeBuild("../CodeNodeIO")` for fbpDsl composite build dependency.
- [ ] T011 [US1] Create root `build.gradle.kts` in CodeNodeIO-DemoProject — configure shared Kotlin version (2.1.21), Compose Multiplatform (1.7.3), KSP, Room, and other shared plugin/dependency versions matching the current monorepo configuration.
- [ ] T012 [US1] Create `gradle.properties` in CodeNodeIO-DemoProject — copy relevant properties from the monorepo's `gradle.properties` (Kotlin/Compose versions, Android SDK versions, org.gradle settings).
- [ ] T013 [US1] Copy `gradle/` wrapper directory from monorepo to CodeNodeIO-DemoProject (gradlew, gradlew.bat, gradle-wrapper.jar, gradle-wrapper.properties).
- [ ] T014 [US1] Update each module's `build.gradle.kts` in CodeNodeIO-DemoProject to ensure `project(":fbpDsl")` references resolve via the composite build. Verify no references to tool-only modules (graphEditor, kotlinCompiler, circuitSimulator) exist.
- [ ] T015 [US1] Build CodeNodeIO-DemoProject with `./gradlew build` — fix any compilation errors from missing dependencies or incorrect module references.
- [ ] T016 [US1] Create `README.md` in CodeNodeIO-DemoProject root — document setup instructions: clone, configure composite build path to CodeNodeIO, build, run KMPMobileApp.
- [ ] T017 [US1] Create `.gitignore` in CodeNodeIO-DemoProject root — include standard Kotlin/KMP patterns (build/, .gradle/, .idea/, *.class, *.jar, .DS_Store).

**Checkpoint**: CodeNodeIO-DemoProject builds independently from a fresh clone

---

## Phase 4: User Story 2 — Update CodeNodeIO Tool References (Priority: P2)

**Goal**: The CodeNodeIO tool repository builds and runs without project modules. The graphEditor discovers modules at runtime.

**Independent Test**: Build CodeNodeIO tool repository, launch graphEditor pointed at CodeNodeIO-DemoProject directory, verify modules load and runtime preview works.

### Implementation for User Story 2

- [ ] T018 [US2] Remove project module directories from the CodeNodeIO tool repository: `Addresses/`, `EdgeArtFilter/`, `GeoLocations/`, `StopWatch/`, `UserProfiles/`, `WeatherForecast/`, `KMPMobileApp/`, `persistence/`, `nodes/`, `iptypes/`
- [ ] T019 [US2] Update `settings.gradle.kts` in CodeNodeIO — remove `include` entries for all removed modules (Addresses, EdgeArtFilter, GeoLocations, StopWatch, UserProfiles, WeatherForecast, KMPMobileApp, persistence, nodes)
- [ ] T020 [US2] Update `graphEditor/build.gradle.kts` — remove all `project(":...")` dependencies for removed modules (StopWatch, UserProfiles, GeoLocations, Addresses, EdgeArtFilter, WeatherForecast, persistence, nodes). Keep fbpDsl, circuitSimulator, kotlinCompiler.
- [ ] T021 [US2] Refactor `Main.kt` at `graphEditor/src/jvmMain/kotlin/Main.kt` — remove all 27 imports from project module packages (io.codenode.stopwatch, io.codenode.userprofiles, io.codenode.geolocations, io.codenode.addresses, io.codenode.edgeartfilter, io.codenode.weatherforecast, io.codenode.persistence). Remove explicit `registry.register(XxxCodeNode)` calls — rely on `NodeDefinitionRegistry.discoverAll()` and `scanDirectory()`. Update module path list in IPTypeDiscovery/NodeDefinitionRegistry to be dynamically discovered from the project directory instead of hardcoded.
- [ ] T022 [US2] Refactor `ModuleSessionFactory.kt` at `graphEditor/src/jvmMain/kotlin/ui/ModuleSessionFactory.kt` — remove all 24 imports from project module packages. Replace module-specific ViewModel/Controller adapter creation with a generic DynamicPipelineController-based approach that works for any discovered module without compile-time knowledge of its types.
- [ ] T023 [US2] Remove or relocate the 6 PreviewProvider files from `graphEditor/src/jvmMain/kotlin/ui/` — StopWatchPreviewProvider.kt, UserProfilesPreviewProvider.kt, GeoLocationsPreviewProvider.kt, AddressesPreviewProvider.kt, EdgeArtFilterPreviewProvider.kt, WeatherForecastPreviewProvider.kt. Move them to their respective modules in CodeNodeIO-DemoProject (under each module's `src/jvmMain/kotlin/.../` directory) and have them register via ServiceLoader or init-time registration.
- [ ] T024 [US2] Refactor Koin module initialization in `Main.kt` — remove hardcoded `DatabaseModule`, `userProfilesModule`, `geoLocationsModule`, `addressesModule` references. Implement discovery-based Koin module loading from the project classpath.
- [ ] T025 [US2] Add project directory configuration to the graphEditor — allow specifying the project root directory (via command-line argument, environment variable, or a settings dialog). When no project is configured, display a message guiding the user to open a project.
- [ ] T026 [US2] Update `IPTypeDiscovery` at `graphEditor/src/jvmMain/kotlin/state/IPTypeDiscovery.kt` — replace the hardcoded module path list with dynamic discovery from the configured project directory.
- [ ] T027 [US2] Update `NodeDefinitionRegistry` scan paths at `graphEditor/src/jvmMain/kotlin/Main.kt` — replace hardcoded `registry.scanDirectory()` calls for each module with dynamic scanning of all module directories found in the project directory.
- [ ] T028 [US2] Build the CodeNodeIO tool repository: `./gradlew :graphEditor:build :fbpDsl:build :kotlinCompiler:build` — verify zero compilation errors referencing removed modules.
- [ ] T029 [US2] Verify no remaining references to project module packages: `grep -r "io.codenode.stopwatch\|io.codenode.userprofiles\|io.codenode.geolocations\|io.codenode.addresses\|io.codenode.edgeartfilter\|io.codenode.weatherforecast\|io.codenode.persistence" graphEditor/src/` should return zero matches (excluding comments).
- [ ] T030 [US2] Launch graphEditor pointed at CodeNodeIO-DemoProject directory — verify all modules are discovered, flow graphs load, and runtime preview functions correctly.

**Checkpoint**: CodeNodeIO tool builds and runs independently. graphEditor discovers modules from any compatible project directory.

---

## Phase 5: User Story 3 — New Project Repository Build Configuration (Priority: P3)

**Goal**: CodeNodeIO-DemoProject has polished build configuration ready for the fbpDsl library transition.

**Independent Test**: Clone CodeNodeIO-DemoProject, verify build, verify fbpDsl dependency can be switched to a Maven coordinate by changing ≤3 lines.

### Implementation for User Story 3

- [ ] T031 [US3] Add a `libs.versions.toml` version catalog to CodeNodeIO-DemoProject at `gradle/libs.versions.toml` — centralize dependency versions (Kotlin, Compose, Room, KSP, coroutines, serialization, Koin, Ktor) for easy management.
- [ ] T032 [US3] Configure the fbpDsl composite build with a conditional fallback in `settings.gradle.kts` — if a published Maven artifact is available, use it; otherwise fall back to `includeBuild("../CodeNodeIO")`. Document the switchover pattern in a comment.
- [ ] T033 [US3] Verify the composite build works: build CodeNodeIO-DemoProject with `./gradlew build` using the local CodeNodeIO fbpDsl.
- [ ] T034 [US3] Add CI configuration (GitHub Actions workflow) to CodeNodeIO-DemoProject at `.github/workflows/build.yml` — build all modules on push/PR. Include the composite build configuration for fbpDsl.
- [ ] T035 [US3] Update `README.md` in CodeNodeIO-DemoProject — document the fbpDsl dependency strategy, how to switch from composite build to published artifact, and the project structure.

**Checkpoint**: CodeNodeIO-DemoProject has professional build configuration and documentation

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation, CI updates, and documentation

- [ ] T036 [P] Add CI configuration to CodeNodeIO tool repository at `.github/workflows/build.yml` — update or create workflow to build only tool modules (exclude project modules).
- [ ] T037 [P] Update CodeNodeIO `README.md` — document the repository separation, link to CodeNodeIO-DemoProject, explain that project modules now live in a separate repository.
- [ ] T038 Run quickstart.md scenarios 1-5 to validate end-to-end functionality across both repositories
- [ ] T039 Clean up the temporary extraction clone created in T005

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 (clean state + target repo created)
- **US1 (Phase 3)**: Depends on Phase 2 (needs extracted repo on GitHub)
- **US2 (Phase 4)**: Can start after Phase 2, but ideally after US1 so the project repo is buildable for testing
- **US3 (Phase 5)**: Depends on US1 (polishes the project repo build)
- **Polish (Phase 6)**: Depends on US1 + US2

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) — creates the buildable project repo
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) — cleans the tool repo. Benefits from US1 for validation.
- **User Story 3 (P3)**: Depends on US1 — polishes the project repo's build configuration

### Parallel Opportunities

- T036 and T037 (CI and README updates) can run in parallel
- US1 and US2 can proceed in parallel after Phase 2 (independent repos)
- Within US2: T018-T020 (removal/cleanup) are sequential, then T021-T027 (refactoring) can partially overlap

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (clean state, create target repo)
2. Complete Phase 2: Extract with git filter-repo
3. Complete Phase 3: US1 (build configuration for new repo)
4. **STOP and VALIDATE**: Both repos exist, project repo builds independently
5. This alone delivers the physical separation

### Incremental Delivery

1. Setup + Foundational → Extracted repo on GitHub
2. Add US1 → CodeNodeIO-DemoProject builds independently (MVP!)
3. Add US2 → CodeNodeIO tool builds independently, graphEditor runtime-discovers modules
4. Add US3 → Professional build configuration, CI, documentation
5. Polish → Final validation across both repos

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- The git filter-repo extraction (T006) is a destructive operation on the clone — always work on a fresh clone, never on the original
- The composite build `includeBuild("../CodeNodeIO")` assumes the two repos are sibling directories — document this convention
- PreviewProvider migration (T023) requires coordination between both repos — the graphEditor removes the files while the project repo adds them
- Koin module discovery (T024) is the most complex refactoring — the current approach has each module's Koin factory as a top-level val that Main.kt references by name

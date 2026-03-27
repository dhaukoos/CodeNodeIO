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

- [X] T001 Merge or close all open feature branches that span both tool and project code — verify with `git branch -a | grep -v main`. All feature branches (004–060) are merged to main.
- [X] T002 Ensure working tree is clean: `git status` shows no uncommitted changes
- [X] T003 Install `git filter-repo` if not already available: `brew install git-filter-repo`
- [X] T004 Create the `CodeNodeIO-DemoProject` repository on GitHub (empty, no README/license/gitignore initialization)

**Checkpoint**: Clean monorepo state and empty target repository ready

---

## Phase 2: Foundational — Extract Project Repository with History

**Purpose**: Create the new repository with all project modules and preserved git history. This MUST complete before tool repository cleanup.

- [X] T005 Clone a fresh copy of CodeNodeIO to a temporary working directory for extraction: `git clone CodeNodeIO CodeNodeIO-DemoProject-extract`
- [X] T006 Run `git filter-repo` on the extract clone to keep only project module directories: `git filter-repo --path Addresses/ --path EdgeArtFilter/ --path GeoLocations/ --path StopWatch/ --path UserProfiles/ --path WeatherForecast/ --path KMPMobileApp/ --path persistence/ --path nodes/ --path iptypes/`
- [X] T007 Verify git history is preserved in the extracted repo: `git log --oneline -- WeatherForecast/` shows full commit history (104 commits total across all modules)
- [X] T008 Add the `CodeNodeIO-DemoProject` GitHub repository as remote and push: pushed to git@github.com:dhaukoos/CodeNodeIO-DemoProject.git
- [X] T009 Verify the push by cloning `CodeNodeIO-DemoProject` fresh from GitHub — all 9 directories present (iptypes/ was empty so not extracted, will be created when needed), 104 commits with full history

**Checkpoint**: New repository exists on GitHub with all project modules and preserved git history

---

## Phase 3: User Story 1 — Move Project Modules to New Repository (Priority: P1) MVP

**Goal**: The new project repository builds independently with its own Gradle configuration.

**Independent Test**: Clone CodeNodeIO-DemoProject fresh, configure fbpDsl composite build, run `./gradlew build`.

### Implementation for User Story 1

- [X] T010 [US1] Create `settings.gradle.kts` in CodeNodeIO-DemoProject root — includes all project modules with composite build for fbpDsl via `includeBuild("../CodeNodeIO")` and `dependencySubstitution`.
- [X] T011 [US1] Create root `build.gradle.kts` in CodeNodeIO-DemoProject — configured with version catalog plugin aliases matching monorepo.
- [X] T012 [US1] Create `gradle.properties` in CodeNodeIO-DemoProject — copied relevant properties from monorepo.
- [X] T013 [US1] Copy `gradle/` wrapper directory from monorepo to CodeNodeIO-DemoProject (gradlew, gradlew.bat, gradle-wrapper.properties, libs.versions.toml).
- [X] T014 [US1] Updated all module `build.gradle.kts` files — replaced `project(":fbpDsl")` with `"io.codenode:fbpDsl"` Maven coordinate (resolved by composite build substitution). No tool-only module references found.
- [X] T015 [US1] Build verified: all 8 modules compile successfully via composite build. Committed generated runtime files (Controller, ControllerInterface, ControllerAdapter) that were gitignored in monorepo.
- [X] T016 [US1] Create `README.md` in CodeNodeIO-DemoProject root — documents setup, sibling directory structure, build instructions, and fbpDsl switchover.
- [X] T017 [US1] Create `.gitignore` in CodeNodeIO-DemoProject root — standard Kotlin/KMP patterns.

**Checkpoint**: CodeNodeIO-DemoProject builds independently from a fresh clone

---

## Phase 4: User Story 2 — Update CodeNodeIO Tool References (Priority: P2)

**Goal**: The CodeNodeIO tool repository builds and runs without project modules. The graphEditor discovers modules at runtime.

**Independent Test**: Build CodeNodeIO tool repository, launch graphEditor pointed at CodeNodeIO-DemoProject directory, verify modules load and runtime preview works.

### Implementation for User Story 2

- [X] T018 [US2] Remove project module directories from the CodeNodeIO tool repository: Addresses/, EdgeArtFilter/, GeoLocations/, StopWatch/, UserProfiles/, WeatherForecast/, KMPMobileApp/, persistence/, nodes/, iptypes/
- [X] T019 [US2] Update `settings.gradle.kts` in CodeNodeIO — removed include entries for all project modules.
- [X] T020 [US2] Update `graphEditor/build.gradle.kts` — removed 8 project(:...) dependencies. Kept fbpDsl, circuitSimulator, kotlinCompiler.
- [X] T021 [US2] Refactor `Main.kt` — removed 27 project module imports, 23 registry.register() calls, 6 PreviewProvider.register() calls, 7 scanDirectory() calls. Replaced with dynamic module directory scanning.
- [X] T022 [US2] Refactor `ModuleSessionFactory.kt` — replaced 24 module-specific imports and ~130 lines of per-module ViewModel/Controller adapters with generic DynamicPipelineController-based session creation (no compile-time module knowledge).
- [X] T023 [US2] Removed 6 PreviewProvider files from graphEditor/src/jvmMain/kotlin/ui/ (to be relocated to project repo modules).
- [X] T024 [US2] Refactored Koin initialization — replaced hardcoded DatabaseModule, userProfilesModule, geoLocationsModule references with empty module set. Project modules register Koin modules at runtime.
- [X] T025 [US2] Project directory configuration already exists via projectRoot (resolved from working directory). graphEditor discovers modules dynamically from this root.
- [X] T026 [US2] Updated IPTypeDiscovery module paths — replaced hardcoded 6-module list with dynamic discovery of all subdirectories with src/commonMain/kotlin.
- [X] T027 [US2] Updated NodeDefinitionRegistry scan paths — replaced 7 hardcoded scanDirectory() calls with dynamic walkTopDown() scanning of all module node directories.
- [X] T028 [US2] Build verified: `./gradlew :graphEditor:compileKotlinJvm :fbpDsl:jvmJar :kotlinCompiler:jvmJar` — BUILD SUCCESSFUL with zero project module errors.
- [X] T029 [US2] Verified: zero remaining project module references in graphEditor/src/jvmMain/ (excluding code generation templates in ModuleSaveService).
- [ ] T030 [US2] Launch graphEditor pointed at CodeNodeIO-DemoProject directory — verify all modules are discovered, flow graphs load, and runtime preview functions correctly.

**Checkpoint**: CodeNodeIO tool builds and runs independently. graphEditor discovers modules from any compatible project directory.

---

## Phase 5: User Story 3 — New Project Repository Build Configuration (Priority: P3)

**Goal**: CodeNodeIO-DemoProject has polished build configuration ready for the fbpDsl library transition.

**Independent Test**: Clone CodeNodeIO-DemoProject, verify build, verify fbpDsl dependency can be switched to a Maven coordinate by changing ≤3 lines.

### Implementation for User Story 3

- [X] T031 [US3] Updated `libs.versions.toml` with project-specific entries (Room 2.8.4, KSP, Koin 4.0.0, Ktor 3.1.1, lifecycle 2.8.0, material3, sqlite). Removed tool-only entries.
- [X] T032 [US3] Configured composite build with conditional fallback (checks for ../CodeNodeIO directory, warns if absent). Documented switchover pattern for published artifacts.
- [X] T033 [US3] Verified build: fixed Compose plugin from 1.11.1 (unreleased) to 1.10.0. `./gradlew jvmJar` succeeds.
- [X] T034 [US3] Added GitHub Actions CI workflow at `.github/workflows/build.yml` — checks out both repos, builds JVM targets, runs tests.
- [X] T035 [US3] Updated `README.md` with setup, graphEditor launch instructions, dependency strategy (composite → Maven), and version catalog reference.

**Checkpoint**: CodeNodeIO-DemoProject has professional build configuration and documentation

---

## Phase 6: Extract preview-api Module (FR-011)

**Goal**: Create a shared `preview-api` module containing PreviewRegistry so project module PreviewProviders compile without depending on graphEditor. Eliminates circular dependency and StackOverflowError.

### Create preview-api in CodeNodeIO

- [X] T036 Created `preview-api/` module with `build.gradle.kts` — KMP module with JVM target, depends on `compose.runtime` and `compose.ui`.
- [X] T037 Moved `PreviewRegistry.kt` and `PreviewComposable` typealias to `preview-api/src/commonMain/kotlin/io/codenode/previewapi/PreviewRegistry.kt`. Deleted old file from graphEditor.
- [X] T038 Added `include(":preview-api")` to `settings.gradle.kts`.
- [X] T039 Added `implementation(project(":preview-api"))` to graphEditor commonMain dependencies.
- [X] T040 Updated imports: `RuntimePreviewPanel.kt` (added explicit import), `ModuleSaveService.kt` (updated generated code import). `DynamicPreviewDiscovery.kt` only had comment reference.
- [X] T041 Verified build: `./gradlew :preview-api:compileKotlinJvm :graphEditor:compileKotlinJvm` — BUILD SUCCESSFUL.

**Checkpoint**: preview-api module exists, graphEditor compiles using it instead of its inline copy

### Update DemoProject

- [ ] T042 Add `preview-api` to composite build substitution in DemoProject `settings.gradle.kts`: `substitute(module("io.codenode:preview-api")).using(project(":preview-api"))`
- [ ] T043 Replace `compileOnly("io.codenode:graphEditor")` with `implementation("io.codenode:preview-api")` in all 6 module `build.gradle.kts` files (jvmMain source set). This eliminates the circular dependency.
- [ ] T044 Update PreviewProvider imports in all 6 modules from `io.codenode.grapheditor.ui.PreviewRegistry` to `io.codenode.previewapi.PreviewRegistry`
- [ ] T045 Verify DemoProject builds: `./gradlew clean jvmJar writeRuntimeClasspath --rerun-tasks`

**Checkpoint**: DemoProject modules compile PreviewProviders via preview-api (no graphEditor dependency)

### Update code generator

- [ ] T046 Update `wireGraphEditorIntegration` in `ModuleSaveService.kt` — generated PreviewProvider imports from `io.codenode.previewapi.PreviewRegistry` instead of `io.codenode.grapheditor.ui.PreviewRegistry`
- [ ] T047 Update `unwireGraphEditorIntegration` accordingly
- [ ] T048 Verify generator: create and remove a test repository module, confirm correct imports and no StackOverflowError

**Checkpoint**: Code generator produces modules with correct preview-api imports

### End-to-end validation

- [ ] T049 Run graphEditor via `:graphEditor:run` from Android Studio — verify all module previews work (StopWatch, UserProfiles, EdgeArtFilter, etc.)
- [ ] T050 Run graphEditor via `./gradlew runGraphEditor` from DemoProject — verify all module previews work
- [ ] T051 Clean up: remove `DynamicPreviewDiscovery` reflection-based class loading (providers now compile normally; keep source file scanning for discovery invocation)

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final validation, CI updates, and documentation

- [ ] T052 [P] Add CI configuration to CodeNodeIO tool repository at `.github/workflows/build.yml` — update or create workflow to build only tool modules (exclude project modules).
- [ ] T053 [P] Update CodeNodeIO `README.md` — document the repository separation, link to CodeNodeIO-DemoProject, explain that project modules now live in a separate repository.
- [ ] T054 Run quickstart.md scenarios 1-5 to validate end-to-end functionality across both repositories
- [ ] T055 Clean up the temporary extraction clone created in T005

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 (clean state + target repo created)
- **US1 (Phase 3)**: Depends on Phase 2 (needs extracted repo on GitHub)
- **US2 (Phase 4)**: Can start after Phase 2, but ideally after US1 so the project repo is buildable for testing
- **US3 (Phase 5)**: Depends on US1 (polishes the project repo build)
- **preview-api (Phase 6)**: Depends on US2 (needs graphEditor decoupled from project modules)
- **Polish (Phase 7)**: Depends on all previous phases

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
- preview-api module (Phase 6) resolves the circular dependency between graphEditor and project module PreviewProviders — project modules depend on the tiny preview-api instead of the entire graphEditor

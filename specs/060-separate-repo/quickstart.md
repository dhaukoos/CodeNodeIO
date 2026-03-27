# Quickstart: Separate Project Repository from Tool Repository

**Feature**: 060-separate-repo

## Scenario 1: Create the New Project Repository

**Steps**:
1. Ensure all feature branches are merged and the working tree is clean
2. Create the new `CodeNodeIO-DemoProject` GitHub repository
3. Extract project modules using `git filter-repo` preserving commit history
4. Push the new repository to GitHub

**Expected**:
- `CodeNodeIO-DemoProject` repository exists on GitHub with the following modules: Addresses, EdgeArtFilter, GeoLocations, StopWatch, UserProfiles, WeatherForecast, KMPMobileApp, persistence, nodes/, iptypes/
- Git history for all moved files is preserved
- The new repository has its own `settings.gradle.kts` and `build.gradle.kts`

---

## Scenario 2: Clean the Tool Repository

**Steps**:
1. Remove project module directories from the CodeNodeIO repository
2. Remove their entries from `settings.gradle.kts`
3. Remove `project(":...")` dependencies from `graphEditor/build.gradle.kts`
4. Remove hardcoded imports from Main.kt, ModuleSessionFactory.kt, and PreviewProvider files
5. Build the tool repository

**Expected**:
- `./gradlew :graphEditor:build :fbpDsl:build :kotlinCompiler:build` succeeds
- No references to project module packages remain in the source
- The graphEditor starts without project modules present

---

## Scenario 3: Build the New Project Repository

**Steps**:
1. Clone `CodeNodeIO-DemoProject` fresh
2. Configure the composite build to reference the local CodeNodeIO tool repository for fbpDsl
3. Run the build

**Expected**:
- All modules compile successfully
- Tests pass
- KMPMobileApp can be launched on a target platform

---

## Scenario 4: Launch graphEditor Against the New Project

**Steps**:
1. Build the tool repository
2. Launch graphEditor with the project directory set to the CodeNodeIO-DemoProject clone
3. Open a module (e.g., WeatherForecast)

**Expected**:
- All modules appear in the module list
- The flow graph loads correctly in the canvas
- Runtime preview works (pipeline starts, data flows)
- IP types are discovered from the project's module `iptypes/` directories

---

## Scenario 5: Verify Git History Preservation

**Steps**:
1. In the `CodeNodeIO-DemoProject` repository, run `git log --oneline -- WeatherForecast/`
2. Compare with the original history from the monorepo

**Expected**:
- Commit history for WeatherForecast (and all other modules) is present and matches the original monorepo history

---

## Scenario 6: PreviewProvider Compilation via preview-api

**Steps**:
1. In CodeNodeIO-DemoProject, verify each module's PreviewProvider imports from `io.codenode.previewapi.PreviewRegistry`
2. Verify each module's `build.gradle.kts` jvmMain depends on `implementation("io.codenode:preview-api")` (NOT `compileOnly("io.codenode:graphEditor")`)
3. Run `./gradlew clean jvmJar --rerun-tasks`
4. Create a new repository module via the graphEditor
5. Verify the generated PreviewProvider imports from `io.codenode.previewapi.PreviewRegistry`

**Expected**:
- All modules compile without depending on graphEditor
- No StackOverflowError during Gradle sync
- Generated modules have correct preview-api import
- All module previews render in the graphEditor

---

## Scenario 7: Switch fbpDsl from Composite Build to Published Artifact

**Steps** (for future validation after fbpDsl is published):
1. In `CodeNodeIO-DemoProject`, replace the composite build `includeBuild("../CodeNodeIO")` with a Maven dependency `implementation("io.codenode:fbpdsl:1.0.0")`
2. Build the project

**Expected**:
- Build succeeds with the published artifact
- No more than 3 lines of build configuration changed

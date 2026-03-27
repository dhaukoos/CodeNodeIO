# Feature Specification: Separate Project Repository from Tool Repository

**Feature Branch**: `060-separate-repo`
**Created**: 2026-03-25
**Status**: Draft
**Input**: User description: "Move KMPMobileApp and tool-created Modules into Separate Github Repository. For early-development convenience, until now the KMPMobileApp and tool-generated modules (Addresses, EdgeArtFilter, GeoLocations, StopWatch, UserProfiles, WeatherForecast) are all residing in the same Github Repository as the CodeNodeIO tool itself. Now move these modules into a new repository to reflect the intended separation between the tool and the projects it creates. The persistence module will also need to be moved. After this separation, subsequent features will include creating initial releases of (1) a fbpDsl library and (2) the CodeNodeIO tool."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Move Project Modules to New Repository (Priority: P1)

A developer separates the tool-generated project modules (Addresses, EdgeArtFilter, GeoLocations, StopWatch, UserProfiles, WeatherForecast), the KMPMobileApp, and the persistence module from the CodeNodeIO tool repository into a new, standalone GitHub repository. After the move, the new repository builds independently and retains its full git history for the moved modules. The CodeNodeIO tool repository no longer contains project modules.

**Why this priority**: This is the core deliverable — the physical separation of codebases into two repositories. Everything else depends on this being done correctly.

**Independent Test**: Clone both repositories from scratch. Verify the new project repository builds successfully on its own. Verify the CodeNodeIO tool repository builds successfully without the project modules.

**Acceptance Scenarios**:

1. **Given** the current monorepo containing both tool and project modules, **When** the separation is performed, **Then** a new GitHub repository exists containing Addresses, EdgeArtFilter, GeoLocations, StopWatch, UserProfiles, WeatherForecast, KMPMobileApp, and persistence modules.
2. **Given** the new project repository, **When** a developer runs the build, **Then** all modules compile and tests pass without referencing the CodeNodeIO tool repository.
3. **Given** the CodeNodeIO tool repository after separation, **When** a developer runs the build, **Then** the tool (graphEditor, fbpDsl, kotlinCompiler) compiles and tests pass without the project modules present.
4. **Given** the new project repository, **When** a developer inspects git history, **Then** the commit history for the moved modules is preserved.

---

### User Story 2 - Update CodeNodeIO Tool References (Priority: P2)

After the project modules are removed, the CodeNodeIO tool repository is updated so that all internal references to the moved modules are cleaned up or made configurable. The graphEditor must still function — it should discover modules from the project directory at runtime rather than having them hardcoded. The tool should work when pointed at any compatible project directory.

**Why this priority**: Without this, the tool repository would have broken references and couldn't build or run. This is the cleanup that makes the separation complete.

**Independent Test**: Build the CodeNodeIO tool repository. Launch the graphEditor and point it at the new project repository's directory. Verify all modules are discovered and the tool functions normally.

**Acceptance Scenarios**:

1. **Given** the tool repository after module removal, **When** a developer builds the tool, **Then** the build succeeds with zero errors referencing removed modules.
2. **Given** the tool repository, **When** hardcoded module names or paths existed in the source, **Then** they are replaced with configurable or discovery-based alternatives.
3. **Given** the graphEditor launched against the new project directory, **When** the user opens a module, **Then** the module loads and the runtime preview functions correctly.
4. **Given** the graphEditor, **When** it starts without a project directory configured, **Then** it displays a clear message guiding the user to open or configure a project.

---

### User Story 3 - New Project Repository Structure and Build Configuration (Priority: P3)

The new project repository has a properly configured build system, dependency declarations, and project structure. It depends on the fbpDsl library (which will be published separately in a subsequent release). During the interim period before fbpDsl is published, the new repository includes fbpDsl as a local dependency or git submodule.

**Why this priority**: The new repository needs to be self-contained and buildable. This story ensures the project can stand on its own and is ready for the upcoming fbpDsl library release.

**Independent Test**: Clone the new project repository. Run the build. Verify all modules compile, tests pass, and the KMPMobileApp runs on the target platforms.

**Acceptance Scenarios**:

1. **Given** the new project repository, **When** a developer clones it fresh, **Then** the build system resolves all dependencies and compiles successfully.
2. **Given** the new project repository, **When** the fbpDsl library is later published as a versioned artifact, **Then** the project can switch from the local/submodule dependency to the published version with minimal changes (updating a version number in the build configuration).
3. **Given** the new project repository, **When** a developer inspects the project structure, **Then** it follows standard conventions for the platform (clear module boundaries, shared build configuration, documented setup).

---

### Edge Cases

- What happens if a developer has uncommitted changes in the project modules during separation? The separation process must only operate on committed, clean state. Uncommitted changes must be committed or stashed before proceeding.
- What happens if the tool repository's build references project module classes at compile time (e.g., in Main.kt node registrations)? These references must be removed or converted to runtime discovery. The tool must not have compile-time dependencies on project modules.
- What happens if the new project repository is cloned without the fbpDsl dependency available? The build must fail with a clear error message indicating the missing dependency and how to resolve it.
- What happens to CI/CD pipelines that currently build the monorepo? They must be updated or split to build each repository independently.
- What happens to existing feature branches that span both tool and project code? They must be completed and merged before separation, or manually split across both repositories.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: All tool-generated project modules (Addresses, EdgeArtFilter, GeoLocations, StopWatch, UserProfiles, WeatherForecast) MUST be moved to a new GitHub repository.
- **FR-002**: The KMPMobileApp module MUST be moved to the new project repository.
- **FR-003**: The persistence module MUST be moved to the new project repository.
- **FR-004**: The CodeNodeIO tool repository MUST build and pass tests after the project modules are removed.
- **FR-005**: The new project repository MUST build and pass tests independently.
- **FR-006**: Git history for all moved modules MUST be preserved in the new repository.
- **FR-007**: The graphEditor MUST NOT have compile-time dependencies on any project module after separation. Module discovery MUST be runtime-based.
- **FR-008**: The new project repository MUST declare its dependency on fbpDsl, with a mechanism to use a local copy until the library is published.
- **FR-009**: Hardcoded module names, paths, or imports referencing project modules in the tool repository MUST be removed or replaced with configurable alternatives.
- **FR-010**: Both repositories MUST have complete, independent build configurations (no shared build files across repositories).
- **FR-011**: Project module PreviewProviders MUST be able to compile without depending on the graphEditor module. A shared `preview-api` module MUST provide the PreviewRegistry interface that both the graphEditor and project modules depend on.

### Key Entities

- **Tool Repository (CodeNodeIO)**: Contains the graphEditor, fbpDsl library, preview-api library, kotlinCompiler, and tooling infrastructure. Does not contain any user project modules after separation.
- **Project Repository**: Contains all tool-generated modules (Addresses, EdgeArtFilter, GeoLocations, StopWatch, UserProfiles, WeatherForecast), KMPMobileApp, and the persistence module. Depends on fbpDsl and preview-api as libraries.
- **fbpDsl Library**: The flow-based programming DSL that both repositories depend on. Currently lives in the tool repository; will be published as a standalone library in a subsequent feature.
- **preview-api Library**: Lightweight module containing PreviewRegistry and PreviewComposable typealias. Depends only on Compose runtime. Both graphEditor and project modules depend on it, avoiding circular dependencies. Will be published as a standalone library alongside fbpDsl.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Both repositories build successfully from a fresh clone with zero cross-repository compile-time dependencies.
- **SC-002**: 100% of moved modules retain their full git commit history in the new repository.
- **SC-003**: The graphEditor discovers and loads all project modules when pointed at the new project directory, with no hardcoded module references in the tool source code.
- **SC-004**: A developer can set up and build either repository independently in under 10 minutes following the repository's README.
- **SC-005**: Switching the new project repository from local fbpDsl to a published version requires changing no more than 3 lines of build configuration.

## Assumptions

- The new project repository will be created under the same GitHub organization/account as CodeNodeIO.
- The repository name for the new project has not been decided — it will be determined during planning. A reasonable default would be something like "CodeNodeIO-DemoProject" or the project's own name.
- fbpDsl will remain in the CodeNodeIO tool repository for now and be published as a library in a subsequent feature (not part of this scope).
- The `nodes/` directory (project-level shared nodes) moves with the project repository.
- The `iptypes/` directory (project-level shared IP types) moves with the project repository, even though it currently contains no files.
- Existing open feature branches should be merged or completed before this separation is performed to avoid complex branch splitting.
- The `.specify/` tooling directory stays in the CodeNodeIO tool repository.

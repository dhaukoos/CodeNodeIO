# Research: Separate Project Repository from Tool Repository

**Feature**: 060-separate-repo
**Date**: 2026-03-25

## Decision 1: Repository Split Strategy

**Decision**: Use `git filter-repo` to extract project modules into the new repository while preserving commit history, then clean the tool repository.

**Rationale**: `git filter-repo` is the modern replacement for `git filter-branch` — it's fast, safe, and can extract subdirectories while rewriting history to include only relevant commits. This preserves the commit history for moved modules per FR-006.

**Alternatives considered**:
- `git subtree split` — viable but creates a linear history without merge commits; `filter-repo` preserves the full DAG.
- Fresh repository with copy-paste — rejected because it loses git history (violates FR-006).
- Git submodules — rejected because it adds complexity; the project should be fully independent.

## Decision 2: New Repository Name

**Decision**: Name the new repository `CodeNodeIO-DemoProject`.

**Rationale**: The repository represents a single project that happens to consist of many modules — mirroring the "Level" hierarchy concept used for nodes and IP types within CodeNodeIO (Module → Project → Universal). "DemoProject" accurately reflects that this is one demonstration project, and the modules within it are project-level components, not independent projects.

**Alternatives considered**:
- `CodeNodeIO-Samples` — implies a collection of unrelated samples rather than a cohesive project.
- `CodeNodeIO-Projects` — implies production projects rather than a demo.
- `KMPMobileApp` — too narrow (the repo contains more than just the mobile app).

## Decision 3: fbpDsl Dependency Strategy for the New Repository

**Decision**: Use a composite build (`includeBuild`) to reference the fbpDsl library from the CodeNodeIO tool repository during the interim period. When fbpDsl is published as a Maven artifact, switch to a standard dependency declaration.

**Rationale**: Composite builds are a first-class Gradle feature that allows one project to consume another project's outputs without publishing. This enables independent development while keeping both repositories buildable. The switch to a published artifact later requires only changing the dependency line.

**Alternatives considered**:
- Git submodule for fbpDsl — adds checkout complexity and version management overhead.
- Copy fbpDsl sources into the new repo — duplicates code, creates divergence risk.
- Publish fbpDsl to Maven Local first — adds a build step and can cause stale artifact issues.

## Decision 4: Handling graphEditor's Compile-Time Dependencies on Project Modules

**Decision**: Remove all compile-time dependencies on project modules from the graphEditor. Convert the 63 hardcoded imports across 8 files (Main.kt, ModuleSessionFactory.kt, 6 PreviewProviders) to runtime discovery. The graphEditor should have zero `project(":ModuleName")` dependencies for generated modules.

**Approach**:
1. **Node registration** (Main.kt): Already mostly discovery-based via `NodeDefinitionRegistry`. Remove the explicit `registry.register(XxxCodeNode)` calls — rely entirely on `discoverAll()` + `scanDirectory()`.
2. **ModuleSessionFactory**: Make module-specific ViewModel/Controller creation discovery-based. Each module can provide a factory via a service interface discovered at runtime.
3. **PreviewProviders**: Move preview provider files into their respective modules. The graphEditor discovers composables via `ComposableDiscovery` + `PreviewRegistry` — preview providers should register themselves, not be hardcoded in graphEditor.
4. **Koin modules (persistence)**: The project repository manages its own Koin module setup. The graphEditor provides a hook for projects to register their Koin modules.

**Rationale**: This achieves FR-007 (no compile-time dependencies on project modules) and FR-009 (no hardcoded module references). The graphEditor becomes a generic tool that works with any compatible project.

**Alternatives considered**:
- Keep graphEditor as a composite build consumer of the project — rejected because it recreates the coupling; the tool should be independent.
- Use reflection to load module classes — this is effectively what the discovery-based approach does, but in a structured way via ServiceLoader or classpath scanning.

## Decision 5: Modules Staying in the Tool Repository

**Decision**: The following modules stay in the CodeNodeIO tool repository:
- `fbpDsl` — core DSL library (will be published separately later)
- `graphEditor` — the visual editor tool
- `kotlinCompiler` — code generation engine
- `circuitSimulator` — runtime simulation engine
- `goCompiler` — Go code generation (if used)
- `idePlugin` — IDE integration

**Decision**: The following modules move to the new repository:
- `Addresses`, `EdgeArtFilter`, `GeoLocations`, `StopWatch`, `UserProfiles`, `WeatherForecast` — generated project modules
- `KMPMobileApp` — the mobile application
- `persistence` — shared Room persistence module
- `nodes/` — project-level shared nodes directory
- `iptypes/` — project-level shared IP types directory

**Rationale**: Clean separation between "tool" and "what the tool creates". The persistence module is project-specific (Room entities for the sample modules), not a tool concern.

## Decision 6: Handling the circuitSimulator Module

**Decision**: `circuitSimulator` stays in the tool repository. It is a core platform component (runtime animation engine), not a project module.

**Rationale**: The graphEditor depends on `circuitSimulator` for connection animations during runtime preview. It's part of the tool's infrastructure, not generated by the tool.

## Dependency Analysis Summary

**Post-separation tool repository compile-time dependencies**:
```
graphEditor → fbpDsl, circuitSimulator, kotlinCompiler
kotlinCompiler → fbpDsl
circuitSimulator → fbpDsl
```

**Post-separation project repository compile-time dependencies**:
```
KMPMobileApp → fbpDsl, StopWatch, UserProfiles, persistence
StopWatch → fbpDsl
UserProfiles → fbpDsl, persistence
GeoLocations → fbpDsl, persistence
Addresses → fbpDsl, persistence
EdgeArtFilter → fbpDsl
WeatherForecast → fbpDsl
persistence → (Room, SQLite - no fbpDsl dependency)
nodes → fbpDsl
```

## Decision 7: PreviewProvider Architecture — preview-api Module

**Decision**: Create a separate `preview-api` module in the CodeNodeIO tool repository containing `PreviewRegistry` and the `PreviewComposable` typealias. Both the graphEditor and project modules depend on `preview-api`, avoiding circular dependencies.

**Rationale**: After repository separation, project module PreviewProviders need to import `PreviewRegistry` to register composable previews. Putting `PreviewRegistry` in the graphEditor creates a circular dependency (project → graphEditor → project). A shared `preview-api` module breaks this cycle cleanly. It depends only on Compose runtime types (`@Composable`, `Modifier`) — no graphEditor, no fbpDsl.

**Alternatives considered**:
- `compileOnly("io.codenode:graphEditor")` in project modules — causes StackOverflowError during Gradle sync due to composite build resolution cycles.
- Convention-based discovery (no PreviewRegistry) — less flexible, assumes naming conventions, can't register multiple previews per module.
- Merge PreviewRegistry into fbpDsl — pollutes the pure Kotlin DSL library with Compose dependencies.
- Reflection-based providers (no compile-time imports) — providers can't compile standalone, fragile.
- Project-level bootstrap file — single file to maintain but still requires graphEditor on classpath.

**Module contents** (~30 lines):
- `PreviewComposable` typealias: `@Composable (viewModel: Any, modifier: Modifier) -> Unit`
- `PreviewRegistry` object: `register()`, `get()`, `hasPreview()`, `registeredNames()`

**Release strategy**: Published as a standalone library alongside fbpDsl (e.g., `io.codenode:preview-api:1.0.0`).

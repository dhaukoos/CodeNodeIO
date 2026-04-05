# CodeNodeIO Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-01-13

## Active Technologies
- Kotlin 2.1.21 (existing project configuration) (002-generic-nodetype)
- Kotlin 2.1.21 (KMP), Compose Desktop 1.7.3 + Compose Desktop (UI), kotlinx-coroutines, kotlinx-serialization (005-graphnode-creation)
- .flow.kts files (DSL serialization format) (005-graphnode-creation)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3, kotlinx-serialization, kotlinx-coroutines (006-passthru-port-segments)
- .flow.kts DSL files (text-based serialization) (006-passthru-port-segments)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + kotlinx-serialization, kotlinx-coroutines, Compose Desktop 1.7.3 (graphEditor) (007-node-execution-control)
- .flow.kts files (text-based DSL serialization) (007-node-execution-control)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, Compose Multiplatform 1.7.3, KotlinPoe (008-stopwatch-virtual-circuit)
- .flow.kts files (DSL serialization for FlowGraph persistence) (008-stopwatch-virtual-circuit)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3, kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0 (011-stopwatch-refactor)
- N/A (in-memory FlowGraph models) (011-stopwatch-refactor)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, KotlinPoet (code generation) (012-channel-connections)
- N/A (in-memory FlowGraph models, generated code) (012-channel-connections)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3, kotlinx-serialization 1.6.0 (016-node-generator)
- JSON file via kotlinx-serialization for CustomNodeRepository persistence (016-node-generator)
- Kotlin 2.1.21 (Kotlin Multiplatform) + Compose Desktop 1.7.3, kotlinx-coroutines 1.8.0 (017-viewmodel-pattern)
- FileCustomNodeRepository (JSON persistence for custom nodes) (017-viewmodel-pattern)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Multiplatform 1.7.3, kotlinx-coroutines 1.8.0, JetBrains lifecycle-viewmodel-compose 2.8.0 (018-stopwatch-viewmodel)
- N/A (in-memory FlowGraph state) (018-stopwatch-viewmodel)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Multiplatform 1.7.3, kotlinx-coroutines 1.8.0, lifecycle-viewmodel-compose 2.8.0 (019-flowgraph-execution-control)
- Kotlin 2.1.21 (Kotlin Multiplatform) + kotlinx-coroutines 1.8.0, Compose Multiplatform 1.7.3, lifecycle-viewmodel-compose 2.8.0 (020-refactor-timer-emitter)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + kotlinx-coroutines 1.8.0 (channels, coroutine scopes) (021-refactor-noderuntime-base)
- N/A (in-memory runtime state) (021-refactor-noderuntime-base)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Multiplatform 1.7.3, kotlinx-coroutines 1.8.0, lifecycle-viewmodel-compose 2.8.0, lifecycle-runtime-compose 2.8.0 (022-generate-flowgraph-viewmodel)
- N/A (in-memory FlowGraph models, generated source code) (022-generate-flowgraph-viewmodel)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3 + kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0 (023-port-type-selector)
- .flow.kts files (DSL text-based serialization via FlowGraphSerializer) (023-port-type-selector)
- N/A (code deletion only) (024-remove-single-invocation)
- N/A (in-memory factory methods only) (025-timed-factory-methods)
- N/A (in-memory models, generated source files) (026-processing-logic-stubs)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, Compose Multiplatform 1.7.3, lifecycle-viewmodel-compose 2.8.0 (027-generate-runtime-files)
- N/A (generates source code files to filesystem) (027-generate-runtime-files)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + kotlinx-coroutines 1.8.0 (MutableStateFlow/StateFlow), kotlinx-serialization 1.6.0 (028-state-properties-stubs)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3 + kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, lifecycle-viewmodel-compose 2.8.0 (029-grapheditor-save)
- Filesystem (generated KMP module directories) (029-grapheditor-save)
- Kotlin 2.1.21 (Kotlin Multiplatform) + Compose Multiplatform 1.7.3, kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, lifecycle-viewmodel-compose 2.8.0 (030-stopwatch-module-refactor)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3, kotlinx-coroutines 1.8.0, lifecycle-viewmodel-compose 2.8.0 (031-grapheditor-runtime-preview)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3 + kotlinx-coroutines 1.8.0, Compose Material (OutlinedTextField, DropdownMenu, Checkbox, Button) (032-ip-generator)
- N/A (in-memory IPTypeRegistry, transient for current session) (032-ip-generator)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3 + Compose Material (OutlinedTextField, Text, Surface, Column, Row), kotlinx-coroutines 1.8.0 (033-graphnode-properties)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3, kotlinx-coroutines 1.8.0 (`select` expression), kotlinx-serialization 1.6.0, lifecycle-viewmodel-compose 2.8.0 (035-any-input-trigger)
- JSON file via kotlinx-serialization (CustomNodeRepository persistence) (035-any-input-trigger)
- Kotlin 2.1.21 (Kotlin Multiplatform) + Compose Desktop 1.7.3, kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, Room 2.8.4, KSP 2.1.21-2.0.1, SQLite Bundled 2.6.2 (036-entity-repository-nodes)
- Room (KMP) with BundledSQLiteDriver — persisted to `~/.codenode/data/app.db` (JVM) (036-entity-repository-nodes)
- Kotlin 2.1.21 (Kotlin Multiplatform) + kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, Compose Multiplatform 1.7.3, lifecycle-viewmodel-compose 2.8.0 (037-source-sink-refactor)
- N/A (in-memory models, generated source code files) (037-source-sink-refactor)
- Kotlin 2.1.21 (Kotlin Multiplatform) + kotlinx-coroutines 1.8.0 (channels, StateFlow, combine, delay), kotlinx-serialization 1.6.0 (038-reactive-source-loop)
- Kotlin 2.1.21 (Kotlin Multiplatform) + Compose Multiplatform 1.7.3, kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, lifecycle-viewmodel-compose 2.8.0, Room 2.8.4 (KMP), KSP 2.1.21-2.0.1, SQLite Bundled 2.6.2 (039-userprofiles-module)
- Room (KMP) with BundledSQLiteDriver — persisted to `~/.codenode/data/app.db` (JVM), platform-specific paths for Android/iOS (039-userprofiles-module)
- Kotlin 2.1.21 (Kotlin Multiplatform) + Compose Desktop 1.7.3, kotlinx-coroutines 1.8.0, lifecycle-viewmodel-compose 2.8.0, Room 2.8.4 (UserProfiles only) (040-generalize-runtime-preview)
- N/A (no new storage; UserProfiles uses existing Room database) (040-generalize-runtime-preview)
- N/A (in-memory animation state) (041-animate-data-flow)
- Kotlin 2.1.21 (Kotlin Multiplatform) + Compose Desktop 1.7.3 + Compose Material (Icons, Surface, Row, Column, Box), Compose Foundation (clickable, background) (042-collapsible-panels)
- N/A (in-memory panel state, session-only) (042-collapsible-panels)
- Kotlin 2.1.21 (Kotlin Multiplatform) + kotlinx-coroutines 1.8.0 + kotlinx-coroutines (channels, StateFlow, select), kotlinx-serialization 1.6.0 (043-dataflow-refinements)
- Kotlin 2.1.21 (Kotlin Multiplatform) + Compose Desktop 1.7.3 + kotlinx-coroutines 1.8.0 (channels, StateFlow), Compose Material (UI) (044-debuggable-data-preview)
- N/A (in-memory snapshots, transient per runtime session) (044-debuggable-data-preview)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Multiplatform 1.7.3 + kotlinx-coroutines 1.8.0, Room 2.8.4 (KMP), KSP 2.1.21-2.0.1, SQLite Bundled 2.6.2 (045-refactor-userprofiles-module)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3 + kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, lifecycle-viewmodel-compose 2.8.0, Room 2.8.4 (KMP), KSP 2.1.21-2.0.1, Koin 4.0.0 (047-entity-module-generator)
- Room (KMP) with BundledSQLiteDriver — all persistence components in shared `persistence` module (feature 046 architecture) (047-entity-module-generator)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3, kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, Room 2.8.4 (KMP), Koin 4.0.0 (048-remove-entity-module)
- Room (KMP) with BundledSQLiteDriver — persistence module; FileCustomNodeRepository (JSON at `~/.codenode/custom-nodes.json`) (048-remove-entity-module)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3 (ImageBitmap, Canvas, Image composable), kotlinx-coroutines 1.8.0 (049-image-filter-pipeline)
- N/A (in-memory image processing, no persistence) (049-image-filter-pipeline)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3, kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, lifecycle-viewmodel-compose 2.8.0 (050-self-contained-codenode)
- FileCustomNodeRepository (JSON at `~/.codenode/custom-nodes.json`) for legacy; classpath + filesystem scanning for new nodes (050-self-contained-codenode)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3, kotlinx-coroutines 1.8.0 (channels, StateFlow, CoroutineScope), lifecycle-viewmodel-compose 2.8.0 (051-dynamic-runtime-pipeline)
- N/A (in-memory pipeline state only) (051-dynamic-runtime-pipeline)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3, kotlinx-coroutines 1.8.0 (channels, StateFlow, select), kotlinx-serialization 1.6.0, lifecycle-viewmodel-compose 2.8.0, Room 2.8.4 (KMP), KSP 2.1.21-2.0.1, SQLite Bundled 2.6.2, Koin 4.0.0 (052-migrate-module-runtimes)
- Room (KMP) with BundledSQLiteDriver for entity modules (UserProfiles, GeoLocations, Addresses); N/A for StopWatch (052-migrate-module-runtimes)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + kotlinx-coroutines 1.8.0, Compose Multiplatform 1.7.3, Room 2.8.4 (KMP), Koin 4.0.0 (053-codenode-flow-runtime)
- Room (KMP) with BundledSQLiteDriver for entity modules; N/A for StopWatch (053-codenode-flow-runtime)
- N/A (removes `~/.codenode/custom-nodes.json` dependency) (054-persist-codenode-metadata)
- N/A (filesystem-based discovery of .kt source files) (055-filesystem-node-palette)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Multiplatform 1.7.3, Room 2.8.4 (KMP), Koin 4.0.0, kotlinx-coroutines 1.8.0 (056-table-header-row)
- Room (KMP) with BundledSQLiteDriver — all persistence in shared `persistence` module (056-table-header-row)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3 + Compose Material3 (UI components), kotlinx-coroutines 1.8.0, lifecycle-viewmodel-compose 2.8.0 (057-grapheditor-text-editor)
- Filesystem (read/write CodeNode `.kt` source files via `File.readText()` / `File.writeText()`) (057-grapheditor-text-editor)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3 + kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, lifecycle-viewmodel-compose 2.8.0, java.net.HttpURLConnection (JVM standard library) (058-weather-forecast-demo)
- N/A (in-memory state only via MutableStateFlow) (058-weather-forecast-demo)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3, kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, lifecycle-viewmodel-compose 2.8.0, Ktor Client 3.1.1 (ktor-client-core, ktor-client-cio, ktor-client-content-negotiation, ktor-serialization-kotlinx-json) (058-weather-forecast-demo)
- N/A (in-memory state only) (058-weather-forecast-demo)
- Filesystem (`.kt` files at three tiers) — replaces `~/.codenode/custom-ip-types.json` (059-filesystem-ip-types)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3 + Gradle 8.13 (build system), git filter-repo (history extraction), Koin 4.0.0 (DI) (060-separate-repo)
- Git repositories (GitHub), Gradle composite builds (060-separate-repo)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + kotlinCompiler module (code generation), fbpDsl (runtime types) (061-codenode-definition-codegen)
- N/A (generates source code files) (061-codenode-definition-codegen)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Room 2.8.4 (KMP), Koin 4.0.0, kotlinx-coroutines 1.8.0, Compose Multiplatform 1.7.3 (062-group-persistence-files)
- Room (KMP) with BundledSQLiteDriver — shared `persistence` module (062-group-persistence-files)
- Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3 (UI), kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, lifecycle-viewmodel-compose 2.8.0 (063-save-graphnodes)
- Filesystem — `.flow.kts` template files at three tier locations (063-save-graphnodes)
- N/A (deliverables are documentation and test files) (064-vertical-slice-refactor)

- Kotlin 1.9+, Go 1.21+ (001-ide-plugin-platform)

## Project Structure

```text
src/
tests/
```

## Commands

# Add commands for Kotlin 1.9+, Go 1.21+

## Code Style

Kotlin 1.9+, Go 1.21+: Follow standard conventions

## Recent Changes
- 064-vertical-slice-refactor: Added Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3 (UI), kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, lifecycle-viewmodel-compose 2.8.0
- 064-vertical-slice-refactor: Added Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3 (UI), kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, lifecycle-viewmodel-compose 2.8.0
- 063-save-graphnodes: Added Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3 (UI), kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, lifecycle-viewmodel-compose 2.8.0


<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->

# Research: Migrate Module Runtimes

**Feature**: 052-migrate-module-runtimes
**Date**: 2026-03-16

## R1: CodeNodeDefinition Pattern for Non-Image Modules

**Decision**: Each module node becomes a Kotlin `object` implementing `CodeNodeDefinition`, following the exact EdgeArtFilter pattern from feature 050.

**Rationale**: The EdgeArtFilter migration proved the pattern works end-to-end: `object NodeName : CodeNodeDefinition` with `createRuntime()` returning the appropriate factory-created runtime. The same pattern applies directly to StopWatch (simpler, no persistence) and entity modules (more complex, with DAO dependencies).

**Alternatives considered**:
- Abstract base class per module type (e.g., `EntityCUDCodeNode<T>`) — rejected because the spec explicitly says "self-contained" and each node's processing logic is already module-specific. Generics would add complexity without reducing code.
- Code generation of CodeNodeDefinitions from `.flow.kt` DSL — rejected because the processing logic must be manually authored (it contains business logic, not boilerplate).

## R2: State Object Access from CodeNodeDefinition Objects

**Decision**: CodeNodeDefinition objects directly reference their module's State singleton (e.g., `StopWatchState`, `UserProfilesState`).

**Rationale**: All module State objects are already Kotlin `object` singletons with internal `MutableStateFlow` fields. The existing processing logic files (e.g., `timeIncrementerTick`) already access these singletons directly. CodeNodeDefinition objects will embed the same logic, maintaining the same access pattern.

**Key insight**: The EdgeArtFilter nodes (ImagePickerCodeNode, ImageViewerCodeNode) already demonstrate this pattern — they reference `EdgeArtFilterState` directly.

## R3: DAO Dependency Injection in Entity CodeNodes

**Decision**: Entity module Repository CodeNodes access DAOs via the existing Koin `Persistence` accessor objects (e.g., `UserProfilesPersistence.dao`).

**Rationale**: The current tick functions (e.g., `userProfileRepositoryTick`) already use `UserProfilesPersistence.dao` which is a `KoinComponent` with `by inject()`. The CodeNodeDefinition's `createRuntime()` will embed the same tick logic, using the same Koin accessor. No changes to Koin modules or persistence layer needed.

**Alternatives considered**:
- Pass DAO as constructor parameter to CodeNodeDefinition — rejected because CodeNodeDefinitions are `object` singletons (no constructor parameters).
- Create a CodeNodeDefinition subinterface with DI support — rejected because it adds framework complexity when Koin's `KoinComponent` pattern already works.

## R4: Runtime Type Mapping per Node

**Decision**: Each node maps to its exact current runtime type to preserve behavioral equivalence.

| Module | Node | Runtime Type | Factory Method | Key Behavior |
|--------|------|-------------|---------------|--------------|
| StopWatch | TimerEmitter | SourceOut2Runtime | `createSourceOut2<Int, Int>` | Combines StopWatchState flows, emits both elapsed values |
| StopWatch | TimeIncrementer | In2Out2Runtime | `createIn2Out2Processor<Int, Int, Int, Int>` | Synchronous receive from both inputs, increments time |
| StopWatch | DisplayReceiver | SinkIn2AnyRuntime | `createSinkIn2Any<Int, Int>` | Any-input trigger with initial values 0, 0 |
| UserProfiles | UserProfileCUD | SourceOut3Runtime | `createSourceOut3<Any, Any, Any>` | Collects from 3 state flows, emits selectively |
| UserProfiles | UserProfileRepository | In3AnyOut2Runtime | `createIn3AnyOut2Processor<Any, Any, Any, Any, Any>` | Any-input with identity tracking, DAO operations |
| UserProfiles | UserProfilesDisplay | SinkIn2Runtime | `createSinkIn2<Any, Any>` | Synchronous receive, updates result/error state |
| GeoLocations | GeoLocationCUD | SourceOut3Runtime | `createSourceOut3<Any, Any, Any>` | Same pattern as UserProfileCUD |
| GeoLocations | GeoLocationRepository | In3AnyOut2Runtime | `createIn3AnyOut2Processor<Any, Any, Any, Any, Any>` | Same pattern as UserProfileRepository |
| GeoLocations | GeoLocationsDisplay | SinkIn2Runtime | `createSinkIn2<Any, Any>` | Same pattern as UserProfilesDisplay |
| Addresses | AddressCUD | SourceOut3Runtime | `createSourceOut3<Any, Any, Any>` | Same pattern as UserProfileCUD |
| Addresses | AddressRepository | In3AnyOut2Runtime | `createIn3AnyOut2Processor<Any, Any, Any, Any, Any>` | Same pattern as UserProfileRepository |
| Addresses | AddressesDisplay | SinkIn2Runtime | `createSinkIn2<Any, Any>` | Same pattern as UserProfilesDisplay |

**Rationale**: Using the exact same factory methods and runtime types guarantees behavioral equivalence (FR-004). The only change is packaging — logic moves from separate tick files into CodeNodeDefinition `createRuntime()` methods.

## R5: Pre-Started Module Lifecycle Adaptation

**Decision**: Entity modules that were previously pre-started (controller.start() called in factory before RuntimeSession) will work naturally with DynamicPipelineController because:
1. DynamicPipelineController starts when RuntimeSession.start() is called
2. Entity source nodes (CUD) watch StateFlows with `.drop(1)`, so they wait for actual user actions
3. No pre-starting is needed — the pipeline starts idle and reacts to user input

**Rationale**: The pre-start pattern was needed because the generated controllers ran their flows immediately on construction. DynamicPipelineController defers pipeline construction to `start()`, which is called by RuntimeSession. The source nodes' `.drop(1)` pattern means they won't emit until the user performs a CRUD action, so no data is lost during the startup window.

**Impact**: `ModuleSessionFactory` no longer needs to call `controller.start()` before creating the RuntimeSession for entity modules. This simplifies the factory code.

## R6: ModuleSessionFactory Dynamic Session Adaptation

**Decision**: Extend the existing dynamic session pattern in ModuleSessionFactory to handle all 4 modules, creating module-specific ViewModels with DynamicPipelineController adapters.

**Rationale**: The factory already has the pattern for EdgeArtFilter:
1. Check `canBuildDynamic()` — returns true if all canvas nodes have CodeNodeDefinitions
2. Create `DynamicPipelineController` with `flowGraphProvider`
3. Create adapter implementing module's `ControllerInterface`
4. Create module-specific ViewModel with adapter

For StopWatch, the adapter implements `StopWatchControllerInterface`. For entity modules, the adapter implements the respective `ControllerInterface`. Each ViewModel is created with the adapter (and DAO for entity modules).

## R7: ServiceLoader Registration

**Decision**: Each module registers its CodeNodeDefinitions via `META-INF/services/io.codenode.fbpdsl.runtime.CodeNodeDefinition` files in the `jvmMain/resources/` directory.

**Rationale**: This matches the EdgeArtFilter pattern. `NodeDefinitionRegistry.discoverCompiledNodes()` uses `ServiceLoader.load(CodeNodeDefinition::class.java)` to find all implementations on the classpath. Each module's service file lists its 3 CodeNodeDefinition fully-qualified class names.

## R8: Identity Tracking in Repository CodeNodes

**Decision**: Embed the per-channel identity tracking (`lastSaveRef`, `lastUpdateRef`, `lastRemoveRef`) directly in the `createRuntime()` lambda closure, replacing the module-level `private var` fields.

**Rationale**: The current tick functions use file-level `private var` fields for identity tracking. When embedded in a CodeNodeDefinition's `createRuntime()`, these become local variables in the lambda closure, scoped to each runtime instance. This is cleaner (no global mutable state) and enables multiple instances of the same node type.

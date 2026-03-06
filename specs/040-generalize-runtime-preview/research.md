# Research: Generalize Runtime Preview

**Feature**: 040-generalize-runtime-preview
**Date**: 2026-03-06

## R1: RuntimeSession Generalization Strategy

**Decision**: Extract a common `ModuleController` interface from the shared lifecycle methods of all generated controllers, and make RuntimeSession accept this interface plus an opaque ViewModel reference.

**Rationale**: Both `StopWatchController` and `UserProfilesController` share identical lifecycle methods: `start()`, `stop()`, `pause()`, `resume()`, `reset()`, `setAttenuationDelay(ms)`, and `executionState: StateFlow<ExecutionState>`. These can be extracted into a common interface. Module-specific state (seconds/minutes for StopWatch, profiles for UserProfiles) stays on the concrete ViewModel — preview providers cast as needed.

**Alternatives considered**:
- **Lambda/factory-based**: Pass lambdas for creating controller/ViewModel to RuntimeSession. Rejected: creates complex, hard-to-read constructor signatures.
- **Reflection/service loader**: Discover module components at runtime via classpath scanning. Rejected: overkill for the current number of modules, adds runtime complexity.
- **Fully generic RuntimeSession<T>**: Parameterize RuntimeSession with ViewModel type. Rejected: forces RuntimePreviewPanel to be generic too, rippling type parameters through the UI layer.

## R2: Preview Provider Registration Pattern

**Decision**: Create a `PreviewRegistry` object in the graphEditor that maps composable names to `@Composable` rendering lambdas. Replace the hardcoded `when` block in RuntimePreviewPanel with a registry lookup.

**Rationale**: The `when` block is the primary source of the "Preview not available" problem. A registry allows each module's preview provider to register its composables at initialization time. Adding a new module requires only adding registry entries — no changes to RuntimePreviewPanel.

**Alternatives considered**:
- **Annotation-based discovery**: Annotate preview functions and discover them at compile time. Rejected: requires annotation processing, adds build complexity for minimal benefit.
- **Convention-based reflection**: Look up `{ModuleName}PreviewProvider` by name via reflection. Rejected: fragile, loses compile-time safety.
- **Plugin architecture**: Define a module plugin interface with preview methods. Rejected: over-engineered for the current scale.

## R3: ViewModel Commonality

**Decision**: No common ViewModel interface. Store the ViewModel as `Any` on RuntimeSession. Preview providers cast to the concrete type they need.

**Rationale**: The two ViewModels share lifecycle control methods but their observable state is completely different (Int seconds/minutes vs List<UserProfileEntity> profiles). Creating a common interface would either be too thin to be useful or require type-unsafe `Any` properties anyway. Preview providers already know which ViewModel type they expect — casting is safe and simple.

**Alternatives considered**:
- **Common ModuleViewModel interface**: Define interface with start/stop/pause/resume/executionState. Rejected: RuntimeSession already delegates lifecycle to the controller, not the ViewModel. The ViewModel is only used by preview providers, which need the concrete type.
- **Sealed class hierarchy**: Define `sealed class ModuleViewModel`. Rejected: forces all ViewModels into a common hierarchy, conflicts with `androidx.lifecycle.ViewModel` base class.

## R4: graphEditor Module Dependencies

**Decision**: Add `implementation(project(":UserProfiles"))` to graphEditor's `build.gradle.kts`. The graphEditor is the desktop IDE tool and must know about all previewable modules.

**Rationale**: The graphEditor renders module-specific composables. It needs compile-time access to the composable functions and ViewModel types. Since the graphEditor is JVM-only (desktop), adding module dependencies doesn't affect mobile builds.

**Alternatives considered**:
- **Dynamic class loading**: Load module classes at runtime from the module directory. Rejected: extremely complex, loses type safety, fragile across Kotlin/JVM versions.
- **IPC-based preview**: Run module previews in a separate process, render to image. Rejected: massive implementation effort, poor interactivity.

## R5: Controller Interface Location

**Decision**: Place the `ModuleController` interface in the `fbpDsl` module (the shared runtime library), since all generated controllers already depend on `fbpDsl` for `ExecutionState`, `FlowGraph`, and runtime classes.

**Rationale**: The `fbpDsl` module is the natural home for shared runtime abstractions. Both `circuitSimulator` and all generated modules already depend on it. Placing the interface here avoids circular dependencies.

**Alternatives considered**:
- **In circuitSimulator**: Would work, but circuitSimulator depends on fbpDsl, and generated modules don't depend on circuitSimulator — so modules couldn't implement the interface.
- **New shared module**: Overkill for a single interface. Adds build complexity.

## R6: UserProfiles Preview in graphEditor Context

**Decision**: The UserProfiles preview in the graphEditor will display the `UserProfiles` composable using a ViewModel initialized from the RuntimeSession. Since UserProfiles depends on Room/database, the graphEditor preview will need database initialization for JVM (using the existing `DatabaseBuilder.jvm.kt` which creates a file-based database at `~/.codenode/data/app.db`).

**Rationale**: The existing JVM database builder already handles this case — it creates a file-based Room database without requiring Android context. The graphEditor runs on JVM desktop, which is already a supported target for the UserProfiles module.

**Alternatives considered**:
- **Mock ViewModel with fake data**: Create a preview-only ViewModel with hardcoded state. Rejected: loses the "live preview" value — users want to see real runtime behavior.
- **In-memory database for preview**: Use an in-memory Room database. Rejected: the existing file-based JVM builder works fine, and persistence across preview sessions is actually useful.

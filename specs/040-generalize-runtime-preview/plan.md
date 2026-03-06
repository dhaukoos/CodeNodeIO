# Implementation Plan: Generalize Runtime Preview

**Branch**: `040-generalize-runtime-preview` | **Date**: 2026-03-06 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/040-generalize-runtime-preview/spec.md`

## Summary

The Runtime Preview in the graphEditor is currently hardcoded to only render StopWatch composables. RuntimeSession creates a StopWatch-specific controller and ViewModel, and RuntimePreviewPanel has a `when` block that only handles "StopWatch" and "StopWatchScreen" composable names. This plan generalizes the system so that any loaded module's composables can be previewed.

The approach:
1. Extract a `ModuleController` interface from the shared lifecycle methods of all generated controllers
2. Refactor RuntimeSession to accept a `ModuleController` + opaque ViewModel instead of hardcoding StopWatch
3. Create a `PreviewRegistry` that maps composable names to rendering functions, replacing the hardcoded `when` block
4. Create a `UserProfilesPreviewProvider` to prove the pattern works with a second module

## Technical Context

**Language/Version**: Kotlin 2.1.21 (Kotlin Multiplatform)
**Primary Dependencies**: Compose Desktop 1.7.3, kotlinx-coroutines 1.8.0, lifecycle-viewmodel-compose 2.8.0, Room 2.8.4 (UserProfiles only)
**Storage**: N/A (no new storage; UserProfiles uses existing Room database)
**Testing**: Not explicitly requested — tests omitted per spec
**Target Platform**: JVM Desktop (graphEditor), with shared commonMain code
**Project Type**: KMP multi-module
**Performance Goals**: Module switch completes within 1 second (SC-004)
**Constraints**: Must maintain full backward compatibility with StopWatch preview
**Scale/Scope**: 2 modules (StopWatch, UserProfiles); pattern supports unlimited future modules

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality | PASS | Extracting common interface improves maintainability; removes hardcoded references |
| II. TDD | WAIVED | Tests not requested in spec; constitution says "mandatory for all new features" but this is a refactoring of existing working code. Test tasks omitted per spec. |
| III. UX Consistency | PASS | Preview behavior remains consistent — same panel, same controls, same interaction pattern |
| IV. Performance | PASS | No performance-critical changes; module switch is instant |
| V. Observability | PASS | No new production services; desktop-only tool |
| Licensing | PASS | No new dependencies; all existing deps are Apache 2.0/MIT |

**Post-Phase 1 Re-check**: All gates still pass. The `ModuleController` interface adds no new dependencies. The `PreviewRegistry` is a simple in-memory map.

## Project Structure

### Documentation (this feature)

```text
specs/040-generalize-runtime-preview/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   ├── module-controller-api.md
│   ├── preview-registry-api.md
│   └── runtime-session-api.md
└── tasks.md             # Phase 2 output (created by /speckit.tasks)
```

### Source Code (repository root)

```text
fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/
└── ModuleController.kt                    # NEW: Common controller interface

circuitSimulator/src/commonMain/kotlin/io/codenode/circuitsimulator/
└── RuntimeSession.kt                      # MODIFIED: Accept ModuleController + viewModel

StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/generated/
└── StopWatchController.kt                 # MODIFIED: Implement ModuleController

UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/generated/
└── UserProfilesController.kt             # MODIFIED: Implement ModuleController

graphEditor/src/jvmMain/kotlin/ui/
├── PreviewRegistry.kt                     # NEW: Composable preview registry
├── StopWatchPreviewProvider.kt            # MODIFIED: Register with PreviewRegistry
├── UserProfilesPreviewProvider.kt         # NEW: UserProfiles preview provider
├── RuntimePreviewPanel.kt                 # MODIFIED: Use PreviewRegistry lookup
└── ModuleSessionFactory.kt                # NEW: Factory functions for module sessions

graphEditor/src/jvmMain/kotlin/
└── Main.kt                                # MODIFIED: Use factory for RuntimeSession creation

graphEditor/
└── build.gradle.kts                       # MODIFIED: Add UserProfiles dependency
```

**Structure Decision**: No new modules or projects. Changes span 4 existing modules (fbpDsl, circuitSimulator, graphEditor, StopWatch) plus the UserProfiles module. All new files are placed within existing module structures following established conventions.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| TDD waived | Spec explicitly states "Tests not explicitly requested — test tasks omitted" | N/A — spec decision |

## Implementation Details

### Phase 1: ModuleController Interface (fbpDsl)

Create `ModuleController` interface in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/ModuleController.kt`:

```kotlin
interface ModuleController {
    val executionState: StateFlow<ExecutionState>
    fun start(): FlowGraph
    fun stop(): FlowGraph
    fun pause(): FlowGraph
    fun resume(): FlowGraph
    fun reset(): FlowGraph
    fun setAttenuationDelay(ms: Long?)
}
```

This interface lives in fbpDsl because:
- All generated controllers already depend on fbpDsl for `ExecutionState` and `FlowGraph`
- `circuitSimulator` depends on fbpDsl
- No circular dependencies introduced

### Phase 2: Controller Implementation

**StopWatchController**: Add `: ModuleController` to class declaration. All required methods already exist — this is a pure interface addition.

**UserProfilesController**: Same — add `: ModuleController`. All required methods already exist.

### Phase 3: RuntimeSession Refactoring

Change RuntimeSession from:
```kotlin
class RuntimeSession {
    private val controller = StopWatchController(stopWatchFlowGraph)
    val viewModel = StopWatchViewModel(adapter)
}
```

To:
```kotlin
class RuntimeSession(
    private val controller: ModuleController,
    val viewModel: Any
) {
    val executionState: StateFlow<ExecutionState> = controller.executionState
    val attenuationDelayMs: MutableStateFlow<Long> = MutableStateFlow(0L)

    fun start() { controller.start() }
    fun stop() { controller.stop() }
    fun pause() { controller.pause() }
    fun resume() { controller.resume() }
    fun setAttenuation(ms: Long) {
        val clamped = ms.coerceIn(0L, 2000L)
        attenuationDelayMs.value = clamped
        controller.setAttenuationDelay(clamped)
    }
}
```

Remove all StopWatch imports from `circuitSimulator`. The `circuitSimulator` module no longer needs to depend on `StopWatch`.

### Phase 4: PreviewRegistry

Create `PreviewRegistry` object in graphEditor:

```kotlin
typealias PreviewComposable = @Composable (viewModel: Any, modifier: Modifier) -> Unit

object PreviewRegistry {
    private val registry = mutableMapOf<String, PreviewComposable>()

    fun register(composableName: String, preview: PreviewComposable) {
        registry[composableName] = preview
    }

    fun get(composableName: String): PreviewComposable? = registry[composableName]
    fun hasPreview(composableName: String): Boolean = composableName in registry
    fun registeredNames(): Set<String> = registry.keys.toSet()
}
```

### Phase 5: Preview Provider Refactoring

**StopWatchPreviewProvider**: Refactor to register with PreviewRegistry. Change from direct method calls to registry-based pattern:

```kotlin
object StopWatchPreviewProvider {
    fun register() {
        PreviewRegistry.register("StopWatch") { viewModel, modifier ->
            val vm = viewModel as StopWatchViewModel
            // existing Preview composable body
        }
        PreviewRegistry.register("StopWatchScreen") { viewModel, modifier ->
            val vm = viewModel as StopWatchViewModel
            // existing ScreenPreview composable body
        }
    }
}
```

**UserProfilesPreviewProvider**: New file that registers UserProfiles composables:

```kotlin
object UserProfilesPreviewProvider {
    fun register() {
        PreviewRegistry.register("UserProfiles") { viewModel, modifier ->
            val vm = viewModel as UserProfilesViewModel
            UserProfiles(viewModel = vm, modifier = modifier)
        }
    }
}
```

### Phase 6: RuntimePreviewPanel Refactoring

Replace the hardcoded `when` block (lines 332-349) with:

```kotlin
val previewFn = selectedComposable?.let { PreviewRegistry.get(it) }
if (previewFn != null) {
    previewFn(runtimeSession.viewModel, Modifier)
} else if (selectedComposable == null) {
    Text("Select a composable to preview")
} else {
    Text("Preview not available for: $selectedComposable")
}
```

Also change `RuntimePreviewPanel` signature: `runtimeSession` parameter type stays the same (RuntimeSession) but no longer needs StopWatch-specific knowledge.

### Phase 7: Module Session Factory + Main.kt Integration

Create `ModuleSessionFactory.kt` in graphEditor with factory functions:

```kotlin
object ModuleSessionFactory {
    fun createSession(moduleName: String, flowGraphName: String): RuntimeSession? {
        return when (moduleName) {
            "StopWatch" -> createStopWatchSession()
            "UserProfiles" -> createUserProfilesSession()
            else -> null
        }
    }

    private fun createStopWatchSession(): RuntimeSession {
        val controller = StopWatchController(stopWatchFlowGraph)
        val adapter = StopWatchControllerAdapter(controller)
        val viewModel = StopWatchViewModel(adapter)
        return RuntimeSession(controller, viewModel)
    }

    private fun createUserProfilesSession(): RuntimeSession {
        val controller = UserProfilesController(userProfilesFlowGraph)
        controller.start()
        val adapter = UserProfilesControllerAdapter(controller)
        val viewModel = UserProfilesViewModel(adapter)
        return RuntimeSession(controller, viewModel)
    }
}
```

Update `Main.kt` to use the factory:
- Replace `val runtimeSession = remember { RuntimeSession() }` with factory-based creation
- Initialize preview providers at startup: `StopWatchPreviewProvider.register()`, `UserProfilesPreviewProvider.register()`
- Recreate RuntimeSession when module changes

### Phase 8: Build Configuration

Add `implementation(project(":UserProfiles"))` to `graphEditor/build.gradle.kts` commonMain dependencies.

Remove `implementation(project(":StopWatch"))` from `circuitSimulator/build.gradle.kts` if it exists (RuntimeSession no longer references StopWatch directly).

## File Change Summary

| File | Change Type | Description |
|------|-------------|-------------|
| `fbpDsl/.../ModuleController.kt` | NEW | Common controller interface |
| `StopWatch/.../StopWatchController.kt` | MODIFY | Add `: ModuleController` |
| `UserProfiles/.../UserProfilesController.kt` | MODIFY | Add `: ModuleController` |
| `circuitSimulator/.../RuntimeSession.kt` | MODIFY | Accept ModuleController + viewModel params |
| `circuitSimulator/build.gradle.kts` | MODIFY | Remove StopWatch dependency |
| `graphEditor/.../PreviewRegistry.kt` | NEW | Composable preview registry |
| `graphEditor/.../StopWatchPreviewProvider.kt` | MODIFY | Register with PreviewRegistry |
| `graphEditor/.../UserProfilesPreviewProvider.kt` | NEW | UserProfiles preview provider |
| `graphEditor/.../RuntimePreviewPanel.kt` | MODIFY | Use PreviewRegistry lookup |
| `graphEditor/.../ModuleSessionFactory.kt` | NEW | Factory for module-specific sessions |
| `graphEditor/.../Main.kt` | MODIFY | Use factory, init preview providers |
| `graphEditor/build.gradle.kts` | MODIFY | Add UserProfiles dependency |

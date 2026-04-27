# Phase 1 Data Model: UI-FBP Runtime Preview

**Date**: 2026-04-26
**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Research**: [research.md](./research.md)

This feature introduces no new persistence and no new domain model. It does add **typed inputs and outputs to the generator pipeline** and a **structured re-generation summary**. Those are the entities described here. Existing types (`UIFBPSpec`, `PortInfo`, `UIFBPParseResult`, `UIFBPGenerateResult`, `UIFBPGeneratedFile`) are referenced but unchanged in shape unless noted.

---

## 1. UIFBPSpec (existing — used as-is)

Source: `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/parser/UIFBPSpec.kt`

```kotlin
data class UIFBPSpec(
    val moduleName: String,           // e.g., "DemoUI" — derived from Composable function name
    val viewModelTypeName: String,    // e.g., "DemoUIViewModel" — derived from viewModel parameter type
    val packageName: String,          // e.g., "io.codenode.demo" — package of the source UI file
    val sourceOutputs: List<PortInfo>,// Composable input parameters (excluding viewModel) → Source CodeNode outputs
    val sinkInputs: List<PortInfo>,   // ViewModel-observed StateFlow properties → Sink CodeNode inputs
    val ipTypeImports: List<String>   // Fully-qualified IP types referenced by ports (for import generation)
)

data class PortInfo(
    val name: String,                 // Property/parameter name
    val typeName: String,             // Simple type name (e.g., "Double", "CalculationResults")
    val isNullable: Boolean = false   // Whether the type is nullable
)
```

**Validation rules** (carried from current parser; relevant to this feature):

- `moduleName` MUST be a valid Kotlin identifier; it is also used as the PreviewRegistry key and so MUST equal the qualifying `@Composable` function's name.
- `packageName` determines the on-disk path; generators MUST translate `.` to `/` consistently.
- Every type in `PortInfo.typeName` that is not a Kotlin built-in MUST appear in `ipTypeImports` so that generated files can import it.

**Relationships**:

- `UIFBPSpec` is the **single input** to all generators in this feature. Two new generators (`UIFBPControllerInterfaceGenerator`, `UIFBPPreviewProviderGenerator`) consume it; one existing generator (`UIFBPViewModelGenerator`) consumes it under a modified contract (constructor signature change).

---

## 2. UIFBPGenerateResult / UIFBPGeneratedFile (existing — extended)

Source: `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPInterfaceGenerator.kt`

The result type stays the same:

```kotlin
data class UIFBPGenerateResult(
    val success: Boolean,
    val filesGenerated: List<UIFBPGeneratedFile> = emptyList(),
    val errorMessage: String? = null
)

data class UIFBPGeneratedFile(
    val relativePath: String,         // Module-relative path, forward-slashed
    val content: String               // Full file content including license header
)
```

**Behavioral change**: `UIFBPInterfaceGenerator.generateAll(spec, includeFlowKt)` now emits the full thick stack — appending five additional `UIFBPGeneratedFile` entries (three from reused entity-module generators, two from new UI-FBP generators) on every call:

| Index | `relativePath` | Source generator |
|---|---|---|
| 1 | `src/commonMain/kotlin/{pkg}/viewmodel/{Module}State.kt` | `UIFBPStateGenerator` (path change) |
| 2 | `src/commonMain/kotlin/{pkg}/viewmodel/{Module}ViewModel.kt` | `UIFBPViewModelGenerator` (path + content change) |
| 3 | `src/commonMain/kotlin/{pkg}/nodes/{Module}SourceCodeNode.kt` | `UIFBPSourceCodeNodeGenerator` (unchanged) |
| 4 | `src/commonMain/kotlin/{pkg}/nodes/{Module}SinkCodeNode.kt` | `UIFBPSinkCodeNodeGenerator` (unchanged) |
| 5 | `src/commonMain/kotlin/{pkg}/controller/{Module}ControllerInterface.kt` | **NEW** `UIFBPControllerInterfaceGenerator` (or `RuntimeControllerInterfaceGenerator` via adapter) |
| 6 | `src/commonMain/kotlin/{pkg}/controller/{Module}Controller.kt` | **REUSED** `RuntimeControllerGenerator` via `UIFBPSpecAdapter` |
| 7 | `src/commonMain/kotlin/{pkg}/controller/{Module}ControllerAdapter.kt` | **REUSED** `RuntimeControllerAdapterGenerator` via `UIFBPSpecAdapter` |
| 8 | `src/commonMain/kotlin/{pkg}/flow/{Module}Flow.kt` | **REUSED** `RuntimeFlowGenerator` via `UIFBPSpecAdapter` |
| 9 | `src/jvmMain/kotlin/{pkg}/userInterface/{Module}PreviewProvider.kt` | **NEW** `UIFBPPreviewProviderGenerator` |
| 10 (opt) | `src/commonMain/kotlin/{pkg}/flow/{Module}.flow.kt` | `UIFBPInterfaceGenerator.generateBootstrapFlowKt` (only when `includeFlowKt` AND target file does not yet exist; otherwise emitted by `UIFBPSaveService` as a merge) |

Indices 3 and 4 may be skipped if `spec.sourceOutputs` / `spec.sinkInputs` are empty (existing behavior preserved). Index 5 is always emitted; the interface degenerates to one with only `executionState` and the control methods if `sinkInputs` is empty. Indices 6, 7, 8, and 9 are always emitted.

**`UIFBPSpecAdapter` (new)**: translates `UIFBPSpec` into the input shape each reused generator expects. The reused generators were designed for entity modules (which have richer specs including persistence, repository, CUD operations). For UI-FBP, the adapter populates only the fields the runtime generators actually use (FlowGraph shape, port wiring, ControllerInterface shape) and leaves entity-only fields empty/null. Unit tests verify that the adapter's outputs produce identical thick-stack code to what entity modules emit, modulo entity-specific concerns the adapter zeros out.

---

## 3. UIFBPSaveService (NEW — orchestration entity)

Lives in `flowGraph-generate/src/jvmMain/kotlin/io/codenode/flowgraphgenerate/save/UIFBPSaveService.kt`. Composes the pure commonMain generators with filesystem I/O and the `.flow.kt` merge.

```kotlin
class UIFBPSaveService(
    private val generator: UIFBPInterfaceGenerator = UIFBPInterfaceGenerator(),
    private val flowKtParser: FlowKtParser = FlowKtParser(),
    private val flowGraphSerializer: FlowGraphSerializer = FlowGraphSerializer()
) {
    fun save(
        spec: UIFBPSpec,
        moduleRoot: File,
        options: UIFBPSaveOptions = UIFBPSaveOptions()
    ): UIFBPSaveResult
}

data class UIFBPSaveOptions(
    val deleteLegacyLocations: Boolean = false,        // If true, removes saved/ and base-package State/ViewModel files
    val touchUpBuildGradle: Boolean = true,            // If false, skip build.gradle.kts edits
    val mergeExistingFlowKt: Boolean = true            // If false, leave existing .flow.kt untouched
)
```

### UIFBPSaveResult

```kotlin
data class UIFBPSaveResult(
    val success: Boolean,
    val files: List<FileChange>,                        // One entry per file the service decided about
    val flowKtMerge: FlowKtMergeReport?,                // null if no .flow.kt was processed
    val buildGradleEdit: BuildGradleEditReport?,        // null if touchUpBuildGradle = false
    val warnings: List<String>,
    val errorMessage: String? = null
)

enum class FileChangeKind { CREATED, UPDATED, UNCHANGED, SKIPPED_CONFLICT, DELETED }

data class FileChange(
    val relativePath: String,
    val kind: FileChangeKind,
    val reason: String? = null                          // Populated for SKIPPED_CONFLICT and DELETED
)

data class FlowKtMergeReport(
    val mode: FlowKtMergeMode,                          // CREATED | UPDATED | UNCHANGED | PARSE_FAILED_SKIPPED
    val portsAdded: List<PortChange>,
    val portsRemoved: List<PortChange>,
    val connectionsDropped: List<DroppedConnection>,
    val userNodesPreserved: Int                         // Count of CodeNodes that are neither Source nor Sink
)

enum class FlowKtMergeMode { CREATED, UPDATED, UNCHANGED, PARSE_FAILED_SKIPPED }

data class PortChange(val nodeName: String, val portName: String, val typeName: String)
data class DroppedConnection(val from: String, val to: String, val reason: String)

data class BuildGradleEditReport(
    val addedJvmTarget: Boolean,
    val addedPreviewApiDependency: Boolean,
    val skippedReason: String? = null                   // E.g., "non-standard build script structure"
)
```

### Behavioral contract

1. **Idempotency**: `save(...)` followed by `save(...)` against the same inputs MUST produce a result where every `FileChange.kind` in the second call is `UNCHANGED` (modulo file-system mtimes, which are not part of the contract). The `FlowKtMergeReport.mode` MUST be `UNCHANGED` on the second call.
2. **Conflict handling**: Any target file whose existing on-disk content differs from the would-be-emitted content is treated as a conflict candidate; the service compares semantic content (parsed or trimmed) and only marks it `UPDATED` if the change is generator-induced (Source/Sink port shape changed, controller interface gained/lost a flow getter). User-edited files at non-generator-targeted paths (e.g., the `.flow.kt`) are merged, not overwritten.
3. **Hand-written collision**: If a file already exists at a generator-target path with a content shape that does NOT carry the `Generated by CodeNodeIO UIFBPInterfaceGenerator` marker comment, the service marks it `SKIPPED_CONFLICT` with a reason and does NOT overwrite. This implements FR-016.
4. **Legacy cleanup**: When `deleteLegacyLocations = true`, the service deletes:
   - `src/commonMain/kotlin/{pkg}/saved/{Module}State.kt`
   - `src/commonMain/kotlin/{pkg}/saved/{Module}ViewModel.kt`
   - `src/commonMain/kotlin/{pkg}/{Module}State.kt` (base package legacy)
   - `src/commonMain/kotlin/{pkg}/{Module}ViewModel.kt` (base package legacy)
   - And removes the empty `saved/` directory if applicable.
   Each deletion appears in `files` with `kind = DELETED` and a reason.
5. **`build.gradle.kts` touch-up**: The service inspects the file as text. If a `jvm {` block is absent, it inserts a conventional one inside the `kotlin { ... }` block. If `implementation("io.codenode:preview-api")` is absent from any `jvmMain` source-set block, it inserts the dependency, creating the `val jvmMain by getting { dependencies { ... } }` block if needed. If the script is heavily customized (heuristic: presence of unparseable patterns), the service emits a warning naming both edits and skips. This implements FR-008/FR-009.

### Validation

- `moduleRoot` MUST be an existing directory containing `build.gradle.kts`. Otherwise `success = false` with a clear `errorMessage`.
- `spec.moduleName` MUST equal the `@Composable` function name in the source file (validated by the upstream parser, not this service).

---

## 4. Generated `{Module}ControllerInterface` shape (the contract)

The **emitted Kotlin interface** is the same shape used today by entity-module ControllerInterfaces (Addresses, UserProfiles), which works with both runtime paths:

- **Runtime Preview path**: GraphEditor's reflection proxy implements the interface (via `ModuleSessionFactory.createControllerProxy`). State-flow getters resolve to `{Module}State.{port}Flow` fields by reflection; control methods route to `DynamicPipelineController`.
- **Production-app path**: the generated `{Module}Controller` class implements the interface directly, with state-flow getters backed by its `{Module}Flow` runtime instance.

For a spec with `sinkInputs = [results: CalculationResults?]` and `sourceOutputs = [a: Double, b: Double]`, the emitted interface is:

```kotlin
interface DemoUIControllerInterface {
    val results: StateFlow<CalculationResults?>      // From sinkInputs (UI observes)
    val executionState: StateFlow<ExecutionState>    // Required by proxy AND production-app controller
    fun start(): FlowGraph
    fun stop(): FlowGraph
    fun pause(): FlowGraph
    fun resume(): FlowGraph
    fun reset(): FlowGraph
}
```

The interface declares one `val xxx: StateFlow<T>` per `sinkInput` (display outputs the UI observes). Source outputs are NOT mirrored on the interface — the UI emits them via `viewModel.emit(...)` writing to `{Module}State._x` mutable fields, but does not observe them back through the controller. This matches `AddressesControllerInterface` and `WeatherForecastControllerInterface`.

**Type aliasing**: `PortInfo.typeName` is rendered verbatim in the interface; nullable types append `?`. The full FQCN appears in the file's imports via `spec.ipTypeImports`.

---

## 5. Generated `{Module}ViewModel` shape (the dependency surface)

Matches the `WeatherForecastViewModel` / `AddressesViewModel` shape — flows read directly from `{Module}State`, control methods delegated through the controller:

```kotlin
class DemoUIViewModel(
    private val controller: DemoUIControllerInterface
) : androidx.lifecycle.ViewModel() {
    // Observable state from module properties
    val results: StateFlow<CalculationResults?> = DemoUIState.resultsFlow

    // Execution state from controller
    val executionState: StateFlow<ExecutionState> = controller.executionState

    // Source emit method
    fun emit(a: Double, b: Double) {
        DemoUIState._a.value = a
        DemoUIState._b.value = b
    }

    // Control methods
    fun start(): FlowGraph = controller.start()
    fun stop(): FlowGraph = controller.stop()
    fun pause(): FlowGraph = controller.pause()
    fun resume(): FlowGraph = controller.resume()
    fun reset(): FlowGraph = controller.reset()
}
```

**Constraints**:
- The constructor MUST be `public` and the parameter MUST be exactly `({Module}ControllerInterface)` so that `ModuleSessionFactory.tryCreateViewModel` matches it via `parameterTypes[0].isAssignableFrom(interfaceClass)`.
- The class MUST extend `androidx.lifecycle.ViewModel` (matches existing convention; required by the `lifecycle-viewmodel-compose` integration the GraphEditor uses).
- `emit(...)` continues to write to `{Module}State._x` mutable fields (same as today). This preserves the existing UI → State → SourceCodeNode flow.
- Sink-input flows are read from `{Module}State.{y}Flow` directly (not via `controller.{y}`), matching WeatherForecast/Addresses precedent. The same code works under both runtime paths because both paths mutate the same `{Module}State` object.

---

## 6. State transitions (re-generation lifecycle)

The only "state machine" in this feature is the per-file decision tree inside `UIFBPSaveService`:

```
For each generator-target file:

  EXISTS?
    NO  → write new file → kind = CREATED
    YES → CONTENT MATCHES would-be-emitted?
            YES → kind = UNCHANGED
            NO  → CARRIES GENERATOR MARKER COMMENT?
                    YES → write new file → kind = UPDATED
                    NO  → leave file → kind = SKIPPED_CONFLICT, warning emitted

For .flow.kt:
  EXISTS?
    NO  → emit bootstrap → mode = CREATED
    YES → PARSEABLE?
            NO  → leave file → mode = PARSE_FAILED_SKIPPED, warning emitted
            YES → DIFF Source/Sink ports against spec
                  IF no diff → mode = UNCHANGED
                  ELSE       → apply port adds/removes, drop invalid connections,
                               re-serialize → mode = UPDATED
```

This is the entirety of the dynamic behavior introduced by the feature; no domain entities have lifecycle states.

---

## 7. What this feature does NOT model

- No new persistence. No database schemas. No migration scripts.
- No changes to `FlowGraph`, `CodeNode`, `Node`, or any `fbpDsl` model classes.
- No changes to `DynamicPipelineController`, `DynamicPipelineBuilder`, `PreviewRegistry`, `DynamicPreviewDiscovery`, or `ModuleSessionFactory` (existing contracts are sufficient — see research Decisions 1–4).
- No changes to the four reused entity-module generators (`RuntimeControllerInterfaceGenerator`, `RuntimeControllerGenerator`, `RuntimeControllerAdapterGenerator`, `RuntimeFlowGenerator`); UI-FBP only adapts inputs to call them.
- No new IP types; the generator continues to use whatever IP types are already discovered by `IPTypeRegistry`.
- No universal-runtime collapse (per spec Clarifications Q2 — deferred to a follow-up feature). The thick stack remains intact.

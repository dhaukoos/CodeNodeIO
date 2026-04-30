# Phase 1 Data Model: UI-FBP Runtime Preview (post-085)

**Date**: 2026-04-28
**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Research**: [research.md](./research.md)

This feature introduces no new persistence and no new domain model. It adds **typed inputs to the generator pipeline** (one new field on `UIFBPSpec`, one new parameter on `PreviewProviderGenerator`) and a **structured re-generation result** (`UIFBPSaveResult` and friends, owned by the new `UIFBPSaveService`). Existing types from the post-085 generator surface (`FlowGraph`, `RuntimeControllerInterfaceGenerator`, `ModuleRuntimeGenerator`, `GenerationFileWriter.FileChangeKind`) are referenced as-is.

---

## 1. UIFBPSpec (existing — extended)

Source: `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/parser/UIFBPSpec.kt`

**Pre-clarification (current shape on disk)**:

```kotlin
data class UIFBPSpec(
    val moduleName: String,            // Conflated: was Composable name + flow-graph prefix + module name
    val viewModelTypeName: String,
    val packageName: String,
    val sourceOutputs: List<PortInfo>,
    val sinkInputs: List<PortInfo>,
    val ipTypeImports: List<String>
)
```

**Post-clarification (target shape)**: per Decision 2, the `moduleName` conflation is broken. Three potentially-distinct identifiers become typed fields:

```kotlin
data class UIFBPSpec(
    val flowGraphPrefix: String,       // Drives generated-file prefix + PreviewRegistry key. Derived from the user-selected .flow.kt file (filename minus `.flow.kt`)
    val composableName: String,        // The user-authored Composable function name from the qualifying UI source file. Drives the function call inside PreviewProvider's register block
    val packageName: String,           // Drives on-disk path translation; e.g., "io.codenode.demo" → src/commonMain/kotlin/io/codenode/demo/
    val viewModelTypeName: String,     // Derived from {flowGraphPrefix}ViewModel post-clarification (was conflated with moduleName)
    val sourceOutputs: List<PortInfo>, // Composable input parameters (excluding viewModel) → Source CodeNode outputs
    val sinkInputs: List<PortInfo>,    // ViewModel-observed StateFlow properties → Sink CodeNode inputs
    val ipTypeImports: List<String>    // Fully-qualified IP types referenced by ports
)

data class PortInfo(
    val name: String,
    val typeName: String,
    val isNullable: Boolean = false
)
```

**Migration**: The `moduleName` field is removed entirely (or retained briefly as a deprecated alias resolving to `flowGraphPrefix` for tooling that still references it). `UIComposableParser` is updated to populate `composableName` from the parsed `@Composable fun X(viewModel: ...)` declaration (already extracted) and `flowGraphPrefix` from the user-selected `.flow.kt` file passed in alongside the UI file (the explicit-pair input from FR-014/FR-015).

**Validation rules**:

- `flowGraphPrefix` MUST be a valid Kotlin identifier (PascalCase recommended, but generator enforces casing for emitted class names).
- `composableName` MUST equal the parsed `@Composable` function name verbatim — the generator does not transform it.
- `packageName` MUST be a non-empty dotted package path; generator translates `.` to `/` consistently.
- Every type in `PortInfo.typeName` that is not a Kotlin built-in MUST appear in `ipTypeImports`.

**Relationships**: `UIFBPSpec` is the single typed input to the UI-FBP-specific generators (`UIFBPStateGenerator`, `UIFBPViewModelGenerator`, `UIFBPSourceCodeNodeGenerator`, `UIFBPSinkCodeNodeGenerator`). The 085-owned generators (`RuntimeControllerInterfaceGenerator`, `ModuleRuntimeGenerator`, `PreviewProviderGenerator`) take a `FlowGraph` model — `UIFBPInterfaceGenerator` (the orchestrator) translates `UIFBPSpec` into that `FlowGraph` (Source + Sink CodeNodes wired into an empty middle).

---

## 2. UIFBPGenerateResult / UIFBPGeneratedFile (existing — extended)

Source: `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPInterfaceGenerator.kt`

The result type stays the same shape:

```kotlin
data class UIFBPGenerateResult(
    val success: Boolean,
    val filesGenerated: List<UIFBPGeneratedFile> = emptyList(),
    val errorMessage: String? = null
)

data class UIFBPGeneratedFile(
    val relativePath: String,          // Module-relative path, forward-slashed
    val content: String                // Full file content including license header + generator marker
)
```

**Behavioral change**: `UIFBPInterfaceGenerator.generateAll(spec, includeFlowKt)` now emits the post-085 universal set — eight `UIFBPGeneratedFile` entries per call:

| Index | `relativePath` (template) | Source generator | Reuse status |
|---|---|---|---|
| 1 | `src/commonMain/kotlin/{pkg}/viewmodel/{FlowGraph}State.kt` | `UIFBPStateGenerator` | MODIFY (path migration to `viewmodel/`; prefix from flow-graph) |
| 2 | `src/commonMain/kotlin/{pkg}/viewmodel/{FlowGraph}ViewModel.kt` | `UIFBPViewModelGenerator` | MODIFY (constructor `({FlowGraph}ControllerInterface)`; flows from State; prefix from flow-graph) |
| 3 | `src/commonMain/kotlin/{pkg}/nodes/{FlowGraph}SourceCodeNode.kt` | `UIFBPSourceCodeNodeGenerator` | MODIFY (minimal: prefix from flow-graph) |
| 4 | `src/commonMain/kotlin/{pkg}/nodes/{FlowGraph}SinkCodeNode.kt` | `UIFBPSinkCodeNodeGenerator` | MODIFY (minimal: prefix from flow-graph) |
| 5 | `src/commonMain/kotlin/{pkg}/controller/{FlowGraph}ControllerInterface.kt` | `RuntimeControllerInterfaceGenerator` (085-owned) | REUSE AS-IS |
| 6 | `src/commonMain/kotlin/{pkg}/controller/{FlowGraph}Runtime.kt` | `ModuleRuntimeGenerator` (085-owned) | REUSE AS-IS |
| 7 | `src/jvmMain/kotlin/{pkg}/userInterface/{FlowGraph}PreviewProvider.kt` | `PreviewProviderGenerator` (085-owned) | EXTEND (add `composableName` parameter) |
| 8 (opt) | `src/commonMain/kotlin/{pkg}/flow/{FlowGraph}.flow.kt` | (bootstrap inside `UIFBPInterfaceGenerator.generateBootstrapFlowKt`) | UNCHANGED — emitted only when `includeFlowKt = true` AND target file does not exist; otherwise the merge case is handled by `UIFBPSaveService` (Decision 7) |

Index 8 is conditional. Indices 3 and 4 may be skipped if `spec.sourceOutputs` / `spec.sinkInputs` are empty (existing behavior preserved). The interface (index 5) degenerates to one with only the inherited `ModuleController` surface if `sinkInputs` is empty. Indices 1, 2, 5, 6, 7 are always emitted.

**`UIFBPSpec → FlowGraph` translation** (in `UIFBPInterfaceGenerator`): per research Decision 1, the orchestrator builds a `FlowGraph` model with two CodeNodes (Source + Sink) wired into an empty middle, and feeds that `FlowGraph` to `RuntimeControllerInterfaceGenerator` and `ModuleRuntimeGenerator`. The translation has no per-port special cases — Source's outputs come from `spec.sourceOutputs`, Sink's inputs from `spec.sinkInputs`. Both nodes carry `_codeNodeClass` configuration entries pointing at the per-flow-graph generated CodeNode FQCNs (so `ModuleRuntimeGenerator`'s NodeRegistry lookup resolves them).

---

## 3. UIFBPSaveService (NEW — orchestration entity)

Lives at `flowGraph-generate/src/jvmMain/kotlin/io/codenode/flowgraphgenerate/save/UIFBPSaveService.kt`. Composes the pure commonMain generators with filesystem I/O, `.flow.kt` parse-and-merge, and host-module validation.

```kotlin
class UIFBPSaveService(
    private val orchestrator: UIFBPInterfaceGenerator = UIFBPInterfaceGenerator(),
    private val flowKtParser: FlowKtParser = FlowKtParser(),
    private val flowGraphSerializer: FlowGraphSerializer = FlowGraphSerializer()
) {
    fun save(
        spec: UIFBPSpec,
        flowGraphFile: File,           // The user-selected .flow.kt file (the explicit-pair input from FR-015)
        moduleRoot: File,              // The host module directory containing build.gradle.kts
        options: UIFBPSaveOptions = UIFBPSaveOptions()
    ): UIFBPSaveResult
}

data class UIFBPSaveOptions(
    val deleteLegacyLocations: Boolean = false,        // If true, removes saved/ + base-package State/ViewModel files (Decision 6)
    val mergeExistingFlowKt: Boolean = true            // If false, leave existing .flow.kt untouched (no merge attempted)
)
```

### UIFBPSaveResult

```kotlin
data class UIFBPSaveResult(
    val success: Boolean,
    val files: List<FileChange>,                       // One entry per file the service decided about
    val flowKtMerge: FlowKtMergeReport?,               // null if no .flow.kt was processed
    val warnings: List<String>,
    val errorMessage: String? = null                   // Populated when success = false (e.g., unscaffolded host)
)

enum class FileChangeKind { CREATED, UPDATED, UNCHANGED, SKIPPED_CONFLICT, DELETED }

data class FileChange(
    val relativePath: String,
    val kind: FileChangeKind,
    val reason: String? = null                         // Populated for SKIPPED_CONFLICT and DELETED
)

data class FlowKtMergeReport(
    val mode: FlowKtMergeMode,                         // CREATED | UPDATED | UNCHANGED | PARSE_FAILED_SKIPPED
    val portsAdded: List<PortChange>,
    val portsRemoved: List<PortChange>,
    val connectionsDropped: List<DroppedConnection>,
    val userNodesPreserved: Int                        // Count of CodeNodes that are neither Source nor Sink
)

enum class FlowKtMergeMode { CREATED, UPDATED, UNCHANGED, PARSE_FAILED_SKIPPED }

data class PortChange(val nodeName: String, val portName: String, val typeName: String)
data class DroppedConnection(val from: String, val to: String, val reason: String)
```

Note: this feature deliberately does **not** model build.gradle.kts edits (Decision 5 retires the auto-edit responsibility). The service detects an unscaffolded host and refuses; no `BuildGradleEditReport` exists in the post-085 model.

### Behavioral contract

1. **Pre-flight host validation** (Decision 5): the service inspects `moduleRoot/build.gradle.kts` as text. If `jvm` followed by `{` is absent, OR `io.codenode:preview-api` is absent, the service returns `UIFBPSaveResult(success = false, errorMessage = "host module is unscaffolded; run quickstart.md VS-A1 migration first: missing {jvm()|preview-api dep}")` and emits zero file changes. **No silent mutation of the build script.**
2. **Idempotency** (FR-011): `save(...)` followed by `save(...)` against the same inputs MUST produce a result where every `FileChange.kind` in the second call is `UNCHANGED` (modulo file-system mtimes). The `FlowKtMergeReport.mode` MUST be `UNCHANGED` on the second call.
3. **Hand-edit safety** (FR-016): A target file existing on disk without the `Generated by CodeNodeIO {Generator}` marker comment is `SKIPPED_CONFLICT` with a reason naming the missing marker — never overwritten. Marker presence is detected with the same `carriesGeneratorMarker` heuristic as feature 085's `GenerationFileWriter` (first ~8 lines contain the marker substring).
4. **Conflict-vs-update**: a target file with the marker AND content matching what would be emitted is `UNCHANGED`; with the marker AND differing content is `UPDATED`; without the marker is `SKIPPED_CONFLICT`.
5. **`.flow.kt` merge** (FR-012, Decision 7): orchestrated through `flowKtParser.parseFlowKt(...)` + `flowGraphSerializer.serialize(...)`. The merge surface is the FlowGraph DSL, not text. A `PARSE_FAILED_SKIPPED` mode is emitted when the user has edited `.flow.kt` into a non-parseable shape; the service preserves the file untouched and surfaces a warning.
6. **Legacy cleanup** (Decision 6): when `deleteLegacyLocations = true`, the service deletes:
   - `src/commonMain/kotlin/{pkg}/saved/{FlowGraph}State.kt`
   - `src/commonMain/kotlin/{pkg}/saved/{FlowGraph}ViewModel.kt`
   - `src/commonMain/kotlin/{pkg}/{FlowGraph}State.kt` (base package legacy)
   - `src/commonMain/kotlin/{pkg}/{FlowGraph}ViewModel.kt` (base package legacy)
   - The empty `saved/` directory if applicable.

   Each deletion appears in `files` with `kind = DELETED` and a `reason`. Files lacking the generator marker remain SKIPPED_CONFLICT regardless of this flag.
7. **Structured summary** (FR-013): `UIFBPSaveResult` is the single summary surface. Both UI callers (the GraphEditor's status line) and CI/automation consumers read it.

### Validation

- `moduleRoot` MUST be an existing directory containing `build.gradle.kts`. Otherwise `success = false`.
- `flowGraphFile` MUST exist and end with `.flow.kt`. Otherwise `success = false`.
- `spec.composableName` MUST equal the `@Composable` function name in the source file (validated by `UIComposableParser` upstream).

---

## 4. Generated `{FlowGraph}ControllerInterface` shape (the contract)

Identical to the post-085 entity-module ControllerInterface shape (e.g., `StopWatchControllerInterface`, `AddressesControllerInterface`):

```kotlin
package io.codenode.{module}.controller

import io.codenode.fbpdsl.runtime.ModuleController
import kotlinx.coroutines.flow.StateFlow
// + IP type imports per spec.ipTypeImports

interface {FlowGraph}ControllerInterface : ModuleController {
    // For each y in spec.sinkInputs:
    val y: StateFlow<{T}>
    // (start/stop/pause/resume/reset/getStatus/setAttenuationDelay/setEmissionObserver/setValueObserver
    //  inherit from ModuleController — generators MUST NOT redeclare them)
}
```

**Two implementations** (post-085, see `contracts/controller-interface.md`):

1. **Runtime Preview path** — `java.lang.reflect.Proxy` constructed by `flowGraph-execute/ModuleSessionFactory.createControllerProxy`. Method calls on the inherited `ModuleController` members reflect to `DynamicPipelineController`; state-flow getters reflect to `{FlowGraph}State.{port}Flow` fields.
2. **Production-app path** — the anonymous `object : {FlowGraph}ControllerInterface, ModuleController by controller { override val y = {FlowGraph}State.yFlow ... }` returned by `create{FlowGraph}Runtime(flowGraph)` (emitted by `ModuleRuntimeGenerator`). The Kotlin interface delegation `ModuleController by controller` resolves all inherited members to the underlying `DynamicPipelineController`.

---

## 5. Generated `{FlowGraph}ViewModel` shape (the dependency surface)

Mirrors the entity-module ViewModel shape (e.g., `WeatherForecastViewModel`, `StopWatchViewModel`):

```kotlin
package io.codenode.{module}.viewmodel

import androidx.lifecycle.ViewModel
import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.{module}.controller.{FlowGraph}ControllerInterface
import io.codenode.{module}.viewmodel.{FlowGraph}State
import kotlinx.coroutines.flow.StateFlow
// + IP type imports

class {FlowGraph}ViewModel(
    private val controller: {FlowGraph}ControllerInterface
) : ViewModel() {
    // Observable state (one per spec.sinkInput) — read directly from State
    val y: StateFlow<{T}> = {FlowGraph}State.yFlow

    // Execution state from controller (inherited via ModuleController)
    val executionState: StateFlow<ExecutionState> = controller.executionState

    // Source emit method — writes to State mutable fields
    fun emit({sourceOutputs as parameters}) {
        {FlowGraph}State._{a}.value = {a}
    }

    // Forwarding control surface — the UI invokes these directly (US1.AS3, US2.AS3).
    // Each delegates one-to-one to the underlying ControllerInterface (which inherits
    // from ModuleController, so the methods exist on `controller`).
    fun start(): FlowGraph = controller.start()
    fun stop(): FlowGraph = controller.stop()
    fun pause(): FlowGraph = controller.pause()
    fun resume(): FlowGraph = controller.resume()
    fun reset(): FlowGraph = controller.reset()
}
```

**Constraints**:

- The constructor MUST be `public` and parameter type MUST be exactly `({FlowGraph}ControllerInterface)` so `ModuleSessionFactory.tryCreateViewModel` matches it via `parameterTypes[0].isAssignableFrom(interfaceClass)`.
- The class MUST extend `androidx.lifecycle.ViewModel`.
- `emit(...)` continues to write to `{FlowGraph}State._x` mutable fields — preserves today's UI → State → SourceCodeNode flow.
- Sink-input flows are read from `{FlowGraph}State.yFlow` directly (matches entity-module precedent; same code works under both runtime paths).
- The forwarding control methods (`start/stop/pause/resume/reset`) are required because the UI invokes them as `viewModel.start()` etc. (US1.AS3, US2.AS3). Each is a one-line delegation to `controller.{same}()`. Generators MUST emit them; they are NOT inherited from `androidx.lifecycle.ViewModel` and the UI cannot reach the controller directly.

---

## 6. State transitions (re-generation lifecycle)

The only "state machine" introduced by this feature is the per-file decision tree inside `UIFBPSaveService`:

```
For each generator-target file:

  Pre-flight: host module has jvm() + preview-api?
    NO  → return success=false, errorMessage describing missing pieces. END.
    YES → continue.

  EXISTS at target path?
    NO  → write new file → kind = CREATED
    YES → CONTENT MATCHES would-be-emitted?
            YES → kind = UNCHANGED
            NO  → CARRIES "Generated by CodeNodeIO" MARKER COMMENT?
                    YES → write new file → kind = UPDATED
                    NO  → leave file → kind = SKIPPED_CONFLICT, warning emitted

For .flow.kt:
  EXISTS at the user-selected path?
    NO  → emit bootstrap → mode = CREATED
    YES → PARSEABLE via FlowKtParser?
            NO  → leave file → mode = PARSE_FAILED_SKIPPED, warning emitted
            YES → DIFF Source/Sink ports against spec
                  IF no diff → mode = UNCHANGED
                  ELSE       → apply port adds/removes, drop invalid connections,
                               re-serialize → mode = UPDATED

If options.deleteLegacyLocations = true:
  For each legacy path (saved/{X}.kt, base-package/{X}.kt where X ∈ {State, ViewModel}):
    EXISTS?
      NO  → no entry
      YES → CARRIES MARKER?
              YES → delete → kind = DELETED, reason = "Legacy {kind} cleanup"
              NO  → kind = SKIPPED_CONFLICT, reason = "Legacy file lacks marker"
```

This is the entirety of the dynamic behavior introduced by the feature; no domain entities have lifecycle states.

---

## 7. What this feature does NOT model

- No new persistence. No database schemas. No migration scripts.
- No changes to `FlowGraph`, `CodeNode`, `Node`, or any `fbpDsl` model classes.
- No changes to `DynamicPipelineController`, `DynamicPipelineBuilder`, `PreviewRegistry`, `DynamicPreviewDiscovery`, or `ModuleSessionFactory` — the post-085 contracts are sufficient (see research Decisions 1–3).
- No changes to `RuntimeControllerInterfaceGenerator`, `ModuleRuntimeGenerator`, `GenerationFileWriter`, or `CodeGenerationRunner` — feature 085's surface is reused as-is.
- No new IP types; the generator continues to use whatever IP types are already discovered by `IPTypeRegistry`.
- No `build.gradle.kts` mutation — Decision 5 retires that responsibility (delegated to feature 085's `ModuleGenerator` scaffolding).
- No implicit module scanning for flow graphs or qualifying UI files — Decision 9 retires that ambiguity surface in favor of an explicit-pair input.
- No multi-UI-per-flow-graph emission — one UI-FBP run = one `{flow graph, UI file}` pair = one consistent generated artifact set.

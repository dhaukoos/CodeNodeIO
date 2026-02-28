# Research: Refine the ViewModel Binding

**Feature**: 034-refine-viewmodel-binding
**Date**: 2026-02-28

## Research Questions

### R1: Observable State Consolidation Strategy

**Question**: How should observable state properties (currently in per-node StateProperties objects) be consolidated into the ViewModel file without creating circular dependencies?

**Decision**: Use a **Module State Object** — a Kotlin `object` declaration named `{ModuleName}State` in the ViewModel file. This object holds all MutableStateFlow/StateFlow pairs derived from sink node input ports.

**Rationale**:
- Preserves the existing reference pattern (`ObjectName.propertyName`) that Flow/Controller generators already use
- No naming clashes with Flow/Controller class properties (the object acts as a namespace)
- Eliminates per-node StateProperties objects in favor of one consolidated state object
- Co-located in the ViewModel file for single-file visibility of all module-level state
- No circular dependency: Flow imports from ViewModel file's top-level object, ViewModel class depends on Controller via ControllerInterface — these are separate concerns in the same file

**Alternatives Considered**:
1. **Top-level properties** (no object): Naming clashes with Flow's own properties require import aliases. More fragile for generated code.
2. **ViewModel instance properties**: Creates construction-order dependency (Flow is created before ViewModel). Would require dependency inversion or injection.
3. **Flow-owned MutableStateFlow (legacy mode)**: Works but keeps state hidden inside Flow. Doesn't satisfy spec requirement for state to be "in the ViewModel file."

### R2: ViewModel Stub Preservation Strategy

**Question**: How should the ViewModel stub be preserved on re-generation, given that it contains both auto-derived properties (Module State object) and user-editable code?

**Decision**: Use **selective regeneration** with marker comments. The Module Properties section (the `{ModuleName}State` object) is delineated by markers and regenerated on each save to reflect current port definitions. Everything outside the markers (user-written ViewModel class code, custom methods, additional imports) is preserved.

**Rationale**:
- Keeps Module State in sync with FlowGraph port definitions automatically — no manual updates needed when ports change
- User-written code (ViewModel class customizations, additional methods, custom reactive bindings) is preserved across regenerations
- Follows the same principle as ProcessingLogicStubGenerator's `extractLambdaBody()` / `generateStubWithPreservedBody()` pattern, but at a section level
- Marker comments provide clear boundaries: `// ===== MODULE PROPERTIES START =====` and `// ===== MODULE PROPERTIES END =====`

**Regeneration algorithm**:
1. If ViewModel file does not exist → generate fresh (full file with markers)
2. If ViewModel file exists → read it, extract everything outside the markers, regenerate the Module Properties section with current port definitions, reassemble the file preserving user code

**Alternatives Considered**:
1. **Simple stub** (generate once, never overwrite): Simpler but Module State becomes stale when ports change. User must manually update property names, types, and defaults — error-prone.
2. **Always regenerate**: Would destroy user modifications, defeating the purpose of a stub.

### R3: Impact on Processing Logic Stubs

**Question**: How does removing StateProperties affect existing processing logic tick functions that reference StateProperties?

**Decision**: Remove `statePropertiesPackage` parameter from ProcessingLogicStubGenerator. New stubs are generated without StateProperties imports. Processing logic manages its own computation state locally.

**Rationale**:
- Generator tick functions should manage their own state (e.g., local `var currentSeconds`) and return computed values via ProcessResult
- Sink tick functions receive values as parameters — they don't need to update state themselves (Flow's consume block handles that)
- Cleaner separation of concerns: tick functions compute, Flow manages state propagation

**Impact on existing StopWatch**:
- Old StopWatch (preserved as StopWatchV2) keeps its StateProperties references — unchanged
- New StopWatch gets fresh stubs without StateProperties imports — user fills in logic
- TimerEmitter tick stub: user adds local state vars and increment logic
- DisplayReceiver tick stub: minimal (receives values, no state update needed)

### R4: Flow Generator Refactoring

**Question**: How should the RuntimeFlowGenerator change to reference the Module State object instead of per-node StateProperties?

**Decision**: Replace `statePropertiesPackage: String?` parameter with `viewModelPackage: String`. Flow imports `{ModuleName}State` from the viewModelPackage and references it like it currently references StateProperties objects.

**Rationale**:
- Minimal change to generator logic — just swap the object name and package
- Same delegation pattern: `{ModuleName}State.secondsFlow`, `{ModuleName}State._seconds.value`, `{ModuleName}State.reset()`
- Consolidates per-node references into a single object reference

**Key changes**:
- Import: `import {viewModelPackage}.{ModuleName}State`
- StateFlow delegation: `val secondsFlow = {ModuleName}State.secondsFlow`
- Consume block updates: `{ModuleName}State._seconds.value = seconds`
- Reset: `{ModuleName}State.reset()`

### R5: Controller/Interface/Adapter Changes

**Question**: Do Controller, ControllerInterface, and ControllerAdapter need changes?

**Decision**: **No changes needed** to the observable state delegation chain. Controller still delegates from Flow, Interface still declares StateFlow properties, Adapter still wraps Controller.

**Rationale**: The delegation chain (Flow → Controller → ControllerInterface → Adapter → ViewModel) remains structurally identical. Only the backing store changes (Module State object instead of StateProperties). Controller sees no difference because Flow still exposes `secondsFlow` as before.

### R6: FR-008 Port Type from IP Types

**Question**: Does the code generator need changes to support IP type-derived port types?

**Decision**: **No code generation changes needed.** Port types in the FlowGraph model are already Kotlin KClass types (Int, String, etc.) set when the user selects an IP type in the graphEditor. The code generator uses `port.dataType.simpleName` which already reflects the IP type selection.

**Rationale**: FR-008 is satisfied by the existing editor-side IP type selector (feature 023-port-type-selector). When a user selects an IP type for a port, the port's KClass type is set accordingly, and code generation picks it up automatically.

### R7: Scope of StateProperties Removal

**Question**: What files reference StatePropertiesGenerator or stateProperties and need cleanup?

**Decision**: Complete removal across 6 files:

| File | Change |
|------|--------|
| `StatePropertiesGenerator.kt` | DELETE entire file |
| `StatePropertiesGeneratorTest.kt` | DELETE entire file |
| `ModuleSaveService.kt` | Remove StateProperties generation, directory creation, orphan cleanup |
| `ModuleSaveServiceTest.kt` | Remove StateProperties-related test assertions |
| `RuntimeFlowGenerator.kt` | Replace `statePropertiesPackage` with `viewModelPackage` |
| `RuntimeFlowGeneratorTest.kt` | Update tests for new delegation pattern |
| `ProcessingLogicStubGenerator.kt` | Remove `statePropertiesPackage` parameter and imports |
| `ProcessingLogicStubGeneratorTest.kt` | Remove StateProperties import test cases |

### R8: Module State Object Property Derivation

**Question**: Which node ports should contribute to the Module State object's properties?

**Decision**: Only **sink node input ports** contribute observable properties. This matches the existing ObservableStateResolver behavior.

**Rationale**:
- Sink input ports represent data arriving at "display" nodes — the data that the UI needs to observe
- Generator output ports represent emitted data — flows through channels, doesn't need separate observation
- Transformer/Processor ports are internal — not user-facing

**Property naming**: Uses ObservableStateResolver's existing disambiguation logic:
- Unique port names → use as-is (e.g., `seconds`)
- Colliding port names across sinks → prefix with node name (e.g., `displaySeconds`)

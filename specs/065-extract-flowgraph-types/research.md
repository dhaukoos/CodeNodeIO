# Research: Extract flowGraph-types Module

**Feature**: 065-extract-flowgraph-types
**Date**: 2026-04-05

## R1: PlacementLevel Enum Dependency

**Decision**: Move `PlacementLevel.kt` to fbpDsl (shared vocabulary module) before extracting flowGraph-types.

**Problem**: Six of the 9 IP type files import `io.codenode.grapheditor.model.PlacementLevel`. If PlacementLevel stays in graphEditor, flowGraph-types would need to depend on graphEditor — creating the exact cycle the extraction is meant to eliminate.

**Rationale**: PlacementLevel is a simple enum (MODULE, PROJECT, UNIVERSAL) with no dependencies beyond standard library. It represents a domain concept (filesystem tier placement) that is shared vocabulary — used by both IP type lifecycle (types) and node discovery (inspect). Moving it to fbpDsl makes it available to all modules without creating cross-module dependencies.

**Alternatives considered**:
- **Move to flowGraph-types**: Would work for types, but then inspect's Node Generator would depend on types for a simple enum — an unnecessary coupling.
- **Duplicate in both modules**: Violates DRY, creates drift risk, makes cross-module APIs fragile.
- **Keep in graphEditor**: Creates cycle (flowGraph-types → graphEditor → flowGraph-types). Rejected.
- **Move to fbpDsl (chosen)**: Clean, cycle-free, shared vocabulary. PlacementLevel is conceptually the same kind of domain term as `IPColor` or `InformationPacketType` which already live in fbpDsl.

**Impact**: PlacementLevel.kt becomes a prerequisite file move. All existing imports of `io.codenode.grapheditor.model.PlacementLevel` change to `io.codenode.fbpdsl.model.PlacementLevel`. This affects files in graphEditor, and eventually in flowGraph-types and flowGraph-inspect.

---

## R2: Module Build Configuration Pattern

**Decision**: Follow the existing KMP module pattern established by fbpDsl for the new flowGraph-types module.

**Module structure**:
- KMP multi-target: JVM (required for graphEditor desktop), iOS targets (for future mobile parity)
- Dependencies: `fbpDsl` only (no compose, no coroutines beyond what fbpDsl transitively provides)
- Package: `io.codenode.flowgraphtypes` (following KMP package naming conventions)
- Source sets: `commonMain` for all 9 files (they use no JVM-specific APIs except `java.io.File` in IPTypeDiscovery, FileIPTypeRepository, and IPTypeFileGenerator)

**JVM-only concern**: Three of the 9 files use `java.io.File` for filesystem operations (IPTypeDiscovery, FileIPTypeRepository, IPTypeFileGenerator). These must go in `jvmMain` source set, with the remaining 6 files (models, registry) in `commonMain`. Alternatively, the filesystem access could be abstracted behind `expect`/`actual` declarations, but that is Phase C refinement — for now, a `jvmMain` source set is sufficient.

**Rationale**: Matching the existing fbpDsl module pattern ensures consistency in the build system. The KMP-first constraint (from project memory) requires all modules to be multiplatform-capable, though the initial extraction targets JVM Desktop.

**Alternatives considered**:
- **JVM-only module**: Simpler but violates the KMP-first mandate. Rejected.
- **Full KMP with expect/actual for File IO**: Correct long-term but premature for Phase B. Out of scope.
- **KMP with jvmMain for file-dependent classes (chosen)**: Pragmatic — models are shared, filesystem logic is JVM-only.

---

## R3: Data Flow Contract Design

**Decision**: The module boundary is the CodeNode's ports — data flows in and out. No service interfaces. Consumers receive `ipTypeMetadata` as data and work with it locally. Mutations flow in as commands through an input port.

**Data flow contracts** (following the data-oriented naming convention from 064 R6):

1. **ipTypeMetadata** (output) — the primary data product. A reactive stream of the complete IP type registry state: all registered types, custom type definitions, properties, file paths, entity module associations. When the registry state changes (types discovered, registered, removed), a new `ipTypeMetadata` packet flows to all 4 downstream consumers (compose, persist, generate, rootSink). Consumers query this data locally — no callback to the types module.

2. **filesystemPaths** (input) — filesystem locations to scan for IP type definition files. Received from the composition root when a project is loaded or the active module changes.

3. **classpathEntries** (input) — classpath locations for resolving KClass references from discovered IP type files.

4. **ipTypeCommands** (input, NEW) — commands for mutating the registry state (register new type, unregister type, update color, generate IP type file). This port does not currently exist in architecture.flow.kt — it needs to be added, with a connection from graphEditor-source. Without it, the types module can only passively discover types from the filesystem and has no way to receive user-initiated mutations.

**Model types** (IPProperty, IPPropertyMeta, IPTypeFileMeta, SerializableIPType, CustomIPTypeDefinition) are exposed directly as data classes in `commonMain` — they are pure data that flows through ports, not service abstractions.

**Rationale**: The 064 planning established that module boundaries are data flows, not service contracts. The port naming convention explicitly chose `ipTypeMetadata` over `ipTypeRegistry` (064 R6). Service interfaces would create a parallel traditional DI layer that duplicates and contradicts the FBP port-based boundary. In the FBP model, the CodeNode's ports ARE the interface — the data shape of what flows through them IS the contract.

**Implications for architecture.flow.kt**: The current flowGraph-types GraphNode has 2 inputs and 1 output. Adding `ipTypeCommands` makes it 3 inputs and 1 output, which requires updating the GraphNode container definition and adding a connection from graphEditor-source. This also updates the ArchitectureFlowKtsTest invariants (connection count increases from 19 to 20).

**Alternatives considered**:
- **Service interfaces (IPTypeRegistryService, etc.)**: Traditional DI pattern. Would work but contradicts the FBP-native architecture established in 064. Creates a parallel boundary mechanism that competes with the CodeNode ports. Rejected.
- **Data flow through ports only (chosen)**: FBP-native. The CodeNode's ports are the boundary. Consumers receive data, not capabilities. Mutations flow as commands, not method calls.
- **Hybrid (service interfaces internally, data flow externally)**: Would allow internal use of DI while presenting an FBP boundary externally. Unnecessarily complex — the internal implementation can use direct class references since it's all within one module.

---

## R4: CodeNode Design for Module-Level Wrapping

**Decision**: Implement the flowGraph-types CodeNode as a `CodeNodeDefinition` that creates an `In3AnyOut1Runtime` — receiving filesystem paths, classpath entries, and mutation commands, producing IP type metadata. The CodeNode uses `anyInput` mode so it re-emits updated metadata whenever any input changes.

**Port mapping** (updated from 2-input/1-output to 3-input/1-output per R3):
- Input 1: `filesystemPaths` (String) — filesystem locations to scan for IP type definition files
- Input 2: `classpathEntries` (String) — classpath entries for KClass resolution
- Input 3: `ipTypeCommands` (String) — serialized commands for registry mutations (register, unregister, generate, update color)
- Output 1: `ipTypeMetadata` (String) — the complete IP type registry state, re-emitted whenever any input triggers a state change

**Processing logic**: On any input change, the CodeNode:
1. If `filesystemPaths` or `classpathEntries` changed: IPTypeDiscovery re-scans and IPTypeRegistry re-registers discovered types
2. If `ipTypeCommands` changed: Deserialize the command and apply the mutation (register, unregister, generate file, update color)
3. In all cases: Serialize the current registry state and emit as `ipTypeMetadata`

**Runtime type**: `In3AnyOut1Runtime<String, String, String, String>` with `anyInput = true`. The `anyInput` mode means the node fires whenever ANY input receives data (not waiting for ALL inputs), which is essential — filesystem paths arrive once at project load, but commands arrive on demand from user actions.

**Rationale**: The `anyInput` pattern is proven in the codebase (UserProfileRepositoryCodeNode uses `In3AnyOut2` with `anyInput = true` for CRUD operations). The 3-input topology accommodates both passive discovery (filesystem/classpath inputs from composition root) and active mutation (command input from user actions).

**Alternatives considered**:
- **In2Out1Runtime (original plan)**: Only 2 inputs — no way to receive mutation commands. Would require a parallel service interface for mutations, contradicting the FBP-native boundary. Rejected.
- **Separate CodeNodes for discovery vs mutation**: Over-decomposition — discovery and mutation both operate on the same registry state. A single node with multiple input ports is the FBP-native approach.
- **In3AnyOut1Runtime (chosen)**: Matches the 3-input, 1-output topology. `anyInput` mode ensures responsiveness to both discovery triggers and user commands.

---

## R5: Composition Root Orchestration

**Decision**: The graphEditor composition root (ViewModels) orchestrates the flowGraph-types CodeNode by wiring its input/output channels. No Koin service interfaces. The composition root sends data to the CodeNode's input ports and subscribes to its output port.

**Wiring pattern**: The composition root constructs the FlowGraphTypesCodeNode, starts it, and wires its channels:
- Sends `filesystemPaths` when a project is loaded (from project directory resolution)
- Sends `classpathEntries` when the runtime classpath is resolved
- Sends `ipTypeCommands` when the user performs type mutations (register, unregister, generate)
- Receives `ipTypeMetadata` and distributes it to ViewModels/UI state that need IP type information

**Data distribution**: The `ipTypeMetadata` output flows to all consumers that currently reference IPTypeRegistry. Instead of calling `registry.getById(id)`, a ViewModel holds the current `ipTypeMetadata` (received from the CodeNode's output channel) and queries it locally. This is equivalent to how the composition root already manages reactive state via `StateFlow` — the CodeNode's output channel is another reactive data source.

**Rationale**: The FBP-native boundary means the CodeNode's channels are the wiring mechanism — not Koin DI. The composition root already owns the lifecycle of ViewModels and state providers; it naturally owns the CodeNode lifecycle too. Koin may still be used for internal construction within the types module, but the module's external boundary is data flow through ports.

**Alternatives considered**:
- **Koin-wired service interfaces**: Would create a parallel non-FBP boundary. Contradicts 064's data-flow-first architecture. Rejected.
- **CodeNode channel wiring in composition root (chosen)**: FBP-native. The composition root wires channels the same way architecture.flow.kt describes connections. Matches the vision that architecture.flow.kt becomes the actual application wiring.

---

## R6: Call Site Migration — Data Flow Consumption

**Decision**: Call sites that currently access IPTypeRegistry directly will instead consume `ipTypeMetadata` as data received from the CodeNode's output channel. Mutations will be expressed as commands sent to the CodeNode's `ipTypeCommands` input port.

**Corrected scope**: 6 call sites (not 8). ConnectionContextMenu.kt and GraphNodePaletteSection.kt don't access IP type internals directly — they already consume downstream data.

**Migration pattern by call site**:

| File | Current Pattern | FBP-Native Pattern |
|------|----------------|-------------------|
| GraphState.kt | `ipTypeRegistry.getByTypeName(name)` | Query locally-held `ipTypeMetadata` |
| GraphNodeTemplateSerializer.kt | `ipTypeRegistry.getByTypeName(name)` | Receive `ipTypeMetadata` as parameter, query locally |
| IPGeneratorViewModel.kt | `ipTypeRegistry.register(...)`, `discovery.parseIPTypeFile(...)`, `repository.add(...)` | Send `ipTypeCommands` (register/generate) to CodeNode input port; receive updated `ipTypeMetadata` from output |
| SharedStateProvider.kt | Holds `IPTypeRegistry` instance | Holds current `ipTypeMetadata` data (received from CodeNode output channel) |
| PropertiesPanel.kt | `ipTypeRegistry.getAllTypes()`, `.getById(id)` | Query locally-held `ipTypeMetadata` |
| IPPaletteViewModel.kt | `ipTypeRegistry.unregister(id)`, `repository.remove(id)` | Send `ipTypeCommands` (unregister/remove) to CodeNode input port; receive updated `ipTypeMetadata` from output |

**Key pattern shift**: Read-only consumers (GraphState, GraphNodeTemplateSerializer, PropertiesPanel) simply hold the current `ipTypeMetadata` and query it locally — they no longer call back to a registry. Mutating consumers (IPGeneratorViewModel, IPPaletteViewModel) send commands to the CodeNode's input port and receive the updated state from its output port.

**Rationale**: This is the FBP-native approach. Data flows through ports; consumers hold local copies of the data they need. Mutations are commands (data flowing in), not method calls. This matches the 064 architecture where `ipTypeMetadata` flows from types to compose, persist, generate, and rootSink as a data connection.

**Alternatives considered**:
- **Service interface delegation**: Traditional DI approach. Would work but creates a non-FBP boundary that contradicts the 064 architecture. Rejected.
- **Data flow consumption (chosen)**: FBP-native. Consumers receive data, send commands. No service abstraction layer.

---

## R7: File Placement — jvmMain vs commonMain Split

**Decision**: Split the 9 files between `commonMain` (6 files) and `jvmMain` (3 files) based on JVM API usage.

**commonMain (6 files)** — pure Kotlin, no platform APIs:
- IPProperty.kt
- IPPropertyMeta.kt
- IPTypeFileMeta.kt
- SerializableIPType.kt
- IPTypeRegistry.kt
- PlacementLevel.kt (moves to fbpDsl, not flowGraph-types — see R1)

**jvmMain (3 files)** — use `java.io.File`:
- IPTypeDiscovery.kt
- FileIPTypeRepository.kt
- IPTypeFileGenerator.kt

**Corrected file count**: 9 files move to flowGraph-types (PlacementLevel moves to fbpDsl separately, making it 9 + 1 prerequisite move = 10 total file moves).

**Rationale**: The KMP-first project mandate requires multiplatform compatibility. Model types and the registry have no platform dependencies — they belong in `commonMain`. The three filesystem-dependent files must stay in `jvmMain` until `expect`/`actual` abstractions are introduced (Phase C).

**Alternatives considered**:
- **All files in jvmMain**: Simpler but prevents iOS/WASM targets from accessing the registry or model types. Violates KMP-first mandate.
- **All files in commonMain with expect/actual for File**: Correct long-term but out of scope for Phase B.
- **Split by platform dependency (chosen)**: Pragmatic, KMP-compliant, no premature abstraction.

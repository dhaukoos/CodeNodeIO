# Feature Specification: Single-Session Generate → Execute (Hot-Compile Nodes)

**Feature Branch**: `086-hot-compile-nodes`
**Created**: 2026-04-29
**Status**: Draft
**Input**: User description: Make "generate → execute" a single-session reality. There is no true generate-and-execute for nodes within a single session today. The constraint is fundamental: a freshly-generated `.kt` file isn't on the JVM's classpath until the host module's jar is rebuilt and (because the GraphEditor's classpath is fixed at process start) the GraphEditor is relaunched. Without a class on the classpath, `createRuntime()` can't be invoked, so Runtime Preview can't execute it. The real fix: hot-compile the generated `.kt` file in-process via embedded kotlinc and load it through a custom classloader. This would make "generate → execute" a single-session reality. Significantly more complex, but it eliminates the rebuild + relaunch friction permanently. Worth considering since this workflow becomes central to the product. Add an intuitive way to re-compile a given node after making textual edits to it. Perhaps a more practical way is to compile the currently selected module — but that raises the question of what happens to Project-level nodes in that scenario.

## User Scenarios & Testing *(mandatory)*

### Canonical workflow this feature is designed to support

A user opens the GraphEditor and follows this loop entirely within one session, with no `./gradlew` invocations and no GraphEditor relaunch:

1. **Generate** a new (Module-tier) CodeNode via the Node Generator.
2. **Immediately** see the new node in the Node Palette with a usable placeholder definition (drag-droppable, connectable, identifiable on the canvas).
3. **Wire** the new node into a flow graph; pick port data types and connection IP types via the GraphEditor's graphical UI.
4. **Save** the flow graph; the `.flow.kt` file on disk reflects those data-type choices.
5. **Edit** the individual node source files in the in-editor code editor to replace the placeholder logic with the desired functionality.
6. **Compile the module** on demand via a UI control (one click, not a Gradle invocation).
7. **Run Runtime Preview** against the saved flow graph; the pipeline executes the user's edited business logic.

The recompile granularity is split deliberately along this workflow:

- **Per-file, automatic, at Node Generation time** (US1) — so step 2 happens immediately. Cost is one file; outcome is "node appears on the palette and is wireable in the GraphEditor".
- **Per-module, manual, on user demand prior to Runtime Preview** (US3) — so step 6 happens when the user is ready to run, not on every keystroke. Cost scales with module size; outcome is "every source in the module reflects the user's latest edits, including intra-module cross-references."

This split is the answer to the open question raised by the user description ("compile the currently selected module — but what happens to Project-level nodes?"). Per-module is invoked against an explicit module selection; Project-tier and Universal-tier nodes use the same per-module mechanism against their respective host modules (the shared `:nodes` module for Project-tier; the user's `~/.codenode/nodes/` directory treated as a synthetic compilation unit for Universal-tier). No tier ever recompiles silently as a side effect of another tier's recompile (FR-010).

---

### User Story 1 - Generate a Node and See It on the Palette Immediately (Priority: P1)

A user who has just generated a new CodeNode (via the Node Generator) wants the new node to appear on the Node Palette immediately, with a usable placeholder definition — so they can drag it onto the canvas, choose its port types, and wire it into a flow graph in the same session, before they start writing real business logic.

**Why this priority**: This is the entry point of the canonical workflow above. Without immediate palette availability after generation, every step downstream is gated on a rebuild + relaunch loop. Closing this gap transforms generation from a "produce a file" operation into a "produce a usable node" operation. The placeholder logic is enough — real logic can be filled in later (US2/US3).

**Independent Test**: Open the GraphEditor against a project. Use the Node Generator to create a new Module-tier CodeNode. Without quitting or running any external Gradle task, the new node appears on the Node Palette under its declared name, can be dragged onto the canvas, and exposes its declared input/output ports — driven by its placeholder `CodeNodeDefinition`.

**Acceptance Scenarios**:

1. **Given** a running GraphEditor session, **When** the user finishes the Node Generator dialog and the source file lands on disk, **Then** the new node appears on the Node Palette without any further user action.
2. **Given** the new node is on the palette, **When** the user drags it onto the canvas, **Then** the GraphEditor renders the node with the input/output ports declared in its placeholder source.
3. **Given** the new node is on the canvas, **When** the user wires its ports to other nodes and chooses connection IP types via the graphical UI, **Then** the choices persist into the saved `.flow.kt`.

---

### User Story 2 - Run Runtime Preview Against a Saved Flow Graph After a Module Recompile (Priority: P2)

A user who has saved a complete flow graph and has edited the individual node sources to provide the real desired functionality wants to compile the host module on demand and then run Runtime Preview, all within the same GraphEditor session.

**Why this priority**: This is the payoff step of the canonical workflow — the moment Runtime Preview actually executes the user's business logic. It depends on US1 (the node had to reach the palette in the first place) and on the user's edits to the placeholder source (covered by US3's recompile control), but it's ranked P2 because once the module compiles successfully, Runtime Preview integration is a one-shot hookup.

**Independent Test**: Open a flow graph whose CodeNodes have been generated and edited in this session. Click the "Recompile module" UI control. Wait for the recompile-success indicator. Click Start in Runtime Preview. Emit a value. The pipeline executes the user's edited business logic — not the placeholder, not a stale launch-time JAR.

**Acceptance Scenarios**:

1. **Given** a saved flow graph and edited node sources in the host module, **When** the user invokes the per-module recompile control and the recompile succeeds, **Then** Runtime Preview's next Start uses the freshly-compiled module — no GraphEditor relaunch and no Gradle invocation.
2. **Given** a successful module recompile, **When** values flow through the pipeline, **Then** their class identity is consistent with the user's edited source (no `ClassCastException` from a stale class definition).
3. **Given** edits made to multiple sources in the same module, **When** the user invokes one per-module recompile, **Then** all of those edits are picked up atomically (intra-module cross-references resolve correctly).

---

### User Story 3 - Recompile Control with Clear Scope and Diagnostics (Priority: P3)

A user who has edited one or more node sources wants an obvious, low-friction control in the GraphEditor UI for triggering per-module recompilation, with clear feedback about what was recompiled and what (if anything) failed.

**Why this priority**: US1 and US2 fix the underlying mechanics; US3 specifies the UI surface and feedback shape so the workflow is discoverable and the failure modes are diagnosable. The per-module trigger MUST be user-invoked (not automatic) so it doesn't fire on every keystroke or save.

**Independent Test**: After editing one or more node sources, the user locates a clearly-labeled "Recompile module" action in the GraphEditor UI. The action's outcome — success or specific compilation errors — is visibly surfaced. The recompiled module is named in the feedback so the user knows what changed.

**Acceptance Scenarios**:

1. **Given** the user has edited a node's source, **When** they invoke the recompile control, **Then** the UI surfaces (a) which module was recompiled, (b) which sources within it succeeded, and (c) which failed with a copyable error message identifying the source file and line.
2. **Given** a recompile fails, **When** the user inspects the error console, **Then** the failure message is sufficient to locate the source file and the line of the offending error without consulting external tools.
3. **Given** the user wants to recompile a Project-tier or Universal-tier node, **When** they invoke the recompile control with that node's host module selected, **Then** the recompile uses the same UI affordance and feedback shape as the Module-tier case.

---

### Edge Cases

- **Recompile while a pipeline is running**: A user invokes recompile against a node that is currently being executed by a Runtime Preview pipeline. The system must not deadlock, leak coroutines, or produce undefined runtime behavior. (Reasonable default: stop the running pipeline first; document this in the recompile feedback.)
- **Compile error in a node that's already loaded**: A previous compile of the node succeeded and its class is in use; a new compile fails. The previously-loaded class must remain usable until the user fixes the error and recompiles successfully. The error must not silently revert the runtime to a broken state.
- **Cross-module references**: A Module-tier node references a class defined in another module that the GraphEditor's classpath knows about. Recompile must resolve those external references the same way the Gradle build would, without manual classpath manipulation by the user.
- **Memory accumulation across many recompiles**: A user recompiles the same node many times in a session. The session's memory footprint must not grow unboundedly with each recompile; old class definitions must be eligible for collection once nothing references them.
- **Tier-aware recompilation**: A user recompiles a Module-tier node. Project-level nodes (which live in a different placement tier and a different host module) are NOT silently recompiled or reloaded by that action. The recompile control's scope must make this distinction visible.
- **Recompiling a node used by an entity-module-generated runtime (GraphEditor scope only)**: A node referenced via the GraphEditor's Runtime Preview path is recompiled. The Runtime Preview's pipeline-build lookup must pick up the new compiled definition, not the launch-time JAR class. (The GraphEditor routes through `NodeDefinitionRegistry`, so FR-017 covers this case.) Production-app behavior — where consumers call `create{Module}Runtime(flowGraph)` directly with `{Module}NodeRegistry::lookup` — is explicitly out of scope per FR-018; production builds rebuild via Gradle.

## Requirements *(mandatory)*

### Functional Requirements

#### Per-file automatic compilation on Node Generation (US1)

- **FR-001**: When a CodeNode source file is produced by the Node Generator, the GraphEditor MUST automatically compile that single file in-process, with no user action required, so the new node appears on the Node Palette and is wireable on the canvas before the user does anything else. UI-FBP code generation and Entity Module generation are explicitly OUT OF SCOPE for the per-file auto-compile path; they emit multi-file artifact sets and rely on the per-module manual recompile (US2) instead.
- **FR-002**: The per-file compile on Node Generation MUST produce a usable placeholder `CodeNodeDefinition` — that is, the generator's emitted skeleton (with stub processing logic) compiles successfully and yields a `CodeNodeDefinition` instance the GraphEditor can register on the palette.
- **FR-003**: If the per-file compile on Node Generation fails (e.g., due to a generator bug or a malformed template), the failure MUST surface in the existing error console (FR-009) and the new node MUST NOT appear on the palette in a broken state. The generator's source file remains on disk so the user can inspect or repair it.

#### Per-module manual compilation prior to Runtime Preview (US2)

- **FR-004**: The GraphEditor MUST expose a user-invoked "Recompile module" control that, when triggered, compiles every CodeNode source in a designated host module in one atomic compile unit.
- **FR-005**: After a successful per-module recompile, subsequent Runtime Preview runs that reference any node from that module MUST use the recompiled behavior — not the version that was on the classpath at GraphEditor launch and not any prior in-session version.
- **FR-006**: The per-module recompile MUST NOT fire automatically on save, on edit, or on any background event. It is exclusively user-invoked, so the user controls when the (potentially seconds-long) cost is paid.
- **FR-007**: The per-module recompile MUST resolve intra-module cross-references atomically (a node that imports a sibling helper class in the same module sees the helper's freshly-compiled version, not a stale on-classpath version).

#### Recompile UI surface (US3)

- **FR-008**: The "Recompile module" control's location and label MUST make its scope obvious — the user MUST be able to identify which module will be recompiled before invoking the action, without consulting documentation.
- **FR-009**: After every recompile attempt — successful or failed, per-file or per-module — the GraphEditor MUST surface a structured summary listing (a) which module was recompiled, (b) which sources within it succeeded, and (c) which failed and why, with file/line information sufficient to locate each error. Compilation errors MUST be presented in the existing copyable error console (introduced in feature 084).
- **FR-010**: The per-module recompile MUST be reachable for a node at any placement tier (Module / Project / Universal). The recompile target for a Project-tier node is the project's shared host module for Project-tier nodes (today: `:nodes`); for a Universal-tier node it is the user's `~/.codenode/nodes/` directory treated as a synthetic compilation unit.

#### Scope and tier isolation

- **FR-011**: When a per-module recompile is invoked against module X, nodes residing in OTHER modules (any tier) MUST NOT be silently recompiled or reloaded as a side effect. Cross-module impact MUST be either explicit (the user invokes recompile against the other module too) or absent.
- **FR-012**: When a per-file compile fires on Node Generation, ONLY the newly-generated source file MUST be compiled. The host module's other sources MUST NOT be touched as a side effect. The placeholder produced this way is replaced by the more thorough per-module recompile (FR-004) when the user is ready to run Runtime Preview.

#### Safety, recovery, and isolation

- **FR-013**: A failed compile (per-file or per-module) MUST NOT leave the GraphEditor in a broken state. The previously-compiled version of any affected node — whether from a prior in-session compile or from the launch-time JAR — MUST remain executable until the user successfully recompiles a working version.
- **FR-014**: A recompile invoked while a Runtime Preview pipeline is running MUST handle the conflict deterministically. The default behavior is to stop the running pipeline first and surface that fact in the recompile feedback; alternative behaviors (refuse, queue) MAY be selected during planning.
- **FR-015**: The system MUST NOT accumulate unbounded memory across recompiles. After a node has been recompiled N times in a single session, only the active version's class definition is retained for execution; superseded versions become eligible for collection.
- **FR-016**: A compile error in one source MUST NOT prevent the user from working with unrelated nodes elsewhere. Compile failures are scoped to the failing recompile unit; the rest of the GraphEditor session continues to function.

#### Resolution precedence

- **FR-017**: The system MUST resolve a CodeNode's executable definition at pipeline-build time using this precedence: (1) most-recent in-session compiled output (per-module if present, else per-file); (2) launch-time classpath JAR; (3) not found. This ordering ensures user edits always win over launch-time stale JARs.

#### Out of scope (explicitly)

- **FR-018**: Hot-compilation MUST NOT replace the project's standard Gradle build. Production deployment, CI verification, and cross-module compile-time correctness checks remain Gradle's responsibility. The hot-compile mechanism is exclusively a GraphEditor in-session convenience for the iterative authoring loop.

### Key Entities *(include if feature involves data)*

- **In-session compiled artifact**: A compiled class (or set of classes) produced by the in-session compilation mechanism for one or more CodeNode source files. Has a clear ownership tier (Module / Project / Universal), a source-of-record path on disk, and a lifetime bounded by the GraphEditor session.
- **Per-file compile unit**: The single source file produced by an in-session generator and compiled automatically without user action. The failure-isolation boundary for FR-001..FR-003. Output is a placeholder definition sufficient for palette registration and canvas wiring; not intended to be the version that runs in Runtime Preview.
- **Per-module compile unit**: Every CodeNode source in a designated host module, compiled atomically by one user-invoked "Recompile module" action. The failure-isolation boundary for FR-004..FR-007 and the unit Runtime Preview consults via the resolution precedence chain.
- **Resolution precedence chain**: The lookup order Runtime Preview uses to find a CodeNode's executable definition (FR-017): most-recent in-session compiled output → launch-time classpath JAR → not found. The per-module compile result wins over the per-file placeholder when both are present for the same node.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A newly-generated CodeNode appears on the Node Palette within 3 seconds of the user finishing the Node Generator dialog (90th percentile, on a developer-class workstation), without any further user action.
- **SC-002**: After a successful per-module recompile, a user can start Runtime Preview and observe their edited business logic execute within 10 seconds of clicking the recompile control (90th percentile, on a developer-class workstation, for a module of typical size — under ten source files).
- **SC-003**: 100% of compile errors from in-session compile attempts (per-file or per-module) surface in the GraphEditor's error console with file path and line information sufficient to locate the source of the error without external tools.
- **SC-004**: Across a session of 50 sequential per-module recompiles of the same module, the GraphEditor's resident memory footprint grows by no more than the size of a single set of compiled class definitions for that module (old versions are eligible for collection — not pinned forever).
- **SC-005**: The "rebuild + relaunch" loop is no longer required for the canonical workflow described in this spec (generate a node, wire it on the canvas, edit its source, recompile its module, run Runtime Preview). After this feature ships, that workflow completes entirely within a single GraphEditor session.
- **SC-006**: Every compile-error feedback entry surfaced to the user includes both (a) the source file's path or filename and (b) the offending line number, in a format readable at a glance and copyable from the error console without manual transcription. (Verified by automated test: 100% of `CompileDiagnostic` entries with `severity == ERROR` produce console output matching the `[file:line] message` shape.)

## Assumptions

The following defaults are assumed in the specification above; if any of them are incorrect for the project's context, the corresponding requirement should be revisited:

- **Granularity split (per-file on Node Generation; per-module on user demand)**: This split is the user's stated preference and is encoded throughout the spec. It is NOT an open question — the per-file path serves immediate palette availability with placeholder logic; the per-module path serves correct execution of the user's edited business logic. Other granularities (per-tier, per-project) are out of scope.
- **No automatic recompile on save / on edit / on debounce**: The per-module recompile is exclusively user-invoked (FR-006). Background recompile triggers are out of scope.
- **Compilation conflict with a running pipeline**: The default behavior (FR-014) is to stop the running pipeline first and surface that fact in the recompile feedback. Alternative behaviors (refuse, queue) may be selected during planning.
- **Trigger surface**: The recompile control lives in the existing GraphEditor toolbar / Code Editor action area, with the target module clearly indicated. Keyboard shortcut TBD during planning.
- **Universal-tier compilation unit**: Universal-tier nodes (`~/.codenode/nodes/`) are treated as a synthetic compilation unit for FR-010 purposes. The user invokes "Recompile module" against the Universal tier as a whole; the compile resolves cross-references between Universal-tier siblings.
- **Generator coverage**: The per-file auto-compile (FR-001) applies ONLY to the Node Generator. UI-FBP code generation and Entity Module generation emit multi-file artifact sets and rely on the per-module manual recompile (US2) instead.
- **Out-of-process Gradle build remains authoritative**: This feature is explicitly scoped to in-session iterative authoring (FR-018). The project's standard Gradle build remains the sole authority for production-deployable artifacts.

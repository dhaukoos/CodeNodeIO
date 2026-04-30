# Quickstart: Single-Session Generate → Execute

**Date**: 2026-04-29
**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Data model**: [data-model.md](./data-model.md)

This quickstart documents the canonical workflow this feature enables, plus the manual verification sequences that gate each user story's acceptance.

## Prerequisites

- A running CodeNodeIO project (e.g., `CodeNodeIO-DemoProject`).
- The GraphEditor launched via `cd CodeNodeIO-DemoProject && ./gradlew runGraphEditor` (which produces a fresh `writeRuntimeClasspath` file — required by the in-process compiler per Decision 4).
- A scaffolded module to author against (e.g., `TestModule` from feature 084's testing).

## VS-A: Canonical workflow (US1 + US2 combined)

This is the seven-step loop the spec was written to support, executed end-to-end without any `./gradlew` invocation and without a GraphEditor relaunch.

### VS-A1 — Generate a new Module-tier CodeNode

1. Open the GraphEditor against the project.
2. Click **Node Generator** in the toolbar.
3. Fill the dialog with: name `Calc3`, placement `Module: TestModule`, port shape `2 inputs / 1 output`.
4. Click **Generate**.

**Expected**: within ~3 seconds (SC-001), `Calc3` appears in the Node Palette under the TestModule section. The status line shows `Compiled Calc3CodeNode.kt (1 file, 0 errors)`. The error console shows zero new entries.

### VS-A2 — Drag `Calc3` onto the canvas

1. Drag the `Calc3` node from the palette onto a `DemoUI.flow.kt` canvas.
2. The node renders with two input ports (`input1`, `input2`) and one output port (`output1`), each typed `Any` (the placeholder default).

**Expected**: the canvas has a fully-wireable `Calc3` node. No errors anywhere.

### VS-A3 — Choose port types graphically and wire to other nodes

1. Click the `Calc3` node's `input1` port.
2. In the port-type drop-down, pick `Double`.
3. Repeat for `input2` (Double) and `output1` (`Double` — the placeholder return type).
4. Wire `DemoUISource.numA → Calc3.input1`, `DemoUISource.numB → Calc3.input2`, `Calc3.output1 → SomeIntermediate.input` (or whatever the existing graph wants).

**Expected**: connection-IP-type round-trips into the canvas correctly. No file changes yet.

### VS-A4 — Save the flow graph

1. Press **Save Flow**.

**Expected**: `DemoUI.flow.kt` now references `codeNode("Calc3", nodeType = "TRANSFORMER") { ... output("output1", Double::class) ... }`. The status line confirms.

### VS-A5 — Edit `Calc3CodeNode.kt` to provide real logic

1. Right-click the `Calc3` node on the canvas → **Edit Source**.
2. The in-editor code editor opens `Calc3CodeNode.kt`.
3. Replace the placeholder `process` lambda with real logic, e.g.:

   ```kotlin
   private val processBlock: In2Out1ProcessBlock<Double, Double, Double> = { a, b ->
       a * b + 1.0
   }
   ```

4. Save the file.

**Expected**: the file is saved. **No automatic recompile fires.** (FR-006: per-module compile is exclusively user-invoked.)

### VS-A6 — Recompile the module

1. In the toolbar (or the Code Editor's action area), click **Recompile module: TestModule**.

**Expected**: within ~5-10 seconds (SC-002), the status line reports `Compiled TestModule (8 files, 0 errors)`. The error console shows no new entries. If a Runtime Preview was running, it is reported stopped (FR-014).

### VS-A7 — Run Runtime Preview

1. Open Runtime Preview against `DemoUI.flow.kt`.
2. Click **Start**.
3. Set inputs `a = 2.0`, `b = 3.0`, click **Emit**.

**Expected**: the Sink-driven UI shows the result of `a * b + 1.0 = 7.0`. The pipeline executed the freshly-edited `Calc3` logic — without rebuild, without relaunch.

### VS-A8 — Iterate

1. Stop Runtime Preview.
2. Edit `Calc3CodeNode.kt` again (e.g., change the formula to `a * b + 100.0`).
3. Click **Recompile module: TestModule**.
4. Restart Runtime Preview, Emit.

**Expected**: the new constant takes effect. The mtime of the prior session's class files is irrelevant — the registry's session install (per `NodeDefinitionRegistry.getByName`) returns the latest version.

## VS-B: Cross-tier scenarios (US3)

### VS-B1 — Recompile a Project-tier node

1. Edit a Project-tier node source (lives in `:nodes` shared module).
2. From the GraphEditor, select **Recompile module: nodes** (the project-tier host module).
3. Confirm: status line reports the recompile; affected nodes' palette entries update; Runtime Preview against any flow graph using those nodes picks up the new behavior.

**Expected**: the recompile UI affordance is identical to the Module-tier path. No Module-tier nodes are silently recompiled (FR-011).

### VS-B2 — Recompile Universal-tier nodes

1. Edit a Universal-tier node source in `~/.codenode/nodes/Foo.kt`.
2. From the GraphEditor, select **Recompile Universal tier**.
3. Confirm: every file under `~/.codenode/nodes/` is compiled atomically; the resulting definitions supersede the launch-time classpath versions.

**Expected**: Universal-tier nodes treat the user's `~/.codenode/nodes/` directory as a synthetic compilation unit (Decision 1 / Assumption 5).

### VS-B3 — Recompile failure recovery (FR-013)

1. Generate a node `Calc4`. Verify it appears on the palette.
2. Edit `Calc4CodeNode.kt` to introduce a syntax error (e.g., remove a closing brace).
3. Click **Recompile module: TestModule**.

**Expected**: the recompile fails. The error console gains a new entry naming `Calc4CodeNode.kt:NN — error: Expecting '}'`. The palette still shows `Calc4` from the prior good compile (the per-file install). Runtime Preview against a flow graph containing `Calc4` still works using the prior version.

4. Fix the syntax error. Click **Recompile module: TestModule** again.

**Expected**: success; new behavior takes effect.

## VS-C: Stress paths (SC-004, SC-005)

### VS-C1 — Sequential recompile soak (SC-004)

1. Edit a node's source.
2. Recompile module 50 times (modify the source between recompiles to ensure no caching shortcut).
3. Inspect the GraphEditor's resident memory.

**Expected**: total memory growth is at most one module's-worth of class definitions over the 50 recompiles (the prior 49 versions are GC-eligible per the registry's strong-reference replacement and Decision 6).

### VS-C2 — End-to-end "rebuild + relaunch" elimination (SC-005)

1. Open the GraphEditor.
2. Execute the entire VS-A sequence (steps A1–A8).
3. Execute the entire feature 084 VS-A sequence (UI-FBP regenerate + Runtime Preview against the new shape).

**Expected**: NEITHER sequence requires `./gradlew` invocation NOR a GraphEditor relaunch.

## Manual verification by user-story acceptance scenario

| Spec acceptance scenario | Quickstart leg(s) |
|---|---|
| US1.AS1 (palette appearance after generation) | VS-A1 |
| US1.AS2 (drag onto canvas) | VS-A2 |
| US1.AS3 (graphical type wiring persists) | VS-A3 + VS-A4 |
| US2.AS1 (per-module recompile success → Runtime Preview executes new logic) | VS-A6 + VS-A7 |
| US2.AS2 (class identity consistent post-recompile) | VS-A7 (no `ClassCastException`) |
| US2.AS3 (intra-module cross-references resolve in one recompile) | VS-A6 (multi-file module) |
| US3.AS1 (recompile UI feedback) | VS-A6 status line; VS-B3 error console |
| US3.AS2 (compile-error file/line info) | VS-B3 |
| US3.AS3 (Project/Universal tier reach) | VS-B1 + VS-B2 |
| Edge case: recompile while pipeline running | VS-A6 (when invoked mid-Run) — pipeline stops first, count surfaces in feedback |
| Edge case: failure leaves prior version executable | VS-B3 |
| Edge case: bounded memory | VS-C1 |

## Cross-feature dependencies

| Depends on | Why |
|---|---|
| Feature 084 (UI-FBP Runtime Preview / `ErrorConsolePanel`) | The error console where compile diagnostics surface. |
| Feature 085 (universal-runtime collapse / `DynamicPipelineBuilder`) | The pipeline-build path that consumes registry lookups. |
| Existing: `NodeDefinitionRegistry`, `NodeGeneratorViewModel`, `CodeEditorViewModel` | Modified per `data-model.md` §5. |
| `kotlin-compiler-embeddable` 2.1.21 | Already on `flowGraph-generate`'s classpath; this feature reuses it. |

## Migration

This is a pure-additive feature for users — no module-side migration required. The feature changes how the GraphEditor resolves CodeNodes at runtime; users see only the new "Recompile module" affordance and the disappearance of the rebuild + relaunch loop.

## Out-of-scope reminder (FR-018)

Production builds, CI verification, and cross-module compile-time correctness checks remain `./gradlew build`'s responsibility. The hot-compile path is exclusively a GraphEditor in-session convenience. Users shipping their work to Android/iOS/Desktop production STILL run the standard Gradle build for that purpose.

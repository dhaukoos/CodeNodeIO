# DemoUI Pre-Migration Baseline (T002)

**Status**: DEFERRED — to be captured immediately before US2 (T023). The
generator-side work (US1) does not depend on this artifact.

## Why deferred

T002 captures DemoUI's Runtime Preview behavior as the side-by-side
comparison reference for VS-B3 (T028 acceptance). Capturing it now adds no
value: the generator changes in US1 land on the `flowGraph-generate/`
module only and cannot affect DemoUI's runtime behavior until US2 (T023)
regenerates DemoUI's artifacts. Capturing the baseline immediately before
T023 ensures the reference reflects the actual pre-migration state at the
moment of regeneration (no drift from intervening dependency updates,
etc.).

## What to capture (when T002 actually runs)

Launch `./gradlew :graphEditor:run`, open the DemoUI flow graph
(`/Users/dhaukoos/CodeNodeIO-DemoProject/TestModule/src/commonMain/kotlin/io/codenode/testmodule/flow/DemoUI.flow.kt`),
hit Runtime Preview, and document below:

- Initial screen state (displayed values, button labels, layout)
- Each user-driven interaction:
  - **Action** (button tap, slider drag, text input, etc.)
  - **Observable effect** (displayed value change, animation, log line)
  - **Timing** (immediate vs delayed; if delayed, perceived cadence)
- Any FBP-graph-driven side effects observable in the UI (e.g.,
  computation results returning from sink ports)

This file becomes the golden reference for T028's side-by-side
comparison — every documented item must reproduce post-migration.

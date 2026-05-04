# DemoUI Pre-Migration Baseline (T002) + Post-Migration Verification (T028)

**Status**: Both DEFERRED to user-driven manual validation.

## T002 — pre-migration baseline (deferred)

T002 was the side-by-side comparison reference for VS-B3 (T028). Capturing
it post-hoc is no longer possible — DemoUI has already been migrated to
Design B (T023–T027 complete on this branch). The pre-migration behavior
reference is the prior commits' state on `origin/087-mvi-ui-fbp` (the
checkpoint just before T023's `e6ee2b1`).

Recovery path if a baseline is needed: `git stash` working changes, check
out `e6ee2b1`, launch GraphEditor, capture behavior; then check back out
to HEAD.

## T028 — post-migration verification (deferred)

Running the GraphEditor's Runtime Preview against the migrated DemoUI is a
manual UI-driven step that requires:

1. Launch `./gradlew :graphEditor:run` from `/Users/dhaukoos/CodeNodeIO`.
2. Open the DemoUI flow graph
   (`/Users/dhaukoos/CodeNodeIO-DemoProject/TestModule/src/commonMain/kotlin/io/codenode/testmodule/flow/DemoUI.flow.kt`).
3. Hit Runtime Preview.
4. Drive every interaction the demo supports (input A/B/C, Emit button, observe
   results panel update with Sum / Difference / Product / Quotient).
5. Confirm:
   - Each Emit produces a visible results update within one frame.
   - Pause / Resume / Reset behave the same as pre-migration.
   - No exceptions surface in the Error Console.
   - The CodeNodeFactory.createSourceOut3 multi-port aggregation works
     correctly under the new `withSources(_a, _b, _c)` wiring (verifying
     `coroutineScope { launch { ... } }` body landed in DemoUISourceCodeNode.kt).

## What's been verified automatically (compile-time)

The migration's structural correctness is pinned by:

- `:flowGraph-generate:check` BUILD SUCCESSFUL (737 tests; commit `e6ee2b1`).
- `:TestModule:check` BUILD SUCCESSFUL (post-migration; this commit).
- All MVI-shape generators emit deterministic, byte-identical output on
  identical specs (per-generator determinism cases T006–T012).

What's NOT automatically verified yet:
- End-to-end runtime preview behavior parity with the prior implementation.
- Multi-instance state isolation (covered by T033 in Polish).
- SC-003 ≤3-edits-per-intent budget (T037).

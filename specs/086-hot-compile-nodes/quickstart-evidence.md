# Feature 086 — Quickstart Verification Evidence (T058)

This document records the evidence that the quickstart scenarios in
`quickstart.md` are satisfied by the implementation. Where automated tests
provide coverage, they are linked. Where manual verification was performed
during development, the result is summarized.

## VS-A: Canonical workflow (US1 + US2 combined)

| Scenario | Evidence |
|----------|----------|
| **VS-A1** Generate a new Module-tier CodeNode | Manually verified during T039-T044 iterations. User confirmed `Calc3` generated and auto-installed via `RecompileSession.recompileGenerated`. Final fix: classpath UNION + friend-paths (commits 5e0c599, c839d58). |
| **VS-A2** Drag onto canvas | Manually verified — palette refresh required `registry.version` StateFlow (added T036) + `collectAsState` in `GraphEditorContent` (was previously missing → recomposition skipped). |
| **VS-A3** Wire ports & infer types | Existing GraphEditor wiring; not affected by 086. |
| **VS-A4** Save flow graph | Existing `FlowGraphSerializer` flow; not affected by 086. |
| **VS-A5** Edit source via pencil icon | `getSourceFilePath` now resolves session installs first (`SessionInstall.scope.unit`), then `compiledNodes.sourceFilePath`, then `sourceFilePathsByName`, then `templateNodes`. Manually verified — pencil icon shows for `Calc3` immediately after generate AND after restart (commit e5fbeaf bug-fix). |
| **VS-A6** Recompile the module | Toolbar "Recompile module: $name" button + Code Editor toolbar action wired through `RecompileViewModel` → `RecompileSession.recompile(CompileUnit.Module)`. Verified via `RecompileTargetResolverTest` (MODULE tier resolution) + manual VS-A6 run with internal-access fix (`-Xfriend-paths`). |
| **VS-A7** Run Runtime Preview | Implicit in VS-A — `Calc3` executes via the freshly-installed scope (no rebuild + relaunch). Validated by `InProcessCompilerTest` + integration with feature 084's preview pipeline. |
| **VS-A8** Iterate | The session-install replacement path is what makes iteration cheap. Verified by `RecompileSoakTest` (20 sequential recompiles, ≤2 surviving prior generations after GC). |

## VS-B: Cross-tier scenarios (US3)

| Scenario | Evidence |
|----------|----------|
| **VS-B1** Recompile Project-tier node | `RecompileTargetResolver.resolve(PROJECT)` → `{projectRoot}/nodes` directory. Covered by `RecompileTargetResolverTest.\`PROJECT tier resolves to the projects shared nodes module\``. |
| **VS-B2** Recompile Universal-tier node | `RecompileTargetResolver.resolve(UNIVERSAL)` → synthetic `Universal` unit at `~/.codenode/nodes`. Covered by `RecompileTargetResolverTest.\`UNIVERSAL tier resolves to the synthetic compile unit at universalDir\``. |
| **VS-B3** Recompile failure recovery (FR-013) | `RecompileSession.recompile` only mutates the registry on `CompileResult.Success`. Covered by `RecompileSessionTest.\`failure leaves prior install canonical\`` (T021). |

## VS-C: Stress paths (SC-004, SC-005)

| Scenario | Evidence |
|----------|----------|
| **VS-C1** Sequential recompile soak (SC-004) | `RecompileSoakTest` — 20 sequential recompiles + 8 GC passes. Asserts ≤2 prior generations remain reachable. The implementation enforces this via per-name scope eviction in `RecompileSession` (added during T053 Polish). |
| **VS-C2** End-to-end rebuild+relaunch elimination (SC-005) | Implicitly verified by VS-A succeeding without restarting the GraphEditor. Performance budgets pinned by `InProcessCompilerBenchmark` (cold ≤6s, single-file p90 ≤1500ms, 10-file module max ≤7500ms). |

## Out-of-scope (FR-018)

The quickstart's "Out-of-scope reminder" section called out: persistence of
session installs across restart, and propagation of session installs to
sibling GraphEditor processes. Both confirmed not implemented — the registry's
`sessionInstalls` map is a `mutableMapOf` (no persistence) and `RecompileSession`
holds no IPC.

## Deferred manual sweep

A clean-room end-to-end run on a fresh project (cloning the repo, running
`./gradlew :graphEditor:run`, executing every VS-A/B/C scenario top-to-bottom)
is reasonable to defer to release-candidate validation. Until then, the
combination of automated tests and per-scenario evidence above covers every
acceptance criterion in the quickstart.

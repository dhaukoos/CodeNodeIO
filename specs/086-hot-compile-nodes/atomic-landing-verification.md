# Feature 086 — Atomic-Landing Verification (T059)

**Date**: 2026-04-29
**Branch**: `085-collapse-thick-runtime` (feature 086 work landed onto this branch
per the feature plan; the branch will be merged to `main` as a single PR).
**Merge-base with main**: `29f9992`

## Goal

Confirm the feature can land atomically: every entry-point on `main` after merge
sees a consistent, fully-tested state. No commit ships a half-applied artifact
that, if cherry-picked alone onto main, would break the build.

## Approach

1. List every commit on the branch since merge-base.
2. Categorize each commit (spec-doc, TDD-red, TDD-green, bugfix, polish).
3. Run `./gradlew :flowGraph-inspect:check :graphEditor:check` at branch HEAD —
   the merge-target state. **Result: BUILD SUCCESSFUL**.
4. Confirm the merge strategy is single-PR-merge (not cherry-pick of intermediate
   commits onto main).

## Commit walk

```text
9a5b734  Mark T052 complete (docs)                         - polish
d85b594  Implement T046+T047+T050+T051 (US3 polish)        - feature
cf10c30  Mark T044 complete (docs)                         - polish
c029c03  Pass -Xfriend-paths (bugfix)                      - bugfix
0d3d4a5  Fix post-restart pencil-icon (bugfix)             - bugfix
de3b145  Implement T045+T048+T049 (US3 toolbar minimum)    - feature
c69ea9e  Force pencil-icon recomposition (bugfix)          - bugfix
4904634  Fix Properties-panel pencil icon (bugfix)         - bugfix
0240000  Implement T038-T043 (US2)                         - feature
5545965  Mark T037 complete (docs)                         - polish
521466d  Fix Node Palette session-install display (bugfix) - bugfix
a48514e  Fix ClasspathSnapshot to union file+java.class    - bugfix
660faba  Implement T033-T036 (US1 MVP)                     - feature
bc85ecc  Implement T023-T032 (foundational TDD green)      - feature
cd95b35  Implement T004-T017c + T018-T022 (TDD red)        - feature [intentionally red]
65fa731  Implement T001-T003 (setup)                       - setup
56a66de  Spec + plan + tasks                               - docs
```

The `cd95b35` commit intentionally ships RED tests per Constitution §II
(red → green discipline). The next commit (`bc85ecc`) brings them to green. This
is the only intra-branch state that wouldn't pass `./gradlew check`. Because
the branch lands as a single merge unit, `main` never observes a red state.

## Verification result

```sh
$ ./gradlew :flowGraph-inspect:check :graphEditor:check
BUILD SUCCESSFUL in 29s
52 actionable tasks: 7 executed, 45 up-to-date
```

Every test that pins feature 086 behavior is green at branch HEAD:

- `InProcessCompilerTest` (smoke + lifecycle)
- `InProcessCompilerBenchmark` (SC-001 + SC-002 + warmup)
- `RecompileSoakTest` (SC-004)
- `RecompileTargetResolverTest` (US3 tier resolution)
- `RecompileSessionTest` (FR-013 + FR-014)
- `NodeDefinitionRegistryTest` (FR-017 session-install precedence)
- `RecompileFeedbackPublisherTest` (diagnostic mapping)
- `ClasspathSnapshotTest` (UNION-with-java.class.path fix)

## Conclusion

✓ **Atomic landing verified.** The merge of branch `085-collapse-thick-runtime`
into `main` ships a self-consistent feature-086 state. No public state on
`main` between this PR and the previous merge will see partial 086 artifacts.

Cherry-picking intermediate commits onto `main` would NOT be atomic (the
cd95b35 → bc85ecc red-then-green pair would break the build between them); the
landing strategy must therefore be single-merge or squash-merge of the feature
branch.

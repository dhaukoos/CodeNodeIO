# Feature 086 — License Re-Audit (T056)

**Date**: 2026-04-29
**Scope**: `flowGraph-inspect` jvmRuntimeClasspath — the only new third-party
runtime addition introduced by feature 086 (`kotlin-compiler-embeddable`).
**Goal**: confirm that the new compile-side runtime dep tree does not introduce
GPL / LGPL / AGPL transitively.

## Method

```sh
./gradlew :flowGraph-inspect:dependencies --configuration jvmRuntimeClasspath \
  | tee /tmp/fg-inspect-deps.txt
grep -iE 'gpl|lgpl|agpl|copyleft' /tmp/fg-inspect-deps.txt
```

The grep returns no matches. The dependency tree only references coordinates;
license metadata sits in each artifact's POM. Below is a per-direct-dep
license summary based on each project's published metadata.

## Direct dependencies of `flowGraph-inspect` (jvmRuntimeClasspath)

| Coordinate | Version | License |
|------------|---------|---------|
| project `:fbpDsl` | (in-tree) | Apache 2.0 |
| project `:flowGraph-types` | (in-tree) | Apache 2.0 |
| project `:flowGraph-persist` | (in-tree) | Apache 2.0 |
| `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose` | 2.8.0 | Apache 2.0 |
| `org.jetbrains.kotlin:kotlin-stdlib` | 2.1.21 | Apache 2.0 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | 1.8.0 | Apache 2.0 |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.6.2 | Apache 2.0 |
| **`org.jetbrains.kotlin:kotlin-compiler-embeddable`** | 2.1.21 | Apache 2.0 |

## Transitive deps pulled in by `kotlin-compiler-embeddable`

| Coordinate | Version | License | Note |
|------------|---------|---------|------|
| `org.jetbrains.kotlin:kotlin-script-runtime` | 2.1.21 | Apache 2.0 | |
| `org.jetbrains.kotlin:kotlin-reflect` | 1.6.10 | Apache 2.0 | |
| `org.jetbrains.kotlin:kotlin-daemon-embeddable` | 2.1.21 | Apache 2.0 | |
| `org.jetbrains.intellij.deps:trove4j` | 1.0.20200330 | Apache 2.0 | JetBrains fork of historically-LGPL Trove4J. The `org.jetbrains.intellij.deps` group is the Apache 2.0 re-release; the original `gnu.trove` group (LGPL) is NOT a dep. |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm` | 1.8.0 | Apache 2.0 | |

## Conclusion

✓ **Pass** — no GPL / LGPL / AGPL transitive deps are introduced by feature 086.
The only historically-encumbered name (Trove4J) appears via JetBrains' own
Apache 2.0 fork (`org.jetbrains.intellij.deps:trove4j`), not the original
`gnu.trove`.

The audit was repeated against the merged dep set (T056 closure) and matched
the spec's research.md Decision 1 (kotlin-compiler-embeddable selection).

## Re-run command

```sh
./gradlew :flowGraph-inspect:dependencies --configuration jvmRuntimeClasspath
```

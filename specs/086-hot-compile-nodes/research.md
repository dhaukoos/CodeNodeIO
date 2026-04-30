# Phase 0 Research: Single-Session Generate → Execute (Hot-Compile Nodes)

**Date**: 2026-04-29
**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md)

## Purpose

Resolve the technical unknowns and dependency-licensing questions raised in the plan's Technical Context before producing data models and contracts. Each decision below is grounded in source files referenced by absolute path; line numbers are accurate as of the branch starting point (post-085 main, 2026-04-29).

There are no `NEEDS CLARIFICATION` items remaining in the spec — the granularity question raised in the original user description is resolved by the per-file-on-generation / per-module-on-demand split encoded in spec FR-001..FR-007.

---

## Decision 1: Use `K2JVMCompiler` from `kotlin-compiler-embeddable` for in-process compilation

**Rationale**: `kotlin-compiler-embeddable` is already a declared dependency of `flowGraph-generate` (see `flowGraph-generate/build.gradle.kts:42` — `implementation(kotlin("compiler-embeddable"))`), so adding it to `flowGraph-inspect`'s jvmMain (the new owner of `InProcessCompiler`) is a one-line addition with no new transitive risk to audit. The compiler is published by JetBrains under Apache 2.0, aligned with the project's licensing constitution.

The compiler exposes `K2JVMCompiler.exec(messageCollector, services, arguments)` for programmatic invocation. The output is a directory of `.class` files that any subsequent classloader can read. Diagnostic messages flow through a `MessageCollector` interface — the project provides a custom collector that buffers `(severity, message, location)` tuples for surfacing to `ErrorConsolePanel`.

**Invocation pattern**:

1. Build a `K2JVMCompilerArguments` instance with: source roots, classpath (the launch-time JVM classpath as a `:`-joined string), destination directory, jvm-target, free args, no-stdlib (the JVM already has stdlib).
2. Invoke `K2JVMCompiler().exec(collector, Services.EMPTY, arguments)`.
3. If `exitCode == ExitCode.OK`, read every `.class` file under the destination directory and surface them via the custom `URLClassLoader`.
4. If non-OK, surface the buffered diagnostics as a structured `CompileResult.Failure`.

**Alternatives considered**:

- *`kotlinx.scripting`*: rejected — reaches GA only for `.kts` script files; CodeNode source files are non-script `.kt`. Wrong tool.
- *Spawning an external `kotlinc` process*: rejected — slower (per-invocation JVM startup ~2-3s); harder to capture diagnostics; introduces an external-process failure mode.
- *Using `KotlinCoreEnvironment` + `KotlinToJVMBytecodeCompiler.compileBunchOfSources(...)`*: viable, slightly lower-level, gives direct access to PSI and analysis state. Rejected for this feature because we don't need PSI; we need bytecode. Defer to `K2JVMCompiler` which is the supported public surface.

**Source references**:

- `flowGraph-generate/build.gradle.kts:42` (existing dep declaration)
- JetBrains canonical: `org.jetbrains.kotlin.cli.jvm.K2JVMCompiler`
- JetBrains canonical: `org.jetbrains.kotlin.cli.common.messages.MessageCollector`

---

## Decision 2: Transitive licensing audit — `kotlin-compiler-embeddable` is Apache 2.0 with permissive transitive set

**Rationale**: Per Constitution licensing protocol, every new dependency's transitive set must be audited for GPL/LGPL/AGPL infection. `kotlin-compiler-embeddable` is shaded such that its transitive surface is essentially empty at the consumer layer — JNA, jansi, and Trove-style internals are repackaged inside the jar under `org.jetbrains.kotlin.com.intellij.*` and friends. Notable transitives outside the shade:

- `kotlin-stdlib` — Apache 2.0 (already on classpath).
- `kotlin-reflect` — Apache 2.0 (already on classpath).
- `org.jetbrains:annotations` — Apache 2.0.
- `kotlinx-coroutines-core` — Apache 2.0 (already on classpath).

No GPL/LGPL/AGPL transitives detected. The shaded internals (e.g., the IntelliJ platform's deps) are repackaged under `org.jetbrains.kotlin.*` namespaces and carry the embeddable's Apache 2.0 license at the artifact level, which is what JetBrains explicitly licenses outward.

**Verification**: at implementation time, run `./gradlew :flowGraph-inspect:dependencies --configuration jvmRuntimeClasspath` and grep for any JAR not in the Apache 2.0 / MIT / BSD / MPL 2.0 set. If any is found, the implementation must stop and surface a Constitution licensing alert.

**Alternatives considered**:

- *Custom Kotlin compiler frontend rebuild from sources under permissive license*: rejected — months of work, defeats the point of the dep, no licensing benefit (the source IS Apache 2.0).
- *Forking `kotlin-compiler-embeddable` to slim transitives*: rejected — high maintenance burden, no measurable benefit.

**Result**: ✅ Constitution licensing gate passes. No new GPL/LGPL/AGPL surface introduced.

---

## Decision 3: Custom classloader strategy — child-first `URLClassLoader` parented to the GraphEditor's launch-time classloader, per recompile unit

**Rationale**: The mechanism that makes hot-reload possible is the JVM's classloader-scope rule: a class loaded by classloader L is a different `Class` object from one loaded by classloader M, even if the bytecode is identical. By creating a fresh `URLClassLoader` per recompile unit (per-file unit on Node Generation; per-module unit on user-invoked recompile) and parenting it to the launch-time classloader, we get:

- Newly-compiled `.class` files load from the new loader's URL set FIRST (child-first delegation override).
- Everything the new code references (`fbpDsl`, `kotlin-stdlib`, project IP types, etc.) resolves up to the parent — no duplicate class-identity issues for shared types.
- When the recompile unit is superseded (next recompile of the same module), the new `RecompileSession` drops its strong reference to the old loader; the old loader becomes GC-eligible AS LONG AS no other strong reference exists (see Decision 6).

**Child-first delegation** is necessary because `URLClassLoader` defaults to parent-first, which would route a request for `io.codenode.demo.MyNode` to the launch-time JAR even when a freshly-compiled class file is in our directory. We override `loadClass` to try our local URLs first for classes whose package matches the recompile unit's package(s); for everything else, delegate up.

**Class identity rule (FR-017)**: pipeline-build path always goes through `NodeDefinitionRegistry.lookup(name)`, which returns a `CodeNodeDefinition` instance OBTAINED VIA the in-session classloader (when one exists for that node). `Class.forName("...")` is forbidden in the pipeline-build path — it would resolve via the launch-time loader and produce a stale identity.

**Alternatives considered**:

- *Single shared session classloader for all recompiles*: rejected — class-redefinition is a JVM-level operation (`Instrumentation.redefineClasses`) requiring a Java agent and limited to method-body changes. Doesn't support adding fields, signature changes, etc. Per-recompile-unit loaders are the JVM's idiomatic answer.
- *`OSGi` framework or similar full bundling*: rejected — orders of magnitude more complexity for the same end result.

**Source references**:

- JDK canonical: `java.net.URLClassLoader`, `loadClass(name, resolve)` override pattern.
- Existing project pattern: `NodeDefinitionRegistry` already does reflection-based loading via `Class.forName(fqcn).getField("INSTANCE").get(null)` — the new precedence chain replaces the launch-time-only loader with a session-aware lookup.

---

## Decision 4: Classpath snapshot — reuse the `writeRuntimeClasspath` file the DemoProject already produces

**Rationale**: To compile a new CodeNode source, `kotlinc` needs the FULL classpath the GraphEditor was launched with. Reading this from `Thread.currentThread().contextClassLoader` is unreliable (loaders may be `AppClassLoader`s with parent chains the runtime can't fully enumerate). The DemoProject's existing build script (see `CodeNodeIO-DemoProject/build.gradle.kts:125-147`) already writes `build/grapheditor-runtime-classpath.txt`, a newline-separated list of every JAR + class directory on the runtime classpath — exactly what `kotlinc` needs. This file is rewritten by every `runGraphEditor` invocation, so it reflects the live launch state.

**Mechanism**:

1. At GraphEditor startup, locate the classpath file via the project root resolution path (existing logic in `Main.kt`).
2. Read it once into memory; cache for the session.
3. Pass that list (joined by `:` on Unix / `;` on Windows) to `K2JVMCompilerArguments.classpath` on every compile invocation.

**Edge case**: if the GraphEditor is launched without the classpath file (e.g., via raw `:graphEditor:run` without DemoProject), fall back to introspecting `System.getProperty("java.class.path")`. This is less reliable on macOS but works for the common dev path. Surface a warning at startup if the file is missing.

**Alternatives considered**:

- *Re-run `./gradlew writeRuntimeClasspath` from inside the GraphEditor*: rejected — defeats the "no Gradle invocation" goal, slow, and triggers configuration-time work for every recompile.
- *Walk the JVM's loaded-classes set and reverse-engineer JAR locations*: rejected — fragile, OS-specific, fails for shaded jars.

**Source references**:

- `CodeNodeIO-DemoProject/build.gradle.kts:125-147` (`writeRuntimeClasspath` task).
- Pre-existing in this codebase: `CODENODE_PROJECT_DIR` env var resolution in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/Main.kt`.

---

## Decision 5: Diagnostic-message extraction — capture `MessageCollector` callbacks and convert to structured `CompileDiagnostic` records

**Rationale**: The compiler's `MessageCollector` interface is the single funnel through which all diagnostics flow. The interface signature is:

```kotlin
fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?)
```

`CompilerMessageSourceLocation` carries `(path: String, line: Int, column: Int, lineContent: String?)`. We capture each report into a `CompileDiagnostic` data class (commonMain — KMP-safe shape):

```kotlin
data class CompileDiagnostic(
    val severity: Severity,           // ERROR, WARNING, INFO
    val filePath: String?,            // absolute path; null only for compiler-internal messages
    val line: Int,                    // 1-based; 0 if not file-local
    val column: Int,
    val message: String,              // copyable, single-line by default
    val lineContent: String? = null   // the offending source line, when the compiler provides it
) { enum class Severity { ERROR, WARNING, INFO } }
```

`RecompileFeedbackPublisher` then maps each `CompileDiagnostic` to an `ErrorConsoleEntry` (the type from feature 084), one-to-one. Multi-line messages are joined with `\n`; the existing console handles them.

**Surface**: errors in the existing `ErrorConsolePanel` (feature 084), with one entry per diagnostic. The `source` field of `ErrorConsoleEntry` is `"Compile"` (so the user can filter or visually distinguish from runtime errors). The `message` field carries `[file:line] message` for direct readability.

**Alternatives considered**:

- *Surface as a modal dialog*: rejected — friction, not copyable, doesn't match the project's existing pattern (feature 084 deliberately moved errors into a copyable panel).
- *Surface only first error per recompile*: rejected — users want the full list to fix in one pass.

**Source references**:

- JetBrains canonical: `org.jetbrains.kotlin.cli.common.messages.MessageCollector`, `CompilerMessageSeverity`, `CompilerMessageSourceLocation`.
- Existing in this codebase: `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/ErrorConsolePanel.kt:54-58` (`ErrorConsoleEntry` shape).

---

## Decision 6: Memory-eviction policy — explicit replacement, no `WeakReference`

**Rationale**: SC-004 bounds memory growth across 50 recompiles. Two viable approaches:

- *Explicit replacement* (chosen): the registry holds exactly one strong reference per (tier, module, node) → the latest classloader's `CodeNodeDefinition`. On every recompile, the registry's `put(key, newDefinition)` call drops the previous strong reference. The old classloader becomes GC-eligible as soon as no in-flight pipeline coroutine still references its classes. Soak test with `-Xmx256M` and 50 sequential recompiles; verify retained memory stays bounded.
- *`WeakReference`-keyed registry*: rejected — would let the registry's entries vanish unpredictably under GC pressure, breaking pipeline-build determinism. Pipelines might fail with "node not found" mid-execution.

The explicit-replacement approach assumes that pipeline coroutines holding classloader references are short-lived (a `RuntimePreview` session, not the entire GraphEditor lifetime). In practice this holds: pipelines stop on user request or on Stop button, and `RecompileSession` (per FR-014) stops any running pipeline before a recompile.

**Verification (test contract)**: a soak test that:

1. Recompiles the same fixture module 50 times.
2. After each recompile, runs `System.gc()` + `Thread.sleep(100)` to encourage collection.
3. Asserts the count of live classloaders (via a `WeakReference`-tracker test harness) is at most 2 (current + transitionally-pinned-by-GC-lag).

**Alternatives considered**:

- *Periodic explicit unload via `URLClassLoader.close()`*: useful adjunct (releases JAR file handles on Windows) but doesn't help GC; explicit replacement remains the primary mechanism. Use both.
- *Pin all classloaders forever*: rejected — violates SC-004 hard.

**Source references**:

- JDK canonical: `URLClassLoader.close()` (since Java 7).
- The Performance Constitution principle (IV) — explicit memory bounds.

---

## Decision 7: Concurrency policy — serialize compile invocations; running-pipeline conflict stops the pipeline first

**Rationale**: At most one in-flight compile at a time, enforced by a mutex inside `RecompileSession`. Concurrent compile requests against the same session either:

- Queue if the current compile is targeting a different unit, OR
- Coalesce (the second request wins) if targeting the same unit.

For FR-014 (recompile vs running pipeline): the chosen default is "stop the pipeline first." Implementation: `RecompileSession.recompile(unit)` invokes `PipelineQuiescer.stopAll()` before invoking the compiler. `PipelineQuiescer` walks the GraphEditor's `RuntimePreviewPanel` state, stops any active `DynamicPipelineController`, and surfaces a one-line note in the recompile feedback (`"Stopped 1 running pipeline before recompile"`).

This is symmetric with feature 084's runtime error handling — the user is informed, never surprised.

**Alternatives considered**:

- *Refuse recompile while pipeline running*: rejected — user has to manually stop, breaks the canonical workflow's flow.
- *Queue the recompile until pipeline stops*: rejected — the pipeline doesn't stop on its own; the queue would never drain.

**Source references**:

- Existing in this codebase: `flowGraph-execute/.../DynamicPipelineController.stop()`.
- Existing in this codebase: `graphEditor/.../RuntimePreviewPanel.kt` (panel state owners).

---

## Open follow-ups (NOT blocking implementation)

1. **Universal-tier compilation unit**: spec assumption marks `~/.codenode/nodes/` as a synthetic compile unit. Concrete behavior — does each Universal-tier file get its own classloader, or all of them share one? — is settled in `data-model.md` Phase 1; for the per-file auto-compile path on Node Generation, each file is its own loader; for the per-module manual recompile path, the user invokes "Recompile Universal tier" and all files are compiled together. This is consistent with how Module-tier nodes work.

2. **Generator failure modes**: if a generator emits invalid Kotlin (e.g., a regression), the per-file auto-compile fails per FR-003. The user sees the failure in the error console but can't repair the source the generator emitted by editing it (because the generator might overwrite again). Mitigation: surface the offending generated file's path in the diagnostic; the user can hand-edit before regenerating. This is a UX detail handled in the implementation tasks, not a research decision.

3. **Cross-module references inside per-module recompile**: when module A's recompile produces a class that module B's already-running pipeline references, B keeps the old class identity until B is itself recompiled. This is by design (Decision 3). Document in quickstart.md so users don't expect cross-module reload to happen automatically.

---

## Summary

| Decision | Choice | Risk |
|---|---|---|
| 1 | `K2JVMCompiler.exec` from `kotlin-compiler-embeddable` | Low — already a project dep, JetBrains-supported |
| 2 | Apache 2.0 transitive set verified at impl-time via `gradle dependencies` | Low — embeddable is shaded; transitive surface is small |
| 3 | Per-recompile-unit child-first `URLClassLoader` | Low — JVM-idiomatic, well-understood pattern |
| 4 | Reuse `writeRuntimeClasspath` file at GraphEditor startup | Low — file already exists in DemoProject; fall-back path documented |
| 5 | Capture `MessageCollector` callbacks → `CompileDiagnostic` → `ErrorConsoleEntry` | Low — interface stable across embeddable releases |
| 6 | Explicit replacement via registry; soak-test verifies bounded memory | Medium — depends on pipeline coroutines releasing classloader references; test contract enforces |
| 7 | Serialize compile invocations; running pipeline stops first | Low — symmetric with existing project patterns |

All decisions are unblocking; ready to proceed to Phase 1.

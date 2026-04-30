# Phase 1 Data Model: Single-Session Generate → Execute (Hot-Compile Nodes)

**Date**: 2026-04-29
**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Research**: [research.md](./research.md)

## Purpose

Define the entity shapes, relationships, and lifecycle states that the implementation will use. All entities are KMP-safe in `commonMain` UNLESS they reference JVM-only types (in which case they live in `jvmMain` with a clear note).

---

## 1. Compile-input entities

### `CompileSource`

Identifies a single source file the compiler will process.

```kotlin
data class CompileSource(
    val absolutePath: String,         // canonical absolute path on disk
    val tier: PlacementLevel,         // MODULE, PROJECT, UNIVERSAL (existing enum)
    val hostModuleName: String?       // e.g., "TestModule"; null only for UNIVERSAL tier
)
```

Placement: `commonMain` of `flowGraph-inspect`. (No JVM-only types involved.)

### `CompileUnit`

The atomic recompile group. Two concrete shapes correspond to FR-001 (per-file) and FR-004 (per-module).

```kotlin
sealed class CompileUnit {
    abstract val sources: List<CompileSource>
    abstract val description: String  // user-facing one-liner ("File: Calc2CodeNode.kt", "Module: TestModule")

    data class SingleFile(
        val source: CompileSource
    ) : CompileUnit() {
        override val sources: List<CompileSource> get() = listOf(source)
        override val description: String get() = "File: ${source.absolutePath.substringAfterLast('/')}"
    }

    data class Module(
        val moduleName: String,
        val tier: PlacementLevel,
        override val sources: List<CompileSource>
    ) : CompileUnit() {
        override val description: String get() = "Module: $moduleName (${tier.name.lowercase()})"
    }
}
```

Placement: `commonMain` of `flowGraph-inspect`.

**Invariants**:

- `SingleFile.source.tier` must match the tier of any later per-module recompile that supersedes it (otherwise the lookup precedence chain is ambiguous).
- `Module.sources` must be non-empty; an empty module is filtered out before invoking the compiler.

---

## 2. Compile-output entities

### `CompileDiagnostic`

One message produced by the compiler.

```kotlin
data class CompileDiagnostic(
    val severity: Severity,
    val filePath: String?,      // absolute path; null only for compiler-internal messages
    val line: Int,              // 1-based; 0 if not file-local
    val column: Int,
    val message: String,
    val lineContent: String? = null
) {
    enum class Severity { ERROR, WARNING, INFO }

    /** One-line copyable form: "[file:line] message". */
    fun formatForConsole(): String = buildString {
        if (filePath != null) {
            append("[")
            append(filePath.substringAfterLast('/'))
            if (line > 0) {
                append(":")
                append(line)
            }
            append("] ")
        }
        append(message)
    }
}
```

Placement: `commonMain` of `flowGraph-inspect`.

### `CompileResult`

Outcome of one compile invocation. Sealed so consumers can pattern-match without a `success: Boolean` second-class field.

```kotlin
sealed class CompileResult {
    abstract val unit: CompileUnit
    abstract val diagnostics: List<CompileDiagnostic>  // may include warnings even on success

    data class Success(
        override val unit: CompileUnit,
        override val diagnostics: List<CompileDiagnostic>,
        val classOutputDir: String,  // absolute path to the temp dir holding the produced .class files
        val loadedDefinitionsByName: Map<String, String>
        // node name → fully-qualified class name; the keys define which CodeNodes are now installable
    ) : CompileResult()

    data class Failure(
        override val unit: CompileUnit,
        override val diagnostics: List<CompileDiagnostic>
        // diagnostics is non-empty by construction — at least one ERROR severity entry must exist
    ) : CompileResult()
}
```

Placement: `commonMain` of `flowGraph-inspect`.

**Invariants**:

- `Success.loadedDefinitionsByName` must be non-empty for `SingleFile` units; for `Module` units it must list every CodeNode source the module emitted.
- `Failure.diagnostics.any { it.severity == ERROR }` is true.

### `RecompileResult`

Aggregate result surfaced to the GraphEditor UI. Wraps `CompileResult` plus session-level metadata.

```kotlin
data class RecompileResult(
    val compileResult: CompileResult,
    val durationMs: Long,
    val pipelinesQuiesced: Int,    // count of running pipelines stopped before this compile (FR-014)
    val warningSummary: String? = null
) {
    val success: Boolean get() = compileResult is CompileResult.Success
    val unit: CompileUnit get() = compileResult.unit
}
```

Placement: `commonMain` of `flowGraph-inspect` (no JVM-only types).

---

## 3. Session-state entities

### `ClassloaderScope`

Per-recompile-unit child-first `URLClassLoader` wrapper.

```kotlin
class ClassloaderScope(
    val unit: CompileUnit,
    val classOutputDir: File,         // jvmMain: java.io.File
    parent: ClassLoader
) : AutoCloseable {
    private val loader: URLClassLoader = ChildFirstURLClassLoader(
        urls = arrayOf(classOutputDir.toURI().toURL()),
        parent = parent,
        ownedPackages = unit.sources.map { it.derivePackagePrefix() }.toSet()
    )

    /** Loads a CodeNodeDefinition by FQCN through this loader; returns null if class is missing. */
    fun loadDefinition(fqcn: String): CodeNodeDefinition?

    /** Releases JAR file handles. Does NOT force GC; that's the registry's responsibility. */
    override fun close()
}
```

Placement: `jvmMain` of `flowGraph-inspect`.

**Lifecycle**:

1. `Created`: when `RecompileSession` launches a successful compile.
2. `Active`: registered as the canonical loader for `(tier, hostModuleName, codeNodeName)` triples in the registry.
3. `Superseded`: a newer `ClassloaderScope` covering the same triples is registered. The old scope's strong reference in the registry is replaced with the new one; subsequent `close()` releases JAR file handles.
4. `Eligible for GC`: once no in-flight pipeline coroutine references its classes (via in-memory `CodeNodeDefinition` instances or runtime objects).

### `RecompileSession`

The session-scoped coordinator. One instance lives in `graphEditor/jvmMain`; created at GraphEditor startup; disposed at GraphEditor shutdown.

```kotlin
class RecompileSession(
    private val compiler: InProcessCompiler,
    private val registry: NodeDefinitionRegistry,
    private val pipelineQuiescer: PipelineQuiescer,
    private val publisher: RecompileFeedbackPublisher,
    private val sessionCacheDir: File   // ~/.codenode/cache/sessions/{uuid}/
) {
    /** Suspending — returns when the compile has finished and the registry is updated. */
    suspend fun recompile(unit: CompileUnit): RecompileResult

    /** Convenience for FR-001's auto-fire path. */
    suspend fun recompileGenerated(file: File, tier: PlacementLevel, hostModule: String?): RecompileResult

    /** Idempotent — safe to call multiple times. */
    fun shutdown()
}
```

Placement: `jvmMain` of `graphEditor`.

**Concurrency invariant** (Decision 7): exactly one compile in flight per session; subsequent `recompile(...)` calls suspend on a `Mutex`.

### `NodeDefinitionRegistry` (modified)

The existing registry gains a session-aware lookup with FR-017 precedence. New methods marked **NEW**.

```kotlin
class NodeDefinitionRegistry {

    // Existing methods retained — see flowGraph-inspect/jvmMain/.../registry/NodeDefinitionRegistry.kt

    /** NEW: install a definition produced by a per-file or per-module recompile. */
    fun installSessionDefinition(
        scope: ClassloaderScope,
        nodeName: String,
        definition: CodeNodeDefinition
    )

    /** NEW: revert a session-installed definition (e.g., after a failed recompile that
     *  invalidates a previous session install). Falls back to the launch-time class if any. */
    fun revertSessionDefinition(nodeName: String)

    /** MODIFIED: getByName now consults session installs first, then the launch-time scan. */
    override fun getByName(name: String): CodeNodeDefinition?
}
```

The registry holds at most one session install per `(tier, hostModule, nodeName)` triple. Replacing it drops the prior `ClassloaderScope` reference (Decision 6). `revertSessionDefinition` is for the failure-recovery case in FR-013 — actually, on closer read, FR-013 says the prior version remains executable on a failed recompile. So `revertSessionDefinition` is invoked only when the user explicitly wants to drop a session install (e.g., a future "Reset module" action). For now it's defined but unused outside tests.

---

## 4. Compile-cache entity

### `SessionCompileCache`

Owns the on-disk directory holding per-unit `.class` outputs for the current GraphEditor session.

```kotlin
class SessionCompileCache(
    val rootDir: File   // ~/.codenode/cache/sessions/{uuid}/
) {
    /** Allocates a fresh subdirectory for one CompileUnit's output. Prior subdirs for the
     *  same unit are not deleted automatically — `RecompileSession` deletes them after the
     *  registry has switched to the new ClassloaderScope (Decision 6). */
    fun allocate(unit: CompileUnit): File

    /** Best-effort cleanup of the entire session root. Invoked at shutdown. */
    fun deleteAll()
}
```

Placement: `jvmMain` of `flowGraph-inspect`.

The cache directory is OS-temp-equivalent (`~/.codenode/cache/sessions/{uuid}/` on Unix; `%LOCALAPPDATA%\codenode\cache\sessions\{uuid}\` on Windows). Per-session UUID prevents collisions if the user runs multiple GraphEditor instances. Best-effort cleanup at shutdown — leftover dirs from crashes are pruned by a startup-time sweep that deletes any `sessions/{uuid}/` directory whose parent process is no longer alive (heuristic: dir mtime older than 24h).

---

## 5. Existing entities consumed (no change needed beyond clearly-noted modifications)

| Entity | Module | Role in this feature |
|---|---|---|
| `PlacementLevel` | `flowGraph-types/commonMain` | Tier identification for `CompileSource.tier` and `CompileUnit.Module.tier`. |
| `CodeNodeDefinition` | `fbpDsl/commonMain` | The artifact produced by `Success.loadedDefinitionsByName` resolution and consumed by Runtime Preview's `DynamicPipelineBuilder`. |
| `ErrorConsoleEntry` | `graphEditor/jvmMain` (feature 084) | Target for `RecompileFeedbackPublisher`'s output. One `CompileDiagnostic` → one `ErrorConsoleEntry`. |
| `DynamicPipelineBuilder` | `flowGraph-execute/commonMain` | Pipeline-build path that calls `NodeDefinitionRegistry.lookup`. No changes to this class — the registry's resolution-precedence change (FR-017) is transparent to callers. |
| `ModuleSessionFactory` | `flowGraph-execute/jvmMain` | Modified to consult `NodeDefinitionRegistry`'s session installs (one-line precedence change in its lookup). |
| `NodeGeneratorViewModel` | `graphEditor/jvmMain` | Modified to fire `RecompileSession.recompileGenerated(...)` after generator output writes to disk (FR-001). |

---

## 6. State diagram: single recompile invocation

```text
                  recompile(unit)
                        │
                        ▼
              ┌─────────────────────┐
              │  await Mutex (1@a   │
              │  time, Decision 7)  │
              └─────────┬───────────┘
                        ▼
              ┌─────────────────────┐
              │ pipelineQuiescer    │
              │ .stopAll()          │  → records pipelinesQuiesced count
              └─────────┬───────────┘
                        ▼
              ┌─────────────────────┐
              │ compiler.compile    │  ← K2JVMCompiler.exec(...)
              │  (unit, classpath)  │
              └─────────┬───────────┘
                        │
            ┌───────────┴───────────┐
       Success                   Failure
            │                       │
            ▼                       ▼
   ┌────────────────┐      ┌────────────────┐
   │ Build new      │      │ Surface diag's │
   │ ClassloaderSc. │      │ to publisher   │
   │ + load defs    │      │ (no registry   │
   │ + install in   │      │  change; FR-13)│
   │ registry       │      └────────┬───────┘
   └────────┬───────┘               │
            ▼                       │
   ┌────────────────┐               │
   │ Surface diag's │               │
   │ + summary to   │               │
   │ publisher      │               │
   └────────┬───────┘               │
            └───────────┬───────────┘
                        ▼
                  return
                  RecompileResult
```

---

## 7. Consistency checks

| Constraint | Where enforced |
|---|---|
| FR-001: per-file compile fires automatically on Node Generation | `NodeGeneratorViewModel.generate()` calls `RecompileSession.recompileGenerated(...)` after the file write. UI-FBP and Entity Module generators do NOT call this path — they rely on per-module manual recompile (FR-004). |
| FR-002: per-file compile produces a usable placeholder definition | `CompileResult.Success.loadedDefinitionsByName` is non-empty AND each value resolves to a `CodeNodeDefinition` instance (verified at install time). |
| FR-005: per-module recompile updates Runtime Preview behavior | `NodeDefinitionRegistry.getByName` consults session installs first (FR-017). `DynamicPipelineBuilder` uses `getByName`. |
| FR-006: per-module recompile is exclusively user-invoked | No automatic trigger in `RecompileSession.recompile(Module(...))`. The only auto-call path is `recompileGenerated(...)` which produces a `SingleFile` unit. |
| FR-010 / FR-011: tier semantics | `CompileUnit.Module.tier` is required; the registry key includes tier. |
| FR-013: failed recompile keeps prior version executable | `installSessionDefinition` is called only on `CompileResult.Success`; on `Failure`, the registry is not touched. |
| FR-014: running-pipeline conflict | `PipelineQuiescer.stopAll()` runs before every compile; result records the count. |
| FR-015: bounded memory | `ClassloaderScope.close()` + registry replacement + soak-test contract. |
| FR-017: resolution precedence | `NodeDefinitionRegistry.getByName` body: session install → launch-time scan → null. |

All FRs from the spec are satisfied by entities defined in §1–§5; no orphan FRs.

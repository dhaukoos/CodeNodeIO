# Contract: `RecompileSession`

**Module**: `graphEditor/jvmMain` · **Source-of-truth path** (when implemented): `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/compile/RecompileSession.kt`

## Surface

```kotlin
package io.codenode.grapheditor.compile

class RecompileSession(
    private val compiler: InProcessCompiler,
    private val registry: NodeDefinitionRegistry,
    private val pipelineQuiescer: PipelineQuiescer,
    private val publisher: RecompileFeedbackPublisher,
    private val sessionCacheDir: File
) {
    /**
     * Compiles [unit] and, on success, installs the resulting CodeNodeDefinitions into
     * the registry. Returns a structured RecompileResult with timing, diagnostics, and
     * the count of pipelines that were stopped before the compile (FR-014).
     *
     * Concurrency: serialized — at most one in-flight compile per session.
     * Failure recovery: on CompileResult.Failure, the registry is NOT touched (FR-013).
     */
    suspend fun recompile(unit: CompileUnit): RecompileResult

    /**
     * Convenience for the FR-001 auto-fire path. Wraps [file] in a SingleFile unit and
     * delegates to [recompile]. Used by NodeGeneratorViewModel after each generator emits.
     */
    suspend fun recompileGenerated(
        file: File,
        tier: PlacementLevel,
        hostModule: String?
    ): RecompileResult

    /** Idempotent shutdown — stops in-flight compiles, closes classloaders, deletes cache. */
    fun shutdown()
}
```

## Behavior — `recompile(unit)`

1. Acquire the session-scoped `Mutex`. (Decision 7: serialize compiles.)
2. Stop any running pipeline via `pipelineQuiescer.stopAll()`. Record the count.
3. Invoke `compiler.compile(unit)`. Wall-clock duration measured.
4. **On `CompileResult.Success`**:
   a. Construct a fresh `ClassloaderScope` parented to the launch-time classloader.
   b. For each `(nodeName, fqcn)` in `loadedDefinitionsByName`: load via `scope.loadDefinition(fqcn)`. If null, surface a synthetic diagnostic and downgrade to `Failure` (defensive — should not happen if compile succeeded).
   c. For each loaded definition: `registry.installSessionDefinition(scope, nodeName, definition)`. The registry replaces any prior install for the same `(tier, hostModule, nodeName)` triple.
   d. Build `RecompileResult.success`.
5. **On `CompileResult.Failure`**: build `RecompileResult` with `success=false`. Do NOT touch the registry. The prior version of the node remains installed.
6. Publish via `publisher.publish(recompileResult)` so the UI surfaces feedback.
7. Release the mutex.
8. Return the `RecompileResult`.

## Behavior — `recompileGenerated(file, tier, hostModule)`

Equivalent to `recompile(CompileUnit.SingleFile(CompileSource(file.absolutePath, tier, hostModule)))`.

## Behavior — `shutdown()`

1. Cancel any in-flight `recompile(...)` coroutine.
2. Iterate every active `ClassloaderScope` registered in the registry's session installs; call `close()` on each (best-effort).
3. Delete `sessionCacheDir` recursively (best-effort; failures are logged, not raised).

## Invariants

- **No registry corruption on failure**: `installSessionDefinition(...)` is called only on Success path. Any partial-success scenario inside step 4 reverts via "downgrade to Failure" (no installs are made).
- **Pipelines must be quiesced before any class replacement**: ordering in step 2 → step 4 is non-negotiable. A class replacement while a pipeline holds runtime references can produce undefined behavior.
- **Single source of truth for feedback**: `publisher.publish(...)` is the only outward signal; no other component publishes RecompileResult-derived events.

## Test contract (`RecompileSessionIntegrationTest.kt`)

| ID | Description | Expected outcome |
|---|---|---|
| `generate-and-use-single-file` | Generate a Module-tier CodeNode source via fixture; `recompileGenerated(...)`; `registry.getByName(...)` returns the new definition. | `RecompileResult.success == true`; `getByName` returns a non-null definition; the definition's `createRuntime()` works. |
| `module-recompile-supersedes-prior-install` | Install file A via per-file path (returns "Calc" Definition v1), then recompile module containing A (returns "Calc" Definition v2). | After step 2, `getByName("Calc")` returns the module-recompile's instance, not the per-file one. |
| `failure-leaves-prior-install-intact` | Install a working version, then recompile a broken edit. | `RecompileResult.success == false`; `getByName(...)` still returns the prior working definition. |
| `running-pipeline-is-stopped-before-compile` | Start a pipeline that uses node X; invoke `recompile(...)` against X's module. | `RecompileResult.pipelinesQuiesced == 1`; pipeline is in `IDLE` state after recompile completes. |
| `serial-mutex-blocks-concurrent-recompile` | Launch two `recompile(...)` calls concurrently against different units. | Both complete successfully; the timing telemetry shows the second strictly started after the first ended. |
| `shutdown-deletes-cache-and-closes-loaders` | Run several recompiles, then `shutdown()`. | Cache directory does not exist on disk; `WeakReference`-tracked classloaders are eligible for GC. |
| `feedback-published-on-every-attempt` | One Success and one Failure recompile. | `publisher.publish(...)` is called exactly twice; each call carries the corresponding `RecompileResult`. |

## Performance contract

- p90 end-to-end for `SingleFile`: ≤ 1.5s after warmup (compile ≤ 1s + classloader/registry ops ≤ 0.5s) — drives SC-001.
- p90 end-to-end for `Module` (≤ 10 files): ≤ 6.0s after warmup — drives SC-002.

## Dependencies

- `InProcessCompiler` (per its own contract)
- `NodeDefinitionRegistry` (modified per [node-definition-registry-v2.md](./node-definition-registry-v2.md))
- `PipelineQuiescer` (jvmMain; encapsulates `RuntimePreviewPanel` interactions)
- `RecompileFeedbackPublisher` (jvmMain; bridges to `ErrorConsolePanel`)

# Contract: `InProcessCompiler`

**Module**: `flowGraph-inspect/jvmMain` · **Source-of-truth path** (when implemented): `flowGraph-inspect/src/jvmMain/kotlin/io/codenode/flowgraphinspect/compile/InProcessCompiler.kt`

## Surface

```kotlin
package io.codenode.flowgraphinspect.compile

class InProcessCompiler(
    private val classpathSnapshot: ClasspathSnapshot,
    private val cache: SessionCompileCache
) {
    /**
     * Compiles every source in [unit] to a fresh output directory under [cache].
     * Returns a structured CompileResult — never throws on compilation failure;
     * exceptions reach the caller only for I/O errors (disk full, permission denied).
     *
     * Concurrency: not internally synchronized. RecompileSession serializes calls.
     * Performance: target ≤ 1.0s p90 for SingleFile units; ≤ 5.0s p90 for Module units
     * with ≤ 10 files. Heavier modules SHOULD warn and proceed.
     */
    suspend fun compile(unit: CompileUnit): CompileResult
}
```

## Inputs

- **`unit`**: a `CompileUnit` (either `SingleFile` or `Module`) describing the source set to compile.
- **`classpathSnapshot`**: read once at GraphEditor startup (Decision 4); contains the launch-time classpath as a list of absolute file paths. Reused for every compile.
- **`cache`**: provides per-unit output directories.

## Outputs

A `CompileResult` (`Success` or `Failure`) per `data-model.md` §2.

- **On Success**: `classOutputDir` is a fresh subdirectory of `cache.rootDir` containing the produced `.class` files; `loadedDefinitionsByName` lists every CodeNodeDefinition's node-name → FQCN.
- **On Failure**: `diagnostics` contains at least one ERROR-severity entry with file/line information.

## Behavior

1. Allocate output dir: `val outputDir = cache.allocate(unit)`.
2. Build `K2JVMCompilerArguments`:
   - `freeArgs = unit.sources.map { it.absolutePath }`
   - `destination = outputDir.absolutePath`
   - `classpath = classpathSnapshot.asPathString(separator = File.pathSeparator)`
   - `noStdlib = true` (stdlib already on classpath)
   - `jvmTarget = "17"` (matches project standard)
3. Invoke `K2JVMCompiler().exec(messageCollector, Services.EMPTY, arguments)`.
4. Capture every `messageCollector.report(...)` callback into `CompileDiagnostic` entries.
5. If `exitCode == ExitCode.OK`:
   a. Walk `outputDir` for `.class` files.
   b. For each file, parse the FQCN from its package path.
   c. Identify which FQCNs implement `CodeNodeDefinition` (heuristic: ends with `CodeNode` or carries a known suffix; final correctness verified at registry-install time when `Class.forName + getField("INSTANCE")` succeeds).
   d. Build `loadedDefinitionsByName` — keyed on the `name` property of each successfully-loaded definition.
   e. Return `CompileResult.Success`.
6. Else: return `CompileResult.Failure(unit, diagnostics)`.

## Invariants

- **No silent error swallowing**: every diagnostic from `MessageCollector` must reach `CompileResult.diagnostics`.
- **No partial-success `Success`**: if ANY file in a `Module` unit fails to compile, the result is `Failure`. The unit is atomic (FR-007's intra-module cross-reference guarantee).
- **No registry side effects**: `compile(...)` does NOT touch `NodeDefinitionRegistry`. That's `RecompileSession`'s job.

## Test contract (`InProcessCompilerTest.kt`)

| ID | Description | Expected outcome |
|---|---|---|
| `single-valid-source` | Compile one fixture `Calc2CodeNode.kt` that returns `Double`. | `Success`; `loadedDefinitionsByName["Calc2"]` exists; output dir contains `Calc2CodeNode.class`. |
| `single-broken-source` | Compile a source with a syntax error (`override val name = "Bad"` missing equals sign). | `Failure`; `diagnostics` contains at least one `ERROR` with `filePath` set and `line > 0`. |
| `module-with-cross-reference` | Module with two files: `HelperType.kt` and `UserCodeNode.kt` (the latter imports the former). | `Success`; both classes present in output dir; `loadedDefinitionsByName["UserCodeNode"]` exists. |
| `module-where-one-file-fails` | Module with one valid file and one with a syntax error. | `Failure` (atomic); diagnostics names the broken file. |
| `module-empty-classpath` | Compile against an empty classpath snapshot. | `Failure`; diagnostic mentions missing `kotlin.Any` or similar core type. |
| `compile-twice-same-unit-different-content` | Compile a single file, modify it on disk, compile again. | Both invocations succeed; second invocation's output dir is a different directory; no leftover from first invocation in second's dir. |
| `large-source-warmup-cost` | First-ever invocation of `K2JVMCompiler` in the test JVM. | Succeeds; records duration; subsequent invocations are at least 5× faster (warmup amortization). |

## Performance contract

- p90 for `SingleFile` after warmup: ≤ 1.0s on a developer-class workstation. Verified via `:flowGraph-inspect:check` benchmark.
- p90 for `Module` with 10 files after warmup: ≤ 5.0s.
- First-invocation latency: ≤ 6s (one-time JVM warmup cost).

## Dependencies

- `kotlin-compiler-embeddable` 2.1.21 (Apache 2.0; already declared in `flowGraph-generate`).
- Project: `CompileUnit`, `CompileResult`, `CompileDiagnostic` (commonMain).
- Project: `ClasspathSnapshot`, `SessionCompileCache` (jvmMain, this contract is the only consumer).

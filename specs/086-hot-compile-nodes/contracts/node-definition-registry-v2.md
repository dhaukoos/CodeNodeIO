# Contract: `NodeDefinitionRegistry` (v2 — session-aware resolution)

**Module**: `flowGraph-inspect/jvmMain` · **Source-of-truth path** (when implemented): `flowGraph-inspect/src/jvmMain/kotlin/io/codenode/flowgraphinspect/registry/NodeDefinitionRegistry.kt`

This contract documents the **modifications** to the existing `NodeDefinitionRegistry` introduced by feature 086. Pre-existing methods (`discoverAll`, `scanDirectory`, `getAllForPalette`, `register`, `registerTemplate`, `nameExists`, `checkPortCompatibility`, etc.) retain their current behavior. The change is the resolution-precedence chain inside `getByName(name)` (FR-017) and two new install/revert methods.

## New surface

```kotlin
class NodeDefinitionRegistry {

    // --- existing fields and methods retained ---

    /**
     * NEW — installs a CodeNodeDefinition produced by an in-session compile,
     * superseding any prior session install or launch-time-classpath entry for
     * the same nodeName.
     *
     * Replaces the prior session install's strong reference to its ClassloaderScope
     * (Decision 6 / SC-004). Old scopes become GC-eligible on the next collection
     * cycle, modulo any in-flight pipeline coroutines holding runtime references.
     */
    fun installSessionDefinition(
        scope: ClassloaderScope,
        nodeName: String,
        definition: CodeNodeDefinition
    )

    /**
     * NEW — drops a session install. Subsequent getByName(nodeName) falls back to
     * the launch-time classpath entry if any. No-op if no session install exists.
     *
     * Reserved for future "Reset module" UX; not currently invoked by US1/US2/US3
     * implementation paths.
     */
    fun revertSessionDefinition(nodeName: String)

    // --- modified method ---

    /**
     * MODIFIED — resolution precedence (FR-017):
     *   1. Most-recent session install (if any)
     *   2. Launch-time classpath compiledNodes map (existing behavior)
     *   3. null
     */
    fun getByName(name: String): CodeNodeDefinition?
}
```

## State

The registry holds two parallel maps:

```kotlin
private val launchTimeNodes: MutableMap<String, CodeNodeDefinition> = mutableMapOf()  // existing — populated by discoverAll/scanDirectory
private val sessionInstalls: MutableMap<String, SessionInstall> = mutableMapOf()       // NEW

private data class SessionInstall(
    val scope: ClassloaderScope,
    val definition: CodeNodeDefinition,
    val installedAtMs: Long
)
```

Resolution: `sessionInstalls[name]?.definition ?: launchTimeNodes[name]`.

## Behavior — `installSessionDefinition`

1. Look up the existing `SessionInstall` for `nodeName`. If present, the existing entry is REPLACED (its `ClassloaderScope` reference is dropped — see Decision 6).
2. Create a new `SessionInstall(scope, definition, System.currentTimeMillis())`.
3. Store under `sessionInstalls[nodeName]`.
4. Emit no events. (Callers — `RecompileSession` — are responsible for surfacing user feedback via the `RecompileFeedbackPublisher`.)

## Behavior — `revertSessionDefinition`

1. Remove `sessionInstalls[nodeName]` if present. The strong reference to its `ClassloaderScope` is dropped at the same instant.
2. No-op if not present.

## Behavior — `getByName` (modified)

```kotlin
override fun getByName(name: String): CodeNodeDefinition? {
    sessionInstalls[name]?.let { return it.definition }
    return launchTimeNodes[name]
}
```

This change is the single "FR-017 implementation" — by routing every consumer through `getByName`, the in-session-first ordering propagates uniformly to `DynamicPipelineBuilder.canBuildDynamic`, `DynamicPipelineBuilder.validate`, and `DynamicPipelineBuilder.build`.

## Backward compatibility

- Existing tests that call `getByName` against a registry whose `sessionInstalls` is empty continue to pass unchanged — the lookup degenerates to the existing launch-time-only behavior.
- No existing public method's signature changes. `installSessionDefinition` and `revertSessionDefinition` are additive.
- Existing `register(node: CodeNodeDefinition)` (the `compiledNodes[node.name] = node` path used by `discoverCompiledNodes` + `scanDirectory`) writes to `launchTimeNodes` — semantics unchanged.

## Test contract (`NodeDefinitionRegistryV2Test.kt`)

| ID | Description | Expected outcome |
|---|---|---|
| `getByName-falls-back-to-launchtime-when-no-session-install` | A node X is on the launch-time classpath; no session install. | `getByName("X")` returns the launch-time instance. |
| `getByName-prefers-session-install-over-launchtime` | Same node X exists at launch and a session install replaces it. | `getByName("X")` returns the session instance (verified by class identity — `it::class.java.classLoader` is the session loader, not `AppClassLoader`). |
| `revertSessionDefinition-falls-back-to-launchtime` | Install a session version, then revert. | `getByName("X")` returns the launch-time instance after revert. |
| `installSessionDefinition-twice-replaces-prior-strong-reference` | Install v1, then v2; verify v1's `ClassloaderScope` is no longer reachable from the registry. | A `WeakReference` to v1's scope is collected after a triggered GC. |
| `getByName-returns-null-for-unknown-name` | No node X exists in either map. | Returns `null`. |
| `getByName-of-template-only-node-returns-null` | Node X registered as a template (NodeTemplateMeta) but never compile-loaded. | `getByName("X")` returns null (templates are palette-only; FR-017 is for executable definitions). |

## Memory invariant

After `N` recompiles of the same node X:

- `sessionInstalls["X"]` holds exactly ONE `SessionInstall` (the most recent).
- The registry holds zero strong references to any prior `ClassloaderScope`.
- The N-1 prior scopes are GC-eligible modulo any pipeline coroutine that still holds a `CodeNodeDefinition` reference (which is bounded by the lifetime of that pipeline's `CoroutineScope`).

This invariant is checked by `RecompileSessionIntegrationTest.shutdown-deletes-cache-and-closes-loaders` and a dedicated `NodeDefinitionRegistryV2Test.installSessionDefinition-twice-replaces-prior-strong-reference`.

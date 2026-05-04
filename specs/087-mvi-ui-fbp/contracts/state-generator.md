# Contract: UIFBPStateGenerator (MVI shape, Design B)

**Implements**: FR-001, FR-009, FR-012, SC-001, SC-005

## Public API

```kotlin
class UIFBPStateGenerator {
    /**
     * Emits the public immutable `data class {Name}State` — UI-facing snapshot.
     * One `val` per spec.sinkInputs entry.
     *
     * Caller (UIFBPInterfaceGenerator) writes to:
     *   {basePackage}/viewmodel/{Name}State.kt
     *
     * NOTE (Design B, 2026-05-03): no companion StateStore is emitted.
     * Per-flow-graph runtime state lives in the {Name}Runtime factory's
     * closure; the State data class is the genuine SSOT for the UI.
     */
    fun generate(spec: UIFBPSpec): String
}
```

## Behavioral contract

**Returns**: a Kotlin file body of the form:

```kotlin
/* license header */
package {spec.packageName}.viewmodel

/* IP-type imports from spec.ipTypeImports */

data class {spec.flowGraphPrefix}State(
    val {sinkInput[0].name}: {kotlinType[0]} = {default[0]},
    val {sinkInput[1].name}: {kotlinType[1]} = {default[1]},
    // … one line per spec.sinkInputs entry
)
```

**Property order**: matches `spec.sinkInputs.indices` (deterministic).

**Default values**: derived per port:

| Declared type     | Default       |
|-------------------|---------------|
| `Int`             | `0`           |
| `Long`            | `0L`          |
| `Double`          | `0.0`         |
| `Float`           | `0.0f`        |
| `Boolean`         | `false`       |
| `String`          | `""`          |
| `Unit`            | `Unit`        |
| nullable / other  | `null`        |

**Edge case — empty `sinkInputs`**: emit `data class {Name}State()` (zero-arg
data class).

## Test contract (RED phase)

Tests live in
`flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPStateGeneratorTest.kt`.

Required test cases:

1. `generate emits a data class with one val per sinkInput in declared order`
2. `generate uses correct defaults per primitive type`
3. `generate uses null default for nullable IP types`
4. `generate on zero sinkInputs emits a zero-arg data class`
5. `generate package matches spec.packageName + .viewmodel`
6. `generate class name uses flowGraphPrefix not composableName`
7. `generate emits IP-type imports only when at least one sinkInput needs them`
8. `generate does NOT emit any StateStore companion (Design B)`
9. `generate does NOT emit MutableStateFlow / asStateFlow / reset (those moved to {Name}Runtime)`
10. `generate handles a non-nullable port carrying a non-primitive IP type by emitting the property as nullable with a null default (per spec Edge Case: "State property whose default cannot be derived")`
11. `generate output is byte-identical across two consecutive calls (determinism)`

Each test uses fixture-string comparison.

# Contract: UIFBPEventGenerator (NEW)

**Implements**: FR-002, FR-008, FR-012, SC-001, SC-005

## Public API

```kotlin
class UIFBPEventGenerator {
    /**
     * Emits the public sealed interface {Name}Event with one case per
     * spec.sourceOutputs entry.
     *
     * Caller (UIFBPInterfaceGenerator) writes to:
     *   {basePackage}/viewmodel/{Name}Event.kt
     */
    fun generate(spec: UIFBPSpec): String
}
```

## Behavioral contract

**Returns**: a Kotlin file body of the form:

```kotlin
/* license header */
package {spec.packageName}.viewmodel

/* IP-type imports from spec.ipTypeImports — only for non-Unit source-port types */

sealed interface {spec.flowGraphPrefix}Event {
    // For each spec.sourceOutputs entry:
    //   if port.typeName == "Unit":
    data object {PortName} : {spec.flowGraphPrefix}Event
    //   else:
    data class Update{PortName}(val value: {kotlinType}) : {spec.flowGraphPrefix}Event
}
```

### Naming convention

- **`PortName`** is `port.name` PascalCased (first letter uppercased; the
  generator does NOT split underscores or camelCase boundaries — the input
  port name is used as-is, only the first letter is uppercased).
- **`Update`** prefix only for valued ports. `Unit`-typed ports emit a
  `data object` named `{PortName}` directly (no `Update` prefix; the verb
  is implicit in the port name itself).

### Edge case — empty `sourceOutputs`

Emit `sealed interface {spec.flowGraphPrefix}Event` with NO body and NO
nested cases. The compiler accepts a sealed interface with zero subtypes;
it's uninhabitable so the ViewModel's `onEvent` `when` expression is
trivially exhaustive (no branches).

### `kotlinType` rule

For each `PortInfo`:
```kotlin
val kotlinType = if (port.isNullable) "${port.typeName}?" else port.typeName
```

## Test contract (RED phase)

Tests live in
`flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPEventGeneratorTest.kt`.

Required test cases:

1. `generate emits sealed interface with one case per sourceOutput`
2. `generate uses Update{PortName} for valued ports`
3. `generate uses data object {PortName} for Unit-typed ports`
4. `generate on zero sourceOutputs emits an empty sealed interface (no body)`
5. `generate uses correct package + flowGraphPrefix-derived class name`
6. `generate emits IP-type imports only when at least one valued port references one`
7. `generate output is byte-identical across two consecutive calls (determinism)`
8. `generate handles nullable source-output port types correctly`

Each test uses fixture-string comparison (full file body matching).

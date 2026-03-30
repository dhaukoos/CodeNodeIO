# Contract: Generated CodeNodeDefinition Files

**Feature**: 061-codenode-definition-codegen

## EntityCUDCodeNodeGenerator

### generate(spec: EntityModuleSpec): String

Generates a `{Entity}CUDCodeNode.kt` file content.

**Generated file structure**:
```kotlin
object {Entity}CUDCodeNode : CodeNodeDefinition {
    override val name = "{Entity}CUD"
    override val category = CodeNodeType.SOURCE
    override val inputPorts = emptyList<PortSpec>()
    override val outputPorts = listOf(
        PortSpec("save", {Entity}::class),
        PortSpec("update", {Entity}::class),
        PortSpec("remove", {Entity}::class)
    )

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createSourceOut3<{Entity}, {Entity}, {Entity}>(
            name = name,
            generate = { emit ->
                coroutineScope {
                    launch { {PluralName}State._save.drop(1).collect { ... emit ProcessResult3 ... } }
                    launch { {PluralName}State._update.drop(1).collect { ... } }
                    launch { {PluralName}State._remove.drop(1).collect { ... } }
                }
            }
        )
    }
}
```

## EntityRepositoryCodeNodeGenerator

### generate(spec: EntityModuleSpec): String

Generates a `{Entity}RepositoryCodeNode.kt` file content.

**Generated file structure**:
```kotlin
object {Entity}RepositoryCodeNode : CodeNodeDefinition {
    override val name = "{Entity}Repository"
    override val category = CodeNodeType.TRANSFORMER
    override val inputPorts = listOf(
        PortSpec("save", {Entity}::class),
        PortSpec("update", {Entity}::class),
        PortSpec("remove", {Entity}::class)
    )
    override val outputPorts = listOf(
        PortSpec("result", String::class),
        PortSpec("error", String::class)
    )
    override val anyInput = true

    override fun createRuntime(name: String): NodeRuntime {
        var lastSaveRef: {Entity}? = null
        var lastUpdateRef: {Entity}? = null
        var lastRemoveRef: {Entity}? = null

        return CodeNodeFactory.createIn3AnyOut2Processor<{Entity}, {Entity}, {Entity}, String, String>(
            name = name,
            initialValue1 = {Entity}(...defaults...),
            initialValue2 = {Entity}(...defaults...),
            initialValue3 = {Entity}(...defaults...),
            process = { save, update, remove ->
                try {
                    val repo = {Entity}Repository({PluralName}Persistence.dao)
                    // Identity tracking + DAO calls with toEntity() conversion
                    // Returns ProcessResult2.first("Saved: ...") or .second("Error: ...")
                } catch (e: Exception) {
                    ProcessResult2.second("Error: ${e.message}")
                }
            }
        )
    }
}
```

## EntityDisplayCodeNodeGenerator

### generate(spec: EntityModuleSpec): String

Generates a `{PluralName}DisplayCodeNode.kt` file content.

**Generated file structure**:
```kotlin
object {PluralName}DisplayCodeNode : CodeNodeDefinition {
    override val name = "{PluralName}Display"
    override val category = CodeNodeType.SINK
    override val inputPorts = listOf(
        PortSpec("result", String::class),
        PortSpec("error", String::class)
    )
    override val outputPorts = emptyList<PortSpec>()

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createSinkIn2<String, String>(
            name = name,
            consume = { result, error ->
                {PluralName}State._result.value = result
                {PluralName}State._error.value = error
            }
        )
    }
}
```

## EntityFlowGraphBuilder Changes

All three nodes must include these configuration entries:

```kotlin
config("_codeNodeClass", "io.codenode.{module}.nodes.{NodeName}CodeNode")
config("_genericType", "{inXoutY}")
```

Note: `_codeNodeDefinition` is no longer needed. Legacy tick-function support has been removed from RuntimeFlowGenerator — all nodes are expected to be CodeNodeDefinition objects.

## RuntimeFlowGenerator Simplification

The legacy dual-path logic (`codeNodeClassNodes` vs `legacyNodes`) is removed. All nodes must have `_codeNodeClass`. The generator unconditionally:
- Imports each node's CodeNodeDefinition object by FQCN
- Instantiates via `{NodeName}CodeNode.createRuntime("{NodeName}")`
- No tick function imports or factory function calls are generated

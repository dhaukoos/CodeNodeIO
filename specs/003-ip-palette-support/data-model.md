# Data Model: InformationPacket Palette Support

**Feature**: 003-ip-palette-support
**Date**: 2026-01-30

## Entity Definitions

### IPColor

Represents an RGB color for visual identification of IP types.

```kotlin
@Serializable
data class IPColor(
    val red: Int = 0,
    val green: Int = 0,
    val blue: Int = 0
) {
    init {
        require(red in 0..255) { "Red must be 0-255, got $red" }
        require(green in 0..255) { "Green must be 0-255, got $green" }
        require(blue in 0..255) { "Blue must be 0-255, got $blue" }
    }

    companion object {
        val BLACK = IPColor(0, 0, 0)
        val WHITE = IPColor(255, 255, 255)
    }
}
```

**Location**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/IPColor.kt`

**Validation Rules**:
- Each component must be in range 0-255
- Defaults to black (0, 0, 0)

---

### InformationPacketType

Defines a type of InformationPacket with associated metadata and visual properties.

```kotlin
@Serializable
data class InformationPacketType(
    val id: String,
    val typeName: String,
    val payloadType: KClass<*>,
    val color: IPColor = IPColor.BLACK,
    val description: String? = null
) {
    init {
        require(id.isNotBlank()) { "ID cannot be blank" }
        require(typeName.isNotBlank()) { "Type name cannot be blank" }
    }

    /**
     * Generates DSL code representation of this IP type
     */
    fun toCode(): String = buildString {
        appendLine("// InformationPacket Type: $typeName")
        appendLine("ipType(\"$typeName\") {")
        appendLine("    payloadType = ${payloadType.simpleName}::class")
        appendLine("    color = IPColor(${color.red}, ${color.green}, ${color.blue})")
        description?.let { appendLine("    description = \"$it\"") }
        appendLine("}")
    }
}
```

**Location**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/InformationPacketType.kt`

**Validation Rules**:
- ID must not be blank
- typeName must not be blank
- payloadType must be a valid KClass

---

### Connection (Modified)

Add optional IP type reference to existing Connection model.

```kotlin
@Serializable
data class Connection(
    val id: String,
    val sourceNodeId: String,
    val sourcePortId: String,
    val targetNodeId: String,
    val targetPortId: String,
    val channelCapacity: Int = 0,
    val parentScopeId: String? = null,
    val ipTypeId: String? = null  // NEW: Reference to InformationPacketType
) {
    // ... existing methods ...
}
```

**Location**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/Connection.kt`

**Backward Compatibility**: `ipTypeId` is nullable with null default, ensuring existing serialized graphs remain valid.

---

### IPTypeRegistry

Runtime registry of available IP types for the graphEditor.

```kotlin
class IPTypeRegistry {
    private val types = mutableMapOf<String, InformationPacketType>()

    fun register(type: InformationPacketType) {
        types[type.id] = type
    }

    fun getById(id: String): InformationPacketType? = types[id]

    fun getByTypeName(typeName: String): InformationPacketType? =
        types.values.find { it.typeName == typeName }

    fun getAllTypes(): List<InformationPacketType> = types.values.toList()

    fun search(query: String): List<InformationPacketType> =
        types.values.filter { it.typeName.contains(query, ignoreCase = true) }

    companion object {
        fun withDefaults(): IPTypeRegistry = IPTypeRegistry().apply {
            register(InformationPacketType(
                id = "ip_any",
                typeName = "Any",
                payloadType = Any::class,
                color = IPColor(0, 0, 0),
                description = "Universal type that accepts any payload"
            ))
            register(InformationPacketType(
                id = "ip_int",
                typeName = "Int",
                payloadType = Int::class,
                color = IPColor(33, 150, 243),
                description = "Integer numeric type"
            ))
            register(InformationPacketType(
                id = "ip_double",
                typeName = "Double",
                payloadType = Double::class,
                color = IPColor(156, 39, 176),
                description = "Floating-point numeric type"
            ))
            register(InformationPacketType(
                id = "ip_boolean",
                typeName = "Boolean",
                payloadType = Boolean::class,
                color = IPColor(76, 175, 80),
                description = "True/false boolean type"
            ))
            register(InformationPacketType(
                id = "ip_string",
                typeName = "String",
                payloadType = String::class,
                color = IPColor(255, 152, 0),
                description = "Text string type"
            ))
        }
    }
}
```

**Location**: `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/IPTypeRegistry.kt`

---

## State Changes

### GraphState (Modified)

Add connection selection and context menu state.

```kotlin
class GraphState(initialGraph: FlowGraph = ...) {
    // ... existing properties ...

    /**
     * Currently selected connection ID (for Properties Panel display)
     */
    var selectedConnectionId by mutableStateOf<String?>(null)
        private set

    /**
     * Context menu state for connection right-click
     */
    var connectionContextMenu by mutableStateOf<ConnectionContextMenuState?>(null)
        private set

    // ... existing methods ...

    fun selectConnection(connectionId: String?) {
        selectedConnectionId = connectionId
        selectedNodeId = null  // Deselect node when connection selected
    }

    fun showConnectionContextMenu(connectionId: String, position: Offset) {
        connectionContextMenu = ConnectionContextMenuState(connectionId, position)
    }

    fun hideConnectionContextMenu() {
        connectionContextMenu = null
    }

    fun updateConnectionIPType(connectionId: String, ipTypeId: String) {
        val connection = flowGraph.connections.find { it.id == connectionId } ?: return
        val updatedConnection = connection.copy(ipTypeId = ipTypeId)
        flowGraph = flowGraph.updateConnection(updatedConnection)
        isDirty = true
    }
}

data class ConnectionContextMenuState(
    val connectionId: String,
    val position: Offset
)
```

---

## Relationships

```
┌─────────────────────┐     ┌──────────────────────┐
│ InformationPacketType│     │     Connection       │
├─────────────────────┤     ├──────────────────────┤
│ id: String          │◄────│ ipTypeId: String?    │
│ typeName: String    │     │ sourceNodeId         │
│ payloadType: KClass │     │ sourcePortId         │
│ color: IPColor      │     │ targetNodeId         │
│ description: String?│     │ targetPortId         │
└─────────────────────┘     └──────────────────────┘
         │
         │ registered in
         ▼
┌─────────────────────┐
│   IPTypeRegistry    │
├─────────────────────┤
│ types: Map<id, Type>│
│ register()          │
│ getById()           │
│ search()            │
└─────────────────────┘
         │
         │ provides data to
         ▼
┌─────────────────────┐
│     IPPalette       │
├─────────────────────┤
│ displays types      │
│ search/filter       │
│ selection callback  │
└─────────────────────┘
```

## Serialization

### Flow Graph DSL Format

IP types are referenced by ID in connection definitions:

```kotlin
flowGraph("MyFlow", "1.0.0") {
    node("generator", "DataGenerator") { ... }
    node("processor", "Transform") { ... }

    connect("generator" to "processor") {
        ipType = "ip_string"  // Reference to registered IP type
    }
}
```

### Backward Compatibility

- Connections without `ipTypeId` are valid (null = untyped/Any)
- Existing .flow.kts files will load without modification
- Missing IP type references default to "Any" type for display purposes

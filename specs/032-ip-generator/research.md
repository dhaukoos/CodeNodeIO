# Research: IP Generator Interface

**Feature**: 032-ip-generator | **Date**: 2026-02-27

## R1: UI Panel Pattern (NodeGeneratorPanel)

**Decision**: Follow existing NodeGeneratorPanel collapsible panel pattern exactly.

**Rationale**: The NodeGeneratorPanel establishes a clear, proven pattern used in the graphEditor:
- Two-level composable: stateful wrapper (`NodeGeneratorPanel`) + stateless content (`NodeGeneratorPanelContent`)
- Collapsible header with arrow indicator (▼/▶) and clickable toggle
- Content conditionally rendered via `if (state.isExpanded)`
- Cancel/Create buttons at bottom, Create disabled until `state.isValid`
- Panel width: 250.dp, background: `Color(0xFFF5F5F5)`, border: 1.dp `Color(0xFFE0E0E0)`, padding: 12.dp

**Alternatives considered**:
- Dialog-based creation: Rejected — inconsistent with existing graph editor UX patterns
- Inline form within IP Palette: Rejected — too cramped at 200.dp width, separate panel provides better UX

## R2: State Management (ViewModel Pattern)

**Decision**: Use `MutableStateFlow<IPGeneratorPanelState>` in an `IPGeneratorViewModel`, consistent with `NodeGeneratorViewModel`.

**Rationale**: All graph editor ViewModels follow this pattern:
- Immutable data class with computed `isValid` property
- `_state.update { it.copy(...) }` for all mutations
- UI collects via `collectAsState()`
- Business logic (validation, creation) encapsulated in ViewModel
- `reset()` method clears form but preserves `isExpanded`

**Alternatives considered**:
- Compose `remember` state only: Rejected — violates project ViewModel convention, harder to test
- Shared ViewModel with IPPalette: Rejected — separate responsibilities, would complicate both VMs

## R3: IP Type Data Model

**Decision**: Use existing `InformationPacketType` model with a new `CompositeIPType` concept for custom types that have properties.

**Rationale**: The existing `InformationPacketType` has `id`, `typeName`, `payloadType: KClass<*>`, `color: IPColor`, `description`. Custom IP types need additional property metadata. Since `payloadType` uses `KClass<*>` which cannot represent user-defined composite types at runtime, custom types will use `Any::class` as the payload type and store property definitions separately.

**Alternatives considered**:
- Extend InformationPacketType with properties field: Rejected — would add complexity to existing model used broadly
- Create entirely separate model: Rejected — custom types still need to appear in IP Palette and be selectable via IPTypeRegistry

## R4: IP Type Registration

**Decision**: Register custom types in existing `IPTypeRegistry` using `register()` method. Use `getByTypeName()` for duplicate name checking.

**Rationale**: The IPTypeRegistry already supports:
- `register(type)` — add/replace type in registry
- `getByTypeName(name)` — lookup by display name (returns null if not found, non-null if taken)
- `getAllTypes()` — returns all registered types for dropdown population
- Built-in types: Any, Int, Double, Boolean, String (via `withDefaults()`)

**Alternatives considered**:
- Separate registry for custom types: Rejected — would require merging for dropdown population, complicates type lookup

## R5: Color Assignment for Custom Types

**Decision**: Auto-assign colors using a predefined palette of distinct colors, cycling through them for each new custom type.

**Rationale**: The spec assumes automatic color assignment. Using a palette of visually distinct colors (avoiding the 5 built-in type colors) ensures new types are easy to differentiate in the UI.

**Alternatives considered**:
- Random color generation: Rejected — could produce low-contrast or duplicate colors
- User color picker: Rejected — out of scope per spec (future enhancement)
- Single default color: Rejected — all custom types would look the same

## R6: Property Row UI Pattern

**Decision**: Adapt the existing `PortEditorRow` pattern from PropertiesPanel for IP property rows.

**Rationale**: PropertiesPanel already has `PortEditorRow` with:
- `OutlinedTextField` for name (flexible width)
- IP type dropdown with color swatch (100.dp width)
- Error indication via `isError = portName.isBlank()`
- This pattern is familiar to users and consistent with the graph editor design language

The IP Generator property row will add:
- Required/optional toggle (Checkbox or Switch)
- Remove button ("-" icon button)

**Alternatives considered**:
- Custom row design: Rejected — inconsistent with existing UI, more development effort

## R7: Layout Positioning

**Decision**: Place IPGeneratorPanel in a Column above the IPPalette, mirroring the NodeGeneratorPanel-above-NodePalette layout.

**Rationale**: Main.kt layout structure is:
```
Row {
  Column { NodeGeneratorPanel; NodePalette }   // Left
  IPPalette                                     // Center-left (200.dp)
  GraphEditorWithToggle                         // Center (weight 1f)
  PropertiesPanel + RuntimePreview              // Right
}
```
The IP Generator should wrap IPPalette in a Column: `Column { IPGeneratorPanel; IPPalette }`.

**Alternatives considered**:
- Place inside IP Palette panel: Rejected — too cramped, inconsistent with Node Generator pattern
- Separate floating panel: Rejected — inconsistent with existing fixed-panel layout

## R8: Custom IP Property Storage

**Decision**: Use kotlinx.serialization to represent and persist custom IP types to JSON, following the `FileCustomNodeRepository` pattern.

**Rationale**: Custom IP types should survive across sessions. The project already has a proven persistence pattern:
- `FileCustomNodeRepository` persists `@Serializable` `CustomNodeDefinition` objects to `~/.codenode/custom-nodes.json`
- Both `graphEditor` and `fbpDsl` modules have `kotlin("plugin.serialization")` and `libs.serialization.json` configured
- `IPColor` is already `@Serializable`

**Key constraint**: `InformationPacketType.payloadType` is `KClass<*>`, which is not natively serializable. Solution: create a `@Serializable` DTO (`SerializableIPType`) that stores `payloadTypeName: String` instead of `KClass<*>`, and maps back to `Any::class` for custom types on deserialization. This mirrors the existing `KClassSerializer` stub which already serializes to simple name and deserializes to `Any::class`.

**Storage approach**:
- New `FileIPTypeRepository` class following `FileCustomNodeRepository` pattern
- Persists to `~/.codenode/custom-ip-types.json`
- Uses `Json { prettyPrint = true; ignoreUnknownKeys = true }`
- Loads custom types on startup, saves after each Create
- Only custom (user-created) types are persisted; built-in types are always loaded from `withDefaults()`

**Serializable model**:
```kotlin
@Serializable
data class SerializableIPType(
    val id: String,
    val typeName: String,
    val payloadTypeName: String,  // e.g., "Any" for custom composite types
    val color: IPColor,
    val description: String? = null,
    val properties: List<SerializableIPProperty> = emptyList()
)

@Serializable
data class SerializableIPProperty(
    val name: String,
    val typeId: String,
    val isRequired: Boolean = true
)
```

**Alternatives considered**:
- In-memory only (transient): Rejected — user clarification requires persistence via kotlinx.serialization
- Custom KSerializer for InformationPacketType: Rejected — over-engineered; a separate DTO cleanly separates serialization from domain model
- Store in InformationPacketType directly: Rejected — `KClass<*>` field makes direct serialization impossible without invasive changes to fbpDsl module

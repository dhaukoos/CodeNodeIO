# Data Model: IP Generator Interface

**Feature**: 032-ip-generator | **Date**: 2026-02-27

## Entities

### IPProperty

Represents a single named, typed property within a custom IP type definition.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| name | String | Non-blank, unique within parent type | Property name (e.g., "age", "email") |
| typeId | String | Must reference a registered IP type ID | The IP type of this property (e.g., "ip_string") |
| isRequired | Boolean | Default: true | Whether the property is required (non-nullable) or optional (nullable with null default) |

**Validation Rules**:
- `name` must not be blank
- `name` must be unique among sibling properties within the same custom IP type
- `typeId` must correspond to a type currently registered in the IPTypeRegistry

**Relationships**:
- Belongs to one `CustomIPTypeDefinition`
- References one `InformationPacketType` via `typeId`

---

### CustomIPTypeDefinition

Represents the metadata for a user-defined composite IP type, including its properties. This is the in-memory definition used during creation and persisted via `FileIPTypeRepository`.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | String | Auto-generated, unique | Unique identifier (e.g., "ip_userprofile") |
| typeName | String | Non-blank, unique across all registered types (case-insensitive) | Display name (e.g., "UserProfile") |
| properties | List&lt;IPProperty&gt; | May be empty (marker type) | Ordered list of typed properties |
| color | IPColor | Auto-assigned from palette | Visual color for the type in the UI |

**Validation Rules**:
- `typeName` must not be blank
- `typeName` must not match any existing registered type name (case-insensitive)
- All properties must have non-blank, unique names
- Zero properties is valid (marker/tag type)

**Relationships**:
- Contains zero or more `IPProperty` instances
- Registered as an `InformationPacketType` in the `IPTypeRegistry` upon creation
- Serialized as `SerializableIPType` for JSON persistence

---

### SerializableIPType

`@Serializable` DTO for persisting custom IP types to JSON. Mirrors `CustomIPTypeDefinition` but replaces `KClass<*>` with a string type name for serialization compatibility.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | String | Non-blank, unique | Matches the registered type ID |
| typeName | String | Non-blank | Display name (e.g., "UserProfile") |
| payloadTypeName | String | Default: "Any" | KClass simple name; always "Any" for custom composite types |
| color | IPColor | Serialized as nested RGB | Visual color |
| description | String? | Optional | Type description |
| properties | List&lt;SerializableIPProperty&gt; | Default: emptyList() | Property definitions |

**Relationships**:
- Converts to/from `CustomIPTypeDefinition` for in-memory use
- Converts to `InformationPacketType` for registry registration (with `payloadType = Any::class`)

---

### SerializableIPProperty

`@Serializable` DTO for a single property within a serialized custom IP type.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| name | String | Non-blank | Property name |
| typeId | String | References a registered type ID | The IP type of this property |
| isRequired | Boolean | Default: true | Required (non-nullable) vs optional (nullable with null default) |

---

### FileIPTypeRepository

Repository class for JSON persistence of custom IP types, following the `FileCustomNodeRepository` pattern.

| Aspect | Detail |
|--------|--------|
| File path | `~/.codenode/custom-ip-types.json` |
| Format | JSON via `Json { prettyPrint = true; ignoreUnknownKeys = true }` |
| Load | Reads file on startup, deserializes `List<SerializableIPType>`, registers each in IPTypeRegistry |
| Save | Serializes all custom types to JSON, writes to file |
| Error handling | Graceful fallback if file missing or corrupt (logs warning, starts with empty list) |

---

### IPGeneratorPanelState

UI state for the IP Generator panel form. Immutable data class with computed validation properties.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| typeName | String | "" | Name entered in the type name text field |
| properties | List&lt;IPPropertyState&gt; | emptyList() | Current property rows in the form |
| isExpanded | Boolean | false | Whether the panel is expanded or collapsed |

**Computed Properties**:
- `isValid: Boolean` — true when typeName is non-blank, no duplicate type name exists, all properties have non-blank unique names
- `hasNameConflict: Boolean` — true when typeName matches an existing registered type (case-insensitive)
- `hasDuplicatePropertyNames: Boolean` — true when two or more properties share the same name
- `hasEmptyPropertyNames: Boolean` — true when any property has a blank name

---

### IPPropertyState

UI state for a single property row in the IP Generator form.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| id | String | UUID | Unique identifier for the row (for list operations) |
| name | String | "" | Property name entered by user |
| selectedTypeId | String | "ip_any" | Selected IP type ID from dropdown |
| isRequired | Boolean | true | Whether property is required or optional |

---

## State Transitions

### IP Generator Form Lifecycle

```
COLLAPSED (initial)
  │
  ├── Toggle Header → EXPANDED (empty form)
  │     │
  │     ├── Enter name → EDITING (name entered)
  │     │     │
  │     │     ├── Add property (+) → EDITING (with properties)
  │     │     │     │
  │     │     │     ├── Edit property name/type/required → EDITING
  │     │     │     ├── Remove property (-) → EDITING
  │     │     │     └── Add more properties (+) → EDITING
  │     │     │
  │     │     ├── Click Create (valid) → type registered → EXPANDED (form reset)
  │     │     └── Click Cancel → EXPANDED (form reset)
  │     │
  │     └── Toggle Header → COLLAPSED (data preserved)
  │
  └── Toggle Header → EXPANDED (data preserved if previously entered)
```

**Key transition rules**:
- Cancel always resets to empty form state (typeName="", properties=[])
- Create registers the type, persists to JSON via FileIPTypeRepository, and resets to empty form state
- Collapse preserves entered data until Cancel is explicitly clicked
- Panel state (expanded/collapsed) persists independently of form data

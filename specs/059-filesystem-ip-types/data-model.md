# Data Model: Filesystem-Based IP Types

**Feature**: 059-filesystem-ip-types
**Date**: 2026-03-24

## Entities

### IPTypeFileMeta

Metadata extracted from an IP type `.kt` file on the filesystem. This is the discovery-time representation used before the type is registered.

| Field | Type | Description |
|-------|------|-------------|
| typeName | String | Display name (e.g., "Coordinates") |
| typeId | String | Unique identifier (e.g., "ip_coordinates") |
| color | IPColor | Palette display color (rgb) |
| properties | List&lt;IPPropertyMeta&gt; | Parsed data class fields |
| filePath | String | Absolute path to the `.kt` source file |
| tier | PlacementLevel | MODULE, PROJECT, or UNIVERSAL |
| packageName | String | Kotlin package declaration from file |
| className | String | Fully qualified class name (package + typeName) |

### IPPropertyMeta

A single property parsed from the data class fields.

| Field | Type | Description |
|-------|------|-------------|
| name | String | Property name (e.g., "latitude") |
| kotlinType | String | Kotlin type as string (e.g., "Double", "String", "List&lt;String&gt;") |
| typeId | String | Resolved IP type ID if it maps to a registered type |
| isRequired | Boolean | True if not nullable (no `?` suffix) |

### PlacementLevel (shared enum)

Determines filesystem location for IP type and node files.

| Value | Display Name | IP Types Directory | Availability |
|-------|-------------|-------------------|--------------|
| MODULE | Module | `{Module}/src/commonMain/kotlin/.../iptypes/` | Only when a module is loaded |
| PROJECT | Project | `iptypes/src/commonMain/kotlin/io/codenode/iptypes/` | Always |
| UNIVERSAL | Universal | `~/.codenode/iptypes/` | Always |

### InformationPacketType (existing, modified)

The existing registry entry. The key change is `payloadType` now carries the real KClass for compiled types.

| Field | Type | Change |
|-------|------|--------|
| id | String | No change |
| typeName | String | No change |
| payloadType | KClass<*> | **Changed**: Real KClass for compiled types, `Any::class` only for Universal templates |
| color | IPColor | No change |
| description | String? | No change |

### CustomIPTypeDefinition (existing, modified)

The domain model for custom types. Gains a `filePath` field.

| Field | Type | Change |
|-------|------|--------|
| id | String | No change |
| typeName | String | No change |
| properties | List&lt;IPProperty&gt; | No change |
| color | IPColor | No change |
| filePath | String? | **Added**: Path to backing `.kt` file |
| tier | PlacementLevel? | **Added**: Which filesystem tier |

## Relationships

```text
IPTypeFileMeta ──parses-from──> .kt file on filesystem
IPTypeFileMeta ──registers-as──> InformationPacketType (in IPTypeRegistry)
IPTypeFileMeta ──creates──> CustomIPTypeDefinition (with filePath + tier)

PlacementLevel ──determines──> filesystem directory
PlacementLevel ──shared-by──> NodeGeneratorViewModel, IPGeneratorViewModel

InformationPacketType.payloadType ──resolves-via──> Class.forName() reflection (compiled types)
InformationPacketType.payloadType ──falls-back──> Any::class (Universal templates)
```

## State Transitions

### IP Type Lifecycle

```text
[File Created] → [Discovered on Launch] → [Parsed to IPTypeFileMeta] → [Registered in IPTypeRegistry]
                                                                              ↓
[File Deleted] → [Not Found on Next Launch] → [Absent from Registry] → [Gone from Palette]
```

### Migration Lifecycle (one-time)

```text
[JSON exists + iptypes/ empty] → [Read JSON entries] → [Generate .kt files in Universal] → [Backup JSON as .bak] → [Normal filesystem discovery]
```

# Research: Filesystem-Based IP Types

**Feature**: 059-filesystem-ip-types
**Date**: 2026-03-24

## Decision 1: Three-Tier Directory Layout for IP Types

**Decision**: Use `/iptypes/` as the subdirectory name at all three tiers for consistency.

| Tier | Node Directory | IP Types Directory |
|------|---|---|
| **MODULE** | `{Module}/src/commonMain/kotlin/io/codenode/{moduleName}/nodes/` | `{Module}/src/commonMain/kotlin/io/codenode/{moduleName}/iptypes/` |
| **PROJECT** | `nodes/src/commonMain/kotlin/io/codenode/nodes/` | `iptypes/src/commonMain/kotlin/io/codenode/iptypes/` |
| **UNIVERSAL** | `~/.codenode/nodes/` | `~/.codenode/iptypes/` |

**Rationale**: Using `iptypes/` consistently across all three tiers makes the convention easy to remember and avoids confusion with general-purpose `models/` directories that may contain non-IP-type classes (e.g., serialization DTOs like `OpenMeteoResponse`). Existing modules that currently store IP type data classes in `models/` (e.g., WeatherForecast, EdgeArtFilter) will be migrated to `iptypes/` as part of this feature.

**Alternatives considered**:
- Using `models/` at Module level — rejected because `models/` directories often contain non-IP-type classes (serialization DTOs, internal models), making discovery ambiguous. A dedicated `iptypes/` directory is unambiguous.
- Using a single flat `~/.codenode/iptypes/` for all levels — rejected because module-level types should live with their module source for compilation and type safety.

## Decision 2: IP Type File Format

**Decision**: Generated files are standard Kotlin `data class` declarations with a metadata comment header for discovery parsing.

```kotlin
/*
 * Coordinates - Custom IP Type
 * @IPType
 * @TypeName Coordinates
 * @TypeId ip_coordinates
 * @Color rgb(30, 144, 255)
 */

package io.codenode.weatherforecast.iptypes

data class Coordinates(
    val latitude: Double,
    val longitude: Double
)
```

**Rationale**: The `@IPType` marker in the comment header enables regex-based discovery (same approach nodes use with `override val name = "..."` parsing). The file remains a valid Kotlin data class that compiles normally. The metadata comment is only needed for discovery — the actual class definition provides compile-time type safety.

**Alternatives considered**:
- Annotation-based marker (`@IPType` Kotlin annotation) — rejected because it requires the annotation class to be on the classpath for Universal-level templates that are not compiled.
- Separate metadata sidecar file — rejected because it breaks the single-file simplicity and creates synchronization issues.

## Decision 3: KClass Resolution Strategy

**Decision**: Two-tier resolution based on whether the type is compiled.

| Tier | Compiled? | KClass Resolution |
|------|-----------|------------------|
| **MODULE** | Yes (on classpath) | `Class.forName(fqcn).kotlin` via reflection |
| **PROJECT** | Yes (on classpath if project-level module exists) | Same reflection approach |
| **UNIVERSAL** | No (source template only) | Falls back to `Any::class`, parsed metadata for UI |

**Rationale**: Module and Project-level types are part of the Gradle build and end up on the classpath. The graphEditor (JVM Desktop app) can use Java reflection to resolve the actual KClass at runtime. Universal types are template files not compiled into the graphEditor classpath, so they cannot have real KClass references — `Any::class` is the correct fallback.

**Alternatives considered**:
- Runtime compilation of Universal-level types — rejected as over-engineering; Universal types serve as templates for palette display, not for compile-time checking.
- Storing KClass string references for deferred resolution — rejected because it doesn't add value over reflection-based resolution for compiled types.

## Decision 4: PlacementLevel Reuse

**Decision**: Reuse the existing `PlacementLevel` enum from `NodeGeneratorViewModel.kt` by extracting it to a shared location.

**Rationale**: Both the Node Generator and IP Generator need the same Module/Project/Universal concept. Duplicating the enum would violate DRY. Extracting to a shared model file (e.g., `graphEditor/src/jvmMain/kotlin/model/PlacementLevel.kt`) allows both ViewModels to reference it.

**Alternatives considered**:
- Duplicate enum in IPGeneratorViewModel — rejected for code duplication.
- Keep in NodeGeneratorViewModel and import from there — rejected because it creates an odd dependency direction.

## Decision 5: Legacy JSON Migration

**Decision**: On first launch after upgrade, if `~/.codenode/custom-ip-types.json` exists and `~/.codenode/iptypes/` does not (or is empty), migrate each entry to a `.kt` file in the Universal directory. Preserve the original JSON file as `custom-ip-types.json.bak`.

**Rationale**: Existing users have custom types in the JSON repository. A seamless migration preserves their work. Writing to Universal ensures types are available globally. The backup file prevents data loss.

**Alternatives considered**:
- No migration (force users to recreate) — rejected for poor user experience.
- Keep JSON as a parallel source — rejected because it defeats the purpose of filesystem-as-source-of-truth.

## Decision 6: Discovery Parsing for IP Type Files

**Decision**: Use regex-based metadata extraction from comment headers, mirroring how `NodeDefinitionRegistry.parseTemplateMetadata()` works for nodes.

**Parsed fields**:
- `@IPType` marker — identifies the file as an IP type definition
- `@TypeName` — display name for the palette
- `@TypeId` — unique identifier for registry
- `@Color` — palette color (rgb format)
- Data class fields — extracted via `data class (\w+)\(([^)]*)\)` regex for property discovery

**Rationale**: This approach is proven in the node system. Regex parsing is fast, doesn't require compilation, and works for Universal-level templates that aren't on the classpath.

**Alternatives considered**:
- kotlinx-serialization annotation scanning — requires compilation, doesn't work for templates.
- AST parsing (kotlin-compiler-embeddable) — heavy dependency for simple metadata extraction.

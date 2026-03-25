# Contract: IP Type Filesystem Discovery

**Feature**: 059-filesystem-ip-types

## IPTypeDiscovery Interface

Discovers IP type definition files from the three-tier filesystem and returns parsed metadata.

### discoverAll()

Scans all three tiers and returns discovered IP type metadata.

**Inputs**: None (uses configured directory paths)

**Returns**: `List<IPTypeFileMeta>` — all discovered types, deduplicated by type name with tier precedence (Module > Project > Universal)

**Behavior**:
1. Scan Module directory (if module loaded) for `.kt` files containing `@IPType` marker
2. Scan Project directory for `.kt` files containing `@IPType` marker
3. Scan Universal directory for `.kt` files containing `@IPType` marker
4. For each file, parse metadata via regex extraction
5. Deduplicate: if same typeName found at multiple tiers, keep the most specific tier
6. Return merged list

**Error handling**: Files that fail to parse are skipped with a warning. Discovery continues for remaining files.

### parseIPTypeFile(filePath: String)

Parses a single `.kt` file and extracts IP type metadata.

**Inputs**: Absolute path to a `.kt` file

**Returns**: `IPTypeFileMeta?` — parsed metadata, or null if file is not a valid IP type definition

**Parsing rules**:
1. File MUST contain `@IPType` marker in a comment header
2. Extract `@TypeName`, `@TypeId`, `@Color` from comment header
3. Extract `data class` declaration and parse field names and types
4. Extract `package` declaration for fully qualified class name

### resolveKClass(meta: IPTypeFileMeta)

Attempts to resolve the real KClass for a discovered IP type.

**Inputs**: Parsed metadata including className

**Returns**: `KClass<*>` — the actual KClass if on classpath, or `Any::class` if not

**Behavior**:
1. Attempt `Class.forName(meta.className).kotlin`
2. If `ClassNotFoundException` → return `Any::class`
3. If success → return resolved KClass

## IPTypeFileGenerator Interface

Generates Kotlin data class files for new IP types.

### generateIPTypeFile(definition, level, activeModulePath?)

Generates and writes a `.kt` file for the given IP type definition.

**Inputs**:
- `definition: CustomIPTypeDefinition` — type name, properties, color
- `level: PlacementLevel` — MODULE, PROJECT, or UNIVERSAL
- `activeModulePath: String?` — required when level is MODULE

**Returns**: `String` — absolute path to the generated file

**Generated file format**:
```kotlin
/*
 * {TypeName} - Custom IP Type
 * @IPType
 * @TypeName {TypeName}
 * @TypeId {typeId}
 * @Color rgb({r}, {g}, {b})
 */

package {packageName}

data class {TypeName}(
    val {prop1Name}: {Prop1Type},
    val {prop2Name}: {Prop2Type}
)
```

**Package resolution by level**:
- MODULE: `io.codenode.{moduleName}.iptypes`
- PROJECT: `io.codenode.iptypes`
- UNIVERSAL: No package (top-level, not compiled)

## IPTypeMigration Interface

One-time migration from legacy JSON repository to filesystem.

### migrateIfNeeded()

Checks for legacy JSON file and migrates to filesystem if needed.

**Behavior**:
1. Check if `~/.codenode/custom-ip-types.json` exists
2. Check if `~/.codenode/iptypes/` is empty or doesn't exist
3. If JSON exists AND iptypes is empty:
   a. Read all entries from JSON
   b. For each entry, generate a `.kt` file in `~/.codenode/iptypes/`
   c. Rename JSON to `custom-ip-types.json.bak`
4. If iptypes is already populated, skip migration (already done)

**Error handling**: If migration fails midway, already-written files are preserved. JSON file is only renamed after all files are written successfully.

# Feature Specification: Filesystem-Based IP Types

**Feature Branch**: `059-filesystem-ip-types`
**Created**: 2026-03-24
**Status**: Draft
**Input**: User description: "IP Types on filesystem — resolve the deeper infrastructure issue where IPTypeRegistry stores custom types with Any::class by design. Model file storage for generated IP Types to match the pattern used for Nodes (Module, Project, Universal). Add a level dropdown on the IP Generator panel. The palette corresponds directly to types found on the filesystem."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - IP Types Discovered from Filesystem (Priority: P1)

When the graphEditor launches, it scans the filesystem for IP type definition files (Kotlin data classes) and populates the IP Type Palette from what it finds. Types are organized in three tiers — Module (inside the active module's source tree), Project (in a shared project-level directory), and Universal (in the user's global `~/.codenode/` directory). Base types (Int, Double, String, Boolean) remain built-in and require no files.

**Why this priority**: This is the foundational change — all other stories depend on IP types being filesystem-backed rather than JSON-repository-backed. Without this, the palette cannot be filesystem-driven and types cannot carry real KClass references.

**Independent Test**: Launch graphEditor with IP type `.kt` files placed in the Module, Project, and Universal directories. Verify all three tiers appear in the IP Type Palette. Delete a file, relaunch, and verify the type is gone from the palette.

**Acceptance Scenarios**:

1. **Given** a Module's `iptypes/` directory contains an IP type data class file, **When** the graphEditor launches with that module loaded, **Then** the type appears in the IP Type Palette as a Module-level type.
2. **Given** an IP type `.kt` file exists in the Project-level IP types directory, **When** the graphEditor launches, **Then** the type appears in the palette as a Project-level type.
3. **Given** an IP type `.kt` file exists in the Universal-level IP types directory, **When** the graphEditor launches, **Then** the type appears in the palette as a Universal-level type.
4. **Given** a previously present IP type `.kt` file is deleted from the filesystem, **When** the graphEditor is relaunched, **Then** the type no longer appears in the palette.
5. **Given** base types (Int, Double, String, Boolean, Any), **When** the graphEditor launches, **Then** these types are always available regardless of filesystem contents.

---

### User Story 2 - IP Generator Level Dropdown (Priority: P2)

The IP Generator panel includes a "Level" dropdown (matching the design of the Node Generator panel's level dropdown) that determines where the generated IP type file is written. The three levels are Module, Project, and Universal. Module is only available when a module is currently loaded. When the user creates an IP type, a Kotlin data class file is generated and written to the appropriate filesystem location.

**Why this priority**: Once filesystem discovery is in place (US1), users need the ability to create new IP types at each level. The level dropdown is the primary UX for controlling where types are stored.

**Independent Test**: Open the IP Generator panel, select each level in the dropdown, fill in a type name and properties, and click Create. Verify the generated `.kt` file appears in the correct directory for each level.

**Acceptance Scenarios**:

1. **Given** the IP Generator panel is open and no module is loaded, **When** the user views the Level dropdown, **Then** only Project and Universal options are available (Module is disabled or hidden).
2. **Given** a module is loaded, **When** the user views the Level dropdown, **Then** Module, Project, and Universal are all available.
3. **Given** the user selects "Module" level and fills in a valid type, **When** the user clicks Create, **Then** a Kotlin data class file is generated in the active module's `iptypes/` source directory.
4. **Given** the user selects "Project" level and fills in a valid type, **When** the user clicks Create, **Then** a Kotlin data class file is generated in the project-level IP types directory.
5. **Given** the user selects "Universal" level and fills in a valid type, **When** the user clicks Create, **Then** a Kotlin data class file is generated in the universal IP types directory (`~/.codenode/iptypes/`).
6. **Given** the user creates a new IP type at any level, **When** creation completes, **Then** the type immediately appears in the IP Type Palette without requiring a relaunch.

---

### User Story 3 - Concrete KClass References for Custom Types (Priority: P3)

Custom IP types carry their actual Kotlin KClass reference (e.g., `Coordinates::class`) instead of `Any::class`. When a module defines its own IP type data classes and those types are on the classpath, the system resolves the real KClass for use in port type checking. This enables compile-time type safety — connecting two nodes with mismatched IP types produces a type error rather than silently passing untyped data.

**Why this priority**: This is the type-safety payoff. It requires filesystem-backed types (US1) and the generation infrastructure (US2) to be in place first. The value is correctness — the system enforces that data flowing between nodes matches the declared types.

**Independent Test**: Create a module with two nodes whose ports declare different concrete IP types (e.g., `Coordinates::class` and `HttpResponse::class`). Verify that the IPTypeRegistry stores the real KClass for compiled types. Verify that port specifications reference the concrete KClass.

**Acceptance Scenarios**:

1. **Given** an IP type data class file exists on the classpath (Module or Project level), **When** the IPTypeRegistry discovers it, **Then** the registered type's `payloadType` is the actual KClass (not `Any::class`).
2. **Given** an IP type file exists only as a source template (Universal level, not compiled), **When** the IPTypeRegistry discovers it, **Then** the type is registered with metadata parsed from the source file (properties, name) and `payloadType` falls back to `Any::class` since it is not compiled.
3. **Given** a node's PortSpec declares `Coordinates::class` as its data type, **When** the flow graph is constructed, **Then** the port carries the `Coordinates` type for connection validation.

---

### Edge Cases

- What happens when two IP type files at different levels define the same type name? The most specific level wins: Module overrides Project, Project overrides Universal.
- What happens when an IP type file has syntax errors or cannot be parsed? The type is skipped with a warning logged, and other types continue to load normally.
- What happens when a Module-level IP type is referenced by a flow graph but the module is not loaded? The type is unavailable in the palette; existing flow graphs referencing it show a "type not found" indicator.
- What happens when the universal IP types directory (`~/.codenode/iptypes/`) does not exist? It is created automatically on first IP type generation at the Universal level.
- What happens when a user deletes an IP type that is actively used in connections within a flow graph? The flow graph retains the type reference string, but the palette no longer shows the type. The graphEditor displays a warning on the affected connections.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST discover IP type definitions from three filesystem tiers: Module (active module source tree), Project (shared project directory), and Universal (user's global `~/.codenode/iptypes/` directory).
- **FR-002**: System MUST populate the IP Type Palette exclusively from filesystem-discovered types plus the built-in base types (Int, Double, String, Boolean, Any).
- **FR-003**: System MUST remove an IP type from the palette when its corresponding file is deleted from the filesystem (effective on next launch).
- **FR-004**: The IP Generator panel MUST include a "Level" dropdown with Module, Project, and Universal options, matching the visual design of the Node Generator panel's level dropdown.
- **FR-005**: The Module option in the Level dropdown MUST be disabled or hidden when no module is currently loaded.
- **FR-006**: When the user creates an IP type, the system MUST generate a Kotlin data class file and write it to the directory corresponding to the selected level.
- **FR-007**: After generating an IP type file, the system MUST immediately register the type in the palette without requiring a relaunch.
- **FR-008**: For IP types on the compiled classpath (Module and Project levels), the IPTypeRegistry MUST store the actual KClass reference instead of `Any::class`.
- **FR-009**: For IP types discovered as source templates only (Universal level), the IPTypeRegistry MUST parse type metadata (name, properties) from the source file.
- **FR-010**: When multiple IP type files at different levels share the same type name, the most specific level MUST take precedence (Module > Project > Universal).
- **FR-011**: Base types (Int, Double, String, Boolean, Any) MUST always be available regardless of filesystem contents and MUST NOT require separate files.
- **FR-012**: The generated IP type file MUST be a valid Kotlin data class with fields matching the properties defined in the IP Generator form.
- **FR-013**: The system MUST migrate existing custom IP types from the legacy JSON repository (`~/.codenode/custom-ip-types.json`) to filesystem files on first launch after upgrade.
- **FR-014**: Existing modules that store IP type data classes in `models/` directories (e.g., WeatherForecast, EdgeArtFilter) MUST be migrated to `iptypes/` directories, with all import references updated accordingly.

### Key Entities

- **IP Type Definition File**: A Kotlin `.kt` file containing a `data class` declaration. Represents a single IP type with its properties (fields). Located in one of three filesystem tiers.
- **IP Type Tier**: The storage level for an IP type — Module (scoped to one module), Project (shared across modules in the project), or Universal (available globally across all projects).
- **Built-in Base Type**: A primitive or standard type (Int, Double, String, Boolean, Any) that is always available without a filesystem file.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All custom IP types in the palette correspond 1:1 with files on the filesystem — no types exist without a backing file (except base types).
- **SC-002**: The IP Generator's Level dropdown functions identically to the Node Generator's Level dropdown in terms of available options and behavior.
- **SC-003**: Module-level and Project-level IP types carry their actual KClass reference in the IPTypeRegistry, not `Any::class`.
- **SC-004**: Deleting an IP type file and relaunching the graphEditor results in the type being absent from the palette within one launch cycle.
- **SC-005**: Creating a new IP type via the IP Generator immediately adds it to the palette without restarting.
- **SC-006**: Existing custom IP types from the legacy JSON repository are preserved and accessible after migration to filesystem storage.

## Assumptions

- The three-tier directory convention for IP types uses `iptypes/` consistently at all levels: Module types in `{Module}/.../iptypes/`, Project types in `iptypes/src/.../io/codenode/iptypes/`, Universal types in `~/.codenode/iptypes/`.
- IP type files follow a naming convention derived from the type name (e.g., `Coordinates.kt` for a type named "Coordinates").
- The generated data class includes a package declaration appropriate to its tier level.
- Migration from the legacy JSON repository is a one-time, non-destructive operation (the JSON file is preserved as a backup).

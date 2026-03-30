# Research: Generate CodeNodeDefinition-Based Repository Modules

**Feature**: 061-codenode-definition-codegen
**Date**: 2026-03-30

## Decision 1: Generator Architecture — Replace vs Extend

**Decision**: Replace the three legacy generators (EntityCUDGenerator, EntityDisplayGenerator, and the implicit tick function pattern) with three new CodeNodeDefinition generators. Keep the existing generator classes but rewrite their output to produce `object : CodeNodeDefinition` files.

**Rationale**: Rewriting the output of existing generators is less disruptive than creating entirely new generator classes. The EntityModuleGenerator orchestrator changes minimally — it still calls `cudGenerator.generate(spec)` and `displayGenerator.generate(spec)`, but the output is now a CodeNodeDefinition object file instead of a factory function.

**Alternatives considered**:
- Create new generator classes (EntityCUDCodeNodeGenerator, etc.) — more files, more orchestrator changes, same result.
- Generate a single combined file with all three nodes — breaks the one-node-per-file convention.

## Decision 2: Generated File Location

**Decision**: Place generated node files in `{Module}/src/commonMain/kotlin/io/codenode/{module}/nodes/` — matching the convention of all existing hand-written modules.

**Rationale**: This is the established convention (UserProfiles, GeoLocations, Addresses, WeatherForecast, EdgeArtFilter, StopWatch all use `nodes/` subdirectory). The graphEditor's `NodeDefinitionRegistry.scanDirectory()` and `DynamicPreviewDiscovery` already scan this location.

## Decision 3: Repository Node Pattern — Identity Tracking

**Decision**: The generated RepositoryCodeNode uses the same identity-tracking pattern as the hand-written nodes: instance-scoped `var lastSaveRef`, `var lastUpdateRef`, `var lastRemoveRef` to prevent stale cached values from re-triggering operations in anyInput mode.

**Rationale**: This is the proven pattern from UserProfileRepositoryCodeNode, GeoLocationRepositoryCodeNode, and AddressRepositoryCodeNode. Without identity tracking, the anyInput processor would re-execute operations for stale values on every trigger.

## Decision 4: CUD Node Pattern — Coroutine-Based Reactive Source

**Decision**: The generated CUDCodeNode uses `coroutineScope` with three independent `launch` blocks, each collecting from a StateFlow (save, update, remove) and emitting selective `ProcessResult3` values.

**Rationale**: This is the exact pattern from UserProfileCUDCodeNode. Each CRUD operation is independently reactive — a save doesn't block updates or removes.

## Decision 5: Type Usage — IP Type vs Any

**Decision**: All generated nodes use the module's IP type (from `iptypes/`) for entity ports and `String` for result/error ports. The generated code includes `toEntity()` conversion at the DAO boundary.

**Rationale**: This matches the typed pattern established in feature 059 (filesystem-ip-types) and the hand-written nodes updated in features 058-059. Using concrete types enables compile-time type checking.

## Decision 6: EntityFlowGraphBuilder Changes

**Decision**: Add `_codeNodeClass` and `_genericType` configuration to all three nodes. Drop `_codeNodeDefinition` (redundant — all nodes are now CodeNodeDefinition). The class names follow the convention: `io.codenode.{module}.nodes.{NodeName}CodeNode`.

**Rationale**: `_codeNodeClass` is required by the RuntimeFlowGenerator to generate `import` statements and `createRuntime()` calls. `_genericType` is required for palette display and build template generation. The `_codeNodeDefinition` boolean flag was only needed to distinguish legacy from modern nodes — with legacy support removed, it's unnecessary.

## Decision 7: Eliminate Legacy Code Path from RuntimeFlowGenerator

**Decision**: Remove the legacy tick-function code path from RuntimeFlowGenerator entirely. All nodes MUST have `_codeNodeClass`. The dual-path branching (`codeNodeClassNodes` vs `legacyNodes`) is replaced with a single path that always uses CodeNodeDefinition instantiation.

**Rationale**: Legacy tick functions are no longer generated or supported. The existing hand-written modules (UserProfiles, GeoLocations, Addresses, StopWatch, EdgeArtFilter, WeatherForecast) all use CodeNodeDefinition objects already. Removing the legacy path simplifies the generator by ~50% and prevents accidental generation of non-compiling tick function references.

**What gets removed**:
- Tick function import generation (`import $basePackage.${node.name.camelCase()}Tick`)
- Legacy node handling (factory function calls with tick params)
- `legacyNodes` filter and all code that references it
- `_codeNodeDefinition` config key (redundant when all nodes are CodeNodeDefinition)

**What remains**:
- `_codeNodeClass` config key (required for FQCN import generation)
- `_genericType` config key (required for palette/build)
- CodeNodeDefinition import + `createRuntime()` instantiation (the only path)

## Decision 8: Legacy Generator Cleanup

**Decision**: Delete the legacy factory function generators (EntityCUDGenerator, EntityDisplayGenerator). Create new generators (EntityCUDCodeNodeGenerator, EntityDisplayCodeNodeGenerator, EntityRepositoryCodeNodeGenerator) that produce CodeNodeDefinition files.

**Rationale**: The legacy generators produce `internal fun createXxx() = CodeNodeFactory.xxx()` which is fundamentally incompatible with the `object XxxCodeNode : CodeNodeDefinition { ... }` pattern. New generator classes are cleaner than trying to adapt the old ones.

## Existing Code Analysis

### RuntimeFlowGenerator — Current Dual-Path Logic (to be simplified)

```
val codeNodeClassNodes = codeNodes.filter { it.configuration["_codeNodeClass"] != null }
val legacyNodes = codeNodes.filter { it.configuration["_codeNodeClass"] == null }
```

After this feature, the `legacyNodes` path and all its associated code is removed. All nodes go through the `codeNodeClassNodes` path:
- Import: `import io.codenode.{module}.nodes.{NodeName}CodeNode`
- Runtime: `val xxx = {NodeName}CodeNode.createRuntime("{NodeName}")`
- No tick function imports, no factory function calls

### Hand-Written Node Structure

Each CodeNodeDefinition follows this structure:
```
object {NodeName}CodeNode : CodeNodeDefinition {
    override val name = "{NodeName}"
    override val category = CodeNodeType.{CATEGORY}
    override val description = "..."
    override val inputPorts = listOf(PortSpec(...))
    override val outputPorts = listOf(PortSpec(...))
    override val anyInput = true  // for repository nodes

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createXxx<...>(
            name = name,
            ...
        )
    }
}
```

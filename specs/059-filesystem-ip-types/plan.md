# Implementation Plan: Filesystem-Based IP Types

**Branch**: `059-filesystem-ip-types` | **Date**: 2026-03-24 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/059-filesystem-ip-types/spec.md`

## Summary

Replace the JSON-backed IP type repository with filesystem-based discovery. IP type definition files (Kotlin data classes with `@IPType` metadata headers) are stored at three tiers — Module, Project, Universal — mirroring the node filesystem pattern. The IP Generator panel gains a Level dropdown. Compiled types resolve their actual KClass via reflection instead of using `Any::class`. Legacy JSON types are migrated to Universal-level files on first launch.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3
**Primary Dependencies**: kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, lifecycle-viewmodel-compose 2.8.0
**Storage**: Filesystem (`.kt` files at three tiers) — replaces `~/.codenode/custom-ip-types.json`
**Testing**: Kotlin commonTest + JVM test (graphEditor module)
**Target Platform**: JVM Desktop (graphEditor), KMP commonMain (IP type data classes)
**Project Type**: Multi-module KMP project
**Performance Goals**: IP type discovery completes within 500ms on launch for up to 100 types
**Constraints**: Universal-level types are not compiled (template-only); KClass resolution only works for compiled types on the classpath
**Scale/Scope**: ~15 files modified/created across graphEditor and fbpDsl modules

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Single responsibility: discovery, generation, registration are separate concerns. Clear type annotations throughout. |
| II. Test-Driven Development | PASS | Tests for discovery parsing, file generation, KClass resolution, and migration. Unit + integration coverage. |
| III. User Experience Consistency | PASS | Level dropdown matches Node Generator design. IP type palette updates immediately after creation. |
| IV. Performance Requirements | PASS | Filesystem scan is O(n) in file count. Regex parsing is lightweight. No scalability concerns at expected scale (~10-50 types). |
| V. Observability & Debugging | PASS | Warning logs for unparseable files. Migration status logged. |
| Licensing | PASS | No new dependencies. Uses JVM standard library reflection (`Class.forName`). All existing dependencies are Apache 2.0/MIT. |

**Post-Phase 1 re-check**: All gates still pass. No new patterns or dependencies introduced beyond what was evaluated.

## Project Structure

### Documentation (this feature)

```text
specs/059-filesystem-ip-types/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── ip-type-discovery.md
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
graphEditor/src/jvmMain/kotlin/
├── model/
│   └── PlacementLevel.kt              # Extracted shared enum (from NodeGeneratorViewModel)
├── state/
│   ├── IPTypeRegistry.kt              # Modified: filesystem-driven discovery, KClass resolution
│   └── IPTypeDiscovery.kt             # New: three-tier filesystem scanning + metadata parsing
├── repository/
│   ├── FileIPTypeRepository.kt        # Modified: deprecated, replaced by filesystem discovery
│   └── IPTypeMigration.kt             # New: one-time JSON→filesystem migration
├── viewmodel/
│   ├── IPGeneratorViewModel.kt        # Modified: Level dropdown, file generation
│   └── NodeGeneratorViewModel.kt      # Modified: use shared PlacementLevel
├── ui/
│   └── IPGeneratorPanel.kt            # Modified: Level dropdown UI
└── Main.kt                            # Modified: replace hardcoded registration with discovery

graphEditor/src/jvmTest/kotlin/
├── state/
│   └── IPTypeDiscoveryTest.kt         # New: parsing + discovery tests
├── repository/
│   └── IPTypeMigrationTest.kt         # New: migration tests
└── viewmodel/
    └── IPGeneratorViewModelTest.kt    # New/modified: level-based generation tests

# Three-tier IP type filesystem locations:
{Module}/src/commonMain/kotlin/io/codenode/{moduleName}/iptypes/  # Module level
iptypes/src/commonMain/kotlin/io/codenode/iptypes/                # Project level
~/.codenode/iptypes/                                               # Universal level
```

**Structure Decision**: Changes are concentrated in the graphEditor module (JVM Desktop) since that's where the IP type registry, generator, and palette live. The fbpDsl module is unchanged — `InformationPacketType` already supports any KClass via its `payloadType: KClass<*>` field. Module-level IP type data classes already exist in their module `models/` directories (e.g., WeatherForecast).

## Complexity Tracking

No constitution violations. All changes follow existing patterns (mirroring the node filesystem discovery from feature 055).

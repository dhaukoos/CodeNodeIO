# Quickstart: CodeNodeIO Development Setup

**Date**: 2026-01-16  
**Status**: Initial Project Setup Complete  
**Target Audience**: Developers contributing to CodeNodeIO IDE Plugin Platform

## Prerequisites

- **Kotlin 2.1.21** (enforced by build.gradle.kts)
- **JDK 11+** (recommended: JDK 17 or 21)
- **Gradle 8.5+** (wrapper included)
- **IntelliJ IDEA 2024.1+** or **Android Studio 2024.1+** (for IDE plugin development)
- **Go 1.21+** (for Go code generation testing)

## Project Structure

```
codenode-io/
├── build.gradle.kts              # Root build config (enforces Kotlin 2.1.21)
├── settings.gradle.kts           # Module definitions
├── fbpDsl/                        # Core FBP domain model
│   ├── build.gradle.kts
│   └── src/commonMain/kotlin/
├── graphEditor/                   # Visual editor (Compose Desktop)
│   ├── build.gradle.kts
│   └── src/jvmMain/kotlin/
├── circuitSimulator/              # Debugging/simulation tool
│   ├── build.gradle.kts
│   └── src/commonMain/kotlin/
├── kotlinCompiler/                # KMP code generator (KotlinPoet 2.2.0)
│   ├── build.gradle.kts
│   └── src/commonMain/kotlin/
├── goCompiler/                    # Go code generator
│   ├── build.gradle.kts
│   └── src/commonMain/kotlin/
├── idePlugin/                     # IntelliJ Platform plugin
│   ├── build.gradle.kts
│   └── src/main/kotlin/
└── specs/                         # Architecture & specifications
    └── 001-ide-plugin-platform/
        ├── research.md            # Technical decisions (UPDATED)
        ├── plan.md                # Implementation plan
        ├── data-model.md          # Core entities
        └── tasks.md               # Implementation tasks
```

## Quick Start

### 1. Clone & Setup

```bash
# Clone the repository (already done)
cd /Users/danahaukoos/CodeNodeIO

# Verify Gradle wrapper
./gradlew --version

# Sync all modules
./gradlew clean
```

### 2. Build All Modules

```bash
# Full build
./gradlew build

# Build specific module
./gradlew fbpDsl:build
./gradlew graphEditor:build
./gradlew kotlinCompiler:build
```

### 3. Run Tests

```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew fbpDsl:test
./gradlew graphEditor:test
```

### 4. Run Graph Editor (Compose Desktop)

```bash
./gradlew graphEditor:run
```

This launches the Compose Desktop UI (currently shows placeholder).

### 5. IDE Setup (IntelliJ IDEA / Android Studio)

1. Open project: `File → Open → /Users/danahaukoos/CodeNodeIO`
2. Select Gradle project import
3. Wait for Gradle sync to complete
4. Verify Kotlin 2.1.21 is detected in `Project Settings → Language & Frameworks → Kotlin`

### 6. Run IDE Plugin (Development)

```bash
# Build and run plugin in IDE
./gradlew idePlugin:runIde

# Or from IDE: Run → Run 'idePlugin'
```

This launches a sandbox IntelliJ instance with the CodeNodeIO plugin installed.

## Version Lock Details

The following versions are **pinned exactly** to ensure reproducible builds:

| Dependency | Version | Why |
|-----------|---------|-----|
| **Kotlin** | 2.1.21 | KotlinPoet 2.2.0 requirement; K2 compiler default |
| **KotlinPoet** | 2.2.0 | Type-safe code generation; tested against Kotlin 2.1.21 |
| **Compose Desktop** | 1.10.0 | Modern UI, K2 optimization, performance improvements |
| **Coroutines** | 1.8.0 | FBP execution model; async/channel support |
| **JUnit 5** | 5.10.1 | Testing framework |
| **IntelliJ Platform SDK** | 2024.1 | IDE plugin framework |

**Important**: These versions form a tested triangle:
```
KotlinPoet 2.2.0 ─→ requires Kotlin 2.1.21
                   ↓
              Compose 1.10.0 ─→ compatible with Kotlin 2.1.21
                   ↓
        IntelliJ Platform SDK 2024.1 ─→ compatible with Kotlin 2.1.21
```

Do NOT upgrade any version without re-validating the triangle.

## Module Dependencies

```
fbpDsl (core domain)
  ├── graphEditor (Compose UI) → fbpDsl
  ├── circuitSimulator → fbpDsl, graphEditor
  ├── kotlinCompiler (KMP gen) → fbpDsl
  ├── goCompiler (Go gen) → fbpDsl
  └── idePlugin (IDE plugin) → all modules
```

## Common Tasks

### Add a new dependency

Update the appropriate `build.gradle.kts`:

```kotlin
// Example: adding a new library to kotlinCompiler
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.example:library:1.0.0")
            }
        }
    }
}
```

**IMPORTANT**: Version-lock all dependencies. Do NOT use floating ranges like `1.0.+`.

### Run specific test class

```bash
./gradlew fbpDsl:test --tests "io.codenode.fbpdsl.model.*"
```

### Generate IDE index cache

```bash
./gradlew clean build
# IntelliJ will auto-regenerate index on next open
```

### Force Gradle dependency refresh

```bash
./gradlew build --refresh-dependencies
```

## Troubleshooting

### Gradle sync fails with "unsupported class-file format"

**Fix**: Verify JDK 11+ is selected:
```bash
./gradlew --version
# Look for "JVM version X.X.X"
```

If wrong JDK, set in IntelliJ: `Project Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JVM`

### Compose Desktop won't run

**Fix**: Ensure graphEditor/build.gradle.kts has `id("org.jetbrains.compose")` plugin

### KotlinPoet compilation errors

**Fix**: Confirm kotlinCompiler/build.gradle.kts has:
```kotlin
commonMainImplementation("com.squareup:kotlinpoet:2.2.0")
```

Version MUST be 2.2.0 (not "2.2.0+" or later).

### IDE plugin runIde fails

**Fix**: 
1. Verify IntelliJ Platform SDK 2024.1 in `Project Settings`
2. Run `./gradlew idePlugin:prepareSandboxPlugin` manually
3. Restart IntelliJ

## Next Steps

**Phase 1 Tasks** (from `specs/001-ide-plugin-platform/tasks.md`):

1. T001: Implement core FBP DSL (InformationPacket, Port, Node, Connection)
2. T002: Build graph rendering engine (Canvas-based)
3. T003: Implement KMP code generation (KotlinPoet)
4. T004: Add contract tests (multi-stage validation)
5. T005: Integrate with IDE plugin framework

See `specs/001-ide-plugin-platform/tasks.md` for detailed task breakdown.

## Documentation

- **Architecture**: `specs/001-ide-plugin-platform/spec.md`
- **Technical Decisions**: `specs/001-ide-plugin-platform/research.md`
- **Data Model**: `specs/001-ide-plugin-platform/data-model.md`
- **Implementation Plan**: `specs/001-ide-plugin-platform/plan.md`
- **API Contracts**: `specs/001-ide-plugin-platform/contracts/ide-plugin-api.md`

## Support

For questions on setup, versioning, or build issues:

1. Check `research.md` for technical rationale
2. Review `plan.md` for architectural decisions
3. Consult `data-model.md` for domain concepts
4. See existing module `build.gradle.kts` for patterns

---

**Last Updated**: 2026-01-16  
**Status**: Ready for Phase 1 Implementation


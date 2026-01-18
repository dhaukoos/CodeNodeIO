# CodeNodeIO

**Visual Flow-based Programming for Full-Stack Development**

CodeNodeIO is a JetBrains IDE plugin that enables developers to create full-stack applications using Flow-based Programming (FBP) principles through a visual CAD-like interface. Design flow graphs visually, generate production-ready Kotlin Multiplatform (Android, iOS, Web) and Go (backend) code automatically.

![Status](https://img.shields.io/badge/status-in%20development-yellow)
![License](https://img.shields.io/badge/license-Apache%202.0-blue)
![Kotlin](https://img.shields.io/badge/kotlin-2.1.21-purple)
![Platform](https://img.shields.io/badge/platform-IntelliJ%20Platform-orange)

---

## What is CodeNodeIO?

CodeNodeIO brings **CAD-level abstraction** to software development through Flow-based Programming, enabling developers to:

- **Visualize** application logic as flow graphs with nodes and connections
- **Design** business logic using drag-and-drop visual composition
- **Generate** production-ready KMP (Kotlin Multiplatform) and Go code automatically
- **Debug** flows using the built-in circuit simulator
- **Maintain** a hybrid visual/textual representation for clarity

**The Vision**: Shift software development from artisan-era "hand-coding" to industrial-era systematic design - just as CAD revolutionized engineering.

---

## Features

### Core Capabilities

- **Visual Flow Graph Editor**: Drag-and-drop nodes onto a canvas, connect ports to define data flow
- **Textual DSL Representation**: View and edit flow graphs in a clean, readable Kotlin DSL format
- **Code Generation**:
  - **KMP Frontend**: Generate Kotlin Multiplatform code for Android, iOS, Web, and Desktop
  - **Go Backend**: Generate idiomatic Go code for server-side services and APIs
- **Circuit Simulator**: Debug flows with pause/resume, speed control, and real-time IP (Information Packet) visualization
- **Validation Engine**: Ensure graphs are correct with port type checking, cycle detection, and connection validation
- **IDE Integration**: Seamless integration with IntelliJ IDEA, Android Studio, and GoLand

### Architecture

Built on **J. Paul Morrison's Flow-based Programming** principles:

- **Information Packets (IPs)**: Data units flowing through the graph
- **Nodes**: Processing units (CodeNodes for leaf logic, GraphNodes for compositions)
- **Ports**: Typed input/output interfaces with validation
- **Connections**: Data flow paths between nodes
- **Graph Execution**: Concurrent processing using Kotlin coroutines and Go goroutines

---

## Quick Start

### Prerequisites

- **Kotlin 2.1.21** (enforced by build.gradle.kts)
- **JDK 11+** (recommended: JDK 17 or 21)
- **Gradle 8.8+** (wrapper included)
- **IntelliJ IDEA 2024.1+** or **Android Studio 2024.1+**
- **Go 1.21+** (for Go code generation testing)

### Installation

```bash
# Clone the repository
git clone https://github.com/YOUR_ORG/CodeNodeIO.git
cd CodeNodeIO

# Verify Gradle setup
./gradlew --version

# Build all modules
./gradlew build

# Run tests
./gradlew test
```

### Running the Graph Editor (Compose Desktop)

```bash
./gradlew graphEditor:run
```

This launches the visual editor as a standalone Compose Desktop application.

### Running the IDE Plugin (Development)

```bash
./gradlew idePlugin:runIde
```

This opens a sandbox IntelliJ instance with the CodeNodeIO plugin installed for testing.

---

## Project Structure

CodeNodeIO is organized as a multi-module Kotlin Multiplatform project:

```
CodeNodeIO/
├── fbpDsl/              # Core FBP domain model and DSL
├── graphEditor/         # Visual editor (Compose Desktop)
├── circuitSimulator/    # Debugging/simulation tool
├── kotlinCompiler/      # KMP code generator (uses KotlinPoet 2.2.0)
├── goCompiler/          # Go code generator
├── idePlugin/           # IntelliJ Platform plugin integration
└── specs/               # Architecture & specifications
    └── 001-ide-plugin-platform/
        ├── spec.md           # Feature specification
        ├── plan.md           # Implementation plan
        ├── research.md       # Technical decisions
        ├── data-model.md     # Core entities
        ├── quickstart.md     # Developer setup guide
        └── tasks.md          # Implementation tasks
```

### Module Dependencies

```
fbpDsl (core domain)
  ├── graphEditor (Compose UI) → fbpDsl
  ├── circuitSimulator → fbpDsl, graphEditor
  ├── kotlinCompiler (KMP gen) → fbpDsl
  ├── goCompiler (Go gen) → fbpDsl
  └── idePlugin (IDE plugin) → all modules
```

---

## Development

### Building Specific Modules

```bash
# Build core FBP DSL
./gradlew fbpDsl:build

# Build graph editor
./gradlew graphEditor:build

# Build code generators
./gradlew kotlinCompiler:build
./gradlew goCompiler:build
```

### Running Tests

```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew fbpDsl:test
./gradlew graphEditor:test
```

### Code Quality

This project follows strict code quality standards:

- **Linting**: ktlint configuration in `.ktlint`
- **Formatting**: EditorConfig rules in `.editorconfig`
- **Code Style**: Kotlin Official style guide
- **Test Coverage**: >80% target per constitution
- **TDD**: Test-Driven Development mandatory for all features

### Version Lock

**IMPORTANT**: The following versions are pinned for reproducible builds:

| Dependency | Version | Why |
|-----------|---------|-----|
| Kotlin | 2.1.21 | KotlinPoet 2.2.0 requirement; K2 compiler |
| KotlinPoet | 2.2.0 | Type-safe code generation |
| Compose Desktop | 1.10.0 | Modern UI, K2 optimization |
| Coroutines | 1.8.0 | FBP execution model |
| IntelliJ Platform SDK | 2024.1 | IDE plugin framework |

Do **NOT** upgrade versions without re-validating the compatibility triangle.

---

## Contributing

We welcome contributions! Please follow these steps:

1. **Read the documentation**:
   - `specs/001-ide-plugin-platform/spec.md` - Feature specification
   - `specs/001-ide-plugin-platform/plan.md` - Implementation plan
   - `specs/001-ide-plugin-platform/quickstart.md` - Developer guide
   - `.specify/memory/constitution.md` - Project governance and standards

2. **Set up your development environment**:
   - Follow the Quick Start instructions above
   - Ensure all tests pass: `./gradlew test`

3. **Follow the development workflow**:
   - **TDD Required**: Write tests first (Red-Green-Refactor)
   - **Code Quality**: Run ktlint before committing
   - **Licensing**: NO GPL/LGPL/AGPL dependencies (Apache 2.0, MIT, BSD-3-Clause only)
   - **Documentation**: Update relevant docs for user-facing changes

4. **Create a Pull Request**:
   - Reference the relevant task from `specs/001-ide-plugin-platform/tasks.md`
   - Ensure all tests pass and coverage meets requirements
   - Follow the commit message format from existing commits

---

## License

This project is licensed under the **Apache License 2.0** - see the [LICENSE](LICENSE) file for details.

### Dependency Licensing Policy

**CRITICAL**: This project has strict licensing requirements:

- ✅ **Permitted**: Apache 2.0, MIT, BSD-3-Clause, EPL 2.0, MPL 2.0
- ❌ **Forbidden**: GPL (v2/v3), LGPL, AGPL (any version)

All dependencies are validated during build. License violations will **block** code generation.

---

## Documentation

- **[Feature Specification](specs/001-ide-plugin-platform/spec.md)**: User stories and requirements
- **[Implementation Plan](specs/001-ide-plugin-platform/plan.md)**: Technical architecture
- **[Data Model](specs/001-ide-plugin-platform/data-model.md)**: Core FBP entities
- **[API Contracts](specs/001-ide-plugin-platform/contracts/ide-plugin-api.md)**: IDE plugin interfaces
- **[Quickstart Guide](specs/001-ide-plugin-platform/quickstart.md)**: Developer setup
- **[Constitution](`.specify/memory/constitution.md)**: Project governance

---

## Roadmap

**Phase 1: Setup** ✅ (Complete)
- Multi-module project structure
- Build configuration and dependencies
- Code quality tooling

**Phase 2: Foundation** (In Progress)
- Core FBP domain model (InformationPacket, Node, Port, Connection, FlowGraph)
- DSL implementation with infix functions
- Module build configurations

**Phase 3: Visual Editor** (Planned)
- Flow graph canvas with drag-and-drop
- Node palette and properties panel
- Graph serialization/deserialization

**Phase 4: Code Generation** (Planned)
- KMP code generator (Android, iOS, Web)
- Go code generator (backend services)
- License validation

**Phase 5: IDE Integration** (Planned)
- IntelliJ Platform plugin integration
- Tool windows and actions
- File type support for `.flow.kts`

**Phase 6: Circuit Simulator** (Planned)
- Flow execution visualization
- Debugging controls (pause/resume/speed)
- IP flow animation

See `specs/001-ide-plugin-platform/tasks.md` for detailed task breakdown.

---

## Support

For setup questions, build issues, or technical guidance:

1. Check the [Quickstart Guide](specs/001-ide-plugin-platform/quickstart.md)
2. Review [Technical Decisions](specs/001-ide-plugin-platform/research.md)
3. Consult the [Implementation Plan](specs/001-ide-plugin-platform/plan.md)
4. Open an issue with detailed context

---

## Acknowledgments

- **J. Paul Morrison** - Flow-based Programming concepts and philosophy
- **JetBrains** - IntelliJ Platform SDK and Kotlin ecosystem
- **Square** - KotlinPoet for type-safe code generation

---

**Built with ❤️ using Kotlin, Compose Multiplatform, and Flow-based Programming principles**

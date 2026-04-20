# Feature Specification: Generate UI-FBP Interface

**Feature Branch**: `075-generate-ui-fbp-interface`
**Created**: 2026-04-19
**Status**: Draft
**Input**: User description: "Create a function called Generate UI-FBP that takes a Compose UI file and generates the files that act as the interface to a flowgraph, including ViewModel, State, a CodeNode Source file, and a CodeNode Sink file."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Generate FBP Interface from a UI File (Priority: P1)

A developer has written a Compose UI file (e.g., DemoUI.kt) in a module's `/userInterface` folder. The UI composable takes a ViewModel as a parameter, where the ViewModel's input functions (e.g., `emit(numA, numB)`) represent data entering the flowgraph, and its state flows (e.g., `results: StateFlow<CalculationResults?>`) represent data leaving the flowgraph for display. The developer triggers "Generate UI-FBP" and the system analyzes the UI file to produce four generated files: a ViewModel class, a State object, a CodeNode Source definition, and a CodeNode Sink definition. These files form the complete interface layer between the Compose UI and a flow-based processing pipeline.

**Why this priority**: This is the core value of the feature — automating the creation of the UI-to-FBP bridge files that developers currently write by hand. Without this, every new UI module requires manually creating four boilerplate files that follow the same structural pattern.

**Independent Test**: Given a UI file like DemoUI.kt that takes a ViewModel with an `emit(numA: Double, numB: Double)` function and a `results: StateFlow<CalculationResults?>` state property, trigger "Generate UI-FBP". Verify that four files are generated: `{Name}ViewModel.kt`, `{Name}State.kt`, a Source CodeNode `.kt` file, and a Sink CodeNode `.kt` file. Each file should compile and follow the established module patterns.

**Acceptance Scenarios**:

1. **Given** a UI file exists at `{Module}/src/commonMain/kotlin/.../userInterface/{Name}.kt` with a composable function that accepts a ViewModel parameter, **When** the developer triggers "Generate UI-FBP", **Then** the system generates `{Name}ViewModel.kt`, `{Name}State.kt`, a Source CodeNode file, and a Sink CodeNode file in the appropriate module directories
2. **Given** the UI composable's ViewModel parameter has input functions (methods the UI calls to send data), **When** the files are generated, **Then** the Source CodeNode has output ports matching the input function parameters (the data types the UI emits into the graph)
3. **Given** the UI composable's ViewModel parameter has observable state properties (StateFlows the UI collects for display), **When** the files are generated, **Then** the Sink CodeNode has input ports matching the state flow types (the data types the graph produces for display)
4. **Given** the generated files, **When** they are compiled alongside the original UI file, **Then** the module compiles without errors and the ViewModel correctly bridges the UI to the Source and Sink nodes via the State object

---

### User Story 2 - Analyze ViewModel Parameter Signature (Priority: P2)

The system parses the UI composable function to extract the ViewModel parameter type and its interface: which functions send data into the graph (Source ports) and which state flows receive data from the graph (Sink ports). This analysis drives the generation of correctly-typed Source and Sink CodeNode definitions.

**Why this priority**: Accurate parsing of the ViewModel signature is the foundation for correct code generation. If the system misidentifies input functions or state flows, the generated files will not match the UI's expectations.

**Independent Test**: Given DemoUI.kt where `DemoUIViewModel` has `emit(numA: Double, numB: Double)` and `results: StateFlow<CalculationResults?>`, the system should extract: Source outputs = `[numA: Double, numB: Double]`, Sink inputs = `[results: CalculationResults]`.

**Acceptance Scenarios**:

1. **Given** a UI composable with `fun DemoUI(viewModel: DemoUIViewModel, ...)`, **When** the system analyzes the file, **Then** it identifies `DemoUIViewModel` as the ViewModel type and derives the module name as "DemoUI"
2. **Given** the ViewModel has a function `emit(numA: Double, numB: Double)`, **When** the system analyzes the signature, **Then** it identifies two Source output ports: `numA` of type `Double` and `numB` of type `Double`
3. **Given** the ViewModel has a property `val results: StateFlow<CalculationResults?>`, **When** the system analyzes the signature, **Then** it identifies one Sink input port: `results` of type `CalculationResults`
4. **Given** a ViewModel with multiple state flow properties, **When** the system analyzes the signature, **Then** each state flow becomes a separate Sink input port with the correct type

---

### User Story 3 - Integration with Generate Module Workflow (Priority: P3)

The "Generate UI-FBP" function follows the patterns established by "Generate Module". It can be triggered from the graph editor as a distinct action. The generated CodeNode Source and Sink files are discoverable by the node palette and can be used in flow graphs. The generated ViewModel and State files follow the same conventions as other generated modules (EdgeArtFilter, WeatherForecast, StopWatch).

**Why this priority**: Integration with the existing toolchain ensures the generated files are immediately usable. Without this, the developer would need to manually register nodes or adjust the generation output.

**Independent Test**: After generating the UI-FBP interface, open the graph editor. The Source and Sink CodeNodes should appear in the node palette. Create a flow graph connecting them with processing nodes in between. The resulting pipeline should be executable.

**Acceptance Scenarios**:

1. **Given** the UI-FBP interface has been generated, **When** the developer opens the node palette in the graph editor, **Then** the Source and Sink CodeNodes for the module are listed and available for use
2. **Given** the generated files follow the established module pattern, **When** "Generate Module" is subsequently run on a flow graph that uses these Source/Sink nodes, **Then** the full module compiles and the runtime pipeline connects the UI to the processing graph

---

### Edge Cases

- What happens when the UI file has no ViewModel parameter? The system reports an error indicating that a ViewModel parameter is required for UI-FBP generation.
- What happens when the ViewModel has no input functions (no Source data)? The system generates only the Sink CodeNode and omits the Source CodeNode, with a warning that the graph will have no UI-driven inputs.
- What happens when the ViewModel has no state flow properties (no Sink data)? The system generates only the Source CodeNode and omits the Sink CodeNode, with a warning that the graph will produce no observable output for the UI.
- What happens when generated files already exist? Existing files are overwritten (same as "Generate Module" behavior for always-regenerated files), since these are generated artifacts, not user-authored code.
- What happens when the ViewModel references IP types from the module's `/iptypes` directory? The generated Source/Sink ports use those IP types, and appropriate imports are included in the generated files.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST provide a "Generate UI-FBP" action that accepts a Compose UI file as input
- **FR-002**: The system MUST parse the UI composable function to identify the ViewModel parameter and extract its interface (input functions and state flow properties)
- **FR-003**: The system MUST generate a `{Name}ViewModel.kt` file that extends ViewModel, delegates to the State object, and exposes control methods matching the UI's input functions
- **FR-004**: The system MUST generate a `{Name}State.kt` object file containing MutableStateFlow/StateFlow pairs for each Source output and Sink input, following the established pattern of internal mutable flows with public read-only counterparts
- **FR-005**: The system MUST generate a CodeNode Source definition file with output ports matching the ViewModel's input function parameters (the data the UI emits into the graph)
- **FR-006**: The system MUST generate a CodeNode Sink definition file with input ports matching the ViewModel's state flow properties (the data the graph produces for UI display)
- **FR-007**: The generated files MUST be placed in the correct module directory structure: ViewModel and State in the base package, CodeNode definitions in the appropriate nodes directory
- **FR-008**: The generated Source and Sink CodeNodes MUST be discoverable by the graph editor's node palette
- **FR-009**: The generated files MUST compile without errors when combined with the original UI file and the module's IP type definitions
- **FR-010**: The system MUST handle IP types referenced in the ViewModel (e.g., `CalculationResults`) by generating correct imports in the Source and Sink files

### Key Entities

- **UI Composable**: The user-authored Compose function that defines the visual interface. Contains a ViewModel parameter whose signature drives the generation.
- **ViewModel**: Generated bridge class connecting the UI to the FBP pipeline. Exposes input functions (emit data to Source) and state flows (display data from Sink).
- **State Object**: Generated singleton holding MutableStateFlow/StateFlow pairs for all data crossing the UI-FBP boundary.
- **Source CodeNode**: Generated node definition with output ports corresponding to the ViewModel's input function parameters. Represents data flowing from the UI into the graph.
- **Sink CodeNode**: Generated node definition with input ports corresponding to the ViewModel's state flow properties. Represents data flowing from the graph back to the UI for display.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Given a UI file following the established pattern (ViewModel parameter with input functions and state flows), the system generates all four interface files in under 5 seconds
- **SC-002**: 100% of generated files compile without errors when combined with the source UI file and module IP types
- **SC-003**: Generated Source and Sink CodeNodes appear in the graph editor node palette without additional manual registration
- **SC-004**: A developer can go from a hand-written UI file to a working FBP-connected module by running "Generate UI-FBP" followed by "Generate Module" — no manual boilerplate authoring required
- **SC-005**: The generated ViewModel, State, Source, and Sink files match the structural patterns established by existing modules (EdgeArtFilter, WeatherForecast, StopWatch) as verified by pattern comparison

## Assumptions

- The UI file exists in a `/userInterface` subdirectory within a module's KMP source structure (e.g., `{Module}/src/commonMain/kotlin/.../userInterface/{Name}.kt`)
- The UI composable function takes a ViewModel as its first non-Modifier parameter, and that ViewModel type name follows the convention `{Name}ViewModel`
- Input functions on the ViewModel are identified by convention: public functions that are not lifecycle methods (not `start`, `stop`, `pause`, `resume`, `reset`) and that take typed parameters
- State flow properties on the ViewModel are identified by their `StateFlow<T>` return type
- The DemoUI file in the TestModule of the DemoProject serves as the reference prototype for the expected input/output pattern
- IP types referenced in the ViewModel are assumed to exist in the module's `/iptypes` directory or in a shared IP types module
- The "Generate UI-FBP" action is a precursor to "Generate Module" — it creates the boundary nodes and ViewModel, after which the developer designs the processing graph and then runs "Generate Module" to create the full runtime

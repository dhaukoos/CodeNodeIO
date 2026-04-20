# Tasks: Generate UI-FBP Interface

**Input**: Design documents from `/specs/075-generate-ui-fbp-interface/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, quickstart.md

**Tests**: Unit tests are included for the parser and generators (foundational infrastructure).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: Create the data model (`UIFBPSpec`, `PortInfo`) and the UI file parser that extracts the ViewModel interface from usage patterns. All generators depend on this.

**⚠️ CRITICAL**: No generator or integration work can begin until this phase is complete

### Tests

- [X] T001 [P] Add test: UIComposableParser extracts module name and ViewModel type from `@Composable fun DemoUI(viewModel: DemoUIViewModel, ...)` in `flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/parser/UIComposableParserTest.kt`
- [X] T002 [P] Add test: UIComposableParser extracts Source outputs from `viewModel.emit(a, b)` call — identifies method name "emit" and parameter names/types `[numA: Double, numB: Double]` in `flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/parser/UIComposableParserTest.kt`
- [X] T003 [P] Add test: UIComposableParser extracts Sink inputs from `viewModel.results.collectAsState()` — identifies property name "results" and type `CalculationResults` in `flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/parser/UIComposableParserTest.kt`
- [X] T004 [P] Add test: UIComposableParser returns error when UI file has no ViewModel parameter in `flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/parser/UIComposableParserTest.kt`

### Implementation

- [X] T005 Create `UIFBPSpec` and `PortInfo` data classes in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/parser/UIFBPSpec.kt`
- [X] T006 Create `UIComposableParser` class with `parse(fileContent: String): UIFBPParseResult` method that uses regex to extract module name, ViewModel type, Source outputs (from emit-style method calls), and Sink inputs (from collectAsState property accesses) in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/parser/UIComposableParser.kt`
- [X] T007 Run tests: `./gradlew :flowGraph-generate:jvmTest --tests "*UIComposableParser*"` to verify parser against DemoUI.kt content

**Checkpoint**: Parser correctly extracts ViewModel interface from UI file usage patterns. Generators can now proceed.

---

## Phase 2: User Story 1 — Generate FBP Interface from a UI File (Priority: P1) 🎯 MVP

**Goal**: Given a UI file, generate all four interface files: ViewModel, State, Source CodeNode, and Sink CodeNode. The generated files compile alongside the original UI file.

**Independent Test**: Run the generator on DemoUI.kt content. Verify four files are produced with correct structure, ports, and imports. Copy generated files into TestModule and compile.

### Tests

- [ ] T008 [P] [US1] Add test: UIFBPViewModelGenerator produces ViewModel class extending ViewModel with emit method and StateFlow properties matching UIFBPSpec in `flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPGeneratorTest.kt`
- [ ] T009 [P] [US1] Add test: UIFBPStateGenerator produces State object with MutableStateFlow/StateFlow pairs for all Source outputs and Sink inputs in `flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPGeneratorTest.kt`
- [ ] T010 [P] [US1] Add test: UIFBPSourceCodeNodeGenerator produces Source CodeNode with correct output ports and CodeNodeType.SOURCE category in `flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPGeneratorTest.kt`
- [ ] T011 [P] [US1] Add test: UIFBPSinkCodeNodeGenerator produces Sink CodeNode with correct input ports and CodeNodeType.SINK category in `flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPGeneratorTest.kt`

### Implementation

- [ ] T012 [P] [US1] Create `UIFBPStateGenerator` with `generate(spec: UIFBPSpec): String` that produces a `{Name}State` object with MutableStateFlow/StateFlow pairs for Source outputs and Sink inputs, plus a `reset()` function, in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPStateGenerator.kt`
- [ ] T013 [P] [US1] Create `UIFBPViewModelGenerator` with `generate(spec: UIFBPSpec): String` that produces a `{Name}ViewModel` class extending ViewModel, exposing StateFlow properties from State (Sink data) and an emit method that writes to State (Source data), in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPViewModelGenerator.kt`
- [ ] T014 [P] [US1] Create `UIFBPSourceCodeNodeGenerator` with `generate(spec: UIFBPSpec): String` that produces a `{Name}SourceCodeNode` object implementing CodeNodeDefinition with SOURCE category, output ports from spec.sourceOutputs, and processing logic that collects from State flows, in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPSourceCodeNodeGenerator.kt`
- [ ] T015 [P] [US1] Create `UIFBPSinkCodeNodeGenerator` with `generate(spec: UIFBPSpec): String` that produces a `{Name}SinkCodeNode` object implementing CodeNodeDefinition with SINK category, input ports from spec.sinkInputs, and processing logic that writes to State flows, in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPSinkCodeNodeGenerator.kt`
- [ ] T016 [US1] Create orchestrator `UIFBPInterfaceGenerator` with `generateAll(spec: UIFBPSpec, outputDir: File): UIFBPGenerateResult` that calls all four generators, writes files to correct locations (ViewModel + State in base package, Source + Sink in nodes/ subdirectory), and returns result tracking, in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPInterfaceGenerator.kt`
- [ ] T017 [US1] Run all generator tests: `./gradlew :flowGraph-generate:jvmTest --tests "*UIFBP*"`

**Checkpoint**: All four generators produce correct output. The orchestrator writes files to the correct locations. Tests pass against DemoUI-derived UIFBPSpec.

---

## Phase 3: User Story 2 — Analyze ViewModel Parameter Signature (Priority: P2)

**Goal**: End-to-end parsing of a real UI file (DemoUI.kt) produces correct UIFBPSpec with accurate Source outputs and Sink inputs including IP type resolution.

**Independent Test**: Feed the actual DemoUI.kt file content to the parser. Verify the resulting UIFBPSpec matches: sourceOutputs = `[numA: Double, numB: Double]`, sinkInputs = `[results: CalculationResults]`.

### Implementation

- [ ] T018 [US2] Add integration test: parse the actual DemoUI.kt file content from TestModule and verify UIFBPSpec has moduleName="DemoUI", viewModelTypeName="DemoUIViewModel", sourceOutputs=[numA:Double, numB:Double], sinkInputs=[results:CalculationResults] in `flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/parser/UIComposableParserTest.kt`
- [ ] T019 [US2] Add integration test: generate all four files from DemoUI-derived UIFBPSpec and verify each file contains expected class/object names, port declarations, and imports in `flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPGeneratorTest.kt`
- [ ] T020 [US2] Handle IP type resolution in UIComposableParser — when the parser finds a type like `CalculationResults`, resolve it by scanning the module's `/iptypes` directory for a matching `@TypeName` or class name in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/parser/UIComposableParser.kt`
- [ ] T021 [US2] Run full test suite: `./gradlew :flowGraph-generate:jvmTest`

**Checkpoint**: Parser handles real UI files with IP type references. End-to-end parsing + generation produces correct output for DemoUI prototype.

---

## Phase 4: User Story 3 — Integration with Generate Module Workflow (Priority: P3)

**Goal**: "Generate UI-FBP" is triggerable from the graph editor toolbar. Generated CodeNode files are discoverable by the node palette.

**Independent Test**: Launch graph editor, click "Generate UI-FBP", select DemoUI.kt, verify files are generated and Source/Sink nodes appear in the palette.

### Implementation

- [ ] T022 [US3] Add `onGenerateUIFBP: () -> Unit` callback parameter to `TopToolbar` and add "Generate UI-FBP" button between "Generate Module" and the divider in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/TopToolbar.kt`
- [ ] T023 [US3] Add `showGenerateUIFBPDialog` state variable and wire `onGenerateUIFBP` callback in `GraphEditorApp.kt` to set it to true in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphEditorApp.kt`
- [ ] T024 [US3] Add Generate UI-FBP handler in `GraphEditorDialogs.kt` — when `showGenerateUIFBPDialog` is true, show a file chooser for `.kt` files, parse the selected file with `UIComposableParser`, run `UIFBPInterfaceGenerator.generateAll()`, and show status message with results in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/GraphEditorDialogs.kt`
- [ ] T025 [US3] Compile and run tests: `./gradlew :graphEditor:compileKotlinJvm :graphEditor:jvmTest`

**Checkpoint**: "Generate UI-FBP" button works end-to-end. Generated CodeNodes are discoverable by the palette via existing NodeDefinitionRegistry scanning.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Full verification across all quickstart scenarios

- [ ] T026 Run full test suite: `./gradlew :flowGraph-generate:jvmTest :graphEditor:jvmTest`
- [ ] T027 Run quickstart.md verification scenarios VS1–VS9
- [ ] T028 Validate generated files against DemoUI hand-written prototypes: compare `DemoUIViewModel.kt`, `DemoUIState.kt` structure with generated output

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: No dependencies — can start immediately. BLOCKS all user stories (parser must work before generators can be tested).
- **User Story 1 (Phase 2)**: Depends on Foundational — four generators need UIFBPSpec from parser
- **User Story 2 (Phase 3)**: Depends on US1 — integration tests validate end-to-end parsing + generation
- **User Story 3 (Phase 4)**: Depends on US1 — toolbar wiring needs generators to exist
- **Polish (Phase 5)**: Depends on all user stories being complete

### Within Each Phase

- Tests (T001–T004, T008–T011) before implementation
- All four generators (T012–T015) can run in parallel — different files, no dependencies
- Orchestrator (T016) depends on all four generators
- Toolbar integration (T022–T024) is sequential (TopToolbar → App → Dialogs)

### Parallel Opportunities

```text
# Foundational tests (all in same test file but independent):
T001, T002, T003, T004  (parser tests)

# US1 generator tests (same test file but independent):
T008, T009, T010, T011  (generator tests)

# US1 generators (different files, no dependencies):
T012, T013, T014, T015  (4 generators in parallel)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Foundational (T001–T007) — parser works
2. Complete Phase 2: User Story 1 (T008–T017) — generators produce correct files
3. **STOP and VALIDATE**: Feed DemoUI.kt to parser, generate files, verify they match prototypes
4. This is independently valuable — developers can run the generator manually even without toolbar integration

### Incremental Delivery

1. Foundational → Parser extracts UIFBPSpec from UI files
2. User Story 1 → Four generators produce correct output (MVP!)
3. User Story 2 → End-to-end validation with real files + IP type resolution
4. User Story 3 → Toolbar button for graph editor integration
5. Polish → Full verification pass

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- The DemoUI.kt in TestModule is the reference prototype — all tests validate against it
- The ViewModel file does NOT exist as input — it is generated. The parser works solely from UI file usage patterns.
- IP types referenced in the ViewModel (e.g., `CalculationResults`) must be resolved from the module's `/iptypes` directory
- Commit after each phase completion

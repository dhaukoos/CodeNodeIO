# Research: Generator CodeNode Wrappers

**Feature**: 079-generator-codenode-wrappers
**Date**: 2026-04-23

## R1: Wrapper Pattern

**Decision**: Each generator wrapper is a Kotlin `object` implementing `CodeNodeDefinition` with `category = CodeNodeType.TRANSFORMER` (1 input → 1 output). The input port accepts the spec/config object. The output port produces the generated file content as a String.

**Rationale**: TRANSFORMER is the correct category — each generator transforms a spec into generated code. Using `object` (singleton) matches the existing CodeNode definition pattern (e.g., `ImagePickerCodeNode`, `UserProfilesDisplayCodeNode`).

**Runtime pattern**: Uses `CodeNodeFactory.createContinuousTransformer<Any, String>` — input is `Any` (cast to FlowGraph/EntityModuleSpec/UIFBPSpec at runtime), output is `String`.

## R2: CodeNodeType Category

**Decision**: Use existing `CodeNodeType.TRANSFORMER` for generator CodeNodes. Do not add a new category.

**Rationale**: Adding a new `GENERATOR` category to `CodeNodeType` would require updating exhaustive `when` expressions across multiple files (same issue as adding `INTERNAL` to PlacementLevel in feature 073). The generators are semantically transformers (input → output), so TRANSFORMER fits. The description field provides differentiation in the palette.

**Alternatives considered**:
- New `GENERATOR` category: Rejected — too many `when` expression changes for marginal benefit.
- `API_ENDPOINT` category: Rejected — semantically wrong.

## R3: File Placement

**Decision**: Place wrappers in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/nodes/` — a `nodes/` subdirectory within the generate module.

**Rationale**: This follows the "eat our own dogfood" principle — the tool's internal modules should reflect the same architecture that the tool creates for external projects. Generated modules place their CodeNode definitions in `nodes/`, so the tool's own CodeNode definitions should follow the same convention. This makes the tool-internal structure a living example of the architecture it promotes.

**Alternatives considered**:
- New `codegen/` directory: Rejected — introduces a non-standard directory name that doesn't match the architecture the tool generates for users.
- In `generator/` alongside the generators: Rejected — mixes generation logic with CodeNode wrapping.

## R4: Discovery

**Decision**: Generator CodeNode wrappers are discovered via the existing `NodeDefinitionRegistry` classpath scanning (compiled nodes via INSTANCE field). Since they're `object` singletons in the flowGraph-generate module (which is on the classpath), they'll be automatically discoverable.

**Rationale**: No additional registration or scanning changes needed. The wrappers follow the same `object : CodeNodeDefinition` pattern that NodeDefinitionRegistry already handles.

## R5: Naming Convention

**Decision**: Wrapper names match the generator class names for clarity. Each wrapper object is named `{GeneratorName}Node` (e.g., `FlowKtGeneratorNode`, `RuntimeControllerGeneratorNode`).

**Naming table**:

| Generator Class | Wrapper Object Name | CodeNode Display Name |
|----------------|--------------------|--------------------|
| FlowKtGenerator | FlowKtGeneratorNode | "FlowKtGenerator" |
| RuntimeFlowGenerator | RuntimeFlowGeneratorNode | "RuntimeFlowGenerator" |
| RuntimeControllerGenerator | RuntimeControllerGeneratorNode | "RuntimeControllerGenerator" |
| RuntimeControllerInterfaceGenerator | RuntimeControllerInterfaceGeneratorNode | "RuntimeControllerInterfaceGenerator" |
| RuntimeControllerAdapterGenerator | RuntimeControllerAdapterGeneratorNode | "RuntimeControllerAdapterGenerator" |
| RuntimeViewModelGenerator | RuntimeViewModelGeneratorNode | "RuntimeViewModelGenerator" |
| UserInterfaceStubGenerator | UserInterfaceStubGeneratorNode | "UserInterfaceStubGenerator" |
| EntityCUDCodeNodeGenerator | EntityCUDGeneratorNode | "EntityCUDGenerator" |
| EntityRepositoryCodeNodeGenerator | EntityRepositoryGeneratorNode | "EntityRepositoryGenerator" |
| EntityDisplayCodeNodeGenerator | EntityDisplayGeneratorNode | "EntityDisplayGenerator" |
| EntityPersistenceGenerator | EntityPersistenceGeneratorNode | "EntityPersistenceGenerator" |
| UIFBPStateGenerator | UIFBPStateGeneratorNode | "UIFBPStateGenerator" |
| UIFBPViewModelGenerator | UIFBPViewModelGeneratorNode | "UIFBPViewModelGenerator" |
| UIFBPSourceCodeNodeGenerator | UIFBPSourceGeneratorNode | "UIFBPSourceGenerator" |
| UIFBPSinkCodeNodeGenerator | UIFBPSinkGeneratorNode | "UIFBPSinkGenerator" |

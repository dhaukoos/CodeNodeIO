# Quickstart Verification: Generator CodeNode Wrappers

**Feature**: 079-generator-codenode-wrappers
**Date**: 2026-04-23

## Prerequisites

- Branch `079-generator-codenode-wrappers` checked out
- Feature 078 (module scaffolding extraction) merged

## Verification Scenarios

### VS1: Compilation

**Steps**:
1. Run `./gradlew :flowGraph-generate:compileKotlinJvm`

**Expected**: All 15 wrapper objects compile.

### VS2: Unit Tests

**Steps**:
1. Run `./gradlew :flowGraph-generate:jvmTest --tests "*GeneratorNode*"`

**Expected**: All wrapper tests pass — each produces non-empty output matching the underlying generator.

### VS3: Module-Level Wrappers

**Steps**:
1. Verify FlowKtGeneratorNode, RuntimeFlowGeneratorNode, RuntimeControllerGeneratorNode, RuntimeControllerInterfaceGeneratorNode, RuntimeControllerAdapterGeneratorNode, RuntimeViewModelGeneratorNode, UserInterfaceStubGeneratorNode all exist and implement CodeNodeDefinition

**Expected**: 7 module-level wrappers, each with TRANSFORMER category, 1 input port, 1 output port.

### VS4: Entity Wrappers

**Steps**:
1. Verify EntityCUDGeneratorNode, EntityRepositoryGeneratorNode, EntityDisplayGeneratorNode, EntityPersistenceGeneratorNode exist

**Expected**: 4 entity wrappers with EntityModuleSpec input port.

### VS5: UI-FBP Wrappers

**Steps**:
1. Verify UIFBPStateGeneratorNode, UIFBPViewModelGeneratorNode, UIFBPSourceGeneratorNode, UIFBPSinkGeneratorNode exist

**Expected**: 4 UI-FBP wrappers with UIFBPSpec input port.

### VS6: Existing Tests Unaffected

**Steps**:
1. Run `./gradlew :flowGraph-generate:jvmTest`

**Expected**: All existing tests pass — zero regressions.

### VS7: Node Palette Discovery

**Steps**:
1. Launch graph editor
2. Open node palette
3. Search for "Generator"

**Expected**: All 15 generator CodeNodes visible in the palette.

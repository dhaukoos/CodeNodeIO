# Research: Remove Single-Invocation Patterns

**Feature**: 024-remove-single-invocation
**Date**: 2026-02-20

## R1: ProcessingLogic Dependency Analysis

**Decision**: ProcessingLogic interface and `processingLogic` property on CodeNode MUST be retained.

**Rationale**: ProcessingLogic is not a "single-invocation only" artifact — it is the bridge between the visual editor and user-written custom node behavior. It is used by:
- **StopWatch module**: `TimerEmitterComponent` and `DisplayReceiverComponent` both implement `ProcessingLogic` as production components
- **kotlinCompiler module**: `ProcessingLogicStubGenerator` generates stub files; `FlowGraphFactoryGenerator` instantiates ProcessingLogic components; `ModuleGenerator` references ProcessingLogic for code generation
- **graphEditor module**: `ModuleSaveService` generates ProcessingLogic stubs on save; `CompilationValidator` validates ProcessingLogic classes exist; `PropertiesPanel` shows `_useCaseClass` property for ProcessingLogic binding

**Alternatives considered**:
- Remove ProcessingLogic → breaks StopWatch, code generation, module save, compilation validation. Not viable.
- Rename to something more descriptive → out of scope for this feature.

**Impact on spec**: US4 (Remove ProcessingLogic Interface) is **not feasible** and must be dropped entirely.

## R2: InformationPacket Dependency Analysis

**Decision**: `InformationPacket` and `InformationPacketFactory` MUST be retained.

**Rationale**: InformationPacket is a core data type used across the entire framework — type system, ports, IP palette, circuit simulation, code generation, and continuous runtime tests. It is not specific to single-invocation patterns.

## R3: Generic `create` Factory Method

**Decision**: The generic `CodeNodeFactory.create()` method MUST be retained.

**Rationale**: This is the most flexible factory method, used for creating CodeNodes with arbitrary port configurations. While it accepts an optional `processingLogic` parameter, it is the only way to create fully custom CodeNode instances. The `_useCaseClass` configuration pattern in the graphEditor depends on creating CodeNodes that later get ProcessingLogic bound via code generation.

**Callers**: Used in test code (ContinuousFactoryTest T043). Also indirectly depended upon by the graphEditor's node creation flow (DragAndDropHandler creates CodeNodes using the model constructor directly, which follows the same pattern).

## R4: Deprecated Factory Methods (createGenerator, createSink)

**Decision**: Safe to remove. Only callers are backward-compatibility tests T041 and T042 in ContinuousFactoryTest.kt.

**Rationale**: These were explicitly deprecated with `@Deprecated` annotations and `ReplaceWith` directives pointing to continuous counterparts. No production code uses them.

## R5: Non-Deprecated Single-Invocation Factory Methods

**Decision**: Safe to remove `createTransformer`, `createFilter`, `createSplitter`, `createMerger`, `createValidator`.

**Rationale**: Exhaustive search found zero callers in any module (fbpDsl, graphEditor, kotlinCompiler, StopWatch). They only appear in their own definitions inside CodeNodeFactory.kt. No tests exercise them directly. They create CodeNodes with embedded ProcessingLogic, which is a pattern the codebase no longer uses — the graphEditor creates CodeNodes via model constructors and binds ProcessingLogic externally through `_useCaseClass` configuration.

## R6: TypedUseCases and ExampleUseCases

**Decision**: Safe to remove both `TypedUseCases.kt` and `ExampleUseCases.kt`.

**Rationale**: TypedUseCases defines abstract base classes (TransformerUseCase, FilterUseCase, etc.) that only ExampleUseCases.kt uses. ExampleUseCases.kt has no callers — it exists purely as documentation/example code. Neither is imported by any production or test code outside the `usecase/` package. GenericNodeGeneratorTest.kt references UseCase delegation in comments only, not in code.

## R7: LifecycleAwareUseCases

**Decision**: Safe to remove.

**Rationale**: Zero callers outside the file itself. Contains LifecycleAwareUseCase, LifecycleManager, LifecycleDecorator, and example classes (DatabaseUseCase, CachedUseCase, BufferedUseCase). None are imported anywhere.

## R8: UseCase-Pattern-Guide.md

**Decision**: Safe to remove.

**Rationale**: Only referenced from `specs/001-ide-plugin-platform/plan.md` (a historical spec). Not linked from any code, README, or active documentation.

## R9: Backward-Compatibility Tests

**Decision**: Remove tests T041, T042, and T043 from ContinuousFactoryTest.kt.

**Rationale**: These tests specifically verify that deprecated/legacy single-invocation methods still work. Once those methods are removed, the tests must go too. All other tests in the file verify continuous factory methods and should remain.

## Summary: Removal Scope

| Item | Remove? | Reason |
|------|---------|--------|
| `createGenerator` (deprecated) | YES | Zero production callers |
| `createSink` (deprecated) | YES | Zero production callers |
| `createTransformer` | YES | Zero callers anywhere |
| `createFilter` | YES | Zero callers anywhere |
| `createSplitter` | YES | Zero callers anywhere |
| `createMerger` | YES | Zero callers anywhere |
| `createValidator` | YES | Zero callers anywhere |
| `TypedUseCases.kt` | YES | Only ExampleUseCases depends |
| `ExampleUseCases.kt` | YES | Zero callers |
| `LifecycleAwareUseCases.kt` | YES | Zero callers |
| `UseCase-Pattern-Guide.md` | YES | Not actively referenced |
| Tests T041-T043 | YES | Test removed methods |
| `ProcessingLogic` interface | NO | Production code depends on it |
| `processingLogic` property | NO | Production code depends on it |
| `create()` generic factory | NO | Used for flexible node creation |
| `InformationPacket` | NO | Core data type |
| `ProcessingLogicStubGenerator` | NO | Used by ModuleSaveService |

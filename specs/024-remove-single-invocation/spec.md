# Feature Specification: Remove Single-Invocation Patterns

**Feature Branch**: `024-remove-single-invocation`
**Created**: 2026-02-20
**Status**: Draft
**Input**: User description: "Remove single-invocation patterns. Remove code designed for and used only by single-invocation patterns."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Remove Deprecated Factory Methods (Priority: P1)

As a developer maintaining the CodeNodeIO codebase, I want to remove the deprecated `createGenerator` and `createSink` factory methods so that the codebase has a single, clear way to create generators and sinks (the continuous versions), eliminating confusion about which factory method to use.

These two methods are explicitly marked `@Deprecated` with `ReplaceWith` pointing to their continuous counterparts (`createContinuousGenerator`, `createContinuousSink`). They return `CodeNode` instead of `NodeRuntime` and are only referenced in backward-compatibility tests.

**Why this priority**: These are the only methods explicitly flagged for removal via deprecation annotations. Removing them is the clearest, lowest-risk cleanup and provides immediate signal that the continuous pattern is the sole supported approach.

**Independent Test**: Can be verified by deleting the two deprecated methods and their backward-compatibility tests, then confirming the project compiles and all remaining tests pass.

**Acceptance Scenarios**:

1. **Given** the `createGenerator` deprecated factory method exists, **When** it is removed along with its backward-compatibility test, **Then** the project compiles successfully and all remaining tests pass.
2. **Given** the `createSink` deprecated factory method exists, **When** it is removed along with its backward-compatibility test, **Then** the project compiles successfully and all remaining tests pass.
3. **Given** both deprecated methods are removed, **When** a developer searches the codebase for `createGenerator` or `createSink`, **Then** only the continuous versions (`createContinuousGenerator`, `createContinuousSink`) appear.

---

### User Story 2 - Remove Single-Invocation UseCase Classes (Priority: P2)

As a developer maintaining the CodeNodeIO codebase, I want to remove the typed UseCase base classes (`TransformerUseCase`, `FilterUseCase`, `ValidatorUseCase`, `SplitterUseCase`, `MergerUseCase`, `GeneratorUseCase`, `SinkUseCase`) and their lifecycle-aware variants (`LifecycleAwareUseCase`, `DatabaseUseCase`, `CachedUseCase`, `BufferedUseCase`) so that the codebase does not carry unused abstractions that exist only for the single-invocation execution model.

These classes implement the `ProcessingLogic` interface to wrap single-invocation behavior. None of them are used by the graphEditor or any production runtime code — they exist only as framework abstractions and examples.

**Why this priority**: These are standalone files with no production callers. Removing them reduces cognitive overhead and prevents new developers from accidentally building on the deprecated pattern.

**Independent Test**: Can be verified by deleting the UseCase files and their example implementations, then confirming the project compiles and all remaining tests pass.

**Acceptance Scenarios**:

1. **Given** the typed UseCase base classes exist in `TypedUseCases.kt`, **When** the file is removed, **Then** the project compiles and all remaining tests pass.
2. **Given** the lifecycle-aware UseCase classes exist in `LifecycleAwareUseCases.kt`, **When** the file is removed, **Then** the project compiles and all remaining tests pass.
3. **Given** the example UseCase implementations exist in `ExampleUseCases.kt`, **When** the file is removed, **Then** the project compiles and all remaining tests pass.
4. **Given** a UseCase pattern guide exists in documentation, **When** the documentation file is removed or updated, **Then** no references to removed classes remain in the codebase.

---

### User Story 3 - Remove Remaining Single-Invocation Factory Methods (Priority: P3)

As a developer maintaining the CodeNodeIO codebase, I want to remove the non-deprecated single-invocation factory methods (`createTransformer`, `createFilter`, `createSplitter`, `createMerger`, `createValidator`) that return `CodeNode` with `ProcessingLogic`, so that all node creation goes through the continuous runtime pattern.

These methods were never formally deprecated but follow the same single-invocation pattern as `createGenerator`/`createSink`. They create `CodeNode` instances with embedded `ProcessingLogic` rather than returning `NodeRuntime` instances with channel-based continuous execution. They have no callers in the graphEditor or production runtime code.

**Why this priority**: These methods are functional and not deprecated, so removing them carries slightly more risk. They should be removed only after confirming no callers depend on them.

**Independent Test**: Can be verified by removing each factory method and confirming the project compiles with no remaining callers. If any caller is found, the method must be retained or the caller migrated first.

**Acceptance Scenarios**:

1. **Given** the `createTransformer` single-invocation factory method exists, **When** it is removed, **Then** the project compiles and no callers remain.
2. **Given** the `createFilter` single-invocation factory method exists, **When** it is removed, **Then** the project compiles and no callers remain.
3. **Given** the `createSplitter` single-invocation factory method exists, **When** it is removed, **Then** the project compiles and no callers remain.
4. **Given** the `createMerger` single-invocation factory method exists, **When** it is removed, **Then** the project compiles and no callers remain.
5. **Given** the `createValidator` single-invocation factory method exists, **When** it is removed, **Then** the project compiles and no callers remain.
6. **Given** all single-invocation factory methods are removed, **When** a developer looks at `CodeNodeFactory`, **Then** only the generic `create` method and the continuous factory methods remain.

---

### User Story 4 - Remove ProcessingLogic Interface and Related Infrastructure (Priority: P4)

As a developer maintaining the CodeNodeIO codebase, I want to remove the `ProcessingLogic` functional interface and any code that exists solely to support single-invocation execution, so that the codebase has one execution model: continuous channel-based runtime.

The `ProcessingLogic` interface is embedded as a transient property on `CodeNode`. Removing it requires verifying that no remaining code path invokes `ProcessingLogic`. The generic `create` factory method also accepts `ProcessingLogic` as a parameter and must be updated or removed.

**Why this priority**: This is the deepest structural change. `ProcessingLogic` is defined on `CodeNode` itself, so this story requires careful verification that the graphEditor and all runtime code paths no longer reference it. It must be done last.

**Independent Test**: Can be verified by removing the `ProcessingLogic` interface, the `processingLogic` property from `CodeNode`, and the generic `create` factory method, then confirming the project compiles and all tests pass.

**Acceptance Scenarios**:

1. **Given** the `ProcessingLogic` interface exists on `CodeNode`, **When** it is removed along with the `processingLogic` property, **Then** the project compiles and all tests pass.
2. **Given** the generic `create` factory method accepts `ProcessingLogic`, **When** the method is removed or updated to no longer accept it, **Then** the project compiles and all tests pass.
3. **Given** all single-invocation code is removed, **When** a developer searches for `ProcessingLogic` or `processingLogic`, **Then** no results are found.

---

### Edge Cases

- What happens if an external consumer (outside this repository) depends on the removed factory methods? **Assumption**: This is an internal library with no external consumers. The removal is a breaking change that is acceptable.
- What happens if `ProcessingLogic` is referenced by serialization code? It is marked `@Transient` and not serialized, so serialization is unaffected.
- What happens if test files reference removed methods? All tests referencing removed code must be deleted or migrated as part of the removal.
- What happens if documentation references removed patterns? Documentation files must be updated or removed to avoid referencing deleted code.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST remove the deprecated `createGenerator` and `createSink` factory methods from `CodeNodeFactory`.
- **FR-002**: The system MUST remove backward-compatibility tests that test removed factory methods.
- **FR-003**: The system MUST remove the `TypedUseCases.kt` file containing single-invocation UseCase base classes.
- **FR-004**: The system MUST remove the `LifecycleAwareUseCases.kt` file containing lifecycle-aware single-invocation UseCase variants.
- **FR-005**: The system MUST remove the `ExampleUseCases.kt` file containing example single-invocation UseCase implementations.
- **FR-006**: The system MUST remove the non-deprecated single-invocation factory methods (`createTransformer`, `createFilter`, `createSplitter`, `createMerger`, `createValidator`) from `CodeNodeFactory`.
- **FR-007**: The system MUST remove the `ProcessingLogic` functional interface and the `processingLogic` property from `CodeNode`.
- **FR-008**: The system MUST remove or update the generic `create` factory method that accepts `ProcessingLogic`.
- **FR-009**: The system MUST remove or update any documentation that references removed single-invocation patterns.
- **FR-010**: The system MUST compile successfully and pass all remaining tests after each removal step.
- **FR-011**: The system MUST NOT remove the continuous factory methods (`createContinuousGenerator`, `createContinuousSink`, `createContinuousTransformer`, `createContinuousFilter`, and all multi-input/output processor methods).

### Key Entities

- **CodeNodeFactory**: Factory object containing both single-invocation (to be removed) and continuous (to be retained) factory methods.
- **ProcessingLogic**: Functional interface for single-invocation node execution, embedded as a transient property on CodeNode.
- **TypedUseCases**: Base classes implementing ProcessingLogic for type-safe single-invocation patterns.
- **LifecycleAwareUseCases**: Extended UseCase classes with initialization and cleanup hooks for single-invocation patterns.
- **CodeNode**: Core model class that currently carries an optional `processingLogic` property.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All single-invocation factory methods are removed from `CodeNodeFactory`, leaving only the continuous factory methods and (optionally) the generic `create` method if it has non-ProcessingLogic uses.
- **SC-002**: All UseCase base classes and their examples are removed, reducing the file count in the `usecase/` directory to zero.
- **SC-003**: The `ProcessingLogic` interface and `processingLogic` property are removed from the codebase.
- **SC-004**: The project compiles successfully with zero errors after all removals.
- **SC-005**: All remaining tests pass after all removals.
- **SC-006**: No references to removed classes, methods, or interfaces remain in source code or documentation.

## Assumptions

- This is an internal library with no external consumers — breaking changes are acceptable.
- The continuous execution model (channel-based `NodeRuntime`) is the sole supported pattern going forward.
- The `InformationPacket` type may still be used by other code paths and should not be removed unless it is exclusively used by single-invocation patterns.
- The `CodeNode` model class itself is retained — only the `processingLogic` property is removed from it.

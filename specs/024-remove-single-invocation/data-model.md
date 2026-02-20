# Data Model: Remove Single-Invocation Patterns

**Feature**: 024-remove-single-invocation
**Date**: 2026-02-20

## Overview

This is a deletion feature — no new entities are introduced. The data model documents what is being removed and what is retained.

## Entities Being Removed

### Factory Methods (from CodeNodeFactory)

| Method | Return Type | Why Removed |
|--------|------------|-------------|
| `createGenerator<T>` | CodeNode | Deprecated, replaced by `createContinuousGenerator` |
| `createSink<T>` | CodeNode | Deprecated, replaced by `createContinuousSink` |
| `createTransformer<TIn, TOut>` | CodeNode | Single-invocation pattern, zero callers |
| `createFilter<T>` | CodeNode | Single-invocation pattern, zero callers |
| `createSplitter<T>` | CodeNode | Single-invocation pattern, zero callers |
| `createMerger<T>` | CodeNode | Single-invocation pattern, zero callers |
| `createValidator<T>` | CodeNode | Single-invocation pattern, zero callers |

### Files Being Removed

| File | Contents | Why Removed |
|------|----------|-------------|
| `TypedUseCases.kt` | TransformerUseCase, FilterUseCase, ValidatorUseCase, SplitterUseCase, MergerUseCase, GeneratorUseCase, SinkUseCase | Abstract base classes with no production callers |
| `LifecycleAwareUseCases.kt` | LifecycleAwareUseCase, LifecycleManager, LifecycleDecorator, DatabaseUseCase, CachedUseCase, BufferedUseCase | Lifecycle-aware variants with no callers |
| `ExampleUseCases.kt` | 12 example UseCase implementations | Example code with no callers |
| `UseCase-Pattern-Guide.md` | Documentation for UseCase pattern | References removed abstractions |

### Tests Being Removed

| Test | Location | Why Removed |
|------|----------|-------------|
| T041 | ContinuousFactoryTest.kt | Tests removed `createGenerator` |
| T042 | ContinuousFactoryTest.kt | Tests removed `createSink` |
| T043 | ContinuousFactoryTest.kt | Tests removed `create` with ProcessingLogic invoke |

## Entities Being Retained

| Entity | Reason |
|--------|--------|
| `ProcessingLogic` interface | Used by StopWatch, kotlinCompiler, graphEditor |
| `processingLogic` property on CodeNode | Required for code generation pipeline |
| `CodeNodeFactory.create()` generic method | Only flexible factory for arbitrary node creation |
| `InformationPacket` / `InformationPacketFactory` | Core data type used across entire framework |
| `ProcessingLogicStubGenerator` | Used by ModuleSaveService for generating stubs |
| All continuous factory methods | Active execution model |

## Relationships

```
CodeNodeFactory
├── create()                    [RETAINED - generic, flexible]
├── createGenerator()           [REMOVED - deprecated]
├── createSink()                [REMOVED - deprecated]
├── createTransformer()         [REMOVED - zero callers]
├── createFilter()              [REMOVED - zero callers]
├── createSplitter()            [REMOVED - zero callers]
├── createMerger()              [REMOVED - zero callers]
├── createValidator()           [REMOVED - zero callers]
├── createContinuousGenerator() [RETAINED]
├── createContinuousSink()      [RETAINED]
├── createContinuousTransformer() [RETAINED]
├── createContinuousFilter()    [RETAINED]
└── createIn*Out*Processor()    [RETAINED - all multi-I/O methods]
```

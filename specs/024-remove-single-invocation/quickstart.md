# Quickstart: Remove Single-Invocation Patterns

**Feature**: 024-remove-single-invocation
**Date**: 2026-02-20

## Overview

This feature removes factory methods, abstract base classes, and example code that exist only for the single-invocation execution pattern. The continuous channel-based runtime is the sole supported execution model. The `ProcessingLogic` interface and related code generation infrastructure are retained because they serve the code generation pipeline, not single-invocation execution.

## Before & After

### CodeNodeFactory Before

```
CodeNodeFactory
├── create()                        ← Generic (KEPT)
├── createGenerator()               ← Deprecated, single-invocation
├── createSink()                    ← Deprecated, single-invocation
├── createTransformer()             ← Single-invocation, no callers
├── createFilter()                  ← Single-invocation, no callers
├── createSplitter()                ← Single-invocation, no callers
├── createMerger()                  ← Single-invocation, no callers
├── createValidator()               ← Single-invocation, no callers
├── createContinuousGenerator()     ← Continuous (KEPT)
├── createContinuousSink()          ← Continuous (KEPT)
├── createContinuousTransformer()   ← Continuous (KEPT)
├── createContinuousFilter()        ← Continuous (KEPT)
└── createIn*Out*Processor()        ← Continuous (KEPT)
```

### CodeNodeFactory After

```
CodeNodeFactory
├── create()                        ← Generic
├── createContinuousGenerator()
├── createContinuousSink()
├── createContinuousTransformer()
├── createContinuousFilter()
└── createIn*Out*Processor()        ← All multi-I/O methods
```

### usecase/ Directory Before

```
fbpDsl/.../usecase/
├── TypedUseCases.kt                ← 7 abstract base classes
├── LifecycleAwareUseCases.kt       ← Lifecycle-aware variants
└── examples/
    └── ExampleUseCases.kt          ← 12 example implementations
```

### usecase/ Directory After

```
fbpDsl/.../usecase/
(empty — directory may be removed)
```

### ContinuousFactoryTest Before

```
Tests T001-T043:
├── T001-T040: Continuous factory tests    ← KEPT
├── T041: Legacy createGenerator test      ← REMOVED
├── T042: Legacy createSink test           ← REMOVED
└── T043: ProcessingLogic invoke test      ← REMOVED
```

### ContinuousFactoryTest After

```
Tests T001-T040:
└── T001-T040: Continuous factory tests    ← All retained
```

## What Stays and Why

| Retained | Reason |
|----------|--------|
| `ProcessingLogic` | Used by StopWatch components, code generation, module save |
| `processingLogic` on CodeNode | Required for code generation pipeline |
| `create()` factory | Only way to create flexible custom CodeNodes |
| `InformationPacket` | Core data type for entire framework |
| All continuous methods | Active execution model |

## Files Changed

| File | Change Type | Description |
|------|-------------|-------------|
| `fbpDsl/.../model/CodeNodeFactory.kt` | Modify | Remove 7 single-invocation factory methods |
| `fbpDsl/.../usecase/TypedUseCases.kt` | Delete | Remove all typed UseCase base classes |
| `fbpDsl/.../usecase/LifecycleAwareUseCases.kt` | Delete | Remove lifecycle-aware UseCase variants |
| `fbpDsl/.../usecase/examples/ExampleUseCases.kt` | Delete | Remove example implementations |
| `fbpDsl/docs/UseCase-Pattern-Guide.md` | Delete | Remove obsolete documentation |
| `fbpDsl/.../runtime/ContinuousFactoryTest.kt` | Modify | Remove backward-compat tests T041-T043 |

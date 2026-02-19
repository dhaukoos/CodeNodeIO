# Data Model: Refactor Base NodeRuntime Class

**Feature**: 021-refactor-noderuntime-base
**Date**: 2026-02-19

## Class Hierarchy: Before vs After

### NodeRuntime (Base Class)

**Before**:
```
NodeRuntime<T: Any>(codeNode, registry?)
├── executionState: ExecutionState
├── nodeControlJob: Job?
├── inputChannel: ReceiveChannel<T>?     ← REMOVE
├── outputChannel: SendChannel<T>?       ← REMOVE
├── start(scope, processingBlock)        ← Remove outputChannel?.close() from finally
├── stop()
├── pause() / resume()
└── isRunning() / isPaused() / isIdle()
```

**After**:
```
NodeRuntime(codeNode, registry?)          ← No generic parameter
├── executionState: ExecutionState
├── nodeControlJob: Job?
├── start(scope, processingBlock)         ← No channel references
├── stop()
├── pause() / resume()
└── isRunning() / isPaused() / isIdle()
```

### Channel Property Migration Table

| Runtime Class | Before (inherited) | After (own property) | Type |
|---|---|---|---|
| **GeneratorRuntime** | `outputChannel: SendChannel<T>` (base) | `outputChannel: SendChannel<T>` (own) | Already creates in init, just needs own var |
| **SinkRuntime** | `inputChannel: ReceiveChannel<T>` (base) | `inputChannel: ReceiveChannel<T>` (own) | Own property, same name (single-input) |
| **TransformerRuntime** | `inputChannel: ReceiveChannel<TIn>` (base) + `transformerOutputChannel` | `inputChannel: ReceiveChannel<TIn>` (own) + `outputChannel` (own) | Own input (same name), rename output |
| **FilterRuntime** | `inputChannel: ReceiveChannel<T>` (base) + `outputChannel: SendChannel<T>` (base) | `inputChannel: ReceiveChannel<T>` (own) + `outputChannel: SendChannel<T>` (own) | Own properties, same names (single-input/output) |
| **Out2GeneratorRuntime** | None used from base | No change | Already has own outputChannel1/2 |
| **Out3GeneratorRuntime** | None used from base | No change | Already has own outputChannel1/2/3 |
| **In2SinkRuntime** | `inputChannel: ReceiveChannel<A>` (base) | `inputChannel1: ReceiveChannel<A>` (own) | Rename + own (multi-input) |
| **In3SinkRuntime** | `inputChannel: ReceiveChannel<A>` (base) | `inputChannel1: ReceiveChannel<A>` (own) | Rename + own (multi-input) |
| **In1Out2Runtime** | `inputChannel: ReceiveChannel<A>` (base) | `inputChannel: ReceiveChannel<A>` (own) | Own property, same name (single-input) |
| **In1Out3Runtime** | `inputChannel: ReceiveChannel<A>` (base) | `inputChannel: ReceiveChannel<A>` (own) | Own property, same name (single-input) |
| **In2Out1Runtime** | `inputChannel: ReceiveChannel<A>` (base) + `processorOutputChannel` | `inputChannel1: ReceiveChannel<A>` (own) + `outputChannel` (own) | Rename input (multi-input), rename output |
| **In2Out2Runtime** | `inputChannel: ReceiveChannel<A>` (base) | `inputChannel1: ReceiveChannel<A>` (own) | Rename + own (multi-input) |
| **In2Out3Runtime** | `inputChannel: ReceiveChannel<A>` (base) | `inputChannel1: ReceiveChannel<A>` (own) | Rename + own (multi-input) |
| **In3Out1Runtime** | `inputChannel: ReceiveChannel<A>` (base) + `processorOutputChannel` | `inputChannel1: ReceiveChannel<A>` (own) + `outputChannel` (own) | Rename input (multi-input), rename output |
| **In3Out2Runtime** | `inputChannel: ReceiveChannel<A>` (base) | `inputChannel1: ReceiveChannel<A>` (own) | Rename + own (multi-input) |
| **In3Out3Runtime** | `inputChannel: ReceiveChannel<A>` (base) | `inputChannel1: ReceiveChannel<A>` (own) | Rename + own (multi-input) |

### Inheritance Change Summary

**Before**: `class SinkRuntime<T: Any>(...) : NodeRuntime<T>(codeNode)`
**After**: `class SinkRuntime<T: Any>(...) : NodeRuntime(codeNode)`

**Before**: `class In2SinkRuntime<A: Any, B: Any>(...) : NodeRuntime<A>(codeNode)`
**After**: `class In2SinkRuntime<A: Any, B: Any>(...) : NodeRuntime(codeNode)`

The subclasses keep their own generic parameters for typed channels. Only the `NodeRuntime<X>` part of the inheritance changes to `NodeRuntime`.

## External Reference Changes

### RuntimeRegistry

**Before**: `NodeRuntime<*>` (7 references)
**After**: `NodeRuntime` (plain, no generic)

### CodeNodeFactory

Factory methods create typed runtime instances. The factory itself doesn't reference `NodeRuntime` directly in most cases - it creates specific subclass instances. No significant changes needed except removing any `NodeRuntime<*>` type annotations.

### DisplayReceiverComponent (StopWatch)

DisplayReceiverComponent delegates to a `SinkRuntime` (single-input). Since single-input runtimes keep the name `inputChannel`, no rename is needed here.

**Before**:
```
var inputChannel: ReceiveChannel<Int>?
    get() = sinkRuntime.inputChannel
    set(value) { sinkRuntime.inputChannel = value }
```

**After**: No change (single-input keeps `inputChannel`).

### StopWatchFlow Wiring

**Before**: `displayReceiver.inputChannel = timerEmitter.outputChannel1`
**After**: No change (single-input keeps `inputChannel`).

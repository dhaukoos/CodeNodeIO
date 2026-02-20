# Quickstart: Timed Factory Methods

**Feature**: 025-timed-factory-methods
**Date**: 2026-02-20

## Before & After

### Before: Manual delay loop in generator

```kotlin
val timer = CodeNodeFactory.createContinuousGenerator<Long>(
    name = "Timer"
) { emit ->
    var elapsed = 0L
    while (currentCoroutineContext().isActive) {
        delay(100)
        elapsed += 100
        emit(elapsed)
    }
}
```

### After: Timed generator with tick function

```kotlin
var elapsed = 0L
val timer = CodeNodeFactory.createTimedGenerator<Long>(
    name = "Timer",
    tickIntervalMs = 100
) {
    elapsed += 100
    elapsed
}
```

## Integration Scenarios

### Scenario 1: Timed Single-Output Generator

```kotlin
var count = 0
val counter = CodeNodeFactory.createTimedGenerator<Int>(
    name = "Counter",
    tickIntervalMs = 1000
) {
    ++count
}

val channel = Channel<Int>(Channel.BUFFERED)
counter.outputChannel = channel
counter.start(scope) { }

// Receives: 1, 2, 3, ... at 1-second intervals
```

### Scenario 2: Timed Transformer

```kotlin
val doubler = CodeNodeFactory.createTimedTransformer<Int, Int>(
    name = "SlowDoubler",
    tickIntervalMs = 500
) { value ->
    value * 2
}

doubler.inputChannel = sourceChannel
doubler.transformerOutputChannel = outputChannel
doubler.start(scope) { }

// Each input is delayed 500ms before doubling and forwarding
```

### Scenario 3: Timed Multi-Input Processor

```kotlin
val adder = CodeNodeFactory.createTimedIn2Out1Processor<Int, Int, Int>(
    name = "SlowAdder",
    tickIntervalMs = 200
) { a, b ->
    a + b
}

adder.inputChannel = channel1
adder.inputChannel2 = channel2
adder.processorOutputChannel = outputChannel
adder.start(scope) { }
```

### Scenario 4: Timed Sink

```kotlin
val logger = CodeNodeFactory.createTimedSink<String>(
    name = "SlowLogger",
    tickIntervalMs = 100
) { message ->
    println("[${System.currentTimeMillis()}] $message")
}

logger.inputChannel = sourceChannel
logger.start(scope) { }

// Each message is consumed with 100ms delay between processing
```

### Scenario 5: Timed Filter

```kotlin
val evenFilter = CodeNodeFactory.createTimedFilter<Int>(
    name = "SlowEvenFilter",
    tickIntervalMs = 250
) { value ->
    value % 2 == 0
}

evenFilter.inputChannel = sourceChannel
evenFilter.outputChannel = outputChannel
evenFilter.start(scope) { }
```

## Verification Checklist

1. Each timed method compiles with its tick type alias
2. Each timed method delegates to the correct continuous method
3. Each timed method returns the same runtime type as the continuous variant
4. The delay-before-tick pattern is consistent across all methods
5. Existing `createTimedOut2Generator` continues to work unchanged

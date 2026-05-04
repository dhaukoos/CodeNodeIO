# Contract: UIFBPViewModelGenerator (MVI shape)

**Implements**: FR-003, FR-004 (publishes the contract), FR-008, FR-009,
FR-011, FR-012, SC-001, SC-005

## Public API

```kotlin
class UIFBPViewModelGenerator {
    /**
     * Emits {Name}ViewModel.kt — the MVI bridge between the FBP controller
     * and the host module's hand-written {Name}Screen composable.
     *
     * Caller (UIFBPInterfaceGenerator) writes to:
     *   {basePackage}/viewmodel/{Name}ViewModel.kt
     */
    fun generate(spec: UIFBPSpec): String
}
```

## Behavioral contract

**Returns**: a Kotlin file body of the form:

```kotlin
/* license header */
package {spec.packageName}.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.FlowGraph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import {spec.packageName}.controller.{spec.flowGraphPrefix}ControllerInterface
/* IP-type imports */

class {spec.flowGraphPrefix}ViewModel(
    private val controller: {spec.flowGraphPrefix}ControllerInterface
) : ViewModel() {

    private val _state = MutableStateFlow({spec.flowGraphPrefix}State())
    val state: StateFlow<{spec.flowGraphPrefix}State> = _state.asStateFlow()

    val executionState: StateFlow<ExecutionState> = controller.executionState

    init {
        // For each spec.sinkInputs entry:
        viewModelScope.launch {
            controller.{sinkPort}.collect { value ->
                _state.update { it.copy({sinkPort} = value) }
            }
        }
        // … one launch block per sinkInput
    }

    fun onEvent(event: {spec.flowGraphPrefix}Event) {
        when (event) {
            // For each Update{PortName}(value):
            is {spec.flowGraphPrefix}Event.Update{PortName} -> controller.emit{PortName}(event.value)
            // For each {PortName} data object (Unit-typed source port):
            {spec.flowGraphPrefix}Event.{PortName} -> controller.emit{PortName}()
        }
    }

    fun start(): FlowGraph = controller.start()
    fun stop(): FlowGraph = controller.stop()
    fun pause(): FlowGraph = controller.pause()
    fun resume(): FlowGraph = controller.resume()
    fun reset(): FlowGraph = controller.reset()
}
```

### Constructor

- Single parameter: `private val controller: {Name}ControllerInterface`.
- Constructor signature MUST match exactly so `ModuleSessionFactory.tryCreateViewModel`'s
  reflection-based instantiation keeps working without modification.

### State pipeline

- Private `MutableStateFlow<{Name}State>` initialized to `{Name}State()`
  (the data-class default constructor — every property at its default).
- Public `state: StateFlow<{Name}State>` exposes a read-only view.
- `init` block launches one collector per `spec.sinkInputs` port. Each
  collector folds the incoming value into a fresh State snapshot via
  `_state.update { it.copy({port} = value) }`.
- Collectors run inside `viewModelScope` — automatically cancelled on
  ViewModel clear.

### Event pipeline

- `fun onEvent(event: {Name}Event)` dispatcher uses an exhaustive `when`
  expression over every Event case.
- Each case calls `controller.emit{PortName}(event.value)` (or
  `controller.emit{PortName}()` for `Unit`-typed `data object` events) —
  the source-port emit method on the `{Name}ControllerInterface` (Design B).
  No shared singleton is touched.

### Edge cases

- **Empty `sinkInputs`**: omit the `init` block entirely. `_state` stays at
  `{Name}State()` (zero-arg) forever.
- **Empty `sourceOutputs`**: emit `fun onEvent(event: {Name}Event) { when (event) {} }`
  — exhaustive over zero cases. The body is a single empty `when`. Compiler
  accepts this because the Event sealed interface has no subtypes (so no
  branches are missing).

### Forwarding control surface

`start / stop / pause / resume / reset` MUST be emitted exactly as today
(byte-identical to current `UIFBPViewModelGenerator` for these lines).
This preserves the FR-007 control-surface contract.

### What's removed (FR-011)

- The prior aggregate `fun emit(...)` method — gone.
- The prior singleton-State `{Name}State` (mutable object) — gone entirely
  (Design B). No `StateStore` rename, no surviving singleton.
- The prior `val xxx: StateFlow<T> = {Name}State.xxxFlow` per-port flow
  members — replaced by the single `state` flow.

## Test contract (RED phase)

Tests live in
`flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/generator/UIFBPViewModelGeneratorTest.kt`.

Required test cases:

1. `generate emits class with constructor (controller: {Name}ControllerInterface)`
2. `generate exposes private MutableStateFlow + public StateFlow<{Name}State> named state`
3. `generate launches one viewModelScope collector per sinkInput, calling _state.update { copy(port = value) }`
4. `generate omits init block when sinkInputs is empty`
5. `generate emits onEvent dispatcher with one when branch per sourceOutput`
6. `generate uses Update{PortName} branches calling controller.emit{PortName}(event.value)`
7. `generate uses {PortName} data-object branches calling controller.emit{PortName}()`
8. `generate emits empty when block when sourceOutputs is empty`
9. `generate forwards start/stop/pause/resume/reset to controller`
10. `generate does NOT emit prior emit(...) aggregate`
11. `generate does NOT reference any singleton State or StateStore (Design B — all state lives on the ViewModel or the controller)`
12. `generate is byte-identical across two consecutive calls (determinism)`

Each test uses fixture-string comparison.

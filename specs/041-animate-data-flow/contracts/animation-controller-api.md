# Contract: Animation Controller API

**Feature**: 041-animate-data-flow
**Date**: 2026-03-06

## 1. NodeRuntime Emission Callback (fbpDsl)

### Addition to NodeRuntime

```kotlin
// In NodeRuntime.kt
open class NodeRuntime(
    val codeNode: CodeNode,
    var registry: RuntimeRegistry? = null
) {
    // ... existing properties ...

    /**
     * Optional callback invoked when this node emits an IP on an output port.
     * Parameters: (nodeId: String, portIndex: Int)
     * Set by ModuleController when emission observation is enabled.
     */
    var onEmit: ((String, Int) -> Unit)? = null
}
```

### Usage in Runtime Classes

Each runtime class invokes the callback immediately after a successful `send()`:

```kotlin
// Example: SourceRuntime.kt
outputChannel?.send(value)
onEmit?.invoke(codeNode.id, 0)  // portIndex 0 for single-output

// Example: SourceOut2Runtime.kt
result.out1?.let { outputChannel1?.send(it); onEmit?.invoke(codeNode.id, 0) }
result.out2?.let { outputChannel2?.send(it); onEmit?.invoke(codeNode.id, 1) }
```

### Addition to ModuleController Interface

```kotlin
// In ModuleController.kt
interface ModuleController {
    // ... existing methods ...

    /**
     * Sets or clears the emission observer for all runtimes in this module.
     * When set, the observer is called with (nodeId, portIndex) on every IP emission.
     * Pass null to clear the observer.
     */
    fun setEmissionObserver(observer: ((String, Int) -> Unit)?)
}
```

## 2. DataFlowAnimationController (circuitSimulator)

### Class Signature

```kotlin
class DataFlowAnimationController {
    /** Currently active dot animations, updated each frame */
    val activeAnimations: StateFlow<List<ConnectionAnimation>>

    /**
     * Creates the emission observer callback to wire to ModuleController.
     * Returns a (nodeId, portIndex) -> Unit callback.
     *
     * @param flowGraph The current FlowGraph for connection lookup
     * @param attenuationMs Current attenuation value (duration = 80% of this)
     */
    fun createEmissionObserver(
        flowGraph: FlowGraph,
        attenuationMs: () -> Long
    ): (String, Int) -> Unit

    /** Pause all animations (freeze progress) */
    fun pause()

    /** Resume all animations from frozen state */
    fun resume()

    /** Clear all animations immediately */
    fun clear()

    /** Start the animation frame loop (advances progress, removes completed) */
    fun startFrameLoop(scope: CoroutineScope)

    /** Stop the animation frame loop */
    fun stopFrameLoop()
}
```

### ConnectionAnimation Data Class

```kotlin
data class ConnectionAnimation(
    val connectionId: String,
    val startTimeMs: Long,
    val durationMs: Long
) {
    /** Current progress [0.0, 1.0] based on system time */
    fun progress(currentTimeMs: Long): Float {
        val elapsed = (currentTimeMs - startTimeMs).toFloat()
        return (elapsed / durationMs).coerceIn(0f, 1f)
    }

    /** Whether this animation has completed */
    fun isComplete(currentTimeMs: Long): Boolean = progress(currentTimeMs) >= 1f
}
```

## 3. RuntimeSession Animation Extensions (circuitSimulator)

### Additions to RuntimeSession

```kotlin
class RuntimeSession(
    private val controller: ModuleController,
    val viewModel: Any
) {
    // ... existing properties ...

    /** Animation controller for managing dot animations */
    val animationController = DataFlowAnimationController()

    /** Whether the "Animate Data Flow" toggle is active */
    private val _animateDataFlow = MutableStateFlow(false)
    val animateDataFlow: StateFlow<Boolean> = _animateDataFlow.asStateFlow()

    /** Minimum attenuation required for animation (ms) */
    val animationAttenuationThreshold: Long = 500L

    /**
     * Toggles the "Animate Data Flow" feature on or off.
     * Only activates if attenuationMs >= animationAttenuationThreshold.
     * When activated, wires the emission observer to the controller.
     * When deactivated, clears the observer.
     */
    fun setAnimateDataFlow(enabled: Boolean)

    // Modified existing methods:
    // - start(): starts animation frame loop if animateDataFlow is true
    // - stop(): clears animations, stops frame loop, clears observer
    // - pause(): pauses animation controller
    // - resume(): resumes animation controller
    // - setAttenuation(): auto-disables animation if below threshold
}
```

## 4. FlowGraphCanvas Animation Rendering (graphEditor)

### New Parameter

```kotlin
@Composable
fun FlowGraphCanvas(
    // ... existing parameters ...
    activeAnimations: List<ConnectionAnimation> = emptyList(),
    // ...
)
```

### Dot Rendering

After drawing connections and before drawing nodes, render animation dots:

```kotlin
// For each active animation:
// 1. Find the connection by ID
// 2. Compute source and target port screen positions (same as drawConnection)
// 3. Compute Bezier control points (same as drawBezierConnection)
// 4. Interpolate position at progress t using existing cubicBezier()
// 5. Draw filled circle at interpolated position
//    - Radius: strokeWidth * 2 (about 6f * scale for normal connections)
//    - Color: connection's IP type color or default dark gray
```

## 5. RuntimePreviewPanel Toggle (graphEditor)

### Toggle Placement

In the Speed Attenuation section, after the slider range labels (0ms / 2000ms) and before the Divider:

```kotlin
// "Animate Data Flow" toggle row
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text("Animate Data Flow", fontSize = 11.sp)
    Switch(
        checked = animateDataFlow,
        onCheckedChange = { runtimeSession?.setAnimateDataFlow(it) },
        enabled = runtimeSession != null && attenuationMs >= 500L
    )
}
```

### New Parameters for RuntimePreviewPanel

```kotlin
@Composable
fun RuntimePreviewPanel(
    // ... existing parameters ...
    animateDataFlow: Boolean = false,
    onAnimateDataFlowChanged: (Boolean) -> Unit = {},
    // ...
)
```

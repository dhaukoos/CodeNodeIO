/*
 * DynamicPipelineControllerScopeAccessTest — feature 087 / Decision 8.
 *
 * Pins the contract that DynamicPipelineController exposes its internal
 * coroutine scope publicly (via `coroutineScope`) so generated UI-FBP
 * Runtime factories can launch source-port emissions on it. Today the
 * scope is held privately as `flowScope`; this test fails to compile
 * until the public accessor is added by T004 GREEN.
 *
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

import io.codenode.fbpdsl.dsl.flowGraph
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DynamicPipelineControllerScopeAccessTest {

    private fun emptyController(): DynamicPipelineController {
        val empty = flowGraph("scope-test", version = "1.0.0") {}
        return DynamicPipelineController(
            flowGraphProvider = { empty },
            lookup = { null }
        )
    }

    @Test
    fun coroutineScope_is_publicly_readable_and_null_before_start() {
        val controller = emptyController()
        // The pinned contract is that `coroutineScope` is a publicly readable
        // member (compile-time check) and is null before the pipeline is
        // constructed (runtime check). Generated UI-FBP Runtime factories
        // depend on this property's signature so they can lazily launch
        // emissions only when the controller is active.
        assertNull(
            controller.coroutineScope,
            "before start(), no per-pipeline scope has been created yet — " +
                "the public accessor must return null."
        )
    }

    @Test
    fun coroutineScope_is_null_after_stop_or_invalid_start() {
        val controller = emptyController()
        // start() on an empty FlowGraph fails validation (per
        // DynamicPipelineBuilder.validate), so flowScope is never assigned.
        // After this call coroutineScope must still be null — a public
        // accessor that returned a stale scope across failed-start /
        // stop() boundaries would let the generated Runtime emit on a
        // cancelled scope.
        controller.start()
        assertNull(
            controller.coroutineScope,
            "after a validation-failed start(), scope is still null — emit " +
                "dispatchers MUST handle the no-scope case gracefully."
        )
        controller.stop()
        assertNull(
            controller.coroutineScope,
            "after stop(), the controller releases its scope."
        )
    }

    @Test
    fun coroutineScope_property_is_assignable_from_external_callers() {
        // Compile-time contract pin: the property's type is `CoroutineScope?`,
        // not `Any?` — generated {Name}Runtime factories declare
        // `controller.coroutineScope?.launch { ... }` and rely on the typed
        // signature. If this test compiles, the public accessor's type is
        // correct. (No runtime assertion needed; the call site is the proof.)
        val controller: DynamicPipelineController = emptyController()
        @Suppress("UNUSED_VARIABLE")
        val scope: kotlinx.coroutines.CoroutineScope? = controller.coroutineScope
        assertNotNull(controller, "controller fixture must construct cleanly")
    }
}

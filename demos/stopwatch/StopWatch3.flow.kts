/*
 * Flow Graph: StopWatch3
 * Version: 1.0.0
 * Generated: 2026-02-11T21:39:16.661031
 */

import io.codenode.fbpdsl.dsl.*
import io.codenode.fbpdsl.model.*

val graph = flowGraph("StopWatch3", version = "1.0.0") {
    // Target Platforms
    targetPlatform(FlowGraph.TargetPlatform.KMP_ANDROID)
    targetPlatform(FlowGraph.TargetPlatform.KMP_IOS)
    targetPlatform(FlowGraph.TargetPlatform.KMP_DESKTOP)

    // Nodes
    val displayreceiver = codeNode("DisplayReceiver") {
        description = "Generic processing node with 2 inputs and no outputs"
        position(665.9267578125, 199.32322692871094)
        input("seconds", Any::class)
        input("minutes", Any::class)
        config("_genericType", "in2out0")
        config("_useCaseClass", "/Users/dhaukoos/CodeNodeIO/demos/stopwatch/DisplayReceiverComponent.kt")
    }

    val timeremitter1 = codeNode("TimerEmitter") {
        description = "Generic processing node with no inputs and 2 outputs"
        position(296.5, 198.25)
        output("elapsedSeconds", Any::class)
        output("elapsedMinutes", Any::class)
        config("_genericType", "in0out2")
        config("_useCaseClass", "/Users/dhaukoos/CodeNodeIO/demos/stopwatch/TimerEmitterComponent.kt")
    }

    // Connections
    timeremitter1.output("elapsedSeconds") connect displayreceiver.input("seconds") withType "ip_int"
    timeremitter1.output("elapsedMinutes") connect displayreceiver.input("minutes") withType "ip_int"
}

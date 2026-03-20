package io.codenode.stopwatch

import io.codenode.fbpdsl.dsl.*
import io.codenode.fbpdsl.model.*

val stopWatchFlowGraph = flowGraph("StopWatch", version = "1.0.0") {
    targetPlatform(FlowGraph.TargetPlatform.KMP_ANDROID)
    targetPlatform(FlowGraph.TargetPlatform.KMP_IOS)

    val timeIncrementer = codeNode("TimeIncrementer") {
        position(491.8232116699219, 310.4267578125)
        input("elapsedSeconds", Int::class)
        input("elapsedMinutes", Int::class)
        output("seconds", Int::class)
        output("minutes", Int::class)
        config("_codeNodeClass", "io.codenode.stopwatch.nodes.TimeIncrementerCodeNode")
        config("_codeNodeDefinition", "true")
        config("_genericType", "in2out2")
    }

    val displayReceiver = codeNode("DisplayReceiver", nodeType = "SINK") {
        position(851.71142578125, 309.20318603515625)
        input("seconds", Int::class)
        input("minutes", Int::class)
        config("_codeNodeClass", "io.codenode.stopwatch.nodes.DisplayReceiverCodeNode")
        config("_codeNodeDefinition", "true")
        config("_genericType", "in2anyout0")
    }

    val timerEmitter = codeNode("TimerEmitter", nodeType = "SOURCE") {
        position(142.42677307128906, 309.8232116699219)
        output("elapsedSeconds", Int::class)
        output("elapsedMinutes", Int::class)
        config("_codeNodeClass", "io.codenode.stopwatch.nodes.TimerEmitterCodeNode")
        config("_codeNodeDefinition", "true")
        config("_genericType", "in0out2")
    }

    timerEmitter.output("elapsedSeconds") connect timeIncrementer.input("elapsedSeconds") withType "ip_int"
    timerEmitter.output("elapsedMinutes") connect timeIncrementer.input("elapsedMinutes") withType "ip_int"
    timeIncrementer.output("seconds") connect displayReceiver.input("seconds") withType "ip_int"
    timeIncrementer.output("minutes") connect displayReceiver.input("minutes") withType "ip_int"
}

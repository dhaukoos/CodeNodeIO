package io.codenode.stopwatch

import io.codenode.fbpdsl.dsl.*
import io.codenode.fbpdsl.model.*

val stopWatchFlowGraph = flowGraph("StopWatch", version = "1.0.0") {
    targetPlatform(FlowGraph.TargetPlatform.KMP_ANDROID)
    targetPlatform(FlowGraph.TargetPlatform.KMP_IOS)

    val timeIncrementer = codeNode("TimeIncrementer", nodeType = "GENERIC") {
        position(491.8232116699219, 310.4267578125)
        input("elapsedSeconds", Int::class)
        input("elapsedMinutes", Int::class)
        output("seconds", Int::class)
        output("minutes", Int::class)
    }

    val displayReceiver = codeNode("DisplayReceiver", nodeType = "GENERIC") {
        position(851.71142578125, 309.20318603515625)
        input("seconds", Int::class)
        input("minutes", Int::class)
    }

    val timerEmitter = codeNode("TimerEmitter", nodeType = "GENERIC") {
        position(142.42677307128906, 309.8232116699219)
        output("elapsedSeconds", Int::class)
        output("elapsedMinutes", Int::class)
    }

    timerEmitter.output("elapsedSeconds") connect timeIncrementer.input("elapsedSeconds") withType "ip_int"
    timerEmitter.output("elapsedMinutes") connect timeIncrementer.input("elapsedMinutes") withType "ip_int"
    timeIncrementer.output("seconds") connect displayReceiver.input("seconds") withType "ip_int"
    timeIncrementer.output("minutes") connect displayReceiver.input("minutes") withType "ip_int"
}

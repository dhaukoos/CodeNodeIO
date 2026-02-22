package io.codenode.stopwatch4

import io.codenode.fbpdsl.dsl.*
import io.codenode.fbpdsl.model.*
import io.codenode.stopwatch4.processingLogic.*

val stopWatch4FlowGraph = flowGraph("StopWatch4", version = "1.0.0") {
    targetPlatform(FlowGraph.TargetPlatform.KMP_ANDROID)
    targetPlatform(FlowGraph.TargetPlatform.KMP_IOS)

    val timerEmitter = codeNode("TimerEmitter", nodeType = GENERIC) {
        position(296.0, 190.0)
        output("elapsedSeconds", Int::class)
        output("elapsedMinuts", Int::class)
    }

    val displayReceiver = codeNode("DisplayReceiver", nodeType = GENERIC) {
        position(744.0, 205.75)
        input("seconds", Int::class)
        input("minutes", Int::class)
    }

    timerEmitter.output("elapsedSeconds") connect displayReceiver.input("seconds") withType "ip_int"
    timerEmitter.output("elapsedMinuts") connect displayReceiver.input("minutes") withType "ip_int"
}

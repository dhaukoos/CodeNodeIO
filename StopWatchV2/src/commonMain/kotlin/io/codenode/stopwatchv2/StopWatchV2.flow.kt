package io.codenode.stopwatchv2

import io.codenode.fbpdsl.dsl.*
import io.codenode.fbpdsl.model.*

val stopWatchV2FlowGraph = flowGraph("StopWatchV2", version = "1.0.0") {
    targetPlatform(FlowGraph.TargetPlatform.KMP_ANDROID)
    targetPlatform(FlowGraph.TargetPlatform.KMP_IOS)

    val timerEmitter = codeNode("TimerEmitter", nodeType = "GENERIC") {
        position(296.0, 190.0)
        output("elapsedSeconds", Int::class)
        output("elapsedMinutes", Int::class)
    }

    val displayReceiver = codeNode("DisplayReceiver", nodeType = "GENERIC") {
        position(744.0, 205.75)
        input("seconds", Int::class)
        input("minutes", Int::class)
    }

    timerEmitter.output("elapsedSeconds") connect displayReceiver.input("seconds")
    timerEmitter.output("elapsedMinutes") connect displayReceiver.input("minutes")
}

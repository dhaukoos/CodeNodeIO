package io.codenode.flowgraphgenerate.flow

import io.codenode.fbpdsl.dsl.*
import io.codenode.fbpdsl.model.*

val generateUIFBPFlowGraph = flowGraph("GenerateUIFBP", version = "1.0.0") {

    val configSource = codeNode("ConfigSource", nodeType = "SOURCE") {
        position(50.0, 300.0)
        output("config", Any::class)
    }

    // Shared generators (flow + controller)
    val flowKtGen = codeNode("FlowKtGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 50.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val runtimeFlowGen = codeNode("RuntimeFlowGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 130.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val controllerGen = codeNode("RuntimeControllerGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 210.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val controllerInterfaceGen = codeNode("RuntimeControllerInterfaceGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 290.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val controllerAdapterGen = codeNode("RuntimeControllerAdapterGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 370.0)
        input("config", Any::class)
        output("content", String::class)
    }

    // UI-FBP specific generators
    val uiFBPStateGen = codeNode("UIFBPStateGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 470.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val uiFBPViewModelGen = codeNode("UIFBPViewModelGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 550.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val uiFBPSourceGen = codeNode("UIFBPSourceGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 630.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val uiFBPSinkGen = codeNode("UIFBPSinkGenerator", nodeType = "TRANSFORMER") {
        position(350.0, 710.0)
        input("config", Any::class)
        output("content", String::class)
    }

    val resultCollector = codeNode("ResultCollector", nodeType = "SINK") {
        position(650.0, 300.0)
        input("flowKt", String::class)
        input("runtimeFlow", String::class)
        input("controller", String::class)
        input("controllerInterface", String::class)
        input("controllerAdapter", String::class)
        input("uiFBPState", String::class)
        input("uiFBPViewModel", String::class)
        input("uiFBPSource", String::class)
        input("uiFBPSink", String::class)
    }

    // Source fan-out
    configSource.output("config") connect flowKtGen.input("config")
    configSource.output("config") connect runtimeFlowGen.input("config")
    configSource.output("config") connect controllerGen.input("config")
    configSource.output("config") connect controllerInterfaceGen.input("config")
    configSource.output("config") connect controllerAdapterGen.input("config")
    configSource.output("config") connect uiFBPStateGen.input("config")
    configSource.output("config") connect uiFBPViewModelGen.input("config")
    configSource.output("config") connect uiFBPSourceGen.input("config")
    configSource.output("config") connect uiFBPSinkGen.input("config")

    // Generator outputs to sink
    flowKtGen.output("content") connect resultCollector.input("flowKt")
    runtimeFlowGen.output("content") connect resultCollector.input("runtimeFlow")
    controllerGen.output("content") connect resultCollector.input("controller")
    controllerInterfaceGen.output("content") connect resultCollector.input("controllerInterface")
    controllerAdapterGen.output("content") connect resultCollector.input("controllerAdapter")
    uiFBPStateGen.output("content") connect resultCollector.input("uiFBPState")
    uiFBPViewModelGen.output("content") connect resultCollector.input("uiFBPViewModel")
    uiFBPSourceGen.output("content") connect resultCollector.input("uiFBPSource")
    uiFBPSinkGen.output("content") connect resultCollector.input("uiFBPSink")
}

/*
 * Flow Graph: Target Architecture
 * Version: 4.0.0
 * Description: Vertical-slice decomposition with source/sink split for the composition root
 * Generated: 2026-04-03
 *
 * Cross-validation with MIGRATION.md:
 *
 * Connection                                                        | MIGRATION.md Interface                    | Flow Direction
 * ----------------------------------------------------------------- | ----------------------------------------- | --------------------
 * types.ipTypeMetadata → compose.ipTypeMetadata                     | IPTypeRegistryService                     | types → compose
 * types.ipTypeMetadata → persist.ipTypeMetadata                     | IPTypeRegistryService                     | types → persist
 * types.ipTypeMetadata → generate.ipTypeMetadata                    | IPTypeRegistryService                     | types → generate
 * types.ipTypeMetadata → rootSink.ipTypeMetadata                    | IPTypeRegistryService                     | types → root
 * inspect.nodeDescriptors → compose.nodeDescriptors                 | NodeRegistryService                       | inspect → compose
 * inspect.nodeDescriptors → execute.nodeDescriptors                 | NodeRegistryService                       | inspect → execute
 * inspect.nodeDescriptors → generate.nodeDescriptors                | NodeRegistryService                       | inspect → generate
 * inspect.nodeDescriptors → rootSink.nodeDescriptors                | NodeRegistryService                       | inspect → root
 * persist.serializedOutput → generate.serializedOutput              | FlowGraphPersistenceService               | persist → generate
 * persist.loadedFlowGraph → rootSink.loadedFlowGraph                | FlowGraphPersistenceService               | persist → root
 * persist.graphNodeTemplates → rootSink.graphNodeTemplates          | GraphNodeTemplateService                  | persist → root
 * compose.graphState → rootSink.graphState                          | GraphCompositionService, UndoRedoService  | compose → root
 * execute.executionState → rootSink.executionState                  | RuntimeExecutionService                   | execute → root
 * execute.animations → rootSink.animations                          | ConnectionAnimationProvider               | execute → root
 * generate.generatedOutput → rootSink.generatedOutput               | CodeGenerationService                     | generate → root
 * rootSource.flowGraphModel → compose.flowGraphModel                | (command flow)                            | root → compose
 * rootSource.flowGraphModel → persist.flowGraphModel                | (command flow)                            | root → persist
 * rootSource.flowGraphModel → execute.flowGraphModel                | (command flow)                            | root → execute
 * rootSource.flowGraphModel → generate.flowGraphModel               | (command flow)                            | root → generate
 * rootSource.ipTypeCommands → types.ipTypeCommands                  | (command flow)                            | root → types
 *
 * Total connections: 20
 * graphEditor-source and graphEditor-sink are two views of the same module (27 files).
 * Source = ViewModel command actions (user intent flowing out to workflow modules).
 * Sink = reactive Compose UI state (display data flowing in from workflow modules).
 *
 * File counts: types:9, inspect:13, persist:8, compose:10, execute:7,
 *              generate:46, root:27 = 120
 */

import io.codenode.fbpdsl.dsl.*
import io.codenode.fbpdsl.model.*

val graph = flowGraph("Target Architecture", version = "4.0.0", description = "Vertical-slice module decomposition — six workflow modules plus source/sink composition root") {
    // Target Platforms
    targetPlatform(FlowGraph.TargetPlatform.KMP_DESKTOP)
    targetPlatform(FlowGraph.TargetPlatform.KMP_ANDROID)
    targetPlatform(FlowGraph.TargetPlatform.KMP_IOS)

    // Nodes (GraphNodes — empty containers representing target vertical-slice modules)

    val types = graphNode("flowGraph-types") {
        description = "IP type lifecycle: discovery, registry, repository, file generation, migration. 9 files. Consolidates IP type concerns from inspect, persist, and generate."
        position(100.0, 100.0)
        exposeInput("filesystemPaths", String::class)
        exposeInput("classpathEntries", String::class)
        exposeInput("ipTypeCommands", String::class)
        exposeOutput("ipTypeMetadata", String::class)

        // Child CodeNode — FlowGraphTypesCodeNode (In3AnyOut1Runtime, anyInput)
        val flowGraphTypes = codeNode("FlowGraphTypes", nodeType = "TRANSFORMER") {
            input("filesystemPaths", String::class)
            input("classpathEntries", String::class)
            input("ipTypeCommands", String::class)
            output("ipTypeMetadata", String::class)
        }

        // Port mappings — wire exposed GraphNode ports to child CodeNode ports
        portMapping("filesystemPaths", "flowGraphTypes", "filesystemPaths")
        portMapping("classpathEntries", "flowGraphTypes", "classpathEntries")
        portMapping("ipTypeCommands", "flowGraphTypes", "ipTypeCommands")
        portMapping("ipTypeMetadata", "flowGraphTypes", "ipTypeMetadata")
    }

    val inspect = graphNode("flowGraph-inspect") {
        description = "Node discovery and inspection: NodeDefinitionRegistry, palette ViewModels, filesystem/classpath scanning. 7 files."
        position(100.0, 400.0)
        exposeInput("filesystemPaths", String::class)
        exposeInput("classpathEntries", String::class)
        exposeOutput("nodeDescriptors", String::class)

        // Child CodeNode — FlowGraphInspectCodeNode (In2AnyOut1Runtime, anyInput)
        val flowGraphInspect = codeNode("FlowGraphInspect", nodeType = "TRANSFORMER") {
            input("filesystemPaths", String::class)
            input("classpathEntries", String::class)
            output("nodeDescriptors", String::class)
        }

        // Port mappings — wire exposed GraphNode ports to child CodeNode ports
        portMapping("filesystemPaths", "flowGraphInspect", "filesystemPaths")
        portMapping("classpathEntries", "flowGraphInspect", "classpathEntries")
        portMapping("nodeDescriptors", "flowGraphInspect", "nodeDescriptors")
    }

    val rootSource = graphNode("graphEditor-source") {
        description = "Composition root — command side: ViewModel actions that dispatch user intent to workflow modules. Part of graphEditor (27 files shared with graphEditor-sink)."
        position(100.0, 700.0)
        exposeOutput("flowGraphModel", String::class)
        exposeOutput("ipTypeCommands", String::class)
    }

    val persist = graphNode("flowGraph-persist") {
        description = "Saving and loading flow graphs: FlowGraphSerializer, FlowKtParser, template registry, file I/O. 6 files."
        position(400.0, 100.0)
        exposeInput("flowGraphModel", String::class)
        exposeInput("ipTypeMetadata", String::class)
        exposeOutput("serializedOutput", String::class)
        exposeOutput("loadedFlowGraph", String::class)
        exposeOutput("graphNodeTemplates", String::class)

        // Child CodeNode — FlowGraphPersistCodeNode (In2AnyOut3Runtime, anyInput)
        val flowGraphPersist = codeNode("FlowGraphPersist", nodeType = "TRANSFORMER") {
            input("flowGraphModel", String::class)
            input("ipTypeMetadata", String::class)
            output("serializedOutput", String::class)
            output("loadedFlowGraph", String::class)
            output("graphNodeTemplates", String::class)
        }

        // Port mappings — wire exposed GraphNode ports to child CodeNode ports
        portMapping("flowGraphModel", "flowGraphPersist", "flowGraphModel")
        portMapping("ipTypeMetadata", "flowGraphPersist", "ipTypeMetadata")
        portMapping("serializedOutput", "flowGraphPersist", "serializedOutput")
        portMapping("loadedFlowGraph", "flowGraphPersist", "loadedFlowGraph")
        portMapping("graphNodeTemplates", "flowGraphPersist", "graphNodeTemplates")
    }

    val compose = graphNode("flowGraph-compose") {
        description = "Building a flow graph interactively: canvas interaction, properties panel, node generator state, view synchronization. 4 files + 1 CodeNode."
        position(400.0, 400.0)
        exposeInput("flowGraphModel", String::class)
        exposeInput("nodeDescriptors", String::class)
        exposeInput("ipTypeMetadata", String::class)
        exposeOutput("graphState", String::class)

        // Child CodeNode — FlowGraphComposeCodeNode (In3AnyOut1Runtime, anyInput)
        val flowGraphCompose = codeNode("FlowGraphCompose", nodeType = "TRANSFORMER") {
            input("flowGraphModel", String::class)
            input("nodeDescriptors", String::class)
            input("ipTypeMetadata", String::class)
            output("graphState", String::class)
        }

        // Port mappings — wire exposed GraphNode ports to child CodeNode ports
        portMapping("flowGraphModel", "flowGraphCompose", "flowGraphModel")
        portMapping("nodeDescriptors", "flowGraphCompose", "nodeDescriptors")
        portMapping("ipTypeMetadata", "flowGraphCompose", "ipTypeMetadata")
        portMapping("graphState", "flowGraphCompose", "graphState")
    }

    val execute = graphNode("flowGraph-execute") {
        description = "Running and observing flow graphs: RuntimeSession, DataFlowAnimationController, DataFlowDebugger, ConnectionAnimation, CircuitSimulator (from circuitSimulator) + ModuleSessionFactory (from graphEditor). 7 files."
        position(400.0, 700.0)
        exposeInput("flowGraphModel", String::class)
        exposeInput("nodeDescriptors", String::class)
        exposeOutput("executionState", String::class)
        exposeOutput("animations", String::class)
        exposeOutput("debugSnapshots", String::class)

        // Child CodeNode — FlowGraphExecuteCodeNode (In2AnyOut3Runtime, anyInput)
        val flowGraphExecute = codeNode("FlowGraphExecute", nodeType = "TRANSFORMER") {
            input("flowGraphModel", String::class)
            input("nodeDescriptors", String::class)
            output("executionState", String::class)
            output("animations", String::class)
            output("debugSnapshots", String::class)
        }

        // Port mappings — wire exposed GraphNode ports to child CodeNode ports
        portMapping("flowGraphModel", "flowGraphExecute", "flowGraphModel")
        portMapping("nodeDescriptors", "flowGraphExecute", "nodeDescriptors")
        portMapping("executionState", "flowGraphExecute", "executionState")
        portMapping("animations", "flowGraphExecute", "animations")
        portMapping("debugSnapshots", "flowGraphExecute", "debugSnapshots")
    }

    val generate = graphNode("flowGraph-generate") {
        description = "Code generation: generators, templates, validators, compilation, module save. Two-node sub-graph: GenerateContextAggregator aggregates flowGraphModel + serializedOutput into generationContext, FlowGraphGenerate combines generationContext + nodeDescriptors + ipTypeMetadata into generatedOutput."
        position(700.0, 400.0)
        exposeInput("flowGraphModel", String::class)
        exposeInput("serializedOutput", String::class)
        exposeInput("nodeDescriptors", String::class)
        exposeInput("ipTypeMetadata", String::class)
        exposeOutput("generatedOutput", String::class)

        // Child CodeNodes
        val generateContextAggregator = codeNode("GenerateContextAggregator", nodeType = "TRANSFORMER") {
            position(50.0, 50.0)
            input("flowGraphModel", String::class)
            input("serializedOutput", String::class)
            output("generationContext", String::class)
        }

        val flowGraphGenerate = codeNode("FlowGraphGenerate", nodeType = "TRANSFORMER") {
            position(350.0, 50.0)
            input("generationContext", String::class)
            input("nodeDescriptors", String::class)
            input("ipTypeMetadata", String::class)
            output("generatedOutput", String::class)
        }

        // Port mappings — wire exposed GraphNode ports to child CodeNode ports
        portMapping("flowGraphModel", "GenerateContextAggregator", "flowGraphModel")
        portMapping("serializedOutput", "GenerateContextAggregator", "serializedOutput")
        portMapping("nodeDescriptors", "FlowGraphGenerate", "nodeDescriptors")
        portMapping("ipTypeMetadata", "FlowGraphGenerate", "ipTypeMetadata")
        portMapping("generatedOutput", "FlowGraphGenerate", "generatedOutput")

        // Internal connection — aggregator output feeds into generate node
        generateContextAggregator.output("generationContext") connect flowGraphGenerate.input("generationContext")
    }

    val rootSink = graphNode("graphEditor-sink") {
        description = "Composition root — display side: reactive Compose UI state flowing in from workflow modules. Part of graphEditor (27 files shared with graphEditor-source)."
        position(1000.0, 400.0)
        exposeInput("graphState", String::class)
        exposeInput("loadedFlowGraph", String::class)
        exposeInput("graphNodeTemplates", String::class)
        exposeInput("executionState", String::class)
        exposeInput("animations", String::class)
        exposeInput("generatedOutput", String::class)
        exposeInput("nodeDescriptors", String::class)
        exposeInput("ipTypeMetadata", String::class)
    }

    // Connections: types → consumers (IP type metadata for all modules that need type info)
    types.output("ipTypeMetadata") connect compose.input("ipTypeMetadata")
    types.output("ipTypeMetadata") connect persist.input("ipTypeMetadata")
    types.output("ipTypeMetadata") connect generate.input("ipTypeMetadata")
    types.output("ipTypeMetadata") connect rootSink.input("ipTypeMetadata")

    // Connections: inspect → consumers (node descriptors only — IP type data now from types)
    inspect.output("nodeDescriptors") connect compose.input("nodeDescriptors")
    inspect.output("nodeDescriptors") connect execute.input("nodeDescriptors")
    inspect.output("nodeDescriptors") connect generate.input("nodeDescriptors")
    inspect.output("nodeDescriptors") connect rootSink.input("nodeDescriptors")

    // Connections: persist → consumers (state flows)
    persist.output("serializedOutput") connect generate.input("serializedOutput")
    persist.output("loadedFlowGraph") connect rootSink.input("loadedFlowGraph")
    persist.output("graphNodeTemplates") connect rootSink.input("graphNodeTemplates")

    // Connections: compose → root sink (state flow)
    compose.output("graphState") connect rootSink.input("graphState")

    // Connections: execute → root sink (state flows)
    execute.output("executionState") connect rootSink.input("executionState")
    execute.output("animations") connect rootSink.input("animations")

    // Connections: generate → root sink (state flow)
    generate.output("generatedOutput") connect rootSink.input("generatedOutput")

    // Connections: root source → workflow modules (command flows — user actions via ViewModels)
    rootSource.output("flowGraphModel") connect compose.input("flowGraphModel")
    rootSource.output("flowGraphModel") connect persist.input("flowGraphModel")
    rootSource.output("flowGraphModel") connect execute.input("flowGraphModel")
    rootSource.output("flowGraphModel") connect generate.input("flowGraphModel")
    rootSource.output("ipTypeCommands") connect types.input("ipTypeCommands")
}

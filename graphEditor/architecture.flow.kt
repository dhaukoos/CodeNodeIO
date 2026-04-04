/*
 * Flow Graph: Target Architecture
 * Version: 3.0.0
 * Description: Vertical-slice decomposition with pure FBP bidirectional data flow — six workflow modules plus composition root
 * Generated: 2026-04-03
 *
 * Cross-validation with MIGRATION.md:
 *
 * Connection                                              | MIGRATION.md Interface                    | Flow Direction
 * ------------------------------------------------------- | ----------------------------------------- | --------------------
 * types.ipTypeMetadata → compose.ipTypeMetadata           | IPTypeRegistryService                     | types → compose
 * types.ipTypeMetadata → persist.ipTypeMetadata           | IPTypeRegistryService                     | types → persist
 * types.ipTypeMetadata → generate.ipTypeMetadata          | IPTypeRegistryService                     | types → generate
 * types.ipTypeMetadata → root.ipTypeMetadata              | IPTypeRegistryService                     | types → root
 * inspect.nodeDescriptors → compose.nodeDescriptors       | NodeRegistryService                       | inspect → compose
 * inspect.nodeDescriptors → execute.nodeDescriptors       | NodeRegistryService                       | inspect → execute
 * inspect.nodeDescriptors → generate.nodeDescriptors      | NodeRegistryService                       | inspect → generate
 * inspect.nodeDescriptors → root.nodeDescriptors          | NodeRegistryService                       | inspect → root
 * persist.serializedOutput → generate.serializedOutput    | FlowGraphPersistenceService               | persist → generate
 * persist.loadedFlowGraph → root.loadedFlowGraph          | FlowGraphPersistenceService               | persist → root
 * persist.graphNodeTemplates → root.graphNodeTemplates    | GraphNodeTemplateService                  | persist → root
 * compose.graphState → root.graphState                    | GraphCompositionService, UndoRedoService  | compose → root
 * execute.executionState → root.executionState            | RuntimeExecutionService                   | execute → root
 * execute.animations → root.animations                    | ConnectionAnimationProvider               | execute → root
 * generate.generatedOutput → root.generatedOutput         | CodeGenerationService                     | generate → root
 * root.flowGraphModel → compose.flowGraphModel            | (command flow)                            | root → compose
 * root.flowGraphModel → persist.flowGraphModel            | (command flow)                            | root → persist
 * root.flowGraphModel → execute.flowGraphModel            | (command flow)                            | root → execute
 * root.flowGraphModel → generate.flowGraphModel           | (command flow)                            | root → generate
 *
 * Total connections: 19
 * Data flow has feedback loops (root ↔ compose, root ↔ persist, root ↔ execute, root ↔ generate)
 * which is expected in FBP — command IPs flow out from root, state IPs flow back.
 * Gradle module dependencies remain acyclic (root depends on each module's interface).
 *
 * File counts: types:9, inspect:13, persist:8, compose:10, execute:7,
 *              generate:46, root:27 = 120
 */

import io.codenode.fbpdsl.dsl.*
import io.codenode.fbpdsl.model.*

val graph = flowGraph("Target Architecture", version = "3.0.0", description = "Vertical-slice module decomposition with pure FBP bidirectional data flow — six workflow modules plus composition root") {
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
        exposeOutput("ipTypeMetadata", String::class)
    }

    val inspect = graphNode("flowGraph-inspect") {
        description = "Understanding available components: node palette, filesystem node scanning, CodeNode text editor. 13 files."
        position(100.0, 400.0)
        exposeInput("filesystemPaths", String::class)
        exposeInput("classpathEntries", String::class)
        exposeOutput("nodeDescriptors", String::class)
    }

    val persist = graphNode("flowGraph-persist") {
        description = "Saving and loading flow graphs: FlowGraphSerializer, FlowKtParser, template registry, file I/O. 8 files."
        position(400.0, 100.0)
        exposeInput("flowGraphModel", String::class)
        exposeInput("ipTypeMetadata", String::class)
        exposeOutput("serializedOutput", String::class)
        exposeOutput("loadedFlowGraph", String::class)
        exposeOutput("graphNodeTemplates", String::class)
    }

    val compose = graphNode("flowGraph-compose") {
        description = "Building a flow graph interactively: graph mutations, port connections, validation, undo/redo. 10 files."
        position(400.0, 400.0)
        exposeInput("flowGraphModel", String::class)
        exposeInput("nodeDescriptors", String::class)
        exposeInput("ipTypeMetadata", String::class)
        exposeOutput("graphState", String::class)
    }

    val execute = graphNode("flowGraph-execute") {
        description = "Running and observing flow graphs: runtime pipeline, execution control, animation, debugging. 7 files. Absorbs circuitSimulator (5 files)."
        position(400.0, 600.0)
        exposeInput("flowGraphModel", String::class)
        exposeInput("nodeDescriptors", String::class)
        exposeOutput("executionState", String::class)
        exposeOutput("animations", String::class)
        exposeOutput("debugSnapshots", String::class)
    }

    val generate = graphNode("flowGraph-generate") {
        description = "Producing deployable code: all generators, templates, validators, module save. 46 files. Absorbs kotlinCompiler (38 files)."
        position(700.0, 400.0)
        exposeInput("flowGraphModel", String::class)
        exposeInput("serializedOutput", String::class)
        exposeInput("nodeDescriptors", String::class)
        exposeInput("ipTypeMetadata", String::class)
        exposeOutput("generatedOutput", String::class)
    }

    val root = graphNode("graphEditor") {
        description = "Composition root: Compose UI, ViewModels, renderers, DI wiring. 27 files. Orchestrates all slices. Outputs are command flows (user actions); inputs are state flows (reactive UI data)."
        position(1000.0, 400.0)
        // State inputs (reactive data flowing in from workflow modules)
        exposeInput("graphState", String::class)
        exposeInput("loadedFlowGraph", String::class)
        exposeInput("graphNodeTemplates", String::class)
        exposeInput("executionState", String::class)
        exposeInput("animations", String::class)
        exposeInput("generatedOutput", String::class)
        exposeInput("nodeDescriptors", String::class)
        exposeInput("ipTypeMetadata", String::class)
        // Command output (user actions dispatched to workflow modules)
        exposeOutput("flowGraphModel", String::class)
    }

    // Connections: types → consumers (IP type metadata for all modules that need type info)
    types.output("ipTypeMetadata") connect compose.input("ipTypeMetadata")
    types.output("ipTypeMetadata") connect persist.input("ipTypeMetadata")
    types.output("ipTypeMetadata") connect generate.input("ipTypeMetadata")
    types.output("ipTypeMetadata") connect root.input("ipTypeMetadata")

    // Connections: inspect → consumers (node descriptors only — IP type data now from types)
    inspect.output("nodeDescriptors") connect compose.input("nodeDescriptors")
    inspect.output("nodeDescriptors") connect execute.input("nodeDescriptors")
    inspect.output("nodeDescriptors") connect generate.input("nodeDescriptors")
    inspect.output("nodeDescriptors") connect root.input("nodeDescriptors")

    // Connections: persist → consumers (state flows)
    persist.output("serializedOutput") connect generate.input("serializedOutput")
    persist.output("loadedFlowGraph") connect root.input("loadedFlowGraph")
    persist.output("graphNodeTemplates") connect root.input("graphNodeTemplates")

    // Connections: compose → root (state flow)
    compose.output("graphState") connect root.input("graphState")

    // Connections: execute → root (state flows)
    execute.output("executionState") connect root.input("executionState")
    execute.output("animations") connect root.input("animations")

    // Connections: generate → root (state flow)
    generate.output("generatedOutput") connect root.input("generatedOutput")

    // Connections: root → workflow modules (command flows — user actions via ViewModels)
    root.output("flowGraphModel") connect compose.input("flowGraphModel")
    root.output("flowGraphModel") connect persist.input("flowGraphModel")
    root.output("flowGraphModel") connect execute.input("flowGraphModel")
    root.output("flowGraphModel") connect generate.input("flowGraphModel")
}

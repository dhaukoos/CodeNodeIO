/*
 * UIFBPSaveService — jvmMain orchestrator that composes UIFBPInterfaceGenerator with
 * filesystem I/O, host-module pre-flight, and the per-file write/merge decision tree.
 *
 * Contract (per data-model.md §3 — UIFBPSaveService):
 *   save(spec, flowGraphFile, moduleRoot, options): UIFBPSaveResult
 *
 * 1. Pre-flight: refuse with an actionable error if moduleRoot/build.gradle.kts is missing
 *    OR lacks `jvm()` OR lacks the `io.codenode:preview-api` dependency. Build wiring is
 *    the responsibility of feature 085's ModuleGenerator (FR-009 / Decision 5); UI-FBP
 *    NEVER mutates build.gradle.kts. The user is directed to the one-time migration
 *    documented in quickstart.md VS-A1.
 * 2. Generate: produce the 8-or-9 entry universal artifact set via UIFBPInterfaceGenerator
 *    (8 mandatory files post-feature-087: the 7 from feature 084 + the new
 *    `{Name}Event.kt`; PreviewProvider's body is unchanged from feature 084).
 * 3. Per-file decide-and-write:
 *      - target missing                        → CREATED
 *      - target matches new content            → UNCHANGED (no write; mtime preserved)
 *      - target carries the generator marker   → UPDATED (overwrite)
 *      - target lacks the marker               → SKIPPED_CONFLICT (hand-edit safety, FR-016)
 * 4. Optional legacy cleanup (deleteLegacyLocations=true): remove pre-085 saved/ duplicates
 *    only when they carry the marker; lacking it → SKIPPED_CONFLICT.
 * 5. .flow.kt parse-and-merge (Decision 7 / FR-011 / FR-012):
 *      - missing or no `flowGraph(`            → bootstrap CodeNode pair → CREATED
 *      - parse OK + zero diff + matching content → UNCHANGED
 *      - parse OK + port-shape or content drift → mutate Source/Sink ports, drop
 *                                                 connections referencing removed ports
 *                                                 (with structured DroppedConnection
 *                                                 reasons), re-serialize → UPDATED
 *      - parse fails                           → PARSE_FAILED_SKIPPED, file untouched,
 *                                                 warning surfaced
 *      User-added CodeNodes (anything not the framework `${flowGraphPrefix}Source/Sink`)
 *      are preserved unchanged across merges.
 *
 * One-time TestModule migration: see specs/084-ui-fbp-runtime-preview/quickstart.md §VS-A1.
 *
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.save

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.Connection
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.Port
import io.codenode.fbpdsl.model.PortFactory
import io.codenode.flowgraphgenerate.generator.FlowKtGenerator
import io.codenode.flowgraphgenerate.generator.UIFBPInterfaceGenerator
import io.codenode.flowgraphgenerate.parser.UIFBPSpec
import io.codenode.flowgraphpersist.serialization.FlowKtParser
import java.io.File

class UIFBPSaveService(
    private val orchestrator: UIFBPInterfaceGenerator = UIFBPInterfaceGenerator(),
    private val flowKtGenerator: FlowKtGenerator = FlowKtGenerator(),
    private val flowKtParser: FlowKtParser = FlowKtParser()
) {

    /**
     * Saves the post-085 UI-FBP universal artifact set into [moduleRoot] for the given spec.
     *
     * Pre-flight: refuses with an actionable error message if [moduleRoot]/build.gradle.kts is
     * missing OR lacks the `jvm()` target OR lacks the `io.codenode:preview-api` dependency.
     *
     * @param spec The UI-FBP spec built by `UIComposableParser` from the user's qualifying UI file.
     * @param flowGraphFile The user-selected `.flow.kt` file (mirrors feature 085's Generate Module
     *        explicit-pair input pattern). Used to derive the bootstrap target path; the merge
     *        case (US4) consumes it for parse-and-serialize.
     * @param moduleRoot The host module's directory (must contain `build.gradle.kts`).
     * @param options Per-call options (legacy cleanup opt-in; .flow.kt merge toggle).
     */
    fun save(
        spec: UIFBPSpec,
        flowGraphFile: File,
        moduleRoot: File,
        options: UIFBPSaveOptions = UIFBPSaveOptions()
    ): UIFBPSaveResult {
        // 1. Pre-flight host validation (Decision 5; FR-009 post-clarification).
        val buildScript = File(moduleRoot, "build.gradle.kts")
        if (!buildScript.exists()) {
            return UIFBPSaveResult(
                success = false,
                errorMessage = "Host module is unscaffolded: $moduleRoot/build.gradle.kts not found. " +
                    "UI-FBP requires a feature-085-scaffolded module. " +
                    "Run quickstart.md VS-A1 migration first."
            )
        }
        val buildScriptText = buildScript.readText()
        val missing = mutableListOf<String>()
        if (!hasJvmTarget(buildScriptText)) missing += "jvm() target"
        if (!hasPreviewApiDependency(buildScriptText)) missing += "io.codenode:preview-api dep in jvmMain"
        if (missing.isNotEmpty()) {
            return UIFBPSaveResult(
                success = false,
                errorMessage = "Host module is unscaffolded: missing ${missing.joinToString(" and ")}. " +
                    "Run quickstart.md VS-A1 migration first to add these to the module's build.gradle.kts."
            )
        }

        // 2. Generate the post-085 universal set (UIFBPInterfaceGenerator returns 7-or-8 files).
        val genResult = orchestrator.generateAll(spec, includeFlowKt = false)
        if (!genResult.success) {
            return UIFBPSaveResult(
                success = false,
                errorMessage = "Generation failed: ${genResult.errorMessage}"
            )
        }

        // 3. Per-file decide-and-write loop.
        val files = mutableListOf<FileChange>()
        for (gf in genResult.filesGenerated) {
            val target = File(moduleRoot, gf.relativePath)
            files += writeOrSkip(target, gf.relativePath, gf.content)
        }

        // 4. Optional legacy-location cleanup (FR-010 / Decision 5).
        if (options.deleteLegacyLocations) {
            files += cleanupLegacyLocations(spec, moduleRoot)
        }

        // 5. .flow.kt parse-and-merge (FR-011 / FR-012 / Decision 7).
        //    Bootstraps a fresh `.flow.kt` if missing/empty; otherwise parses, computes the
        //    Source/Sink port diff against [spec], drops connections referencing now-removed
        //    ports, and re-serializes via [FlowKtGenerator]. Surfaces the structured
        //    [FlowKtMergeReport] in the result.
        val warnings = mutableListOf<String>()
        val flowKtMerge = if (options.mergeExistingFlowKt) {
            mergeFlowKt(spec, flowGraphFile, warnings)
        } else null

        return UIFBPSaveResult(
            success = true,
            files = files,
            flowKtMerge = flowKtMerge,
            warnings = warnings
        )
    }

    // ========== build.gradle.kts heuristic checks ==========

    private fun hasJvmTarget(buildScript: String): Boolean {
        // Match `jvm()` or `jvm {` or `jvm("alias") {` etc. — robust enough for typical scripts.
        val pattern = Regex("""\bjvm\s*[({]""")
        return pattern.containsMatchIn(buildScript)
    }

    private fun hasPreviewApiDependency(buildScript: String): Boolean =
        buildScript.contains("io.codenode:preview-api")

    // ========== per-file decision tree ==========

    private fun writeOrSkip(target: File, relativePath: String, newContent: String): FileChange {
        if (!target.exists()) {
            target.parentFile?.mkdirs()
            target.writeText(newContent)
            return FileChange(relativePath = relativePath, kind = FileChangeKind.CREATED)
        }

        val existing = target.readText()
        if (normalizeForCompare(existing) == normalizeForCompare(newContent)) {
            return FileChange(relativePath = relativePath, kind = FileChangeKind.UNCHANGED)
        }

        // Content differs. Honor FR-016: only overwrite if the existing file carries the
        // generator marker; otherwise refuse (user has hand-edited it).
        if (carriesGeneratorMarker(existing)) {
            target.writeText(newContent)
            return FileChange(relativePath = relativePath, kind = FileChangeKind.UPDATED)
        }
        return FileChange(
            relativePath = relativePath,
            kind = FileChangeKind.SKIPPED_CONFLICT,
            reason = "File lacks the \"$generatorMarker\" marker comment; appears hand-edited. " +
                "Delete or restore manually before re-running."
        )
    }

    /**
     * Normalizes content for comparison: trims trailing whitespace per line and the file's tail.
     * Avoids spurious UPDATED marks from EOL or trailing-newline drift.
     */
    private fun normalizeForCompare(content: String): String =
        content.lineSequence().map { it.trimEnd() }.joinToString("\n").trimEnd()

    /**
     * Mirrors `GenerationFileWriter.carriesGeneratorMarker` from feature 085 — checks the file's
     * head for the standard `Generated by CodeNodeIO` marker comment.
     */
    private fun carriesGeneratorMarker(content: String): Boolean =
        content.lineSequence().take(8).joinToString("\n").contains(generatorMarker)

    private val generatorMarker = "Generated by CodeNodeIO"

    // ========== legacy cleanup (T037 / FR-010) ==========

    /**
     * Removes legacy generator-output files the post-085 pipeline no longer emits, but only if
     * each file carries the [generatorMarker]. Marker-less files are reported as
     * [FileChangeKind.SKIPPED_CONFLICT] so a hand-edited file is never silently deleted.
     *
     * Currently covers the pre-085 `saved/` duplicate locations under `{packageName}/saved/`.
     * Empty parent directories are removed best-effort after deletion.
     */
    private fun cleanupLegacyLocations(spec: UIFBPSpec, moduleRoot: File): List<FileChange> {
        val packagePath = spec.packageName.replace('.', '/')
        val legacyRel = listOf(
            "src/commonMain/kotlin/$packagePath/saved/${spec.flowGraphPrefix}State.kt",
            "src/commonMain/kotlin/$packagePath/saved/${spec.flowGraphPrefix}ViewModel.kt"
        )

        val results = mutableListOf<FileChange>()
        val touchedDirs = mutableSetOf<File>()

        for (rel in legacyRel) {
            val file = File(moduleRoot, rel)
            if (!file.exists()) continue

            val content = file.readText()
            if (carriesGeneratorMarker(content)) {
                file.delete()
                file.parentFile?.let { touchedDirs += it }
                results += FileChange(relativePath = rel, kind = FileChangeKind.DELETED)
            } else {
                results += FileChange(
                    relativePath = rel,
                    kind = FileChangeKind.SKIPPED_CONFLICT,
                    reason = "Legacy file lacks the \"$generatorMarker\" marker comment; appears hand-edited. " +
                        "Delete or move it manually before re-running with deleteLegacyLocations=true."
                )
            }
        }

        // Best-effort: prune now-empty legacy parent directories.
        for (dir in touchedDirs) {
            if (dir.isDirectory && dir.listFiles().isNullOrEmpty()) {
                dir.delete()
            }
        }

        return results
    }

    // ========== .flow.kt parse-and-merge (T045 / FR-011 / FR-012 / Decision 7) ==========

    /**
     * Bootstraps or merges the `.flow.kt` file at [flowGraphFile] for the given [spec].
     *
     * Flow:
     *  - File missing OR contains no `flowGraph(` declaration → bootstrap with a Source +
     *    Sink CodeNode pair derived from the spec; mode = [FlowKtMergeMode.CREATED].
     *  - File parses successfully → compute the Source/Sink port diff (by NAME) against the
     *    spec, drop connections referencing now-removed port IDs, mutate the FlowGraph
     *    in-place, and re-serialize via [FlowKtGenerator]. mode = UPDATED or UNCHANGED.
     *  - File contains `flowGraph(` but [FlowKtParser] rejects it → write nothing; emit a
     *    warning; mode = [FlowKtMergeMode.PARSE_FAILED_SKIPPED].
     *
     * User-added CodeNodes (anything that isn't named `${flowGraphPrefix}Source` or
     * `${flowGraphPrefix}Sink`) are preserved unchanged across merges; only connections
     * referencing removed ports are dropped.
     */
    private fun mergeFlowKt(
        spec: UIFBPSpec,
        flowGraphFile: File,
        warnings: MutableList<String>
    ): FlowKtMergeReport {
        val sourceName = "${spec.flowGraphPrefix}Source"
        val sinkName = "${spec.flowGraphPrefix}Sink"
        val flowPackage = "${spec.packageName}.flow"

        // Bootstrap path: file doesn't exist, or content has no flowGraph() declaration
        // (covers empty files and the placeholder-comment-only fixture pattern).
        val needsBootstrap = !flowGraphFile.exists() ||
            !flowGraphFile.readText().contains("flowGraph(")
        if (needsBootstrap) {
            val bootstrap = buildBootstrapGraph(spec)
            val bootstrapOverrides = portTypeOverridesForBootstrap(spec, bootstrap)
            val content = flowKtGenerator.generateFlowKt(
                flowGraph = bootstrap,
                packageName = flowPackage,
                ipTypeImports = spec.ipTypeImports,
                portTypeOverrides = bootstrapOverrides
            )
            flowGraphFile.parentFile?.mkdirs()
            flowGraphFile.writeText(content)
            return FlowKtMergeReport(mode = FlowKtMergeMode.CREATED, userNodesPreserved = 0)
        }

        // Parse the existing file.
        val existingText = flowGraphFile.readText()
        val parseResult = flowKtParser.parseFlowKt(existingText)
        val existing = parseResult.graph
        if (!parseResult.isSuccess || existing == null) {
            warnings += "Could not parse ${flowGraphFile.name}: ${parseResult.errorMessage ?: "unknown error"}. " +
                "File left untouched. Fix the syntax or delete the file before re-running."
            return FlowKtMergeReport(mode = FlowKtMergeMode.PARSE_FAILED_SKIPPED)
        }

        val codeNodes = existing.rootNodes.filterIsInstance<CodeNode>()
        val source = codeNodes.firstOrNull { it.name == sourceName }
        val sink = codeNodes.firstOrNull { it.name == sinkName }

        // Compute the by-name port diff for Source and Sink.
        val desiredSourceNames = spec.sourceOutputs.map { it.name }.toSet()
        val desiredSinkNames = spec.sinkInputs.map { it.name }.toSet()
        val currentSourceNames = source?.outputPorts?.map { it.name }?.toSet() ?: emptySet()
        val currentSinkNames = sink?.inputPorts?.map { it.name }?.toSet() ?: emptySet()

        val portsAdded = mutableListOf<PortChange>()
        val portsRemoved = mutableListOf<PortChange>()

        if (source != null) {
            for (newPort in spec.sourceOutputs) {
                if (newPort.name !in currentSourceNames) {
                    portsAdded += PortChange(sourceName, newPort.name, newPort.typeName)
                }
            }
            for (oldPort in source.outputPorts) {
                if (oldPort.name !in desiredSourceNames) {
                    portsRemoved += PortChange(sourceName, oldPort.name, oldPort.typeName)
                }
            }
        }
        if (sink != null) {
            for (newPort in spec.sinkInputs) {
                if (newPort.name !in currentSinkNames) {
                    portsAdded += PortChange(sinkName, newPort.name, newPort.typeName)
                }
            }
            for (oldPort in sink.inputPorts) {
                if (oldPort.name !in desiredSinkNames) {
                    portsRemoved += PortChange(sinkName, oldPort.name, oldPort.typeName)
                }
            }
        }

        // Count user-added CodeNodes (anything that isn't the framework Source/Sink).
        val userNodesPreserved = codeNodes.count { it.name != sourceName && it.name != sinkName }

        // Apply mutations: replace Source/Sink CodeNodes with merged port lists.
        val removedPortIds = mutableSetOf<String>()
        val newSource = source?.let {
            val keptOutputs = it.outputPorts.filter { p -> p.name in desiredSourceNames }
            removedPortIds += it.outputPorts.filter { p -> p.name !in desiredSourceNames }.map { p -> p.id }
            val addedOutputs = spec.sourceOutputs
                .filter { p -> p.name !in currentSourceNames }
                .map { p -> PortFactory.outputWithType(p.name, Any::class, it.id) }
            it.copy(outputPorts = keptOutputs + addedOutputs)
        }
        val newSink = sink?.let {
            val keptInputs = it.inputPorts.filter { p -> p.name in desiredSinkNames }
            removedPortIds += it.inputPorts.filter { p -> p.name !in desiredSinkNames }.map { p -> p.id }
            val addedInputs = spec.sinkInputs
                .filter { p -> p.name !in currentSinkNames }
                .map { p -> PortFactory.inputWithType(p.name, Any::class, it.id, required = true) }
            it.copy(inputPorts = keptInputs + addedInputs)
        }

        // Drop connections referencing any removed port. Surface each via DroppedConnection.
        val portIdToName = codeNodes
            .flatMap { it.inputPorts + it.outputPorts }
            .associate { it.id to it.name }
        val nodeIdToName = existing.rootNodes.associate { it.id to it.name }

        val connectionsDropped = mutableListOf<DroppedConnection>()
        val survivingConnections = existing.connections.filter { conn ->
            val sourceRemoved = conn.sourcePortId in removedPortIds
            val targetRemoved = conn.targetPortId in removedPortIds
            if (sourceRemoved || targetRemoved) {
                val side = if (sourceRemoved) "Source" else "Sink"
                connectionsDropped += DroppedConnection(
                    from = "${nodeIdToName[conn.sourceNodeId] ?: conn.sourceNodeId}." +
                        "${portIdToName[conn.sourcePortId] ?: conn.sourcePortId}",
                    to = "${nodeIdToName[conn.targetNodeId] ?: conn.targetNodeId}." +
                        "${portIdToName[conn.targetPortId] ?: conn.targetPortId}",
                    reason = "$side port was removed from the UI signature; connection has no valid endpoint."
                )
                false
            } else true
        }

        val mergedNodes: List<Node> = existing.rootNodes.map { node ->
            when {
                node is CodeNode && node.name == sourceName -> newSource ?: node
                node is CodeNode && node.name == sinkName -> newSink ?: node
                else -> node
            }
        }
        val mergedGraph = existing.copy(
            rootNodes = mergedNodes,
            connections = survivingConnections
        )

        // Build port-type overrides for ALL spec ports (kept + added) plus parser hints
        // for types the parser couldn't resolve to a KClass (e.g., user IP types like
        // CalculationResults). The spec is the source of truth for port types — if a
        // file's port type drifted from the spec (e.g., a previous buggy save left
        // `Any` in place of `Double`), the spec's typeName overrides it on re-emit.
        val mergeOverrides = mutableMapOf<String, String>()
        mergeOverrides.putAll(parseResult.portTypeNameHints)
        newSource?.outputPorts?.forEach { port ->
            spec.sourceOutputs.firstOrNull { it.name == port.name }
                ?.let { mergeOverrides[port.id] = it.typeName }
        }
        newSink?.inputPorts?.forEach { port ->
            spec.sinkInputs.firstOrNull { it.name == port.name }
                ?.let { mergeOverrides[port.id] = it.typeName }
        }

        val newContent = flowKtGenerator.generateFlowKt(
            flowGraph = mergedGraph,
            packageName = flowPackage,
            ipTypeImports = spec.ipTypeImports,
            portTypeOverrides = mergeOverrides
        )

        // Decide UNCHANGED vs UPDATED by comparing the canonical re-emit to the file on
        // disk (modulo trailing whitespace). This catches type/import drift on kept
        // ports — not just port-name diffs. Skipping the write when content matches
        // preserves mtime for the idempotent-resave case.
        val portShapeChanged = portsAdded.isNotEmpty() || portsRemoved.isNotEmpty()
        val contentDrifted = normalizeForCompare(existingText) != normalizeForCompare(newContent)

        if (!portShapeChanged && !contentDrifted) {
            return FlowKtMergeReport(
                mode = FlowKtMergeMode.UNCHANGED,
                userNodesPreserved = userNodesPreserved
            )
        }

        flowGraphFile.writeText(newContent)
        return FlowKtMergeReport(
            mode = FlowKtMergeMode.UPDATED,
            portsAdded = portsAdded,
            portsRemoved = portsRemoved,
            connectionsDropped = connectionsDropped,
            userNodesPreserved = userNodesPreserved
        )
    }

    /**
     * Builds an in-memory bootstrap [FlowGraph] for [spec]: a Source CodeNode mirroring
     * `spec.sourceOutputs` and a Sink CodeNode mirroring `spec.sinkInputs`, with no
     * connections. New ports use `Any::class`; user-driven IP-type assignments come later
     * via the GraphEditor connection workflow.
     */
    private fun buildBootstrapGraph(spec: UIFBPSpec): FlowGraph {
        val nodes = mutableListOf<Node>()
        val zeroPosition = Node.Position(0.0, 0.0)
        if (spec.sourceOutputs.isNotEmpty()) {
            val sourceId = "node_${spec.flowGraphPrefix.lowercase()}source"
            val outputPorts: List<Port<*>> = spec.sourceOutputs.map { p ->
                PortFactory.outputWithType(p.name, Any::class, sourceId)
            }
            nodes += CodeNode(
                id = sourceId,
                name = "${spec.flowGraphPrefix}Source",
                codeNodeType = CodeNodeType.SOURCE,
                position = zeroPosition,
                outputPorts = outputPorts
            )
        }
        if (spec.sinkInputs.isNotEmpty()) {
            val sinkId = "node_${spec.flowGraphPrefix.lowercase()}sink"
            val inputPorts: List<Port<*>> = spec.sinkInputs.map { p ->
                PortFactory.inputWithType(p.name, Any::class, sinkId, required = true)
            }
            nodes += CodeNode(
                id = sinkId,
                name = "${spec.flowGraphPrefix}Sink",
                codeNodeType = CodeNodeType.SINK,
                position = zeroPosition,
                inputPorts = inputPorts
            )
        }
        return FlowGraph(
            id = "flow_${spec.flowGraphPrefix.lowercase()}",
            name = spec.flowGraphPrefix,
            version = "1.0.0",
            rootNodes = nodes,
            connections = emptyList()
        )
    }

    /**
     * Builds a `portId → typeName` override map for bootstrap emission so that the spec's
     * declared port type names (e.g., `Double`, `CalculationResults`) appear in the
     * `.flow.kt` rather than the `Any` we use for the in-memory [Port.dataType].
     */
    private fun portTypeOverridesForBootstrap(
        spec: UIFBPSpec,
        bootstrap: FlowGraph
    ): Map<String, String> {
        val overrides = mutableMapOf<String, String>()
        val codeNodes = bootstrap.rootNodes.filterIsInstance<CodeNode>()
        codeNodes.firstOrNull { it.codeNodeType == CodeNodeType.SOURCE }?.outputPorts?.forEach { port ->
            spec.sourceOutputs.firstOrNull { it.name == port.name }
                ?.let { overrides[port.id] = it.typeName }
        }
        codeNodes.firstOrNull { it.codeNodeType == CodeNodeType.SINK }?.inputPorts?.forEach { port ->
            spec.sinkInputs.firstOrNull { it.name == port.name }
                ?.let { overrides[port.id] = it.typeName }
        }
        return overrides
    }
}

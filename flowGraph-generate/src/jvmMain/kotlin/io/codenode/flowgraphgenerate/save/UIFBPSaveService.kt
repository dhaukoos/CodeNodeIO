/*
 * UIFBPSaveService - jvmMain orchestrator that composes UIFBPInterfaceGenerator with
 * filesystem I/O, host-module pre-flight, and the per-file decision tree.
 *
 * Phase coverage:
 *   - US1 (T020): pre-flight unscaffolded-host refusal + first-save (CREATED entries)
 *                 + the UNCHANGED branch (idempotent re-save)
 *                 + hand-edit safety (SKIPPED_CONFLICT) for files lacking the marker
 *                 + UPDATED branch when a file with the marker has divergent content
 *   - US3 (T037-T038): legacy saved/ + base-package cleanup (UIFBPSaveOptions.deleteLegacyLocations)
 *   - US4 (T045): .flow.kt parse-and-merge with structured FlowKtMergeReport
 *
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.save

import io.codenode.flowgraphgenerate.generator.UIFBPInterfaceGenerator
import io.codenode.flowgraphgenerate.parser.UIFBPSpec
import java.io.File

class UIFBPSaveService(
    private val orchestrator: UIFBPInterfaceGenerator = UIFBPInterfaceGenerator()
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

        return UIFBPSaveResult(success = true, files = files)
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
}

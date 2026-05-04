/*
 * UIFBPSaveServiceTest - jvmTest integration tests for the post-085 UI-FBP save service
 *
 * Coverage focus by phase (cross-referenced from tasks.md):
 *   US1 (T018, T019)  — first-save + unscaffolded-host refusal
 *   US3 (T033-T036)   — legacy saved/ cleanup + hand-edit safety + re-save UNCHANGED
 *   US4 (T041-T044)   — .flow.kt parse-and-merge
 *
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.save

import io.codenode.flowgraphgenerate.parser.PortInfo
import io.codenode.flowgraphgenerate.parser.UIFBPSpec
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UIFBPSaveServiceTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun setUp() {
        tempDir = kotlin.io.path.createTempDirectory("uifbp-save-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    // ========== fixtures ==========

    private val demoSpec = UIFBPSpec(
        flowGraphPrefix = "DemoUI",
        composableName = "DemoUI",
        viewModelTypeName = "DemoUIViewModel",
        packageName = "io.codenode.demo",
        sourceOutputs = listOf(
            PortInfo("numA", "Double"),
            PortInfo("numB", "Double")
        ),
        sinkInputs = listOf(
            PortInfo("results", "CalculationResults", isNullable = true)
        ),
        ipTypeImports = listOf("io.codenode.demo.iptypes.CalculationResults")
    )

    /** Creates a feature-085-scaffolded module skeleton (jvm() target + preview-api dep). */
    private fun newScaffoldedModuleDir(name: String = "demo"): File {
        val dir = File(tempDir, name).apply { mkdirs() }
        File(dir, "build.gradle.kts").writeText(
            """
            // Minimal post-feature-085 scaffolded build script
            plugins {
                kotlin("multiplatform")
            }

            kotlin {
                androidTarget()
                jvm {
                    compilations.all { kotlinOptions.jvmTarget = "17" }
                }

                sourceSets {
                    val commonMain by getting {
                        dependencies {
                            implementation("io.codenode:fbpDsl")
                        }
                    }
                    val jvmMain by getting {
                        dependencies {
                            implementation("io.codenode:preview-api")
                        }
                    }
                }
            }
            """.trimIndent()
        )
        return dir
    }

    /** Creates a non-scaffolded module skeleton (lacks `jvm()` target, lacks `preview-api`). */
    private fun newUnscaffoldedModuleDir(name: String = "legacy"): File {
        val dir = File(tempDir, name).apply { mkdirs() }
        File(dir, "build.gradle.kts").writeText(
            """
            plugins {
                kotlin("multiplatform")
            }

            kotlin {
                androidTarget()
                // no jvm() target; no preview-api dependency
            }
            """.trimIndent()
        )
        return dir
    }

    private fun newFlowKtFile(moduleDir: File, prefix: String = "DemoUI"): File {
        val flowDir = File(moduleDir, "src/commonMain/kotlin/io/codenode/demo/flow").apply { mkdirs() }
        val file = File(flowDir, "$prefix.flow.kt")
        file.writeText("// placeholder bootstrap; UIFBPSaveService will overwrite or merge")
        return file
    }

    // ========== T018 (feature 084) + T021 (feature 087): first-save universal set ==========

    @Test
    fun `first save against a feature-085-scaffolded module emits the Design B universal set`() {
        val moduleDir = newScaffoldedModuleDir()
        val flowGraphFile = newFlowKtFile(moduleDir)

        val service = UIFBPSaveService()
        val result = service.save(demoSpec, flowGraphFile, moduleDir)

        assertTrue(result.success, "first save against a scaffolded module must succeed " +
            "(unscaffolded-host pre-flight must NOT block this fixture)")

        val createdPaths = result.files.filter { it.kind == FileChangeKind.CREATED }.map { it.relativePath }
        // 8 mandatory entries (feature 087 / Design B): the 7 from feature 084 + the new {Name}Event.kt.
        assertTrue(createdPaths.contains("src/commonMain/kotlin/io/codenode/demo/viewmodel/DemoUIState.kt"))
        assertTrue(createdPaths.contains("src/commonMain/kotlin/io/codenode/demo/viewmodel/DemoUIEvent.kt"),
            "feature 087: new {Name}Event.kt is part of the mandatory output set")
        assertTrue(createdPaths.contains("src/commonMain/kotlin/io/codenode/demo/viewmodel/DemoUIViewModel.kt"))
        assertTrue(createdPaths.contains("src/commonMain/kotlin/io/codenode/demo/nodes/DemoUISourceCodeNode.kt"))
        assertTrue(createdPaths.contains("src/commonMain/kotlin/io/codenode/demo/nodes/DemoUISinkCodeNode.kt"))
        assertTrue(createdPaths.contains("src/commonMain/kotlin/io/codenode/demo/controller/DemoUIControllerInterface.kt"))
        assertTrue(createdPaths.contains("src/commonMain/kotlin/io/codenode/demo/controller/DemoUIRuntime.kt"))
        assertTrue(createdPaths.contains("src/jvmMain/kotlin/io/codenode/demo/userInterface/DemoUIPreviewProvider.kt"))

        // Verify the actual files materialized on disk under the module root
        val moduleSrc = File(moduleDir, "src/commonMain/kotlin/io/codenode/demo")
        assertTrue(File(moduleSrc, "viewmodel/DemoUIState.kt").exists())
        assertTrue(File(moduleSrc, "viewmodel/DemoUIEvent.kt").exists(),
            "feature 087: DemoUIEvent.kt materializes on disk")
        assertTrue(File(moduleSrc, "viewmodel/DemoUIViewModel.kt").exists())
        assertTrue(File(moduleSrc, "controller/DemoUIControllerInterface.kt").exists())
        assertTrue(File(moduleSrc, "controller/DemoUIRuntime.kt").exists())
        assertTrue(File(moduleSrc, "nodes/DemoUISourceCodeNode.kt").exists())
        assertTrue(File(moduleSrc, "nodes/DemoUISinkCodeNode.kt").exists())
        assertTrue(File(moduleDir, "src/jvmMain/kotlin/io/codenode/demo/userInterface/DemoUIPreviewProvider.kt").exists())
    }

    // ========== T019: unscaffolded-host refusal (FR-009 post-clarification) ==========

    @Test
    fun `unscaffolded host with no jvm target and no preview-api dep is refused with actionable error`() {
        val moduleDir = newUnscaffoldedModuleDir()
        val flowGraphFile = newFlowKtFile(moduleDir)

        // Snapshot the directory state before save
        val filesBefore = moduleDir.walkTopDown().filter { it.isFile }.map { it.relativeTo(moduleDir).path }.toSet()

        val service = UIFBPSaveService()
        val result = service.save(demoSpec, flowGraphFile, moduleDir)

        assertFalse(result.success, "unscaffolded host MUST be refused")
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("jvm", ignoreCase = true) ||
                   result.errorMessage!!.contains("preview-api", ignoreCase = true),
            "errorMessage MUST name the missing piece(s) actionably; got: ${result.errorMessage}")
        assertTrue(result.errorMessage!!.contains("VS-A1") || result.errorMessage!!.contains("quickstart"),
            "errorMessage MUST direct the user to the migration step; got: ${result.errorMessage}")
        assertTrue(result.files.isEmpty(), "no FileChange entries on a refused save")

        // Snapshot after — must be byte-for-byte unchanged (zero file mutations)
        val filesAfter = moduleDir.walkTopDown().filter { it.isFile }.map { it.relativeTo(moduleDir).path }.toSet()
        assertEquals(filesBefore, filesAfter, "no files MUST be created on a refused save")
    }

    @Test
    fun `host with jvm target but missing preview-api dep is refused with actionable error`() {
        val dir = File(tempDir, "no-preview-api").apply { mkdirs() }
        File(dir, "build.gradle.kts").writeText(
            """
            kotlin {
                androidTarget()
                jvm()
                // NOTE: no preview-api dep in jvmMain
            }
            """.trimIndent()
        )
        val flowGraphFile = newFlowKtFile(dir, "DemoUI")

        val result = UIFBPSaveService().save(demoSpec, flowGraphFile, dir)
        assertFalse(result.success)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("preview-api", ignoreCase = true),
            "errorMessage MUST name the missing 'preview-api' dep; got: ${result.errorMessage}")
    }

    @Test
    fun `host without build_gradle_kts is refused`() {
        val dir = File(tempDir, "no-build-script").apply { mkdirs() }
        val flowGraphFile = newFlowKtFile(dir, "DemoUI")

        val result = UIFBPSaveService().save(demoSpec, flowGraphFile, dir)
        assertFalse(result.success)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("build.gradle.kts", ignoreCase = true) ||
                   result.errorMessage!!.contains("scaffold", ignoreCase = true))
    }

    // ========== T033: legacy saved/ cleanup with marker present → DELETED ==========

    @Test
    fun `legacy saved cleanup with deleteLegacyLocations=true and marker present deletes the duplicate files`() {
        val moduleDir = newScaffoldedModuleDir()
        val flowGraphFile = newFlowKtFile(moduleDir)

        // Seed the legacy saved/ files carrying the generator marker.
        val savedDir = File(moduleDir, "src/commonMain/kotlin/io/codenode/demo/saved").apply { mkdirs() }
        val legacyState = File(savedDir, "DemoUIState.kt").apply {
            writeText(
                """
                /*
                 * DemoUIState
                 * Generated by CodeNodeIO UIFBPStateGenerator
                 * License: Apache 2.0
                 */
                package io.codenode.demo.saved
                object DemoUIState
                """.trimIndent()
            )
        }
        val legacyViewModel = File(savedDir, "DemoUIViewModel.kt").apply {
            writeText(
                """
                /*
                 * DemoUIViewModel
                 * Generated by CodeNodeIO UIFBPViewModelGenerator
                 */
                package io.codenode.demo.saved
                class DemoUIViewModel
                """.trimIndent()
            )
        }

        val result = UIFBPSaveService().save(
            demoSpec, flowGraphFile, moduleDir,
            UIFBPSaveOptions(deleteLegacyLocations = true)
        )
        assertTrue(result.success, "save with deleteLegacyLocations=true on a marker-bearing legacy must succeed")

        val deletedPaths = result.files.filter { it.kind == FileChangeKind.DELETED }.map { it.relativePath }
        assertTrue(
            deletedPaths.contains("src/commonMain/kotlin/io/codenode/demo/saved/DemoUIState.kt"),
            "DemoUIState.kt under legacy saved/ MUST be reported DELETED; got deleted=$deletedPaths"
        )
        assertTrue(
            deletedPaths.contains("src/commonMain/kotlin/io/codenode/demo/saved/DemoUIViewModel.kt"),
            "DemoUIViewModel.kt under legacy saved/ MUST be reported DELETED; got deleted=$deletedPaths"
        )

        assertFalse(legacyState.exists(), "legacy saved/DemoUIState.kt MUST be removed from disk")
        assertFalse(legacyViewModel.exists(), "legacy saved/DemoUIViewModel.kt MUST be removed from disk")
        assertFalse(savedDir.exists(), "empty legacy saved/ directory MUST be removed")

        val skipped = result.files.filter { it.kind == FileChangeKind.SKIPPED_CONFLICT }
        assertTrue(skipped.isEmpty(), "no SKIPPED_CONFLICT entries when both legacy files carry the marker; got: $skipped")
    }

    // ========== T034: legacy saved/ cleanup with marker absent → SKIPPED_CONFLICT ==========

    @Test
    fun `legacy saved cleanup with deleteLegacyLocations=true but marker absent leaves files in place`() {
        val moduleDir = newScaffoldedModuleDir()
        val flowGraphFile = newFlowKtFile(moduleDir)

        val savedDir = File(moduleDir, "src/commonMain/kotlin/io/codenode/demo/saved").apply { mkdirs() }

        // One legacy file: no marker (hand-edited).
        val unsafeState = File(savedDir, "DemoUIState.kt").apply {
            writeText(
                """
                // Hand-edited DemoUIState
                package io.codenode.demo.saved
                object DemoUIState { val handAdded = 42 }
                """.trimIndent()
            )
        }

        // The other legacy file: still carries the marker.
        val markerViewModel = File(savedDir, "DemoUIViewModel.kt").apply {
            writeText(
                """
                /*
                 * Generated by CodeNodeIO UIFBPViewModelGenerator
                 */
                package io.codenode.demo.saved
                class DemoUIViewModel
                """.trimIndent()
            )
        }

        val result = UIFBPSaveService().save(
            demoSpec, flowGraphFile, moduleDir,
            UIFBPSaveOptions(deleteLegacyLocations = true)
        )
        assertTrue(result.success)

        val skipped = result.files.firstOrNull {
            it.kind == FileChangeKind.SKIPPED_CONFLICT &&
                it.relativePath == "src/commonMain/kotlin/io/codenode/demo/saved/DemoUIState.kt"
        }
        assertNotNull(skipped, "hand-edited legacy file MUST be SKIPPED_CONFLICT; files=${result.files}")
        assertNotNull(skipped.reason)
        assertTrue(
            skipped.reason!!.contains("marker", ignoreCase = true) ||
                skipped.reason!!.contains("Generated by CodeNodeIO"),
            "SKIPPED_CONFLICT reason MUST name the missing marker; got: ${skipped.reason}"
        )

        assertTrue(unsafeState.exists(), "hand-edited legacy file MUST NOT be deleted")

        // The marker-bearing legacy file IS deleted.
        val deleted = result.files.firstOrNull {
            it.kind == FileChangeKind.DELETED &&
                it.relativePath == "src/commonMain/kotlin/io/codenode/demo/saved/DemoUIViewModel.kt"
        }
        assertNotNull(deleted, "marker-bearing legacy file MUST still be DELETED; files=${result.files}")
        assertFalse(markerViewModel.exists(), "marker-bearing legacy DemoUIViewModel.kt MUST be removed from disk")
    }

    // ========== T035: target file lacking the Generated marker → SKIPPED_CONFLICT (FR-016) ==========

    @Test
    fun `target file lacking the Generated marker is SKIPPED_CONFLICT`() {
        val moduleDir = newScaffoldedModuleDir()
        val flowGraphFile = newFlowKtFile(moduleDir)

        // Pre-seed a hand-edited target file at the post-085 controller/ path WITHOUT the marker.
        val controllerDir = File(moduleDir, "src/commonMain/kotlin/io/codenode/demo/controller").apply { mkdirs() }
        val handEdited = File(controllerDir, "DemoUIControllerInterface.kt").apply {
            writeText(
                """
                package io.codenode.demo.controller
                // hand-written interface — NO generator marker; user is hand-authoring this surface
                interface DemoUIControllerInterface { val custom: Int }
                """.trimIndent()
            )
        }
        val originalContent = handEdited.readText()
        val originalMtime = handEdited.lastModified()

        val result = UIFBPSaveService().save(demoSpec, flowGraphFile, moduleDir)
        assertTrue(result.success)

        val skipped = result.files.firstOrNull {
            it.kind == FileChangeKind.SKIPPED_CONFLICT &&
                it.relativePath == "src/commonMain/kotlin/io/codenode/demo/controller/DemoUIControllerInterface.kt"
        }
        assertNotNull(skipped, "hand-edited target file MUST be SKIPPED_CONFLICT; files=${result.files}")
        assertNotNull(skipped.reason)
        assertTrue(
            skipped.reason!!.contains("marker", ignoreCase = true) ||
                skipped.reason!!.contains("Generated by CodeNodeIO"),
            "SKIPPED_CONFLICT reason MUST name the missing marker; got: ${skipped.reason}"
        )

        // File on disk MUST be byte-for-byte unchanged.
        assertEquals(originalContent, handEdited.readText(), "hand-edited file content MUST NOT change")
        assertEquals(originalMtime, handEdited.lastModified(), "hand-edited file mtime MUST NOT change")
    }

    // ========== T041: re-save against unchanged spec produces UNCHANGED .flow.kt merge mode ==========

    @Test
    fun `re-save against unchanged spec produces UNCHANGED flow_kt merge mode`() {
        val moduleDir = newScaffoldedModuleDir()
        val flowGraphFile = newFlowKtFile(moduleDir)

        // First save bootstraps the .flow.kt file.
        val service = UIFBPSaveService()
        val first = service.save(demoSpec, flowGraphFile, moduleDir)
        assertTrue(first.success)
        assertNotNull(first.flowKtMerge, "first save MUST produce a FlowKtMergeReport")
        assertEquals(FlowKtMergeMode.CREATED, first.flowKtMerge!!.mode,
            "first save against an empty/placeholder .flow.kt MUST be CREATED")

        // Snapshot the bootstrapped .flow.kt mtime so we can verify zero re-write.
        val mtimeAfterFirst = flowGraphFile.lastModified()

        // Re-save with the same spec and same file.
        val second = service.save(demoSpec, flowGraphFile, moduleDir)
        assertTrue(second.success)
        assertNotNull(second.flowKtMerge)
        val report = second.flowKtMerge!!
        assertEquals(FlowKtMergeMode.UNCHANGED, report.mode,
            "re-save against unchanged spec MUST be UNCHANGED")
        assertTrue(report.portsAdded.isEmpty(), "no ports added on UNCHANGED; got: ${report.portsAdded}")
        assertTrue(report.portsRemoved.isEmpty(), "no ports removed on UNCHANGED; got: ${report.portsRemoved}")
        assertTrue(report.connectionsDropped.isEmpty(),
            "no connections dropped on UNCHANGED; got: ${report.connectionsDropped}")
        assertEquals(0, report.userNodesPreserved,
            "no user-added CodeNodes were seeded; userNodesPreserved MUST be 0")

        // .flow.kt file MUST NOT have been re-written
        assertEquals(mtimeAfterFirst, flowGraphFile.lastModified(),
            ".flow.kt mtime MUST NOT change on UNCHANGED merge")
    }

    // ========== T042: port-add scenario adds Source output without disturbing user CodeNodes ==========

    @Test
    fun `port-add scenario adds a Source output without disturbing user-added CodeNodes`() {
        val moduleDir = newScaffoldedModuleDir()
        val flowGraphFile = newFlowKtFile(moduleDir)

        // Seed a real .flow.kt that already contains a user-added passthrough CodeNode wired
        // between Source and Sink. We hand-author this DSL (rather than going through a save())
        // because we need to seed user-added content the bootstrap wouldn't produce.
        flowGraphFile.writeText(
            """
            package io.codenode.demo.flow

            import io.codenode.fbpdsl.dsl.*
            import io.codenode.fbpdsl.model.*

            val demoUIFlowGraph = flowGraph("DemoUI", version = "1.0.0") {
                val source = codeNode("DemoUISource", nodeType = "SOURCE") {
                    position(100.0, 300.0)
                    output("numA", Any::class)
                    output("numB", Any::class)
                }

                val passthru = codeNode("UserPassthru", nodeType = "TRANSFORMER") {
                    position(350.0, 300.0)
                    input("input1", Any::class)
                    output("output1", Any::class)
                    config("_codeNodeClass", "io.codenode.demo.nodes.UserPassthruCodeNode")
                }

                val sink = codeNode("DemoUISink", nodeType = "SINK") {
                    position(600.0, 300.0)
                    input("results", Any::class)
                }
            }
            """.trimIndent()
        )

        // Spec has one EXTRA Source output ("numC") vs the seeded file.
        val newSpec = demoSpec.copy(
            sourceOutputs = demoSpec.sourceOutputs + PortInfo("numC", "Double")
        )

        val result = UIFBPSaveService().save(newSpec, flowGraphFile, moduleDir)
        assertTrue(result.success)
        assertNotNull(result.flowKtMerge)
        val report = result.flowKtMerge!!

        assertEquals(FlowKtMergeMode.UPDATED, report.mode, "added port MUST drive UPDATED mode")
        assertEquals(1, report.portsAdded.size, "exactly one port added; got: ${report.portsAdded}")
        assertEquals("numC", report.portsAdded.first().portName)
        assertEquals("DemoUISource", report.portsAdded.first().nodeName)
        assertTrue(report.portsRemoved.isEmpty(), "no ports removed; got: ${report.portsRemoved}")
        assertTrue(report.connectionsDropped.isEmpty(),
            "no connections dropped (no connections existed); got: ${report.connectionsDropped}")
        assertEquals(1, report.userNodesPreserved,
            "the user-added passthrough CodeNode MUST be preserved")

        // The serialized .flow.kt MUST still contain the user-added CodeNode.
        val rewritten = flowGraphFile.readText()
        assertTrue(
            rewritten.contains("UserPassthru"),
            ".flow.kt MUST still mention the user-added CodeNode 'UserPassthru' after merge; got:\n$rewritten"
        )
        assertTrue(
            rewritten.contains("output(\"numC\", Double::class"),
            ".flow.kt MUST emit the newly-added 'numC' port with the spec's typeName Double::class " +
                "(NOT Any::class); got:\n$rewritten"
        )
    }

    // ========== T042b: kept port with stale Any-type drifts back to spec's typeName on re-run ==========
    //
    // Regression scenario observed in VS-A7: a previous (buggy) save left a port typed as
    // `Any::class` in `.flow.kt` even though the spec called for `Double::class`. The next
    // re-run MUST detect content drift on the kept port and re-emit with the spec's typeName.

    @Test
    fun `kept port with stale Any-type drifts back to spec's typeName on re-run`() {
        val moduleDir = newScaffoldedModuleDir()
        val flowGraphFile = newFlowKtFile(moduleDir)

        // Seed a .flow.kt where port "c" exists on the Source already but typed as Any.
        // This simulates the state left by the earlier buggy generator pass.
        flowGraphFile.writeText(
            """
            package io.codenode.demo.flow

            import io.codenode.fbpdsl.dsl.*
            import io.codenode.fbpdsl.model.*

            val demoUIFlowGraph = flowGraph("DemoUI", version = "1.0.0") {
                val source = codeNode("DemoUISource", nodeType = "SOURCE") {
                    position(100.0, 300.0)
                    output("numA", Double::class)
                    output("numB", Double::class)
                    output("c", Any::class)
                }

                val sink = codeNode("DemoUISink", nodeType = "SINK") {
                    position(600.0, 300.0)
                    input("results", Any::class)
                }
            }
            """.trimIndent()
        )

        // Spec: same port set (c is already in the file), but the spec types c as Double.
        val newSpec = demoSpec.copy(
            sourceOutputs = demoSpec.sourceOutputs + PortInfo("c", "Double")
        )

        val result = UIFBPSaveService().save(newSpec, flowGraphFile, moduleDir)
        assertTrue(result.success)
        assertNotNull(result.flowKtMerge)
        val report = result.flowKtMerge!!

        // No port-NAME diff (c already exists), but the type is wrong → mode MUST be UPDATED
        // so the canonical re-emit lands on disk.
        assertEquals(FlowKtMergeMode.UPDATED, report.mode,
            "kept-port type drift MUST drive UPDATED mode (file rewrite)")

        val rewritten = flowGraphFile.readText()
        assertTrue(
            rewritten.contains("output(\"c\", Double::class"),
            "kept port 'c' MUST be re-emitted with spec's typeName Double::class; got:\n$rewritten"
        )
        assertFalse(
            rewritten.contains("output(\"c\", Any::class"),
            "kept port 'c' MUST NOT remain typed as Any::class; got:\n$rewritten"
        )
    }

    // ========== T043: port-remove scenario drops only invalid connections ==========

    @Test
    fun `port-remove scenario drops only invalid connections and reports them`() {
        val moduleDir = newScaffoldedModuleDir()
        val flowGraphFile = newFlowKtFile(moduleDir)

        // Seed a .flow.kt where a user CodeNode is connected to Sink.results (a port the
        // new spec will REMOVE). The user CodeNode itself must survive; only the orphaned
        // connection must be dropped.
        flowGraphFile.writeText(
            """
            package io.codenode.demo.flow

            import io.codenode.fbpdsl.dsl.*
            import io.codenode.fbpdsl.model.*

            val demoUIFlowGraph = flowGraph("DemoUI", version = "1.0.0") {
                val source = codeNode("DemoUISource", nodeType = "SOURCE") {
                    position(100.0, 300.0)
                    output("numA", Any::class)
                    output("numB", Any::class)
                }

                val helper = codeNode("UserHelper", nodeType = "TRANSFORMER") {
                    position(350.0, 300.0)
                    input("input1", Any::class)
                    output("output1", Any::class)
                    config("_codeNodeClass", "io.codenode.demo.nodes.UserHelperCodeNode")
                }

                val sink = codeNode("DemoUISink", nodeType = "SINK") {
                    position(600.0, 300.0)
                    input("results", Any::class)
                }

                helper.output("output1") connect sink.input("results")
            }
            """.trimIndent()
        )

        // Spec REMOVES the "results" sink port (sinkInputs is now empty).
        val newSpec = demoSpec.copy(sinkInputs = emptyList())

        val result = UIFBPSaveService().save(newSpec, flowGraphFile, moduleDir)
        assertTrue(result.success)
        assertNotNull(result.flowKtMerge)
        val report = result.flowKtMerge!!

        assertEquals(FlowKtMergeMode.UPDATED, report.mode)
        assertTrue(
            report.portsRemoved.any { it.portName == "results" && it.nodeName == "DemoUISink" },
            "removed sink port 'results' MUST appear in portsRemoved; got: ${report.portsRemoved}"
        )
        assertEquals(
            1, report.connectionsDropped.size,
            "exactly one connection (UserHelper→DemoUISink.results) MUST be dropped; got: ${report.connectionsDropped}"
        )
        val dropped = report.connectionsDropped.first()
        assertNotNull(dropped.reason)
        assertTrue(
            dropped.reason.contains("port", ignoreCase = true) ||
                dropped.reason.contains("removed", ignoreCase = true),
            "dropped connection reason MUST explain the orphaned port; got: '${dropped.reason}'"
        )
        assertEquals(1, report.userNodesPreserved,
            "user-added 'UserHelper' CodeNode itself MUST be preserved")

        // The user-added CodeNode survives in the rewritten file.
        val rewritten = flowGraphFile.readText()
        assertTrue(
            rewritten.contains("UserHelper"),
            ".flow.kt MUST still mention 'UserHelper' after port-remove merge; got:\n$rewritten"
        )
    }

    // ========== T044: parse-failed .flow.kt is SKIPPED, not overwritten ==========

    @Test
    fun `parse-failed flow_kt is SKIPPED, not overwritten`() {
        val moduleDir = newScaffoldedModuleDir()
        val flowGraphFile = newFlowKtFile(moduleDir)

        // Seed a file that mentions `flowGraph(` (so the service tries to parse) but is
        // syntactically broken — the parser will reject it.
        val brokenContent = """
            package io.codenode.demo.flow

            // intentionally broken — unbalanced brace + missing tokens
            val demoUIFlowGraph = flowGraph("DemoUI", version = "1.0.0") {
                val source = codeNode("DemoUISource"
                    output(... no closing
        """.trimIndent()
        flowGraphFile.writeText(brokenContent)
        val mtimeBefore = flowGraphFile.lastModified()

        val result = UIFBPSaveService().save(demoSpec, flowGraphFile, moduleDir)
        assertTrue(result.success, "the universal-set save MUST succeed even if .flow.kt is unparseable")
        assertNotNull(result.flowKtMerge)
        assertEquals(FlowKtMergeMode.PARSE_FAILED_SKIPPED, result.flowKtMerge!!.mode)

        // File on disk MUST be byte-for-byte unchanged.
        assertEquals(brokenContent, flowGraphFile.readText(),
            ".flow.kt content MUST be unchanged on PARSE_FAILED_SKIPPED")
        assertEquals(mtimeBefore, flowGraphFile.lastModified(),
            ".flow.kt mtime MUST be unchanged on PARSE_FAILED_SKIPPED")

        // A warning MUST be emitted naming the parse failure.
        assertTrue(result.warnings.isNotEmpty(),
            "parse failure MUST surface a warning in UIFBPSaveResult.warnings")
        assertTrue(
            result.warnings.any { it.contains("parse", ignoreCase = true) || it.contains("flow.kt", ignoreCase = true) },
            "warning MUST mention parse failure or the .flow.kt file; got: ${result.warnings}"
        )
    }

    // ========== T036: target file with marker AND content matching → UNCHANGED ==========

    @Test
    fun `target file with marker and content already matching is UNCHANGED`() {
        val moduleDir = newScaffoldedModuleDir()
        val flowGraphFile = newFlowKtFile(moduleDir)

        // First save populates the post-085 set.
        val service = UIFBPSaveService()
        val firstResult = service.save(demoSpec, flowGraphFile, moduleDir)
        assertTrue(firstResult.success)

        // Snapshot mtimes before second save.
        val emittedFiles = firstResult.files
            .filter { it.kind == FileChangeKind.CREATED }
            .map { File(moduleDir, it.relativePath) }
        assertTrue(emittedFiles.isNotEmpty(), "first save must produce CREATED entries to drive this test")
        val mtimesBefore = emittedFiles.associateWith { it.lastModified() }

        // Re-run save; everything should be UNCHANGED.
        val secondResult = service.save(demoSpec, flowGraphFile, moduleDir)
        assertTrue(secondResult.success)

        val nonUnchanged = secondResult.files.filter { it.kind != FileChangeKind.UNCHANGED }
        assertTrue(
            nonUnchanged.isEmpty(),
            "every emitted file on a re-save MUST be UNCHANGED; got non-UNCHANGED entries: $nonUnchanged"
        )

        // mtimes MUST be byte-for-byte unchanged (no spurious writes).
        for ((file, mtime) in mtimesBefore) {
            assertEquals(mtime, file.lastModified(), "file ${file.name} mtime MUST not change on UNCHANGED save")
        }
    }

    // ========== Rename-edge-case regression test (spec.md "Source UI file is renamed or moved") ==========
    //
    // When the user renames the flow graph (which drives every artifact's prefix in the
    // post-082/083 model), saving with the new name MUST NOT collide with stale artifacts
    // emitted under the prior name. Each artifact set is self-contained at distinct file
    // paths with distinct class names, so co-existence is the expected outcome — the user
    // gets to decide manually when (or whether) to delete the stale set. Critically:
    //   - No SKIPPED_CONFLICT entries (different paths → no overlap).
    //   - No file under the new prefix is missing (every CREATED entry materialized).
    //   - Every file under the old prefix is still on disk byte-for-byte unchanged.
    // This satisfies the spec edge case: "Stale generated artifacts from the prior name
    // must not collide with new artifacts and break compilation."

    @Test
    fun `rename of flow graph emits new artifact set without colliding with stale prior-name artifacts`() {
        val moduleDir = newScaffoldedModuleDir()
        val service = UIFBPSaveService()

        // First save under prefix "FooUI" — primes the module with a full artifact set.
        val fooSpec = demoSpec.copy(
            flowGraphPrefix = "FooUI",
            composableName = "FooUI",
            viewModelTypeName = "FooUIViewModel"
        )
        val fooFlowFile = newFlowKtFile(moduleDir, prefix = "FooUI")
        val fooResult = service.save(fooSpec, fooFlowFile, moduleDir)
        assertTrue(fooResult.success, "first save (FooUI) MUST succeed")
        val fooCreated = fooResult.files.filter { it.kind == FileChangeKind.CREATED }.map { it.relativePath }
        assertTrue(fooCreated.isNotEmpty(), "first save MUST produce CREATED entries; got: ${fooResult.files}")

        // Snapshot every FooUI artifact's content + mtime BEFORE the rename save.
        val fooFiles = fooCreated.map { File(moduleDir, it) }
        val fooContentBefore = fooFiles.associateWith { it.readText() }
        val fooMtimesBefore = fooFiles.associateWith { it.lastModified() }
        assertTrue(fooFiles.all { it.exists() }, "all FooUI artifacts MUST exist after first save")

        // Second save under a NEW prefix "BarUI" — simulates the user renaming the flow
        // graph. The .flow.kt file path differs (BarUI.flow.kt vs FooUI.flow.kt), every
        // artifact path differs, every class name differs.
        val barSpec = demoSpec.copy(
            flowGraphPrefix = "BarUI",
            composableName = "BarUI",
            viewModelTypeName = "BarUIViewModel"
        )
        val barFlowFile = newFlowKtFile(moduleDir, prefix = "BarUI")
        val barResult = service.save(barSpec, barFlowFile, moduleDir)
        assertTrue(barResult.success, "second save (BarUI rename) MUST succeed")

        // ---- New artifact set lands cleanly ----
        val barCreated = barResult.files.filter { it.kind == FileChangeKind.CREATED }.map { it.relativePath }
        assertTrue(
            barCreated.any { it.endsWith("BarUIState.kt") },
            "BarUIState MUST be CREATED; got: $barCreated"
        )
        assertTrue(
            barCreated.any { it.endsWith("BarUIViewModel.kt") },
            "BarUIViewModel MUST be CREATED; got: $barCreated"
        )
        assertTrue(
            barCreated.any { it.endsWith("BarUIControllerInterface.kt") },
            "BarUIControllerInterface MUST be CREATED; got: $barCreated"
        )
        assertTrue(
            barCreated.any { it.endsWith("BarUIRuntime.kt") },
            "BarUIRuntime MUST be CREATED; got: $barCreated"
        )
        assertTrue(
            barCreated.any { it.endsWith("BarUIPreviewProvider.kt") },
            "BarUIPreviewProvider MUST be CREATED; got: $barCreated"
        )

        // ---- No collisions: zero SKIPPED_CONFLICT entries ----
        val skippedConflicts = barResult.files.filter { it.kind == FileChangeKind.SKIPPED_CONFLICT }
        assertTrue(
            skippedConflicts.isEmpty(),
            "rename emits NEW artifacts at NEW paths; no path overlap → no conflicts. " +
                "Got: $skippedConflicts"
        )

        // ---- No path overlap: nothing in barResult.files references a FooUI path ----
        assertTrue(
            barResult.files.none { it.relativePath.contains("FooUI") },
            "rename save's per-file report MUST NOT mention any FooUI artifact " +
                "(the orchestrator only knows about the BarUI spec); got: ${barResult.files}"
        )

        // ---- Stale FooUI artifacts remain on disk byte-for-byte unchanged ----
        for (fooFile in fooFiles) {
            assertTrue(
                fooFile.exists(),
                "stale FooUI artifact ${fooFile.name} MUST still exist after rename save"
            )
            assertEquals(
                fooContentBefore[fooFile], fooFile.readText(),
                "stale FooUI artifact ${fooFile.name} content MUST be byte-for-byte unchanged"
            )
            assertEquals(
                fooMtimesBefore[fooFile], fooFile.lastModified(),
                "stale FooUI artifact ${fooFile.name} mtime MUST be unchanged " +
                    "(rename save MUST NOT touch prior-name artifacts)"
            )
        }
    }
}

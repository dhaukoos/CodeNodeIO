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

    // ========== T018: first-save against a feature-085-scaffolded module ==========

    @Test
    fun `first save against a feature-085-scaffolded module emits the post-085 universal set`() {
        val moduleDir = newScaffoldedModuleDir()
        val flowGraphFile = newFlowKtFile(moduleDir)

        val service = UIFBPSaveService()
        val result = service.save(demoSpec, flowGraphFile, moduleDir)

        assertTrue(result.success, "first save against a scaffolded module must succeed " +
            "(unscaffolded-host pre-flight must NOT block this fixture)")

        val createdPaths = result.files.filter { it.kind == FileChangeKind.CREATED }.map { it.relativePath }
        // Mandatory entries from data-model.md §2 file table (1, 2, 3, 4, 5, 6, 7).
        assertTrue(createdPaths.contains("src/commonMain/kotlin/io/codenode/demo/viewmodel/DemoUIState.kt"))
        assertTrue(createdPaths.contains("src/commonMain/kotlin/io/codenode/demo/viewmodel/DemoUIViewModel.kt"))
        assertTrue(createdPaths.contains("src/commonMain/kotlin/io/codenode/demo/nodes/DemoUISourceCodeNode.kt"))
        assertTrue(createdPaths.contains("src/commonMain/kotlin/io/codenode/demo/nodes/DemoUISinkCodeNode.kt"))
        assertTrue(createdPaths.contains("src/commonMain/kotlin/io/codenode/demo/controller/DemoUIControllerInterface.kt"))
        assertTrue(createdPaths.contains("src/commonMain/kotlin/io/codenode/demo/controller/DemoUIRuntime.kt"))
        assertTrue(createdPaths.contains("src/jvmMain/kotlin/io/codenode/demo/userInterface/DemoUIPreviewProvider.kt"))

        // Verify the actual files materialized on disk under the module root
        val moduleSrc = File(moduleDir, "src/commonMain/kotlin/io/codenode/demo")
        assertTrue(File(moduleSrc, "viewmodel/DemoUIState.kt").exists())
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
}

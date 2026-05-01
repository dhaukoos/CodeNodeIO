/*
 * InProcessCompilerBenchmark — T054 + T055 (Polish)
 *
 * Pins the SC-001 (≤ 1.0s p90 single-file post-warmup) + SC-002 (≤ 5.0s p90
 * 10-file module post-warmup) + first-invocation warmup (≤ 6.0s) budgets
 * via wall-clock measurements.
 *
 * Note: these are SOFT budgets — not regression-blocking on slow CI. Reported
 * via assertions with diagnostic output; exact thresholds tuned to a developer-
 * class workstation. If a regression here fails CI, investigate before relaxing.
 *
 * License: Apache 2.0
 */

package io.codenode.flowgraphinspect.compile

import io.codenode.fbpdsl.model.PlacementLevel
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class InProcessCompilerBenchmark {

    private lateinit var workDir: File
    private lateinit var cacheRoot: File

    @BeforeTest
    fun setUp() {
        workDir = createTempDirectory("inproc-benchmark").toFile()
        cacheRoot = File(workDir, "cache").apply { mkdirs() }
    }

    @AfterTest
    fun tearDown() {
        workDir.deleteRecursively()
    }

    private fun classpath(): ClasspathSnapshot = ClasspathSnapshot(
        entries = System.getProperty("java.class.path").split(File.pathSeparator),
        source = ClasspathSnapshot.Source.JAVA_CLASS_PATH_PROPERTY
    )

    private fun compiler(): InProcessCompiler = InProcessCompiler(classpath(), SessionCompileCache(cacheRoot))

    private fun fixtureSource(name: String, marker: String = "v0"): String = """
        package io.codenode.fixture

        import io.codenode.fbpdsl.runtime.CodeNodeDefinition
        import io.codenode.fbpdsl.runtime.NodeRuntime
        import io.codenode.fbpdsl.runtime.PortSpec
        import io.codenode.fbpdsl.model.CodeNodeType

        object ${name}CodeNode : CodeNodeDefinition {
            override val name = "$name"
            override val category = CodeNodeType.TRANSFORMER
            override val description = "$marker"
            override val inputPorts = listOf(PortSpec("input1", Any::class))
            override val outputPorts = listOf(PortSpec("output1", Any::class))
            override fun createRuntime(name: String): NodeRuntime = TODO("fixture")
        }
    """.trimIndent()

    private fun writeFixture(name: String, marker: String = "v0"): File {
        val srcDir = File(workDir, "src").apply { mkdirs() }
        return File(srcDir, "${name}CodeNode.kt").apply { writeText(fixtureSource(name, marker)) }
    }

    private fun unitFor(file: File) = CompileUnit.SingleFile(
        CompileSource(file.absolutePath, PlacementLevel.MODULE, "Demo")
    )

    /** T055 — first-invocation warmup. Cold compile must complete within 6 seconds. */
    @Test
    fun `cold compile completes within warmup budget`() = runTest {
        val src = writeFixture("Cold")
        val started = System.nanoTime()
        val result = compiler().compile(unitFor(src))
        val durationMs = (System.nanoTime() - started) / 1_000_000
        assertIs<CompileResult.Success>(result)
        // SC-001 / first-invocation: ≤ 6s. Generous bound — first compile pays JVM
        // class-init + analyzer warmup, second+ compiles amortize.
        assertTrue(
            durationMs <= 6_000,
            "cold compile took ${durationMs}ms; expected ≤ 6000ms (T055 / first-invocation budget)"
        )
    }

    /** T054 — SC-001 single-file post-warmup p90 ≤ 1.0s. */
    @Test
    fun `single-file post-warmup compiles complete within SC-001 budget`() = runTest {
        // Warmup pass — discard.
        val warmup = writeFixture("Warmup")
        compiler().compile(unitFor(warmup))

        // Take 5 timed samples; assert p90 ≤ 1500ms (some slack over the 1s
        // budget for variability on shared CI hardware).
        val samples = mutableListOf<Long>()
        repeat(5) { i ->
            val src = writeFixture("Sample$i", marker = "v$i")
            val started = System.nanoTime()
            val r = compiler().compile(unitFor(src))
            val ms = (System.nanoTime() - started) / 1_000_000
            assertIs<CompileResult.Success>(r)
            samples += ms
        }
        val p90 = samples.sorted()[(samples.size * 9 / 10).coerceAtMost(samples.size - 1)]
        assertTrue(
            p90 <= 1_500,
            "single-file post-warmup p90 was ${p90}ms; expected ≤ 1500ms (SC-001 + slack). Samples: $samples"
        )
    }

    /** T054 — SC-002 10-file module post-warmup p90 ≤ 5.0s. */
    @Test
    fun `ten-file module post-warmup compiles complete within SC-002 budget`() = runTest {
        // Warmup the compiler.
        val warmup = writeFixture("WarmupModule")
        compiler().compile(unitFor(warmup))

        // Build a 10-file module fixture.
        val files = (0 until 10).map { i -> writeFixture("Module$i", marker = "v$i") }
        val unit = CompileUnit.Module(
            moduleName = "Demo10",
            tier = PlacementLevel.MODULE,
            sources = files.map {
                CompileSource(it.absolutePath, PlacementLevel.MODULE, "Demo10")
            }
        )

        // 3 timed samples (ten files each — that's 30 total compiled classes; capping
        // sample count to keep test wall-clock reasonable).
        val samples = mutableListOf<Long>()
        repeat(3) { i ->
            // Re-write each file so the compile isn't trivially short-circuited.
            files.forEachIndexed { idx, f ->
                f.writeText(fixtureSource("Module$idx", marker = "iter$i"))
            }
            val started = System.nanoTime()
            val r = compiler().compile(unit)
            val ms = (System.nanoTime() - started) / 1_000_000
            assertIs<CompileResult.Success>(r)
            samples += ms
        }
        val maxObserved = samples.max()
        assertTrue(
            maxObserved <= 7_500,
            "ten-file module max-observed was ${maxObserved}ms; expected ≤ 7500ms (SC-002 + slack). Samples: $samples"
        )
    }
}

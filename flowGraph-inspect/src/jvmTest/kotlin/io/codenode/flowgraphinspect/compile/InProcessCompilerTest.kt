/*
 * InProcessCompilerTest - TDD Red tests for the in-process Kotlin compiler
 * License: Apache 2.0
 */

package io.codenode.flowgraphinspect.compile

import io.codenode.fbpdsl.model.PlacementLevel
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class InProcessCompilerTest {

    private lateinit var workDir: File
    private lateinit var cacheRoot: File

    @BeforeTest
    fun setUp() {
        workDir = createTempDirectory("inproc-test").toFile()
        cacheRoot = File(workDir, "cache").apply { mkdirs() }
    }

    @AfterTest
    fun tearDown() {
        workDir.deleteRecursively()
    }

    /**
     * Snapshot of the test JVM's runtime classpath. The compiler needs everything the
     * fixture sources reference (fbpDsl, kotlin-stdlib, etc.) — easiest source is the
     * test JVM's own java.class.path.
     */
    private fun testClasspathSnapshot(): ClasspathSnapshot = ClasspathSnapshot(
        entries = System.getProperty("java.class.path").split(File.pathSeparator),
        source = ClasspathSnapshot.Source.JAVA_CLASS_PATH_PROPERTY
    )

    private fun cache() = SessionCompileCache(cacheRoot)

    private fun compiler(): InProcessCompiler = InProcessCompiler(testClasspathSnapshot(), cache())

    private fun writeFixture(name: String, content: String): File {
        val srcDir = File(workDir, "src").apply { mkdirs() }
        return File(srcDir, name).apply { writeText(content) }
    }

    private fun unitFor(file: File) = CompileUnit.SingleFile(
        CompileSource(file.absolutePath, PlacementLevel.MODULE, "Demo")
    )

    private val validFooSource = """
        package io.codenode.fixture

        import io.codenode.fbpdsl.runtime.CodeNodeDefinition
        import io.codenode.fbpdsl.runtime.NodeRuntime
        import io.codenode.fbpdsl.runtime.PortSpec
        import io.codenode.fbpdsl.model.CodeNodeType

        object FooCodeNode : CodeNodeDefinition {
            override val name = "Foo"
            override val category = CodeNodeType.TRANSFORMER
            override val description = "fixture"
            override val inputPorts = listOf(PortSpec("input1", Any::class))
            override val outputPorts = listOf(PortSpec("output1", Any::class))
            override fun createRuntime(name: String): NodeRuntime = TODO("fixture")
        }
    """.trimIndent()

    @Test
    fun `single-valid-source produces Success with the loaded definition name`() = runTest {
        val src = writeFixture("FooCodeNode.kt", validFooSource)
        val result = compiler().compile(unitFor(src))
        assertIs<CompileResult.Success>(result)
        assertTrue(
            result.loadedDefinitionsByName.containsKey("Foo"),
            "loadedDefinitionsByName must include the node's declared name; got: ${result.loadedDefinitionsByName}"
        )
        assertTrue(result.classOutputDir.startsWith(cacheRoot.absolutePath))
    }

    @Test
    fun `single-broken-source produces Failure with file-line diagnostic`() = runTest {
        val brokenSource = """
            package io.codenode.fixture
            object Broken : CodeNodeDefinition  // missing import + missing braces
        """.trimIndent()
        val src = writeFixture("BrokenCodeNode.kt", brokenSource)
        val result = compiler().compile(unitFor(src))
        assertIs<CompileResult.Failure>(result)
        val errors = result.diagnostics.filter { it.severity == CompileDiagnostic.Severity.ERROR }
        assertTrue(errors.isNotEmpty(), "Failure must have at least one ERROR diagnostic")
        val withLocation = errors.firstOrNull { it.filePath != null && it.line > 0 }
        assertNotNull(
            withLocation,
            "at least one ERROR must carry filePath + line>0 for actionable feedback (SC-003)"
        )
    }

    @Test
    fun `module-with-cross-reference compiles two files atomically`() = runTest {
        val helperSource = """
            package io.codenode.fixture
            data class Helper(val n: Int)
        """.trimIndent()
        val userSource = """
            package io.codenode.fixture

            import io.codenode.fbpdsl.runtime.CodeNodeDefinition
            import io.codenode.fbpdsl.runtime.NodeRuntime
            import io.codenode.fbpdsl.runtime.PortSpec
            import io.codenode.fbpdsl.model.CodeNodeType

            object UserCodeNode : CodeNodeDefinition {
                override val name = "User"
                override val category = CodeNodeType.TRANSFORMER
                override val description = "uses Helper"
                override val inputPorts = listOf(PortSpec("input1", Any::class))
                override val outputPorts = listOf(PortSpec("output1", Any::class))
                private val h = Helper(42)
                override fun createRuntime(name: String): NodeRuntime = TODO("fixture")
            }
        """.trimIndent()
        val helper = writeFixture("Helper.kt", helperSource)
        val user = writeFixture("UserCodeNode.kt", userSource)
        val unit = CompileUnit.Module(
            moduleName = "Demo",
            tier = PlacementLevel.MODULE,
            sources = listOf(
                CompileSource(helper.absolutePath, PlacementLevel.MODULE, "Demo"),
                CompileSource(user.absolutePath, PlacementLevel.MODULE, "Demo")
            )
        )
        val result = compiler().compile(unit)
        assertIs<CompileResult.Success>(result)
        assertTrue(result.loadedDefinitionsByName.containsKey("User"))
    }

    @Test
    fun `module-where-one-file-fails returns Failure atomically`() = runTest {
        val good = writeFixture("Good.kt", validFooSource)
        val bad = writeFixture("Bad.kt", "package io.codenode.fixture\nobject Bad : CodeNodeDefinition  // broken")
        val unit = CompileUnit.Module(
            moduleName = "Demo",
            tier = PlacementLevel.MODULE,
            sources = listOf(
                CompileSource(good.absolutePath, PlacementLevel.MODULE, "Demo"),
                CompileSource(bad.absolutePath, PlacementLevel.MODULE, "Demo")
            )
        )
        val result = compiler().compile(unit)
        assertIs<CompileResult.Failure>(result)
        // The diagnostic naming the broken file must be present.
        val pointsToBad = result.diagnostics.any {
            it.severity == CompileDiagnostic.Severity.ERROR &&
                it.filePath?.endsWith("Bad.kt") == true
        }
        assertTrue(pointsToBad, "diagnostic must name Bad.kt; got: ${result.diagnostics}")
    }

    @Test
    fun `module-empty-classpath returns Failure for missing core types`() = runTest {
        val src = writeFixture("FooCodeNode.kt", validFooSource)
        val emptySnap = ClasspathSnapshot(
            entries = emptyList(),
            source = ClasspathSnapshot.Source.JAVA_CLASS_PATH_PROPERTY
        )
        val compiler = InProcessCompiler(emptySnap, cache())
        val result = compiler.compile(unitFor(src))
        assertIs<CompileResult.Failure>(result)
        // Without stdlib + fbpDsl on the classpath the compile must produce ERROR diagnostics.
        assertTrue(result.diagnostics.any { it.severity == CompileDiagnostic.Severity.ERROR })
    }

    @Test
    fun `compile-twice-same-unit-different-content yields distinct output dirs`() = runTest {
        val compiler = compiler()
        val src = writeFixture("FooCodeNode.kt", validFooSource)
        val first = compiler.compile(unitFor(src))
        assertIs<CompileResult.Success>(first)

        // Mutate source — compile again.
        src.writeText(validFooSource.replace("\"fixture\"", "\"fixture-v2\""))
        val second = compiler.compile(unitFor(src))
        assertIs<CompileResult.Success>(second)

        assertNotEquals(
            first.classOutputDir, second.classOutputDir,
            "consecutive compiles of the same unit must produce distinct output dirs (Decision 6)"
        )
    }

    @Test
    fun `large-source-warmup-cost subsequent invocations are faster than first`() = runTest {
        // Establishes the warmup-cost expectation. A pristine JVM's first K2JVMCompiler.exec
        // is slow (class-init + analyzer warmup); subsequent calls are at least 5x faster.
        val src = writeFixture("FooCodeNode.kt", validFooSource)
        val compiler = compiler()

        val firstStart = System.nanoTime()
        val firstResult = compiler.compile(unitFor(src))
        val firstDurationNs = System.nanoTime() - firstStart
        assertIs<CompileResult.Success>(firstResult)

        // Warm follow-up.
        src.writeText(validFooSource.replace("\"fixture\"", "\"fixture-warm\""))
        val secondStart = System.nanoTime()
        val secondResult = compiler.compile(unitFor(src))
        val secondDurationNs = System.nanoTime() - secondStart
        assertIs<CompileResult.Success>(secondResult)

        assertTrue(
            secondDurationNs * 5 <= firstDurationNs,
            "warm compile (${secondDurationNs / 1_000_000} ms) should be at least 5x faster than " +
                "first compile (${firstDurationNs / 1_000_000} ms) — warmup cost amortizes"
        )
    }
}

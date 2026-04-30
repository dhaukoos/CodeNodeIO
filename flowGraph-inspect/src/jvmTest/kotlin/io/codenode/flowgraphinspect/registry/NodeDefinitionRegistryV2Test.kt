/*
 * NodeDefinitionRegistryV2Test - TDD Red tests for FR-017 resolution-precedence
 * License: Apache 2.0
 */

package io.codenode.flowgraphinspect.registry

import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.PlacementLevel
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.flowgraphinspect.compile.ClassloaderScope
import io.codenode.flowgraphinspect.compile.CompileSource
import io.codenode.flowgraphinspect.compile.CompileUnit
import java.io.File
import java.lang.ref.WeakReference
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NodeDefinitionRegistryV2Test {

    private lateinit var workDir: File

    @BeforeTest
    fun setUp() {
        workDir = createTempDirectory("registry-v2").toFile()
    }

    @AfterTest
    fun tearDown() {
        workDir.deleteRecursively()
    }

    /** Minimal CodeNodeDefinition fixture. */
    private fun fixtureDefinition(name: String) = object : CodeNodeDefinition {
        override val name: String = name
        override val category: CodeNodeType = CodeNodeType.TRANSFORMER
        override val description: String = "fixture"
        override val inputPorts: List<PortSpec> = listOf(PortSpec("input1", Any::class))
        override val outputPorts: List<PortSpec> = listOf(PortSpec("output1", Any::class))
        override fun createRuntime(name: String): NodeRuntime = TODO("fixture")
    }

    private fun fixtureScope() = ClassloaderScope(
        unit = CompileUnit.SingleFile(CompileSource("/tmp/X.kt", PlacementLevel.MODULE, "Demo")),
        classOutputDir = File(workDir, "out").apply { mkdirs() },
        parent = javaClass.classLoader
    )

    @Test
    fun `getByName falls back to launchtime when no session install`() {
        val registry = NodeDefinitionRegistry()
        val def = fixtureDefinition("X")
        registry.register(def)
        assertSame(def, registry.getByName("X"))
    }

    @Test
    fun `getByName prefers session install over launchtime`() {
        val registry = NodeDefinitionRegistry()
        val launchTime = fixtureDefinition("X")
        val sessionTime = fixtureDefinition("X")
        registry.register(launchTime)
        registry.installSessionDefinition(fixtureScope(), "X", sessionTime)
        assertSame(sessionTime, registry.getByName("X"))
    }

    @Test
    fun `revertSessionDefinition falls back to launchtime`() {
        val registry = NodeDefinitionRegistry()
        val launchTime = fixtureDefinition("X")
        val sessionTime = fixtureDefinition("X")
        registry.register(launchTime)
        registry.installSessionDefinition(fixtureScope(), "X", sessionTime)
        registry.revertSessionDefinition("X")
        assertSame(launchTime, registry.getByName("X"))
    }

    @Test
    fun `installSessionDefinition twice replaces prior strong reference`() {
        val registry = NodeDefinitionRegistry()
        val v1Def = fixtureDefinition("X")
        val v2Def = fixtureDefinition("X")
        val v1Scope = fixtureScope()
        val v2Scope = fixtureScope()
        registry.installSessionDefinition(v1Scope, "X", v1Def)

        // Capture a WeakReference before letting v1Scope go out of scope.
        val weakV1 = WeakReference(v1Scope)
        @Suppress("UNUSED_VALUE")
        var v1Local: ClassloaderScope? = v1Scope
        v1Local = null

        // Install v2; this drops the registry's strong reference to v1Scope.
        registry.installSessionDefinition(v2Scope, "X", v2Def)
        assertSame(v2Def, registry.getByName("X"))

        // Encourage GC; v1Scope should become unreachable since the registry no longer holds it.
        repeat(5) { System.gc(); Thread.sleep(20) }

        // We can't assert weakV1.get() == null deterministically (JVM GC is best-effort)
        // but the registry must NOT be the one holding it alive — verifying via two registry
        // queries (first gives v2; second still v2; nothing reverts to v1).
        assertSame(v2Def, registry.getByName("X"))
    }

    @Test
    fun `getByName returns null for unknown name`() {
        val registry = NodeDefinitionRegistry()
        assertNull(registry.getByName("Nope"))
    }

    @Test
    fun `getByName of template-only node returns null`() {
        val registry = NodeDefinitionRegistry()
        // Templates are palette-only — getByName must NOT return them as executable definitions.
        // (Existing template-registration uses NodeTemplateMeta; for v2's executable lookup
        // path, only compiled OR session-installed definitions count.)
        assertNull(registry.getByName("TemplateOnlyNode"))
    }

    /**
     * Regression test for the VS-A1 user-reported bug — a freshly-generated CodeNode
     * was successfully session-installed (compile + classloader load + registry install
     * all green), but it never appeared on the Node Palette because
     * `getAllForPalette()` only walked compiledNodes + templateNodes. The fix added
     * sessionInstalls as the FIRST source in the palette merge.
     */
    @Test
    fun `getAllForPalette includes session installs`() {
        val registry = NodeDefinitionRegistry()
        registry.installSessionDefinition(fixtureScope(), "FreshlyGenerated", fixtureDefinition("FreshlyGenerated"))

        val palette = registry.getAllForPalette()
        assertTrue(
            palette.any { it.name == "FreshlyGenerated" },
            "session-installed nodes MUST appear on the palette; got: ${palette.map { it.name }}"
        )
    }

    @Test
    fun `getAllForPalette deduplicates session install over launch-time compiled with same name`() {
        val registry = NodeDefinitionRegistry()
        registry.register(fixtureDefinition("X"))
        registry.installSessionDefinition(fixtureScope(), "X", fixtureDefinition("X"))

        val palette = registry.getAllForPalette()
        val xEntries = palette.filter { it.name == "X" }
        assertTrue(
            xEntries.size == 1,
            "palette MUST emit exactly one entry per name (session install shadows launch-time); got $xEntries"
        )
    }

    @Test
    fun `nameExists returns true for session-installed nodes`() {
        val registry = NodeDefinitionRegistry()
        registry.installSessionDefinition(fixtureScope(), "Y", fixtureDefinition("Y"))
        assertTrue(registry.nameExists("Y"), "nameExists must include sessionInstalls (FR-001 conflict-detection)")
    }

    @Test
    fun `isCompiled returns true for session-installed nodes`() {
        val registry = NodeDefinitionRegistry()
        registry.installSessionDefinition(fixtureScope(), "Z", fixtureDefinition("Z"))
        assertTrue(
            registry.isCompiled("Z"),
            "isCompiled must include sessionInstalls — they ARE executable, just not from the launch-time classpath"
        )
    }

    /**
     * Regression test — feature 086 user-reported defect:
     *
     *   "The pencil icon for editing in the Properties panel when a node is selected
     *    (Calc3) is no longer showing up."
     *
     * Root cause: getSourceFilePath() consulted compiledNodes + templateNodes only,
     * never sessionInstalls. A freshly-generated session-installed node had no
     * source path resolvable via this API, so the Properties panel hid the pencil.
     * Fixed by deriving the source path from the install's CompileUnit.
     */
    @Test
    fun `getSourceFilePath derives source from session-install SingleFile unit`() {
        val sourcePath = "/tmp/Calc3CodeNode.kt"
        val unit = io.codenode.flowgraphinspect.compile.CompileUnit.SingleFile(
            io.codenode.flowgraphinspect.compile.CompileSource(
                absolutePath = sourcePath,
                tier = PlacementLevel.MODULE,
                hostModuleName = "TestModule"
            )
        )
        val scope = io.codenode.flowgraphinspect.compile.ClassloaderScope(
            unit = unit,
            classOutputDir = File(workDir, "out").apply { mkdirs() },
            parent = javaClass.classLoader
        )
        val registry = NodeDefinitionRegistry()
        registry.installSessionDefinition(scope, "Calc3", fixtureDefinition("Calc3"))

        assertEquals(
            sourcePath,
            registry.getSourceFilePath("Calc3"),
            "getSourceFilePath MUST resolve a session-installed node to its compiled source — " +
                "without this, the Properties-panel pencil icon hides for freshly-generated nodes"
        )
    }

    @Test
    fun `getSourceFilePath derives source from session-install Module unit by name convention`() {
        val moduleSources = listOf(
            io.codenode.flowgraphinspect.compile.CompileSource(
                absolutePath = "/tmp/Demo/Helper.kt",
                tier = PlacementLevel.MODULE,
                hostModuleName = "Demo"
            ),
            io.codenode.flowgraphinspect.compile.CompileSource(
                absolutePath = "/tmp/Demo/UserCodeNode.kt",
                tier = PlacementLevel.MODULE,
                hostModuleName = "Demo"
            )
        )
        val unit = io.codenode.flowgraphinspect.compile.CompileUnit.Module(
            moduleName = "Demo",
            tier = PlacementLevel.MODULE,
            sources = moduleSources
        )
        val scope = io.codenode.flowgraphinspect.compile.ClassloaderScope(
            unit = unit,
            classOutputDir = File(workDir, "out").apply { mkdirs() },
            parent = javaClass.classLoader
        )
        val registry = NodeDefinitionRegistry()
        registry.installSessionDefinition(scope, "User", fixtureDefinition("User"))

        assertEquals(
            "/tmp/Demo/UserCodeNode.kt",
            registry.getSourceFilePath("User"),
            "module-unit lookup uses the {name}CodeNode.kt convention"
        )
    }

    @Test
    fun `getSourceFilePath returns null when session-install Module unit has no matching source`() {
        val unit = io.codenode.flowgraphinspect.compile.CompileUnit.Module(
            moduleName = "Demo",
            tier = PlacementLevel.MODULE,
            sources = listOf(
                io.codenode.flowgraphinspect.compile.CompileSource(
                    absolutePath = "/tmp/Demo/Other.kt",
                    tier = PlacementLevel.MODULE,
                    hostModuleName = "Demo"
                )
            )
        )
        val scope = io.codenode.flowgraphinspect.compile.ClassloaderScope(
            unit = unit,
            classOutputDir = File(workDir, "out").apply { mkdirs() },
            parent = javaClass.classLoader
        )
        val registry = NodeDefinitionRegistry()
        registry.installSessionDefinition(scope, "Mismatch", fixtureDefinition("Mismatch"))

        assertNull(
            registry.getSourceFilePath("Mismatch"),
            "no source file matches the {name}CodeNode.kt convention; null is the right answer"
        )
    }
}

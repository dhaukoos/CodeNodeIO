/*
 * CrossModuleIsolationTest — TDD Red regression test for FR-011:
 * recompiling module X MUST NOT silently recompile or reload nodes
 * from any other module Y.
 *
 * Added by /speckit.analyze remediation H2.
 *
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
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertSame

class CrossModuleIsolationTest {

    private lateinit var workDir: File

    @BeforeTest
    fun setUp() {
        workDir = createTempDirectory("xmod-isolation").toFile()
    }

    @AfterTest
    fun tearDown() {
        workDir.deleteRecursively()
    }

    private fun fixtureDefinition(name: String) = object : CodeNodeDefinition {
        override val name: String = name
        override val category: CodeNodeType = CodeNodeType.TRANSFORMER
        override val description: String = "fixture-$name"
        override val inputPorts: List<PortSpec> = listOf(PortSpec("input1", Any::class))
        override val outputPorts: List<PortSpec> = listOf(PortSpec("output1", Any::class))
        override fun createRuntime(name: String): NodeRuntime = TODO("fixture")
    }

    private fun fixtureScope(moduleName: String) = ClassloaderScope(
        unit = CompileUnit.Module(
            moduleName = moduleName,
            tier = PlacementLevel.MODULE,
            sources = listOf(CompileSource("/tmp/$moduleName/X.kt", PlacementLevel.MODULE, moduleName))
        ),
        classOutputDir = File(workDir, "$moduleName-out").apply { mkdirs() },
        parent = javaClass.classLoader
    )

    @Test
    fun `recompiling module A leaves module B's session install untouched`() {
        val registry = NodeDefinitionRegistry()

        // Two distinct modules; each contributes one node with a different name.
        val nodeA = fixtureDefinition("NodeA")
        val nodeB = fixtureDefinition("NodeB")
        val scopeA1 = fixtureScope("ModuleA")
        val scopeB = fixtureScope("ModuleB")
        registry.installSessionDefinition(scopeA1, "NodeA", nodeA)
        registry.installSessionDefinition(scopeB, "NodeB", nodeB)

        // "Recompile module A" — replace ModuleA's session install with a new ClassloaderScope.
        val nodeA2 = fixtureDefinition("NodeA") // same name, new instance
        val scopeA2 = fixtureScope("ModuleA")
        registry.installSessionDefinition(scopeA2, "NodeA", nodeA2)

        // ModuleA's lookup now returns the v2 instance.
        assertSame(nodeA2, registry.getByName("NodeA"))

        // ModuleB's lookup MUST still return the original instance — FR-011.
        assertSame(
            nodeB, registry.getByName("NodeB"),
            "FR-011 violation: recompiling ModuleA changed ModuleB's session install. " +
                "Cross-module isolation MUST hold."
        )
    }

    @Test
    fun `revertSessionDefinition for one module does not affect another module`() {
        val registry = NodeDefinitionRegistry()

        val nodeA = fixtureDefinition("NodeA")
        val nodeB = fixtureDefinition("NodeB")
        registry.installSessionDefinition(fixtureScope("ModuleA"), "NodeA", nodeA)
        registry.installSessionDefinition(fixtureScope("ModuleB"), "NodeB", nodeB)

        registry.revertSessionDefinition("NodeA")

        // ModuleB still resolves.
        assertSame(nodeB, registry.getByName("NodeB"))
    }
}

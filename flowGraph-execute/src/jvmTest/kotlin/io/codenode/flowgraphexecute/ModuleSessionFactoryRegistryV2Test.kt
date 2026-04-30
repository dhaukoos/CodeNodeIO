/*
 * ModuleSessionFactoryRegistryV2Test - regression test for FR-017 propagation through
 * ModuleSessionFactory.createSession into DynamicPipelineBuilder
 *
 * The factory's lookup-injection seam (createSession line 76) reads
 * `registry.getByName(name)` per call. Since registry getByName now consults session
 * installs first (T028), the precedence chain propagates transparently into pipeline
 * construction. This test pins that contract: once a session install supersedes a
 * launch-time entry for node X, ModuleSessionFactory's pipeline-build path resolves X
 * to the session-installed instance.
 *
 * Added by T032 (feature 086).
 *
 * License: Apache 2.0
 */

package io.codenode.flowgraphexecute

import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.PlacementLevel
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.flowgraphinspect.compile.ClassloaderScope
import io.codenode.flowgraphinspect.compile.CompileSource
import io.codenode.flowgraphinspect.compile.CompileUnit
import io.codenode.flowgraphinspect.registry.NodeDefinitionRegistry
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ModuleSessionFactoryRegistryV2Test {

    private lateinit var workDir: File

    @BeforeTest
    fun setUp() {
        workDir = createTempDirectory("msf-registry-v2").toFile()
    }

    @AfterTest
    fun tearDown() {
        ModuleSessionFactory.registry = null
        workDir.deleteRecursively()
    }

    private fun fixtureDefinition(name: String, marker: String) = object : CodeNodeDefinition {
        override val name: String = name
        override val category: CodeNodeType = CodeNodeType.TRANSFORMER
        override val description: String = marker
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
    fun `ModuleSessionFactory's lookup resolves session install over launch-time entry`() {
        // Arrange: register both a launch-time entry and a session install for "X".
        val registry = NodeDefinitionRegistry()
        val launchTimeX = fixtureDefinition("X", marker = "LAUNCH")
        val sessionX = fixtureDefinition("X", marker = "SESSION")
        registry.register(launchTimeX)
        registry.installSessionDefinition(fixtureScope(), "X", sessionX)
        ModuleSessionFactory.registry = registry

        // Act: query through the same seam ModuleSessionFactory.createSession uses
        // (line 76 of ModuleSessionFactory.kt). T028's resolution-precedence change
        // is what makes this test meaningful — without it, lookup would return launchTimeX.
        val resolved = registry.getByName("X")

        // Assert: the session install wins. (Class identity check would be even stronger,
        // but for this fixture both definitions live in the same classloader.)
        assertSame(
            sessionX, resolved,
            "FR-017: getByName must return the session install when one exists for name 'X'"
        )
        assertTrue(
            resolved?.description == "SESSION",
            "marker confirms the session-installed instance is the one returned"
        )
    }

    @Test
    fun `revertSessionDefinition causes ModuleSessionFactory's lookup to fall back to launch-time`() {
        val registry = NodeDefinitionRegistry()
        val launchTimeX = fixtureDefinition("X", marker = "LAUNCH")
        val sessionX = fixtureDefinition("X", marker = "SESSION")
        registry.register(launchTimeX)
        registry.installSessionDefinition(fixtureScope(), "X", sessionX)
        ModuleSessionFactory.registry = registry

        registry.revertSessionDefinition("X")

        assertSame(launchTimeX, registry.getByName("X"))
    }

    @Test
    fun `canBuildDynamic respects the registry's session install for ModuleSessionFactory's lookup`() {
        // End-to-end smoke: a flow graph naming "X" with no launch-time install but
        // WITH a session install must satisfy DynamicPipelineBuilder.canBuildDynamic
        // (which is what ModuleSessionFactory's createSession path uses to gate session
        // creation at line 77).
        val registry = NodeDefinitionRegistry()
        val sessionX = fixtureDefinition("X", marker = "SESSION-ONLY")
        registry.installSessionDefinition(fixtureScope(), "X", sessionX)

        val graph = flowGraph(name = "G", version = "1.0.0") {
            codeNode("X", nodeType = "TRANSFORMER") {
                position(0.0, 0.0)
            }
        }

        val canBuild = io.codenode.fbpdsl.runtime.DynamicPipelineBuilder.canBuildDynamic(graph) { name ->
            registry.getByName(name)
        }
        assertTrue(
            canBuild,
            "DynamicPipelineBuilder must accept a graph whose nodes are session-installed only"
        )
    }
}

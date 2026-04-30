/*
 * ClassloaderScopeTest - TDD Red tests for ClassloaderScope.loadDefinition + close
 * License: Apache 2.0
 */

package io.codenode.flowgraphinspect.compile

import io.codenode.fbpdsl.model.PlacementLevel
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNull

class ClassloaderScopeTest {

    private lateinit var emptyOutputDir: File

    @BeforeTest
    fun setUp() {
        emptyOutputDir = createTempDirectory("scope-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        emptyOutputDir.deleteRecursively()
    }

    private fun fixtureUnit() = CompileUnit.SingleFile(
        CompileSource("/tmp/Foo.kt", PlacementLevel.MODULE, "Demo")
    )

    @Test
    fun `loadDefinition returns null for missing FQCN`() {
        val scope = ClassloaderScope(
            unit = fixtureUnit(),
            classOutputDir = emptyOutputDir,
            parent = javaClass.classLoader
        )
        assertNull(scope.loadDefinition("io.fictional.Missing"))
    }

    @Test
    fun `loadDefinition returns null for class that exists but is not a Kotlin object`() {
        // String exists on the parent classpath but is not a Kotlin object exposing INSTANCE.
        val scope = ClassloaderScope(
            unit = fixtureUnit(),
            classOutputDir = emptyOutputDir,
            parent = javaClass.classLoader
        )
        assertNull(scope.loadDefinition("kotlin.String"))
    }

    @Test
    fun `close is idempotent`() {
        val scope = ClassloaderScope(
            unit = fixtureUnit(),
            classOutputDir = emptyOutputDir,
            parent = javaClass.classLoader
        )
        scope.close()
        scope.close() // must not throw
    }

    // Note: positive load tests (where loadDefinition returns a real CodeNodeDefinition)
    // are exercised end-to-end via T013's InProcessCompilerTest, which produces a fixture
    // .class output directory and constructs a scope around it.
}

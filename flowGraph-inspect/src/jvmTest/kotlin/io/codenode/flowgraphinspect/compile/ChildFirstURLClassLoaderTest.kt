/*
 * ChildFirstURLClassLoaderTest - TDD Red tests for ChildFirstURLClassLoader delegation
 * License: Apache 2.0
 */

package io.codenode.flowgraphinspect.compile

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ChildFirstURLClassLoaderTest {

    private lateinit var emptyDir: File

    @BeforeTest
    fun setUp() {
        emptyDir = createTempDirectory("childfirst-cl-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        emptyDir.deleteRecursively()
    }

    @Test
    fun `non-owned class is loaded via parent delegation`() {
        // PlacementLevel is on the parent classpath; its package is NOT in ownedPackages.
        val loader = ChildFirstURLClassLoader(
            urls = arrayOf(emptyDir.toURI().toURL()),
            parent = javaClass.classLoader,
            ownedPackages = setOf("io.fictional.owned")
        )
        val clazz = loader.loadClass("io.codenode.fbpdsl.model.PlacementLevel")
        // Same Class instance the parent would resolve — non-owned packages don't isolate identity.
        assertSame(io.codenode.fbpdsl.model.PlacementLevel::class.java, clazz)
    }

    @Test
    fun `owned-package class missing locally falls back to parent`() {
        // Owned package matches but the class is only in parent → child lookup fails → delegate up.
        val loader = ChildFirstURLClassLoader(
            urls = arrayOf(emptyDir.toURI().toURL()),
            parent = javaClass.classLoader,
            ownedPackages = setOf("io.codenode.fbpdsl.model")
        )
        val clazz = loader.loadClass("io.codenode.fbpdsl.model.PlacementLevel")
        // Parent delegation succeeded; class identity matches parent (no local override available).
        assertSame(io.codenode.fbpdsl.model.PlacementLevel::class.java, clazz)
    }

    @Test
    fun `unknown class throws ClassNotFoundException`() {
        val loader = ChildFirstURLClassLoader(
            urls = arrayOf(emptyDir.toURI().toURL()),
            parent = javaClass.classLoader,
            ownedPackages = setOf("io.fictional.owned")
        )
        assertFailsWith<ClassNotFoundException> {
            loader.loadClass("io.fictional.Missing\$NotARealClass")
        }
    }

    @Test
    fun `ownedPackages prefix match treats sub-packages as owned`() {
        // T025 contract: ownedPackages contains "io.fictional", classes under "io.fictional.sub" qualify.
        // Negative-only test (no local class): owned-prefix match still triggers child-first lookup
        // (which fails locally) before delegating to parent. Parent doesn't have it either → CNFE.
        val loader = ChildFirstURLClassLoader(
            urls = arrayOf(emptyDir.toURI().toURL()),
            parent = javaClass.classLoader,
            ownedPackages = setOf("io.fictional")
        )
        assertFailsWith<ClassNotFoundException> {
            loader.loadClass("io.fictional.sub.Missing")
        }
    }

    @Test
    fun `loadClass with default resolve flag works through public API`() {
        val loader = ChildFirstURLClassLoader(
            urls = arrayOf(emptyDir.toURI().toURL()),
            parent = javaClass.classLoader,
            ownedPackages = emptySet()
        )
        val clazz: Class<*> = loader.loadClass("kotlin.text.StringsKt")
        assertTrue(clazz.name == "kotlin.text.StringsKt")
    }
}

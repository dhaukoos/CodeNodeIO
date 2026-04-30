/*
 * ClasspathSnapshotTest - TDD Red tests for ClasspathSnapshot.load
 * License: Apache 2.0
 */

package io.codenode.flowgraphinspect.compile

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClasspathSnapshotTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun setUp() {
        tempDir = createTempDirectory("clpsnap-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    /** Helper: write a fixture file path that actually exists on disk. */
    private fun fixtureExisting(name: String): String {
        val f = File(tempDir, name).apply { writeText("placeholder") }
        return f.absolutePath
    }

    @Test
    fun `load reads classpath entries from the file and unions with java_class_path`() {
        val a = fixtureExisting("a.jar")
        val b = fixtureExisting("b.jar")
        val cpFile = File(tempDir, "grapheditor-runtime-classpath.txt").apply {
            writeText("$a\n$b")
        }
        val snap = ClasspathSnapshot.load(
            classpathFile = cpFile,
            fallbackProperty = "" // empty property; only file entries in this test
        )
        assertEquals(setOf(a, b), snap.entries.toSet())
        assertEquals(ClasspathSnapshot.Source.CLASSPATH_FILE, snap.source)
    }

    @Test
    fun `load skips blank lines in the file`() {
        val a = fixtureExisting("a.jar")
        val b = fixtureExisting("b.jar")
        val c = fixtureExisting("c.jar")
        val cpFile = File(tempDir, "cp.txt").apply {
            writeText("$a\n\n$b\n   \n$c")
        }
        val snap = ClasspathSnapshot.load(classpathFile = cpFile, fallbackProperty = "")
        assertEquals(setOf(a, b, c), snap.entries.toSet())
    }

    @Test
    fun `load falls back to java_class_path when file is null`() {
        val x = fixtureExisting("x.jar")
        val y = fixtureExisting("y.jar")
        val snap = ClasspathSnapshot.load(
            classpathFile = null,
            fallbackProperty = "$x${File.pathSeparator}$y"
        )
        assertEquals(setOf(x, y), snap.entries.toSet())
        assertEquals(ClasspathSnapshot.Source.JAVA_CLASS_PATH_PROPERTY, snap.source)
    }

    @Test
    fun `load falls back to java_class_path when file does not exist`() {
        val cpFile = File(tempDir, "missing.txt") // not created
        val only = fixtureExisting("only.jar")
        val snap = ClasspathSnapshot.load(
            classpathFile = cpFile,
            fallbackProperty = only
        )
        assertEquals(listOf(only), snap.entries)
        assertEquals(ClasspathSnapshot.Source.JAVA_CLASS_PATH_PROPERTY, snap.source)
    }

    @Test
    fun `load unions file entries with java_class_path entries`() {
        // The real production path: file has project-module JARs; java.class.path has tool
        // JARs. Both must reach the compiler. (Fix for the missing-fbpDsl bug surfaced when
        // running the GraphEditor against DemoProject.)
        val projectJar = fixtureExisting("project-module.jar")
        val toolJar = fixtureExisting("fbpdsl.jar")
        val cpFile = File(tempDir, "cp.txt").apply { writeText(projectJar) }
        val snap = ClasspathSnapshot.load(
            classpathFile = cpFile,
            fallbackProperty = toolJar
        )
        assertTrue(snap.entries.contains(projectJar), "file entries must be present: ${snap.entries}")
        assertTrue(snap.entries.contains(toolJar), "java.class.path entries must be present: ${snap.entries}")
    }

    @Test
    fun `load filters non-existent entries from both file and java_class_path`() {
        val real = fixtureExisting("real.jar")
        val bogusFile = "/tmp/does-not-exist-${System.nanoTime()}.jar"
        val bogusProp = "/tmp/also-missing-${System.nanoTime()}.jar"
        val cpFile = File(tempDir, "cp.txt").apply { writeText("$real\n$bogusFile") }
        val snap = ClasspathSnapshot.load(
            classpathFile = cpFile,
            fallbackProperty = bogusProp
        )
        assertEquals(listOf(real), snap.entries)
    }

    @Test
    fun `load deduplicates entries appearing in both file and java_class_path`() {
        val both = fixtureExisting("shared.jar")
        val cpFile = File(tempDir, "cp.txt").apply { writeText(both) }
        val snap = ClasspathSnapshot.load(
            classpathFile = cpFile,
            fallbackProperty = both
        )
        assertEquals(1, snap.entries.size, "duplicate entries must be deduplicated; got ${snap.entries}")
        assertEquals(both, snap.entries.first())
    }

    @Test
    fun `asPathString joins entries with provided separator`() {
        val snap = ClasspathSnapshot(
            entries = listOf("/a.jar", "/b.jar"),
            source = ClasspathSnapshot.Source.CLASSPATH_FILE
        )
        assertEquals("/a.jar:/b.jar", snap.asPathString(separator = ":"))
        assertEquals("/a.jar;/b.jar", snap.asPathString(separator = ";"))
    }

    @Test
    fun `asPathString defaults to platform path separator`() {
        val snap = ClasspathSnapshot(
            entries = listOf("/a.jar", "/b.jar"),
            source = ClasspathSnapshot.Source.CLASSPATH_FILE
        )
        val joined = snap.asPathString()
        assertTrue(joined.contains(File.pathSeparator), "expected platform separator in: $joined")
    }
}

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

    @Test
    fun `load reads classpath entries from grapheditor-runtime-classpath dot txt`() {
        val cpFile = File(tempDir, "grapheditor-runtime-classpath.txt").apply {
            writeText(
                """
                /tmp/jar1.jar
                /tmp/jar2.jar
                /tmp/classes/main
                """.trimIndent()
            )
        }
        val snap = ClasspathSnapshot.load(classpathFile = cpFile)
        assertEquals(
            listOf("/tmp/jar1.jar", "/tmp/jar2.jar", "/tmp/classes/main"),
            snap.entries
        )
        assertEquals(ClasspathSnapshot.Source.CLASSPATH_FILE, snap.source)
    }

    @Test
    fun `load preserves entry order from the file`() {
        val cpFile = File(tempDir, "cp.txt").apply {
            writeText("/z.jar\n/a.jar\n/m.jar")
        }
        val snap = ClasspathSnapshot.load(classpathFile = cpFile)
        assertEquals(listOf("/z.jar", "/a.jar", "/m.jar"), snap.entries)
    }

    @Test
    fun `load skips blank lines in the file`() {
        val cpFile = File(tempDir, "cp.txt").apply {
            writeText("/a.jar\n\n/b.jar\n   \n/c.jar")
        }
        val snap = ClasspathSnapshot.load(classpathFile = cpFile)
        assertEquals(listOf("/a.jar", "/b.jar", "/c.jar"), snap.entries)
    }

    @Test
    fun `load falls back to java_class_path when file is null`() {
        val snap = ClasspathSnapshot.load(
            classpathFile = null,
            fallbackProperty = "/x.jar${File.pathSeparator}/y.jar${File.pathSeparator}/z.jar"
        )
        assertEquals(listOf("/x.jar", "/y.jar", "/z.jar"), snap.entries)
        assertEquals(ClasspathSnapshot.Source.JAVA_CLASS_PATH_PROPERTY, snap.source)
    }

    @Test
    fun `load falls back to java_class_path when file does not exist`() {
        val cpFile = File(tempDir, "missing.txt") // not created
        val snap = ClasspathSnapshot.load(
            classpathFile = cpFile,
            fallbackProperty = "/only.jar"
        )
        assertEquals(listOf("/only.jar"), snap.entries)
        assertEquals(ClasspathSnapshot.Source.JAVA_CLASS_PATH_PROPERTY, snap.source)
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

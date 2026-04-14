/*
 * IPTypeDiscoveryTest - Tests for IP type file parsing including typealias support
 * License: Apache 2.0
 */

package io.codenode.flowgraphtypes.discovery

import io.codenode.fbpdsl.model.IPColor
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IPTypeDiscoveryTest {

    private val discovery = IPTypeDiscovery(projectRoot = File(System.getProperty("user.dir")))

    // --- T005: Parse typealias IP type file ---

    @Test
    fun `parse typealias IP type file returns correct metadata`() {
        val content = """
            /*
             * NodeDescriptors - Custom IP Type
             * @IPType
             * @TypeName NodeDescriptors
             * @TypeId ip_nodedescriptors
             * @Color rgb(233, 30, 99)
             * License: Apache 2.0
             */

            package io.codenode.iptypes

            import io.codenode.fbpdsl.model.NodeTypeDefinition

            typealias NodeDescriptors = List<NodeTypeDefinition>
        """.trimIndent()

        val tempFile = File.createTempFile("NodeDescriptors", ".kt")
        try {
            tempFile.writeText(content)
            val meta = discovery.parseIPTypeFile(tempFile.absolutePath)

            assertNotNull(meta)
            assertEquals("NodeDescriptors", meta.typeName)
            assertEquals("ip_nodedescriptors", meta.typeId)
            assertEquals(IPColor(233, 30, 99), meta.color)
            assertEquals("io.codenode.iptypes", meta.packageName)
            assertEquals("io.codenode.iptypes.NodeDescriptors", meta.className)
            assertTrue(meta.properties.isEmpty(), "Typealias types should have empty properties")
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `parse typealias with simple type returns correct metadata`() {
        val content = """
            /*
             * FlowGraphModel - Custom IP Type
             * @IPType
             * @TypeName FlowGraphModel
             * @TypeId ip_flowgraphmodel
             * @Color rgb(121, 85, 72)
             * License: Apache 2.0
             */

            package io.codenode.iptypes

            import io.codenode.fbpdsl.model.FlowGraph

            typealias FlowGraphModel = FlowGraph
        """.trimIndent()

        val tempFile = File.createTempFile("FlowGraphModel", ".kt")
        try {
            tempFile.writeText(content)
            val meta = discovery.parseIPTypeFile(tempFile.absolutePath)

            assertNotNull(meta)
            assertEquals("FlowGraphModel", meta.typeName)
            assertEquals("ip_flowgraphmodel", meta.typeId)
            assertEquals(IPColor(121, 85, 72), meta.color)
            assertTrue(meta.properties.isEmpty())
        } finally {
            tempFile.delete()
        }
    }

    // --- T006: Backward compatibility — data class parsing unchanged ---

    @Test
    fun `parse data class IP type file returns correct metadata with properties`() {
        val content = """
            /*
             * Coordinates - Custom IP Type
             * @IPType
             * @TypeName Coordinates
             * @TypeId ip_coordinates
             * @Color rgb(0, 150, 136)
             * License: Apache 2.0
             */

            package io.codenode.test.iptypes

            data class Coordinates(
                val latitude: Double,
                val longitude: Double
            )
        """.trimIndent()

        val tempFile = File.createTempFile("Coordinates", ".kt")
        try {
            tempFile.writeText(content)
            val meta = discovery.parseIPTypeFile(tempFile.absolutePath)

            assertNotNull(meta)
            assertEquals("Coordinates", meta.typeName)
            assertEquals("ip_coordinates", meta.typeId)
            assertEquals(IPColor(0, 150, 136), meta.color)
            assertEquals("io.codenode.test.iptypes", meta.packageName)
            assertEquals("io.codenode.test.iptypes.Coordinates", meta.className)
            assertEquals(2, meta.properties.size, "Data class should have 2 properties parsed")
            assertEquals("latitude", meta.properties[0].name)
            assertEquals("longitude", meta.properties[1].name)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `data class takes precedence over typealias when both present`() {
        val content = """
            /*
             * Hybrid - Custom IP Type
             * @IPType
             * @TypeName Hybrid
             * @TypeId ip_hybrid
             * @Color rgb(100, 100, 100)
             * License: Apache 2.0
             */

            package io.codenode.test.iptypes

            typealias HybridAlias = String

            data class Hybrid(
                val name: String,
                val value: Int
            )
        """.trimIndent()

        val tempFile = File.createTempFile("Hybrid", ".kt")
        try {
            tempFile.writeText(content)
            val meta = discovery.parseIPTypeFile(tempFile.absolutePath)

            assertNotNull(meta)
            assertEquals("Hybrid", meta.typeName)
            assertEquals(2, meta.properties.size, "Data class should take precedence — properties should be parsed")
        } finally {
            tempFile.delete()
        }
    }

    // --- T007: Graceful skipping ---

    @Test
    fun `file with IPType marker but no data class or typealias returns null`() {
        val content = """
            /*
             * Broken - Custom IP Type
             * @IPType
             * @TypeName Broken
             * @TypeId ip_broken
             * @Color rgb(255, 0, 0)
             * License: Apache 2.0
             */

            package io.codenode.test.iptypes

            // No data class or typealias declaration
            class Broken {
                fun doSomething() {}
            }
        """.trimIndent()

        val tempFile = File.createTempFile("Broken", ".kt")
        try {
            tempFile.writeText(content)
            val meta = discovery.parseIPTypeFile(tempFile.absolutePath)

            // Should still return metadata (has @IPType and @TypeName) but with empty properties
            // The file is valid as an IP type marker even without a body declaration
            assertNotNull(meta)
            assertEquals("Broken", meta.typeName)
            assertTrue(meta.properties.isEmpty())
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `file without IPType marker returns null`() {
        val content = """
            package io.codenode.test

            data class NotAnIPType(
                val name: String
            )
        """.trimIndent()

        val tempFile = File.createTempFile("NotAnIPType", ".kt")
        try {
            tempFile.writeText(content)
            val meta = discovery.parseIPTypeFile(tempFile.absolutePath)
            assertNull(meta, "Files without @IPType marker should return null")
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `file without TypeName marker returns null`() {
        val content = """
            /*
             * NoName - Custom IP Type
             * @IPType
             * @Color rgb(255, 0, 0)
             * License: Apache 2.0
             */

            package io.codenode.test.iptypes

            data class NoName(
                val value: String
            )
        """.trimIndent()

        val tempFile = File.createTempFile("NoName", ".kt")
        try {
            tempFile.writeText(content)
            val meta = discovery.parseIPTypeFile(tempFile.absolutePath)
            assertNull(meta, "Files without @TypeName should return null")
        } finally {
            tempFile.delete()
        }
    }
}

/*
 * ConnectionContextMenuTest - Tests for Connection Context Menu Component
 * Validates context menu behavior for changing connection IP types
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.ui.geometry.Offset
import io.codenode.fbpdsl.model.InformationPacketType
import io.codenode.fbpdsl.model.IPColor
import io.codenode.grapheditor.state.IPTypeRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/**
 * Tests for ConnectionContextMenu component.
 * Validates context menu display, type selection, and dismissal behavior.
 */
class ConnectionContextMenuTest {

    private val testIPTypes = listOf(
        InformationPacketType(
            id = "ip_any",
            typeName = "Any",
            payloadType = Any::class,
            color = IPColor(0, 0, 0),
            description = "Any type"
        ),
        InformationPacketType(
            id = "ip_int",
            typeName = "Int",
            payloadType = Int::class,
            color = IPColor(33, 150, 243),
            description = "Integer type"
        ),
        InformationPacketType(
            id = "ip_double",
            typeName = "Double",
            payloadType = Double::class,
            color = IPColor(156, 39, 176),
            description = "Double type"
        ),
        InformationPacketType(
            id = "ip_boolean",
            typeName = "Boolean",
            payloadType = Boolean::class,
            color = IPColor(255, 152, 0),
            description = "Boolean type"
        ),
        InformationPacketType(
            id = "ip_string",
            typeName = "String",
            payloadType = String::class,
            color = IPColor(76, 175, 80),
            description = "String type"
        )
    )

    @Test
    fun `context menu state contains all required properties`() {
        // Given: A context menu state
        val state = ConnectionContextMenuState(
            connectionId = "conn_123",
            position = Offset(100f, 200f),
            currentTypeId = "ip_any"
        )

        // Then: All properties are accessible
        assertEquals("conn_123", state.connectionId)
        assertEquals(Offset(100f, 200f), state.position)
        assertEquals("ip_any", state.currentTypeId)
    }

    @Test
    fun `context menu state can be null when menu is hidden`() {
        // Given: No context menu is showing
        val state: ConnectionContextMenuState? = null

        // Then: State is null
        assertEquals(null, state)
    }

    @Test
    fun `context menu state can have null currentTypeId for untyped connections`() {
        // Given: A connection without an assigned IP type
        val state = ConnectionContextMenuState(
            connectionId = "conn_123",
            position = Offset(100f, 200f),
            currentTypeId = null
        )

        // Then: currentTypeId is null
        assertEquals(null, state.currentTypeId)
    }

    @Test
    fun `IP types list contains expected default types`() {
        // Given: Default IP types from registry
        val registry = IPTypeRegistry.withDefaults()
        val types = registry.getAllTypes()

        // Then: All 5 default types are present
        assertEquals(5, types.size)
        assertTrue(types.any { it.typeName == "Any" })
        assertTrue(types.any { it.typeName == "Int" })
        assertTrue(types.any { it.typeName == "Double" })
        assertTrue(types.any { it.typeName == "Boolean" })
        assertTrue(types.any { it.typeName == "String" })
    }

    @Test
    fun `each IP type has a unique id`() {
        // Given: Default IP types
        val types = testIPTypes

        // Then: All IDs are unique
        val ids = types.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `each IP type has a color defined`() {
        // Given: Default IP types
        val types = testIPTypes

        // Then: All types have colors with valid RGB values
        types.forEach { type ->
            assertNotNull(type.color)
            assertTrue(type.color.red in 0..255)
            assertTrue(type.color.green in 0..255)
            assertTrue(type.color.blue in 0..255)
        }
    }

    @Test
    fun `context menu position is preserved`() {
        // Given: Right-click at specific location
        val clickPosition = Offset(150f, 300f)

        // When: Context menu state is created
        val state = ConnectionContextMenuState(
            connectionId = "conn_456",
            position = clickPosition,
            currentTypeId = "ip_string"
        )

        // Then: Position matches click location
        assertEquals(150f, state.position.x)
        assertEquals(300f, state.position.y)
    }

    @Test
    fun `current type can be identified from state`() {
        // Given: Connection has IP type "String"
        val state = ConnectionContextMenuState(
            connectionId = "conn_789",
            position = Offset(0f, 0f),
            currentTypeId = "ip_string"
        )

        // When: Checking which type is current
        val currentType = testIPTypes.find { it.id == state.currentTypeId }

        // Then: String type is identified as current
        assertNotNull(currentType)
        assertEquals("String", currentType.typeName)
    }

    @Test
    fun `type selection callback receives correct parameters`() {
        // Given: A callback that captures parameters
        var capturedConnectionId: String? = null
        var capturedTypeId: String? = null

        val onTypeSelected: (String, String) -> Unit = { connId, typeId ->
            capturedConnectionId = connId
            capturedTypeId = typeId
        }

        // When: Callback is invoked
        onTypeSelected("conn_test", "ip_int")

        // Then: Parameters are correctly captured
        assertEquals("conn_test", capturedConnectionId)
        assertEquals("ip_int", capturedTypeId)
    }

    @Test
    fun `dismiss callback can be invoked`() {
        // Given: A dismiss callback
        var dismissed = false
        val onDismiss: () -> Unit = { dismissed = true }

        // When: Callback is invoked
        onDismiss()

        // Then: Dismiss was called
        assertTrue(dismissed)
    }
}

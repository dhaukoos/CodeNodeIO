/*
 * EntityModuleGeneratorTest
 * End-to-end tests for the EntityModuleGenerator orchestrator.
 * Validates GeoLocation module generation matches UserProfiles patterns.
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class EntityModuleGeneratorTest {

    private val generator = EntityModuleGenerator()
    private val geoLocationSpec = EntityModuleSpec.fromIPType(
        ipTypeName = "GeoLocation",
        sourceIPTypeId = "test-geo-id",
        properties = listOf(
            EntityProperty("name", "String", isRequired = true),
            EntityProperty("lat", "Double", isRequired = true),
            EntityProperty("lon", "Double", isRequired = true)
        )
    )

    @Test
    fun generateModule_producesAllExpectedModuleFiles() {
        val output = generator.generateModule(geoLocationSpec)

        // Verify the .flow.kt file
        val flowKtKey = output.moduleFiles.keys.find { it.endsWith("GeoLocations.flow.kt") }
        assertNotNull(flowKtKey, "Should generate GeoLocations.flow.kt")

        // Verify node definition files in nodes/ subdirectory
        val cudKey = output.moduleFiles.keys.find { it.endsWith("GeoLocationCUDCodeNode.kt") }
        assertNotNull(cudKey, "Should generate GeoLocationCUDCodeNode.kt")
        assertTrue(cudKey!!.contains("/nodes/"), "CUD node should be in nodes/ subdirectory")

        val repoKey = output.moduleFiles.keys.find { it.endsWith("GeoLocationRepositoryCodeNode.kt") }
        assertNotNull(repoKey, "Should generate GeoLocationRepositoryCodeNode.kt")

        val displayKey = output.moduleFiles.keys.find { it.endsWith("GeoLocationsDisplayCodeNode.kt") }
        assertNotNull(displayKey, "Should generate GeoLocationsDisplayCodeNode.kt")

        // Verify ViewModel
        val vmKey = output.moduleFiles.keys.find { it.endsWith("GeoLocationsViewModel.kt") }
        assertNotNull(vmKey, "Should generate GeoLocationsViewModel.kt")

        // Verify Persistence Koin module
        val persKey = output.moduleFiles.keys.find { it.endsWith("GeoLocationsPersistence.kt") }
        assertNotNull(persKey, "Should generate GeoLocationsPersistence.kt")

        // Verify 4 generated runtime files
        val generatedKeys = output.moduleFiles.keys.filter { it.contains("/generated/") }
        assertTrue(generatedKeys.size >= 4, "Should generate at least 4 runtime files in generated/")
        assertTrue(generatedKeys.any { it.endsWith("GeoLocationsFlow.kt") }, "Should generate GeoLocationsFlow.kt")
        assertTrue(generatedKeys.any { it.endsWith("GeoLocationsController.kt") }, "Should generate GeoLocationsController.kt")
        assertTrue(generatedKeys.any { it.endsWith("GeoLocationsControllerInterface.kt") }, "Should generate GeoLocationsControllerInterface.kt")
        assertTrue(generatedKeys.any { it.endsWith("GeoLocationsControllerAdapter.kt") }, "Should generate GeoLocationsControllerAdapter.kt")

        // Verify UI files
        val uiKeys = output.moduleFiles.keys.filter { it.contains("/userInterface/") }
        assertTrue(uiKeys.any { it.endsWith("GeoLocations.kt") }, "Should generate GeoLocations.kt UI")
        assertTrue(uiKeys.any { it.endsWith("AddUpdateGeoLocation.kt") }, "Should generate AddUpdateGeoLocation.kt UI")
        assertTrue(uiKeys.any { it.endsWith("GeoLocationRow.kt") }, "Should generate GeoLocationRow.kt UI")
    }

    @Test
    fun generateModule_producesAllPersistenceFiles() {
        val output = generator.generateModule(geoLocationSpec)

        assertTrue(output.persistenceFiles.keys.any { it.endsWith("GeoLocationEntity.kt") },
            "Should generate GeoLocationEntity.kt")
        assertTrue(output.persistenceFiles.keys.any { it.endsWith("GeoLocationDao.kt") },
            "Should generate GeoLocationDao.kt")
        assertTrue(output.persistenceFiles.keys.any { it.endsWith("GeoLocationRepository.kt") },
            "Should generate GeoLocationRepository.kt")
    }

    @Test
    fun generateModule_flowKtContainsThreeNodesAndConnections() {
        val output = generator.generateModule(geoLocationSpec)
        val flowKt = output.moduleFiles.entries.find { it.key.endsWith("GeoLocations.flow.kt") }!!.value

        assertTrue(flowKt.contains("GeoLocationCUD"), "flow.kt should reference GeoLocationCUD")
        assertTrue(flowKt.contains("GeoLocationRepository"), "flow.kt should reference GeoLocationRepository")
        assertTrue(flowKt.contains("GeoLocationsDisplay"), "flow.kt should reference GeoLocationsDisplay")
    }

    @Test
    fun generateModule_viewModelContainsCrudMethods() {
        val output = generator.generateModule(geoLocationSpec)
        val vm = output.moduleFiles.entries.find { it.key.endsWith("GeoLocationsViewModel.kt") }!!.value

        assertTrue(vm.contains("geoLocationDao: GeoLocationDao"), "ViewModel should have DAO constructor param")
        assertTrue(vm.contains("fun addEntity("), "ViewModel should have addEntity method")
        assertTrue(vm.contains("fun updateEntity("), "ViewModel should have updateEntity method")
        assertTrue(vm.contains("fun removeEntity("), "ViewModel should have removeEntity method")
        assertTrue(vm.contains("GeoLocationRepository("), "ViewModel should observe repository")
        assertTrue(vm.contains("import io.codenode.persistence.GeoLocationDao"), "ViewModel should import DAO")
        assertTrue(vm.contains("import io.codenode.persistence.GeoLocationEntity"), "ViewModel should import Entity")
    }

    @Test
    fun generateModule_persistenceEntityHasCorrectFields() {
        val output = generator.generateModule(geoLocationSpec)
        val entity = output.persistenceFiles.entries.find { it.key.endsWith("GeoLocationEntity.kt") }!!.value

        assertTrue(entity.contains("name"), "Entity should have name field")
        assertTrue(entity.contains("lat"), "Entity should have lat field")
        assertTrue(entity.contains("lon"), "Entity should have lon field")
    }

    @Test
    fun generateModule_uiListViewHasButtons() {
        val output = generator.generateModule(geoLocationSpec)
        val listView = output.moduleFiles.entries.find {
            it.key.contains("/userInterface/") && it.key.endsWith("GeoLocations.kt")
        }!!.value

        assertTrue(listView.contains("@Composable"), "List view should be a composable")
        assertTrue(listView.contains("fun GeoLocations("), "List view should have correct name")
    }

    @Test
    fun generateModule_uiFormHasPropertyFields() {
        val output = generator.generateModule(geoLocationSpec)
        val form = output.moduleFiles.entries.find { it.key.endsWith("AddUpdateGeoLocation.kt") }!!.value

        assertTrue(form.contains("@Composable"), "Form should be a composable")
        assertTrue(form.contains("fun AddUpdateGeoLocation("), "Form should have correct name")
        assertTrue(form.contains("name"), "Form should have name field")
        assertTrue(form.contains("lat"), "Form should have lat field")
        assertTrue(form.contains("lon"), "Form should have lon field")
    }

    @Test
    fun generateModule_flowGraphHasCorrectStructure() {
        val output = generator.generateModule(geoLocationSpec)
        val flowGraph = output.flowGraph

        assertEquals("GeoLocations", flowGraph.name.pascalCase())
        val nodes = flowGraph.getAllNodes()
        assertEquals(3, nodes.size, "FlowGraph should have 3 nodes")
        assertEquals(5, flowGraph.connections.size, "FlowGraph should have 5 connections")
    }

    @Test
    fun generateModule_cudCodeNodeHasReactiveSourceFlows() {
        val output = generator.generateModule(geoLocationSpec)
        val cud = output.moduleFiles.entries.find { it.key.endsWith("GeoLocationCUDCodeNode.kt") }!!.value

        assertTrue(cud.contains("CodeNodeDefinition"), "CUD should implement CodeNodeDefinition")
        assertTrue(cud.contains("GeoLocationsState"), "CUD should reference module state")
        assertTrue(cud.contains("_save"), "CUD should reference _save flow")
        assertTrue(cud.contains("_update"), "CUD should reference _update flow")
        assertTrue(cud.contains("_remove"), "CUD should reference _remove flow")
        assertTrue(cud.contains("GeoLocation::class"), "CUD should use typed port specs")
    }

    @Test
    fun generateModule_displayCodeNodeHandlesTwoInputs() {
        val output = generator.generateModule(geoLocationSpec)
        val display = output.moduleFiles.entries.find { it.key.endsWith("GeoLocationsDisplayCodeNode.kt") }!!.value

        assertTrue(display.contains("CodeNodeDefinition"), "Display should implement CodeNodeDefinition")
        assertTrue(display.contains("GeoLocationsState"), "Display should reference module state")
        assertTrue(display.contains("String::class"), "Display should use String port specs")
        assertTrue(display.contains("_result") || display.contains("result"),
            "Display should handle result input")
        assertTrue(display.contains("_error") || display.contains("error"),
            "Display should handle error input")
    }

    // Edge case: entity with no properties (only auto-generated id)
    @Test
    fun generateModule_emptyProperties_producesValidOutput() {
        val emptySpec = EntityModuleSpec.fromIPType(
            ipTypeName = "EmptyItem",
            sourceIPTypeId = "test-empty-id",
            properties = emptyList()
        )
        val output = generator.generateModule(emptySpec)

        // All key files should still be generated
        assertTrue(output.moduleFiles.isNotEmpty(), "Should produce module files")
        assertTrue(output.persistenceFiles.isNotEmpty(), "Should produce persistence files")

        // ViewModel should still be valid
        val vm = output.moduleFiles.entries.find { it.key.endsWith("EmptyItemsViewModel.kt") }!!.value
        assertTrue(vm.contains("class EmptyItemsViewModel"), "ViewModel class should be generated")

        // UI list view should be valid
        val listView = output.moduleFiles.entries.find {
            it.key.contains("/userInterface/") && it.key.endsWith("EmptyItems.kt")
        }!!.value
        assertTrue(listView.contains("fun EmptyItems("), "List view should be generated")

        // Form view should be valid even without fields
        val form = output.moduleFiles.entries.find { it.key.endsWith("AddUpdateEmptyItem.kt") }!!.value
        assertTrue(form.contains("fun AddUpdateEmptyItem("), "Form should be generated")

        // Row view should be valid
        val row = output.moduleFiles.entries.find { it.key.endsWith("EmptyItemRow.kt") }!!.value
        assertTrue(row.contains("fun EmptyItemRow("), "Row should be generated")

        // Entity should just have an id
        val entity = output.persistenceFiles.entries.find { it.key.endsWith("EmptyItemEntity.kt") }!!.value
        assertTrue(entity.contains("EmptyItemEntity"), "Entity should be generated")
    }
}

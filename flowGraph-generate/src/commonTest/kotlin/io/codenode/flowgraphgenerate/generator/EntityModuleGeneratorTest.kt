/*
 * EntityModuleGeneratorTest
 * End-to-end tests for the EntityModuleGenerator orchestrator.
 * Validates Sample module generation matches expected patterns.
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.generator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class EntityModuleGeneratorTest {

    private val generator = EntityModuleGenerator()
    private val sampleSpec = EntityModuleSpec.fromIPType(
        ipTypeName = "Sample",
        sourceIPTypeId = "test-sample-id",
        properties = listOf(
            EntityProperty("name", "String", isRequired = true),
            EntityProperty("lat", "Double", isRequired = true),
            EntityProperty("lon", "Double", isRequired = true)
        )
    )

    @Test
    fun generateModule_producesAllExpectedModuleFiles() {
        val output = generator.generateModule(sampleSpec)

        // Verify the .flow.kt file
        val flowKtKey = output.moduleFiles.keys.find { it.endsWith("Samples.flow.kt") }
        assertNotNull(flowKtKey, "Should generate Samples.flow.kt")

        // Verify node definition files in nodes/ subdirectory
        val cudKey = output.moduleFiles.keys.find { it.endsWith("SampleCUDCodeNode.kt") }
        assertNotNull(cudKey, "Should generate SampleCUDCodeNode.kt")
        assertTrue(cudKey!!.contains("/nodes/"), "CUD node should be in nodes/ subdirectory")

        val repoKey = output.moduleFiles.keys.find { it.endsWith("SampleRepositoryCodeNode.kt") }
        assertNotNull(repoKey, "Should generate SampleRepositoryCodeNode.kt")

        val displayKey = output.moduleFiles.keys.find { it.endsWith("SamplesDisplayCodeNode.kt") }
        assertNotNull(displayKey, "Should generate SamplesDisplayCodeNode.kt")

        // Verify ViewModel
        val vmKey = output.moduleFiles.keys.find { it.endsWith("SamplesViewModel.kt") }
        assertNotNull(vmKey, "Should generate SamplesViewModel.kt")

        // Verify Persistence Koin module
        val persKey = output.moduleFiles.keys.find { it.endsWith("SamplesPersistence.kt") }
        assertNotNull(persKey, "Should generate SamplesPersistence.kt")

        // Feature 085 (universal-runtime collapse): generated/ now holds
        // ControllerInterface + Runtime (the legacy Flow + Controller + Adapter trio is gone).
        val generatedKeys = output.moduleFiles.keys.filter { it.contains("/generated/") }
        assertTrue(generatedKeys.size >= 2, "Should generate ControllerInterface + Runtime in generated/")
        assertTrue(generatedKeys.any { it.endsWith("SamplesControllerInterface.kt") }, "Should generate SamplesControllerInterface.kt")
        assertTrue(generatedKeys.any { it.endsWith("SamplesRuntime.kt") }, "Should generate SamplesRuntime.kt")

        // Verify UI files
        val uiKeys = output.moduleFiles.keys.filter { it.contains("/userInterface/") }
        assertTrue(uiKeys.any { it.endsWith("Samples.kt") }, "Should generate Samples.kt UI")
        assertTrue(uiKeys.any { it.endsWith("AddUpdateSample.kt") }, "Should generate AddUpdateSample.kt UI")
        assertTrue(uiKeys.any { it.endsWith("SampleRow.kt") }, "Should generate SampleRow.kt UI")
    }

    @Test
    fun generateModule_producesAllPersistenceFiles() {
        val output = generator.generateModule(sampleSpec)

        assertTrue(output.persistenceFiles.keys.any { it.endsWith("SampleEntity.kt") },
            "Should generate SampleEntity.kt")
        assertTrue(output.persistenceFiles.keys.any { it.endsWith("SampleDao.kt") },
            "Should generate SampleDao.kt")
        assertTrue(output.persistenceFiles.keys.any { it.endsWith("SampleRepository.kt") },
            "Should generate SampleRepository.kt")

        // Verify files are in entity subdirectory
        assertTrue(output.persistenceFiles.keys.all { it.contains("/sample/") },
            "All persistence files should be in sample/ subdirectory")

        // Verify entity sub-package in generated content
        val entity = output.persistenceFiles.entries.find { it.key.endsWith("SampleEntity.kt") }!!.value
        assertTrue(entity.contains("package io.codenode.persistence.sample"),
            "Entity should declare entity sub-package")
    }

    @Test
    fun generateModule_flowKtContainsThreeNodesAndConnections() {
        val output = generator.generateModule(sampleSpec)
        val flowKt = output.moduleFiles.entries.find { it.key.endsWith("Samples.flow.kt") }!!.value

        assertTrue(flowKt.contains("SampleCUD"), "flow.kt should reference SampleCUD")
        assertTrue(flowKt.contains("SampleRepository"), "flow.kt should reference SampleRepository")
        assertTrue(flowKt.contains("SamplesDisplay"), "flow.kt should reference SamplesDisplay")
    }

    @Test
    fun generateModule_viewModelContainsCrudMethods() {
        val output = generator.generateModule(sampleSpec)
        val vm = output.moduleFiles.entries.find { it.key.endsWith("SamplesViewModel.kt") }!!.value

        assertTrue(vm.contains("sampleDao: SampleDao"), "ViewModel should have DAO constructor param")
        assertTrue(vm.contains("fun addEntity("), "ViewModel should have addEntity method")
        assertTrue(vm.contains("fun updateEntity("), "ViewModel should have updateEntity method")
        assertTrue(vm.contains("fun removeEntity("), "ViewModel should have removeEntity method")
        assertTrue(vm.contains("SampleRepository("), "ViewModel should observe repository")
        assertTrue(vm.contains("import io.codenode.persistence.sample.SampleDao"), "ViewModel should import DAO from entity sub-package")
        assertTrue(vm.contains("import io.codenode.persistence.sample.SampleEntity"), "ViewModel should import Entity from entity sub-package")
    }

    @Test
    fun generateModule_persistenceEntityHasCorrectFields() {
        val output = generator.generateModule(sampleSpec)
        val entity = output.persistenceFiles.entries.find { it.key.endsWith("SampleEntity.kt") }!!.value

        assertTrue(entity.contains("name"), "Entity should have name field")
        assertTrue(entity.contains("lat"), "Entity should have lat field")
        assertTrue(entity.contains("lon"), "Entity should have lon field")
    }

    @Test
    fun generateModule_uiListViewHasButtons() {
        val output = generator.generateModule(sampleSpec)
        val listView = output.moduleFiles.entries.find {
            it.key.contains("/userInterface/") && it.key.endsWith("Samples.kt")
        }!!.value

        assertTrue(listView.contains("@Composable"), "List view should be a composable")
        assertTrue(listView.contains("fun Samples("), "List view should have correct name")
    }

    @Test
    fun generateModule_uiFormHasPropertyFields() {
        val output = generator.generateModule(sampleSpec)
        val form = output.moduleFiles.entries.find { it.key.endsWith("AddUpdateSample.kt") }!!.value

        assertTrue(form.contains("@Composable"), "Form should be a composable")
        assertTrue(form.contains("fun AddUpdateSample("), "Form should have correct name")
        assertTrue(form.contains("name"), "Form should have name field")
        assertTrue(form.contains("lat"), "Form should have lat field")
        assertTrue(form.contains("lon"), "Form should have lon field")
    }

    @Test
    fun generateModule_flowGraphHasCorrectStructure() {
        val output = generator.generateModule(sampleSpec)
        val flowGraph = output.flowGraph

        assertEquals("Samples", flowGraph.name.pascalCase())
        val nodes = flowGraph.getAllNodes()
        assertEquals(3, nodes.size, "FlowGraph should have 3 nodes")
        assertEquals(5, flowGraph.connections.size, "FlowGraph should have 5 connections")
    }

    @Test
    fun generateModule_cudCodeNodeHasReactiveSourceFlows() {
        val output = generator.generateModule(sampleSpec)
        val cud = output.moduleFiles.entries.find { it.key.endsWith("SampleCUDCodeNode.kt") }!!.value

        assertTrue(cud.contains("CodeNodeDefinition"), "CUD should implement CodeNodeDefinition")
        assertTrue(cud.contains("SamplesState"), "CUD should reference module state")
        assertTrue(cud.contains("_save"), "CUD should reference _save flow")
        assertTrue(cud.contains("_update"), "CUD should reference _update flow")
        assertTrue(cud.contains("_remove"), "CUD should reference _remove flow")
        assertTrue(cud.contains("Sample::class"), "CUD should use typed port specs")
    }

    @Test
    fun generateModule_displayCodeNodeHandlesTwoInputs() {
        val output = generator.generateModule(sampleSpec)
        val display = output.moduleFiles.entries.find { it.key.endsWith("SamplesDisplayCodeNode.kt") }!!.value

        assertTrue(display.contains("CodeNodeDefinition"), "Display should implement CodeNodeDefinition")
        assertTrue(display.contains("SamplesState"), "Display should reference module state")
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

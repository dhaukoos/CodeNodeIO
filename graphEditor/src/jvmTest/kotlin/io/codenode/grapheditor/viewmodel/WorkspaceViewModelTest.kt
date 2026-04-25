/*
 * WorkspaceViewModelTest - Unit tests for WorkspaceViewModel
 * License: Apache 2.0
 */

package io.codenode.grapheditor.viewmodel

import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.*

class WorkspaceViewModelTest {

    private lateinit var tempDir: File
    private lateinit var configFile: File

    @BeforeTest
    fun setup() {
        tempDir = createTempDirectory("workspace-test").toFile()
        configFile = File(tempDir, "config.properties")
    }

    @AfterTest
    fun teardown() {
        tempDir.deleteRecursively()
    }

    private fun createViewModel() = WorkspaceViewModel(configFile = configFile)

    private fun createModuleDir(name: String): File {
        val dir = File(tempDir, name)
        dir.mkdirs()
        File(dir, "build.gradle.kts").createNewFile()
        return dir
    }

    @Test
    fun `initial state has no module loaded`() = runTest {
        val vm = createViewModel()
        assertNull(vm.currentModuleDir.value)
        assertEquals("", vm.currentModuleName.value)
        assertNull(vm.activeFlowGraphName.value)
        assertTrue(vm.mruModules.value.isEmpty())
        assertFalse(vm.isDirty.value)
    }

    @Test
    fun `openModule sets state correctly`() = runTest {
        val vm = createViewModel()
        val moduleDir = createModuleDir("StopWatch")

        vm.openModule(moduleDir)

        assertEquals(moduleDir, vm.currentModuleDir.value)
        assertEquals("StopWatch", vm.currentModuleName.value)
        assertNull(vm.activeFlowGraphName.value)
        assertEquals(1, vm.mruModules.value.size)
        assertEquals(moduleDir, vm.mruModules.value[0])
    }

    @Test
    fun `createModule sets state correctly`() = runTest {
        val vm = createViewModel()
        val moduleDir = createModuleDir("WeatherForecast")

        vm.createModule(moduleDir)

        assertEquals(moduleDir, vm.currentModuleDir.value)
        assertEquals("WeatherForecast", vm.currentModuleName.value)
        assertEquals(1, vm.mruModules.value.size)
    }

    @Test
    fun `switchModule updates MRU and resets dirty`() = runTest {
        val vm = createViewModel()
        val module1 = createModuleDir("StopWatch")
        val module2 = createModuleDir("UserProfiles")

        vm.openModule(module1)
        vm.markDirty()
        assertTrue(vm.isDirty.value)

        vm.switchModule(module2)

        assertEquals(module2, vm.currentModuleDir.value)
        assertEquals("UserProfiles", vm.currentModuleName.value)
        assertFalse(vm.isDirty.value)
        assertEquals(2, vm.mruModules.value.size)
        assertEquals(module2, vm.mruModules.value[0])
        assertEquals(module1, vm.mruModules.value[1])
    }

    @Test
    fun `setActiveFlowGraph updates name`() = runTest {
        val vm = createViewModel()
        vm.setActiveFlowGraph("MainFlow")
        assertEquals("MainFlow", vm.activeFlowGraphName.value)
    }

    @Test
    fun `markDirty and markClean toggle isDirty`() = runTest {
        val vm = createViewModel()
        assertFalse(vm.isDirty.value)

        vm.markDirty()
        assertTrue(vm.isDirty.value)

        vm.markClean()
        assertFalse(vm.isDirty.value)
    }

    @Test
    fun `persistState and restoreState round-trip`() = runTest {
        val module1 = createModuleDir("StopWatch")
        val module2 = createModuleDir("UserProfiles")

        val vm1 = createViewModel()
        vm1.openModule(module1)
        vm1.openModule(module2)
        vm1.persistState()

        val vm2 = createViewModel()
        vm2.restoreState()

        assertEquals(module2.absolutePath, vm2.currentModuleDir.value?.absolutePath)
        assertEquals("UserProfiles", vm2.currentModuleName.value)
        assertEquals(2, vm2.mruModules.value.size)
        assertEquals(module2.absolutePath, vm2.mruModules.value[0].absolutePath)
        assertEquals(module1.absolutePath, vm2.mruModules.value[1].absolutePath)
    }

    @Test
    fun `restoreState skips deleted directories`() = runTest {
        val module1 = createModuleDir("StopWatch")
        val module2 = createModuleDir("UserProfiles")

        val vm1 = createViewModel()
        vm1.openModule(module1)
        vm1.openModule(module2)
        vm1.persistState()

        module1.deleteRecursively()

        val vm2 = createViewModel()
        vm2.restoreState()

        assertEquals(1, vm2.mruModules.value.size)
        assertEquals(module2.absolutePath, vm2.mruModules.value[0].absolutePath)
    }

    @Test
    fun `restoreState with no config file is no-op`() = runTest {
        val vm = createViewModel()
        vm.restoreState()

        assertNull(vm.currentModuleDir.value)
        assertEquals("", vm.currentModuleName.value)
        assertTrue(vm.mruModules.value.isEmpty())
    }

    @Test
    fun `MRU keeps most recent first and limits to 5`() = runTest {
        val vm = createViewModel()
        val modules = (1..7).map { createModuleDir("Module$it") }

        modules.forEach { vm.openModule(it) }

        assertEquals(5, vm.mruModules.value.size)
        assertEquals(modules[6], vm.mruModules.value[0])
        assertEquals(modules[5], vm.mruModules.value[1])
    }

    @Test
    fun `MRU does not duplicate entries`() = runTest {
        val vm = createViewModel()
        val module1 = createModuleDir("StopWatch")
        val module2 = createModuleDir("UserProfiles")

        vm.openModule(module1)
        vm.openModule(module2)
        vm.openModule(module1)

        assertEquals(2, vm.mruModules.value.size)
        assertEquals(module1, vm.mruModules.value[0])
        assertEquals(module2, vm.mruModules.value[1])
    }

    @Test
    fun `persistState preserves existing config properties`() = runTest {
        configFile.parentFile?.mkdirs()
        val props = java.util.Properties()
        props.setProperty("subscription.tier", "SIM")
        configFile.outputStream().use { props.store(it, null) }

        val vm = createViewModel()
        val module = createModuleDir("StopWatch")
        vm.openModule(module)

        val restored = java.util.Properties()
        configFile.inputStream().use { restored.load(it) }
        assertEquals("SIM", restored.getProperty("subscription.tier"))
        assertEquals(module.absolutePath, restored.getProperty("workspace.current"))
    }
}

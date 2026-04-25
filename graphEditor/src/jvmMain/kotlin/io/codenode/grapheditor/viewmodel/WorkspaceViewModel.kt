/*
 * WorkspaceViewModel - Manages module workspace context, active flowGraph, and MRU list
 * License: Apache 2.0
 */

package io.codenode.grapheditor.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Properties

class WorkspaceViewModel(
    private val configFile: File = File(System.getProperty("user.home"), ".codenode/config.properties")
) : ViewModel() {

    private val _currentModuleDir = MutableStateFlow<File?>(null)
    val currentModuleDir: StateFlow<File?> = _currentModuleDir.asStateFlow()

    private val _currentModuleName = MutableStateFlow("")
    val currentModuleName: StateFlow<String> = _currentModuleName.asStateFlow()

    private val _activeFlowGraphName = MutableStateFlow<String?>(null)
    val activeFlowGraphName: StateFlow<String?> = _activeFlowGraphName.asStateFlow()

    private val _mruModules = MutableStateFlow<List<File>>(emptyList())
    val mruModules: StateFlow<List<File>> = _mruModules.asStateFlow()

    private val _isDirty = MutableStateFlow(false)
    val isDirty: StateFlow<Boolean> = _isDirty.asStateFlow()

    companion object {
        private const val KEY_WORKSPACE_CURRENT = "workspace.current"
        private const val KEY_WORKSPACE_MRU = "workspace.mru"
        private const val MAX_MRU_SIZE = 5
    }

    fun openModule(moduleDir: File) {
        _currentModuleDir.value = moduleDir
        _currentModuleName.value = moduleDir.name
        _activeFlowGraphName.value = null
        addToMru(moduleDir)
        persistState()
    }

    fun createModule(moduleDir: File) {
        _currentModuleDir.value = moduleDir
        _currentModuleName.value = moduleDir.name
        _activeFlowGraphName.value = null
        addToMru(moduleDir)
        persistState()
    }

    fun switchModule(moduleDir: File) {
        _currentModuleDir.value = moduleDir
        _currentModuleName.value = moduleDir.name
        _activeFlowGraphName.value = null
        _isDirty.value = false
        addToMru(moduleDir)
        persistState()
    }

    fun setActiveFlowGraph(name: String, file: File? = null) {
        _activeFlowGraphName.value = name
    }

    fun markDirty() {
        _isDirty.value = true
    }

    fun markClean() {
        _isDirty.value = false
    }

    fun persistState() {
        try {
            configFile.parentFile?.mkdirs()
            val props = Properties()
            if (configFile.exists()) {
                configFile.inputStream().use { props.load(it) }
            }
            val currentDir = _currentModuleDir.value
            if (currentDir != null) {
                props.setProperty(KEY_WORKSPACE_CURRENT, currentDir.absolutePath)
            } else {
                props.remove(KEY_WORKSPACE_CURRENT)
            }
            val mruPaths = _mruModules.value.joinToString(",") { it.absolutePath }
            if (mruPaths.isNotEmpty()) {
                props.setProperty(KEY_WORKSPACE_MRU, mruPaths)
            } else {
                props.remove(KEY_WORKSPACE_MRU)
            }
            configFile.outputStream().use { props.store(it, "CodeNodeIO configuration") }
        } catch (_: Exception) {
            // Best-effort persistence
        }
    }

    fun restoreState() {
        if (!configFile.exists()) return
        try {
            val props = Properties()
            configFile.inputStream().use { props.load(it) }

            val currentPath = props.getProperty(KEY_WORKSPACE_CURRENT)
            if (currentPath != null) {
                val dir = File(currentPath)
                if (dir.isDirectory) {
                    _currentModuleDir.value = dir
                    _currentModuleName.value = dir.name
                }
            }

            val mruPaths = props.getProperty(KEY_WORKSPACE_MRU)
            if (mruPaths != null) {
                _mruModules.value = mruPaths.split(",")
                    .map { File(it.trim()) }
                    .filter { it.isDirectory }
                    .take(MAX_MRU_SIZE)
            }
        } catch (_: Exception) {
            // Best-effort restore
        }
    }

    private fun addToMru(moduleDir: File) {
        val current = _mruModules.value.toMutableList()
        current.removeAll { it.absolutePath == moduleDir.absolutePath }
        current.add(0, moduleDir)
        _mruModules.value = current.take(MAX_MRU_SIZE)
    }
}

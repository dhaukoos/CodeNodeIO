# Internal Interface Contracts: Entity Repository Nodes

**Feature**: 036-entity-repository-nodes
**Date**: 2026-03-01

## Contract 1: RepositoryNodeDefinition Extension

**Module**: graphEditor
**File**: `graphEditor/src/jvmMain/kotlin/repository/CustomNodeDefinition.kt`

### Extended Fields

```kotlin
@Serializable
data class CustomNodeDefinition(
    val id: String,
    val name: String,
    val inputCount: Int,
    val outputCount: Int,
    val genericType: String,
    val createdAt: Long,
    val anyInput: Boolean = false,
    // NEW fields for repository nodes
    val isRepository: Boolean = false,
    val sourceIPTypeId: String? = null,
    val sourceIPTypeName: String? = null
)
```

### Factory Method

```kotlin
companion object {
    fun createRepository(
        ipTypeName: String,
        sourceIPTypeId: String
    ): CustomNodeDefinition
    // Returns: CustomNodeDefinition with:
    //   name = "{ipTypeName}Repository"
    //   inputCount = 3, outputCount = 2
    //   genericType = "in3out2"
    //   isRepository = true
    //   sourceIPTypeId = sourceIPTypeId
    //   sourceIPTypeName = ipTypeName
}
```

### Port Configuration for toNodeTypeDefinition()

When `isRepository == true`, `toNodeTypeDefinition()` must create ports:
- Input ports: save({ipTypeName}), update({ipTypeName}), remove({ipTypeName})
- Output ports: result({ipTypeName}), error(String)

Port types are resolved from IPTypeRegistry at conversion time.

---

## Contract 2: Repository Node Creation Service

**Module**: graphEditor
**File**: `graphEditor/src/jvmMain/kotlin/viewmodel/PropertiesPanelViewModel.kt` (or new service)

### Create Repository Node

```kotlin
fun createRepositoryNode(
    ipType: InformationPacketType,
    ipTypeId: String,
    customNodeRepository: CustomNodeRepository
): CustomNodeDefinition?
```

**Preconditions**:
- `ipType` must be a custom IP type (exists in `IPTypeRegistry.getCustomTypeIds()`)
- No existing repository node for this `ipTypeId`

**Postconditions**:
- New `CustomNodeDefinition` with `isRepository = true` persisted
- Returns the created definition, or null if preconditions not met

### Check Repository Exists

```kotlin
fun repositoryExistsForIPType(
    ipTypeId: String,
    customNodeRepository: CustomNodeRepository
): Boolean
```

---

## Contract 3: RepositoryCodeGenerator

**Module**: kotlinCompiler
**File**: `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RepositoryCodeGenerator.kt`

### Generate Repository Persistence Code

```kotlin
class RepositoryCodeGenerator {
    fun generateBaseDao(packageName: String): String
    fun generateEntity(
        entityName: String,
        properties: List<EntityProperty>,
        packageName: String
    ): String
    fun generateDao(
        entityName: String,
        tableName: String,
        packageName: String
    ): String
    fun generateRepository(
        entityName: String,
        packageName: String
    ): String
    fun generateDatabase(
        entities: List<EntityInfo>,
        packageName: String
    ): String
    fun generateDatabaseBuilder(
        platform: String,
        packageName: String,
        dbFileName: String
    ): String
}

data class EntityProperty(
    val name: String,
    val kotlinType: String,
    val isRequired: Boolean
)

data class EntityInfo(
    val entityName: String,
    val tableName: String,
    val daoName: String
)
```

### Integration Point

`ModuleSaveService.saveModule()` detects repository nodes in the FlowGraph:
```kotlin
val repositoryNodes = flowGraph.getAllCodeNodes()
    .filter { it.configuration["_repository"] == "true" }

if (repositoryNodes.isNotEmpty()) {
    val repoGenerator = RepositoryCodeGenerator()
    // Generate persistence/ package files
}
```

---

## Contract 4: IPTypePropertiesPanel (UI)

**Module**: graphEditor
**File**: `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt`

### Composable Signature

```kotlin
@Composable
fun IPTypePropertiesPanel(
    ipType: InformationPacketType,
    ipTypeRegistry: IPTypeRegistry,
    onCreateRepositoryNode: (() -> Unit)? = null,
    repositoryExists: Boolean = false,
    modifier: Modifier = Modifier
)
```

### Display Requirements

1. Always show: type name, color swatch, description
2. For custom types with properties: show Properties section with name/type/required badge
3. For custom types: show "Create Repository Node" button
   - Enabled when `onCreateRepositoryNode != null && !repositoryExists`
   - Disabled with "Repository exists" label when `repositoryExists == true`
4. For built-in types: no Properties section, no create button

---

## Contract 5: Generated Runtime Wiring for Repository Nodes

**Module**: kotlinCompiler
**Integration**: RuntimeFlowGenerator extension

### Repository Node Runtime Pattern

When generating Flow class code for a repository node, emit:

```kotlin
// In start() method:
scope.launch {
    val repository = {EntityName}Repository(
        DatabaseModule.getDatabase().{entityName}Dao()
    )

    // Observe all - reactive stream to result output
    scope.launch {
        repository.observeAll().collect { entities ->
            {varName}.outputChannel1.send(entities)
        }
    }

    // Save input handler
    scope.launch {
        for (item in {varName}.inputChannel1) {
            try {
                repository.save(item)
            } catch (e: Exception) {
                {varName}.outputChannel2.send(e.message ?: "Save failed")
            }
        }
    }

    // Update input handler
    scope.launch {
        for (item in {varName}.inputChannel2) {
            try {
                repository.update(item)
            } catch (e: Exception) {
                {varName}.outputChannel2.send(e.message ?: "Update failed")
            }
        }
    }

    // Remove input handler
    scope.launch {
        for (item in {varName}.inputChannel3) {
            try {
                repository.remove(item)
            } catch (e: Exception) {
                {varName}.outputChannel2.send(e.message ?: "Remove failed")
            }
        }
    }
}
```

### Error Channel Contract

- All repository operation errors are caught and sent as String messages to the error output channel
- Error messages include the operation name and exception message
- Channel closure on the error port does not affect other operations

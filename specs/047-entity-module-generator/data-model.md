# Data Model: Generalize Entity Repository Module Creation

## Entities

### EntityModuleSpec

Represents the specification for generating a complete entity module. Derived from a custom IP Type.

| Field | Type | Description |
|-------|------|-------------|
| entityName | String | PascalCase entity name (e.g., "GeoLocation") |
| entityNameLower | String | camelCase variant (e.g., "geoLocation") |
| pluralName | String | PascalCase plural (e.g., "GeoLocations") |
| pluralNameLower | String | camelCase plural (e.g., "geoLocations") |
| properties | List<EntityProperty> | Entity fields from IP Type definition |
| sourceIPTypeId | String | UUID of the source custom IP Type |
| basePackage | String | Module package (e.g., "io.codenode.geolocations") |
| persistencePackage | String | Always "io.codenode.persistence" |

### EntityProperty (existing)

Already defined in `kotlinCompiler/generator/RepositoryCodeGenerator.kt`:

| Field | Type | Description |
|-------|------|-------------|
| name | String | Property name (camelCase) |
| kotlinType | String | Kotlin type string ("String", "Int", "Double", "Boolean", etc.) |
| isRequired | Boolean | If true → non-nullable; if false → nullable with default null |

### CustomNodeDefinition (existing, extended)

Extended with two new factory methods:

| Factory Method | Inputs | Outputs | Configuration |
|---------------|--------|---------|---------------|
| createCUD | 0 | 3 (save, update, remove) | _cudSource=true, _sourceIPTypeId, _sourceIPTypeName |
| createRepository | 3 (save, update, remove) | 2 (result, error) | _repository=true, _sourceIPTypeId, _sourceIPTypeName |
| createDisplay | 2 (entities, error) | 0 | _display=true, _sourceIPTypeId, _sourceIPTypeName |

## Generated File Map

For entity "GeoLocation", the following files are generated:

### Entity Module (GeoLocations/)

| File | Generator | Write Policy |
|------|-----------|-------------|
| `GeoLocations.flow.kt` | FlowKtGenerator | Always overwrite |
| `GeoLocationCUD.kt` | EntityCUDGenerator | Write-once |
| `GeoLocationsDisplay.kt` | EntityDisplayGenerator | Write-once |
| `GeoLocationsViewModel.kt` | RuntimeViewModelGenerator (modified) | Selective regeneration |
| `GeoLocationsPersistence.kt` | EntityPersistenceGenerator | Write-once |
| `generated/GeoLocationsFlow.kt` | RuntimeFlowGenerator | Always overwrite |
| `generated/GeoLocationsController.kt` | RuntimeControllerGenerator | Always overwrite |
| `generated/GeoLocationsControllerInterface.kt` | RuntimeControllerInterfaceGenerator | Always overwrite |
| `generated/GeoLocationsControllerAdapter.kt` | RuntimeControllerAdapterGenerator | Always overwrite |
| `processingLogic/GeoLocationRepositoryProcessLogic.kt` | ProcessingLogicStubGenerator | Write-once |
| `userInterface/GeoLocations.kt` | EntityUIGenerator.generateListView | Write-once |
| `userInterface/AddUpdateGeoLocation.kt` | EntityUIGenerator.generateFormView | Write-once |
| `userInterface/GeoLocationRow.kt` | EntityUIGenerator.generateRowView | Write-once |

### Persistence Module (persistence/)

| File | Generator | Write Policy |
|------|-----------|-------------|
| `GeoLocationEntity.kt` | RepositoryCodeGenerator.generateEntity | Write-once |
| `GeoLocationDao.kt` | RepositoryCodeGenerator.generateDao | Write-once |
| `GeoLocationRepository.kt` | RepositoryCodeGenerator.generateRepository | Write-once |
| `AppDatabase.kt` | RepositoryCodeGenerator.generateDatabase | Regenerated (all entities) |

## Property-to-UI Mapping

| Kotlin Type | Form Input | Display Format | Keyboard Type |
|-------------|-----------|----------------|---------------|
| String | OutlinedTextField | Text as-is | Default |
| Int | OutlinedTextField (numeric validation) | Text of value | Number |
| Long | OutlinedTextField (numeric validation) | Text of value | Number |
| Double | OutlinedTextField (numeric validation) | Formatted to 6 decimal places | Decimal |
| Float | OutlinedTextField (numeric validation) | Formatted to 2 decimal places | Decimal |
| Boolean | Checkbox | "Yes"/"No" text | N/A |

Optional properties (isRequired=false) show "(optional)" in label and allow empty input.

## Relationships

```
IP Type (IP Generator)
  └─ triggers ──→ EntityModuleSpec
                    ├── 3x CustomNodeDefinition (CUD, Repository, Display)
                    ├── 1x FlowGraph (connecting the 3 nodes)
                    ├── 1x Generated Module directory (13 files)
                    └── 3x Persistence files in shared persistence module
```

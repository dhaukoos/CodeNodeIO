# Quickstart: Generalize Entity Repository Module Creation

## Validation Scenario: GeoLocation Module

### Prerequisites

1. graphEditor is running (`./gradlew :graphEditor:run`)
2. No existing "GeoLocation" IP Type or "GeoLocations" module

### Steps

1. **Create GeoLocation IP Type**
   - Open the IP Generator panel
   - Enter type name: "GeoLocation"
   - Add properties:
     - `latitude` — Double, required
     - `longitude` — Double, required
     - `label` — String, required
     - `altitude` — Double, optional
     - `isActive` — Boolean, required
   - Click "Create"

2. **Generate the Module**
   - Select the GeoLocation IP Type in the IP Type Properties panel
   - Verify the button reads "Create Repository Module" (not "Create Repository Node")
   - Click "Create Repository Module"
   - Verify the button changes to "Module exists" (disabled)

3. **Verify Generated Files**
   - Check that `GeoLocations/` directory exists with:
     - `src/commonMain/kotlin/io/codenode/geolocations/GeoLocations.flow.kt`
     - `src/commonMain/kotlin/io/codenode/geolocations/GeoLocationCUD.kt`
     - `src/commonMain/kotlin/io/codenode/geolocations/GeoLocationsDisplay.kt`
     - `src/commonMain/kotlin/io/codenode/geolocations/GeoLocationsViewModel.kt`
     - `src/commonMain/kotlin/io/codenode/geolocations/GeoLocationsPersistence.kt`
     - `src/commonMain/kotlin/io/codenode/geolocations/generated/` (4 files)
     - `src/commonMain/kotlin/io/codenode/geolocations/processingLogic/GeoLocationRepositoryProcessLogic.kt`
     - `src/commonMain/kotlin/io/codenode/geolocations/userInterface/GeoLocations.kt`
     - `src/commonMain/kotlin/io/codenode/geolocations/userInterface/AddUpdateGeoLocation.kt`
     - `src/commonMain/kotlin/io/codenode/geolocations/userInterface/GeoLocationRow.kt`

4. **Verify Persistence Files**
   - Check `persistence/src/commonMain/kotlin/io/codenode/persistence/`:
     - `GeoLocationEntity.kt` — has fields: id, latitude, longitude, label, altitude (nullable), isActive
     - `GeoLocationDao.kt` — extends BaseDao, has getAllAsFlow()
     - `GeoLocationRepository.kt` — wraps DAO with save/update/remove/observeAll
     - `AppDatabase.kt` — lists both UserProfileEntity and GeoLocationEntity in @Database annotation

5. **Verify Compilation**
   - Add `include(":GeoLocations")` to settings.gradle.kts
   - Add Koin wiring to graphEditor Main.kt:
     ```kotlin
     single { DatabaseModule.getDatabase().geoLocationDao() }
     ```
   - Add `geoLocationsModule` to Koin modules list
   - Run `./gradlew :GeoLocations:compileKotlinJvm`
   - Verify BUILD SUCCESSFUL

6. **Verify FlowGraph Structure**
   - Open GeoLocations.flow.kt
   - Confirm three nodes: GeoLocationCUD (source), GeoLocationRepository (processor), GeoLocationsDisplay (sink)
   - Confirm connections: CUD.save → Repository.save, CUD.update → Repository.update, CUD.remove → Repository.remove, Repository.result → Display.entities (or similar)

7. **Verify ViewModel Methods**
   - Open GeoLocationsViewModel.kt
   - Confirm methods: addGeoLocation(item), updateGeoLocation(item), removeGeoLocation(item)
   - Confirm DAO constructor parameter and repository observation in init{}

8. **Verify UI Files**
   - Open GeoLocations.kt — list view with add/update/remove buttons
   - Open AddUpdateGeoLocation.kt — form with fields for latitude (numeric), longitude (numeric), label (text), altitude (numeric, optional), isActive (checkbox)
   - Open GeoLocationRow.kt — single row showing all properties

### Expected Result

A complete, compilable GeoLocations module that follows the exact same structure and patterns as UserProfiles, generated from a single button click.

# Quickstart: Filesystem-Based IP Types

**Feature**: 059-filesystem-ip-types

## Scenario 1: IP Types Discovered from Module Directory

**Setup**: WeatherForecast module has typed data classes in `iptypes/` directory with `@IPType` metadata headers.

**Steps**:
1. Launch graphEditor
2. Open the IP Type Palette

**Expected**: Coordinates, HttpResponse, ForecastData, ForecastDisplayList, and ChartData appear as Module-level types with their defined colors. Built-in types (Int, Double, String, Boolean, Any) also present.

---

## Scenario 2: Create IP Type at Project Level

**Steps**:
1. Launch graphEditor (no module loaded)
2. Open the IP Generator panel
3. Verify Level dropdown shows "Project" and "Universal" (Module disabled)
4. Select "Project" level
5. Enter type name: "SessionInfo"
6. Add properties: `userId` (String), `timestamp` (Double), `isActive` (Boolean)
7. Click Create

**Expected**:
- File created at `iptypes/src/commonMain/kotlin/io/codenode/iptypes/SessionInfo.kt`
- File contains `data class SessionInfo(val userId: String, val timestamp: Double, val isActive: Boolean)`
- File contains `@IPType` metadata comment header
- "SessionInfo" immediately appears in IP Type Palette

---

## Scenario 3: Create IP Type at Module Level

**Steps**:
1. Launch graphEditor with WeatherForecast module loaded
2. Open IP Generator panel
3. Verify Level dropdown shows "Module", "Project", and "Universal"
4. Select "Module" level
5. Enter type name: "WeatherAlert"
6. Add properties: `severity` (String), `message` (String)
7. Click Create

**Expected**:
- File created at `WeatherForecast/src/commonMain/kotlin/io/codenode/weatherforecast/iptypes/WeatherAlert.kt`
- "WeatherAlert" immediately appears in IP Type Palette as a Module-level type

---

## Scenario 4: Create IP Type at Universal Level

**Steps**:
1. Open IP Generator panel
2. Select "Universal" level
3. Enter type name: "LogEntry"
4. Add properties: `level` (String), `message` (String), `timestamp` (Double)
5. Click Create

**Expected**:
- File created at `~/.codenode/iptypes/LogEntry.kt`
- "LogEntry" immediately appears in IP Type Palette

---

## Scenario 5: Delete IP Type File and Relaunch

**Steps**:
1. Create a Project-level IP type "TestType" via the IP Generator
2. Verify "TestType" appears in palette
3. Close graphEditor
4. Delete `iptypes/src/commonMain/kotlin/io/codenode/iptypes/TestType.kt` from filesystem
5. Relaunch graphEditor

**Expected**: "TestType" is no longer in the IP Type Palette. No errors on launch.

---

## Scenario 6: KClass Resolution for Compiled Types

**Steps**:
1. Ensure WeatherForecast module is compiled (types are on classpath)
2. Launch graphEditor
3. Inspect IPTypeRegistry for `Coordinates` type

**Expected**: The registered `Coordinates` type has `payloadType = Coordinates::class` (not `Any::class`). Port connections using Coordinates enforce type matching.

---

## Scenario 7: Legacy JSON Migration

**Setup**: `~/.codenode/custom-ip-types.json` exists with custom types. `~/.codenode/iptypes/` directory does not exist.

**Steps**:
1. Launch graphEditor

**Expected**:
- `~/.codenode/iptypes/` directory created
- Each type from JSON has a corresponding `.kt` file in `~/.codenode/iptypes/`
- JSON file renamed to `custom-ip-types.json.bak`
- All migrated types appear in IP Type Palette

---

## Scenario 8: Duplicate Type Name Across Tiers

**Setup**: Both Module and Universal directories contain an IP type named "Coordinates".

**Steps**:
1. Launch graphEditor with the module loaded

**Expected**: Module-level "Coordinates" takes precedence. Only one "Coordinates" entry appears in the palette, with the Module version's properties and color.

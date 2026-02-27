# Quickstart: IP Generator Interface

**Feature**: 032-ip-generator | **Date**: 2026-02-27

## Integration Scenario 1: Create a Simple Marker Type

1. Launch the graph editor
2. Locate the IP Generator panel above the IP Types palette
3. Click the header to expand the panel
4. Enter "Signal" in the name text field
5. Leave properties empty (zero properties)
6. Click Create
7. **Expected**: "Signal" appears in the IP Types palette with an auto-assigned color
8. **Expected**: The form resets to its empty state

## Integration Scenario 2: Create a Composite Type with Properties

1. Expand the IP Generator panel
2. Enter "UserProfile" in the name text field
3. Click "+" to add a property row
4. Enter "name" as the property name, select "String" from the type dropdown, leave required checked
5. Click "+" to add another property row
6. Enter "age" as the property name, select "Int" from the type dropdown, uncheck required
7. Click "+" to add another property row
8. Enter "active" as the property name, select "Boolean" from the type dropdown, leave required checked
9. Click Create
10. **Expected**: "UserProfile" appears in the IP Types palette
11. **Expected**: The type is available in port type dropdowns throughout the editor
12. **Expected**: The generated code representation includes:
    - `val name: String` (required, non-nullable)
    - `val age: Int? = null` (optional, nullable with null default)
    - `val active: Boolean` (required, non-nullable)

## Integration Scenario 3: Validation Prevents Invalid Creation

1. Expand the IP Generator panel
2. Leave the name field empty
3. **Expected**: Create button is disabled
4. Enter "String" (an existing built-in type name)
5. **Expected**: Create button is disabled, visual indicator shows name is taken
6. Enter "MyType" (a valid unique name)
7. Click "+" to add a property, leave property name empty
8. **Expected**: Create button is disabled
9. Enter "field1" as property name
10. Click "+" to add another property, enter "field1" as property name (duplicate)
11. **Expected**: Create button is disabled, visual indicator shows duplicate
12. Change second property name to "field2"
13. **Expected**: Create button is now enabled

## Integration Scenario 4: Cancel Resets Form

1. Expand the IP Generator panel
2. Enter "TempType" in the name field
3. Add two properties with names and types
4. Click Cancel
5. **Expected**: Name field is empty, all property rows are removed
6. **Expected**: Panel remains expanded

## Integration Scenario 5: Custom Type Used as Property Type

1. Create a "Location" type with properties "lat" (Double) and "lon" (Double)
2. Open the IP Generator again
3. Enter "Event" as the type name
4. Add a property "place", open the type dropdown
5. **Expected**: "Location" appears in the dropdown alongside built-in types
6. Select "Location" as the type for the "place" property
7. Click Create
8. **Expected**: "Event" type is created with a "place" property of type "Location"

## Integration Scenario 6: Persistence Across Sessions

1. Create a "UserProfile" type with properties "name" (String, required) and "age" (Int, optional)
2. Close the graph editor
3. Reopen the graph editor
4. **Expected**: "UserProfile" appears in the IP Types palette with its assigned color
5. **Expected**: The type is available in port type dropdowns
6. Open the IP Generator, try to create another type named "UserProfile"
7. **Expected**: Create button is disabled (duplicate name detected from persisted type)

## Test Verification Points

- **Registry integration**: After creation, `IPTypeRegistry.getByTypeName("UserProfile")` returns non-null
- **Palette display**: New type visible in `IPPaletteContent` with color swatch
- **Dropdown availability**: New type appears in `PortEditorRow` type dropdown
- **Duplicate prevention**: `IPTypeRegistry.getByTypeName("String")` returns non-null, blocking re-creation
- **Case-insensitive**: Creating "string" (lowercase) is also blocked by existing "String"
- **Persistence round-trip**: `FileIPTypeRepository.save()` then `load()` produces identical types with properties
- **Startup loading**: Custom types from `~/.codenode/custom-ip-types.json` are registered in IPTypeRegistry on app launch

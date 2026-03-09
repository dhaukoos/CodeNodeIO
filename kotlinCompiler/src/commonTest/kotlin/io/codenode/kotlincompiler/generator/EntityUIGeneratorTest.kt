/*
 * EntityUIGenerator Test
 * Tests for generating UI composable files for entity CRUD modules
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import kotlin.test.*

class EntityUIGeneratorTest {

    private val generator = EntityUIGenerator()
    private val spec = EntityModuleSpec.fromIPType(
        ipTypeName = "GeoLocation",
        sourceIPTypeId = "ip_geolocation_test123",
        properties = listOf(
            EntityProperty("latitude", "Double", true),
            EntityProperty("longitude", "Double", true),
            EntityProperty("label", "String", true),
            EntityProperty("altitude", "Double", false),
            EntityProperty("isActive", "Boolean", true)
        )
    )

    // ========== generateListView Tests ==========

    @Test
    fun `generateListView contains composable function signature`() {
        val result = generator.generateListView(spec)
        assertTrue(result.contains("@Composable"))
        assertTrue(result.contains("fun GeoLocations("))
    }

    @Test
    fun `generateListView contains ViewModel parameter`() {
        val result = generator.generateListView(spec)
        assertTrue(result.contains("viewModel: GeoLocationsViewModel"))
    }

    @Test
    fun `generateListView contains LazyColumn`() {
        val result = generator.generateListView(spec)
        assertTrue(result.contains("LazyColumn"))
    }

    @Test
    fun `generateListView contains add button`() {
        val result = generator.generateListView(spec)
        assertTrue(result.contains("Text(\"Add\")"))
    }

    @Test
    fun `generateListView contains update button`() {
        val result = generator.generateListView(spec)
        assertTrue(result.contains("Text(\"Update\")"))
    }

    @Test
    fun `generateListView contains remove button`() {
        val result = generator.generateListView(spec)
        assertTrue(result.contains("Text(\"Remove\")"))
    }

    @Test
    fun `generateListView contains remove confirmation dialog`() {
        val result = generator.generateListView(spec)
        assertTrue(result.contains("AlertDialog"))
        assertTrue(result.contains("showRemoveConfirmation"))
    }

    @Test
    fun `generateListView contains correct package`() {
        val result = generator.generateListView(spec)
        assertTrue(result.contains("package io.codenode.geolocations.userInterface"))
    }

    @Test
    fun `generateListView contains entity import`() {
        val result = generator.generateListView(spec)
        assertTrue(result.contains("import io.codenode.persistence.GeoLocationEntity"))
    }

    @Test
    fun `generateListView contains GeoLocationRow reference`() {
        val result = generator.generateListView(spec)
        assertTrue(result.contains("GeoLocationRow("))
    }

    // ========== generateFormView Tests ==========

    @Test
    fun `generateFormView contains composable function signature`() {
        val result = generator.generateFormView(spec)
        assertTrue(result.contains("@Composable"))
        assertTrue(result.contains("fun AddUpdateGeoLocation("))
    }

    @Test
    fun `generateFormView contains OutlinedTextField for latitude`() {
        val result = generator.generateFormView(spec)
        assertTrue(result.contains("latitudeText"))
        assertTrue(result.contains("OutlinedTextField"))
    }

    @Test
    fun `generateFormView contains OutlinedTextField for longitude`() {
        val result = generator.generateFormView(spec)
        assertTrue(result.contains("longitudeText"))
    }

    @Test
    fun `generateFormView contains OutlinedTextField for label`() {
        val result = generator.generateFormView(spec)
        assertTrue(result.contains("labelText"))
    }

    @Test
    fun `generateFormView contains Checkbox for isActive`() {
        val result = generator.generateFormView(spec)
        assertTrue(result.contains("Checkbox"))
        assertTrue(result.contains("isActive"))
    }

    @Test
    fun `generateFormView contains KeyboardType for numeric fields`() {
        val result = generator.generateFormView(spec)
        assertTrue(result.contains("KeyboardType.Decimal"))
    }

    @Test
    fun `generateFormView contains optional label for altitude`() {
        val result = generator.generateFormView(spec)
        assertTrue(result.contains("(optional)"))
    }

    @Test
    fun `generateFormView contains entity entity parameter`() {
        val result = generator.generateFormView(spec)
        assertTrue(result.contains("existingItem: GeoLocationEntity?"))
    }

    @Test
    fun `generateFormView contains onSave callback`() {
        val result = generator.generateFormView(spec)
        assertTrue(result.contains("onSave: (GeoLocationEntity) -> Unit"))
    }

    @Test
    fun `generateFormView constructs entity with all properties`() {
        val result = generator.generateFormView(spec)
        assertTrue(result.contains("GeoLocationEntity("))
        assertTrue(result.contains("latitude ="))
        assertTrue(result.contains("longitude ="))
        assertTrue(result.contains("label ="))
        assertTrue(result.contains("altitude ="))
        assertTrue(result.contains("isActive ="))
    }

    // ========== generateRowView Tests ==========

    @Test
    fun `generateRowView contains composable function signature`() {
        val result = generator.generateRowView(spec)
        assertTrue(result.contains("@Composable"))
        assertTrue(result.contains("fun GeoLocationRow("))
    }

    @Test
    fun `generateRowView contains entity parameter`() {
        val result = generator.generateRowView(spec)
        assertTrue(result.contains("item: GeoLocationEntity"))
    }

    @Test
    fun `generateRowView displays latitude property`() {
        val result = generator.generateRowView(spec)
        assertTrue(result.contains("item.latitude"))
    }

    @Test
    fun `generateRowView displays longitude property`() {
        val result = generator.generateRowView(spec)
        assertTrue(result.contains("item.longitude"))
    }

    @Test
    fun `generateRowView displays label property`() {
        val result = generator.generateRowView(spec)
        assertTrue(result.contains("item.label"))
    }

    @Test
    fun `generateRowView displays altitude with N-A fallback`() {
        val result = generator.generateRowView(spec)
        assertTrue(result.contains("item.altitude"))
    }

    @Test
    fun `generateRowView displays isActive as Yes-No`() {
        val result = generator.generateRowView(spec)
        assertTrue(result.contains("isActive: Yes"))
        assertTrue(result.contains("isActive: No"))
    }

    @Test
    fun `generateRowView contains clickable modifier`() {
        val result = generator.generateRowView(spec)
        assertTrue(result.contains("clickable"))
    }

    @Test
    fun `generateRowView contains correct package`() {
        val result = generator.generateRowView(spec)
        assertTrue(result.contains("package io.codenode.geolocations.userInterface"))
    }
}

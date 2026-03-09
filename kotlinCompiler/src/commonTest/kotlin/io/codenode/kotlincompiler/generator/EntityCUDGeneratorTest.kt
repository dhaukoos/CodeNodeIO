/*
 * EntityCUDGenerator Test
 * Tests for generating {Entity}CUD source node stub files
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import kotlin.test.*

class EntityCUDGeneratorTest {

    private val generator = EntityCUDGenerator()
    private val spec = EntityModuleSpec.fromIPType(
        ipTypeName = "GeoLocation",
        sourceIPTypeId = "ip_geolocation_test123",
        properties = listOf(
            EntityProperty("latitude", "Double", true),
            EntityProperty("longitude", "Double", true),
            EntityProperty("label", "String", true)
        )
    )

    @Test
    fun `generate contains correct package`() {
        val result = generator.generate(spec)
        assertTrue(result.contains("package io.codenode.geolocations"))
    }

    @Test
    fun `generate contains entity CUD function name`() {
        val result = generator.generate(spec)
        assertTrue(result.contains("fun createGeoLocationCUD()"))
    }

    @Test
    fun `generate contains createSourceOut3 factory call`() {
        val result = generator.generate(spec)
        assertTrue(result.contains("CodeNodeFactory.createSourceOut3"))
    }

    @Test
    fun `generate contains correct node name`() {
        val result = generator.generate(spec)
        assertTrue(result.contains("name = \"GeoLocationCUD\""))
    }

    @Test
    fun `generate contains save StateFlow collection`() {
        val result = generator.generate(spec)
        assertTrue(result.contains("GeoLocationsState._save.drop(1).collect"))
    }

    @Test
    fun `generate contains update StateFlow collection`() {
        val result = generator.generate(spec)
        assertTrue(result.contains("GeoLocationsState._update.drop(1).collect"))
    }

    @Test
    fun `generate contains remove StateFlow collection`() {
        val result = generator.generate(spec)
        assertTrue(result.contains("GeoLocationsState._remove.drop(1).collect"))
    }

    @Test
    fun `generate contains ProcessResult3 imports`() {
        val result = generator.generate(spec)
        assertTrue(result.contains("import io.codenode.fbpdsl.runtime.ProcessResult3"))
    }

    @Test
    fun `generate contains coroutineScope usage`() {
        val result = generator.generate(spec)
        assertTrue(result.contains("coroutineScope"))
    }

    @Test
    fun `generate emits ProcessResult3 for save`() {
        val result = generator.generate(spec)
        assertTrue(result.contains("emit(ProcessResult3(save, null, null))"))
    }

    @Test
    fun `generate emits ProcessResult3 for update`() {
        val result = generator.generate(spec)
        assertTrue(result.contains("emit(ProcessResult3(null, update, null))"))
    }

    @Test
    fun `generate emits ProcessResult3 for remove`() {
        val result = generator.generate(spec)
        assertTrue(result.contains("emit(ProcessResult3(null, null, remove))"))
    }
}

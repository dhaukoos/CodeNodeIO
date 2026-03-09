/*
 * EntityDisplayGenerator Test
 * Tests for generating {Entity}sDisplay sink node stub files
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import kotlin.test.*

class EntityDisplayGeneratorTest {

    private val generator = EntityDisplayGenerator()
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
    fun `generate contains correct function name`() {
        val result = generator.generate(spec)
        assertTrue(result.contains("fun createGeoLocationsDisplay()"))
    }

    @Test
    fun `generate contains createSinkIn2 factory call`() {
        val result = generator.generate(spec)
        assertTrue(result.contains("CodeNodeFactory.createSinkIn2"))
    }

    @Test
    fun `generate contains correct node name`() {
        val result = generator.generate(spec)
        assertTrue(result.contains("name = \"GeoLocationsDisplay\""))
    }

    @Test
    fun `generate contains two input parameters in consume lambda`() {
        val result = generator.generate(spec)
        assertTrue(result.contains("consume = { result, error ->"))
    }

    @Test
    fun `generate updates result StateFlow`() {
        val result = generator.generate(spec)
        assertTrue(result.contains("GeoLocationsState._result.value = result"))
    }

    @Test
    fun `generate updates error StateFlow`() {
        val result = generator.generate(spec)
        assertTrue(result.contains("GeoLocationsState._error.value = error"))
    }

    @Test
    fun `generate contains CodeNodeFactory import`() {
        val result = generator.generate(spec)
        assertTrue(result.contains("import io.codenode.fbpdsl.model.CodeNodeFactory"))
    }
}

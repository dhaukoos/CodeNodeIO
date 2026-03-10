package io.codenode.kotlincompiler

import io.codenode.kotlincompiler.generator.*
import java.io.File

fun main() {
    val spec = EntityModuleSpec.fromIPType(
        ipTypeName = "GeoLocation",
        sourceIPTypeId = "test-geo-id",
        properties = listOf(
            EntityProperty("name", "String", isRequired = true),
            EntityProperty("lat", "Double", isRequired = true),
            EntityProperty("lon", "Double", isRequired = true)
        )
    )

    val generator = EntityModuleGenerator()
    val moduleGenerator = ModuleGenerator()
    val output = generator.generateModule(spec)

    val projectRoot = File(".")
    val moduleDir = File(projectRoot, "GeoLocations")
    val persistenceDir = File(projectRoot, "persistence/src/commonMain/kotlin/io/codenode/persistence")

    // Write build.gradle.kts
    val buildGradle = moduleGenerator.generateBuildGradle(output.flowGraph, spec.pluralName, isEntityModule = true)
    File(moduleDir, "build.gradle.kts").apply {
        parentFile.mkdirs()
        writeText(buildGradle)
    }
    println("Wrote: GeoLocations/build.gradle.kts")

    // Write module files
    for ((relativePath, content) in output.moduleFiles) {
        val file = File(moduleDir, relativePath)
        file.parentFile.mkdirs()
        file.writeText(content)
        println("Wrote: GeoLocations/$relativePath")
    }

    // Write persistence files (only if they don't exist)
    for ((relativePath, content) in output.persistenceFiles) {
        val fileName = File(relativePath).name
        val file = File(persistenceDir, fileName)
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.writeText(content)
            println("Wrote (persistence): persistence/.../$fileName")
        } else {
            println("Skipped (exists): persistence/.../$fileName")
        }
    }

    println("\nDone! GeoLocations module generated at: ${moduleDir.absolutePath}")
}

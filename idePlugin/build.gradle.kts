/*
 * idePlugin Module
 * IntelliJ Platform plugin integration
 * License: Apache 2.0
 */

plugins {
    kotlin("jvm")
}

repositories {
    google()
    mavenCentral()
}

// Configure compilation to target Java 11 regardless of toolchain version
tasks.withType<JavaCompile> {
    options.release.set(11)
}

dependencies {
    implementation(libs.coroutines.core)
    implementation(libs.serialization.json)
    implementation(project(":fbpDsl"))
    implementation(project(":kotlinCompiler"))
    implementation(project(":goCompiler"))

    testImplementation(libs.junit5.all)
    testImplementation(kotlin("test"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}


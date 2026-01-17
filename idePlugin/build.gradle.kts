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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation(project(":fbpDsl"))
    implementation(project(":kotlinCompiler"))
    implementation(project(":goCompiler"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation(kotlin("test"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}


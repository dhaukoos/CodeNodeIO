/*
 * idePlugin Module
 * IntelliJ Platform plugin integration
 * License: Apache 2.0
 */

plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij") version "1.17.3"
}

repositories {
    google()
    mavenCentral()
}

// Configure IntelliJ Platform plugin
intellij {
    version.set("2024.1")
    type.set("IC") // IntelliJ IDEA Community Edition
    // No plugin dependencies needed - we parse .flow.kts files as text using our own DSL
}

// Configure plugin version compatibility
tasks {
    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("") // Empty string means no upper bound - compatible with all future versions
    }
}

// Configure Kotlin JVM target to 17 (compatible with IntelliJ Platform 2024.1)
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}

// Configure Java compilation to target Java 17 for IntelliJ Platform 2024.1
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // Note: Kotlin stdlib and coroutines are provided by IntelliJ Platform
    implementation(libs.serialization.json)
    implementation(libs.kotlinPoet)  // Required for code generation service

    // Dependencies on all project modules
    implementation(project(":fbpDsl"))
    implementation(project(":graphEditor"))
    implementation(project(":circuitSimulator"))
    implementation(project(":kotlinCompiler"))
    implementation(project(":goCompiler"))

    testImplementation(libs.junit5.all)
    testImplementation(kotlin("test"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}


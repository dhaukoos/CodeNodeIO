/*
 * flowGraph-inspect Module
 * Node discovery, definition registry, palette state management, filesystem scanning
 * License: Apache 2.0
 */

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }

    // iOS targets for multiplatform support
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "flowGraphInspect"
            isStatic = true
        }
    }

    // Use default hierarchy template for shared iOS source sets
    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":fbpDsl"))
                implementation(project(":flowGraph-types"))
                implementation(project(":flowGraph-persist"))
                implementation(libs.coroutines.core)
                implementation(libs.serialization.json)
                implementation(kotlin("stdlib"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.coroutines.test)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.coroutines.core)
                implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
                // Feature 086: in-process Kotlin compiler for hot-recompile of CodeNode source files.
                // Apache 2.0 (JetBrains-aligned per Constitution licensing protocol).
                implementation(kotlin("compiler-embeddable"))
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libs.junit5.all)
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

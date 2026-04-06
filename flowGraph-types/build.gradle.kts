/*
 * flowGraph-types Module
 * IP type lifecycle: discovery, registry, repository, file generation, migration
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
            baseName = "flowGraphTypes"
            isStatic = true
        }
    }

    // Use default hierarchy template for shared iOS source sets
    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":fbpDsl"))
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

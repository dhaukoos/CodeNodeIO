/*
 * kotlinCompiler Module
 * Kotlin code generation for KMP targets
 * License: Apache 2.0
 */

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm {
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinPoet)
                implementation(libs.coroutines.core)
                implementation(libs.serialization.json)
                implementation(project(":fbpDsl"))
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


/*
 * flowGraph-generate Module
 * Code generation: all generators, templates, validators, compilation, module save
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

sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":fbpDsl"))
                implementation(project(":flowGraph-types"))
                implementation(project(":flowGraph-persist"))
                implementation(project(":flowGraph-inspect"))
                implementation(libs.kotlinPoet)
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
                implementation(kotlin("compiler-embeddable"))
                implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
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

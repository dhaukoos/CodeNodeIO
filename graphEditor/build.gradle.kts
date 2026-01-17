/*
 * graphEditor Module
 * Visual graph editor using Compose Desktop
 * License: Apache 2.0
 */

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm {
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                implementation(project(":fbpDsl"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter:5.10.1")
            }
        }
    }
}


tasks.withType<Test> {
    useJUnitPlatform()
}


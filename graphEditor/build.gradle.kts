/*
 * graphEditor Module
 * Visual graph editor using Compose Desktop
 * License: Apache 2.0
 */

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvm {
        withJava()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.coroutines.core)
                implementation(project(":fbpDsl"))
                // Compose Multiplatform dependencies (using compose DSL)
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.runtime)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.coroutines.swing)
                // Compose Desktop runtime for current platform
                implementation(compose.desktop.currentOs)
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

compose.desktop {
    application {
        mainClass = "io.codenode.grapheditor.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "CodeNodeIO Graph Editor"
            packageVersion = "1.0.0"
        }
    }
}


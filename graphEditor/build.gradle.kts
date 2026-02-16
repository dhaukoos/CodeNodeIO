/*
 * graphEditor Module
 * Visual graph editor using Compose Desktop
 * License: Apache 2.0
 */

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.coroutines.core)
                implementation(libs.serialization.json)
                implementation(project(":fbpDsl"))
                // Compose Multiplatform dependencies (using compose DSL)
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.materialIconsExtended)
                implementation(compose.runtime)
                // JetBrains Multiplatform ViewModel
                implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
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
                // KotlinCompiler for module generation
                implementation(project(":kotlinCompiler"))
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libs.junit5.all)
                implementation(libs.coroutines.test)
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


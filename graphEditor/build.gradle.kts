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
                implementation(project(":flowGraph-types"))
                implementation(project(":flowGraph-persist"))
                implementation(project(":preview-api"))
                implementation(project(":circuitSimulator"))
                // Koin DI
                implementation("io.insert-koin:koin-core:4.0.0")
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

// When CODENODE_PROJECT_DIR is set, include project module JARs + transitive dependencies
// on the runtime classpath. This allows `:graphEditor:run` to discover and execute project modules.
//
// Setup (one-time after building the project):
//   cd $CODENODE_PROJECT_DIR && ./gradlew jvmJar writeRuntimeClasspath
val projectDir = System.getenv("CODENODE_PROJECT_DIR")
if (projectDir != null) {
    val classpathFile = file("$projectDir/build/grapheditor-runtime-classpath.txt")
    if (classpathFile.exists()) {
        val classpathEntries = classpathFile.readLines().filter { it.isNotBlank() }.map { file(it) }
        kotlin.sourceSets.getByName("jvmMain") {
            dependencies {
                classpathEntries.forEach { jar ->
                    runtimeOnly(files(jar))
                }
            }
        }
    }
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


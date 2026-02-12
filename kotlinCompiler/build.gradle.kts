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

// Task to regenerate StopWatch module files using the latest generator
tasks.register<JavaExec>("regenerateStopWatch") {
    description = "Regenerate StopWatch module generated files using latest ModuleGenerator"
    group = "generation"
    mainClass.set("io.codenode.kotlincompiler.tools.RegenerateStopWatchKt")
    classpath = kotlin.jvm().compilations["main"].runtimeDependencyFiles +
            kotlin.jvm().compilations["main"].output.allOutputs
    workingDir = rootDir
    dependsOn("jvmMainClasses")
}


/*
 * CodeNodeIO IDE Plugin Platform
 * Project structure for Flow-Based Programming visual editor
 * License: Apache 2.0
 */

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        kotlin("jvm") version "2.1.30"
        kotlin("multiplatform") version "2.1.30"
        kotlin("plugin.serialization") version "2.1.30"
        id("org.jetbrains.compose") version "1.11.1"
        id("org.jetbrains.kotlin.plugin.compose") version "2.1.30"
        id("org.jetbrains.kotlin.plugin.parcelize") version "2.1.30"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://www.jetbrains.com/intellij-repository/releases")
        }
        maven {
            url = uri("https://marketplace.jetbrains.com/plugins/downloadable")
        }
    }
}

rootProject.name = "codenode-io"

// Core platform modules
include(":fbpDsl")
include(":graphEditor")
include(":circuitSimulator")
include(":kotlinCompiler")
include(":goCompiler")
// include(":idePlugin")  // Disabled for now due to compatibility issues


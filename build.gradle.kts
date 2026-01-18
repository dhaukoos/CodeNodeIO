/*
 * CodeNodeIO IDE Plugin Platform
 * Root build configuration for Flow-Based Programming visual editor
 * License: Apache 2.0
 */

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// Common version constraints across all modules
allprojects {
    group = "io.codenode"
    version = "0.1.0-SNAPSHOT"
}


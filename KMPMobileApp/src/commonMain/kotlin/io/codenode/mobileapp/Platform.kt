/*
 * Platform abstraction for Kotlin Multiplatform
 * Provides expect/actual pattern for platform-specific implementations
 */
package io.codenode.mobileapp

/**
 * Platform information interface.
 * Each platform provides its own implementation.
 */
interface Platform {
    val name: String
}

/**
 * Expect declaration for platform-specific Platform implementation.
 */
expect fun getPlatform(): Platform

/**
 * Returns a greeting message including the platform name.
 */
fun greet(): String = "Hello from ${getPlatform().name}!"

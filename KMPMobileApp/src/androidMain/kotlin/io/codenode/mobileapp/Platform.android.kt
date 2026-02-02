/*
 * Android platform implementation
 */
package io.codenode.mobileapp

import android.os.Build

/**
 * Android-specific Platform implementation.
 */
class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

/**
 * Actual implementation for Android platform.
 */
actual fun getPlatform(): Platform = AndroidPlatform()

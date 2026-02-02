/*
 * iOS platform implementation
 */
package io.codenode.mobileapp

import platform.UIKit.UIDevice

/**
 * iOS-specific Platform implementation.
 */
class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

/**
 * Actual implementation for iOS platform.
 */
actual fun getPlatform(): Platform = IOSPlatform()

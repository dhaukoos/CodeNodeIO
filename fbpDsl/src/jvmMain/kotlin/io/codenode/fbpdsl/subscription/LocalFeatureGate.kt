/*
 * LocalFeatureGate - Properties-file-backed FeatureGate implementation
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.subscription

import io.codenode.fbpdsl.model.FeatureGate
import io.codenode.fbpdsl.model.SubscriptionTier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Properties

class LocalFeatureGate(
    private val configFile: File = File(System.getProperty("user.home"), ".codenode/config.properties")
) : FeatureGate {

    private val _currentTier = MutableStateFlow(loadTier())
    override val currentTier: StateFlow<SubscriptionTier> = _currentTier.asStateFlow()

    fun setTier(tier: SubscriptionTier) {
        _currentTier.value = tier
        saveTier(tier)
    }

    private fun loadTier(): SubscriptionTier {
        if (!configFile.exists()) return SubscriptionTier.FREE
        return try {
            val props = Properties()
            configFile.inputStream().use { props.load(it) }
            val tierName = props.getProperty("subscription.tier", "FREE").uppercase()
            SubscriptionTier.valueOf(tierName)
        } catch (_: Exception) {
            SubscriptionTier.FREE
        }
    }

    private fun saveTier(tier: SubscriptionTier) {
        try {
            configFile.parentFile?.mkdirs()
            val props = Properties()
            if (configFile.exists()) {
                configFile.inputStream().use { props.load(it) }
            }
            props.setProperty("subscription.tier", tier.name)
            configFile.outputStream().use { props.store(it, "CodeNodeIO configuration") }
        } catch (_: Exception) {
            // Best-effort persistence — tier is already updated in memory
        }
    }
}

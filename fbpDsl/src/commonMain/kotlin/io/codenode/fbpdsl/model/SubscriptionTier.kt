/*
 * SubscriptionTier - Subscription tier model and feature gating
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlinx.coroutines.flow.StateFlow

enum class SubscriptionTier {
    FREE, PRO, SIM;

    fun hasAccess(required: SubscriptionTier): Boolean = this.ordinal >= required.ordinal
}

interface FeatureGate {
    val currentTier: StateFlow<SubscriptionTier>

    fun canGenerate(): Boolean = currentTier.value.hasAccess(SubscriptionTier.PRO)
    fun canSimulate(): Boolean = currentTier.value.hasAccess(SubscriptionTier.SIM)
    fun canUseRepositoryNodes(): Boolean = currentTier.value.hasAccess(SubscriptionTier.PRO)
}

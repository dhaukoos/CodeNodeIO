/*
 * SubscriptionTierTest - Tests for tier ordering and access checks
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals

class SubscriptionTierTest {

    @Test
    fun `FREE has access to FREE only`() {
        assertTrue(SubscriptionTier.FREE.hasAccess(SubscriptionTier.FREE))
        assertFalse(SubscriptionTier.FREE.hasAccess(SubscriptionTier.PRO))
        assertFalse(SubscriptionTier.FREE.hasAccess(SubscriptionTier.SIM))
    }

    @Test
    fun `PRO has access to FREE and PRO`() {
        assertTrue(SubscriptionTier.PRO.hasAccess(SubscriptionTier.FREE))
        assertTrue(SubscriptionTier.PRO.hasAccess(SubscriptionTier.PRO))
        assertFalse(SubscriptionTier.PRO.hasAccess(SubscriptionTier.SIM))
    }

    @Test
    fun `SIM has access to all tiers`() {
        assertTrue(SubscriptionTier.SIM.hasAccess(SubscriptionTier.FREE))
        assertTrue(SubscriptionTier.SIM.hasAccess(SubscriptionTier.PRO))
        assertTrue(SubscriptionTier.SIM.hasAccess(SubscriptionTier.SIM))
    }

    @Test
    fun `ordinal ordering is FREE lt PRO lt SIM`() {
        assertTrue(SubscriptionTier.FREE.ordinal < SubscriptionTier.PRO.ordinal)
        assertTrue(SubscriptionTier.PRO.ordinal < SubscriptionTier.SIM.ordinal)
    }

    @Test
    fun `valueOf parses tier names correctly`() {
        assertEquals(SubscriptionTier.FREE, SubscriptionTier.valueOf("FREE"))
        assertEquals(SubscriptionTier.PRO, SubscriptionTier.valueOf("PRO"))
        assertEquals(SubscriptionTier.SIM, SubscriptionTier.valueOf("SIM"))
    }
}

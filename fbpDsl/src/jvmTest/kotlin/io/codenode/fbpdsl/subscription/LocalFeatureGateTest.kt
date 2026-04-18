/*
 * LocalFeatureGateTest - Tests for properties-file-backed FeatureGate
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.subscription

import io.codenode.fbpdsl.model.SubscriptionTier
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class LocalFeatureGateTest {

    private fun createTempConfig(content: String? = null): File {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "codenode-test-${System.nanoTime()}")
        tempDir.mkdirs()
        val configFile = File(tempDir, "config.properties")
        if (content != null) {
            configFile.writeText(content)
        }
        return configFile
    }

    @Test
    fun `defaults to SIM when config file does not exist`() {
        val configFile = File(System.getProperty("java.io.tmpdir"), "nonexistent-${System.nanoTime()}.properties")
        val gate = LocalFeatureGate(configFile)
        assertEquals(SubscriptionTier.SIM, gate.currentTier.value)
    }

    @Test
    fun `reads PRO tier from config file`() {
        val configFile = createTempConfig("subscription.tier=PRO")
        val gate = LocalFeatureGate(configFile)
        assertEquals(SubscriptionTier.PRO, gate.currentTier.value)
        configFile.parentFile.deleteRecursively()
    }

    @Test
    fun `reads SIM tier from config file`() {
        val configFile = createTempConfig("subscription.tier=SIM")
        val gate = LocalFeatureGate(configFile)
        assertEquals(SubscriptionTier.SIM, gate.currentTier.value)
        configFile.parentFile.deleteRecursively()
    }

    @Test
    fun `defaults to SIM for invalid tier value`() {
        val configFile = createTempConfig("subscription.tier=INVALID")
        val gate = LocalFeatureGate(configFile)
        assertEquals(SubscriptionTier.SIM, gate.currentTier.value)
        configFile.parentFile.deleteRecursively()
    }

    @Test
    fun `is case insensitive for tier names`() {
        val configFile = createTempConfig("subscription.tier=pro")
        val gate = LocalFeatureGate(configFile)
        assertEquals(SubscriptionTier.PRO, gate.currentTier.value)
        configFile.parentFile.deleteRecursively()
    }

    @Test
    fun `setTier updates StateFlow immediately`() {
        val configFile = createTempConfig("subscription.tier=FREE")
        val gate = LocalFeatureGate(configFile)
        assertEquals(SubscriptionTier.FREE, gate.currentTier.value)

        gate.setTier(SubscriptionTier.PRO)
        assertEquals(SubscriptionTier.PRO, gate.currentTier.value)

        gate.setTier(SubscriptionTier.SIM)
        assertEquals(SubscriptionTier.SIM, gate.currentTier.value)
        configFile.parentFile.deleteRecursively()
    }

    @Test
    fun `setTier persists to config file`() {
        val configFile = createTempConfig()
        val gate = LocalFeatureGate(configFile)
        gate.setTier(SubscriptionTier.SIM)

        val gate2 = LocalFeatureGate(configFile)
        assertEquals(SubscriptionTier.SIM, gate2.currentTier.value)
        configFile.parentFile.deleteRecursively()
    }

    @Test
    fun `canGenerate returns correct results per tier`() {
        val configFile = createTempConfig("subscription.tier=FREE")
        val gate = LocalFeatureGate(configFile)

        gate.setTier(SubscriptionTier.FREE)
        assertFalse(gate.canGenerate())

        gate.setTier(SubscriptionTier.PRO)
        assertTrue(gate.canGenerate())

        gate.setTier(SubscriptionTier.SIM)
        assertTrue(gate.canGenerate())
        configFile.parentFile.deleteRecursively()
    }

    @Test
    fun `canSimulate returns correct results per tier`() {
        val configFile = createTempConfig("subscription.tier=FREE")
        val gate = LocalFeatureGate(configFile)

        gate.setTier(SubscriptionTier.FREE)
        assertFalse(gate.canSimulate())

        gate.setTier(SubscriptionTier.PRO)
        assertFalse(gate.canSimulate())

        gate.setTier(SubscriptionTier.SIM)
        assertTrue(gate.canSimulate())
        configFile.parentFile.deleteRecursively()
    }

    @Test
    fun `canUseRepositoryNodes returns correct results per tier`() {
        val configFile = createTempConfig("subscription.tier=FREE")
        val gate = LocalFeatureGate(configFile)

        gate.setTier(SubscriptionTier.FREE)
        assertFalse(gate.canUseRepositoryNodes())

        gate.setTier(SubscriptionTier.PRO)
        assertTrue(gate.canUseRepositoryNodes())

        gate.setTier(SubscriptionTier.SIM)
        assertTrue(gate.canUseRepositoryNodes())
        configFile.parentFile.deleteRecursively()
    }
}

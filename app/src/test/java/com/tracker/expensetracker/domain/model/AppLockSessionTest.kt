package com.tracker.expensetracker.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockSessionTest {
    @Test
    fun privateContentWaitsForSettingsAndColdStartsRequireAuthentication() {
        val cold = AppLockSession()

        assertFalse(cold.canShowContent)
        assertTrue(cold.load(AppLockMode.OFF).canShowContent)
        assertFalse(cold.load(AppLockMode.ONE_MINUTE).canShowContent)
        assertTrue(cold.load(AppLockMode.ONE_MINUTE).afterAuthentication().canShowContent)
    }

    @Test
    fun immediateModeLocksOnBackgroundButNotDuringRotation() {
        val unlocked = AppLockSession().load(AppLockMode.IMMEDIATE).afterAuthentication()

        assertTrue(unlocked.onBackground(100, isConfigurationChange = true).onForeground(200).canShowContent)
        assertFalse(unlocked.onBackground(100, isConfigurationChange = false).canShowContent)
        assertFalse(unlocked.onBackground(100, isConfigurationChange = false).onForeground(101).canShowContent)
    }

    @Test
    fun timedModeLocksAtTheSelectedThresholdAndOnlyCountsTimeInBackground() {
        val unlocked = AppLockSession().load(AppLockMode.THIRTY_SECONDS).afterAuthentication()
        val backgrounded = unlocked.onBackground(100, isConfigurationChange = false)
        val returnedEarly = backgrounded.onForeground(30_099)

        assertTrue(returnedEarly.canShowContent)
        assertTrue(returnedEarly.onForeground(50_000).canShowContent)
        assertFalse(backgrounded.onForeground(30_100).canShowContent)
        assertTrue(returnedEarly.onBackground(70_000, isConfigurationChange = false).onForeground(70_001).canShowContent)
    }

    @Test
    fun modeChangesAfterAuthenticationPreserveForegroundAndBackgroundProtection() {
        val noLock = AppLockSession().load(AppLockMode.OFF)
        val enabled = noLock.changeMode(AppLockMode.IMMEDIATE)
        val enabledWhileAway = noLock.onBackground(500, isConfigurationChange = false).changeMode(AppLockMode.IMMEDIATE)

        assertTrue(enabled.canShowContent)
        assertFalse(enabledWhileAway.onForeground(501).canShowContent)
        assertTrue(enabled.changeMode(AppLockMode.OFF).onBackground(600, false).onForeground(900).canShowContent)
    }
}

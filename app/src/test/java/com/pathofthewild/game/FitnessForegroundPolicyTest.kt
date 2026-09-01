package com.pathofthewild.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FitnessForegroundPolicyTest {
    @Test
    fun stepSensor_requiresForegroundPermissionAndHardware() {
        assertTrue(
            FitnessForegroundPolicy.shouldRegisterStepSensor(
                isForeground = true,
                activityPermissionGranted = true,
                hasStepSensor = true
            )
        )
        assertFalse(FitnessForegroundPolicy.shouldRegisterStepSensor(false, true, true))
        assertFalse(FitnessForegroundPolicy.shouldRegisterStepSensor(true, false, true))
        assertFalse(FitnessForegroundPolicy.shouldRegisterStepSensor(true, true, false))
    }

    @Test
    fun healthRefresh_requiresForegroundAvailableSdkAndClient() {
        assertTrue(
            FitnessForegroundPolicy.shouldRefreshHealth(
                isForeground = true,
                healthSdkAvailable = true,
                healthClientAvailable = true
            )
        )
        assertFalse(FitnessForegroundPolicy.shouldRefreshHealth(false, true, true))
        assertFalse(FitnessForegroundPolicy.shouldRefreshHealth(true, false, true))
        assertFalse(FitnessForegroundPolicy.shouldRefreshHealth(true, true, false))
    }
}

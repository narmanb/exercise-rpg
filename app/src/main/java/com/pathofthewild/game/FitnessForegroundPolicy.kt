package com.pathofthewild.game

/**
 * Keeps foreground-only fitness work explicit and testable.
 * Durable fitness history remains Health Connect's job; the direct sensor is only a live bridge.
 */
internal object FitnessForegroundPolicy {
    fun shouldRegisterStepSensor(
        isForeground: Boolean,
        activityPermissionGranted: Boolean,
        hasStepSensor: Boolean
    ): Boolean = isForeground && activityPermissionGranted && hasStepSensor

    fun shouldRefreshHealth(
        isForeground: Boolean,
        healthSdkAvailable: Boolean,
        healthClientAvailable: Boolean
    ): Boolean = isForeground && healthSdkAvailable && healthClientAvailable
}

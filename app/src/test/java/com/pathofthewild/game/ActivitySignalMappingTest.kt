package com.pathofthewild.game

import com.google.android.gms.location.DetectedActivity
import org.junit.Assert.assertEquals
import org.junit.Test

class ActivitySignalMappingTest {
    @Test
    fun supportedDetectedActivitiesMapToStableGameKinds() {
        assertEquals(ActivitySignalKind.Walking, ActivitySignalMapping.fromDetectedType(DetectedActivity.WALKING))
        assertEquals(ActivitySignalKind.Running, ActivitySignalMapping.fromDetectedType(DetectedActivity.RUNNING))
        assertEquals(ActivitySignalKind.OnFoot, ActivitySignalMapping.fromDetectedType(DetectedActivity.ON_FOOT))
        assertEquals(ActivitySignalKind.InVehicle, ActivitySignalMapping.fromDetectedType(DetectedActivity.IN_VEHICLE))
        assertEquals(ActivitySignalKind.OnBicycle, ActivitySignalMapping.fromDetectedType(DetectedActivity.ON_BICYCLE))
        assertEquals(ActivitySignalKind.Still, ActivitySignalMapping.fromDetectedType(DetectedActivity.STILL))
    }

    @Test
    fun unsupportedDetectedActivitiesRemainUnknown() {
        assertEquals(ActivitySignalKind.Unknown, ActivitySignalMapping.fromDetectedType(DetectedActivity.TILTING))
        assertEquals(ActivitySignalKind.Unknown, ActivitySignalMapping.fromDetectedType(DetectedActivity.UNKNOWN))
    }
}

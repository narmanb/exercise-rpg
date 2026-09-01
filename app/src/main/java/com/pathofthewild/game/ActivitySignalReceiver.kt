package com.pathofthewild.game

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity

internal class ActivitySignalReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || !ActivityRecognitionResult.hasResult(intent)) return
        val result = ActivityRecognitionResult.extractResult(intent) ?: return
        val probable = result.mostProbableActivity ?: return
        val observedAt = result.time.takeIf { it > 0L } ?: System.currentTimeMillis()
        ActivitySignalStore(context.applicationContext).save(
            ActivitySignalRules.normalize(
                kind = ActivitySignalMapping.fromDetectedType(probable.type),
                confidence = probable.confidence,
                observedAtEpochMs = observedAt
            )
        )
    }
}

internal object ActivitySignalMapping {
    fun fromDetectedType(type: Int): ActivitySignalKind = when (type) {
        DetectedActivity.WALKING -> ActivitySignalKind.Walking
        DetectedActivity.RUNNING -> ActivitySignalKind.Running
        DetectedActivity.ON_FOOT -> ActivitySignalKind.OnFoot
        DetectedActivity.IN_VEHICLE -> ActivitySignalKind.InVehicle
        DetectedActivity.ON_BICYCLE -> ActivitySignalKind.OnBicycle
        DetectedActivity.STILL -> ActivitySignalKind.Still
        else -> ActivitySignalKind.Unknown
    }
}

internal object ActivitySignalRegistration {
    const val DETECTION_INTERVAL_MS = 60_000L
    private const val REQUEST_CODE = 7301
    private const val ACTION_ACTIVITY_SAMPLE = "com.pathofthewild.game.ACTIVITY_SAMPLE"

    fun request(
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        runCatching {
            ActivityRecognition.getClient(context.applicationContext)
                .requestActivityUpdates(DETECTION_INTERVAL_MS, pendingIntent(context))
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { error -> onFailure(error) }
        }.onFailure(onFailure)
    }

    fun remove(
        context: Context,
        onComplete: () -> Unit = {}
    ) {
        runCatching {
            ActivityRecognition.getClient(context.applicationContext)
                .removeActivityUpdates(pendingIntent(context))
                .addOnCompleteListener { onComplete() }
        }.onFailure { onComplete() }
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context.applicationContext, ActivitySignalReceiver::class.java)
            .setAction(ACTION_ACTIVITY_SAMPLE)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        return PendingIntent.getBroadcast(context.applicationContext, REQUEST_CODE, intent, flags)
    }
}

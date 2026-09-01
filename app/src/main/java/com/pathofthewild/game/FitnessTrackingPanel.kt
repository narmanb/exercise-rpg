package com.pathofthewild.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.Instant

@Composable
internal fun FitnessTrackingPanel(
    trackingMode: StepTrackingMode,
    healthStatus: String,
    healthPermissionGranted: Boolean,
    healthLastSyncEpochMs: Long?,
    sensorStatus: String,
    activityPermissionGranted: Boolean,
    activitySamplingStatus: String,
    activitySignal: ActivitySignal?,
    onRequestHealth: () -> Unit,
    onRequestActivity: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text("Fitness tracking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TrackingStatusLine("Tracking mode", trackingMode.label)
            TrackingStatusLine("Health Connect", healthStatus)
            TrackingStatusLine("Live step sensor", sensorStatus)
            TrackingStatusLine(
                "Activity Recognition",
                if (activityPermissionGranted) activitySamplingStatus else "Permission needed"
            )

            val signal = activitySignal
            TrackingStatusLine(
                "Recent activity",
                if (signal == null) {
                    "Waiting for sample"
                } else {
                    "${signal.kind.label} · ${signal.confidence}% · ${ActivitySignalRules.age(signal, System.currentTimeMillis()).label}"
                }
            )
            TrackingStatusLine(
                "Last Health sync",
                healthLastSyncEpochMs?.let { Instant.ofEpochMilli(it).toString() } ?: "Not yet"
            )

            if (!healthPermissionGranted || !activityPermissionGranted) {
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!healthPermissionGranted) {
                        OutlinedButton(onClick = onRequestHealth, modifier = Modifier.weight(1f)) {
                            Text("Health access")
                        }
                    }
                    if (!activityPermissionGranted) {
                        OutlinedButton(onClick = onRequestActivity, modifier = Modifier.weight(1f)) {
                            Text("Activity access")
                        }
                    }
                }
            }

            Text(
                "Activity classification is supporting evidence only; it does not currently reject or change step rewards.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TrackingStatusLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.padding(horizontal = 6.dp))
        Text(value, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

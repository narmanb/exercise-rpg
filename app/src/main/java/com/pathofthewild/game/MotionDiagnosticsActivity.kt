package com.pathofthewild.game

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.time.Instant
import java.util.Locale

/** Dedicated shadow-mode diagnostics opened by the tracking notification. */
class MotionDiagnosticsActivity : ComponentActivity() {
    private lateinit var store: MotionTrackingStore
    private lateinit var content: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private val refresher = object : Runnable {
        override fun run() {
            render()
            handler.postDelayed(this, 500L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = MotionTrackingStore(this)
        val scroll = ScrollView(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
        }
        scroll.addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        handler.post(refresher)
    }

    override fun onPause() {
        handler.removeCallbacks(refresher)
        super.onPause()
    }

    private fun render() {
        val snapshot = store.load()
        content.removeAllViews()

        addText("Custom Motion Tracker", 26f, true)
        addText("SHADOW MODE — neither research counter grants RPG rewards.", 16f, true)
        addSpacer()
        addLine("Foreground service", if (snapshot.serviceRunning) "Running" else "Stopped")

        addText("Side-by-side step counts", 20f, true)
        addLine("Path custom confirmed steps", snapshot.confirmedStepCount.toString())
        addLine("Oxford research steps", snapshot.oxfordStepCount.toString())
        addText(
            "Oxford research = orientation-independent acceleration-magnitude Windowed Peak Detection, adapted from the MIT-licensed University of Oxford step-counter project.",
            14f,
            false
        )
        addSpacer(8)

        addText("Path custom detector", 18f, true)
        addLine("Raw peak candidates", snapshot.rawCandidateCount.toString())
        addLine("Rejected candidates — total", snapshot.rejectedCandidateCount.toString())
        addLine("↳ rotational / phone-swing", snapshot.rejectedRotationalCount.toString())
        addLine("↳ weak peak→valley cycle", snapshot.rejectedWeakCycleCount.toString())
        addLine("↳ peak without valid valley", snapshot.rejectedNoValleyCount.toString())
        addLine("↳ too sideways", snapshot.rejectedSidewaysCount.toString())
        addLine("↳ too fast", snapshot.rejectedTooFastCount.toString())
        addLine("Rotation-like candidates", snapshot.suspiciousCandidateCount.toString())
        addLine("Sensor samples processed", snapshot.sampleCount.toString())

        addSpacer(8)
        addText("Oxford research detector", 18f, true)
        addLine("Peak-score candidates", snapshot.oxfordPeakCandidateCount.toString())
        addLine("Scored 100 Hz samples", snapshot.oxfordScoreSampleCount.toString())
        addLine("Adaptive score mean", String.format(Locale.US, "%.3f", snapshot.oxfordDetectorMean))
        addLine("Adaptive score std dev", String.format(Locale.US, "%.3f", snapshot.oxfordDetectorStd))

        addSpacer(8)
        addText("Last Path custom candidate", 18f, true)
        addLine("Peak→valley amplitude", String.format(Locale.US, "%.2f m/s²", snapshot.lastCycleAmplitude))
        addLine("Peak cycle jerk", String.format(Locale.US, "%.1f m/s³", snapshot.lastCycleJerk))
        addLine("Peak cycle gyro", String.format(Locale.US, "%.2f rad/s", snapshot.lastCycleGyro))
        addLine("Vertical-motion fraction", String.format(Locale.US, "%.2f", snapshot.lastVerticalFraction))
        addLine("Interval from previous plausible candidate", if (snapshot.lastCandidateIntervalMs > 0L) "${snapshot.lastCandidateIntervalMs} ms" else "—")
        addSpacer(8)
        addText("Adaptive Path walking model", 18f, true)
        addLine("Accepted cycle amplitude mean", String.format(Locale.US, "%.2f m/s²", snapshot.acceptedAmplitudeMean))
        addLine(
            "Accepted cadence mean",
            if (snapshot.acceptedIntervalMeanNs > 0L) "${snapshot.acceptedIntervalMeanNs / 1_000_000L} ms" else "Learning"
        )
        addLine("Sensor stack", snapshot.sensorSummary)
        addLine(
            "Last motion update",
            if (snapshot.lastEventEpochMs > 0L) Instant.ofEpochMilli(snapshot.lastEventEpochMs).toString() else "None yet"
        )
        addSpacer()
        addText(
            "Clean comparison test: tap Reset shadow test. Walk exactly 20 normal steps with the phone at your side and screenshot. Then stand still and swing that arm back/forth 20 times and screenshot. Then turn the screen off, walk exactly 20 more normal steps with the phone at your side, turn it back on, and screenshot again. We care most about how each of the two step counts changes between screenshots.",
            16f,
            false
        )
        addSpacer()

        Button(this).apply {
            text = "Reset shadow test"
            setOnClickListener {
                MotionTrackingService.reset(this@MotionDiagnosticsActivity)
                render()
            }
            content.addView(this, matchWidth())
        }
        Button(this).apply {
            text = "Stop tracking"
            setOnClickListener {
                MotionTrackingService.stop(this@MotionDiagnosticsActivity)
                render()
            }
            content.addView(this, matchWidth())
        }
        Button(this).apply {
            text = "Open game"
            setOnClickListener { startActivity(Intent(this@MotionDiagnosticsActivity, MainActivity::class.java)) }
            content.addView(this, matchWidth())
        }
    }

    private fun addLine(label: String, value: String) {
        addText("$label\n$value", 18f, false)
        addSpacer(6)
    }

    private fun addText(text: String, sizeSp: Float, bold: Boolean) {
        content.addView(TextView(this).apply {
            this.text = text
            textSize = sizeSp
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.START
        }, matchWidth())
    }

    private fun addSpacer(heightDp: Int = 14) {
        content.addView(TextView(this), LinearLayout.LayoutParams(1, dp(heightDp)))
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(8) }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

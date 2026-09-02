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
        addText("SHADOW MODE — these steps are diagnostic only and do not grant RPG rewards yet.", 16f, true)
        addSpacer()
        addLine("Foreground service", if (snapshot.serviceRunning) "Running" else "Stopped")
        addLine("Custom confirmed steps", snapshot.confirmedStepCount.toString())
        addLine("Raw motion candidates", snapshot.rawCandidateCount.toString())
        addLine("Rejected candidates", snapshot.rejectedCandidateCount.toString())
        addLine("Suspicious candidates", snapshot.suspiciousCandidateCount.toString())
        addLine("Sensor samples processed", snapshot.sampleCount.toString())
        addLine("Last vertical-motion fraction", String.format(Locale.US, "%.2f", snapshot.lastVerticalFraction))
        addLine("Sensor stack", snapshot.sensorSummary)
        addLine(
            "Last motion update",
            if (snapshot.lastEventEpochMs > 0L) Instant.ofEpochMilli(snapshot.lastEventEpochMs).toString() else "None yet"
        )
        addSpacer()
        addText("Test 1: walk 20 normal steps. Test 2: stand still and swing the phone back and forth about 20 times. Screenshot this screen after each test.", 16f, false)
        addSpacer()

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

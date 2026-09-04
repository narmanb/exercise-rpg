package com.pathofthewild.game

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Foreground fitness service that keeps the custom motion pedometer alive while the game is not
 * visible. The notification has an explicit Stop action; when Android allows notification dismissal,
 * its delete intent stops tracking as well.
 */
class MotionTrackingService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var store: MotionTrackingStore
    private lateinit var oxfordCounter: OxfordShadowStepCounter
    private var accelerometer: Sensor? = null
    private var gravitySensor: Sensor? = null
    private var gyroscope: Sensor? = null

    private var state = MotionPedometerState()
    private var gravityX = 0f
    private var gravityY = 0f
    private var gravityZ = SensorManager.GRAVITY_EARTH
    private var gravityTimestampNs = 0L
    private var fallbackGravityX = 0f
    private var fallbackGravityY = 0f
    private var fallbackGravityZ = SensorManager.GRAVITY_EARTH
    private var latestGyroMagnitude = 0f
    private var gyroTimestampNs = 0L
    private var lastPersistTimestampNs = 0L
    private var lastCandidateCount = 0L

    override fun onCreate() {
        super.onCreate()
        store = MotionTrackingStore(this)
        val epoch = store.currentCharacterEpoch()
        if (epoch <= 0L) {
            stopSelf()
            return
        }
        store.ensureCharacter(epoch)
        state = store.initialPedometerState()
        val oxfordStart = store.initialOxfordSnapshot()
        oxfordCounter = OxfordShadowStepCounter(
            initialStepCount = oxfordStart.stepCount,
            initialPeakCandidateCount = oxfordStart.peakCandidateCount,
            initialScoreSampleCount = oxfordStart.scoreSampleCount
        )
        lastCandidateCount = state.rawCandidateCount

        createNotificationChannel()
        promoteToForeground()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        val summary = buildString {
            append(if (accelerometer != null) "accelerometer" else "NO accelerometer")
            append(" · ")
            append(if (gravitySensor != null) "gravity" else "gravity fallback")
            append(" · ")
            append(if (gyroscope != null) "gyro" else "no gyro")
            append(" · Oxford WPD shadow")
        }
        store.setServiceState(running = true, sensorSummary = summary)
        registerSensors()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopTracking()
                return START_NOT_STICKY
            }
            ACTION_RESET -> {
                resetShadowTracking()
                return START_STICKY
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (::sensorManager.isInitialized) sensorManager.unregisterListener(this)
        if (::store.isInitialized) {
            if (::oxfordCounter.isInitialized) {
                store.savePedometerState(state, oxfordCounter.snapshot(), System.currentTimeMillis())
            }
            store.setServiceState(running = false)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_GRAVITY -> {
                gravityX = event.values[0]
                gravityY = event.values[1]
                gravityZ = event.values[2]
                gravityTimestampNs = event.timestamp
            }
            Sensor.TYPE_GYROSCOPE -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                latestGyroMagnitude = sqrt(x * x + y * y + z * z)
                gyroTimestampNs = event.timestamp
            }
            Sensor.TYPE_ACCELEROMETER -> processAccelerometer(event)
        }
    }

    private fun processAccelerometer(event: SensorEvent) {
        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]

        // Research comparator: same raw accelerometer event, independent algorithm. It never feeds
        // the reward ledger; its only purpose is target-device comparison in diagnostics.
        val oxfordResult = oxfordCounter.observe(event.timestamp, ax, ay, az)

        val gravityAlpha = 0.02f
        fallbackGravityX += gravityAlpha * (ax - fallbackGravityX)
        fallbackGravityY += gravityAlpha * (ay - fallbackGravityY)
        fallbackGravityZ += gravityAlpha * (az - fallbackGravityZ)

        val gravityFresh = gravityTimestampNs > 0L && event.timestamp - gravityTimestampNs in 0L..500_000_000L
        val gx = if (gravityFresh) gravityX else fallbackGravityX
        val gy = if (gravityFresh) gravityY else fallbackGravityY
        val gz = if (gravityFresh) gravityZ else fallbackGravityZ
        val gravityMagnitude = sqrt(gx * gx + gy * gy + gz * gz).coerceAtLeast(0.1f)
        val ux = gx / gravityMagnitude
        val uy = gy / gravityMagnitude
        val uz = gz / gravityMagnitude

        val dx = ax - gx
        val dy = ay - gy
        val dz = az - gz
        val vertical = dx * ux + dy * uy + dz * uz
        val dynamicSquared = dx * dx + dy * dy + dz * dz
        val horizontal = sqrt(max(0f, dynamicSquared - vertical * vertical))
        val gyroFresh = gyroTimestampNs > 0L && event.timestamp - gyroTimestampNs in 0L..500_000_000L
        val gyro = if (gyroFresh) latestGyroMagnitude else 0f

        val result = MotionPedometer.observe(
            state = state,
            timestampNs = event.timestamp,
            verticalAcceleration = vertical,
            horizontalAcceleration = horizontal,
            gyroMagnitude = gyro
        )
        state = result.state

        val candidateChanged = state.rawCandidateCount != lastCandidateCount
        val periodicPersist = lastPersistTimestampNs == 0L || event.timestamp - lastPersistTimestampNs >= 2_000_000_000L
        if (
            candidateChanged ||
            result.newlyConfirmedSteps > 0L ||
            result.rejection != null ||
            oxfordResult.newlyConfirmedSteps > 0L ||
            periodicPersist
        ) {
            store.savePedometerState(state, oxfordCounter.snapshot(), System.currentTimeMillis())
            lastPersistTimestampNs = event.timestamp
            lastCandidateCount = state.rawCandidateCount
        }
    }

    private fun registerSensors() {
        val accel = accelerometer
        if (accel == null) {
            store.setServiceState(running = false, sensorSummary = "No accelerometer — custom tracking unavailable")
            stopTracking()
            return
        }
        // 25 Hz resolves normal walking cadence while using substantially less power than game-rate sampling.
        // The Oxford comparator internally interpolates this same stream to its original 100 Hz processing grid.
        val samplingUs = 40_000
        sensorManager.registerListener(this, accel, samplingUs)
        gravitySensor?.let { sensorManager.registerListener(this, it, samplingUs) }
        gyroscope?.let { sensorManager.registerListener(this, it, samplingUs) }
    }

    private fun promoteToForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val detailsIntent = Intent(this, MotionDiagnosticsActivity::class.java)
        val detailsPending = PendingIntent.getActivity(
            this,
            10,
            detailsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = Intent(this, MotionTrackingService::class.java).setAction(ACTION_STOP)
        val stopPending = PendingIntent.getService(
            this,
            11,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setContentTitle("Path of the Wild activity tracking")
            .setContentText("Custom motion tracking active · tap for details")
            .setContentIntent(detailsPending)
            .setDeleteIntent(stopPending)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOnlyAlertOnce(true)
            .setOngoing(false)
            .addAction(Notification.Action.Builder(null, "Stop tracking", stopPending).build())
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Activity tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Path of the Wild motion tracking active while the game is closed."
                setShowBadge(false)
            }
        )
    }

    private fun resetShadowTracking() {
        state = MotionPedometerState()
        oxfordCounter = OxfordShadowStepCounter()
        lastCandidateCount = 0L
        lastPersistTimestampNs = 0L
        if (::store.isInitialized) {
            store.resetShadowCounters()
            store.setServiceState(running = true)
            store.savePedometerState(state, oxfordCounter.snapshot(), System.currentTimeMillis())
        }
    }

    private fun stopTracking() {
        if (::sensorManager.isInitialized) sensorManager.unregisterListener(this)
        if (::store.isInitialized) {
            if (::oxfordCounter.isInitialized) {
                store.savePedometerState(state, oxfordCounter.snapshot(), System.currentTimeMillis())
            }
            store.setServiceState(running = false)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_REMOVE)
        else @Suppress("DEPRECATION") stopForeground(true)
        stopSelf()
    }

    companion object {
        const val ACTION_STOP = "com.pathofthewild.game.STOP_MOTION_TRACKING"
        const val ACTION_RESET = "com.pathofthewild.game.RESET_MOTION_TRACKING"
        private const val CHANNEL_ID = "path_of_the_wild_motion_tracking"
        private const val NOTIFICATION_ID = 4107

        fun start(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED
            ) return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return false

            return runCatching {
                val intent = Intent(context, MotionTrackingService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
                else context.startService(intent)
                true
            }.getOrDefault(false)
        }

        fun reset(context: Context) {
            val intent = Intent(context, MotionTrackingService::class.java).setAction(ACTION_RESET)
            runCatching { context.startService(intent) }
                .onFailure { MotionTrackingStore(context).resetShadowCounters() }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MotionTrackingService::class.java))
        }
    }
}

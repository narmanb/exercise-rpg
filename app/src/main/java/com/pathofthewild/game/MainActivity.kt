package com.pathofthewild.game

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.ext.SdkExtensions
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PathOfTheWildTheme {
                PathOfTheWildApp()
            }
        }
    }
}

class HealthRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PathOfTheWildTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Health Connect in Path of the Wild", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Text("Step access is used only to turn activity after character creation into Adventure Points and modest walking XP. The game does not need unrelated health records.")
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { finish() }) { Text("Return") }
                    }
                }
            }
        }
    }
}

private enum class Destination(val label: String, val short: String) {
    Home("Home", "Home"),
    Adventure("Adventure", "Map"),
    Calories("Calories", "Food"),
    Diagnostics("Diagnostics", "Debug")
}

private data class CharacterProfile(
    val name: String,
    val createdAtEpochMs: Long,
    val healthBaselineToday: Long?,
    val sensorBaseline: Float?
)

private data class FoodEntry(val name: String, val calories: Int)

private class GameStore(context: Context) {
    private val prefs = context.getSharedPreferences("path_of_the_wild_save", Context.MODE_PRIVATE)

    fun loadProfile(): CharacterProfile? {
        val name = prefs.getString("character_name", null) ?: return null
        return CharacterProfile(
            name = name,
            createdAtEpochMs = prefs.getLong("character_created", 0L),
            healthBaselineToday = if (prefs.contains("health_baseline")) prefs.getLong("health_baseline", 0L) else null,
            sensorBaseline = if (prefs.contains("sensor_baseline")) prefs.getFloat("sensor_baseline", 0f) else null
        )
    }

    fun createProfile(name: String, healthBaseline: Long?, sensorBaseline: Float?): CharacterProfile {
        val created = System.currentTimeMillis()
        prefs.edit()
            .putString("character_name", name.trim())
            .putLong("character_created", created)
            .apply {
                if (healthBaseline != null) putLong("health_baseline", healthBaseline)
                if (sensorBaseline != null) putFloat("sensor_baseline", sensorBaseline)
            }
            .putStringSet("unlocked_tiles", setOf("2,2"))
            .putInt("player_x", 2)
            .putInt("player_y", 2)
            .putInt("adventure_spent", 0)
            .apply()
        return loadProfile()!!
    }

    fun setHealthBaseline(value: Long) {
        prefs.edit().putLong("health_baseline", value).apply()
    }

    fun setSensorBaseline(value: Float) {
        prefs.edit().putFloat("sensor_baseline", value).apply()
    }

    fun unlockedTiles(): Set<String> = prefs.getStringSet("unlocked_tiles", setOf("2,2"))?.toSet() ?: setOf("2,2")

    fun unlockTile(x: Int, y: Int) {
        val updated = unlockedTiles().toMutableSet().apply { add("$x,$y") }
        prefs.edit().putStringSet("unlocked_tiles", updated).apply()
    }

    fun playerPosition(): Pair<Int, Int> = prefs.getInt("player_x", 2) to prefs.getInt("player_y", 2)

    fun setPlayerPosition(x: Int, y: Int) {
        prefs.edit().putInt("player_x", x).putInt("player_y", y).apply()
    }

    fun adventureSpent(): Int = prefs.getInt("adventure_spent", 0)

    fun spendAdventurePoint() {
        prefs.edit().putInt("adventure_spent", adventureSpent() + 1).apply()
    }

    fun monsterDefeated(): Boolean = prefs.getBoolean("wildling_defeated", false)

    fun setMonsterDefeated() {
        prefs.edit().putBoolean("wildling_defeated", true).apply()
    }

    private fun todayKey(): String = LocalDate.now().toString()

    fun calorieTarget(): Int = prefs.getInt("calorie_target", 2400)

    fun setCalorieTarget(target: Int) {
        prefs.edit().putInt("calorie_target", target.coerceIn(500, 10000)).apply()
    }

    fun foodEntriesToday(): List<FoodEntry> {
        val raw = prefs.getString("food_${todayKey()}", "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                repeat(array.length()) { index ->
                    val obj = array.getJSONObject(index)
                    add(FoodEntry(obj.getString("name"), obj.getInt("calories")))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun addFood(entry: FoodEntry) {
        val entries = foodEntriesToday() + entry
        val array = JSONArray()
        entries.forEach {
            array.put(JSONObject().put("name", it.name).put("calories", it.calories))
        }
        prefs.edit().putString("food_${todayKey()}", array.toString()).apply()
    }
}

@Composable
private fun PathOfTheWildTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = Color(0xFFB7C9A3),
        onPrimary = Color(0xFF1C2A1C),
        secondary = Color(0xFFA8B7C8),
        background = Color(0xFF10151B),
        surface = Color(0xFF182029),
        surfaceVariant = Color(0xFF232D38),
        onSurface = Color(0xFFE9EEF3),
        onSurfaceVariant = Color(0xFFC0CBD5)
    )
    MaterialTheme(colorScheme = colors, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PathOfTheWildApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { GameStore(context) }
    val scope = rememberCoroutineScope()

    var profile by remember { mutableStateOf(store.loadProfile()) }
    var destination by remember { mutableStateOf(Destination.Home) }

    val readStepsPermission = remember { HealthPermission.getReadPermission(StepsRecord::class) }
    var healthSdkStatus by remember { mutableIntStateOf(HealthConnectClient.getSdkStatus(context)) }
    var healthClient by remember { mutableStateOf<HealthConnectClient?>(null) }
    var healthPermissionGranted by remember { mutableStateOf(false) }
    var healthTodaySteps by remember { mutableLongStateOf(0L) }
    var healthCharacterSteps by remember { mutableLongStateOf(0L) }
    var healthLoaded by remember { mutableStateOf(false) }
    var healthError by remember { mutableStateOf<String?>(null) }

    var activityPermissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        )
    }
    var rawSensorSteps by remember { mutableFloatStateOf(-1f) }
    var hasStepSensor by remember { mutableStateOf(false) }

    suspend fun refreshHealth() {
        val client = healthClient ?: return
        try {
            healthPermissionGranted = readStepsPermission in client.permissionController.getGrantedPermissions()
            if (!healthPermissionGranted) {
                healthLoaded = false
                return
            }
            val now = Instant.now()
            val zone = ZoneId.systemDefault()
            val startToday = LocalDate.now().atStartOfDay(zone).toInstant()
            healthTodaySteps = client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startToday, now)
                )
            )[StepsRecord.COUNT_TOTAL] ?: 0L

            val p = profile
            healthCharacterSteps = if (p != null) {
                val created = Instant.ofEpochMilli(p.createdAtEpochMs)
                client.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(created, now)
                    )
                )[StepsRecord.COUNT_TOTAL] ?: 0L
            } else {
                0L
            }
            healthLoaded = true
            healthError = null
        } catch (t: Throwable) {
            healthError = t.message ?: t::class.java.simpleName
            healthLoaded = false
        }
    }

    val healthPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        healthPermissionGranted = readStepsPermission in granted
        scope.launch { refreshHealth() }
    }

    val activityPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> activityPermissionGranted = granted }

    LaunchedEffect(Unit) {
        healthSdkStatus = HealthConnectClient.getSdkStatus(context)
        if (healthSdkStatus == HealthConnectClient.SDK_AVAILABLE) {
            healthClient = HealthConnectClient.getOrCreate(context)
            refreshHealth()
        }
    }

    LaunchedEffect(profile?.createdAtEpochMs, healthPermissionGranted) {
        if (healthPermissionGranted) refreshHealth()
    }

    DisposableEffect(activityPermissionGranted) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        hasStepSensor = stepSensor != null
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                rawSensorSteps = event.values.firstOrNull() ?: -1f
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (activityPermissionGranted && stepSensor != null) {
            sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    LaunchedEffect(profile?.createdAtEpochMs, healthLoaded, healthTodaySteps) {
        val p = profile ?: return@LaunchedEffect
        if (p.healthBaselineToday == null && healthLoaded) {
            store.setHealthBaseline(healthTodaySteps)
            profile = store.loadProfile()
        }
    }

    LaunchedEffect(profile?.createdAtEpochMs, rawSensorSteps) {
        val p = profile ?: return@LaunchedEffect
        if (p.sensorBaseline == null && rawSensorSteps >= 0f) {
            store.setSensorBaseline(rawSensorSteps)
            profile = store.loadProfile()
        }
    }

    val sensorDelta = profile?.sensorBaseline?.let { baseline ->
        if (rawSensorSteps >= baseline) (rawSensorSteps - baseline).toLong() else 0L
    } ?: 0L
    val eligibleSteps = max(healthCharacterSteps, sensorDelta)
    val walkingXp = eligibleSteps / 100L
    val adventureEarned = eligibleSteps / 500L
    val adventureAvailable = max(0L, 4L + adventureEarned - store.adventureSpent().toLong())

    if (profile == null) {
        CharacterCreationScreen(
            healthStatus = healthStatusLabel(healthSdkStatus, healthPermissionGranted),
            sensorStatus = sensorStatusLabel(hasStepSensor, activityPermissionGranted),
            onRequestHealth = {
                if (healthSdkStatus == HealthConnectClient.SDK_AVAILABLE) {
                    healthPermissionLauncher.launch(setOf(readStepsPermission))
                }
            },
            onRequestActivity = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                }
            },
            onCreate = { name ->
                profile = store.createProfile(
                    name = name,
                    healthBaseline = if (healthLoaded) healthTodaySteps else null,
                    sensorBaseline = rawSensorSteps.takeIf { it >= 0f }
                )
                scope.launch { refreshHealth() }
            }
        )
        return
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val wide = maxWidth >= 600.dp
        if (wide) {
            Row(Modifier.fillMaxSize()) {
                NavigationRail(Modifier.fillMaxHeight()) {
                    Spacer(Modifier.height(12.dp))
                    Destination.entries.forEach { item ->
                        NavigationRailItem(
                            selected = destination == item,
                            onClick = { destination = item },
                            icon = { Text(item.short.take(1), fontWeight = FontWeight.Black) },
                            label = { Text(item.label) }
                        )
                    }
                }
                Scaffold(
                    modifier = Modifier.weight(1f),
                    topBar = { TopAppBar(title = { Text("Path of the Wild") }) }
                ) { padding ->
                    DestinationContent(
                        destination = destination,
                        modifier = Modifier.padding(padding),
                        profile = profile!!,
                        eligibleSteps = eligibleSteps,
                        walkingXp = walkingXp,
                        adventureAvailable = adventureAvailable,
                        healthSdkStatus = healthSdkStatus,
                        healthPermissionGranted = healthPermissionGranted,
                        healthTodaySteps = healthTodaySteps,
                        healthCharacterSteps = healthCharacterSteps,
                        healthError = healthError,
                        hasStepSensor = hasStepSensor,
                        activityPermissionGranted = activityPermissionGranted,
                        rawSensorSteps = rawSensorSteps,
                        sensorDelta = sensorDelta,
                        store = store,
                        onRequestHealth = {
                            if (healthSdkStatus == HealthConnectClient.SDK_AVAILABLE) {
                                healthPermissionLauncher.launch(setOf(readStepsPermission))
                            }
                        },
                        onRequestActivity = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                            }
                        },
                        onRefreshHealth = { scope.launch { refreshHealth() } }
                    )
                }
            }
        } else {
            Scaffold(
                topBar = { TopAppBar(title = { Text("Path of the Wild") }) },
                bottomBar = {
                    NavigationBar {
                        Destination.entries.forEach { item ->
                            NavigationBarItem(
                                selected = destination == item,
                                onClick = { destination = item },
                                icon = { Text(item.short.take(1), fontWeight = FontWeight.Black) },
                                label = { Text(item.short) }
                            )
                        }
                    }
                }
            ) { padding ->
                DestinationContent(
                    destination = destination,
                    modifier = Modifier.padding(padding),
                    profile = profile!!,
                    eligibleSteps = eligibleSteps,
                    walkingXp = walkingXp,
                    adventureAvailable = adventureAvailable,
                    healthSdkStatus = healthSdkStatus,
                    healthPermissionGranted = healthPermissionGranted,
                    healthTodaySteps = healthTodaySteps,
                    healthCharacterSteps = healthCharacterSteps,
                    healthError = healthError,
                    hasStepSensor = hasStepSensor,
                    activityPermissionGranted = activityPermissionGranted,
                    rawSensorSteps = rawSensorSteps,
                    sensorDelta = sensorDelta,
                    store = store,
                    onRequestHealth = {
                        if (healthSdkStatus == HealthConnectClient.SDK_AVAILABLE) {
                            healthPermissionLauncher.launch(setOf(readStepsPermission))
                        }
                    },
                    onRequestActivity = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        }
                    },
                    onRefreshHealth = { scope.launch { refreshHealth() } }
                )
            }
        }
    }
}

@Composable
private fun CharacterCreationScreen(
    healthStatus: String,
    sensorStatus: String,
    onRequestHealth: () -> Unit,
    onRequestActivity: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    Surface(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(Modifier.height(28.dp)) }
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    HumanoidGlyph(Modifier.size(110.dp))
                    Text("Path of the Wild", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Create your Adventurer", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Card(Modifier.widthIn(max = 620.dp).fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { if (it.length <= 24) name = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Adventurer name") },
                            singleLine = true
                        )
                        Text("Fitness rewards begin when this character is created. Older Health Connect history does not count.")
                        HorizontalDivider()
                        StatusLine("Health Connect", healthStatus)
                        StatusLine("Device step sensor", sensorStatus)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onRequestHealth) { Text("Health Connect") }
                            OutlinedButton(onClick = onRequestActivity) { Text("Step access") }
                        }
                        Button(
                            onClick = { onCreate(name.ifBlank { "Adventurer" }) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Begin Journey") }
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun DestinationContent(
    destination: Destination,
    modifier: Modifier,
    profile: CharacterProfile,
    eligibleSteps: Long,
    walkingXp: Long,
    adventureAvailable: Long,
    healthSdkStatus: Int,
    healthPermissionGranted: Boolean,
    healthTodaySteps: Long,
    healthCharacterSteps: Long,
    healthError: String?,
    hasStepSensor: Boolean,
    activityPermissionGranted: Boolean,
    rawSensorSteps: Float,
    sensorDelta: Long,
    store: GameStore,
    onRequestHealth: () -> Unit,
    onRequestActivity: () -> Unit,
    onRefreshHealth: () -> Unit
) {
    when (destination) {
        Destination.Home -> HomeScreen(modifier, profile, eligibleSteps, walkingXp, adventureAvailable, store)
        Destination.Adventure -> AdventureScreen(modifier, adventureAvailable, store)
        Destination.Calories -> CaloriesScreen(modifier, store)
        Destination.Diagnostics -> DiagnosticsScreen(
            modifier = modifier,
            healthSdkStatus = healthSdkStatus,
            healthPermissionGranted = healthPermissionGranted,
            healthTodaySteps = healthTodaySteps,
            healthCharacterSteps = healthCharacterSteps,
            healthError = healthError,
            hasStepSensor = hasStepSensor,
            activityPermissionGranted = activityPermissionGranted,
            rawSensorSteps = rawSensorSteps,
            sensorDelta = sensorDelta,
            eligibleSteps = eligibleSteps,
            profile = profile,
            onRequestHealth = onRequestHealth,
            onRequestActivity = onRequestActivity,
            onRefreshHealth = onRefreshHealth
        )
    }
}

@Composable
private fun ScreenColumn(modifier: Modifier = Modifier, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
private fun HomeScreen(
    modifier: Modifier,
    profile: CharacterProfile,
    eligibleSteps: Long,
    walkingXp: Long,
    adventureAvailable: Long,
    store: GameStore
) {
    val calories = store.foodEntriesToday().sumOf { it.calories }
    val target = store.calorieTarget()
    ScreenColumn(modifier) {
        item {
            ResponsiveCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    HumanoidGlyph(Modifier.size(74.dp))
                    Column(Modifier.weight(1f)) {
                        Text(profile.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Adventurer · Lv 1", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Walking XP $walkingXp", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        item {
            MetricPair(
                leftTitle = "Eligible steps",
                leftValue = eligibleSteps.toString(),
                rightTitle = "Adventure Points",
                rightValue = adventureAvailable.toString()
            )
        }
        item {
            ResponsiveCard {
                Text("Walking rewards", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Prototype balance: 1 Adventure Point per 500 eligible steps and 1 XP per 100 eligible steps. Values will be tuned later.")
            }
        }
        item {
            ResponsiveCard {
                Text("Today's food", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("$calories / $target calories")
                Text(
                    if (calories <= target) "Within current target" else "${calories - target} above current target",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            ResponsiveCard {
                Text("Current milestone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Explore the nearby wild tiles. Unlocked tiles stay unlocked and can be revisited for free.")
            }
        }
    }
}

@Composable
private fun MetricPair(leftTitle: String, leftValue: String, rightTitle: String, rightValue: String) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 520.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(leftTitle, leftValue, Modifier.weight(1f))
                MetricCard(rightTitle, rightValue, Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(leftTitle, leftValue, Modifier.fillMaxWidth())
                MetricCard(rightTitle, rightValue, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ResponsiveCard(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Card(Modifier.widthIn(max = 900.dp).fillMaxWidth()) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
private fun AdventureScreen(modifier: Modifier, adventureAvailable: Long, store: GameStore) {
    var unlocked by remember { mutableStateOf(store.unlockedTiles()) }
    var position by remember { mutableStateOf(store.playerPosition()) }
    var remainingAdventure by remember(adventureAvailable) { mutableLongStateOf(adventureAvailable) }
    var showEncounter by remember { mutableStateOf(false) }
    var wildHp by remember { mutableIntStateOf(90) }
    var defeated by remember { mutableStateOf(store.monsterDefeated()) }

    val effectiveAvailable = remainingAdventure
    val monsterTile = 3 to 2

    fun tryMove(x: Int, y: Int) {
        if (x !in 0..4 || y !in 0..4) return
        val distance = kotlin.math.abs(x - position.first) + kotlin.math.abs(y - position.second)
        if (distance != 1) return
        val key = "$x,$y"
        if (key !in unlocked) {
            if (effectiveAvailable <= 0L) return
            store.unlockTile(x, y)
            store.spendAdventurePoint()
            unlocked = store.unlockedTiles()
            remainingAdventure = (remainingAdventure - 1L).coerceAtLeast(0L)
        }
        store.setPlayerPosition(x, y)
        position = x to y
        if (!defeated && position == monsterTile) showEncounter = true
    }

    ScreenColumn(modifier) {
        item {
            ResponsiveCard {
                Text("The Nearby Wilds", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Adventure Points available: $effectiveAvailable")
                Text("Tap an adjacent tile. Revealing a new tile costs 1 point; moving through an unlocked tile is free.")
            }
        }
        item {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(Modifier.widthIn(max = 560.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(5) { y ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(5) { x ->
                                val key = "$x,$y"
                                val isUnlocked = key in unlocked
                                val isPlayer = position == (x to y)
                                val isMonster = !defeated && monsterTile == (x to y) && isUnlocked
                                val isTown = x == 1 && y == 1 && isUnlocked
                                val isCave = x == 4 && y == 4 && isUnlocked
                                MapTile(
                                    modifier = Modifier.weight(1f).aspectRatio(1f),
                                    unlocked = isUnlocked,
                                    player = isPlayer,
                                    monster = isMonster,
                                    town = isTown,
                                    cave = isCave,
                                    onClick = { tryMove(x, y) }
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            ResponsiveCard {
                Text("Map prototype", fontWeight = FontWeight.Bold)
                Text("Person silhouette = your Adventurer · horned marker = wild encounter · T = town · C = cave")
                Text("Towns/caves will use separate free-movement maps rather than spending Adventure Points inside them.")
            }
        }
    }

    if (showEncounter) {
        AlertDialog(
            onDismissRequest = { showEncounter = false },
            title = { Text("Wildling Encounter") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MonsterGlyph(Modifier.size(130.dp))
                    Text("Wildling HP $wildHp / 90", fontWeight = FontWeight.Bold)
                    Text("Temporary combat placeholder. Full party combat, turn forecast, guarding, techniques, and capture come later.")
                }
            },
            confirmButton = {
                Button(onClick = {
                    wildHp = (wildHp - 30).coerceAtLeast(0)
                    if (wildHp == 0) {
                        store.setMonsterDefeated()
                        defeated = true
                        showEncounter = false
                    }
                }) { Text("Attack") }
            },
            dismissButton = { TextButton(onClick = { showEncounter = false }) { Text("Retreat") } }
        )
    }
}

@Composable
private fun MapTile(
    modifier: Modifier,
    unlocked: Boolean,
    player: Boolean,
    monster: Boolean,
    town: Boolean,
    cave: Boolean,
    onClick: () -> Unit
) {
    val background = if (unlocked) Color(0xFF394E37) else Color(0xFF242A30)
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = background,
        tonalElevation = if (unlocked) 2.dp else 0.dp
    ) {
        Box(Modifier.fillMaxSize().padding(4.dp), contentAlignment = Alignment.Center) {
            when {
                player -> HumanoidGlyph(Modifier.fillMaxSize().padding(6.dp))
                monster -> MonsterGlyph(Modifier.fillMaxSize().padding(7.dp))
                town -> Text("T", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
                cave -> Text("C", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
                unlocked -> Text("·", color = Color(0xFFB6CDA9), style = MaterialTheme.typography.headlineMedium)
                else -> Text("?", color = Color(0xFF79828A), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CaloriesScreen(modifier: Modifier, store: GameStore) {
    var entries by remember { mutableStateOf(store.foodEntriesToday()) }
    var target by remember { mutableIntStateOf(store.calorieTarget()) }
    var food by remember { mutableStateOf("") }
    var caloriesText by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf(target.toString()) }
    val total = entries.sumOf { it.calories }

    ScreenColumn(modifier) {
        item {
            ResponsiveCard {
                Text("Today's Calories", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("$total / $target")
                Text("Food history will be archived by date; the expanding history graph is a later milestone.")
            }
        }
        item {
            ResponsiveCard {
                Text("Add food", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = food,
                    onValueChange = { if (it.length <= 60) food = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Food") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = caloriesText,
                    onValueChange = { caloriesText = it.filter(Char::isDigit).take(5) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Calories") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        val value = caloriesText.toIntOrNull()?.coerceIn(1, 10000) ?: return@Button
                        val entry = FoodEntry(food.ifBlank { "Food" }, value)
                        store.addFood(entry)
                        entries = store.foodEntriesToday()
                        food = ""
                        caloriesText = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Add to today") }
            }
        }
        item {
            ResponsiveCard {
                Text("Daily target", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter(Char::isDigit).take(5) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Target calories") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = {
                    val value = targetText.toIntOrNull() ?: return@OutlinedButton
                    store.setCalorieTarget(value)
                    target = store.calorieTarget()
                    targetText = target.toString()
                }) { Text("Save target") }
            }
        }
        item {
            ResponsiveCard {
                Text("Entries", fontWeight = FontWeight.Bold)
                if (entries.isEmpty()) {
                    Text("Nothing logged yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    entries.forEachIndexed { index, entry ->
                        if (index > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(entry.name, Modifier.weight(1f))
                            Text(entry.calories.toString(), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsScreen(
    modifier: Modifier,
    healthSdkStatus: Int,
    healthPermissionGranted: Boolean,
    healthTodaySteps: Long,
    healthCharacterSteps: Long,
    healthError: String?,
    hasStepSensor: Boolean,
    activityPermissionGranted: Boolean,
    rawSensorSteps: Float,
    sensorDelta: Long,
    eligibleSteps: Long,
    profile: CharacterProfile,
    onRequestHealth: () -> Unit,
    onRequestActivity: () -> Unit,
    onRefreshHealth: () -> Unit
) {
    val onDeviceHealthStepsAvailable = remember {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            runCatching { SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 20 }.getOrDefault(false)
    }
    ScreenColumn(modifier) {
        item {
            ResponsiveCard {
                Text("Fitness Diagnostics", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("This screen intentionally exposes the separate counters while step reconciliation is being developed.")
            }
        }
        item {
            ResponsiveCard {
                DiagnosticLine("Final in-game eligible steps", eligibleSteps.toString())
                DiagnosticLine("Health Connect — today", healthTodaySteps.toString())
                DiagnosticLine("Health Connect — since character", healthCharacterSteps.toString())
                DiagnosticLine("Direct sensor raw since boot", if (rawSensorSteps >= 0) rawSensorSteps.toLong().toString() else "Waiting")
                DiagnosticLine("Direct sensor delta from character baseline", sensorDelta.toString())
            }
        }
        item {
            ResponsiveCard {
                Text("Data sources", fontWeight = FontWeight.Bold)
                DiagnosticLine("Health Connect SDK", healthStatusLabel(healthSdkStatus, healthPermissionGranted))
                DiagnosticLine("Health Connect on-device step collection", if (onDeviceHealthStepsAvailable) "Supported" else "Not supported by this OS/module")
                DiagnosticLine("Hardware TYPE_STEP_COUNTER", if (hasStepSensor) "Available" else "Unavailable")
                DiagnosticLine("Activity Recognition permission", if (activityPermissionGranted) "Granted" else "Not granted")
                healthError?.let { DiagnosticLine("Health Connect error", it) }
                Spacer(Modifier.height(8.dp))
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    if (maxWidth >= 420.dp) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onRequestHealth, Modifier.weight(1f)) { Text("Health permission") }
                            OutlinedButton(onClick = onRequestActivity, Modifier.weight(1f)) { Text("Sensor permission") }
                            Button(onClick = onRefreshHealth, Modifier.weight(1f)) { Text("Refresh") }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onRequestHealth, Modifier.fillMaxWidth()) { Text("Health permission") }
                            OutlinedButton(onClick = onRequestActivity, Modifier.fillMaxWidth()) { Text("Sensor permission") }
                            Button(onClick = onRefreshHealth, Modifier.fillMaxWidth()) { Text("Refresh") }
                        }
                    }
                }
            }
        }
        item {
            ResponsiveCard {
                Text("Character fitness epoch", fontWeight = FontWeight.Bold)
                DiagnosticLine("Created", Instant.ofEpochMilli(profile.createdAtEpochMs).toString())
                DiagnosticLine("Health baseline at creation", profile.healthBaselineToday?.toString() ?: "Not available at creation")
                DiagnosticLine("Sensor baseline at creation", profile.sensorBaseline?.toLong()?.toString() ?: "Not available at creation")
                Text("The authoritative long-term sync/reward ledger is the next tracking milestone; this build uses the character timestamp plus the live sensor baseline for safe early testing.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DiagnosticLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(value, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

@Composable
private fun StatusLine(label: String, value: String) = DiagnosticLine(label, value)

private fun healthStatusLabel(status: Int, permissionGranted: Boolean): String = when (status) {
    HealthConnectClient.SDK_AVAILABLE -> if (permissionGranted) "Connected" else "Available — permission needed"
    HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> "Provider install/update required"
    else -> "Unavailable"
}

private fun sensorStatusLabel(hasSensor: Boolean, permissionGranted: Boolean): String = when {
    !hasSensor -> "Checking / unavailable"
    permissionGranted -> "Active"
    else -> "Permission needed"
}

@Composable
private fun HumanoidGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val ink = Color(0xFFE7D7BC)
        val coat = Color(0xFF78906D)
        val dark = Color(0xFF273128)
        drawCircle(ink, radius = w * 0.13f, center = Offset(w * 0.5f, h * 0.22f))
        drawRoundRect(coat, topLeft = Offset(w * 0.34f, h * 0.35f), size = Size(w * 0.32f, h * 0.34f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .06f))
        drawLine(ink, Offset(w * .35f, h * .42f), Offset(w * .18f, h * .58f), strokeWidth = w * .07f, cap = StrokeCap.Round)
        drawLine(ink, Offset(w * .65f, h * .42f), Offset(w * .82f, h * .56f), strokeWidth = w * .07f, cap = StrokeCap.Round)
        drawLine(dark, Offset(w * .43f, h * .68f), Offset(w * .34f, h * .91f), strokeWidth = w * .09f, cap = StrokeCap.Round)
        drawLine(dark, Offset(w * .57f, h * .68f), Offset(w * .66f, h * .91f), strokeWidth = w * .09f, cap = StrokeCap.Round)
        drawLine(Color(0xFFB9C4CE), Offset(w * .77f, h * .48f), Offset(w * .91f, h * .20f), strokeWidth = w * .035f, cap = StrokeCap.Round)
    }
}

@Composable
private fun MonsterGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val body = Color(0xFF9C6B65)
        val dark = Color(0xFF3D2426)
        val eye = Color(0xFFF3D86B)
        val w = size.width
        val h = size.height
        val hornLeft = Path().apply {
            moveTo(w * .30f, h * .30f)
            lineTo(w * .14f, h * .04f)
            lineTo(w * .42f, h * .20f)
            close()
        }
        val hornRight = Path().apply {
            moveTo(w * .70f, h * .30f)
            lineTo(w * .86f, h * .04f)
            lineTo(w * .58f, h * .20f)
            close()
        }
        drawPath(hornLeft, dark)
        drawPath(hornRight, dark)
        drawOval(body, topLeft = Offset(w * .16f, h * .18f), size = Size(w * .68f, h * .62f))
        drawCircle(eye, w * .055f, Offset(w * .37f, h * .43f))
        drawCircle(eye, w * .055f, Offset(w * .63f, h * .43f))
        drawCircle(dark, w * .022f, Offset(w * .37f, h * .43f))
        drawCircle(dark, w * .022f, Offset(w * .63f, h * .43f))
        drawArc(dark, 15f, 150f, false, topLeft = Offset(w * .34f, h * .51f), size = Size(w * .32f, h * .18f), style = Stroke(width = w * .035f, cap = StrokeCap.Round))
        drawLine(dark, Offset(w * .28f, h * .74f), Offset(w * .20f, h * .94f), strokeWidth = w * .08f, cap = StrokeCap.Round)
        drawLine(dark, Offset(w * .72f, h * .74f), Offset(w * .80f, h * .94f), strokeWidth = w * .08f, cap = StrokeCap.Round)
    }
}

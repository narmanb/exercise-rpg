package com.pathofthewild.game

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Lightweight launcher gate for the foreground tracking notification. If notification permission is
 * declined, the game still opens but custom background motion tracking stays off rather than running
 * without an obvious user-controlled Stop notification.
 */
class LauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !promptedBefore()
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        } else {
            openGame()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATIONS) {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_PROMPTED, true).apply()
            openGame()
        }
    }

    private fun promptedBefore(): Boolean =
        getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_PROMPTED, false)

    private fun openGame() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS = 4401
        private const val PREFS = "path_of_the_wild_launcher"
        private const val KEY_PROMPTED = "tracking_notification_prompted"
    }
}

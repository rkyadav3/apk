package com.ravi.tvlauncher

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalTvMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences
    private var apps by mutableStateOf<List<AppItem>>(emptyList())
    private var hidden by mutableStateOf<Set<String>>(emptySet())
    private var homeSignal by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("launcher", Context.MODE_PRIVATE)
        hidden = HashSet(prefs.getStringSet("hidden", emptySet()) ?: emptySet())

        setContent {
            MaterialTheme {
                LauncherScreen(
                    apps = apps.filter { it.pkg !in hidden },
                    hiddenCount = apps.count { it.pkg in hidden },
                    homeSignal = homeSignal,
                    onLaunch = ::launchApp,
                    onHide = ::hideApp,
                    onUnhideAll = ::unhideAll,
                    onSettings = ::openSettings,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    // Pressing Home while already on the launcher scrolls back to the first app.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homeSignal++
    }

    private fun reload() {
        lifecycleScope.launch {
            val list = withContext(Dispatchers.Default) {
                AppRepo.load(applicationContext).sortedWith(
                    compareByDescending<AppItem> { prefs.getInt("n_${it.pkg}", 0) }
                        .thenBy { it.label.lowercase() }
                )
            }
            apps = list
        }
    }

    private fun launchApp(app: AppItem) {
        prefs.edit().putInt("n_${app.pkg}", prefs.getInt("n_${app.pkg}", 0) + 1).apply()
        try {
            startActivity(app.intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Can't open ${app.label}", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Can't open ${app.label}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideApp(app: AppItem) {
        hidden = hidden + app.pkg
        prefs.edit().putStringSet("hidden", HashSet(hidden)).apply()
        Toast.makeText(this, "${app.label} hidden", Toast.LENGTH_SHORT).show()
    }

    private fun unhideAll() {
        hidden = emptySet()
        prefs.edit().remove("hidden").apply()
    }

    private fun openSettings() {
        try {
            startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Settings not available", Toast.LENGTH_SHORT).show()
        }
    }
}

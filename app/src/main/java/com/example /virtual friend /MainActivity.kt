package com.example.virtualfriend

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.virtualfriend.data.SettingsRepository
import com.example.virtualfriend.model.FriendSettings
import com.example.virtualfriend.overlay.FriendOverlayService
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    private lateinit var repo: SettingsRepository
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = SettingsRepository(this)
        setContent { VirtualFriendApp() }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            try {
                val onboarding = repo.onboardingComplete.first()
                val s = repo.settings.first()
                if (onboarding && s.enabled && Settings.canDrawOverlays(this@MainActivity)) {
                    startFriendService()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startFriendService() {
        try {
            val intent = Intent(this, FriendOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopFriendService() { 
        try {
            stopService(Intent(this, FriendOverlayService::class.java)) 
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openOverlaySettings() {
        try {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    @Composable
    private fun VirtualFriendApp() {
        val settings by repo.settings.collectAsState(initial = FriendSettings())
        val onboarding by repo.onboardingComplete.collectAsState(initial = false)
        MaterialTheme(colorScheme = lightColorScheme()) {
            if (!onboarding) OnboardingScreen(settings) else SettingsScreen(settings)
        }
    }

    @Composable
    private fun OnboardingScreen(settings: FriendSettings) {
        val overlayGranted = Settings.canDrawOverlays(this)
        Surface(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(35.dp))
                Text("Virtual Friend", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(6.dp))
                Text("A tiny Itachi companion for your phone.", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(26.dp))
                Text("What it does", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("It can float above other apps, wander gently, show short reminders, and chat offline. The companion is designed to stay small and unobtrusive.")
                Spacer(Modifier.height(22.dp))
                Text("Why overlay permission?", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("Android requires a user-approved overlay permission for apps that draw above other apps. Virtual Friend uses the official Android settings page and never bypasses this protection.")
                Spacer(Modifier.height(18.dp))
                if (!overlayGranted) {
                    Button(onClick = ::openOverlaySettings, Modifier.fillMaxWidth()) { Text("Enable floating friend") }
                } else {
                    Button(onClick = {
                        lifecycleScope.launch { repo.completeOnboarding() }
                        startFriendService()
                        requestNotifications()
                    }, Modifier.fillMaxWidth()) { Text("Start Itachi") }
                }
                Spacer(Modifier.height(14.dp))
                Text("You can pause the friend, change visit timing, resize it, or turn the summon button off from Settings.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    @Composable
    private fun SettingsScreen(settings: FriendSettings) {
        val overlayGranted = Settings.canDrawOverlays(this)
        val context = this
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
            Text("Virtual Friend", style = MaterialTheme.typography.headlineMedium)
            Text("Itachi companion settings", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(18.dp))

            if (!overlayGranted) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Floating permission is off", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text("Android has revoked or not yet granted the permission needed for the overlay.")
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = ::openOverlaySettings) { Text("Open Android overlay settings") }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            SettingSwitch("Friend enabled", settings.enabled) { enabled ->
                lifecycleScope.launch { repo.update { it.copy(enabled = enabled) } }
                if (enabled && overlayGranted) startFriendService() else if (!enabled) stopFriendService()
            }
            SettingSwitch("Check-ins", settings.checkInsEnabled) { v -> lifecycleScope.launch { repo.update { it.copy(checkInsEnabled = v) } } }
            SettingSwitch("Water reminders", settings.waterEnabled) { v -> lifecycleScope.launch { repo.update { it.copy(waterEnabled = v) } } }
            SettingSwitch("Sound", settings.soundEnabled) { v -> lifecycleScope.launch { repo.update { it.copy(soundEnabled = v) } } }
            SettingSwitch("AI chat mode", settings.aiChatEnabled) { v -> lifecycleScope.launch { repo.update { it.copy(aiChatEnabled = v) } } }
            SettingSwitch("Floating summon button", settings.summonButtonEnabled) { v -> lifecycleScope.launch { repo.update { it.copy(summonButtonEnabled = v) } } }

            Spacer(Modifier.height(12.dp))
            Text("Friend size", style = MaterialTheme.typography.titleMedium)
            Slider(settings.friendSize, { v -> lifecycleScope.launch { repo.update { it.copy(friendSize = v) } } }, valueRange = 0.45f..1.1f)
            Text("${(settings.friendSize * 100).toInt()}%")

            Text("Animation speed", style = MaterialTheme.typography.titleMedium)
            Slider(settings.animationSpeed, { v -> lifecycleScope.launch { repo.update { it.copy(animationSpeed = v) } } }, valueRange = 0.5f..2f)
            Text("${String.format("%.1fx", settings.animationSpeed)}")

            Spacer(Modifier.height(10.dp))
            Text("Visit frequency", style = MaterialTheme.typography.titleMedium)
            FrequencyRow(settings.visitMinutes, listOf(5, 10, 15, 30, 60), "minutes") { v -> lifecycleScope.launch { repo.update { it.copy(visitMinutes = v) } } }

            Spacer(Modifier.height(8.dp))
            Text("Water reminder frequency", style = MaterialTheme.typography.titleMedium)
            FrequencyRow(settings.waterMinutes, listOf(30, 60, 120), "minutes") { v -> lifecycleScope.launch { repo.update { it.copy(waterMinutes = v) } } }

            Spacer(Modifier.height(12.dp))
            Text("Quiet hours", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickTime(settings.quietStart) { v -> lifecycleScope.launch { repo.update { it.copy(quietStart = v) } } } }) { Text("Start ${formatMinutes(settings.quietStart)}") }
                OutlinedButton(onClick = { pickTime(settings.quietEnd) { v -> lifecycleScope.launch { repo.update { it.copy(quietEnd = v) } } } }) { Text("End ${formatMinutes(settings.quietEnd)}") }
            }
            Text("Automatic visits and water reminders pause during quiet hours. Manual summon stays available.", style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(18.dp))
            Text("Recovery", style = MaterialTheme.typography.titleLarge)
            Button(onClick = { sendAction(FriendOverlayService.ACTION_BRING_BACK) }, Modifier.fillMaxWidth()) { Text("Bring friend back") }
            OutlinedButton(onClick = { lifecycleScope.launch { repo.update { it.copy(pauseUntil = System.currentTimeMillis() + 30 * 60_000L) } } }, Modifier.fillMaxWidth()) { Text("Pause 30 minutes") }
            OutlinedButton(onClick = { lifecycleScope.launch { repo.update { it.copy(pauseUntil = System.currentTimeMillis() + 60 * 60_000L) } } }, Modifier.fillMaxWidth()) { Text("Pause 1 hour") }
            OutlinedButton(onClick = { sendAction(FriendOverlayService.ACTION_PAUSE_TOMORROW) }, Modifier.fillMaxWidth()) { Text("Pause until tomorrow") }
            TextButton(onClick = { lifecycleScope.launch { repo.update { it.copy(pauseUntil = 0L) } } }, Modifier.fillMaxWidth()) { Text("Resume") }
            OutlinedButton(onClick = {
                lifecycleScope.launch { repo.resetAll() }
                stopFriendService()
            }, Modifier.fillMaxWidth()) { Text("Reset all friend settings") }

            Spacer(Modifier.height(24.dp))
            Text("Overlay permission is controlled by Android. Battery optimization policies vary by phone manufacturer, so background reliability may differ on aggressive battery-saving devices.", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(20.dp))
        }
    }

    @Composable
    private fun SettingSwitch(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f))
            Switch(checked, onCheckedChange)
        }
    }

    @Composable
    private fun FrequencyRow(selected: Int, values: List<Int>, unit: String, onSelect: (Int) -> Unit) {
        var showDialog by remember { mutableStateOf(false) }
        var customText by remember { mutableStateOf(selected.toString()) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            values.forEach { value ->
                FilterChip(selected == value, { onSelect(value) }, label = { Text(if (value >= 60) "${value / 60}h" else "${value}m") })
            }
        }
        if (selected !in values) Text("Custom: $selected $unit", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { customText = selected.toString(); showDialog = true }) { Text("Custom") }
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Custom frequency") },
                text = {
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { customText = it.filter(Char::isDigit).take(4) },
                        label = { Text("Minutes") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        customText.toIntOrNull()?.takeIf { it >= 1 }?.let(onSelect)
                        showDialog = false
                    }) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
            )
        }
    }

    private fun sendAction(action: String) {
        try {
            val i = Intent(this, FriendOverlayService::class.java).setAction(action)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun pickTime(minutes: Int, onPicked: (Int) -> Unit) {
        val h = minutes / 60
        val m = minutes % 60
        TimePickerDialog(this, { _, hour, minute -> onPicked(hour * 60 + minute) }, h, m, true).show()
    }

    private fun formatMinutes(value: Int): String = String.format("%02d:%02d", value / 60, value % 60)
}

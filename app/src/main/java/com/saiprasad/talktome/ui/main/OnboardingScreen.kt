package com.saiprasad.talktome.ui.main

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.saiprasad.talktome.data.HistoryRepository
import com.saiprasad.talktome.data.SettingsRepository
import com.saiprasad.talktome.service.ServiceStatus
import com.saiprasad.talktome.service.TalkToMeAccessibilityService

@Composable
fun OnboardingScreen() {
    val context = LocalContext.current
    var audioGranted by remember { mutableStateOf(hasAudioPermission(context)) }
    var accessibilityEnabled by remember { mutableStateOf(hasAccessibilityPermission(context)) }
    var batteryUnrestricted by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }

    // Coming back from a Settings screen re-checks everything; no "I've granted it" button needed.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        audioGranted = hasAudioPermission(context)
        accessibilityEnabled = hasAccessibilityPermission(context)
        batteryUnrestricted = isIgnoringBatteryOptimizations(context)
    }

    val audioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        audioGranted = isGranted
    }

    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        if (audioGranted && accessibilityEnabled) {
            HomeContent(context, batteryUnrestricted)
        } else {
            SetupContent(
                context = context,
                audioGranted = audioGranted,
                onRequestAudio = { audioLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            )
        }
    }
}

@Composable
private fun HomeContent(context: Context, batteryUnrestricted: Boolean) {
    val settingsRepo = remember { SettingsRepository.getInstance(context) }
    val historyRepo = remember { HistoryRepository.getInstance(context) }
    val isBubbleEnabled by settingsRepo.bubbleEnabled.collectAsState()
    val serviceConnected by ServiceStatus.isConnected.collectAsState()
    val historyList by historyRepo.history.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("TalkToMe", style = MaterialTheme.typography.headlineMedium)
        }

        item {
            if (serviceConnected) {
                StatusCard(
                    title = if (isBubbleEnabled) "Ready" else "Bubble is turned off",
                    body = if (isBubbleEnabled) {
                        "Tap any text box in WhatsApp, Telegram, etc. The mic appears above your keyboard."
                    } else {
                        "Turn on the floating bubble below whenever you want to dictate."
                    },
                    isError = false,
                )
            } else {
                StatusCard(
                    title = "Service isn't running",
                    body = "Accessibility is on, but Android stopped TalkToMe. Open Accessibility settings, " +
                        "turn TalkToMe off and back on, then return here.",
                    isError = true,
                    actionLabel = "Open Accessibility settings",
                    onAction = { openAccessibilitySettings(context) },
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Floating bubble", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Show the mic above the keyboard",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = isBubbleEnabled,
                        onCheckedChange = { settingsRepo.setBubbleEnabled(it) },
                    )
                }
            }
        }

        if (!batteryUnrestricted) {
            item {
                StatusCard(
                    title = "Keep TalkToMe running",
                    body = "Allow unrestricted battery use so Android doesn't stop the bubble in the background. " +
                        "On Xiaomi/Oppo/Vivo also enable Autostart for TalkToMe.",
                    isError = false,
                    actionLabel = "Allow",
                    onAction = { requestIgnoreBatteryOptimizations(context) },
                )
            }
        }

        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text("History", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Tap an item to copy it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (historyList.isEmpty()) {
            item {
                Text(
                    "No past transcriptions yet.",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            items(historyList) { item ->
                HistoryItem(item, context)
            }
        }
    }
}

@Composable
private fun SetupContent(context: Context, audioGranted: Boolean, onRequestAudio: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        Text(
            "Welcome to TalkToMe",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Tap the mic, speak in Telugu, and get Tenglish typed anywhere.",
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (!audioGranted) {
            PermissionRequestRow(
                title = "1. Microphone",
                description = "Needed to record your voice.",
                onClick = onRequestAudio,
            )
        } else {
            PermissionRequestRow(
                title = "2. Accessibility Service",
                description = "Shows the mic above your keyboard and types the text into other apps. " +
                    "Open Accessibility → Installed apps (or Downloaded apps) → TalkToMe and turn on " +
                    "\"Use TalkToMe\". Leave the shortcut toggle off.",
                onClick = { openAccessibilitySettings(context) },
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Toggle greyed out or says \"Restricted setting\"?",
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Android blocks accessibility for apps installed outside the Play Store. Open App info, " +
                        "tap ⋮ (top right) → \"Allow restricted settings\", then come back and try again.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { openAppInfo(context) }) {
                    Text("Open App info")
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    body: String,
    isError: Boolean,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    val colors = if (isError) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    }
    Card(shape = RoundedCornerShape(12.dp), colors = colors, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium)
            if (actionLabel != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

@Composable
fun HistoryItem(text: String, context: Context) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("TalkToMe Transcript", text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun PermissionRequestRow(title: String, description: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(description, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onClick) {
            Text("Grant Permission")
        }
    }
}

private fun openAccessibilitySettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
}

private fun openAppInfo(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
    )
}

private fun requestIgnoreBatteryOptimizations(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
        )
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }
}

fun hasAudioPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
}

fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

fun hasAccessibilityPermission(context: Context): Boolean {
    val expected = ComponentName(context, TalkToMeAccessibilityService::class.java)
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    // Entries can be stored in full ("pkg/pkg.Cls") or short ("pkg/.Cls") form; compare components.
    val splitter = TextUtils.SimpleStringSplitter(':')
    splitter.setString(enabledServices)
    while (splitter.hasNext()) {
        if (ComponentName.unflattenFromString(splitter.next()) == expected) return true
    }
    return false
}

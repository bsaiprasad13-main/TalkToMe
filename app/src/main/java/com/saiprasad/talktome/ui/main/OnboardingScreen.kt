package com.saiprasad.talktome.ui.main

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import com.saiprasad.talktome.data.HistoryRepository

@Composable
fun OnboardingScreen() {
    val context = LocalContext.current
    var audioGranted by remember { mutableStateOf(hasAudioPermission(context)) }
    var overlayGranted by remember { mutableStateOf(hasOverlayPermission(context)) }
    var accessibilityGranted by remember { mutableStateOf(hasAccessibilityPermission(context)) }

    val audioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        audioGranted = isGranted
    }

    val checkPermissions = {
        audioGranted = hasAudioPermission(context)
        overlayGranted = hasOverlayPermission(context)
        accessibilityGranted = hasAccessibilityPermission(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (audioGranted && overlayGranted && accessibilityGranted) {
            Text(
                "TalkToMe History",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                "The floating bubble will appear when you tap on a text box. Tap below to copy past transcriptions.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            val historyRepo = remember { HistoryRepository(context) }
            val historyList by historyRepo.history.collectAsState()

            if (historyList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No past transcriptions yet.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(historyList) { item ->
                        HistoryItem(item, context)
                    }
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
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
                        onClick = { audioLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                    )
                } else if (!overlayGranted) {
                    PermissionRequestRow(
                        title = "2. Display Over Other Apps",
                        description = "Needed to show the floating mic bubble.",
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    )
                } else if (!accessibilityGranted) {
                    PermissionRequestRow(
                        title = "3. Accessibility Service",
                        description = "Needed to type the translated text into other apps. Go to Settings -> Accessibility -> Installed Apps -> TalkToMe and enable it.",
                        onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = checkPermissions) {
                    Text("I've granted it, continue")
                }
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
                val clip = ClipData.newPlainText("TalkToMe Translation", text)
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

fun hasAudioPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
}

fun hasOverlayPermission(context: Context): Boolean {
    return Settings.canDrawOverlays(context)
}

fun hasAccessibilityPermission(context: Context): Boolean {
    var accessibilityEnabled = 0
    val service = "${context.packageName}/com.saiprasad.talktome.service.TalkToMeAccessibilityService"
    try {
        accessibilityEnabled = Settings.Secure.getInt(
            context.applicationContext.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED
        )
    } catch (e: Settings.SettingNotFoundException) {
        // Ignore
    }
    val mStringColonSplitter = TextUtils.SimpleStringSplitter(':')

    if (accessibilityEnabled == 1) {
        val settingValue = Settings.Secure.getString(
            context.applicationContext.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        if (settingValue != null) {
            mStringColonSplitter.setString(settingValue)
            while (mStringColonSplitter.hasNext()) {
                val accessibilityService = mStringColonSplitter.next()
                if (accessibilityService.equals(service, ignoreCase = true)) {
                    return true
                }
            }
        }
    }
    return false
}

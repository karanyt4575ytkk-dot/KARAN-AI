package com.example.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.automation.KaranAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PermissionItem(
    val id: String,
    val name: String,
    val purpose: String,
    val isGranted: Boolean,
    val isSystemSettingsRequired: Boolean,
    val explanation: String
)

class PermissionManager(private val context: Context) {

    private val _permissionsState = MutableStateFlow<List<PermissionItem>>(emptyList())
    val permissionsState: StateFlow<List<PermissionItem>> = _permissionsState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val list = mutableListOf<PermissionItem>()

        // 1. Microphone
        val micGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        list.add(
            PermissionItem(
                id = "microphone",
                name = "Microphone (Audio)",
                purpose = "Voice commands, push-to-talk, and speech recognition.",
                isGranted = micGranted,
                isSystemSettingsRequired = false,
                explanation = "Allows you to speak to KARAN in Hindi or English."
            )
        )

        // 2. Notifications
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        list.add(
            PermissionItem(
                id = "notifications",
                name = "Notifications",
                purpose = "Alerts on long-running task completions and build outcomes.",
                isGranted = notifGranted,
                isSystemSettingsRequired = false,
                explanation = "Notifies you when background coding builds or planned workflows finish."
            )
        )

        // 3. Accessibility Service
        val accessGranted = KaranAccessibilityService.isServiceRunning()
        list.add(
            PermissionItem(
                id = "accessibility",
                name = "Accessibility Service",
                purpose = "Legitimate device automation (clicking, scrolling, UI inspection on your explicit command).",
                isGranted = accessGranted,
                isSystemSettingsRequired = true,
                explanation = "Must be enabled manually in Android Accessibility Settings. KARAN only acts upon your explicit direction."
            )
        )

        // 4. Internet
        list.add(
            PermissionItem(
                id = "internet",
                name = "Internet & Network",
                purpose = "Access Gemini AI reasoning engine and online web references.",
                isGranted = true,
                isSystemSettingsRequired = false,
                explanation = "Standard Android install-time permission."
            )
        )

        _permissionsState.value = list
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openAccessibilitySettings() {
        KaranAccessibilityService.openAccessibilitySettings(context)
    }
}

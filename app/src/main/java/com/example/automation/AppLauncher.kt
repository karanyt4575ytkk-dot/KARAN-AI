package com.example.automation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

data class AppInfo(
    val appName: String,
    val packageName: String
)

class AppLauncher(private val context: Context) {

    fun launchAppByPackage(packageName: String): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return true
    }

    fun launchAppByName(nameQuery: String): Pair<Boolean, String> {
        val apps = getInstalledApps()
        val query = nameQuery.trim().lowercase()

        // Exact or best match
        val matched = apps.firstOrNull { it.appName.lowercase() == query }
            ?: apps.firstOrNull { it.appName.lowercase().contains(query) }

        if (matched != null) {
            val launched = launchAppByPackage(matched.packageName)
            return if (launched) {
                Pair(true, "Launched ${matched.appName}")
            } else {
                Pair(false, "Failed to launch ${matched.appName}")
            }
        }

        // Check common aliases
        when (query) {
            "browser", "chrome", "internet" -> {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                return Pair(true, "Launched web browser")
            }
            "settings" -> {
                val settingsIntent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                return Pair(true, "Launched system settings")
            }
        }

        return Pair(false, "App '$nameQuery' not found on device")
    }

    fun getInstalledApps(): List<AppInfo> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        return resolveInfos.mapNotNull {
            val pkg = it.activityInfo?.packageName ?: return@mapNotNull null
            val label = it.loadLabel(pm)?.toString() ?: pkg
            AppInfo(appName = label, packageName = pkg)
        }.distinctBy { it.packageName }.sortedBy { it.appName }
    }
}

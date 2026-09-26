package com.example.automation

import android.content.Context
import android.content.Intent
import android.net.Uri

class BrowserAutomation(private val context: Context) {

    fun openUrl(url: String): Boolean {
        return try {
            val formatted = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else {
                url
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formatted)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openFirebaseConsole(projectId: String? = null): Boolean {
        val url = if (projectId.isNullOrBlank()) {
            "https://console.firebase.google.com"
        } else {
            "https://console.firebase.google.com/project/$projectId/overview"
        }
        return openUrl(url)
    }

    fun openSearch(query: String): Boolean {
        val encoded = Uri.encode(query)
        return openUrl("https://www.google.com/search?q=$encoded")
    }

    fun openGitHub(repoPath: String? = null): Boolean {
        val url = if (repoPath.isNullOrBlank()) "https://github.com" else "https://github.com/$repoPath"
        return openUrl(url)
    }
}

package com.example.ai

object ResponseManager {

    fun formatVoiceSummary(rawText: String): String {
        // Strip code snippets and markdown for speech
        return rawText
            .replace(Regex("""```[\s\S]*?```"""), "कोड फाइल अपडेट कर दी गई है।")
            .replace(Regex("""`[^`]*`"""), "")
            .replace(Regex("""[*#_>\[\]]"""), "")
            .trim()
            .take(250)
    }

    fun isHindiLanguage(text: String): Boolean {
        return text.any { it in '\u0900'..'\u097F' }
    }
}

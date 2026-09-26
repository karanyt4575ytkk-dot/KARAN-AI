package com.example.coding

data class ErrorDiagnosis(
    val errorType: String,
    val description: String,
    val explanationHindi: String,
    val suggestedFix: String,
    val probableFile: String?
)

class ErrorAnalyzer {

    fun diagnose(errorMessage: String): ErrorDiagnosis {
        val lower = errorMessage.lowercase()

        return when {
            lower.contains("referenceerror") || lower.contains("is not defined") -> {
                val match = Regex("""([a-zA-Z0-9_$]+) is not defined""").find(errorMessage)
                val variable = match?.groupValues?.getOrNull(1) ?: "variable"
                ErrorDiagnosis(
                    errorType = "ReferenceError",
                    description = "Variable or function '$variable' is used before being declared or imported.",
                    explanationHindi = "वेरिएबल या फंक्शन '$variable' डिफाइन नहीं है या सही क्रम में लोड नहीं हुआ है।",
                    suggestedFix = "Ensure '$variable' is declared with 'let/const' or that the script defining it is included before it is called.",
                    probableFile = "app.js"
                )
            }
            lower.contains("firebase") && (lower.contains("no-app") || lower.contains("not initialized")) -> {
                ErrorDiagnosis(
                    errorType = "FirebaseInitializationError",
                    description = "Firebase SDK was accessed before 'initializeApp(firebaseConfig)' was completed.",
                    explanationHindi = "Firebase इनिशियलाइज़ होने से पहले कॉल किया जा रहा है।",
                    suggestedFix = "Call initializeApp(firebaseConfig) at the top of your script before calling auth() or getFirestore().",
                    probableFile = "firebase.js"
                )
            }
            lower.contains("syntaxerror") || lower.contains("unexpected token") -> {
                ErrorDiagnosis(
                    errorType = "SyntaxError",
                    description = "There is a syntax error such as an unclosed quote, bracket, or typo.",
                    explanationHindi = "कोड में सिंटैक्स की गलती है (जैसे ब्रैकेट या कोट्स का बंद न होना)।",
                    suggestedFix = "Check for matching braces {}, parentheses (), or unclosed quotes in the line mentioned.",
                    probableFile = "index.html"
                )
            }
            lower.contains("404") || lower.contains("not found") -> {
                ErrorDiagnosis(
                    errorType = "ResourceNotFound (404)",
                    description = "A referenced stylesheet, script, or image path does not exist.",
                    explanationHindi = "फाइल का पाथ नहीं मिल रहा है।",
                    suggestedFix = "Verify relative file paths (e.g., './css/style.css' vs 'style.css') and ensure target file is created.",
                    probableFile = "index.html"
                )
            }
            lower.contains("cors") || lower.contains("access-control-allow-origin") -> {
                ErrorDiagnosis(
                    errorType = "CORS Policy Restriction",
                    description = "Cross-Origin Resource Sharing blocked request to an external API.",
                    explanationHindi = "ब्राउज़र सुरक्षा नीति के कारण बाहरी API ब्लॉक हो गई है।",
                    suggestedFix = "Use a CORS proxy or configure server-side headers to allow requests from the app origin.",
                    probableFile = "app.js"
                )
            }
            else -> {
                ErrorDiagnosis(
                    errorType = "General Error",
                    description = "Execution or parsing error: $errorMessage",
                    explanationHindi = "इस एरर को समझने के लिए संबंधित कोड फाइल्स की जांच की जा रही है।",
                    suggestedFix = "Inspect console output, check the recent changes, and ensure all dependencies are properly resolved.",
                    probableFile = null
                )
            }
        }
    }
}

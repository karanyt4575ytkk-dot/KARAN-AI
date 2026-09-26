package com.example.ai

data class ModelOption(
    val id: String,
    val displayName: String,
    val description: String,
    val isRecommended: Boolean = false
)

object GeminiConfig {
    const val MODEL_FLASH_LITE = "gemini-3.1-flash-lite-preview"
    const val MODEL_FLASH_LITE_LATEST = "gemini-flash-lite-latest"
    const val MODEL_FLASH_LATEST = "gemini-flash-latest"
    const val MODEL_FLASH_3_5 = "gemini-3.5-flash"
    const val MODEL_FLASH_3_8 = "gemini-3.8-flash"
    const val MODEL_PRO_PREVIEW = "gemini-3.1-pro-preview"

    // Default primary model: fast, highly-available, supports function calling & Hindi/English reasoning
    const val DEFAULT_MODEL = MODEL_FLASH_LITE

    val AVAILABLE_MODELS = listOf(
        ModelOption(
            id = MODEL_FLASH_LITE,
            displayName = "Flash Lite 3.1 (Recommended)",
            description = "Fastest response, high availability, complete tool & voice support",
            isRecommended = true
        ),
        ModelOption(
            id = MODEL_FLASH_LITE_LATEST,
            displayName = "Flash Lite Latest",
            description = "Stable lightweight Flash model with consistent availability",
            isRecommended = false
        ),
        ModelOption(
            id = MODEL_FLASH_LATEST,
            displayName = "Flash Latest",
            description = "Standard Gemini flash model",
            isRecommended = false
        ),
        ModelOption(
            id = MODEL_FLASH_3_8,
            displayName = "Gemini 3.8 Flash",
            description = "Next-gen flash model for high-throughput tasks",
            isRecommended = false
        ),
        ModelOption(
            id = MODEL_FLASH_3_5,
            displayName = "Gemini 3.5 Flash",
            description = "High capability (frequently encounters peak demand 503)",
            isRecommended = false
        ),
        ModelOption(
            id = MODEL_PRO_PREVIEW,
            displayName = "Gemini 3.1 Pro",
            description = "Deep reasoning & complex code analysis",
            isRecommended = false
        )
    )

    // Fallback models in priority order if the active model experiences 503 UNAVAILABLE or 429
    val FALLBACK_MODELS = listOf(
        MODEL_FLASH_LITE,
        MODEL_FLASH_LITE_LATEST,
        MODEL_FLASH_LATEST
    )

    // Retry settings for 503 UNAVAILABLE and transient errors
    const val MAX_RETRIES = 3
    const val INITIAL_BACKOFF_MS = 1000L
    const val BACKOFF_MULTIPLIER = 2.0
    const val MAX_BACKOFF_MS = 4000L

    fun sanitizeModelName(model: String): String {
        return model.trim().removePrefix("models/")
    }

    fun getFallbackModels(activeModel: String): List<String> {
        val sanitizedActive = sanitizeModelName(activeModel)
        return FALLBACK_MODELS.filter { sanitizeModelName(it) != sanitizedActive }
    }
}


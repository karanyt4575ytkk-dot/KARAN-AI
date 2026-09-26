package com.example.ai

import com.example.BuildConfig
import com.example.security.SecretManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.pow

data class GeminiModelResponse(
    val text: String?,
    val functionCalls: List<GeminiFunctionCall> = emptyList(),
    val isSuccess: Boolean = true,
    val errorMessage: String? = null,
    val friendlyErrorMessage: String? = null,
    val isRetryable: Boolean = false,
    val modelUsed: String? = null
)

data class GeminiFunctionCall(
    val name: String,
    val arguments: JSONObject
)

data class ParsedApiError(
    val code: Int,
    val status: String,
    val message: String,
    val is503Unavailable: Boolean,
    val is429QuotaExceeded: Boolean
)

class GeminiClient(private val secretManager: SecretManager) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    var activeModel: String = GeminiConfig.DEFAULT_MODEL

    private fun getEffectiveApiKey(): String {
        val userCustomKey = secretManager.getSecret("CUSTOM_GEMINI_API_KEY")
        if (!userCustomKey.isNullOrBlank()) {
            return userCustomKey
        }
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Throwable) {
            ""
        }
        return if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
            buildKey
        } else {
            ""
        }
    }

    private fun parseErrorBody(responseCode: Int, responseBody: String?): ParsedApiError {
        var status = if (responseCode == 503) "UNAVAILABLE" else "ERROR_$responseCode"
        var message = "The AI service is temporarily unavailable. (HTTP $responseCode)"
        if (!responseBody.isNullOrBlank()) {
            try {
                val json = JSONObject(responseBody)
                val errObj = json.optJSONObject("error")
                if (errObj != null) {
                    status = errObj.optString("status", status)
                    message = errObj.optString("message", message)
                }
            } catch (_: Exception) {
                if (responseCode == 503) {
                    message = "This model is currently experiencing high demand. Spikes in demand are usually temporary."
                }
            }
        }
        val is503 = responseCode == 503 ||
                status.equals("UNAVAILABLE", ignoreCase = true) ||
                message.contains("high demand", ignoreCase = true) ||
                message.contains("temporarily unavailable", ignoreCase = true)
        val is429 = responseCode == 429 ||
                status.equals("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                message.contains("quota", ignoreCase = true)
        return ParsedApiError(responseCode, status, message, is503, is429)
    }

    suspend fun generateWithTools(
        systemInstruction: String,
        conversationHistory: List<Pair<String, String>>, // role, text
        latestPrompt: String,
        toolsDeclarations: JSONArray?,
        onRetryAttempt: ((attempt: Int, model: String, delayMs: Long) -> Unit)? = null
    ): GeminiModelResponse = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank()) {
            return@withContext GeminiModelResponse(
                text = null,
                isSuccess = false,
                errorMessage = "API key missing",
                friendlyErrorMessage = "Gemini API key is not configured. Please add your key in the Secrets panel or app Settings.",
                isRetryable = false
            )
        }

        val primaryModel = GeminiConfig.sanitizeModelName(activeModel)

        // Try primary model first with exponential backoff for 503 / UNAVAILABLE
        val primaryResult = executeWithRetry(
            model = primaryModel,
            apiKey = apiKey,
            systemInstruction = systemInstruction,
            conversationHistory = conversationHistory,
            latestPrompt = latestPrompt,
            toolsDeclarations = toolsDeclarations,
            onRetryAttempt = onRetryAttempt
        )

        if (primaryResult.isSuccess) {
            return@withContext primaryResult
        }

        // If primary model failed specifically with 503 UNAVAILABLE or 429, try healthy fallback models in priority order
        if (primaryResult.isRetryable) {
            val fallbacks = GeminiConfig.getFallbackModels(primaryModel)
            for (fallback in fallbacks) {
                onRetryAttempt?.invoke(1, fallback, 300L)
                delay(300L)
                val fallbackResult = executeWithRetry(
                    model = fallback,
                    apiKey = apiKey,
                    systemInstruction = systemInstruction,
                    conversationHistory = conversationHistory,
                    latestPrompt = latestPrompt,
                    toolsDeclarations = toolsDeclarations,
                    onRetryAttempt = onRetryAttempt,
                    maxRetries = 1
                )
                if (fallbackResult.isSuccess) {
                    return@withContext fallbackResult
                }
            }
        }

        primaryResult
    }

    private suspend fun executeWithRetry(
        model: String,
        apiKey: String,
        systemInstruction: String,
        conversationHistory: List<Pair<String, String>>,
        latestPrompt: String,
        toolsDeclarations: JSONArray?,
        onRetryAttempt: ((attempt: Int, model: String, delayMs: Long) -> Unit)?,
        maxRetries: Int = GeminiConfig.MAX_RETRIES
    ): GeminiModelResponse {
        var lastError: ParsedApiError? = null
        var lastExceptionMessage: String? = null

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestJson = buildRequestBodyJson(systemInstruction, conversationHistory, latestPrompt, toolsDeclarations)
        val cleanModel = GeminiConfig.sanitizeModelName(model)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$apiKey"

        for (attempt in 0..maxRetries) {
            if (attempt > 0) {
                val backoff = min(
                    (GeminiConfig.INITIAL_BACKOFF_MS * (GeminiConfig.BACKOFF_MULTIPLIER.pow(attempt - 1))).toLong(),
                    GeminiConfig.MAX_BACKOFF_MS
                )
                onRetryAttempt?.invoke(attempt, cleanModel, backoff)
                delay(backoff)
            }

            val requestBody = requestJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()

                    if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                        val parsed = JSONObject(responseBody)
                        val candidates = parsed.optJSONArray("candidates")
                        if (candidates == null || candidates.length() == 0) {
                            val promptFeedback = parsed.optJSONObject("promptFeedback")
                            val blockReason = promptFeedback?.optString("blockReason")
                            val text = if (!blockReason.isNullOrBlank()) {
                                "Notice: Response filtered by safety guidelines ($blockReason)."
                            } else {
                                "KARAN: No response generated by model."
                            }
                            return GeminiModelResponse(
                                text = text,
                                isSuccess = true,
                                modelUsed = cleanModel
                            )
                        }

                        val firstCand = candidates.getJSONObject(0)
                        val contentObj = firstCand.optJSONObject("content")
                        val parts = contentObj?.optJSONArray("parts")

                        val functionCalls = mutableListOf<GeminiFunctionCall>()
                        val textBuilder = StringBuilder()

                        if (parts != null) {
                            for (i in 0 until parts.length()) {
                                val part = parts.getJSONObject(i)
                                if (part.has("text")) {
                                    textBuilder.append(part.getString("text"))
                                }
                                if (part.has("functionCall")) {
                                    val fCall = part.getJSONObject("functionCall")
                                    val name = fCall.getString("name")
                                    val args = fCall.optJSONObject("args") ?: JSONObject()
                                    functionCalls.add(GeminiFunctionCall(name, args))
                                }
                            }
                        }

                        val finalOutputText = textBuilder.toString().trim()
                        return GeminiModelResponse(
                            text = if (finalOutputText.isNotBlank()) finalOutputText else null,
                            functionCalls = functionCalls,
                            isSuccess = true,
                            modelUsed = cleanModel
                        )
                    }

                    // Handle non-success
                    val parsedErr = parseErrorBody(response.code, responseBody)
                    lastError = parsedErr

                    // If not a retryable 503 or 429, don't keep looping
                    if (!parsedErr.is503Unavailable && !parsedErr.is429QuotaExceeded) {
                        return createFriendlyErrorResponse(parsedErr, cleanModel)
                    }
                }
            } catch (e: IOException) {
                lastExceptionMessage = e.message
            } catch (e: Exception) {
                return GeminiModelResponse(
                    text = null,
                    isSuccess = false,
                    errorMessage = e.message,
                    friendlyErrorMessage = "Unexpected error while contacting AI service: ${e.localizedMessage ?: "Unknown"}",
                    isRetryable = false,
                    modelUsed = cleanModel
                )
            }
        }

        // Exhausted retries
        return if (lastError != null) {
            createFriendlyErrorResponse(lastError!!, cleanModel)
        } else {
            GeminiModelResponse(
                text = null,
                isSuccess = false,
                errorMessage = lastExceptionMessage ?: "Connection timed out",
                friendlyErrorMessage = "Network connection timed out while reaching AI services. Please check your connection and tap Retry.",
                isRetryable = true,
                modelUsed = cleanModel
            )
        }
    }

    private fun createFriendlyErrorResponse(error: ParsedApiError, model: String): GeminiModelResponse {
        val friendlyMsg = when {
            error.is503Unavailable -> {
                "The AI service ($model) is currently experiencing temporary high demand (503 Service Unavailable). KARAN automatically retried with exponential backoff and checked fallback models, but servers remain busy. Please wait a moment and tap 'Retry Request', or select another model in Settings."
            }
            error.is429QuotaExceeded -> {
                "API rate limit or quota exceeded (429). Please wait a moment and tap 'Retry Request', or check your API key quota in Settings."
            }
            error.code == 404 -> {
                "The requested model '$model' was not found. Please switch to an active model (such as Flash Lite) in Settings."
            }
            error.code == 401 || error.code == 403 -> {
                "Authentication failed (${error.code}). Please verify your Gemini API key in app Settings."
            }
            else -> {
                "AI service temporarily unavailable (${error.code}: ${error.status}). Please check your connection and tap 'Retry Request'."
            }
        }

        return GeminiModelResponse(
            text = null,
            isSuccess = false,
            errorMessage = "${error.status} (${error.code}): ${error.message}",
            friendlyErrorMessage = friendlyMsg,
            isRetryable = error.is503Unavailable || error.is429QuotaExceeded || error.code >= 500,
            modelUsed = model
        )
    }

    private fun buildRequestBodyJson(
        systemInstruction: String,
        conversationHistory: List<Pair<String, String>>,
        latestPrompt: String,
        toolsDeclarations: JSONArray?
    ): JSONObject {
        val requestJson = JSONObject()

        // System Instruction
        val sysInstructionObj = JSONObject()
        val sysParts = JSONArray()
        sysParts.put(JSONObject().put("text", systemInstruction))
        sysInstructionObj.put("parts", sysParts)
        requestJson.put("systemInstruction", sysInstructionObj)

        // Contents - Merge adjacent turns of the same role to maintain strict alternation
        val mergedTurns = mutableListOf<Pair<String, String>>()
        for (item in conversationHistory.takeLast(6)) {
            val role = if (item.first == "assistant") "model" else "user"
            val text = item.second.trim()
            if (text.isBlank()) continue

            if (mergedTurns.isNotEmpty() && mergedTurns.last().first == role) {
                val prev = mergedTurns.removeAt(mergedTurns.lastIndex)
                mergedTurns.add(role to "${prev.second}\n$text")
            } else {
                mergedTurns.add(role to text)
            }
        }

        val finalUserPrompt = latestPrompt.trim()
        if (mergedTurns.isNotEmpty() && mergedTurns.last().first == "user") {
            val prev = mergedTurns.removeAt(mergedTurns.lastIndex)
            mergedTurns.add("user" to "${prev.second}\n$finalUserPrompt")
        } else {
            mergedTurns.add("user" to finalUserPrompt)
        }

        val contentsArray = JSONArray()
        for ((role, text) in mergedTurns) {
            val cObj = JSONObject()
            cObj.put("role", role)
            val parts = JSONArray()
            parts.put(JSONObject().put("text", text))
            cObj.put("parts", parts)
            contentsArray.put(cObj)
        }

        requestJson.put("contents", contentsArray)

        // Tools
        if (toolsDeclarations != null && toolsDeclarations.length() > 0) {
            val toolsArray = JSONArray()
            val toolObj = JSONObject()
            toolObj.put("functionDeclarations", toolsDeclarations)
            toolsArray.put(toolObj)
            requestJson.put("tools", toolsArray)
        }

        // Generation Config
        val config = JSONObject()
        config.put("temperature", 0.4)
        config.put("topP", 0.95)
        config.put("maxOutputTokens", 2048)
        requestJson.put("generationConfig", config)

        return requestJson
    }
}

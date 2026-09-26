package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    var speechRate: Float = 1.0f
        set(value) {
            field = value
            tts?.setSpeechRate(value)
        }

    var speechPitch: Float = 1.0f
        set(value) {
            field = value
            tts?.setPitch(value)
        }

    var isTtsEnabled: Boolean = true

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            // Default to Hindi or English
            val hindi = Locale.forLanguageTag("hi-IN")
            val available = tts?.isLanguageAvailable(hindi)
            if (available == TextToSpeech.LANG_AVAILABLE || available == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                tts?.language = hindi
            } else {
                tts?.language = Locale.US
            }
            tts?.setSpeechRate(speechRate)
            tts?.setPitch(speechPitch)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
        }
    }

    fun speak(text: String, languageCode: String? = null) {
        if (!isTtsEnabled || !isInitialized) return
        stop()

        // Clean markdown symbols for natural speech
        val cleanText = text
            .replace(Regex("""```[\s\S]*?```"""), "कोड ब्लॉक शामिल है।")
            .replace(Regex("""`[^`]*`"""), "")
            .replace(Regex("""[*#_>\[\]]"""), "")
            .trim()

        if (cleanText.isBlank()) return

        if (languageCode != null) {
            val loc = when (languageCode) {
                "hi" -> Locale.forLanguageTag("hi-IN")
                "en" -> Locale.US
                else -> Locale.getDefault()
            }
            tts?.language = loc
        } else {
            // Auto detect Hindi characters in text
            val hasHindi = cleanText.any { it in '\u0900'..'\u097F' }
            tts?.language = if (hasHindi) Locale.forLanguageTag("hi-IN") else Locale.US
        }

        val utteranceId = "KARAN_" + System.currentTimeMillis()
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

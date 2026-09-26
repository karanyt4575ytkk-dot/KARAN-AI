package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.GeminiConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("KARAN", appName)
  }

  @Test
  fun `verify gemini model sanitize and fallback configuration`() {
    assertEquals("gemini-3.1-flash-lite-preview", GeminiConfig.sanitizeModelName("models/gemini-3.1-flash-lite-preview"))
    assertEquals("gemini-flash-latest", GeminiConfig.sanitizeModelName("gemini-flash-latest"))

    val fallbacks = GeminiConfig.getFallbackModels("gemini-3.5-flash")
    assertTrue("Fallbacks must contain at least one healthy model", fallbacks.isNotEmpty())
    assertFalse("Fallbacks must not contain the active model itself", fallbacks.contains("gemini-3.5-flash"))

    // Max retries must be bounded to prevent infinite loops
    assertTrue("Max retries should be bounded", GeminiConfig.MAX_RETRIES in 1..5)
  }
}

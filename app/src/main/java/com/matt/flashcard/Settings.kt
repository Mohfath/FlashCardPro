package com.matt.flashcard

import android.content.Context

/** The AI services that can write the Latin-letter reading of a Card. Each runs on its provider's cheapest model. */
enum class AiProvider(val label: String, val keyHelp: String) {
    ANTHROPIC("Claude", "Get a key at console.anthropic.com"),
    GEMINI("Gemini", "Get a key at aistudio.google.com"),
    OPENAI("OpenAI", "Get a key at platform.openai.com"),
}

/**
 * The learner's own API keys, kept in this app's private storage on the phone. A key left empty falls back to the one
 * baked into debug builds from local.properties (release builds bake in none).
 */
class AppSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("secrets", Context.MODE_PRIVATE)

    fun provider(): AiProvider =
        AiProvider.entries.firstOrNull { it.name == prefs.getString("aiProvider", null) } ?: AiProvider.ANTHROPIC

    fun setProvider(p: AiProvider) = prefs.edit().putString("aiProvider", p.name).apply()

    fun aiKey(p: AiProvider) = own("ai_${p.name}").ifBlank { if (p == AiProvider.ANTHROPIC) BuildConfig.ANTHROPIC_API_KEY else "" }
    fun setAiKey(p: AiProvider, key: String) = put("ai_${p.name}", key)

    fun elevenLabsKey() = own("elevenlabs").ifBlank { BuildConfig.ELEVENLABS_API_KEY }
    fun setElevenLabsKey(key: String) = put("elevenlabs", key)

    fun deepLKey() = own("deepl").ifBlank { BuildConfig.DEEPL_API_KEY }
    fun setDeepLKey(key: String) = put("deepl", key)

    /** What the learner typed themselves, for showing in the Settings screen. */
    fun own(name: String): String = prefs.getString(name, "").orEmpty()

    private fun put(name: String, value: String) = prefs.edit().putString(name, value.trim()).apply()
}

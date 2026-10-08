package com.matt.flashcard

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class AudioException(message: String) : Exception(message)

/** Premade voice "Sarah". Premade voices are the ones the free plan can use through the API. */
const val DEFAULT_VOICE_ID = "EXAVITQu4vr4xnSDxMaL"
private const val MODEL_ID = "eleven_multilingual_v2"
private const val MODEL_ID_V3 = "eleven_v3"

/**
 * Multilingual v2 guesses the language from the text and has no Persian, Thai, Hungarian or Vietnamese (Persian comes
 * out sounding Arabic). Eleven v3 covers them and, unlike v2, honours a language_code, so these use v3 with the tag.
 */
private val V3_ONLY_LANGUAGES = setOf("fa", "th", "hu", "vi")

internal fun needsV3(languageCode: String?) = languageCode?.lowercase() in V3_ONLY_LANGUAGES

internal fun elevenLabsRequestBody(text: String, languageCode: String? = null): String =
    if (needsV3(languageCode)) {
        JSONObject().put("text", text).put("model_id", MODEL_ID_V3).put("language_code", languageCode!!.lowercase()).toString()
    } else {
        JSONObject().put("text", text).put("model_id", MODEL_ID).toString()
    }

internal fun elevenLabsUrl(voiceId: String) =
    "https://api.elevenlabs.io/v1/text-to-speech/$voiceId?output_format=mp3_44100_64"

/** Prefers ElevenLabs' own explanation when it sent one, else falls back on the status code. */
internal fun elevenLabsErrorMessage(code: Int, body: String): String {
    val detail = runCatching { JSONObject(body).opt("detail") }.getOrNull()
    val fromServer = when (detail) {
        is JSONObject -> detail.optString("message")
        is String -> detail
        else -> ""
    }
    if (fromServer.isNotBlank()) return "ElevenLabs: $fromServer"
    return when (code) {
        401 -> "ElevenLabs rejected the API key"
        429 -> "ElevenLabs: too many requests, try again in a moment"
        else -> "ElevenLabs error ($code)"
    }
}

class ElevenLabsClient(
    private val apiKey: () -> String,
    private val voiceId: String = DEFAULT_VOICE_ID,
) {
    suspend fun synthesize(text: String, languageCode: String? = null): ByteArray = withContext(Dispatchers.IO) {
        val key = apiKey()
        if (key.isBlank()) throw AudioException("No ElevenLabs key: add it in Settings")
        val conn = URL(elevenLabsUrl(voiceId)).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 10_000
            conn.readTimeout = 30_000
            conn.doOutput = true
            conn.setRequestProperty("xi-api-key", key)
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(elevenLabsRequestBody(text, languageCode).toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode != 200) {
                val body = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw AudioException(elevenLabsErrorMessage(conn.responseCode, body))
            }
            conn.inputStream.use { it.readBytes() }.also {
                if (it.isEmpty()) throw AudioException("ElevenLabs returned no audio")
            }
        } catch (e: AudioException) {
            throw e
        } catch (e: java.io.IOException) {
            throw AudioException("No connection to ElevenLabs")
        } finally {
            conn.disconnect()
        }
    }
}

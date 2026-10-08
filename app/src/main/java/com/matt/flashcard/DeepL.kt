package com.matt.flashcard

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class TranslationException(message: String) : Exception(message)

/** Turns "en"/"hr" style codes into DeepL's upper-case language codes. */
internal fun deepLRequestBody(text: String, from: String, to: String): String =
    JSONObject()
        .put("text", JSONArray().put(text))
        .put("source_lang", from.uppercase())
        .put("target_lang", deepLTarget(to))
        .toString()

/** DeepL wants a regional variant for these as a target (not as a source). */
private fun deepLTarget(code: String) = when (val up = code.uppercase()) {
    "EN" -> "EN-GB"
    "PT" -> "PT-PT"
    "ZH" -> "ZH-HANS"
    else -> up
}

internal fun parseDeepLTranslation(json: String): String {
    val translations = JSONObject(json).optJSONArray("translations")
    val text = translations?.optJSONObject(0)?.optString("text").orEmpty()
    if (text.isBlank()) throw TranslationException("DeepL returned no translation")
    return text
}

internal fun deepLEndpoint(key: String) =
    if (key.endsWith(":fx")) "https://api-free.deepl.com/v2/translate" else "https://api.deepl.com/v2/translate"

internal fun deepLErrorMessage(code: Int) = when (code) {
    403 -> "DeepL rejected the API key"
    429 -> "Too many requests, try again in a moment"
    456 -> "DeepL monthly character quota used up"
    else -> "DeepL error ($code)"
}

class DeepLClient(private val apiKey: () -> String) {
    suspend fun translate(text: String, from: String, to: String): String = withContext(Dispatchers.IO) {
        val key = apiKey()
        if (key.isBlank()) throw TranslationException("No DeepL key: add it in Settings")
        val conn = URL(deepLEndpoint(key)).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.doOutput = true
            conn.setRequestProperty("Authorization", "DeepL-Auth-Key $key")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(deepLRequestBody(text, from, to).toByteArray()) }
            if (conn.responseCode != 200) throw TranslationException(deepLErrorMessage(conn.responseCode))
            parseDeepLTranslation(conn.inputStream.bufferedReader().use { it.readText() })
        } catch (e: TranslationException) {
            throw e
        } catch (e: java.io.IOException) {
            throw TranslationException("No connection to DeepL")
        } finally {
            conn.disconnect()
        }
    }
}

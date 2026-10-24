package com.matt.flashcard

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class TranslationException(message: String) : Exception(message)

/** Turns "en"/"hr" style codes into DeepL's upper-case language codes; one request can carry several texts. */
internal fun deepLRequestBody(texts: List<String>, from: String, to: String, formality: Formality = Formality.AUTO): String =
    JSONObject()
        .put("text", JSONArray(texts))
        .put("source_lang", from.uppercase())
        .put("target_lang", deepLTarget(to))
        .apply { formality.deepL?.let { put("formality", it) } }
        .toString()

internal fun deepLRequestBody(text: String, from: String, to: String, formality: Formality = Formality.AUTO): String =
    deepLRequestBody(listOf(text), from, to, formality)

/** DeepL wants a regional variant for these as a target (not as a source). */
private fun deepLTarget(code: String) = when (val up = code.uppercase()) {
    "EN" -> "EN-GB"
    "PT" -> "PT-PT"
    "ZH" -> "ZH-HANS"
    else -> up
}

/** All translations of a request, in order; [expected] is how many texts were sent. */
internal fun parseDeepLTranslations(json: String, expected: Int): List<String> {
    val translations = JSONObject(json).optJSONArray("translations")
    val texts = (0 until (translations?.length() ?: 0)).map { translations!!.optJSONObject(it)?.optString("text").orEmpty() }
    if (texts.size != expected || texts.any { it.isBlank() }) throw TranslationException("DeepL returned no translation")
    return texts
}

internal fun parseDeepLTranslation(json: String): String = parseDeepLTranslations(json, 1).single()

internal fun deepLEndpoint(key: String) =
    if (key.endsWith(":fx")) "https://api-free.deepl.com/v2/translate" else "https://api.deepl.com/v2/translate"

internal fun deepLErrorMessage(code: Int) = when (code) {
    403 -> "DeepL rejected the API key"
    429 -> "Too many requests, try again in a moment"
    456 -> "DeepL monthly character quota used up"
    else -> "DeepL error ($code)"
}

class DeepLClient(private val apiKey: () -> String) {
    suspend fun translate(text: String, from: String, to: String, formality: Formality = Formality.AUTO): String =
        translateMany(listOf(text), from, to, formality).single()

    /** Translates several texts in one request; the result has the same order and size as [texts]. */
    suspend fun translateMany(texts: List<String>, from: String, to: String, formality: Formality = Formality.AUTO): List<String> =
        withContext(Dispatchers.IO) {
            val key = apiKey()
            if (key.isBlank()) throw TranslationException("No DeepL key: add it in Settings")
            val conn = URL(deepLEndpoint(key)).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.connectTimeout = 10_000
                conn.readTimeout = 30_000
                conn.doOutput = true
                conn.setRequestProperty("Authorization", "DeepL-Auth-Key $key")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(deepLRequestBody(texts, from, to, formality).toByteArray()) }
                if (conn.responseCode != 200) throw TranslationException(deepLErrorMessage(conn.responseCode))
                parseDeepLTranslations(conn.inputStream.bufferedReader().use { it.readText() }, texts.size)
            } catch (e: TranslationException) {
                throw e
            } catch (e: java.io.IOException) {
                throw TranslationException("No connection to DeepL")
            } finally {
                conn.disconnect()
            }
        }
}

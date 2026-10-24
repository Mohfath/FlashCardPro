package com.matt.flashcard

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class RomanizationException(message: String) : Exception(message)

private const val PERSIAN_SPELLING =
    " Persian is spoken informally, so write the colloquial pronunciation, not the formal written one (mikhaam, not mikhaaham). " +
        "Always spell the same sound the same way: kh for خ, sh, ch, zh, gh for غ and ق, aa for آ and the long ا, " +
        "oo and ee for long vowels, and short a, e, o. Never use x, q, c, accents or other diacritics. " +
        "Examples: خیر kheyr, خوبی khoobi, می‌خوام mikhaam, " +
        "ممنون mamnoon, سلام salaam, چطوری chetoori."

internal fun romanizationSystemPrompt(languageName: String) =
    "The user is learning $languageName but cannot read its script. Rewrite the text they send in the Latin alphabet, " +
        "as a pronunciation guide a learner can read aloud: use the standard romanization for the language " +
        "(Hepburn for Japanese, Pinyin with tone marks for Chinese, Revised Romanization for Korean) and include the short " +
        "vowels that Arabic and Persian script leaves out. Keep punctuation. If the text is already in Latin letters or is not " +
        "$languageName, repeat it unchanged. Reply with the Latin-letter text only, on one line, and never add quotes, notes, " +
        "explanations or a translation." +
        if (languageName == "Persian") PERSIAN_SPELLING else ""

private const val COLLOQUIAL_PERSIAN_PROMPT =
    "Each numbered item is an English sentence, then \" => \", then its formal written Persian translation. " +
        "Rewrite the Persian the way a native Tehrani speaker would really say it in everyday conversation, in Persian script. " +
        "Use spoken verb forms and endings (می‌خواهم → می‌خوام, می‌کنید → می‌کنین, نمی‌دانم → نمی‌دونم), contractions (است → ه, چیست → چیه, کجاست → کجاس, را → رو), everyday words instead of bookish ones (نام → اسم, آیا dropped), and drop a subject pronoun when speech would. " +
        "Write a contracted ه or ام attached to the previous word, never as a separate word (دوست منه, سالته, دانشجوام). " +
        "Never replace a verb or noun with a different one (نمی‌فهمم stays نمی‌فهمم, never نمی‌دونم). " +
        "The meaning must stay exactly that of the English sentence: " +
        "same tense, person and sense. " +
        "If the Persian translation is wrong or unnatural, correct it (for \"bathroom\" or \"toilet\" use دستشویی, not حمام). " +
        "Keep punctuation and the zero-width non-joiner after می. " +
        "Reply with the same numbers, one line per number, as \"1. persian text\", and nothing else."

/** Parses "1. text" lines back into a list; null unless every one of the [count] lines is there. */
internal fun parseNumberedLines(answer: String, count: Int): List<String>? {
    val byNumber = answer.lines().mapNotNull { Regex("""^\s*(\d+)[.)]\s*(.+)$""").matchEntire(it)?.destructured }
        .associate { (n, t) -> n.toInt() to t.trim() }
    return (1..count).map { byNumber[it] ?: return null }
}

/**
 * Models sometimes answer with a chat reply instead of the reading (for example when the text is not in the language).
 * Anything that is not a short single line is not a reading, so the original text is kept instead.
 */
internal fun cleanReading(input: String, answer: String): String {
    val text = answer.trim()
    val tooLong = text.length > input.length * 4 + 40
    return if (text.isEmpty() || tooLong || '\n' in text) input.trim() else text
}

/** One JSON POST to an AI service; each provider supplies its own url, headers, body and way of reading the answer. */
private suspend fun postForText(
    service: String,
    url: String,
    headers: Map<String, String>,
    body: String,
    errorMessage: (Int) -> String,
    parse: (String) -> String,
): String = withContext(Dispatchers.IO) {
    val conn = URL(url).openConnection() as HttpURLConnection
    try {
        conn.requestMethod = "POST"
        conn.connectTimeout = 10_000
        conn.readTimeout = 30_000
        conn.doOutput = true
        conn.setRequestProperty("content-type", "application/json")
        headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        if (conn.responseCode != 200) throw RomanizationException(errorMessage(conn.responseCode))
        parse(conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
    } catch (e: RomanizationException) {
        throw e
    } catch (e: java.io.IOException) {
        throw RomanizationException("No connection to $service")
    } finally {
        conn.disconnect()
    }
}

private fun genericError(service: String, code: Int) = when (code) {
    400 -> "$service did not accept the request (400)"
    401, 403 -> "$service rejected the API key"
    429 -> "Too many requests to $service, try again in a moment"
    500, 502, 503, 529 -> "$service is busy, try again in a moment"
    else -> "$service error ($code)"
}

// --- Claude (Anthropic) ---

private const val CLAUDE_MODEL = "claude-haiku-4-5-20251001"

internal fun anthropicRequestBody(text: String, languageName: String, system: String = romanizationSystemPrompt(languageName), temperature: Double? = null): String =
    JSONObject()
        .put("model", CLAUDE_MODEL)
        .put("max_tokens", 500)
        .apply { temperature?.let { put("temperature", it) } }
        .put("system", system)
        .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", text)))
        .toString()

internal fun parseAnthropicText(json: String): String {
    val content = JSONObject(json).optJSONArray("content")
    val text = (0 until (content?.length() ?: 0))
        .mapNotNull { content?.optJSONObject(it) }
        .firstOrNull { it.optString("type") == "text" }
        ?.optString("text").orEmpty().trim()
    if (text.isBlank()) throw RomanizationException("Claude returned no text")
    return text
}

internal fun anthropicErrorMessage(code: Int) = genericError("Claude", code)

// --- Gemini (Google) ---

private const val GEMINI_MODEL = "gemini-2.5-flash-lite"

internal fun geminiRequestBody(text: String, languageName: String, system: String = romanizationSystemPrompt(languageName), temperature: Double? = null): String =
    JSONObject()
        .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
        .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", text)))))
        .put("generationConfig", JSONObject().put("maxOutputTokens", 500).apply { temperature?.let { put("temperature", it) } })
        .toString()

internal fun parseGeminiText(json: String): String {
    val parts = JSONObject(json).optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
    val text = (0 until (parts?.length() ?: 0)).joinToString("") { parts!!.optJSONObject(it)?.optString("text").orEmpty() }.trim()
    if (text.isBlank()) throw RomanizationException("Gemini returned no text")
    return text
}

// --- OpenAI ---

private const val OPENAI_MODEL = "gpt-6-luna"

internal fun openAiRequestBody(text: String, languageName: String, system: String = romanizationSystemPrompt(languageName), temperature: Double? = null): String =
    JSONObject()
        .put("model", OPENAI_MODEL)
        .put("max_completion_tokens", 1000)
        .put(
            "messages",
            JSONArray()
                .put(JSONObject().put("role", "system").put("content", system))
                .put(JSONObject().put("role", "user").put("content", text)),
        )
        .toString()

internal fun parseOpenAiText(json: String): String {
    val text = JSONObject(json).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty().trim()
    if (text.isBlank()) throw RomanizationException("OpenAI returned no text")
    return text
}

private suspend fun askAi(settings: AppSettings, text: String, languageName: String, system: String, temperature: Double? = null): String {
    val provider = settings.provider()
    val key = settings.aiKey(provider)
    if (key.isBlank()) throw RomanizationException("No ${provider.label} key: add it in Settings")
    return when (provider) {
        AiProvider.ANTHROPIC -> postForText(
            "Claude", "https://api.anthropic.com/v1/messages",
            mapOf("x-api-key" to key, "anthropic-version" to "2023-06-01"),
            anthropicRequestBody(text, languageName, system, temperature), ::anthropicErrorMessage, ::parseAnthropicText,
        )
        AiProvider.GEMINI -> postForText(
            "Gemini", "https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent",
            mapOf("x-goog-api-key" to key),
            geminiRequestBody(text, languageName, system, temperature), { genericError("Gemini", it) }, ::parseGeminiText,
        )
        AiProvider.OPENAI -> postForText(
            "OpenAI", "https://api.openai.com/v1/chat/completions",
            mapOf("Authorization" to "Bearer $key"),
            openAiRequestBody(text, languageName, system), { genericError("OpenAI", it) }, ::parseOpenAiText,
        )
    }
}

/** Turns text in a non-Latin script into Latin letters, using whichever AI service the learner chose in Settings. */
class Romanizer(private val settings: AppSettings) {
    suspend fun romanize(text: String, languageName: String): String =
        cleanReading(text, askAi(settings, text, languageName, romanizationSystemPrompt(languageName)))

    /** The reverse: Latin letters back into the language's own script, for a Card whose Latin reading was edited. */
    suspend fun toScript(reading: String, languageName: String): String =
        cleanReading(reading, askAi(settings, reading, languageName, scriptFromReadingPrompt(languageName)))
}

private fun scriptFromReadingPrompt(languageName: String) =
    "The user writes $languageName as it is spoken, in Latin letters. Rewrite it in $languageName's own script, keeping the colloquial " +
        "spoken wording and the punctuation. Reply with the script text only, on one line, and never add quotes, notes, explanations or a translation."

/** Rewrites formal written Persian (what DeepL returns) as informal spoken Persian, checked against [originals]; any failure keeps the DeepL lines. */
class PersianColloquializer(private val settings: AppSettings) {
    suspend fun rewrite(originals: List<String>, lines: List<String>): List<String> = lines.indices.chunked(8).flatMap { chunk ->
        try {
            val numbered = chunk.mapIndexed { n, i -> "${n + 1}. ${originals[i]} => ${lines[i]}" }.joinToString("\n")
            parseNumberedLines(askAi(settings, numbered, "Persian", COLLOQUIAL_PERSIAN_PROMPT, temperature = 0.0), chunk.size) ?: chunk.map { lines[it] }
        } catch (e: RomanizationException) {
            chunk.map { lines[it] }
        }
    }
}

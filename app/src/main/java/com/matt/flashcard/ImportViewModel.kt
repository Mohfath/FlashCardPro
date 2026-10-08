package com.matt.flashcard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** One pasted line: [first] alone, or [first] and [second] when the line holds both sides of the Card. */
data class ImportLine(val raw: String, val first: String, val second: String?)

private val PairSeparator = Regex("\t|\\s[-–—]\\s|;|\\|")

/** Blank lines are skipped; a tab, spaced dash, ';' or '|' splits a line into the two sides of a Card. */
internal fun parseImportLines(text: String): List<ImportLine> =
    text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.map { raw ->
        val match = PairSeparator.find(raw)
        if (match != null) {
            val a = raw.substring(0, match.range.first).trim().take(MAX_SOURCE_CHARS)
            val b = raw.substring(match.range.last + 1).trim().take(MAX_SOURCE_CHARS)
            when {
                a.isNotEmpty() && b.isNotEmpty() -> ImportLine(raw, a, b)
                else -> ImportLine(raw, (a.ifEmpty { b }), null)
            }
        } else {
            ImportLine(raw, raw.take(MAX_SOURCE_CHARS), null)
        }
    }.toList()

data class ImportUi(
    val deckName: String = "",
    val sourceLabel: String = "",
    val targetLabel: String = "",
    val sourceFlag: String = "",
    val targetFlag: String = "",
    val text: String = "",
    /** Whether the first (or only) part of each line is in the language being learned. */
    val firstIsTarget: Boolean = false,
    val running: Boolean = false,
    val total: Int = 0,
    val done: Int = 0,
    val failed: Int = 0,
    val withoutAudio: Int = 0,
    val withoutLatin: Int = 0,
    val current: String = "",
    val finished: Boolean = false,
    val note: String? = null,
)

class ImportViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).dao()
    private val audio = AudioStore(app)
    private val settings = AppSettings(app)
    private val deepL = DeepLClient { settings.deepLKey() }
    private val elevenLabs = ElevenLabsClient({ settings.elevenLabsKey() })
    private val romanizer = Romanizer(settings)
    private var deck: Deck? = null
    private var job: Job? = null

    private val _ui = MutableStateFlow<ImportUi?>(null)
    val ui: StateFlow<ImportUi?> = _ui

    fun open(deckId: Long) {
        if (deck?.id == deckId) return
        viewModelScope.launch {
            val d = dao.deck(deckId) ?: return@launch
            deck = d
            _ui.value = ImportUi(
                deckName = d.name,
                sourceLabel = languageName(d.sourceLanguage),
                targetLabel = languageName(d.targetLanguage),
                sourceFlag = languageFor(d.sourceLanguage)?.flag.orEmpty(),
                targetFlag = languageFor(d.targetLanguage)?.flag.orEmpty(),
            )
        }
    }

    fun close() {
        job?.cancel()
        deck = null
        _ui.value = null
    }

    fun setText(text: String) = update { copy(text = text, finished = false, note = null) }

    fun setFirstIsTarget(value: Boolean) = update { copy(firstIsTarget = value) }

    fun cancel() {
        job?.cancel()
    }

    /** Adds the lines one by one; whatever fails or is left over stays in the text box to retry. */
    fun start() {
        val d = deck ?: return
        val current = _ui.value ?: return
        if (current.running) return
        val lines = parseImportLines(current.text)
        if (lines.isEmpty()) return
        update { copy(running = true, total = lines.size, done = 0, failed = 0, withoutAudio = 0, withoutLatin = 0, current = "", finished = false, note = null) }
        job = viewModelScope.launch {
            val left = lines.toMutableList()
            val failedLines = mutableListOf<String>()
            var lastError: String? = null
            try {
                for (line in lines) {
                    update { copy(current = line.first) }
                    try {
                        val (source, target) = resolveSides(d, line, current.firstIsTarget)
                        val audioName = try {
                            audio.save(elevenLabs.synthesize(target, d.targetLanguage))
                        } catch (e: AudioException) {
                            lastError = e.message
                            update { copy(withoutAudio = withoutAudio + 1) }
                            null
                        }
                        val reading = if (!d.romanize) null else try {
                            romanizer.romanize(target, languageFor(d.targetLanguage)?.name ?: d.targetLanguage)
                        } catch (e: RomanizationException) {
                            lastError = e.message
                            update { copy(withoutLatin = withoutLatin + 1) }
                            null
                        }
                        dao.insertCard(
                            Card(deckId = d.id, sourceText = source, targetText = target, targetAudioPath = audioName, romanization = reading),
                        )
                        update { copy(done = done + 1) }
                    } catch (e: TranslationException) {
                        failedLines += line.raw
                        lastError = e.message
                        update { copy(failed = failed + 1) }
                    }
                    left.removeAt(0)
                }
            } catch (e: CancellationException) {
                // fall through to publish what is left
                update { copy(note = "Stopped.") }
            } finally {
                val remaining = (failedLines + left.map { it.raw }).joinToString("\n")
                update {
                    copy(
                        running = false, finished = true, current = "", text = remaining,
                        note = note ?: lastError?.let { "Last problem: $it" },
                    )
                }
            }
        }
    }

    private suspend fun resolveSides(d: Deck, line: ImportLine, firstIsTarget: Boolean): Pair<String, String> {
        if (line.second != null) {
            return if (firstIsTarget) line.second to line.first else line.first to line.second
        }
        return if (firstIsTarget) {
            deepL.translate(line.first, d.targetLanguage, d.sourceLanguage) to line.first
        } else {
            line.first to deepL.translate(line.first, d.sourceLanguage, d.targetLanguage)
        }
    }

    private fun update(block: ImportUi.() -> ImportUi) {
        _ui.value = _ui.value?.block()
    }
}

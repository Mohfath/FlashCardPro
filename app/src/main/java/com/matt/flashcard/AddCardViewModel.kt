package com.matt.flashcard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

const val MAX_SOURCE_CHARS = 280

/** Quiet time after the last keystroke before the other box is translated. */
private const val TRANSLATE_DELAY_MS = 1_000L

enum class Side { SOURCE, TARGET }

data class AddCardUi(
    val deckName: String = "",
    val sourceLabel: String = "",
    val targetLabel: String = "",
    val sourceFlag: String = "",
    val targetFlag: String = "",
    val targetCode: String = "",
    /** The Deck keeps a Latin-letter reading of each Card. */
    val romanize: Boolean = false,
    val romanization: String = "",
    val romanizing: Boolean = false,
    val source: String = "",
    val target: String = "",
    val example: String = "",
    /** The box DeepL is about to fill or is filling, so the screen can show "Translating…" there. */
    val translatingInto: Side? = null,
    /** A box the user typed in themselves is never overwritten by a translation. */
    val sourceTyped: Boolean = false,
    val targetTyped: Boolean = false,
    val savingAudio: Boolean = false,
    /** Set after audio generation failed: the next Add stores the Card without audio. */
    val saveWithoutAudio: Boolean = false,
    val error: String? = null,
) {
    val canSave get() = target.isNotBlank() && source.isNotBlank() && translatingInto == null && !romanizing && !savingAudio
}

class AddCardViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).dao()
    private val audio = AudioStore(app)
    private val settings = AppSettings(app)
    private val deepL = DeepLClient { settings.deepLKey() }
    private val elevenLabs = ElevenLabsClient({ settings.elevenLabsKey() })
    private val romanizer = Romanizer(settings)
    private var deck: Deck? = null
    private var translateJob: Job? = null
    private var romanizeJob: Job? = null

    private val _ui = MutableStateFlow<AddCardUi?>(null)
    val ui: StateFlow<AddCardUi?> = _ui

    fun open(deckId: Long) {
        if (deck?.id == deckId) return
        viewModelScope.launch {
            val d = dao.deck(deckId) ?: return@launch
            deck = d
            _ui.value = AddCardUi(
                deckName = d.name,
                sourceLabel = languageName(d.sourceLanguage),
                targetLabel = languageName(d.targetLanguage),
                sourceFlag = languageFor(d.sourceLanguage)?.flag.orEmpty(),
                targetFlag = languageFor(d.targetLanguage)?.flag.orEmpty(),
                targetCode = d.targetLanguage,
                romanize = d.romanize,
            )
        }
    }

    fun close() {
        translateJob?.cancel()
        romanizeJob?.cancel()
        deck = null
        _ui.value = null
    }

    fun setSource(text: String) {
        update { copy(source = text.take(MAX_SOURCE_CHARS), sourceTyped = text.isNotBlank(), error = null, saveWithoutAudio = false) }
        translateAfterPause(Side.SOURCE)
    }

    fun setTarget(text: String) {
        update { copy(target = text.take(MAX_SOURCE_CHARS), targetTyped = text.isNotBlank(), error = null, saveWithoutAudio = false) }
        translateAfterPause(Side.TARGET)
        romanizeAfter(TRANSLATE_DELAY_MS)
    }

    fun setRomanization(text: String) {
        romanizeJob?.cancel()
        update { copy(romanization = text.take(MAX_SOURCE_CHARS), romanizing = false) }
    }

    /** Writes the target text in Latin letters (Decks that romanize only), unless the user is typing it themselves. */
    private fun romanizeAfter(delayMs: Long) {
        val d = deck ?: return
        if (!d.romanize) return
        romanizeJob?.cancel()
        val text = _ui.value?.target.orEmpty()
        if (text.isBlank()) {
            update { copy(romanizing = false, romanization = "") }
            return
        }
        update { copy(romanizing = true) }
        romanizeJob = viewModelScope.launch {
            delay(delayMs)
            try {
                val result = romanizer.romanize(text.trim(), languageFor(d.targetLanguage)?.name ?: d.targetLanguage).take(MAX_SOURCE_CHARS)
                update { copy(romanization = result, romanizing = false) }
            } catch (e: RomanizationException) {
                update { copy(romanizing = false, error = e.message) }
            }
        }
    }

    fun setExample(text: String) = update { copy(example = text.take(MAX_SOURCE_CHARS)) }

    fun clear() {
        translateJob?.cancel()
        romanizeJob?.cancel()
        update { AddCardUi(deckName, sourceLabel, targetLabel, sourceFlag, targetFlag, targetCode, romanize) }
    }

    /** Fills the box the user did not type in, once they stop typing; a box they typed in is left alone. */
    private fun translateAfterPause(typedIn: Side) {
        val d = deck ?: return
        translateJob?.cancel()
        val current = _ui.value ?: return
        val into = if (typedIn == Side.SOURCE) Side.TARGET else Side.SOURCE
        val text = if (typedIn == Side.SOURCE) current.source else current.target
        val otherTyped = if (into == Side.TARGET) current.targetTyped else current.sourceTyped
        if (text.isBlank() || otherTyped) {
            update { copy(translatingInto = null) }
            return
        }
        update { copy(translatingInto = into) }
        translateJob = viewModelScope.launch {
            delay(TRANSLATE_DELAY_MS)
            val from = if (typedIn == Side.SOURCE) d.sourceLanguage else d.targetLanguage
            val to = if (typedIn == Side.SOURCE) d.targetLanguage else d.sourceLanguage
            try {
                val result = deepL.translate(text.trim(), from, to).take(MAX_SOURCE_CHARS)
                update { if (into == Side.TARGET) copy(target = result, translatingInto = null) else copy(source = result, translatingInto = null) }
                if (into == Side.TARGET) romanizeAfter(0)
            } catch (e: TranslationException) {
                update { copy(translatingInto = null, error = e.message) }
            }
        }
    }

    fun save(onSaved: () -> Unit) {
        val d = deck ?: return
        val current = _ui.value ?: return
        if (!current.canSave) return
        val target = current.target.trim()
        update { copy(savingAudio = true, error = null) }
        viewModelScope.launch {
            var audioName: String? = null
            if (!current.saveWithoutAudio) {
                try {
                    audioName = audio.save(elevenLabs.synthesize(target, d.targetLanguage))
                } catch (e: AudioException) {
                    update { copy(savingAudio = false, saveWithoutAudio = true, error = "${e.message}. Press Add again to add without audio.") }
                    return@launch
                }
            }
            dao.insertCard(
                Card(
                    deckId = d.id, sourceText = current.source.trim(), targetText = target, targetAudioPath = audioName,
                    example = current.example.trim().ifBlank { null },
                    romanization = if (d.romanize) current.romanization.trim().ifBlank { null } else null,
                ),
            )
            update { AddCardUi(deckName, sourceLabel, targetLabel, sourceFlag, targetFlag, targetCode, romanize) }
            onSaved()
        }
    }

    private fun update(block: AddCardUi.() -> AddCardUi) {
        _ui.value = _ui.value?.block()
    }
}

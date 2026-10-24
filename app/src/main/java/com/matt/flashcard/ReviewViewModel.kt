package com.matt.flashcard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ReviewUi(
    val deckName: String,
    val cardId: Long?,
    val box: Int,
    val promptText: String,
    val promptLanguage: String,
    val answerText: String,
    val answerLanguage: String,
    val revealed: Boolean,
    val extraPractice: Boolean,
    val dueRemaining: Int,
    val pendingDelete: Boolean,
    /** Audio file of the Prompt / Answer when that side is the target language and audio exists. */
    val promptAudio: String? = null,
    val answerAudio: String? = null,
    val example: String? = null,
    /** Latin-letter reading of whichever side is the language being learned (Decks that romanize). */
    val promptRomanization: String? = null,
    val answerRomanization: String? = null,
    /** Show only the Latin letters, not the native script. */
    val hideNative: Boolean = false,
    /** Flag emoji of the Prompt / Answer language, for the car screen. */
    val promptFlag: String = "",
    val answerFlag: String = "",
    /** Whether that side is the language being learned (it gets the speaker button, with or without audio yet). */
    val promptIsTarget: Boolean = false,
    val answerIsTarget: Boolean = false,
    /** Audio for the Card on screen is being made. */
    val makingAudio: Boolean = false,
    val audioError: String? = null,
    /** The Card on screen as stored, for the Edit dialog. */
    val sourceText: String = "",
    val targetText: String = "",
    val sourceLabel: String = "",
    val targetLabel: String = "",
    val targetReading: String = "",
)

class ReviewViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).dao()
    private val audio = AudioStore(app)
    private val player = AudioPlayer(app, audio)
    private var session: ReviewSession? = null
    private var deck: Deck? = null

    private val _ui = MutableStateFlow<ReviewUi?>(null)
    val ui: StateFlow<ReviewUi?> = _ui

    fun open(deckId: Long) {
        if (deck?.id == deckId && session != null) return
        close()
        viewModelScope.launch {
            val d = dao.deck(deckId) ?: return@launch
            deck = d
            session = ReviewSession(dao.cardsIn(d.id), { System.currentTimeMillis() })
            publish()
        }
    }

    fun play(audioName: String) = player.play(audioName)

    private val elevenLabs = ElevenLabsClient({ AppSettings(app).elevenLabsKey() })
    private var makingAudio = false
    private var audioError: String? = null

    /** Makes the audio of the Card on screen the first time its speaker is tapped (one ElevenLabs request), then plays it. */
    fun makeAudio() {
        val d = deck ?: return
        val s = session ?: return
        val card = s.current ?: return
        if (makingAudio || card.targetAudioPath != null) return
        makingAudio = true
        audioError = null
        publish()
        viewModelScope.launch {
            try {
                val name = audio.save(elevenLabs.synthesize(card.targetText, d.targetLanguage))
                val updated = card.copy(targetAudioPath = name)
                dao.updateCard(updated)
                s.refreshCurrent(updated)
                makingAudio = false
                publish()
                play(name)
            } catch (e: AudioException) {
                audioError = e.message
                makingAudio = false
                publish()
            }
        }
    }

    override fun onCleared() = player.stop()

    fun close() {
        player.stop()
        session = null
        deck = null
        _ui.value = null
    }

    fun reveal() = act { reveal() }
    fun answer(verdict: Verdict) = act { persist(answer(verdict)) }
    fun delete() = act { persist(delete()) }

    /**
     * Saves new texts for the Card on screen. A changed target text no longer matches its audio or reading, so those are dropped and the
     * reading is made again. For a Deck shown in Latin letters the learner edits the [reading] instead: the script is then rebuilt from it.
     */
    fun edit(source: String, target: String, reading: String? = null) {
        val d = deck ?: return
        val s = session ?: return
        val card = s.current ?: return
        val newSource = source.trim()
        if (newSource.isEmpty() || target.isBlank()) return
        val newReading = reading?.trim()
        if (newReading != null && newReading.isNotEmpty() && newReading != card.romanization.orEmpty()) {
            viewModelScope.launch {
                try {
                    val script = Romanizer(AppSettings(getApplication())).toScript(newReading, languageFor(d.targetLanguage)?.name ?: d.targetLanguage)
                    save(card, d, s, newSource, script.trim(), newReading)
                } catch (e: RomanizationException) {
                    audioError = e.message
                    publish()
                }
            }
        } else {
            save(card, d, s, newSource, target.trim(), null)
        }
    }

    /** [reading] is the Latin reading to keep when it was just written by the learner; null means keep or remake the old one. */
    private fun save(card: Card, d: Deck, s: ReviewSession, newSource: String, newTarget: String, reading: String?) {
        val targetChanged = newTarget != card.targetText
        if (targetChanged) audio.delete(card.targetAudioPath)
        val updated = card.copy(
            sourceText = newSource, targetText = newTarget,
            targetAudioPath = if (targetChanged) null else card.targetAudioPath,
            romanization = reading ?: if (targetChanged) null else card.romanization,
        )
        s.refreshCurrent(updated)
        audioError = null
        publish()
        viewModelScope.launch {
            dao.updateCard(updated)
            if (targetChanged && reading == null && d.romanize) {
                try {
                    val remade = Romanizer(AppSettings(getApplication())).romanize(newTarget, languageFor(d.targetLanguage)?.name ?: d.targetLanguage)
                    val withReading = updated.copy(romanization = remade)
                    dao.updateCard(withReading)
                    if (s.current?.id == withReading.id) {
                        s.refreshCurrent(withReading)
                        publish()
                    }
                } catch (e: RomanizationException) {
                    audioError = e.message
                    publish()
                }
            }
        }
    }

    private fun act(block: ReviewSession.() -> Unit) {
        audioError = null
        session?.block()
        publish()
    }

    private fun persist(change: Change?) {
        when (change) {
            is Change.Update -> viewModelScope.launch {
                dao.updateCard(change.card)
                dao.recordPractice()
            }
            is Change.Remove -> viewModelScope.launch {
                dao.deleteCard(change.card)
                audio.delete(change.card.targetAudioPath)
            }
            null -> Unit
        }
    }

    private fun publish() {
        val s = session ?: return
        val d = deck ?: return
        _ui.value = buildReviewUi(d, s).copy(makingAudio = makingAudio, audioError = audioError)
    }
}

/** What the learner should see for the session's current Card; shared by the phone and the car. */
internal fun buildReviewUi(d: Deck, s: ReviewSession): ReviewUi {
    val card = s.current
        ?: return ReviewUi(d.name, null, 0, "", "", "", "", false, false, 0, false)
    val srcToTarget = s.direction == Direction.SOURCE_TO_TARGET
    return ReviewUi(
        deckName = d.name,
        cardId = card.id,
        box = card.box,
        promptText = if (srcToTarget) card.sourceText else card.targetText,
        promptLanguage = languageLabel(if (srcToTarget) d.sourceLanguage else d.targetLanguage),
        answerText = if (srcToTarget) card.targetText else card.sourceText,
        answerLanguage = languageLabel(if (srcToTarget) d.targetLanguage else d.sourceLanguage),
        revealed = s.revealed,
        extraPractice = s.extraPractice,
        dueRemaining = s.dueRemaining,
        pendingDelete = s.pendingDelete,
        promptAudio = if (srcToTarget) null else card.targetAudioPath,
        answerAudio = if (srcToTarget) card.targetAudioPath else null,
        example = card.example,
        promptRomanization = if (srcToTarget) null else card.romanization,
        answerRomanization = if (srcToTarget) card.romanization else null,
        hideNative = d.readingMode == ReadingMode.LATIN,
        promptFlag = languageFor(if (srcToTarget) d.sourceLanguage else d.targetLanguage)?.flag.orEmpty(),
        answerFlag = languageFor(if (srcToTarget) d.targetLanguage else d.sourceLanguage)?.flag.orEmpty(),
        promptIsTarget = !srcToTarget,
        answerIsTarget = srcToTarget,
        sourceText = card.sourceText,
        targetText = card.targetText,
        sourceLabel = languageLabel(d.sourceLanguage),
        targetLabel = languageLabel(d.targetLanguage),
        targetReading = card.romanization.orEmpty(),
    )
}

private fun languageLabel(code: String) = when (code) {
    "hr" -> "HRVATSKI"
    else -> languageName(code)
}

/** The Prompt / Answer text as the deck wants it shown: the Latin reading instead of the script for Latin-only decks. */
internal val ReviewUi.promptShown: String get() = promptRomanization?.takeIf { hideNative } ?: promptText
internal val ReviewUi.answerShown: String get() = answerRomanization?.takeIf { hideNative } ?: answerText

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
    val redoingAudio: Boolean = false,
    val audioError: String? = null,
)

class ReviewViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).dao()
    private val audio = AudioStore(app)
    private val player = AudioPlayer(app, audio)
    private val elevenLabs = ElevenLabsClient({ AppSettings(app).elevenLabsKey() })
    private var redoingAudio = false
    private var audioError: String? = null
    private val prefs = app.getSharedPreferences("ui", android.content.Context.MODE_PRIVATE)
    private var hideNative = prefs.getBoolean("hideNative", false)
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

    /** Makes the audio of the Card on screen again, e.g. after a voice or model change; the old file is deleted. */
    fun redoAudio() {
        val d = deck ?: return
        val s = session ?: return
        val card = s.current ?: return
        if (redoingAudio) return
        redoingAudio = true
        audioError = null
        publish()
        viewModelScope.launch {
            try {
                val name = audio.save(elevenLabs.synthesize(card.targetText, d.targetLanguage))
                val updated = card.copy(targetAudioPath = name)
                dao.updateCard(updated)
                audio.delete(card.targetAudioPath)
                s.refreshCurrent(updated)
                redoingAudio = false
                publish()
                play(name)
            } catch (e: AudioException) {
                audioError = e.message
                redoingAudio = false
                publish()
            }
        }
    }

    fun toggleNative() {
        hideNative = !hideNative
        prefs.edit().putBoolean("hideNative", hideNative).apply()
        publish()
    }

    private fun act(block: ReviewSession.() -> Unit) {
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
        _ui.value = buildReviewUi(d, s, hideNative).copy(redoingAudio = redoingAudio, audioError = audioError)
    }
}

/** What the learner should see for the session's current Card; shared by the phone and the car. */
internal fun buildReviewUi(d: Deck, s: ReviewSession, hideNative: Boolean = false): ReviewUi {
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
        hideNative = hideNative,
    )
}

private fun languageLabel(code: String) = when (code) {
    "hr" -> "HRVATSKI"
    else -> languageName(code)
}

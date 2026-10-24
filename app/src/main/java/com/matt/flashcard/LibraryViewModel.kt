package com.matt.flashcard

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LibraryUi(
    val deckName: String = "",
    val sourceLabel: String = "",
    val targetLabel: String = "",
    val sourceFlag: String = "",
    val targetFlag: String = "",
    /** libraryIds of the Library Cards this Deck already holds. */
    val addedIds: Set<String> = emptySet(),
    /** The category being copied right now, if any. */
    val copyingCategoryId: String? = null,
    val note: String? = null,
    val noteIsError: Boolean = false,
)

private const val ENGLISH = "en"

class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).dao()
    private val settings = AppSettings(app)
    private val deepL = DeepLClient({ settings.deepLKey() }, PersianColloquializer(settings))
    private val romanizer = Romanizer(settings)
    private var deck: Deck? = null
    private var job: Job? = null
    private var idsJob: Job? = null
    private var screenOpen = false

    private val _ui = MutableStateFlow<LibraryUi?>(null)
    val ui: StateFlow<LibraryUi?> = _ui

    fun open(deckId: Long) {
        screenOpen = true
        if (deck?.id == deckId) {
            observeAddedIds(deckId)
            return
        }
        viewModelScope.launch {
            val d = dao.deck(deckId) ?: return@launch
            deck = d
            _ui.value = LibraryUi(
                deckName = d.name,
                sourceLabel = languageName(d.sourceLanguage),
                targetLabel = languageName(d.targetLanguage),
                sourceFlag = languageFor(d.sourceLanguage)?.flag.orEmpty(),
                targetFlag = languageFor(d.targetLanguage)?.flag.orEmpty(),
            )
            observeAddedIds(deckId)
        }
    }

    private fun observeAddedIds(deckId: Long) {
        idsJob?.cancel()
        idsJob = viewModelScope.launch { dao.libraryIdsIn(deckId).collect { ids -> update { copy(addedIds = ids.toSet()) } } }
    }

    /** Leaving the Library does not stop a copy in progress: it finishes in the background and the screen picks it up when reopened. */
    fun close() {
        screenOpen = false
        idsJob?.cancel()
        if (job?.isActive != true) reset()
    }

    private fun reset() {
        deck = null
        _ui.value = null
    }

    fun dismissNote() = update { copy(note = null, noteIsError = false) }

    /** Translates the Cards of [category] the Deck does not have yet, into the Deck's two languages, and adds them. Adds nothing if a translation fails. */
    fun copy(category: LibraryCategory) {
        val d = deck ?: return
        val current = _ui.value ?: return
        if (current.copyingCategoryId != null) return
        val missing = missingEntries(category, current.addedIds)
        if (missing.isEmpty()) return
        updateFor(d.id) { copy(copyingCategoryId = category.id, note = null, noteIsError = false) }
        job = viewModelScope.launch {
            try {
                val english = missing.map { it.english }
                val sources = translateInto(english, d.sourceLanguage, d)
                val targets = translateInto(english, d.targetLanguage, d)
                val language = languageFor(d.targetLanguage)?.name ?: d.targetLanguage
                var withoutLatin = 0
                val cards = missing.indices.map { i ->
                    val reading = if (!d.romanize) null else try {
                        romanizer.romanize(targets[i], language)
                    } catch (e: RomanizationException) {
                        withoutLatin++
                        null
                    }
                    Card(deckId = d.id, sourceText = sources[i], targetText = targets[i], romanization = reading, libraryId = missing[i].libraryId)
                }
                dao.insertCards(cards)
                updateFor(d.id) {
                    copy(
                        note = "Added ${cards.size} card${if (cards.size == 1) "" else "s"}" +
                            if (withoutLatin > 0) ". $withoutLatin without Latin letters, check your AI key in Settings." else ".",
                        noteIsError = false,
                    )
                }
            } catch (e: TranslationException) {
                updateFor(d.id) { copy(note = e.message, noteIsError = true) }
            } catch (e: CancellationException) {
                throw e
            } finally {
                updateFor(d.id) { copy(copyingCategoryId = null) }
                if (!screenOpen && deck?.id == d.id) {
                    val note = _ui.value?.note
                    reset()
                    note?.let { Toast.makeText(getApplication(), it, Toast.LENGTH_LONG).show() }
                }
            }
        }
    }

    /** English is the language of the Library itself, so it is used as written; everything else goes through DeepL in one request. */
    private suspend fun translateInto(english: List<String>, language: String, d: Deck): List<String> =
        if (language == ENGLISH) english else deepL.translateMany(english, ENGLISH, language, d.formality)

    private fun update(block: LibraryUi.() -> LibraryUi) {
        _ui.value = _ui.value?.block()
    }

    /** For the copy job, which may outlive the screen: changes the state only while it still belongs to the Deck being copied. */
    private fun updateFor(deckId: Long, block: LibraryUi.() -> LibraryUi) {
        if (deck?.id == deckId) update(block)
    }
}

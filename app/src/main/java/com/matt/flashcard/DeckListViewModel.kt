package com.matt.flashcard

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DeckListViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).dao()
    private val prefs = app.getSharedPreferences("ui", Context.MODE_PRIVATE)

    /** The Deck shown on the first screen; -1 until one is picked (the screen then falls back to the first Deck). */
    val selectedDeckId = MutableStateFlow(prefs.getLong("activeDeck", -1L))

    fun select(id: Long) {
        selectedDeckId.value = id
        prefs.edit().putLong("activeDeck", id).apply()
    }

    /** Null until the first load, so the screen can tell "loading" from "no Decks". */
    val decks: StateFlow<List<DeckSummary>?> =
        combine(dao.decks(), dao.allCards()) { decks, cards ->
            summarize(decks, cards, System.currentTimeMillis())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The last seven days of practice, oldest first. */
    val week: StateFlow<List<DayCount>> =
        dao.practiceSince(todayEpochDay() - 6).map { rows -> buildWeek(rows, todayEpochDay()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), buildWeek(emptyList(), todayEpochDay()))

    init {
        viewModelScope.launch { dao.seedIfEmpty() }
    }

    /** Removes the Deck; its Cards go with it (cascade), and so do their audio files. */
    fun deleteDeck(deck: Deck) {
        viewModelScope.launch {
            val audio = AudioStore(getApplication())
            dao.cardsIn(deck.id).forEach { audio.delete(it.targetAudioPath) }
            dao.deleteDeck(deck)
        }
    }

    fun createDeck(name: String, sourceLanguage: String, targetLanguage: String, romanize: Boolean) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            select(dao.insertDeck(Deck(name = trimmed, sourceLanguage = sourceLanguage, targetLanguage = targetLanguage, romanize = romanize)))
        }
    }
}

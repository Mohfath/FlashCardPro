package com.matt.flashcard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckListLogicTest {
    private fun summary(id: Long) = DeckSummary(Deck(id = id, name = "d$id"), total = 0, due = 0, boxCounts = List(5) { 0 })

    @Test fun pageIndexFindsTheSelectedDeck() {
        val decks = listOf(summary(10), summary(20), summary(30))
        assertEquals(0, pageIndexOf(decks, 10))
        assertEquals(2, pageIndexOf(decks, 30))
    }

    @Test fun pageIndexFallsBackToTheFirstPage() {
        val decks = listOf(summary(10), summary(20))
        assertEquals(0, pageIndexOf(decks, -1))
        assertEquals(0, pageIndexOf(emptyList(), 5))
    }

    @Test fun onlyCardsWithoutAudioAreListed() {
        val cards = listOf(
            Card(id = 1, deckId = 1, sourceText = "a", targetText = "b", targetAudioPath = "x.mp3"),
            Card(id = 2, deckId = 1, sourceText = "c", targetText = "d"),
            Card(id = 3, deckId = 1, sourceText = "e", targetText = "f"),
        )
        assertEquals(listOf(2L, 3L), cardsWithoutAudio(cards).map { it.id })
    }

    @Test fun summaryCountsCardsWithoutAudio() {
        val deck = Deck(id = 1, name = "d")
        val cards = listOf(
            Card(id = 1, deckId = 1, sourceText = "a", targetText = "b", targetAudioPath = "x.mp3"),
            Card(id = 2, deckId = 1, sourceText = "c", targetText = "d"),
        )
        assertEquals(1, summarize(listOf(deck), cards, 0L).single().missingAudio)
    }

    @Test fun emptyDeckCannotBeReviewed() {
        val action = reviewAction(total = 0, due = 0)
        assertFalse(action.enabled)
        assertEquals("No cards yet", action.label)
    }

    @Test fun dueCardsShowTheCountOnTheButton() {
        val action = reviewAction(total = 8, due = 3)
        assertTrue(action.enabled)
        assertFalse(action.extraPractice)
        assertEquals("Review · 3 due", action.label)
    }

    @Test fun nothingDueMeansExtraPractice() {
        val action = reviewAction(total = 8, due = 0)
        assertTrue(action.enabled && action.extraPractice)
        assertEquals("Extra practice", action.label)
    }

    @Test fun everyHelpSectionHasTextAndTitle() {
        assertTrue(helpSections.isNotEmpty())
        helpSections.forEach { assertTrue(it.title.isNotBlank() && it.lines.isNotEmpty() && it.lines.all { l -> l.isNotBlank() }) }
    }
}

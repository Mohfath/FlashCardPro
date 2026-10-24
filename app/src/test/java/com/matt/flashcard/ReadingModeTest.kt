package com.matt.flashcard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingModeTest {
    private val card = Card(id = 1, deckId = 1, sourceText = "I am", targetText = "هستم", romanization = "hastam")

    private fun reviewUi(mode: ReadingMode) = buildReviewUi(Deck(name = "p").withReadingMode(mode), ReviewSession(listOf(card), { 0L }))

    @Test fun modeIsReadFromTheTwoDeckFlags() {
        assertEquals(ReadingMode.SCRIPT, Deck(name = "a").readingMode)
        assertEquals(ReadingMode.LATIN, Deck(name = "a", romanize = true).readingMode)
        assertEquals(ReadingMode.LATIN, Deck(name = "a", romanize = true, latinOnly = true).readingMode)
    }

    @Test fun withReadingModeRoundTrips() {
        ReadingMode.entries.forEach { assertEquals(it, Deck(name = "a").withReadingMode(it).readingMode) }
    }

    @Test fun latinOnlyDeckHidesTheScriptOnTheReviewCard() {
        assertTrue(reviewUi(ReadingMode.LATIN).hideNative)
    }

    @Test fun scriptDecksKeepTheScript() {
        assertFalse(reviewUi(ReadingMode.SCRIPT).hideNative)
    }

    @Test fun thereAreOnlyTwoAlphabetsToChooseFrom() {
        assertEquals(listOf(ReadingMode.SCRIPT, ReadingMode.LATIN), ReadingMode.entries)
    }

    @Test fun carTextUsesLatinForLatinOnlyDecks() {
        val ui = ReviewUi("d", 1, 1, "هستم", "PERSIAN", "I am", "ENGLISH", true, false, 1, false, promptRomanization = "hastam", hideNative = true)
        assertEquals("hastam", ui.promptShown)
        assertEquals("I am", ui.answerShown)
        assertEquals("هستم", ui.copy(hideNative = false).promptShown)
    }
}

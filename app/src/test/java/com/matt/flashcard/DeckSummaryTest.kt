package com.matt.flashcard

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class DeckSummaryTest {
    private val now = 1_000_000_000_000L
    private val deckA = Deck(id = 1, name = "A")
    private val deckB = Deck(id = 2, name = "B")

    private fun card(id: Long, deckId: Long, box: Int, reviewedDaysAgo: Long? = null) = Card(
        id = id, deckId = deckId, sourceText = "s", targetText = "t", box = box,
        lastReviewedAt = reviewedDaysAgo?.let { now - TimeUnit.DAYS.toMillis(it) },
    )

    @Test fun countsTotalsDueAndBoxesPerDeck() {
        val cards = listOf(
            card(1, 1, box = 1),                       // never reviewed: due
            card(2, 1, box = 3, reviewedDaysAgo = 1),  // box 3 waits 4 days: not due
            card(3, 1, box = 3, reviewedDaysAgo = 5),  // due
            card(4, 2, box = 5, reviewedDaysAgo = 0),  // not due
        )
        val (a, b) = summarize(listOf(deckA, deckB), cards, now)
        assertEquals(3, a.total)
        assertEquals(2, a.due)
        assertEquals(listOf(1, 0, 2, 0, 0), a.boxCounts)
        assertEquals(1, b.total)
        assertEquals(0, b.due)
        assertEquals(listOf(0, 0, 0, 0, 1), b.boxCounts)
    }

    @Test fun emptyDeckHasZeroes() {
        val s = summarize(listOf(deckA), emptyList(), now).single()
        assertEquals(0, s.total)
        assertEquals(0, s.due)
        assertEquals(listOf(0, 0, 0, 0, 0), s.boxCounts)
    }
}

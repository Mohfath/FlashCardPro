package com.matt.flashcard

data class DeckSummary(
    val deck: Deck,
    val total: Int,
    val due: Int,
    /** Cards per Box, index 0 = Box 1. */
    val boxCounts: List<Int>,
)

fun summarize(decks: List<Deck>, cards: List<Card>, now: Long): List<DeckSummary> {
    val byDeck = cards.groupBy { it.deckId }
    return decks.map { deck ->
        val own = byDeck[deck.id].orEmpty()
        DeckSummary(
            deck = deck,
            total = own.size,
            due = own.count { Leitner.isDue(it.box, it.lastReviewedAt, now) },
            boxCounts = (Leitner.MIN_BOX..Leitner.MAX_BOX).map { b -> own.count { it.box == b } },
        )
    }
}

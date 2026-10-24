package com.matt.flashcard

/** Which carousel page shows the selected Deck; the first page when nothing matches. */
fun pageIndexOf(decks: List<DeckSummary>, selectedId: Long): Int =
    decks.indexOfFirst { it.deck.id == selectedId }.coerceAtLeast(0)

/** Cards that have no spoken audio yet. */
fun cardsWithoutAudio(cards: List<Card>): List<Card> = cards.filter { it.targetAudioPath == null }

/** The main button of a Deck card: what it says and whether it can be pressed. */
data class ReviewAction(val label: String, val enabled: Boolean, val extraPractice: Boolean)

fun reviewAction(total: Int, due: Int): ReviewAction = when {
    total == 0 -> ReviewAction("No cards yet", enabled = false, extraPractice = false)
    due > 0 -> ReviewAction("Review · $due due", enabled = true, extraPractice = false)
    else -> ReviewAction("Extra practice", enabled = true, extraPractice = true)
}

/** Progress of making audio for the Cards of a Deck that have none. */
data class MissingAudio(val deckId: Long, val done: Int, val total: Int, val finished: Boolean = false, val error: String? = null)

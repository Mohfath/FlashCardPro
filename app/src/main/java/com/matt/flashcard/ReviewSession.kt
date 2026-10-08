package com.matt.flashcard

import kotlin.random.Random

enum class Direction { SOURCE_TO_TARGET, TARGET_TO_SOURCE }

/** What the learner needs persisted after an action. */
sealed interface Change {
    data class Update(val card: Card) : Change
    data class Remove(val card: Card) : Change
}

class ReviewSession(
    cards: List<Card>,
    private val now: () -> Long,
    private val random: Random = Random.Default,
) {
    private val due = ArrayDeque(cards.filter { Leitner.isDue(it.box, it.lastReviewedAt, now()) }.shuffled(random))
    private val notDue = cards.filterNot { c -> due.any { it.id == c.id } }.toMutableList()
    private var extra = ArrayDeque<Card>()

    var current: Card? = null
        private set
    var direction: Direction = Direction.SOURCE_TO_TARGET
        private set
    var revealed = false
        private set
    var pendingDelete = false
        private set
    var extraPractice = false
        private set
    val dueRemaining get() = due.size + if (current != null && !extraPractice) 1 else 0

    init { advance() }

    private fun advance() {
        revealed = false
        pendingDelete = false
        if (due.isEmpty() && extra.isEmpty() && notDue.isNotEmpty()) {
            extra = ArrayDeque(notDue.shuffled(random))
            notDue.clear()
        }
        extraPractice = due.isEmpty() && extra.isNotEmpty()
        current = if (due.isNotEmpty()) due.removeFirst() else extra.removeFirstOrNull()
        direction = if (random.nextBoolean()) Direction.SOURCE_TO_TARGET else Direction.TARGET_TO_SOURCE
    }

    fun reveal() {
        if (current != null) revealed = true
    }

    /** Know / Didn't know. Only valid after Reveal. Extra practice never moves Boxes. */
    fun answer(verdict: Verdict): Change? {
        val card = current ?: return null
        if (!revealed) return null
        val change = if (extraPractice) {
            notDue.add(card)
            null
        } else {
            val updated = card.copy(box = Leitner.nextBox(card.box, verdict), lastReviewedAt = now())
            // No longer Due, so it joins the pool Extra practice draws from once nothing is Due.
            notDue.add(updated)
            Change.Update(updated)
        }
        advance()
        return change
    }

    /** Swaps in a newer copy of the Card being shown (for example after its audio was redone). */
    fun refreshCurrent(updated: Card) {
        if (current?.id == updated.id) current = updated
    }

    /** First call asks for confirmation; the second call deletes. */
    fun delete(): Change? {
        val card = current ?: return null
        if (!pendingDelete) {
            pendingDelete = true
            return null
        }
        advance()
        return Change.Remove(card)
    }
}

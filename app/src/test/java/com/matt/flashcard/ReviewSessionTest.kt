package com.matt.flashcard

import java.util.concurrent.TimeUnit
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewSessionTest {
    private val now = 1_000_000_000_000L
    private fun card(id: Long, box: Int = 1, reviewedDaysAgo: Long? = null) = Card(
        id = id, deckId = 1, sourceText = "s$id", targetText = "t$id", box = box,
        lastReviewedAt = reviewedDaysAgo?.let { now - TimeUnit.DAYS.toMillis(it) },
    )
    private fun session(vararg cards: Card) = ReviewSession(cards.toList(), { now }, Random(1))

    @Test fun startsOnADueCardUnrevealed() {
        val s = session(card(1))
        assertEquals(1L, s.current?.id)
        assertFalse(s.revealed)
        assertFalse(s.extraPractice)
    }

    @Test fun verdictBeforeRevealDoesNothing() {
        val s = session(card(1))
        assertNull(s.answer(Verdict.KNOW))
        assertEquals(1L, s.current?.id)
    }

    @Test fun knowMovesUpAndStampsTime() {
        val s = session(card(1, box = 2))
        s.reveal()
        val change = s.answer(Verdict.KNOW) as Change.Update
        assertEquals(3, change.card.box)
        assertEquals(now, change.card.lastReviewedAt)
        // Nothing else is Due, so the session carries on with Extra practice.
        assertTrue(s.extraPractice)
        assertEquals(1L, s.current?.id)
    }

    @Test fun didntKnowResetsToBoxOne() {
        val s = session(card(1, box = 4))
        s.reveal()
        assertEquals(1, (s.answer(Verdict.DIDNT_KNOW) as Change.Update).card.box)
    }

    @Test fun dueCardsComeBeforeNotDueOnes() {
        val s = session(card(1, box = 3, reviewedDaysAgo = 0), card(2))
        assertEquals(2L, s.current?.id)
        assertEquals(1, s.dueRemaining)
    }

    @Test fun whenNothingIsDueExtraPracticeStartsAndDoesNotMoveBoxes() {
        val s = session(card(1, box = 3, reviewedDaysAgo = 0))
        assertTrue(s.extraPractice)
        s.reveal()
        assertNull(s.answer(Verdict.KNOW))
    }

    @Test fun extraPracticeFollowsLastDueCard() {
        val s = session(card(1), card(2, box = 3, reviewedDaysAgo = 0))
        s.reveal(); s.answer(Verdict.KNOW)
        assertEquals(2L, s.current?.id)
        assertTrue(s.extraPractice)
    }

    @Test fun extraPracticeStartsEvenWhenEveryCardWasDueAtTheStart() {
        val s = session(card(1), card(2))
        repeat(2) { s.reveal(); s.answer(Verdict.KNOW) }
        assertTrue(s.current != null)
        assertTrue(s.extraPractice)
    }

    @Test fun extraPracticeCardsKeepCirculating() {
        val s = session(card(1))
        s.reveal(); s.answer(Verdict.KNOW)
        repeat(3) {
            assertEquals(1L, s.current?.id)
            assertTrue(s.extraPractice)
            s.reveal(); assertNull(s.answer(Verdict.KNOW))
        }
    }

    @Test fun deletedCardLeavesExtraPracticePool() {
        val s = session(card(1), card(2))
        s.reveal(); s.answer(Verdict.KNOW)
        s.reveal(); s.answer(Verdict.KNOW)
        val shown = s.current!!.id
        s.delete()
        s.delete()
        assertEquals(setOf(1L, 2L) - shown, setOfNotNull(s.current?.id))
    }

    @Test fun deleteNeedsASecondPress() {
        val s = session(card(1), card(2))
        val first = s.current!!
        assertNull(s.delete())
        assertTrue(s.pendingDelete)
        assertEquals(first.id, (s.delete() as Change.Remove).card.id)
        assertFalse(s.pendingDelete)
    }

    @Test fun movingOnClearsPendingDelete() {
        val s = session(card(1), card(2))
        s.delete()
        s.reveal(); s.answer(Verdict.KNOW)
        assertFalse(s.pendingDelete)
    }

    @Test fun directionVariesAcrossCards() {
        val s = ReviewSession((1L..40L).map { card(it) }, { now }, Random(7))
        val seen = mutableSetOf<Direction>()
        repeat(40) { seen += s.direction; s.reveal(); s.answer(Verdict.KNOW) }
        assertEquals(setOf(Direction.SOURCE_TO_TARGET, Direction.TARGET_TO_SOURCE), seen)
    }
}

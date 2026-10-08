package com.matt.flashcard

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeitnerTest {
    private val now = 1_000_000_000_000L
    private fun daysAgo(d: Long) = now - TimeUnit.DAYS.toMillis(d)

    @Test fun neverReviewedCardIsDue() = assertTrue(Leitner.isDue(1, null, now))

    @Test fun knowMovesUpOneBox() = assertEquals(3, Leitner.nextBox(2, Verdict.KNOW))

    @Test fun knowStopsAtTopBox() = assertEquals(5, Leitner.nextBox(5, Verdict.KNOW))

    @Test fun didntKnowResetsToBoxOne() = assertEquals(1, Leitner.nextBox(4, Verdict.DIDNT_KNOW))

    @Test fun boxOneDueAfterOneDay() {
        assertFalse(Leitner.isDue(1, daysAgo(0), now))
        assertTrue(Leitner.isDue(1, daysAgo(1), now))
    }

    @Test fun boxFiveWaitsSixteenDays() {
        assertFalse(Leitner.isDue(5, daysAgo(15), now))
        assertTrue(Leitner.isDue(5, daysAgo(16), now))
    }
}

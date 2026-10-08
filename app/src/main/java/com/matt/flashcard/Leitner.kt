package com.matt.flashcard

import java.util.concurrent.TimeUnit

enum class Verdict { KNOW, DIDNT_KNOW }

object Leitner {
    const val MIN_BOX = 1
    const val MAX_BOX = 5

    private val waitDays = mapOf(1 to 1L, 2 to 2L, 3 to 4L, 4 to 8L, 5 to 16L)

    /** A Card never reviewed is Due immediately. */
    fun isDue(box: Int, lastReviewedAt: Long?, now: Long): Boolean {
        if (lastReviewedAt == null) return true
        val wait = TimeUnit.DAYS.toMillis(waitDays.getValue(box))
        return now - lastReviewedAt >= wait
    }

    fun nextBox(box: Int, verdict: Verdict): Int = when (verdict) {
        Verdict.KNOW -> minOf(box + 1, MAX_BOX)
        Verdict.DIDNT_KNOW -> MIN_BOX
    }
}

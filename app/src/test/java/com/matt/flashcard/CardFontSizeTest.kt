package com.matt.flashcard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardFontSizeTest {
    @Test fun singleWordsAreBig() = assertEquals(48, cardFontSize("midunam".length))

    @Test fun longerTextGetsSmaller() {
        val sizes = listOf(5, 20, 45, 120).map(::cardFontSize)
        assertEquals(sizes.sortedDescending(), sizes)
        assertTrue(sizes.first() > sizes.last())
    }

    @Test fun neverSmallerThanTheOldSize() = assertTrue(cardFontSize(500) >= 24)
}

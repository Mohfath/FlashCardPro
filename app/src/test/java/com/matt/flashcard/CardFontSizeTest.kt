package com.matt.flashcard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardFontSizeTest {
    @Test fun shortWordsAreBig() = assertEquals(48, cardFontSize("midunam"))

    @Test fun longerTextGetsSmaller() {
        val sizes = listOf("Hi", "How are you today?", "Could you please tell me where the station is?", "x ".repeat(60)).map(::cardFontSize)
        assertEquals(sizes.sortedDescending(), sizes)
        assertTrue(sizes.first() > sizes.last())
    }

    @Test fun aLongWordGetsSmallerSoItDoesNotBreak() {
        assertTrue(cardFontSize("Nachmittag") < 48)
        assertTrue(cardFontSize("Entschuldigung") < cardFontSize("Nachmittag"))
    }

    @Test fun neverSmallerThanTheFloor() = assertTrue(cardFontSize("Donaudampfschifffahrtsgesellschaft " + "word ".repeat(40)) >= 22)

    @Test fun roomForTheSpeakerButtonMakesLongWordsSmaller() {
        assertTrue(cardFontSize("understand.", speakerShown = true) < cardFontSize("understand.", speakerShown = false))
    }

    @Test fun elevenLetterWordFitsWithoutBreaking() {
        // 11 letters of a bold font (0.72 em each) must fit in 250 dp
        assertTrue(cardFontSize("understand.") * 0.72 * 11 <= 250)
    }
}

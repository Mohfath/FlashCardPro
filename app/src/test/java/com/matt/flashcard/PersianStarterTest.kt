package com.matt.flashcard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersianStarterTest {
    @Test fun everyCardHasAllThreeParts() {
        persianStarterCards.forEach {
            assertTrue("blank part in $it", it.english.isNotBlank() && it.persian.isNotBlank() && it.latin.isNotBlank())
        }
    }

    @Test fun noCardIsListedTwice() {
        val keys = persianStarterCards.map { it.english to it.persian }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test fun persianSideIsPersianScript() {
        persianStarterCards.forEach { card ->
            assertTrue("not Persian script: $card", card.persian.any { it in '؀'..'ۿ' })
            assertTrue("Latin reading has Persian script: $card", card.latin.none { it in '؀'..'ۿ' })
        }
    }

    @Test fun thereAreExactlyThirtyCards() = assertEquals(30, persianStarterCards.size)
}

package com.matt.flashcard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LanguagesTest {
    @Test fun listIsAlphabetical() {
        assertEquals(Languages.map { it.name }.sorted(), Languages.map { it.name })
    }

    @Test fun persianIsThere() {
        assertNotNull(languageFor("fa"))
    }
}

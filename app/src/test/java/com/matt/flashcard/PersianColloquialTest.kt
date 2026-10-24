package com.matt.flashcard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersianColloquialTest {
    @Test fun persianPromptFixesKhAndExamples() {
        val prompt = romanizationSystemPrompt("Persian")
        assertTrue("kheyr" in prompt && "Never use x" in prompt)
    }

    @Test fun otherLanguagesKeepTheGenericPrompt() {
        assertFalse("kheyr" in romanizationSystemPrompt("Japanese"))
    }

    @Test fun numberedAnswerIsParsedInOrder() {
        assertEquals(listOf("a", "b"), parseNumberedLines("1. a\n2) b", 2))
    }

    @Test fun missingLineMeansNoRewrite() {
        assertNull(parseNumberedLines("1. a", 2))
    }

    @Test fun customSystemPromptIsSent() {
        assertTrue("SYS" in anthropicRequestBody("x", "Persian", "SYS"))
    }
}

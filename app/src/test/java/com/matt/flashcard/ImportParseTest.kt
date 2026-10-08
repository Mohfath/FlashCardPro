package com.matt.flashcard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImportParseTest {
    @Test fun blankLinesAreSkipped() {
        assertEquals(listOf("Hello", "Thanks"), parseImportLines("Hello\n\n  \nThanks\n").map { it.first })
    }

    @Test fun singleTextHasNoSecondSide() {
        val line = parseImportLines("Good morning").single()
        assertEquals("Good morning", line.first)
        assertNull(line.second)
    }

    @Test fun pairsSplitOnTabDashSemicolonAndBar() {
        val lines = parseImportLines("Yes\tDa\nNo - Ne\nPlease; Molim\nThanks | Hvala")
        assertEquals(listOf("Yes" to "Da", "No" to "Ne", "Please" to "Molim", "Thanks" to "Hvala"), lines.map { it.first to it.second })
    }

    @Test fun hyphenInsideAWordDoesNotSplit() {
        assertNull(parseImportLines("well-known").single().second)
    }

    @Test fun lineWithEmptySideFallsBackToSingle() {
        val line = parseImportLines("Hello;").single()
        assertEquals("Hello", line.first)
        assertNull(line.second)
    }
}

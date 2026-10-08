package com.matt.flashcard

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeAndAiTest {
    @Test fun weekCoversSevenDaysEndingToday() {
        val week = buildWeek(listOf(PracticeDay(100, 4), PracticeDay(98, 2), PracticeDay(50, 9)), today = 100)
        assertEquals((94L..100L).toList(), week.map { it.epochDay })
        assertEquals(listOf(0, 0, 0, 0, 2, 0, 4), week.map { it.count })
        assertTrue(week.last().isToday)
        assertFalse(week.first().isToday)
    }

    @Test fun skippedDaysAreZero() {
        assertTrue(buildWeek(emptyList(), 10).all { it.count == 0 })
    }

    @Test fun geminiRequestAndAnswer() {
        val body = JSONObject(geminiRequestBody("سلام", "Persian"))
        assertEquals("سلام", body.getJSONArray("contents").getJSONObject(0).getJSONArray("parts").getJSONObject(0).getString("text"))
        assertEquals("salâm", parseGeminiText("""{"candidates":[{"content":{"parts":[{"text":"sal"},{"text":"âm\n"}]}}]}"""))
    }

    @Test(expected = RomanizationException::class) fun geminiEmptyAnswerIsAnError() {
        parseGeminiText("""{"candidates":[]}""")
    }

    @Test fun openAiRequestAndAnswer() {
        val body = JSONObject(openAiRequestBody("こんにちは", "Japanese"))
        assertEquals("system", body.getJSONArray("messages").getJSONObject(0).getString("role"))
        assertEquals("konnichiwa", parseOpenAiText("""{"choices":[{"message":{"content":" konnichiwa "}}]}"""))
    }

    @Test(expected = RomanizationException::class) fun openAiEmptyAnswerIsAnError() {
        parseOpenAiText("""{"choices":[{"message":{"content":""}}]}""")
    }
}

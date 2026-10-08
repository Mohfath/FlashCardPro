package com.matt.flashcard

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnthropicTest {
    @Test fun requestCarriesTextAndLanguage() {
        val body = JSONObject(anthropicRequestBody("おはよう", "Japanese"))
        assertEquals("おはよう", body.getJSONArray("messages").getJSONObject(0).getString("content"))
        assertTrue(body.getString("system").contains("Japanese"))
    }

    @Test fun parsesFirstTextBlock() {
        val json = """{"content":[{"type":"text","text":" ohayou \n"}]}"""
        assertEquals("ohayou", parseAnthropicText(json))
    }

    @Test(expected = RomanizationException::class) fun emptyAnswerIsAnError() {
        parseAnthropicText("""{"content":[]}""")
    }
}

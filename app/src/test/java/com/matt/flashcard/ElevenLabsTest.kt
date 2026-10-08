package com.matt.flashcard

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ElevenLabsTest {
    @Test fun requestBodyCarriesTextAndMultilingualModel() {
        val body = JSONObject(elevenLabsRequestBody("Dobro jutro"))
        assertEquals("Dobro jutro", body.getString("text"))
        assertEquals("eleven_multilingual_v2", body.getString("model_id"))
    }

    @Test fun persianUsesV3WithItsLanguageTag() {
        val body = JSONObject(elevenLabsRequestBody("salam", "fa"))
        assertEquals("eleven_v3", body.getString("model_id"))
        assertEquals("fa", body.getString("language_code"))
    }

    @Test fun languagesV2KnowsKeepV2AndSendNoTag() {
        val body = JSONObject(elevenLabsRequestBody("Dobro jutro", "hr"))
        assertEquals("eleven_multilingual_v2", body.getString("model_id"))
        assertTrue(!body.has("language_code"))
    }

    @Test fun urlTargetsTheVoiceAndAsksForMp3() {
        val url = elevenLabsUrl("abc123")
        assertTrue(url.contains("/text-to-speech/abc123"))
        assertTrue(url.contains("output_format=mp3"))
    }

    @Test fun serverExplanationWinsOverStatusCode() {
        val body = """{"detail":{"status":"quota_exceeded","message":"You have run out of credits"}}"""
        assertEquals("ElevenLabs: You have run out of credits", elevenLabsErrorMessage(401, body))
    }

    @Test fun plainStringDetailIsUsed() {
        assertEquals("ElevenLabs: Bad voice", elevenLabsErrorMessage(400, """{"detail":"Bad voice"}"""))
    }

    @Test fun fallsBackOnStatusCodeWhenBodyIsUnusable() {
        assertEquals("ElevenLabs rejected the API key", elevenLabsErrorMessage(401, ""))
        assertEquals("ElevenLabs error (500)", elevenLabsErrorMessage(500, "not json"))
    }
}

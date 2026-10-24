package com.matt.flashcard

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepLTest {
    @Test fun requestBodyUsesUpperCaseLanguageCodes() {
        val body = JSONObject(deepLRequestBody("Good morning", "en", "hr"))
        assertEquals("EN", body.getString("source_lang"))
        assertEquals("HR", body.getString("target_lang"))
        assertEquals("Good morning", body.getJSONArray("text").getString(0))
    }

    @Test fun parsesTranslationText() {
        val json = """{"translations":[{"detected_source_language":"EN","text":"Možemo li dobiti račun, molim?"}]}"""
        assertEquals("Možemo li dobiti račun, molim?", parseDeepLTranslation(json))
    }

    @Test(expected = TranslationException::class)
    fun emptyTranslationIsAnError() {
        parseDeepLTranslation("""{"translations":[]}""")
    }

    @Test fun freeKeysUseTheFreeEndpoint() {
        assertTrue(deepLEndpoint("abc:fx").startsWith("https://api-free.deepl.com"))
        assertTrue(deepLEndpoint("abc").startsWith("https://api.deepl.com"))
    }

    @Test fun errorCodesGetFriendlyMessages() {
        assertEquals("DeepL monthly character quota used up", deepLErrorMessage(456))
        assertEquals("DeepL rejected the API key", deepLErrorMessage(403))
        assertEquals("DeepL error (500)", deepLErrorMessage(500))
    }

    @Test fun informalAndFormalAreSentAsPreferences() {
        assertEquals("prefer_less", JSONObject(deepLRequestBody("Hi", "en", "de", Formality.INFORMAL)).getString("formality"))
        assertEquals("prefer_more", JSONObject(deepLRequestBody("Hi", "en", "de", Formality.FORMAL)).getString("formality"))
    }

    @Test fun automaticSendsNoFormality() {
        assertTrue(!JSONObject(deepLRequestBody("Hi", "en", "de", Formality.AUTO)).has("formality"))
        assertTrue(!JSONObject(deepLRequestBody("Hi", "en", "de")).has("formality"))
    }

    @Test fun formalityIsOfferedOnlyWhereDeepLSupportsIt() {
        assertTrue(supportsFormality("de") && supportsFormality("ja"))
        assertTrue(!supportsFormality("hr") && !supportsFormality("fa") && !supportsFormality("en"))
    }

    @Test fun deckDefaultsToInformalAndRoundTrips() {
        assertEquals(Formality.INFORMAL, Deck(name = "a").formality)
        Formality.entries.forEach { assertEquals(it, Deck(name = "a").withFormality(it).formality) }
    }
}

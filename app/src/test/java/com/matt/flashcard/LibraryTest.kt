package com.matt.flashcard

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryTest {
    private val categories = libraryLevels.flatMap { it.categories }
    private val sample = LibraryCategory("t-sample", "Sample", "x", List(LIBRARY_CARDS_PER_CATEGORY) { "card $it" })

    @Test fun thereAreTenLevelsNumberedInOrder() {
        assertEquals((1..10).toList(), libraryLevels.map { it.number })
    }

    @Test fun everyCategoryHasExactlyTwentyCards() {
        categories.forEach { assertEquals("${it.id} has ${it.cards.size} cards", LIBRARY_CARDS_PER_CATEGORY, it.cards.size) }
    }

    @Test fun noCardIsBlank() {
        categories.forEach { c -> c.cards.forEach { assertTrue("blank card in ${c.id}", it.isNotBlank()) } }
    }

    @Test fun categoryIdsAreUnique() {
        assertEquals(categories.size, categories.map { it.id }.toSet().size)
    }

    @Test fun noEnglishCardAppearsTwiceInTheLibrary() {
        val all = categories.flatMap { c -> c.cards.map { it.trim().lowercase() } }
        val dupes = all.groupBy { it }.filterValues { it.size > 1 }.keys
        assertTrue("duplicate cards: $dupes", dupes.isEmpty())
    }

    @Test fun libraryIdsIncludeTheCategoryAndPosition() {
        assertEquals("t-sample/0", sample.libraryId(0))
        assertEquals("t-sample/19", sample.libraryId(19))
    }

    @Test fun nothingAddedMeansEverythingIsMissing() {
        assertEquals(20, missingEntries(sample, emptySet()).size)
        assertFalse(isAdded(sample, emptySet()))
    }

    @Test fun allPresentMeansAdded() {
        val ids = List(20) { sample.libraryId(it) }.toSet()
        assertTrue(missingEntries(sample, ids).isEmpty())
        assertTrue(isAdded(sample, ids))
    }

    @Test fun deletingOneCardMakesOnlyThatOneMissing() {
        val ids = List(20) { sample.libraryId(it) }.toSet() - sample.libraryId(7)
        val missing = missingEntries(sample, ids)
        assertEquals(listOf(LibraryEntry("t-sample/7", "card 7")), missing)
        assertFalse(isAdded(sample, ids))
    }

    @Test fun otherCategoriesDoNotCountAsAdded() {
        assertFalse(isAdded(sample, setOf("other/0", "other/1")))
    }

    @Test fun severalTextsGoInOneDeepLRequestWithFormality() {
        val body = JSONObject(deepLRequestBody(listOf("Hello", "Thank you"), "en", "de", Formality.INFORMAL))
        assertEquals(listOf("Hello", "Thank you"), List(2) { body.getJSONArray("text").getString(it) })
        assertEquals("DE", body.getString("target_lang"))
        assertEquals("prefer_less", body.getString("formality"))
    }

    @Test fun batchResponseKeepsOrder() {
        val json = """{"translations":[{"text":"Hallo"},{"text":"Danke"}]}"""
        assertEquals(listOf("Hallo", "Danke"), parseDeepLTranslations(json, 2))
    }

    @Test(expected = TranslationException::class)
    fun batchResponseWithTheWrongCountIsRejected() {
        parseDeepLTranslations("""{"translations":[{"text":"Hallo"}]}""", 2)
    }

    @Test(expected = TranslationException::class)
    fun batchResponseWithABlankTranslationIsRejected() {
        parseDeepLTranslations("""{"translations":[{"text":"Hallo"},{"text":" "}]}""", 2)
    }
}

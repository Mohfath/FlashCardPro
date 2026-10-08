package com.matt.flashcard

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshCurrentTest {
    @Test fun refreshedAudioSurvivesAnAnswer() {
        val card = Card(id = 1, deckId = 1, sourceText = "Hello", targetText = "Bok", targetAudioPath = "old.mp3")
        val session = ReviewSession(listOf(card), { 1_000L }, Random(1))
        session.refreshCurrent(card.copy(targetAudioPath = "new.mp3"))
        assertEquals("new.mp3", session.current?.targetAudioPath)
        session.reveal()
        val change = session.answer(Verdict.KNOW) as Change.Update
        assertEquals("new.mp3", change.card.targetAudioPath)
    }

    @Test fun refreshIgnoresAnotherCard() {
        val card = Card(id = 1, deckId = 1, sourceText = "Hello", targetText = "Bok", targetAudioPath = "a.mp3")
        val session = ReviewSession(listOf(card), { 1_000L }, Random(1))
        session.refreshCurrent(card.copy(id = 2, targetAudioPath = "b.mp3"))
        assertEquals("a.mp3", session.current?.targetAudioPath)
    }
}

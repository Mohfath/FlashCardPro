package com.matt.flashcard

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DaoTest {
    private lateinit var db: AppDatabase
    private val dao get() = db.dao()

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).build()
    }

    @After fun tearDown() = db.close()

    @Test fun practiceIsCountedPerDay() = runBlocking {
        dao.recordPractice(day = 500)
        dao.recordPractice(day = 500)
        dao.recordPractice(day = 501)
        val rows = dao.practiceSince(500).first().sortedBy { it.day }
        assertEquals(listOf(500L to 2, 501L to 1), rows.map { it.day to it.count })
    }

    @Test fun newCardIsDueAndKnowMovesItUp() = runBlocking {
        val deckId = dao.insertDeck(Deck(name = "Croatian"))
        dao.insertCard(Card(deckId = deckId, sourceText = "Good morning", targetText = "Dobro jutro"))
        val card = dao.cardsIn(deckId).single()
        assertTrue(Leitner.isDue(card.box, card.lastReviewedAt, System.currentTimeMillis()))
        dao.updateCard(card.copy(box = Leitner.nextBox(card.box, Verdict.KNOW), lastReviewedAt = 1L))
        assertEquals(2, dao.cardsIn(deckId).single().box)
    }

    @Test fun deletingDeckDeletesItsCards() = runBlocking {
        val deckId = dao.insertDeck(Deck(name = "Croatian"))
        dao.insertCard(Card(deckId = deckId, sourceText = "Thanks", targetText = "Hvala"))
        db.openHelper.writableDatabase.execSQL("DELETE FROM deck WHERE id = $deckId")
        assertEquals(0, dao.cardsIn(deckId).size)
    }

    @Test fun deleteCardRemovesIt() = runBlocking {
        val deckId = dao.insertDeck(Deck(name = "Croatian"))
        dao.insertCard(Card(deckId = deckId, sourceText = "Yes", targetText = "Da"))
        dao.deleteCard(dao.cardsIn(deckId).single())
        assertEquals(0, dao.cardsIn(deckId).size)
    }
}

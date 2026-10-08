package com.matt.flashcard

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Acts like Android Auto: a media browser/controller talking to [FlashcardService] on the device. */
@RunWith(AndroidJUnit4::class)
class AutoServiceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dao = AppDatabase.get(context).dao()
    private lateinit var deck: Deck
    private lateinit var browser: MediaBrowser

    @Before fun setUp() = runBlocking {
        val id = dao.insertDeck(Deck(name = "AutoTest"))
        deck = dao.deck(id)!!
        listOf("One" to "Jedan", "Two" to "Dva", "Three" to "Tri")
            .forEach { (en, hr) -> dao.insertCard(Card(deckId = id, sourceText = en, targetText = hr)) }
        val token = SessionToken(context, ComponentName(context, FlashcardService::class.java))
        browser = MediaBrowser.Builder(context, token).buildAsync().get(15, TimeUnit.SECONDS)
    }

    @After fun tearDown() = runBlocking {
        onMain { browser.release() }
        dao.deleteDeck(deck)
    }

    private fun <T> onMain(block: () -> T): T {
        val out = AtomicReference<T>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync { out.set(block()) }
        return out.get()
    }

    private fun waitUntil(what: String, timeoutMs: Long = 8_000, check: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            if (onMain(check)) return
            Thread.sleep(100)
        }
        throw AssertionError("Timed out waiting for: $what")
    }

    // Only call these inside onMain / waitUntil: the controller must be touched on the main thread.
    private fun title() = browser.mediaMetadata.title?.toString().orEmpty()
    private fun subtitle() = browser.mediaMetadata.subtitle?.toString().orEmpty()
    private fun cards() = runBlocking { dao.cardsIn(deck.id) }

    private fun startReview() {
        val children = onMain { browser.getChildren("root", 0, 50, null) }.get(10, TimeUnit.SECONDS).value!!
        val deckItem: MediaItem = children.first { it.mediaMetadata.title == "AutoTest" }
        onMain { browser.setMediaItem(deckItem); browser.prepare() }
        waitUntil("Card to appear") { title().isNotEmpty() && title() != "AutoTest" }
    }

    @Test fun browseListShowsDecksWithDueCounts() {
        val root = onMain { browser.getLibraryRoot(null) }.get(10, TimeUnit.SECONDS).value!!
        assertTrue(root.mediaMetadata.isBrowsable == true)
        val children = onMain { browser.getChildren(root.mediaId, 0, 50, null) }.get(10, TimeUnit.SECONDS).value!!
        val item = children.first { it.mediaMetadata.title == "AutoTest" }
        assertEquals("3 Due · 3 Cards", item.mediaMetadata.subtitle.toString())
        assertTrue(item.mediaMetadata.isPlayable == true)
    }

    @Test fun choosingADeckShowsAPromptWithArtworkAndEnabledButtons() {
        startReview()
        assertEquals("Press play to reveal", onMain { subtitle() })
        val meta = onMain { browser.mediaMetadata }
        assertNotNull("artwork", meta.artworkData)
        assertTrue("artwork should be a real image", meta.artworkData!!.size > 2_000)
        onMain {
            assertTrue(browser.isCommandAvailable(Player.COMMAND_PLAY_PAUSE))
            assertTrue(browser.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT))
            assertTrue(browser.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS))
        }
    }

    @Test fun playRevealsTheAnswerAfterTheChooseDeckGraceWindow() {
        startReview()
        val prompt = onMain { title() }
        Thread.sleep(2_500) // choosing a Deck also sends "play"; that must not Reveal
        assertEquals("Press play to reveal", onMain { subtitle() })
        onMain { browser.play() }
        waitUntil("Answer to show") { subtitle() != "Press play to reveal" }
        val answer = onMain { subtitle() }
        assertEquals(setOf("One", "Two", "Three", "Jedan", "Dva", "Tri").containsAll(listOf(prompt, answer)), true)
        assertTrue(prompt != answer)
        assertFalse("play must not stay in the playing state", onMain { browser.playWhenReady })
    }

    @Test fun nextMeansKnowAndPreviousMeansDidntKnow() {
        startReview()
        Thread.sleep(2_500)
        onMain { browser.play() }
        waitUntil("Reveal") { subtitle() != "Press play to reveal" }
        val first = onMain { title() }
        onMain { browser.seekToNext() }
        waitUntil("next Card") { title() != first && subtitle() == "Press play to reveal" }
        waitUntil("Know saved", 5_000) { cards().count { it.box == 2 } == 1 }

        onMain { browser.play() }
        waitUntil("Reveal 2") { subtitle() != "Press play to reveal" }
        val second = onMain { title() }
        onMain { browser.seekToPrevious() }
        waitUntil("third Card") { title() != second && subtitle() == "Press play to reveal" }
        waitUntil("Didn't know saved", 5_000) { cards().count { it.lastReviewedAt != null } == 2 }
        assertEquals(1, cards().count { it.box == 2 })
        assertEquals(1, cards().count { it.box == 1 && it.lastReviewedAt != null })
    }

    @Test fun nextBeforeRevealDoesNothing() {
        startReview()
        val first = onMain { title() }
        onMain { browser.seekToNext() }
        Thread.sleep(800)
        assertEquals(first, onMain { title() })
        assertEquals(0, cards().count { it.lastReviewedAt != null })
    }

    @Test fun deleteNeedsASecondPress() {
        startReview()
        val delete = SessionCommand("com.matt.flashcard.DELETE", android.os.Bundle.EMPTY)
        onMain { browser.sendCustomCommand(delete, android.os.Bundle.EMPTY) }
        waitUntil("confirmation prompt") { subtitle().startsWith("Press Delete again") }
        assertEquals(3, cards().size)
        onMain { browser.sendCustomCommand(delete, android.os.Bundle.EMPTY) }
        waitUntil("Card deleted", 5_000) { cards().size == 2 }
    }
}

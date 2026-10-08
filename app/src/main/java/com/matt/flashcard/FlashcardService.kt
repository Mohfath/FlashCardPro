package com.matt.flashcard

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val ROOT_ID = "root"
private const val DECK_PREFIX = "deck:"
private val KNOW_COMMAND = SessionCommand("com.matt.flashcard.KNOW", Bundle.EMPTY)
private val AGAIN_COMMAND = SessionCommand("com.matt.flashcard.AGAIN", Bundle.EMPTY)

/**
 * What Android Auto connects to (ADR 0001): a media app. The browse list shows the Decks,
 * choosing one starts a review, and the now-playing controls are the review buttons.
 */
class FlashcardService : MediaLibraryService() {
    private lateinit var player: ReviewPlayer
    private var librarySession: MediaLibrarySession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        player = ReviewPlayer(this)
        val session = MediaLibrarySession.Builder(this, player, LibraryCallback()).build()
        librarySession = session
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = librarySession

    override fun onDestroy() {
        scope.cancel()
        librarySession?.release()
        librarySession = null
        player.release()
        super.onDestroy()
    }

    /** The two answer buttons next to play: cross (Again) on the left, check (Know) on the right. */
    private fun customLayout(): ImmutableList<CommandButton> = ImmutableList.of(
        CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setDisplayName("Again")
            .setCustomIconResId(R.drawable.ic_again)
            .setSessionCommand(AGAIN_COMMAND)
            .build(),
        CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setDisplayName("Know")
            .setCustomIconResId(R.drawable.ic_know)
            .setSessionCommand(KNOW_COMMAND)
            .build(),
    )

    private inner class LibraryCallback : MediaLibrarySession.Callback {
        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                .buildUpon().add(KNOW_COMMAND).add(AGAIN_COMMAND).build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands)
                .setCustomLayout(customLayout())
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession, controller: MediaSession.ControllerInfo, customCommand: SessionCommand, args: Bundle,
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                KNOW_COMMAND.customAction -> player.answer(Verdict.KNOW)
                AGAIN_COMMAND.customAction -> player.answer(Verdict.DIDNT_KNOW)
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        /** Steering-wheel next and previous keys answer the Card, as the skip buttons used to. */
        override fun onMediaButtonEvent(session: MediaSession, controllerInfo: MediaSession.ControllerInfo, intent: Intent): Boolean {
            @Suppress("DEPRECATION")
            val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return false
            val verdict = when (event.keyCode) {
                KeyEvent.KEYCODE_MEDIA_NEXT -> Verdict.KNOW
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> Verdict.DIDNT_KNOW
                else -> return false
            }
            if (event.action == KeyEvent.ACTION_DOWN) player.answer(verdict)
            return true
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession, browser: MediaSession.ControllerInfo, params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val root = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder().setIsBrowsable(true).setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED).setTitle("Flashcards").build(),
                ).build()
            return Futures.immediateFuture(LibraryResult.ofItem(root, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession, browser: MediaSession.ControllerInfo, parentId: String,
            page: Int, pageSize: Int, params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val result = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
            if (parentId != ROOT_ID) {
                result.set(LibraryResult.ofItemList(ImmutableList.of(), params))
                return result
            }
            scope.launch {
                val dao = AppDatabase.get(applicationContext).dao()
                val summaries = withIO { summarize(dao.decks().first(), dao.allCards().first(), System.currentTimeMillis()) }
                result.set(LibraryResult.ofItemList(ImmutableList.copyOf(summaries.map(::deckItem)), params))
            }
            return result
        }

        override fun onGetItem(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, mediaId: String): ListenableFuture<LibraryResult<MediaItem>> {
            val result = SettableFuture.create<LibraryResult<MediaItem>>()
            val deckId = mediaId.removePrefix(DECK_PREFIX).toLongOrNull()
            if (deckId == null) {
                result.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                return result
            }
            scope.launch {
                val dao = AppDatabase.get(applicationContext).dao()
                val summary = withIO { summarize(dao.decks().first(), dao.allCards().first(), System.currentTimeMillis()) }
                    .firstOrNull { it.deck.id == deckId }
                result.set(if (summary == null) LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE) else LibraryResult.ofItem(deckItem(summary), null))
            }
            return result
        }

        /** Choosing a Deck in the car starts its review; the playlist itself is managed by [ReviewPlayer]. */
        override fun onSetMediaItems(
            mediaSession: MediaSession, controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            mediaItems.firstOrNull()?.mediaId?.removePrefix(DECK_PREFIX)?.toLongOrNull()?.let(player::openDeck)
            return Futures.immediateFuture(MediaSession.MediaItemsWithStartPosition(mediaItems, 0, 0))
        }
    }

    private fun deckItem(s: DeckSummary): MediaItem =
        MediaItem.Builder()
            .setMediaId("$DECK_PREFIX${s.deck.id}")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(s.deck.name)
                    .setSubtitle("${s.due} Due · ${s.total} Cards")
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                    .build(),
            ).build()

    private suspend fun <T> withIO(block: suspend () -> T): T = kotlinx.coroutines.withContext(Dispatchers.IO) { block() }
}

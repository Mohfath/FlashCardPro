package com.matt.flashcard

import android.content.Context
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Presents a review session to Android Auto as a media player, so the car's own media controls drive it:
 * previous = Didn't know, play/pause = Reveal, next = Know. Delete is a custom action (see [requestDelete]).
 *
 * The playlist is [placeholder, current Card, placeholder]: the two placeholders only keep the car's
 * previous and next buttons enabled. Whatever the car seeks to, the current Card stays at index 1.
 */
class ReviewPlayer(context: Context) : SimpleBasePlayer(Looper.getMainLooper()) {
    private val appContext = context.applicationContext
    private val dao = AppDatabase.get(appContext).dao()
    private val audioStore = AudioStore(appContext)
    private val speaker = AudioPlayer(appContext, audioStore)
    private val artwork = CardArtwork(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var deck: Deck? = null
    private var session: ReviewSession? = null
    private var ui: ReviewUi? = null
    private var artworkBytes: ByteArray? = null

    /** Choosing a Deck in the car sends "play" right after; that one must not count as Reveal. */
    private var ignorePlayUntil = 0L

    val pendingDelete get() = ui?.pendingDelete == true

    fun openDeck(deckId: Long) {
        scope.launch {
            val (d, cards) = withContext(Dispatchers.IO) { dao.deck(deckId) to dao.cardsIn(deckId) }
            d ?: return@launch
            deck = d
            session = ReviewSession(cards, { System.currentTimeMillis() })
            ignorePlayUntil = SystemClock.elapsedRealtime() + IGNORE_PLAY_MS
            refresh(previous = null)
        }
    }

    fun requestDelete() {
        val s = session ?: return
        val before = ui
        val change = s.delete()
        persist(change)
        refresh(before)
    }

    override fun getState(): State {
        val current = ui
        val builder = State.Builder()
            .setPlayWhenReady(false, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setCurrentMediaItemIndex(1)
        if (current == null) {
            return builder
                .setAvailableCommands(COMMANDS_IDLE)
                .setPlaybackState(STATE_IDLE)
                .setPlaylist(emptyList())
                .build()
        }
        val finished = current.cardId == null
        val playlist = if (finished) listOf(item("done", finishedMetadata()))
        else listOf(item("before", MediaMetadata.EMPTY), item("card:${current.cardId}", cardMetadata(current)), item("after", MediaMetadata.EMPTY))
        return builder
            .setAvailableCommands(if (finished) COMMANDS_IDLE else COMMANDS_REVIEWING)
            .setPlaybackState(STATE_READY)
            .setPlaylist(playlist)
            .setCurrentMediaItemIndex(if (finished) 0 else 1)
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        if (playWhenReady && SystemClock.elapsedRealtime() >= ignorePlayUntil) {
            val s = session
            val before = ui
            if (s != null && s.current != null && !s.revealed) {
                s.reveal()
                refresh(before)
            }
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
        val verdict = when (seekCommand) {
            COMMAND_SEEK_TO_NEXT, COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> Verdict.KNOW
            COMMAND_SEEK_TO_PREVIOUS, COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> Verdict.DIDNT_KNOW
            else -> null
        }
        val s = session
        if (verdict != null && s != null) {
            val before = ui
            persist(s.answer(verdict))
            refresh(before)
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleSetMediaItems(mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long): ListenableFuture<*> =
        Futures.immediateVoidFuture()

    override fun handlePrepare(): ListenableFuture<*> = Futures.immediateVoidFuture()

    override fun handleStop(): ListenableFuture<*> {
        speaker.stop()
        session = null
        deck = null
        ui = null
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleRelease(): ListenableFuture<*> {
        speaker.stop()
        scope.cancel()
        return Futures.immediateVoidFuture()
    }

    private fun refresh(previous: ReviewUi?) {
        val d = deck ?: return
        val s = session ?: return
        val next = buildReviewUi(d, s)
        ui = next
        artworkBytes = if (next.cardId != null) artwork.render(next) else null
        invalidateState()
        // The target-language side speaks as soon as it appears, like on the phone.
        if (next.cardId != null) {
            val newCard = previous?.cardId != next.cardId
            val newlyRevealed = next.revealed && previous?.revealed != true
            if (newCard && !next.revealed) next.promptAudio?.let(speaker::play)
            else if (newlyRevealed) next.answerAudio?.let(speaker::play)
        }
    }

    private fun persist(change: Change?) {
        when (change) {
            is Change.Update -> scope.launch(Dispatchers.IO) {
                dao.updateCard(change.card)
                dao.recordPractice()
            }
            is Change.Remove -> scope.launch(Dispatchers.IO) {
                dao.deleteCard(change.card)
                audioStore.delete(change.card.targetAudioPath)
            }
            null -> Unit
        }
    }

    private fun item(id: String, metadata: MediaMetadata) =
        MediaItemData.Builder(id)
            .setMediaItem(MediaItem.Builder().setMediaId(id).setMediaMetadata(metadata).build())
            .setMediaMetadata(metadata)
            .setDurationUs(C.TIME_UNSET)
            .build()

    private fun cardMetadata(ui: ReviewUi): MediaMetadata {
        val subtitle = when {
            ui.pendingDelete -> "Press Delete again to remove this Card"
            ui.revealed -> ui.answerText
            else -> "Press play to reveal"
        }
        return MediaMetadata.Builder()
            .setTitle(ui.promptText)
            .setSubtitle(subtitle)
            .setArtist(subtitle)
            .setAlbumTitle(ui.deckName)
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .also { b -> artworkBytes?.let { b.setArtworkData(it, MediaMetadata.PICTURE_TYPE_FRONT_COVER) } }
            .build()
    }

    private fun finishedMetadata() = MediaMetadata.Builder()
        .setTitle("All done for now")
        .setSubtitle("No Cards left in ${deck?.name.orEmpty()}")
        .setIsPlayable(true)
        .setIsBrowsable(false)
        .build()

    private companion object {
        const val IGNORE_PLAY_MS = 2_000L

        val COMMANDS_REVIEWING: Player.Commands = Player.Commands.Builder().addAll(
            COMMAND_PLAY_PAUSE, COMMAND_PREPARE, COMMAND_STOP,
            COMMAND_SEEK_TO_NEXT, COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            COMMAND_SEEK_TO_PREVIOUS, COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            COMMAND_GET_CURRENT_MEDIA_ITEM, COMMAND_GET_TIMELINE, COMMAND_GET_METADATA,
            COMMAND_SET_MEDIA_ITEM, COMMAND_CHANGE_MEDIA_ITEMS,
        ).build()

        val COMMANDS_IDLE: Player.Commands = Player.Commands.Builder().addAll(
            COMMAND_PREPARE, COMMAND_STOP,
            COMMAND_GET_CURRENT_MEDIA_ITEM, COMMAND_GET_TIMELINE, COMMAND_GET_METADATA,
            COMMAND_SET_MEDIA_ITEM, COMMAND_CHANGE_MEDIA_ITEMS,
        ).build()
    }
}

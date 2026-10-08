package com.matt.flashcard

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import java.io.File
import java.io.IOException
import java.util.UUID

/** Audio generated once per Card and kept in app storage. Cards hold only the file name. */
class AudioStore(context: Context) {
    private val dir = File(context.filesDir, "audio").apply { mkdirs() }

    fun save(bytes: ByteArray): String {
        val name = "${UUID.randomUUID()}.mp3"
        File(dir, name).writeBytes(bytes)
        return name
    }

    fun file(name: String) = File(dir, name)

    fun delete(name: String?) {
        if (name != null) file(name).delete()
    }
}

/** Plays one sentence at a time, briefly pausing other audio (music) while it speaks. */
class AudioPlayer(context: Context, private val store: AudioStore) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attributes)
        .build()
    private var player: MediaPlayer? = null

    fun play(name: String) {
        val file = store.file(name)
        if (!file.exists()) return
        stop()
        audioManager.requestAudioFocus(focusRequest)
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(attributes)
                setDataSource(file.path)
                setOnCompletionListener { stop() }
                prepare()
                start()
            }
        } catch (e: IOException) {
            stop()
        }
    }

    fun stop() {
        player?.release()
        player = null
        audioManager.abandonAudioFocusRequest(focusRequest)
    }
}

package com.matt.flashcard

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardArtworkTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun ui(revealed: Boolean, prompt: String = "Možemo li dobiti račun, molim?", answer: String = "Could we have the bill, please?", extra: Boolean = false, pendingDelete: Boolean = false) =
        ReviewUi("Croatian Basics", 9, 3, prompt, "HRVATSKI", answer, "ENGLISH", revealed, extra, 5, pendingDelete)

    @Test fun rendersReadableArtworkForEveryState() {
        val artwork = CardArtwork(context)
        val dir = File(context.cacheDir, "artwork").apply { mkdirs() }
        val cases = mapOf(
            "prompt" to ui(false),
            "revealed" to ui(true),
            "extra" to ui(false, extra = true),
            "long" to ui(true, prompt = "Željela bih rezervirati stol za dvoje uz more večeras, ako je moguće.", answer = "I would like to reserve a table for two by the sea tonight, if possible."),
            "delete" to ui(true, pendingDelete = true),
        )
        cases.forEach { (name, u) ->
            val bytes = artwork.render(u)
            assertTrue("$name artwork too small", bytes.size > 5_000)
            File(dir, "$name.jpg").writeBytes(bytes)
        }
    }
}

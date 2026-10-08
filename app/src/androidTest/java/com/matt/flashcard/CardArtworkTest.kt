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

    private val croatia = Languages.first { it.code == "hr" }.flag
    private val britain = Languages.first { it.code == "en" }.flag
    private val iran = Languages.first { it.code == "fa" }.flag

    private fun ui(
        revealed: Boolean, prompt: String = "Možemo li dobiti račun, molim?", answer: String = "Could we have the bill, please?",
        extra: Boolean = false, pendingDelete: Boolean = false, box: Int = 3,
    ) = ReviewUi(
        "Croatian Basics", 9, box, prompt, "HRVATSKI", answer, "ENGLISH", revealed, extra, 5, pendingDelete,
        promptFlag = croatia, answerFlag = britain,
    )

    @Test fun rendersReadableArtworkForEveryState() {
        val artwork = CardArtwork(context)
        val dir = File(context.cacheDir, "artwork").apply { mkdirs() }
        val cases = mapOf(
            "prompt" to ui(false),
            "revealed" to ui(true),
            "extra" to ui(false, extra = true),
            "long" to ui(true, prompt = "Željela bih rezervirati stol za dvoje uz more večeras, ako je moguće.", answer = "I would like to reserve a table for two by the sea tonight, if possible."),
            "delete" to ui(true, pendingDelete = true),
            "box1" to ui(true, prompt = "Dobro jutro", answer = "Good morning", box = 1),
            "box2" to ui(true, prompt = "Hvala lijepa", answer = "Thank you very much", box = 2),
            "box3" to ui(true, prompt = "Gdje je plaža?", answer = "Where is the beach?", box = 3),
            "box4" to ui(true, prompt = "Koliko ovo košta?", answer = "How much does this cost?", box = 4),
            "box5" to ui(true, prompt = "Vidimo se sutra", answer = "See you tomorrow", box = 5),
            "persian" to ReviewUi(
                "Persian Basics", 3, 2, "Good morning", "ENGLISH", "صبح بخیر", "PERSIAN", true, false, 4, false,
                answerRomanization = "Sobh-e bakhir", promptFlag = britain, answerFlag = iran,
            ),
        )
        // Also written where adb can read it without root, so the renders can be looked at on a computer.
        val shared = File(context.externalCacheDir, "artwork").apply { mkdirs() }
        cases.forEach { (name, u) ->
            val bytes = artwork.render(u)
            assertTrue("$name artwork too small", bytes.size > 5_000)
            File(dir, "$name.jpg").writeBytes(bytes)
            File(shared, "$name.jpg").writeBytes(bytes)
        }
    }
}

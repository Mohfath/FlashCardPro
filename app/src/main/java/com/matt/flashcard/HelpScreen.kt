package com.matt.flashcard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/** A short title and the lines of text under it. */
data class HelpSection(val title: String, val lines: List<String>)

/** What the Help page says, written from what the app really does. */
val helpSections: List<HelpSection> = listOf(
    HelpSection(
        "Getting started",
        listOf(
            "Open Settings (the gear) and add your own keys: DeepL translates, ElevenLabs speaks, and Claude, Gemini or OpenAI writes Latin letters. Keys stay on this phone.",
            "Tap + New Deck, name it, and pick the language you know and the language you want to learn.",
        ),
    ),
    HelpSection(
        "Adding cards",
        listOf(
            "Add Card: type in either language and the other fills in by itself. You can edit it before saving.",
            "Import: paste a list, one card per line, from the Add Card page.",
            "Deck Library: ready-made levels with topics such as home, kitchen or the phone. Copy a topic and its 20 cards are translated into your deck's two languages and added.",
        ),
    ),
    HelpSection(
        "Reviewing",
        listOf(
            "Tap the big button on a deck to review. Tap Reveal to see the answer.",
            "Swipe right or tap Know when you knew it; swipe left or tap Didn't know when you didn't. Swipe down, or tap Delete twice, to remove a card.",
            "When nothing is due, the button says Extra practice. Extra practice doesn't move cards between boxes.",
        ),
    ),
    HelpSection(
        "Boxes",
        listOf(
            "Cards live in five boxes. Know moves a card up one box and it comes back later; Didn't know sends it to Box 1, so you see it again soon.",
            "The bar on each deck shows how many cards are in each box.",
        ),
    ),
    HelpSection(
        "Deck options (the three dots)",
        listOf(
            "Show cards in: for languages like Persian, Arabic or Japanese, show the native script, Latin letters, or both.",
            "Tone: for languages that have it, translate informally (default) or formally.",
            "Redo Missing Audio: make audio for the cards that have none. It uses ElevenLabs credits.",
            "Delete Deck: removes the deck and all its cards for good.",
        ),
    ),
    HelpSection(
        "Last 7 days",
        listOf("A circle with a number is a day you practised, with how many cards. An ice cube is a day you skipped."),
    ),
    HelpSection(
        "In the car",
        listOf(
            "Flashcards also works on the Android Auto screen: the card is read aloud, play reveals the answer, and the Know and Again buttons mark it.",
            "It isn't on the Play Store, so turn on developer mode in Android Auto to see it.",
        ),
    ),
)

@Composable
fun HelpScreen(onBack: () -> Unit) {
    val c = LocalFlashColors.current
    Box(Modifier.fillMaxSize().background(c.canvas).safeDrawingPadding()) {
        Column(Modifier.fillMaxSize()) {
            TopBar(onBack, "Help")
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                helpSections.forEach { section ->
                    val shape = RoundedCornerShape(24.dp)
                    Column(Modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.divider, shape).padding(18.dp)) {
                        Text(section.title, style = FlashType.headlineSm, color = c.ink)
                        section.lines.forEach { line ->
                            Spacer(Modifier.height(8.dp))
                            Text(line, style = FlashType.bodyMd, color = c.inkSecondary)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

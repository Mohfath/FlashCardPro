package com.matt.flashcard

/** One Persian Card: English on the source side, Persian script plus its Latin-letter reading on the target side. */
data class StarterCard(val english: String, val persian: String, val latin: String)

private const val DECK_NAME = "Persian Basics"

/**
 * The 30 easiest Cards from the first two teaching sessions (content/ folder): small words first, then short
 * sentences built from them. Mistakes in the transcripts were not carried over.
 * Persian words use a zero-width non-joiner (‌) after می, as in normal Persian typing.
 */
val persianStarterCards: List<StarterCard> = listOf(
    StarterCard("I", "من", "man"),
    StarterCard("you (friendly)", "تو", "to"),
    StarterCard("he / she / it", "اون", "un"),
    StarterCard("from", "از", "az"),
    StarterCard("where", "کجا", "kojâ"),
    StarterCard("who", "کی", "ki"),
    StarterCard("what", "چی", "chi"),
    StarterCard("and", "و", "va"),
    StarterCard("tea", "چای", "châi"),
    StarterCard("coffee", "قهوه", "qahve"),
    StarterCard("water", "آب", "âb"),
    StarterCard("bread", "نون", "nun"),
    StarterCard("good", "خوب", "khub"),
    StarterCard("great", "عالی", "âli"),
    StarterCard("happy", "شاد", "shâd"),
    StarterCard("tired", "خسته", "khaste"),
    StarterCard("hungry", "گرسنه", "gorosne"),
    StarterCard("thirsty", "تشنه", "teshne"),
    StarterCard("doctor", "دکتر", "doktor"),
    StarterCard("teacher", "معلم", "moalem"),
    StarterCard("I am", "هستم", "hastam"),
    StarterCard("you are", "هستی", "hasti"),
    StarterCard("I am Sara.", "من سارا هستم.", "man Sârâ hastam."),
    StarterCard("Who are you?", "تو کی هستی؟", "to ki hasti?"),
    StarterCard("Where are you from?", "از کجا هستی؟", "az kojâ hasti?"),
    StarterCard("I am from Italy.", "من از ایتالیا هستم.", "man az Itâliâ hastam."),
    StarterCard("I am hungry.", "من گرسنه هستم.", "man gorosne hastam."),
    StarterCard("I want tea.", "من چای می‌خوام.", "man châi mikhâm."),
    StarterCard("What do you want?", "تو چی می‌خوای؟", "to chi mikhây?"),
    StarterCard("I drink water.", "من آب می‌خورم.", "man âb mikhoram."),
)

/**
 * Adds the "Persian Basics" Deck (Persian, with Latin-letter readings) unless one with that name already exists.
 * Returns true if it was added. Cards carry no audio; it is generated later like for any other Card.
 */
suspend fun FlashcardDao.seedPersianStarter(): Boolean {
    if (deckNamed(DECK_NAME) != null) return false
    val deckId = insertDeck(Deck(name = DECK_NAME, sourceLanguage = "en", targetLanguage = "fa").withReadingMode(ReadingMode.LATIN))
    persianStarterCards.forEach {
        insertCard(Card(deckId = deckId, sourceText = it.english, targetText = it.persian, romanization = it.latin))
    }
    return true
}

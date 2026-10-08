package com.matt.flashcard

/** Placeholder Cards so the review screen has something to show until Add Card exists. */
suspend fun FlashcardDao.seedIfEmpty() {
    if (firstDeck() != null) return
    val deckId = insertDeck(Deck(name = "Croatian Basics"))
    listOf(
        "Good morning" to "Dobro jutro",
        "Thank you very much" to "Hvala lijepa",
        "Where is the bus station?" to "Gdje je autobusni kolodvor?",
        "I would like a coffee, please." to "Želio bih kavu, molim.",
        "How much does this cost?" to "Koliko ovo košta?",
        "Could we have the bill, please?" to "Molim vas, račun.",
        "I don't understand." to "Ne razumijem.",
        "Do you speak English?" to "Govorite li engleski?",
    ).forEach { (en, hr) -> insertCard(Card(deckId = deckId, sourceText = en, targetText = hr)) }
}

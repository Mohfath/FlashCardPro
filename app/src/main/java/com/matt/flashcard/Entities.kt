package com.matt.flashcard

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "deck")
data class Deck(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sourceLanguage: String = "en",
    val targetLanguage: String = "hr",
    /** Also keep a Latin-letter reading of each Card, for learners who cannot read the target script. */
    @ColumnInfo(defaultValue = "0") val romanize: Boolean = false,
    /** With [romanize] on: show only the Latin letters and hide the native script. */
    @ColumnInfo(defaultValue = "0") val latinOnly: Boolean = false,
    /** Stored [Formality] ordinal: how DeepL words translations into the language being learned (0 = informal). */
    @ColumnInfo(defaultValue = "0") val formalityCode: Int = 0,
) {
    val formality: Formality get() = Formality.entries.getOrElse(formalityCode) { Formality.INFORMAL }

    fun withFormality(f: Formality) = copy(formalityCode = f.ordinal)

    val readingMode: ReadingMode
        get() = when {
            !romanize -> ReadingMode.SCRIPT
            latinOnly -> ReadingMode.LATIN
            else -> ReadingMode.BOTH
        }

    fun withReadingMode(mode: ReadingMode) = copy(romanize = mode != ReadingMode.SCRIPT, latinOnly = mode == ReadingMode.LATIN)
}

/** Tone of translations. [deepL] is DeepL's "prefer" value, which is ignored (not an error) for languages without the feature. */
enum class Formality(val label: String, val deepL: String?) {
    INFORMAL("Informal", "prefer_less"),
    FORMAL("Formal", "prefer_more"),
    AUTO("Automatic", null),
}

/** How a Deck shows the language being learned: the native script, Latin letters, or both. */
enum class ReadingMode(val label: String) {
    SCRIPT("Native script only"),
    LATIN("Latin letters only"),
    BOTH("Native script and Latin letters"),
}

@Entity(
    tableName = "card",
    foreignKeys = [ForeignKey(Deck::class, ["id"], ["deckId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("deckId")],
)
data class Card(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long,
    val sourceText: String,
    val targetText: String,
    val box: Int = Leitner.MIN_BOX,
    val lastReviewedAt: Long? = null,
    val targetAudioPath: String? = null,
    /** Optional example sentence in the language being learned. */
    val example: String? = null,
    /** The target text written in Latin letters; only set for Decks that romanize. */
    val romanization: String? = null,
)

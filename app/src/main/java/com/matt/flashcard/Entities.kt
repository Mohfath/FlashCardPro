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
)

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

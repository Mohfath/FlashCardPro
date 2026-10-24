package com.matt.flashcard

/** How many Cards every Library category holds. */
const val LIBRARY_CARDS_PER_CATEGORY = 20

/**
 * A topic of ready-made Cards written in English. The order of [cards] must never change once released:
 * a Card's position is part of its [libraryId], which is how a Deck remembers what it already has.
 * Add new Cards at the end of a category's list, or in a new category.
 */
data class LibraryCategory(val id: String, val name: String, val emoji: String, val cards: List<String>) {
    fun libraryId(position: Int) = "$id/$position"
}

/** One step of the course. A level with no categories yet is shown as "coming soon". */
data class LibraryLevel(val number: Int, val title: String, val categories: List<LibraryCategory>) {
    val ready get() = categories.isNotEmpty()
}

/** An English Card of a category together with its stable id. */
data class LibraryEntry(val libraryId: String, val english: String)

/** The Cards of [category] that a Deck holding [existingIds] does not have yet, in order. */
fun missingEntries(category: LibraryCategory, existingIds: Set<String>): List<LibraryEntry> =
    category.cards.mapIndexed { i, english -> LibraryEntry(category.libraryId(i), english) }
        .filter { it.libraryId !in existingIds }

/** A category counts as added when the Deck already has every one of its Cards. */
fun isAdded(category: LibraryCategory, existingIds: Set<String>) = missingEntries(category, existingIds).isEmpty()

val libraryLevels: List<LibraryLevel> = listOf(
    LibraryLevel(1, "First steps", level1Categories),
    LibraryLevel(2, "Everyday life", level2Categories),
    LibraryLevel(3, "Getting by", level3Categories),
    LibraryLevel(4, "Intermediate", emptyList()),
    LibraryLevel(5, "Confident", emptyList()),
    LibraryLevel(6, "Upper intermediate", emptyList()),
    LibraryLevel(7, "Fluent", emptyList()),
    LibraryLevel(8, "Advanced", emptyList()),
    LibraryLevel(9, "Expert", emptyList()),
    LibraryLevel(10, "Mastery", emptyList()),
)

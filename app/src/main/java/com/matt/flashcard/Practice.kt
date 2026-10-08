package com.matt.flashcard

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** How many Cards the learner answered on one day; [day] is days since 1970-01-01 in the phone's own time zone. */
@Entity(tableName = "practice_day")
data class PracticeDay(@PrimaryKey val day: Long, val count: Int)

/** One slot of the week strip on the first screen. */
data class DayCount(val epochDay: Long, val count: Int, val isToday: Boolean) {
    /** One-letter weekday, e.g. "M". */
    val letter: String
        get() = LocalDate.ofEpochDay(epochDay).dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
}

internal fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

/** The last seven days ending today, oldest first, with 0 for days nothing was practiced. */
internal fun buildWeek(rows: List<PracticeDay>, today: Long): List<DayCount> =
    (6 downTo 0).map { back ->
        val day = today - back
        DayCount(day, rows.firstOrNull { it.day == day }?.count ?: 0, isToday = back == 0)
    }

/** Counts one answered Card for today. */
suspend fun FlashcardDao.recordPractice(day: Long = todayEpochDay()) {
    if (insertPracticeDay(PracticeDay(day, 1)) == -1L) bumpPracticeDay(day)
}

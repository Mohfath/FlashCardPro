package com.matt.flashcard

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    @Insert suspend fun insertDeck(deck: Deck): Long
    @Insert suspend fun insertCard(card: Card): Long
    @Update suspend fun updateCard(card: Card)
    @Delete suspend fun deleteCard(card: Card)
    @Delete suspend fun deleteDeck(deck: Deck)
    @Query("SELECT * FROM deck ORDER BY name") fun decks(): Flow<List<Deck>>
    @Query("SELECT * FROM deck ORDER BY id LIMIT 1") suspend fun firstDeck(): Deck?
    @Query("SELECT * FROM deck WHERE id = :id") suspend fun deck(id: Long): Deck?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertPracticeDay(day: PracticeDay): Long
    @Query("UPDATE practice_day SET count = count + 1 WHERE day = :day") suspend fun bumpPracticeDay(day: Long)
    @Query("SELECT * FROM practice_day WHERE day >= :from") fun practiceSince(from: Long): Flow<List<PracticeDay>>
    @Query("SELECT * FROM card") fun allCards(): Flow<List<Card>>
    @Query("SELECT * FROM card WHERE deckId = :deckId") suspend fun cardsIn(deckId: Long): List<Card>
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE card ADD COLUMN example TEXT")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE deck ADD COLUMN romanize INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE card ADD COLUMN romanization TEXT")
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS practice_day (day INTEGER NOT NULL, count INTEGER NOT NULL, PRIMARY KEY(day))")
    }
}

@Database(entities = [Deck::class, Card::class, PracticeDay::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): FlashcardDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "flashcards.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build().also { instance = it }
        }
    }
}

package com.example.parkingtimerapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [HistoryEntry::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new table with the new schema
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS history_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        messageType TEXT NOT NULL,
                        timeValue TEXT,
                        messageValue TEXT
                    )
                """)

                // Copy data from old table to new table
                database.execSQL("""
                    INSERT INTO history_new (id, timestamp, messageType, timeValue)
                    SELECT id, timestamp, 'TIME_SET', description
                    FROM history_table
                """)

                // Drop old table
                database.execSQL("DROP TABLE history_table")

                // Rename new table to the correct name
                database.execSQL("ALTER TABLE history_new RENAME TO history")
            }
        }
    }
}

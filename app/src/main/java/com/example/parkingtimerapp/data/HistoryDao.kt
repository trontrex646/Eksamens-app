package com.example.parkingtimerapp.data

import kotlinx.coroutines.flow.Flow
import androidx.room.*

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getHistoryInRange(startTime: Long, endTime: Long): Flow<List<HistoryEntry>>

    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(historyEntry: HistoryEntry)

    @Delete
    suspend fun delete(historyEntry: HistoryEntry)

    @Query("DELETE FROM history")
    suspend fun deleteAll()
}

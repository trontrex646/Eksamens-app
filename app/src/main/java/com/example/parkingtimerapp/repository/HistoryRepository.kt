package com.example.parkingtimerapp.repository

import android.util.Log
import com.example.parkingtimerapp.data.HistoryDao
import com.example.parkingtimerapp.data.HistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import java.text.SimpleDateFormat
import java.util.*

class HistoryRepository(private val historyDao: HistoryDao) {
    val allHistory: Flow<List<HistoryEntry>> = historyDao.getAllHistory()

    fun getHistoryInRange(startTime: Long, endTime: Long): Flow<List<HistoryEntry>> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT)
        Log.d("HistoryRepository", "=== EXECUTING SQL QUERY ===")
        Log.d("HistoryRepository", "Query: SELECT * FROM history WHERE timestamp BETWEEN $startTime AND $endTime")
        Log.d("HistoryRepository", "Start time: ${dateFormat.format(Date(startTime))} (${startTime})")
        Log.d("HistoryRepository", "End time: ${dateFormat.format(Date(endTime))} (${endTime})")
        
        return historyDao.getHistoryInRange(startTime, endTime)
            .onEach { entries ->
                Log.d("HistoryRepository", "=== FILTERED RESULTS ===")
                Log.d("HistoryRepository", "Filtered query returned ${entries.size} entries")
                if (entries.isEmpty()) {
                    Log.d("HistoryRepository", "No entries found in the specified range")
                } else {
                    entries.forEach { entry ->
                        val entryDate = dateFormat.format(Date(entry.timestamp))
                        val isInRange = entry.timestamp in startTime..endTime
                        Log.d("HistoryRepository", "Entry: $entryDate (${entry.timestamp}) - In range: $isInRange")
                    }
                }
            }
    }

    suspend fun insert(historyEntry: HistoryEntry) {
        historyDao.insert(historyEntry)
    }

    suspend fun delete(historyEntry: HistoryEntry) {
        historyDao.delete(historyEntry)
    }

    suspend fun deleteAll() {
        historyDao.deleteAll()
    }
}


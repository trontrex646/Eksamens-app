package com.example.parkingtimerapp.utils

import android.util.Log
import com.example.parkingtimerapp.data.HistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import java.text.SimpleDateFormat
import java.util.*

fun Flow<List<HistoryEntry>>.logResults(tag: String): Flow<List<HistoryEntry>> = onEach { entries ->
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT)
    Log.d(tag, "Query returned ${entries.size} entries")
    entries.forEach { entry ->
        Log.d(tag, "Entry timestamp: ${dateFormat.format(Date(entry.timestamp))}")
    }
} 
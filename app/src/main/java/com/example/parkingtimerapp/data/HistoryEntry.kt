package com.example.parkingtimerapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long,
    val messageType: String, // "TIME_SET" or "MESSAGE_SENT"
    val timeValue: String? = null, // For storing time format
    val messageValue: String? = null // For storing custom message
)

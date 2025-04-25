package com.example.parkingtimerapp.viewmodel

import android.util.Log
import androidx.lifecycle.*
import com.example.parkingtimerapp.data.HistoryEntry
import com.example.parkingtimerapp.repository.HistoryRepository
import com.example.parkingtimerapp.utils.logResults
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HistoryViewModel(private val repository: HistoryRepository) : ViewModel() {
    private val _currentFilter = MutableStateFlow<DateFilter?>(null)
    private val currentFilter = _currentFilter.asStateFlow()

    // Add isFiltered state
    private val _isFiltered = MutableStateFlow(false)
    val isFiltered = _isFiltered.asStateFlow()

    // Combine the filter with the history list
    val historyList = currentFilter.flatMapLatest { filter ->
        when (filter) {
            null -> {
                Log.d("HistoryViewModel", "No filter applied, showing all history")
                repository.allHistory.onEach { entries ->
                    Log.d("HistoryViewModel", "All history query returned ${entries.size} entries")
                    if (entries.isNotEmpty()) {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT)
                        Log.d("HistoryViewModel", "First item: ${dateFormat.format(Date(entries.first().timestamp))}")
                        Log.d("HistoryViewModel", "Last item: ${dateFormat.format(Date(entries.last().timestamp))}")
                    }
                }
            }
            else -> {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT)
                Log.d("HistoryViewModel", "=== FILTERING DETAILS ===")
                Log.d("HistoryViewModel", "Filter start time: ${dateFormat.format(Date(filter.startTime))} (${filter.startTime})")
                Log.d("HistoryViewModel", "Filter end time: ${dateFormat.format(Date(filter.endTime))} (${filter.endTime})")
                
                repository.getHistoryInRange(filter.startTime, filter.endTime)
                    .onEach { entries ->
                        Log.d("HistoryViewModel", "=== FILTERED RESULTS ===")
                        Log.d("HistoryViewModel", "Filtered query returned ${entries.size} entries")
                        if (entries.isEmpty()) {
                            Log.d("HistoryViewModel", "No entries found in the specified range")
                        } else {
                            entries.forEach { entry ->
                                val entryDate = dateFormat.format(Date(entry.timestamp))
                                val isInRange = entry.timestamp in filter.startTime..filter.endTime
                                Log.d("HistoryViewModel", "Entry: $entryDate (${entry.timestamp}) - In range: $isInRange")
                            }
                        }
                    }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addHistory(historyEntry: HistoryEntry) {
        viewModelScope.launch {
            repository.insert(historyEntry)
        }
    }

    fun deleteHistory(historyEntry: HistoryEntry) {
        viewModelScope.launch {
            repository.delete(historyEntry)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }

    fun filterHistoryByDate(startTime: Long, endTime: Long) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT)
        Log.d("HistoryViewModel", "=== SETTING NEW FILTER ===")
        Log.d("HistoryViewModel", "Start time: ${dateFormat.format(Date(startTime))} (${startTime})")
        Log.d("HistoryViewModel", "End time: ${dateFormat.format(Date(endTime))} (${endTime})")
        
        _currentFilter.value = DateFilter(startTime, endTime)
        _isFiltered.value = true
    }

    fun clearFilter() {
        Log.d("HistoryViewModel", "Clearing filter")
        _currentFilter.value = null
        _isFiltered.value = false
    }
}

data class DateFilter(
    val startTime: Long,
    val endTime: Long
)

// ✅ Update ViewModelFactory to accept a repository instead of HistoryDao
class HistoryViewModelFactory(private val repository: HistoryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

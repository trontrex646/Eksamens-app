package com.example.parkingtimerapp.ui.history

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.parkingtimerapp.R
import com.example.parkingtimerapp.data.AppDatabase
import com.example.parkingtimerapp.data.HistoryEntry
import com.example.parkingtimerapp.repository.HistoryRepository
import com.example.parkingtimerapp.ui.theme.ParkingTimerAppTheme
import com.example.parkingtimerapp.viewmodel.HistoryViewModel
import com.example.parkingtimerapp.viewmodel.HistoryViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.*

class HistoryFragment : Fragment() {
    private val viewModel: HistoryViewModel by viewModels {
        val database = AppDatabase.getDatabase(requireContext())
        val repository = HistoryRepository(database.historyDao())
        HistoryViewModelFactory(repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                ParkingTimerAppTheme {
                    HistoryScreen()
                }
            }
        }
    }

    @Composable
    private fun HistoryScreen() {
        var showDatePicker by remember { mutableStateOf(false) }
        val historyItems by viewModel.historyList.collectAsState(initial = emptyList())
        val isFiltered by viewModel.isFiltered.collectAsState(initial = false)
        val context = LocalContext.current

        // Add logging for UI updates
        LaunchedEffect(historyItems) {
            Log.d("HistoryScreen", "History items updated, count: ${historyItems.size}")
            if (historyItems.isNotEmpty()) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT)
                Log.d("HistoryScreen", "First item: ${dateFormat.format(Date(historyItems[0].timestamp))}")
                Log.d("HistoryScreen", "Last item: ${dateFormat.format(Date(historyItems[historyItems.size - 1].timestamp))}")
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Filter by Date button
                    FilledTonalButton(
                        onClick = { 
                            // Show the Material date picker dialog
                            val builder = MaterialAlertDialogBuilder(context)
                            val view = LayoutInflater.from(context).inflate(R.layout.dialog_date_range, null)
                            
                            val startDatePicker = view.findViewById<DatePicker>(R.id.startDatePicker)
                            val endDatePicker = view.findViewById<DatePicker>(R.id.endDatePicker)
                            
                            // Use ROOT locale for consistent date format
                            startDatePicker.firstDayOfWeek = Calendar.MONDAY
                            endDatePicker.firstDayOfWeek = Calendar.MONDAY
                            
                            // Enable year selection
                            startDatePicker.apply {
                                calendarViewShown = false
                                spinnersShown = true
                            }
                            
                            endDatePicker.apply {
                                calendarViewShown = false
                                spinnersShown = true
                            }
                            
                            val currentDate = Calendar.getInstance()
                            startDatePicker.updateDate(
                                currentDate.get(Calendar.YEAR),
                                currentDate.get(Calendar.MONTH),
                                1 // Start from beginning of month
                            )
                            endDatePicker.updateDate(
                                currentDate.get(Calendar.YEAR),
                                currentDate.get(Calendar.MONTH),
                                currentDate.getActualMaximum(Calendar.DAY_OF_MONTH) // End at end of month
                            )
                            
                            builder.setView(view)
                                .setTitle(context.getString(R.string.select_date_range))
                                .setPositiveButton(context.getString(R.string.filter)) { _, _ ->
                                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT)
                                    Log.d("HistoryFragment", "Selected date range:")
                                    Log.d("HistoryFragment", "Start picker - year: ${startDatePicker.year}, month: ${startDatePicker.month}, day: ${startDatePicker.dayOfMonth}")
                                    Log.d("HistoryFragment", "End picker - year: ${endDatePicker.year}, month: ${endDatePicker.month}, day: ${endDatePicker.dayOfMonth}")
                                    
                                    val startCalendar = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, startDatePicker.year)
                                        set(Calendar.MONTH, startDatePicker.month)
                                        set(Calendar.DAY_OF_MONTH, startDatePicker.dayOfMonth)
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    val endCalendar = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, endDatePicker.year)
                                        set(Calendar.MONTH, endDatePicker.month)
                                        set(Calendar.DAY_OF_MONTH, endDatePicker.dayOfMonth)
                                        set(Calendar.HOUR_OF_DAY, 23)
                                        set(Calendar.MINUTE, 59)
                                        set(Calendar.SECOND, 59)
                                        set(Calendar.MILLISECOND, 999)
                                    }

                                    Log.d("HistoryFragment", "Converted Calendar dates:")
                                    Log.d("HistoryFragment", "Start Calendar: ${dateFormat.format(startCalendar.time)} (${startCalendar.timeInMillis})")
                                    Log.d("HistoryFragment", "End Calendar: ${dateFormat.format(endCalendar.time)} (${endCalendar.timeInMillis})")
                                    
                                    // Ensure end date is not before start date
                                    if (endCalendar.timeInMillis < startCalendar.timeInMillis) {
                                        Toast.makeText(context, "End date cannot be before start date", Toast.LENGTH_SHORT).show()
                                        return@setPositiveButton
                                    }
                                    
                                    Log.d("HistoryFragment", "Date range: ${dateFormat.format(Date(startCalendar.timeInMillis))} to ${dateFormat.format(Date(endCalendar.timeInMillis))}")
                                    viewModel.filterHistoryByDate(startCalendar.timeInMillis, endCalendar.timeInMillis)
                                }
                                .setNegativeButton(context.getString(R.string.cancel), null)
                                .show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = getString(R.string.filter_by_date))
                    }

                    // Clear Filter button (only shown when filter is active)
                    if (isFiltered) {
                        FilledTonalButton(
                            onClick = {
                                viewModel.clearFilter()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = getString(R.string.clear_filter))
                        }
                    }

                    // Clear History button
                    FilledTonalButton(
                        onClick = { viewModel.clearAllHistory() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = getString(R.string.clear_history))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // History list or empty state
                if (historyItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(
                                    if (isFiltered) R.string.no_history_in_range
                                    else R.string.no_history_yet
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    Column {
                        if (isFiltered) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 1.dp
                            ) {
                                Text(
                                    text = stringResource(R.string.showing_filtered_results),
                                    modifier = Modifier.padding(8.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(historyItems) { historyItem ->
                                HistoryItemCard(
                                    historyEntry = historyItem,
                                    onDelete = { viewModel.deleteHistory(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun HistoryItemCard(
        historyEntry: HistoryEntry,
        onDelete: (HistoryEntry) -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    val description = when (historyEntry.messageType) {
                        "TIME_SET" -> stringResource(R.string.time_set_to, historyEntry.timeValue ?: "")
                        "MESSAGE_SENT" -> stringResource(R.string.message_sent_desc, historyEntry.messageValue ?: "")
                        else -> ""
                    }
                    
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.set_on, formatDate(historyEntry.timestamp)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onDelete(historyEntry) }) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    private fun formatDate(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.ROOT)
        return dateFormat.format(Date(timestamp))
    }
} 
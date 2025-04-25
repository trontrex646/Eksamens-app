package com.example.parkingtimerapp.ui.home

import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.parkingtimerapp.MainActivity
import com.example.parkingtimerapp.R
import com.example.parkingtimerapp.bluetooth.BluetoothDeviceListActivity
import com.example.parkingtimerapp.databinding.FragmentHomeBinding
import com.example.parkingtimerapp.ui.components.CustomTimePicker
import com.example.parkingtimerapp.ui.theme.ParkingTimerAppTheme
import com.google.firebase.perf.util.Timer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.parkingtimerapp.data.AppDatabase
import com.example.parkingtimerapp.data.HistoryEntry
import com.example.parkingtimerapp.repository.HistoryRepository
import com.example.parkingtimerapp.viewmodel.HistoryViewModel
import com.example.parkingtimerapp.viewmodel.HistoryViewModelFactory
import com.example.parkingtimerapp.utils.LanguageUtils
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.ExperimentalMaterial3Api
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
class HomeFragment : Fragment() {
    private lateinit var sharedPreferences: android.content.SharedPreferences
    private val bluetoothHelper by lazy {
        (requireActivity() as MainActivity).getBluetoothHelper()
    }
    private val historyViewModel: HistoryViewModel by viewModels {
        val database = AppDatabase.getDatabase(requireContext())
        val repository = HistoryRepository(database.historyDao())
        HistoryViewModelFactory(repository)
    }
    private var is24HourFormat: Boolean = false
    private var autoUpdateJob: Job? = null
    private var isAutoUpdateEnabled = false
    private var selectedHours = 0
    private var selectedMinutes = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        sharedPreferences = requireActivity().getSharedPreferences("AppSettings", android.content.Context.MODE_PRIVATE)
        val languageCode = sharedPreferences.getString("AppLanguage", "lv") ?: "lv"
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        // Create a context with the correct locale
        val updatedContext = LanguageUtils.setAppLanguage(requireContext(), languageCode)
        
        return ComposeView(updatedContext).apply {
            setContent {
                ParkingTimerAppTheme {
                    HomeScreen()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedPreferences = requireActivity().getSharedPreferences("AppSettings", android.content.Context.MODE_PRIVATE)
        is24HourFormat = sharedPreferences.getBoolean("Is24HourFormat", false)

        Log.d("HomeFragment", "Setting up connection state observer")
        // Set up connection state observer
        lifecycleScope.launch {
            bluetoothHelper.connectionState.collect { isConnected ->
                Log.d("HomeFragment", "Connection state changed: $isConnected")
                view.post {
                    // Update UI state when connection state changes
                    (view as? ComposeView)?.setContent {
                        ParkingTimerAppTheme {
                            HomeScreen()
                        }
                    }
                }
            }
        }

        // Restore last connected device
        val lastDeviceAddress = sharedPreferences.getString("LastConnectedDevice", null)
        if (lastDeviceAddress != null) {
            Log.d("HomeFragment", "Attempting to reconnect to last device: $lastDeviceAddress")
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val connected = bluetoothHelper.connect(lastDeviceAddress)
                    Log.d("HomeFragment", "Reconnection attempt result: $connected")
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Failed to reconnect: ${e.message}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Failed to reconnect: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Log.d("HomeFragment", "No last connected device found")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_BLUETOOTH_CONNECT && resultCode == android.app.Activity.RESULT_OK) {
            val deviceAddress = data?.getStringExtra("deviceAddress")
            if (deviceAddress != null) {
                Log.d("HomeFragment", "Attempting to connect to selected device: $deviceAddress")
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val connected = bluetoothHelper.connect(deviceAddress)
                        Log.d("HomeFragment", "Connection attempt result: $connected")
                        withContext(Dispatchers.Main) {
                            if (connected) {
                                // Save last connected device
                                sharedPreferences.edit().putString("LastConnectedDevice", deviceAddress).apply()
                                Log.d("HomeFragment", "Successfully connected and saved device address")
                            } else {
                                Log.e("HomeFragment", "Failed to connect to device")
                                Toast.makeText(requireContext(), "Failed to connect", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "Error connecting to device: ${e.message}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Error connecting: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Log.e("HomeFragment", "No device address received from device selection")
            }
        }
    }

    private fun startAutoUpdate() {
        autoUpdateJob = lifecycleScope.launch(Dispatchers.IO) {
            while (isActive && isAutoUpdateEnabled) {
                try {
                    val calendar = Calendar.getInstance()
                    val currentHours = calendar.get(Calendar.HOUR_OF_DAY)
                    val currentMinutes = calendar.get(Calendar.MINUTE)
                    val timeString = String.format(Locale.getDefault(), "T%02d:%02d", currentHours, currentMinutes)
                    Log.d("HomeFragment", "Auto-update sending time: $timeString")
                    
                    if (bluetoothHelper.isConnected()) {
                        bluetoothHelper.sendData(timeString)
                    }

                    delay(60000) // Wait for 1 minute before next update
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Auto-update error: ${e.message}", Toast.LENGTH_SHORT).show()
                        isAutoUpdateEnabled = false
                    }
                    break
                }
            }
        }
    }

    private fun stopAutoUpdate() {
        autoUpdateJob?.cancel()
        autoUpdateJob = null
    }

    private fun loadCustomMessages(): MutableSet<String> {
        val messages = sharedPreferences.getStringSet("customMessages", setOf())?.toMutableSet() ?: mutableSetOf()
        Log.d("HomeFragment", "Loaded custom messages: $messages")
        return messages
    }

    private fun saveCustomMessages(messages: Set<String>) {
        Log.d("HomeFragment", "Saving custom messages: $messages")
        sharedPreferences.edit().putStringSet("customMessages", messages).apply()
    }

    @Composable
    private fun HomeScreen() {
        var hours by remember { mutableStateOf(selectedHours) }
        var minutes by remember { mutableStateOf(selectedMinutes) }
        var showMessageDialog by remember { mutableStateOf(false) }
        var showAddMessageDialog by remember { mutableStateOf(false) }
        var showEditDialog by remember { mutableStateOf(false) }
        var showDeleteDialog by remember { mutableStateOf(false) }
        var currentMessage by remember { mutableStateOf("") }
        var selectedPresetMessage by remember { mutableStateOf("") }
        var customMessages by remember { mutableStateOf(loadCustomMessages()) }
        var isConnected by remember { mutableStateOf(bluetoothHelper.isConnected()) }
        val sheetState = rememberModalBottomSheetState()
        val context = LocalContext.current
        val activity = context as? MainActivity

        // Update connection state when it changes
        LaunchedEffect(Unit) {
            bluetoothHelper.connectionState.collect { connected ->
                isConnected = connected
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Connection status card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isConnected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isConnected) getString(R.string.connected) else getString(R.string.not_connected),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isConnected)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = if (isConnected) getString(R.string.device_ready) else getString(R.string.connect_device),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isConnected)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            FilledTonalButton(
                                onClick = {
                                    if (isConnected) {
                                        bluetoothHelper.disconnect()
                                        isConnected = false
                                    } else {
                                        val intent = Intent(requireContext(), BluetoothDeviceListActivity::class.java)
                                        startActivityForResult(intent, REQUEST_CODE_BLUETOOTH_CONNECT)
                                    }
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isConnected)
                                        MaterialTheme.colorScheme.secondaryContainer
                                    else
                                        MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = if (isConnected)
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(
                                    imageVector = if (isConnected) Icons.Outlined.BluetoothDisabled else Icons.Outlined.Bluetooth,
                                    contentDescription = if (isConnected) "Disconnect" else "Connect",
                                    tint = if (isConnected)
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isConnected) getString(R.string.disconnect) else getString(R.string.connect),
                                    color = if (isConnected)
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    // Time picker section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            try {
                                                val calendar = Calendar.getInstance()
                                                val currentHours = calendar.get(Calendar.HOUR_OF_DAY)
                                                val currentMinutes = calendar.get(Calendar.MINUTE)
                                                val timeString = String.format(Locale.getDefault(), "%02d:%02d", currentHours, currentMinutes)
                                                Log.d("HomeFragment", "Sending current time: $timeString")
                                                bluetoothHelper.sendData(timeString)
                                                
                                                // Save to history
                                                    val historyEntry = createHistoryEntry(timeString, is24HourFormat, currentHours, currentMinutes)
                                                historyViewModel.addHistory(historyEntry)
                                                
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(requireContext(), "Current time sent: $timeString", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(requireContext(), "Failed to send time: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    enabled = isConnected,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                        disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Icon(
                                        Icons.Outlined.Timer,
                                        contentDescription = "Send Current Time",
                                        tint = if (isConnected)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        getString(R.string.current_time),
                                        color = if (isConnected)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            CustomTimePicker(
                                initialHours = hours,
                                initialMinutes = minutes,
                                is24HourFormat = is24HourFormat, // Use the setting from SharedPreferences
                                onTimeChanged = { newHours, newMinutes ->
                                    hours = newHours
                                    minutes = newMinutes
                                }
                            )

                            Button(
                                onClick = {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        try {
                                            val formattedTime = if (is24HourFormat) {
                                                String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
                                            } else {
                                                // Convert to 12-hour format
                                                val amPm = if (hours >= 12) "PM" else "AM"
                                                val displayHours = when {
                                                    hours == 0 -> 12 // Midnight case
                                                    hours == 12 -> 12 // Noon case
                                                    hours > 12 -> hours - 12
                                                    else -> hours
                                                }
                                                String.format(Locale.getDefault(), "%02d:%02d %s", displayHours, minutes, amPm)
                                            }

                                            Log.d("HomeFragment", "Sending selected time: $formattedTime")
                                            bluetoothHelper.sendData(formattedTime)

                                            // Save to history
                                                val historyEntry = createHistoryEntry(formattedTime, is24HourFormat, hours, minutes)
                                            historyViewModel.addHistory(historyEntry)

                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(requireContext(), "Time sent: $formattedTime", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(requireContext(), "Failed to send time: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = isConnected,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                                )
                            ) {
                                Icon(
                                    Icons.Outlined.Send,
                                    contentDescription = "Send Time",
                                    tint = if (isConnected)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    getString(R.string.send_selected_time),
                                    color = if (isConnected)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                        // Preset messages button
                        FilledTonalButton(
                            onClick = { 
                                Log.d("HomeFragment", "Preset messages button clicked")
                                showMessageDialog = true
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Outlined.Email,
                                    contentDescription = getString(R.string.preset_messages),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = getString(R.string.preset_messages),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                if (showMessageDialog) {
                    ModalBottomSheet(
                        onDismissRequest = { showMessageDialog = false },
                        sheetState = sheetState,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .imePadding()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = getString(R.string.preset_messages),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                                FilledTonalButton(
                                onClick = { 
                                    showMessageDialog = false
                                    showAddMessageDialog = true 
                                }
                                ) {
                                Icon(Icons.Outlined.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                Text(getString(R.string.add_message))
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            if (customMessages.isEmpty()) {
                                    Text(
                                    text = getString(R.string.no_preset_messages),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(32.dp)
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f, false)
                                ) {
                                    items(customMessages.toList()) { message ->
                                        ListItem(
                                            headlineContent = { Text(message) },
                                            trailingContent = {
                                    Row {
                                                    IconButton(onClick = {
                                                selectedPresetMessage = message
                                                        currentMessage = message
                                                        showMessageDialog = false
                                                showEditDialog = true
                                                    }) {
                                                        Icon(Icons.Outlined.Edit, "Edit")
                                        }
                                                    IconButton(onClick = {
                                                selectedPresetMessage = message
                                                        showMessageDialog = false
                                                showDeleteDialog = true
                                                    }) {
                                                        Icon(Icons.Outlined.Delete, "Delete")
                                        }
                                        IconButton(
                                            onClick = {
                                                lifecycleScope.launch(Dispatchers.IO) {
                                                    try {
                                                        bluetoothHelper.sendData(message)
                                                                    val historyEntry = createMessageHistoryEntry(message)
                                                                    historyViewModel.addHistory(historyEntry)
                                                        withContext(Dispatchers.Main) {
                                                                        Toast.makeText(context, getString(R.string.message_sent), Toast.LENGTH_SHORT).show()
                                                                        showMessageDialog = false
                                                        }
                                                    } catch (e: Exception) {
                                                        withContext(Dispatchers.Main) {
                                                                        Toast.makeText(context, getString(R.string.failed_to_send, e.message), Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                                        },
                                                        enabled = isConnected
                                        ) {
                                                        Icon(Icons.Outlined.Send, "Send")
                                        }
                                    }
                                }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    // Add Message Dialog
                if (showAddMessageDialog) {
                    ModalBottomSheet(
                        onDismissRequest = { showAddMessageDialog = false },
                        sheetState = sheetState,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .imePadding()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = getString(R.string.add_message),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = currentMessage,
                                    onValueChange = { currentMessage = it },
                                    label = { Text(getString(R.string.message)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showAddMessageDialog = false }) {
                                    Text(getString(R.string.cancel))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (currentMessage.isNotBlank()) {
                                            customMessages = (customMessages + currentMessage).toMutableSet()
                                            saveCustomMessages(customMessages)
                                            currentMessage = ""
                                            showAddMessageDialog = false
                                        }
                                    }
                                ) {
                                    Text(getString(R.string.add))
                                }
                                }
                            Spacer(modifier = Modifier.height(8.dp))
                            }
                    }
                    }

                    // Edit Message Dialog
                    if (showEditDialog) {
                    ModalBottomSheet(
                            onDismissRequest = { showEditDialog = false },
                        sheetState = sheetState,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .imePadding()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = getString(R.string.edit_message),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                value = currentMessage,
                                onValueChange = { currentMessage = it },
                                    label = { Text(getString(R.string.message)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showEditDialog = false }) {
                                    Text(getString(R.string.cancel))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (currentMessage.isNotBlank()) {
                                            customMessages = customMessages.map { 
                                                if (it == selectedPresetMessage) currentMessage else it 
                                            }.toMutableSet()
                                            saveCustomMessages(customMessages)
                                            showEditDialog = false
                                        }
                                    }
                                ) {
                                    Text(getString(R.string.save))
                                }
                                }
                            Spacer(modifier = Modifier.height(8.dp))
                            }
                    }
                    }

                    // Delete Message Dialog
                    if (showDeleteDialog) {
                    ModalBottomSheet(
                            onDismissRequest = { showDeleteDialog = false },
                        sheetState = sheetState,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .imePadding()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = getString(R.string.delete_message),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(getString(R.string.delete_confirm))
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showDeleteDialog = false }) {
                                    Text(getString(R.string.cancel))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        customMessages = customMessages.filter { it != selectedPresetMessage }.toMutableSet()
                                        saveCustomMessages(customMessages)
                                        showDeleteDialog = false
                                    }
                                ) {
                                    Text(getString(R.string.delete))
                                }
                                }
                            Spacer(modifier = Modifier.height(8.dp))
                            }
                    }
                }
            }
        }
    }

    @Composable
    private fun MessageDialog(
        currentMessage: String,
        onMessageChange: (String) -> Unit,
        onDismiss: () -> Unit,
        onSend: (String) -> Unit,
        onSavePreset: (String) -> Unit,
        onEditPreset: () -> Unit,
        onDeletePreset: () -> Unit,
        customMessages: List<String>,
        selectedPresetMessage: String,
        onPresetSelected: (String) -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Send Message", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = currentMessage,
                        onValueChange = onMessageChange,
                        label = { Text("Message", color = MaterialTheme.colorScheme.onSurface) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    if (customMessages.isNotEmpty()) {
                        Text(
                            text = "Preset Messages",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        customMessages.forEach { message ->
                            TextButton(
                                onClick = { onPresetSelected(message) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = message,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSend(currentMessage) }
                    ) {
                        Text("Send", color = MaterialTheme.colorScheme.onPrimary)
                    }

                    TextButton(
                        onClick = { onSavePreset(currentMessage) }
                    ) {
                        Text("Save Preset", color = MaterialTheme.colorScheme.primary)
                    }

                    if (customMessages.isNotEmpty()) {
                        TextButton(
                            onClick = onEditPreset
                        ) {
                            Text("Edit", color = MaterialTheme.colorScheme.primary)
                        }

                        TextButton(
                            onClick = onDeletePreset
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    @Composable
    private fun EditPresetDialog(
        currentMessage: String,
        onDismiss: () -> Unit,
        onSave: (String) -> Unit
    ) {
        var message by remember { mutableStateOf(currentMessage) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Edit Preset Message", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message", color = MaterialTheme.colorScheme.onSurface) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = { onSave(message) }
                ) {
                    Text("Save", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    @Composable
    private fun DeletePresetDialog(
        customMessages: List<String>,
        onDismiss: () -> Unit,
        onDelete: (String) -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Delete Preset Message") },
            text = {
                Column {
                    customMessages.forEach { message ->
                        TextButton(
                            onClick = { onDelete(message) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(message)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    private fun createHistoryEntry(timeString: String, is24HourFormat: Boolean, hours: Int, minutes: Int): HistoryEntry {
        val calendar = Calendar.getInstance()
        val timestamp = calendar.timeInMillis
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT)
        Log.d("HomeFragment", "Creating history entry with timestamp: ${dateFormat.format(Date(timestamp))} (${timestamp})")
        val timeFormat = if (is24HourFormat) {
            String.format("%02d:%02d", hours, minutes)
        } else {
            String.format("%02d:%02d %s", 
                if (hours > 12) hours - 12 
                else if (hours == 0) 12 
                else hours, 
                minutes, 
                if (hours >= 12) "PM" else "AM")
        }
        
        return HistoryEntry(
            timestamp = timestamp,
            messageType = "TIME_SET",
            timeValue = timeFormat
        )
    }

    private fun createMessageHistoryEntry(message: String): HistoryEntry {
        return HistoryEntry(
            timestamp = System.currentTimeMillis(),
            messageType = "MESSAGE_SENT",
            messageValue = message
        )
    }

    companion object {
        private const val REQUEST_CODE_BLUETOOTH_CONNECT = 1
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopAutoUpdate()
    }
}
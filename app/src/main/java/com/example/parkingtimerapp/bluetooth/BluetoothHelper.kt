package com.example.parkingtimerapp.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

class BluetoothHelper(private val context: Context) {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private val UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var isConnecting = false
    private val CONNECTION_TIMEOUT_MS = 10000L
    private val MAX_RETRY_ATTEMPTS = 3
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var connectionJob: Job? = null
    private var reconnectJob: Job? = null
    private var lastConnectedDevice: BluetoothDevice? = null

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState

    init {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        Log.d("BluetoothHelper", "Initialized with adapter: ${bluetoothAdapter?.name}")
    }

    suspend fun connect(address: String): Boolean {
        if (isConnecting) {
            Log.d("BluetoothHelper", "Already attempting to connect, skipping new connection request")
            return false
        }
        isConnecting = true
        Log.d("BluetoothHelper", "Starting connection attempt to device: $address")
        
        try {
            disconnect()
            
            val device = bluetoothAdapter?.getRemoteDevice(address)
            if (device == null) {
                Log.e("BluetoothHelper", "Device not found for address: $address")
                isConnecting = false
                return false
            }

            return withContext(Dispatchers.IO) {
                var attempts = 0
                var connected = false

                while (attempts < MAX_RETRY_ATTEMPTS && !connected) {
                    try {
                        attempts++
                        Log.d("BluetoothHelper", "Attempting to connect to ${device.name} (attempt $attempts)")
                        
                        bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID_SPP)
                        Log.d("BluetoothHelper", "Socket created, attempting to connect...")
                        bluetoothSocket?.connect()
                        
                        if (bluetoothSocket?.isConnected == true) {
                            outputStream = bluetoothSocket?.outputStream
                            if (outputStream == null) {
                                Log.e("BluetoothHelper", "Failed to get output stream after successful connection")
                                throw IOException("Failed to get output stream")
                            }
                            Log.d("BluetoothHelper", "Connected successfully to ${device.name}")
                            connected = true
                            _connectionState.value = true
                            lastConnectedDevice = device
                            startConnectionMonitoring()
                        } else {
                            Log.e("BluetoothHelper", "Socket connected but isConnected returned false")
                        }
                    } catch (e: IOException) {
                        Log.e("BluetoothHelper", "Connection attempt $attempts failed: ${e.message}")
                        closeSocket()
                        if (attempts < MAX_RETRY_ATTEMPTS) {
                            Log.d("BluetoothHelper", "Waiting before retry...")
                            delay(1000) // Wait 1 second before retrying
                        }
                    }
                }
                
                isConnecting = false
                if (!connected) {
                    Log.e("BluetoothHelper", "Failed to connect after $MAX_RETRY_ATTEMPTS attempts")
                }
                connected
            }
        } catch (e: Exception) {
            Log.e("BluetoothHelper", "Error in connect: ${e.message}")
            isConnecting = false
            _connectionState.value = false
            throw e
        }
    }

    private fun startConnectionMonitoring() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            Log.d("BluetoothHelper", "Starting connection monitoring")
            while (isActive) {
                if (!isConnected()) {
                    Log.d("BluetoothHelper", "Connection lost, attempting to reconnect")
                    _connectionState.value = false
                    lastConnectedDevice?.address?.let { 
                        Log.d("BluetoothHelper", "Attempting to reconnect to ${lastConnectedDevice?.name}")
                        connect(it)
                    }
                }
                delay(1000) // Check connection every second
            }
        }
    }

    private fun closeSocket() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e("BluetoothHelper", "Error closing socket: ${e.message}")
        } finally {
            outputStream = null
            bluetoothSocket = null
        }
    }

    suspend fun sendData(message: String) {
        if (!isConnected()) {
            Log.e("BluetoothHelper", "Cannot send data: Not connected")
            throw IOException("Not connected")
        }

        withContext(Dispatchers.IO) {
            try {
                Log.d("BluetoothHelper", "Attempting to send data: $message")
                val data = "$message\n".toByteArray()  // Add newline to end of message
                outputStream?.let { stream ->
                    stream.write(data)
                    stream.flush()
                    Log.d("BluetoothHelper", "Data sent successfully")
                } ?: throw IOException("Output stream is null")
            } catch (e: IOException) {
                Log.e("BluetoothHelper", "Error sending data: ${e.message}")
                _connectionState.value = false
                closeSocket()
                throw e
            }
        }
    }

    fun disconnect() {
        connectionJob?.cancel()
        reconnectJob?.cancel()
        closeSocket()
        _connectionState.value = false
        isConnecting = false
        Log.d("BluetoothHelper", "Disconnected successfully")
    }

    fun isConnected(): Boolean {
        return try {
            bluetoothSocket?.isConnected == true && !isConnecting && outputStream != null
        } catch (e: Exception) {
            false
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun onDestroy() {
        scope.cancel()
        disconnect()
    }
}
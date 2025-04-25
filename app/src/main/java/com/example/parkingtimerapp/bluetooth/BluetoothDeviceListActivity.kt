package com.example.parkingtimerapp.bluetooth

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.parkingtimerapp.R

class BluetoothDeviceListActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var deviceListView: ListView
    private lateinit var scanButton: Button
    private val deviceList = mutableListOf<String>()
    private val devices = mutableListOf<BluetoothDevice>()
    private var isScanning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_device_list)

        deviceListView = findViewById(R.id.listDevices)
        scanButton = findViewById(R.id.scanButton)

        if (hasBluetoothPermissions()) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter.isEnabled) {
                setupUI()
            } else {
                Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_LONG).show()
                finish()
            }
        } else {
            Log.e("Bluetooth", "Bluetooth permissions not granted!")
            Toast.makeText(this, "Bluetooth permissions required", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupUI() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceList)
        deviceListView.adapter = adapter

        deviceListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val device = devices[position]
            val returnIntent = Intent()
            returnIntent.putExtra("deviceAddress", device.address)
            returnIntent.putExtra("deviceName", device.name)
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
        }

        scanButton.setOnClickListener {
            if (!isScanning) {
                startScan()
            } else {
                stopScan()
            }
        }

        // Start initial scan
        startScan()
    }

    private fun startScan() {
        if (!isScanning) {
            deviceList.clear()
            devices.clear()
            (deviceListView.adapter as ArrayAdapter<*>).notifyDataSetChanged()
            
            // Show paired devices first
            val pairedDevices = bluetoothAdapter.bondedDevices
            if (!pairedDevices.isNullOrEmpty()) {
                for (device in pairedDevices) {
                    addDevice(device)
                }
            }

            // Start discovery
            bluetoothAdapter.startDiscovery()
            isScanning = true
            scanButton.text = "Stop Scan"
            Toast.makeText(this, "Scanning for devices...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopScan() {
        if (isScanning) {
            bluetoothAdapter.cancelDiscovery()
            isScanning = false
            scanButton.text = "Start Scan"
            Toast.makeText(this, "Scan stopped", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addDevice(device: BluetoothDevice) {
        if (!devices.contains(device)) {
            val deviceInfo = "${device.name ?: "Unknown Device"}\n${device.address}"
            deviceList.add(deviceInfo)
            devices.add(device)
            (deviceListView.adapter as ArrayAdapter<*>).notifyDataSetChanged()
            Log.d("Bluetooth", "Found device: ${device.name} - ${device.address}")
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScan()
    }
}

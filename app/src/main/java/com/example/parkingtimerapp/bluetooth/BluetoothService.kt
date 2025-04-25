package com.example.parkingtimerapp.bluetooth

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Build
import android.os.IBinder
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BluetoothService : Service() {
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    companion object {
        private val HC06_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra("device", BluetoothDevice::class.java)
        } else {
            intent?.getParcelableExtra("device")
        }

        device?.let {
            connectToDevice(it)
        }

        return START_STICKY
    }

    private fun connectToDevice(device: BluetoothDevice) {
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(HC06_UUID)
            bluetoothSocket?.connect()
            inputStream = bluetoothSocket?.inputStream
            outputStream = bluetoothSocket?.outputStream
        } catch (e: IOException) {
            e.printStackTrace()
            stopSelf()
        }
    }

    fun sendData(data: String) {
        try {
            outputStream?.write(data.toByteArray())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        try {
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}

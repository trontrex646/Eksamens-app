package com.example.parkingtimerapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.parkingtimerapp.bluetooth.BluetoothHelper
import com.example.parkingtimerapp.databinding.ActivityMainBinding
import com.example.parkingtimerapp.utils.LanguageUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

private val REQUIRED_BLUETOOTH_PERMISSIONS = arrayOf(
    Manifest.permission.BLUETOOTH,
    Manifest.permission.BLUETOOTH_ADMIN,
    Manifest.permission.BLUETOOTH_SCAN,
    Manifest.permission.BLUETOOTH_CONNECT,
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

private const val REQUEST_CODE_BLUETOOTH = 100

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bluetoothHelper: com.example.parkingtimerapp.bluetooth.BluetoothHelper
    private lateinit var sharedPreferences: SharedPreferences
    private var reconnectionJob: Job? = null
    private val maxReconnectionAttempts = 3

    override fun attachBaseContext(newBase: Context) {
        val sharedPreferences = newBase.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val languageCode = sharedPreferences.getString("AppLanguage", "lv") ?: "lv"
        val context = LanguageUtils.setAppLanguage(newBase, languageCode)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply language configuration before super.onCreate
        sharedPreferences = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val languageCode = sharedPreferences.getString("AppLanguage", "lv") ?: "lv"
        val config = resources.configuration
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        createConfigurationContext(config)
        resources.updateConfiguration(config, resources.displayMetrics)

        super.onCreate(savedInstanceState)

        // Check if this is a restart after language change
        if (intent.getBooleanExtra("LANGUAGE_CHANGE_RESTART", false)) {
            // Clear the flag
            intent.removeExtra("LANGUAGE_CHANGE_RESTART")
        }

        // Apply dark mode settings
        val isDarkMode = sharedPreferences.getBoolean("DarkMode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check and request permissions first
        if (checkAndRequestBluetoothPermissions()) {
            // Only initialize Bluetooth if permissions are already granted
            initializeBluetoothAndReconnect()
        }

        setupNavigation()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        navHostFragment?.navController?.let { navController ->
            binding.navView.setupWithNavController(navController)
        }
    }

    private fun initializeBluetoothAndReconnect() {
        bluetoothHelper = com.example.parkingtimerapp.bluetooth.BluetoothHelper(this)
        
        // Start reconnection attempt
        val lastDeviceAddress = sharedPreferences.getString("LastConnectedDevice", null)
        if (lastDeviceAddress != null) {
            attemptReconnection(lastDeviceAddress)
        }
    }

    private fun attemptReconnection(deviceAddress: String) {
        reconnectionJob?.cancel() // Cancel any existing reconnection attempts
        
        reconnectionJob = lifecycleScope.launch(Dispatchers.IO) {
            var attempts = 0
            var connected = false
            
            while (attempts < maxReconnectionAttempts && !connected) {
                attempts++
                try {
                    Log.d("MainActivity", "Attempting to reconnect to $deviceAddress (Attempt $attempts/$maxReconnectionAttempts)")
                    connected = bluetoothHelper.connect(deviceAddress)
                    
                    if (connected) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@MainActivity,
                                "Successfully reconnected to device",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else if (attempts < maxReconnectionAttempts) {
                        delay(1000) // Wait 1 second before next attempt
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Reconnection attempt $attempts failed: ${e.message}")
                    if (attempts >= maxReconnectionAttempts) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@MainActivity,
                                "Failed to reconnect after $maxReconnectionAttempts attempts",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        delay(1000) // Wait 1 second before next attempt
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Try to reconnect if we're not already connected
        if (!bluetoothHelper.isConnected()) {
            val lastDeviceAddress = sharedPreferences.getString("LastConnectedDevice", null)
            if (lastDeviceAddress != null) {
                attemptReconnection(lastDeviceAddress)
            }
        }
    }

    private fun checkAndRequestBluetoothPermissions(): Boolean {
        val missingPermissions = REQUIRED_BLUETOOTH_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            // Show a toast to inform the user about the permissions
            Toast.makeText(
                this,
                "This app needs Bluetooth and location permissions to function properly",
                Toast.LENGTH_LONG
            ).show()
            
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                REQUEST_CODE_BLUETOOTH
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_BLUETOOTH) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                Toast.makeText(this, "Permissions granted! Initializing Bluetooth...", Toast.LENGTH_SHORT).show()
                initializeBluetoothAndReconnect()
            } else {
                Toast.makeText(
                    this,
                    "Permissions are required for Bluetooth functionality. Please grant the permissions in Settings.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val languageCode = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
            .getString("AppLanguage", "lv") ?: "lv"
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        createConfigurationContext(config)
        LanguageUtils.setAppLanguage(this, languageCode)
    }

    override fun onDestroy() {
        super.onDestroy()
        reconnectionJob?.cancel()
        bluetoothHelper.onDestroy()
    }

    fun getBluetoothHelper(): com.example.parkingtimerapp.bluetooth.BluetoothHelper {
        return bluetoothHelper
    }

    fun restartAppForLanguageChange(newLanguageCode: String) {
        Log.d("MainActivity", "Performing complete app restart for language change to: $newLanguageCode")
        
        // Save the new language
        sharedPreferences.edit().putString("AppLanguage", newLanguageCode).apply()
        
        // Clean up current state
        bluetoothHelper.disconnect()
        bluetoothHelper.onDestroy()
        
        // Create intent for restarting the app from launcher
        val packageManager = packageManager
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("LANGUAGE_CHANGE_RESTART", true)
        }
        
        if (intent != null) {
            // Post delayed to ensure proper cleanup
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(intent)
                
                // Force stop the app completely
                finishAffinity() // Close all activities
                finishAndRemoveTask() // Remove from recent tasks
                
                // Kill the app process
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(0)
            }, 100)
        }
    }
}

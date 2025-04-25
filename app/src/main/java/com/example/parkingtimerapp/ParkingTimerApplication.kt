package com.example.parkingtimerapp

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import com.example.parkingtimerapp.utils.LanguageUtils
import java.util.Locale

class ParkingTimerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        updateLocale()
    }

    override fun attachBaseContext(base: Context) {
        val sharedPreferences = base.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val languageCode = sharedPreferences.getString("AppLanguage", "lv") ?: "lv"
        super.attachBaseContext(LanguageUtils.setAppLanguage(base, languageCode))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateLocale()
    }

    private fun updateLocale() {
        val sharedPreferences = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val languageCode = sharedPreferences.getString("AppLanguage", "lv") ?: "lv"
        val locale = LanguageUtils.getLocale(languageCode)
        
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            createConfigurationContext(config)
        } else {
            resources.updateConfiguration(config, resources.displayMetrics)
        }
    }
} 
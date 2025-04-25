package com.example.parkingtimerapp.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import java.util.*

object LanguageUtils {
    // Map display names to language codes
    val SUPPORTED_LANGUAGES = mapOf(
        "Latviešu" to "lv",
        "English" to "en",
        "Spanish" to "es",
        "French" to "fr",
        "German" to "de"
    )

    fun setAppLanguage(context: Context, languageCode: String): Context {
        val locale = getLocale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        val resources = context.resources
        resources.updateConfiguration(config, resources.displayMetrics)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLayoutDirection(locale)
        }
        
        return context.createConfigurationContext(config)
    }

    fun getLocale(languageCode: String): Locale {
        return Locale(languageCode)
    }

    fun getDisplayName(languageCode: String): String {
        return SUPPORTED_LANGUAGES.entries.find { it.value == languageCode }?.key ?: "Latviešu"
    }

    fun getLanguageCode(displayName: String): String {
        return SUPPORTED_LANGUAGES[displayName] ?: "lv"
    }

    fun wrapContext(context: Context): Context {
        val sharedPreferences = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val languageCode = sharedPreferences.getString("AppLanguage", "lv") ?: "lv"
        return setAppLanguage(context, languageCode)
    }
} 
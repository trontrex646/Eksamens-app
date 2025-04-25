package com.example.parkingtimerapp.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.parkingtimerapp.MainActivity
import com.example.parkingtimerapp.databinding.FragmentSettingsBinding
import com.example.parkingtimerapp.utils.LanguageUtils

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences =
            requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        // Dark Mode
        val isDarkMode = sharedPreferences.getBoolean("DarkMode", false)
        binding.switchDarkMode.isChecked = isDarkMode
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("DarkMode", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
            requireActivity().recreate()
        }



        // Time Format
        val is24HourFormat = sharedPreferences.getBoolean("Is24HourFormat", false)
        binding.switchTimeFormat.isChecked = is24HourFormat
        binding.switchTimeFormat.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("Is24HourFormat", isChecked).apply()
        }

        // Language Selection
        setupLanguageSpinner()

        // Bluetooth Device Management
        binding.btnBluetoothSettings.setOnClickListener {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            startActivity(intent)
        }

        // Clock Themes
        val themeMap = mapOf(
            getString(com.example.parkingtimerapp.R.string.theme_default) to "Default",
            getString(com.example.parkingtimerapp.R.string.theme_dark) to "Dark",
            getString(com.example.parkingtimerapp.R.string.theme_colorful) to "Colorful",
            getString(com.example.parkingtimerapp.R.string.theme_minimalist) to "Minimalist"
        )
        
        val themes = themeMap.keys.toTypedArray()
        val themeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, themes)
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerClockTheme.adapter = themeAdapter

        // Set current theme selection
        val currentTheme = sharedPreferences.getString("ClockTheme", "Default")?.let { savedTheme ->
            themes.find { themeMap[it] == savedTheme } ?: themes[0]
        } ?: themes[0]
        
        val currentThemePosition = themes.indexOf(currentTheme)
        if (currentThemePosition != -1) {
            binding.spinnerClockTheme.setSelection(currentThemePosition)
        }

        binding.spinnerClockTheme.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedTheme = themes[position]
                    val internalThemeName = themeMap[selectedTheme] ?: "Default"
                    sharedPreferences.edit()
                        .putString("ClockTheme", internalThemeName)
                        .apply()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }



        // App Version
        val packageInfo =
            requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
        val versionName = packageInfo.versionName
        binding.tvAppVersion.text =
            getString(com.example.parkingtimerapp.R.string.version, versionName)


    }

    private fun setupLanguageSpinner() {
        val languages = LanguageUtils.SUPPORTED_LANGUAGES.keys.toTypedArray()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLanguage.adapter = adapter

        // Set current language selection
        val currentLanguageCode = sharedPreferences.getString("AppLanguage", "lv") ?: "lv"
        val currentLanguage = LanguageUtils.getDisplayName(currentLanguageCode)
        val currentPosition = languages.indexOf(currentLanguage)
        
        // Flag to prevent triggering during initial setup
        var isInitialSelection = true
        
        if (currentPosition != -1) {
            binding.spinnerLanguage.setSelection(currentPosition)
        }

        binding.spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isInitialSelection) {
                    isInitialSelection = false
                    return
                }
                
                val selectedLanguage = languages[position]
                val languageCode = LanguageUtils.getLanguageCode(selectedLanguage)
                val currentCode = sharedPreferences.getString("AppLanguage", "lv") ?: "lv"
                
                if (languageCode != currentCode) {
                    // Use the new restart method from MainActivity
                    (requireActivity() as MainActivity).restartAppForLanguageChange(languageCode)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
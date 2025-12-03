package com.example.todoapp2025

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsViewModel(private val context: Context) : ViewModel() {


    // Preference keys
    private val DARK_MODE = booleanPreferencesKey("dark_mode")
    private val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
    private val DAILY_SUMMARY_ENABLED = booleanPreferencesKey("daily_summary_enabled")

    // Flows
    val isDarkMode = context.dataStore.data.map { prefs -> prefs[DARK_MODE] ?: false }
    val isRemindersEnabled = context.dataStore.data.map { prefs -> prefs[REMINDERS_ENABLED] ?: false }
    val isDailySummaryEnabled = context.dataStore.data.map { prefs -> prefs[DAILY_SUMMARY_ENABLED] ?: false }

    // Setters
    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { prefs -> prefs[DARK_MODE] = enabled }
        }
    }

    fun setReminders(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { prefs -> prefs[REMINDERS_ENABLED] = enabled }
        }
    }

    fun setDailySummary(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { prefs -> prefs[DAILY_SUMMARY_ENABLED] = enabled }
        }
    }


}

// Factory
class SettingsVMFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

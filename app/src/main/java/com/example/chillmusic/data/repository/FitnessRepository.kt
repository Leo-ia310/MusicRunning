package com.example.chillmusic.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FitnessRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val _dailySteps = MutableStateFlow(0)
    val dailySteps: StateFlow<Int> = _dailySteps.asStateFlow()

    init {
        loadTodaySteps()
    }

    private fun getTodayDateString(): String {
        return dateFormat.format(Date())
    }

    private fun loadTodaySteps() {
        val today = getTodayDateString()
        val steps = prefs.getInt(today, 0)
        _dailySteps.value = steps
    }

    fun addStep() {
        val today = getTodayDateString()
        val currentSteps = prefs.getInt(today, 0)
        val newSteps = currentSteps + 1
        
        prefs.edit().putInt(today, newSteps).apply()
        _dailySteps.value = newSteps
    }

    fun getHistory(): Map<String, Int> {
        val history = mutableMapOf<String, Int>()
        prefs.all.forEach { (key, value) ->
            if (value is Int) {
                history[key] = value
            }
        }
        return history
    }
}

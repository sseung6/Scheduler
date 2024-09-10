package com.example.diary

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CalendarFragment())
                .commit()
        }
        // 다크 모드 상태 적용
        applyDarkMode(getDarkModeState())
    }
    fun applyDarkMode(isDarkMode: Boolean) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
    fun getDarkModeState(): Boolean {
        val sharedPreferences = getPreferences(Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("DARK_MODE", false)
    }
    fun toggleDarkMode() {
        val sharedPreferences = getPreferences(Context.MODE_PRIVATE)
        val isDarkMode = !getDarkModeState()
        with(sharedPreferences.edit()) {
            putBoolean("DARK_MODE", isDarkMode)
            apply()
        }
        applyDarkMode(isDarkMode)

        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is DateFragment) {
            fragment.updateUIForDarkMode()
        } else if (fragment is CalendarFragment) {
            fragment.updateUIForDarkMode()
        }
    }
}

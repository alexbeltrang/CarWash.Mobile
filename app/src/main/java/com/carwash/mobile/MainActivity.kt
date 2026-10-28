package com.carwash.mobile

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.carwash.mobile.ingreso.AndroidServerSettings
import com.carwash.mobile.ingreso.IngresoScreen
import com.carwash.mobile.ingreso.IngresoViewModel
import com.carwash.mobile.ui.theme.CarWashTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkTheme = isSystemInDarkTheme()
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
            val model: IngresoViewModel = viewModel(factory = viewModelFactory {
                initializer {
                    val emulator = Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish")
                    val defaultUrl = if (emulator) "http://10.0.2.2:5197/" else ""
                    IngresoViewModel(AndroidServerSettings(applicationContext, defaultUrl))
                }
            })
            CarWashTheme {
                IngresoScreen(model)
            }
        }
    }
}


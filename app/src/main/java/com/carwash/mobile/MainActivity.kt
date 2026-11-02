package com.carwash.mobile

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.carwash.mobile.asignacion.ApiAsignacionRepository
import com.carwash.mobile.asignacion.AsignacionScreen
import com.carwash.mobile.asignacion.AsignacionViewModel
import com.carwash.mobile.auth.AndroidSessionStore
import com.carwash.mobile.auth.ApiAuthRepository
import com.carwash.mobile.auth.LoginScreen
import com.carwash.mobile.auth.LoginViewModel
import com.carwash.mobile.ingreso.AndroidServerSettings
import com.carwash.mobile.ingreso.IngresoScreen
import com.carwash.mobile.ingreso.IngresoViewModel
import com.carwash.mobile.ingreso.ServerSettings
import com.carwash.mobile.ui.theme.CarWashTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings: ServerSettings = AndroidServerSettings(applicationContext, defaultUrl())
        setContent {
            val darkTheme = isSystemInDarkTheme()
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
            val login: LoginViewModel = viewModel(factory = viewModelFactory {
                initializer { LoginViewModel(ApiAuthRepository({ listOf(settings.serverUrl, settings.backupUrl) }), AndroidSessionStore(applicationContext)) }
            })
            val session = login.state.session
            CarWashTheme {
                if (session == null) {
                    LoginScreen(login, settings.serverUrl) { settings.serverUrl = it }
                } else {
                    val model: IngresoViewModel = viewModel(factory = viewModelFactory {
                        initializer { IngresoViewModel(settings) }
                    })
                    var showVehicles by rememberSaveable { mutableStateOf(false) }
                    if (showVehicles) {
                        val vehicles: AsignacionViewModel = viewModel(factory = viewModelFactory {
                            initializer { AsignacionViewModel(ApiAsignacionRepository.create(session.baseUrl), session) }
                        })
                        AsignacionScreen(vehicles, onBack = { showVehicles = false }, onLogout = {
                            showVehicles = false
                            login.logout()
                        })
                    } else {
                        IngresoScreen(model, onOpenVehicles = { showVehicles = true })
                    }
                }
            }
        }
    }

    private fun defaultUrl(): String {
        val emulator = Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish")
        return if (emulator) "http://10.0.2.2:5197/" else ""
    }
}
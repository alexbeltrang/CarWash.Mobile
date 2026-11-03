package com.carwash.mobile

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import com.carwash.mobile.reportes.ApiReporteVentasRepository
import com.carwash.mobile.reportes.ReporteVentasScreen
import com.carwash.mobile.reportes.ReporteVentasViewModel
import com.carwash.mobile.reportes.ReportStatus
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
            DisposableEffect(login) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) login.checkSessionExpiry()
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }
            var destination by rememberSaveable { mutableStateOf(AppDestination.INGRESO) }
            CarWashTheme {
                if (session == null || login.state.requiresReentry) {
                    LoginScreen(login, settings.serverUrl, onServerUrl = { settings.serverUrl = it }, onCancelReentry = {
                        destination = AppDestination.INGRESO
                        login.cancelReentry()
                    })
                } else {
                    val model: IngresoViewModel = viewModel(factory = viewModelFactory {
                        initializer { IngresoViewModel(settings) }
                    })
                    val report: ReporteVentasViewModel = viewModel(factory = viewModelFactory {
                        initializer { ReporteVentasViewModel(ApiReporteVentasRepository(session), session) }
                    })
                    LaunchedEffect(session) { report.bind(session, ApiReporteVentasRepository(session)) }
                    DisposableEffect(report, login) {
                        onDispose { if (login.state.session == null) report.clear() }
                    }
                    LaunchedEffect(report.state.status, destination) {
                        if (destination == AppDestination.REPORTE && (session.accessToken == null || report.state.status == ReportStatus.SESSION_REQUIRED)) {
                            report.clear()
                            destination = AppDestination.INGRESO
                            login.logout()
                        }
                        if (session.isAdmin && report.state.status == ReportStatus.FORBIDDEN) {
                            destination = AppDestination.INGRESO
                            login.reportForbidden()
                        }
                    }
                    BackHandler(enabled = destination != AppDestination.INGRESO) { destination = AppDestination.INGRESO }
                    val logout = {
                        report.clear()
                        destination = AppDestination.INGRESO
                        login.logout()
                    }
                    AppNavigation(session.isAdmin, destination, onDestination = { target ->
                        login.checkSessionExpiry()
                        if (login.state.session != null) destination = target
                    }) {
                        when (if (!session.isAdmin && destination == AppDestination.REPORTE) AppDestination.INGRESO else destination) {
                            AppDestination.INGRESO -> IngresoScreen(model, onOpenVehicles = { destination = AppDestination.VEHICULOS }, onLogout = logout)
                            AppDestination.VEHICULOS -> {
                                val vehicles: AsignacionViewModel = viewModel(key = "vehicles:${session.userId}:${session.baseUrl}:${session.isAdmin}", factory = viewModelFactory {
                                    initializer { AsignacionViewModel(ApiAsignacionRepository.create(session.baseUrl), session.metadata()) }
                                })
                                AsignacionScreen(vehicles, onBack = { destination = AppDestination.INGRESO }, onLogout = logout)
                            }
                            AppDestination.REPORTE -> if (report.ownsSession(session) && session.accessToken != null) {
                                ReporteVentasScreen(report, onBack = { destination = AppDestination.INGRESO }, onLogout = logout)
                            }
                        }
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

enum class AppDestination { INGRESO, VEHICULOS, REPORTE }

@Composable
fun AppNavigation(isAdmin: Boolean, destination: AppDestination, onDestination: (AppDestination) -> Unit, content: @Composable () -> Unit) {
    val destinations = if (isAdmin) AppDestination.entries else listOf(AppDestination.INGRESO, AppDestination.VEHICULOS)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= 600.dp) {
            Row(Modifier.fillMaxSize()) {
                NavigationRail(Modifier.fillMaxHeight()) {
                    destinations.forEach { target ->
                        NavigationRailItem(selected = target == destination, onClick = { onDestination(target) },
                            icon = { Icon(destinationIcon(target), contentDescription = null) },
                            label = { Text(stringResource(destinationLabel(target))) })
                    }
                }
                Box(Modifier.weight(1f).fillMaxHeight()) { content() }
            }
        } else {
            Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), bottomBar = {
                NavigationBar {
                    destinations.forEach { target ->
                        NavigationBarItem(selected = target == destination, onClick = { onDestination(target) },
                            icon = { Icon(destinationIcon(target), contentDescription = null) },
                            label = { Text(stringResource(destinationLabel(target))) })
                    }
                }
            }) { padding -> Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) { content() } }
        }
    }
}

private fun destinationIcon(destination: AppDestination) = when (destination) {
    AppDestination.INGRESO -> Icons.Default.Home
    AppDestination.VEHICULOS -> Icons.Default.DirectionsCar
    AppDestination.REPORTE -> Icons.Default.Assessment
}

private fun destinationLabel(destination: AppDestination) = when (destination) {
    AppDestination.INGRESO -> R.string.nav_ingreso
    AppDestination.VEHICULOS -> R.string.nav_vehicles
    AppDestination.REPORTE -> R.string.nav_sales
}
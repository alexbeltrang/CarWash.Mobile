package com.carwash.mobile.asignacion

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.carwash.mobile.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsignacionScreen(model: AsignacionViewModel, onBack: () -> Unit, onLogout: () -> Unit) {
    val state = model.state
    LaunchedEffect(model) { model.refresh() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vehicles_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.vehicles_back))
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, stringResource(R.string.logout))
                    }
                    IconButton(onClick = model::refresh, enabled = !state.loading) {
                        Icon(Icons.Default.Refresh, stringResource(R.string.refresh))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            state.message?.let { Text(stringResource(messageRes(it)), Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary) }
            if (!model.canAssign) {
                Text(stringResource(R.string.assign_admin_only_hint), Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall)
            }
            when {
                state.loading -> CircularProgressIndicator(Modifier.padding(16.dp))
                state.loadFailed -> Text(stringResource(R.string.vehicles_load_failed), Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                state.vehicles.isEmpty() -> Text(stringResource(R.string.vehicles_empty), Modifier.padding(16.dp))
                else -> LazyColumn(Modifier.fillMaxWidth()) {
                    items(state.vehicles, key = { it.idTurno }) { vehicle ->
                        ListItem(
                            modifier = Modifier.clickable(enabled = model.canAssign) { model.select(vehicle) },
                            headlineContent = { Text(vehicle.placa.orEmpty()) },
                            supportingContent = {
                                Text(
                                    vehicle.operatorName?.let { stringResource(R.string.vehicle_operator, it) }
                                        ?: stringResource(R.string.vehicle_unassigned),
                                )
                            },
                            overlineContent = { Text(listOfNotNull(vehicle.tipoVehiculo, vehicle.marca).joinToString(" · ")) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
    state.selected?.let { vehicle ->
        AlertDialog(
            onDismissRequest = model::dismiss,
            title = {
                Text(stringResource(if (vehicle.operatorName == null) R.string.assign_title else R.string.assign_reassign_title) + " · " + vehicle.placa.orEmpty())
            },
            text = {
                when {
                    state.operatorsLoading -> CircularProgressIndicator()
                    state.operators.isEmpty() -> Text(stringResource(R.string.assign_no_operators))
                    else -> LazyColumn {
                        items(state.operators, key = { it.idOperario }) { operator ->
                            ListItem(
                                modifier = Modifier.clickable(enabled = !state.saving) { model.assign(operator) },
                                headlineContent = { Text(operator.fullName) },
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = model::dismiss, enabled = !state.saving) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

private fun messageRes(message: AsignacionMessage) = when (message) {
    AsignacionMessage.ASSIGNED -> R.string.assign_assigned
    AsignacionMessage.FORBIDDEN -> R.string.assign_forbidden
    AsignacionMessage.NOT_FOUND -> R.string.assign_not_found
    AsignacionMessage.CONFLICT -> R.string.assign_conflict
    AsignacionMessage.ERROR -> R.string.assign_error
}

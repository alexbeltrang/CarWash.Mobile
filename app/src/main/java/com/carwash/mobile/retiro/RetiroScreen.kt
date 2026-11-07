package com.carwash.mobile.retiro

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.carwash.mobile.R
import com.carwash.mobile.ingreso.money
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetiroScreen(model: RetiroViewModel, onBack: () -> Unit) {
    val state = model.state
    val focus = LocalFocusManager.current
    var reprintDialog by remember(state.receipt?.idTurno) { mutableStateOf(false) }
    if (reprintDialog) AlertDialog(onDismissRequest = { reprintDialog = false },
        title = { Text(stringResource(R.string.pickup_print_again)) }, text = { Text(stringResource(R.string.pickup_print_review)) },
        confirmButton = { TextButton(onClick = { reprintDialog = false; model.print(newCopy = true) }, modifier = Modifier.testTag("pickupPrintConfirm")) { Text(stringResource(R.string.pickup_print_again)) } },
        dismissButton = { TextButton(onClick = { reprintDialog = false }, modifier = Modifier.testTag("pickupPrintCancel")) { Text(stringResource(R.string.pickup_print_cancel)) } })
    Scaffold(topBar = { TopAppBar(title = { com.carwash.mobile.empresa.EmpresaHeader(stringResource(R.string.pickup_title)) }, navigationIcon = {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.pickup_back)) }
    }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(state.plate, model::editPlate, label = { Text(stringResource(R.string.pickup_plate)) },
                singleLine = true, enabled = !state.busy, modifier = Modifier.fillMaxWidth().testTag("pickupPlate"))
            FilledTonalButton(onClick = { focus.clearFocus(); model.search() }, enabled = !state.busy && state.plate.isNotBlank(), modifier = Modifier.fillMaxWidth().testTag("pickupSearch")) {
                Icon(Icons.Default.Search, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.pickup_search))
            }
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let { error ->
                Text(stringResource(when (error) {
                    RetiroError.SESSION_REQUIRED -> R.string.pickup_session_required
                    RetiroError.NOT_FOUND -> R.string.pickup_not_found
                    RetiroError.INVALID_CODE -> R.string.pickup_invalid_code
                    RetiroError.ALREADY_DELIVERED -> R.string.pickup_unavailable
                    RetiroError.INVALID_INPUT -> R.string.pickup_invalid_input
                    RetiroError.UNAVAILABLE -> R.string.pickup_unavailable
                }), color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("pickupError"))
            }
            state.receipt?.let { receipt ->
                HorizontalDivider()
                Text(receipt.placa, style = MaterialTheme.typography.headlineSmall)
                Text(listOfNotNull(receipt.cliente, receipt.marca).joinToString(" · "))
                Text(stringResource(R.string.pickup_date, localDate(receipt.fechaIngreso)))
                receipt.servicios.forEach { service ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(service.nombre, Modifier.weight(1f)); Text(money(service.precio))
                    }
                }
                Text(stringResource(R.string.pickup_total, money(receipt.total)), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.pickup_print_width, 80), modifier = Modifier.testTag("pickupPrintWidth80"))
                Button(onClick = { focus.clearFocus(); if (state.printJob != null) reprintDialog = true else model.print() },
                    enabled = !state.busy && state.printHistoryLoaded && !state.printCheckRequired && state.printJob?.estado !in listOf("PENDING", "SUBMITTING"),
                    modifier = Modifier.fillMaxWidth().testTag("pickupPrint")) {
                    Icon(Icons.Default.Print, null); Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (state.printJob != null) R.string.pickup_print_again else if (state.printRequestId != null) R.string.pickup_print_retry else R.string.pickup_print))
                }
                state.printJob?.let { job -> Text(stringResource(when (job.estado) {
                    "PENDING" -> R.string.pickup_print_pending
                    "SUBMITTING" -> R.string.pickup_print_submitting
                    "SUBMITTED" -> R.string.pickup_print_submitted
                    "FAILED" -> R.string.pickup_print_failed
                    else -> R.string.pickup_print_unknown
                }, job.impresora), modifier = Modifier.testTag("pickupPrintStatus")) }
                if (state.printError) Text(stringResource(R.string.pickup_print_error), color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("pickupPrintError"))
                if (state.printCheckRequired) Text(stringResource(R.string.pickup_print_check_required), color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = model::checkPrint, enabled = !state.busy, modifier = Modifier.fillMaxWidth().testTag("pickupPrintCheck")) {
                    Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.pickup_print_check))
                }
                OutlinedButton(onClick = model::refresh, enabled = !state.busy, modifier = Modifier.fillMaxWidth().testTag("pickupRefresh")) {
                    Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.pickup_refresh))
                }
            }
        }
    }
}

private fun localDate(value: String): String = runCatching {
    OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
}.getOrDefault(value)
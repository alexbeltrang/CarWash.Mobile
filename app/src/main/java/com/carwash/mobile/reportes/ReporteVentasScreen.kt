package com.carwash.mobile.reportes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.carwash.mobile.R
import com.carwash.mobile.ingreso.money
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReporteVentasScreen(
    model: ReporteVentasViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onReentry: () -> Unit = {},
    onForbidden: () -> Unit = {},
) {
    val state = model.state
    var editingInitial by remember { mutableStateOf<Boolean?>(null) }
    var showOptions by remember { mutableStateOf(false) }
    LaunchedEffect(model, state.edited.initialDate, state.edited.finalDate) { model.loadOptions() }
    LaunchedEffect(state.status) {
        if (state.status == ReportStatus.SESSION_REQUIRED) onReentry()
        if (state.status == ReportStatus.FORBIDDEN) onForbidden()
    }
    Scaffold(topBar = {
        TopAppBar(expandedHeight = maxOf(64.dp, (64 * LocalDensity.current.fontScale).dp),
            title = { Text(stringResource(R.string.report_title)) }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.report_back)) }
        }, actions = {
            IconButton(onClick = { model.clear(); onLogout() }) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, stringResource(R.string.logout))
            }
        })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).imePadding().testTag("report_list"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!model.canQuery) {
                item { Text(stringResource(R.string.report_admin_only), Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error) }
            } else {
                item {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ReportDateField(state.edited.initialDate, R.string.report_initial_date, R.string.report_select_initial,
                            if ("fechaInicial" in state.fieldErrors) R.string.report_initial_required else null) { editingInitial = true }
                        ReportDateField(state.edited.finalDate, R.string.report_final_date, R.string.report_select_final,
                            if ("fechaFinal" in state.fieldErrors) {
                                if (state.edited.finalDate == null) R.string.report_final_required else R.string.report_invalid_range
                            } else null) { editingInitial = false }
                        TextButton(onClick = { showOptions = !showOptions }) {
                            Icon(if (showOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                            Text(stringResource(R.string.report_optional_filters))
                        }
                        if (showOptions) {
                            if (state.optionsLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                            if (state.optionsError != null) {
                                Text(stringResource(R.string.report_options_unavailable), color = MaterialTheme.colorScheme.error)
                                TextButton(onClick = model::loadOptions) { Text(stringResource(R.string.report_options_retry)) }
                            }
                            ReportFilterMenu(R.string.report_payment, R.string.report_all_payments, state.edited.paymentMethodId,
                                state.options?.paymentMethods.orEmpty(), !state.optionsLoading) { model.edit(state.edited.copy(paymentMethodId = it)) }
                            ReportFilterMenu(R.string.report_operator, R.string.report_all_operators, state.edited.operatorId,
                                state.options?.operators.orEmpty(), !state.optionsLoading) { model.edit(state.edited.copy(operatorId = it)) }
                            ReportFilterMenu(R.string.report_vehicle_type, R.string.report_all_types, state.edited.vehicleTypeId,
                                state.options?.vehicleTypes.orEmpty(), !state.optionsLoading) { model.edit(state.edited.copy(vehicleTypeId = it)) }
                            OutlinedTextField(value = state.edited.plate.orEmpty(), onValueChange = { model.edit(state.edited.copy(plate = it)) },
                                label = { Text(stringResource(R.string.report_plate)) }, singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("report_plate"))
                            TextButton(onClick = model::clearFilters) {
                                Icon(Icons.Default.ClearAll, contentDescription = null)
                                Text(stringResource(R.string.report_clear_filters))
                            }
                        }
                        Button(onClick = model::consult, enabled = state.status != ReportStatus.LOADING) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Text(stringResource(R.string.report_consult), Modifier.padding(start = 8.dp))
                        }
                    }
                }
                if (state.status == ReportStatus.LOADING) item { CircularProgressIndicator(Modifier.padding(16.dp)) }
                state.page?.let { page ->
                    item {
                        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.report_period, page.criteria.initialDate.toString(), page.criteria.finalDate.toString()), style = MaterialTheme.typography.titleSmall)
                            page.criteria.paymentMethodId?.let { Text(stringResource(R.string.report_applied_payment, it)) }
                            page.criteria.operatorId?.let { Text(stringResource(R.string.report_applied_operator, it)) }
                            page.criteria.vehicleTypeId?.let { Text(stringResource(R.string.report_applied_type, it)) }
                            page.criteria.plate?.let { Text(stringResource(R.string.report_applied_plate, it)) }
                            if (state.previousResults) Text(stringResource(R.string.report_previous), color = MaterialTheme.colorScheme.tertiary)
                            Text(stringResource(R.string.report_count, page.count), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.report_total, money(page.total)), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.report_consulted, page.consultedAt.toString()), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (state.status == ReportStatus.EMPTY) item { Text(stringResource(R.string.report_empty), Modifier.padding(16.dp)) }
                    items(page.items, key = { it.id }) { movement ->
                        val missing = stringResource(R.string.report_missing)
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(movement.plate ?: missing, style = MaterialTheme.typography.titleMedium)
                            Text(movement.collectedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss XXX")), style = MaterialTheme.typography.bodySmall)
                            Text(movement.vehicleType ?: missing)
                            Text(movement.operator ?: missing)
                            Text(movement.paymentMethod ?: missing)
                            Text(money(movement.amount), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.report_reference, movement.id, movement.turnId), style = MaterialTheme.typography.bodySmall)
                            HorizontalDivider(Modifier.padding(top = 8.dp))
                        }
                    }
                    if (page.nextCursor != null && state.status == ReportStatus.RESULTS) item {
                        TextButton(onClick = model::loadMore, enabled = !state.loadingMore, modifier = Modifier.padding(horizontal = 8.dp)) {
                            if (state.loadingMore) CircularProgressIndicator() else Text(stringResource(R.string.report_load_more))
                        }
                    }
                }
                if (state.status == ReportStatus.ERROR) item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        if (state.partial) Text(stringResource(R.string.report_partial), color = MaterialTheme.colorScheme.tertiary)
                        Text(stringResource(reportErrorText(state.error)), color = MaterialTheme.colorScheme.error)
                        if (state.error == ReportErrorKind.INVALID_CURSOR) {
                            TextButton(onClick = model::refresh) { Text(stringResource(R.string.report_update)) }
                        } else {
                            TextButton(onClick = model::retry) { Text(stringResource(R.string.report_retry)) }
                        }
                    }
                }
                if (state.status == ReportStatus.DATA_CHANGED) item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text(stringResource(R.string.report_changed), color = MaterialTheme.colorScheme.tertiary)
                        TextButton(onClick = model::refresh) { Text(stringResource(R.string.report_update)) }
                    }
                }
                if (state.status == ReportStatus.SESSION_REQUIRED) item {
                    TextButton(onClick = onReentry) { Text(stringResource(R.string.report_reentry)) }
                }
            }
            item { HorizontalDivider(Modifier.padding(top = 8.dp)) }
        }
    }
    editingInitial?.let { initial ->
        val selected = if (initial) state.edited.initialDate else state.edited.finalDate
        val picker = rememberDatePickerState(initialSelectedDateMillis = selected?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(), yearRange = 1..9999)
        DatePickerDialog(onDismissRequest = { editingInitial = null }, confirmButton = {
            TextButton(onClick = {
                picker.selectedDateMillis?.let { milliseconds ->
                    val date = Instant.ofEpochMilli(milliseconds).atZone(ZoneOffset.UTC).toLocalDate()
                    model.edit(if (initial) state.edited.copy(initialDate = date) else state.edited.copy(finalDate = date))
                }
                editingInitial = null
            }, enabled = picker.selectedDateMillis != null) { Text(stringResource(R.string.report_accept)) }
        }, dismissButton = {
            TextButton(onClick = { editingInitial = null }) { Text(stringResource(R.string.cancel)) }
        }) { DatePicker(picker) }
    }
}

@Composable
private fun ReportDateField(date: LocalDate?, label: Int, description: Int, error: Int?, onSelect: () -> Unit) {
    OutlinedTextField(value = date?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")).orEmpty(), onValueChange = {}, readOnly = true,
        modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(label)) }, isError = error != null,
        supportingText = error?.let { { Text(stringResource(it)) } }, trailingIcon = {
            IconButton(onClick = onSelect) { Icon(Icons.Default.DateRange, stringResource(description)) }
        })
}

private fun reportErrorText(error: ReportErrorKind?) = when (error) {
    ReportErrorKind.INVALID_FILTERS -> R.string.report_invalid_filters
    ReportErrorKind.INVALID_CURSOR -> R.string.report_invalid_cursor
    ReportErrorKind.NETWORK -> R.string.report_network_error
    ReportErrorKind.SESSION_REQUIRED -> R.string.report_reentry
    ReportErrorKind.ADMIN_REQUIRED -> R.string.report_admin_only
    else -> R.string.report_unavailable
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportFilterMenu(label: Int, all: Int, selectedId: Int?, options: List<FilterOption>, enabled: Boolean, onSelect: (Int?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = options.firstOrNull { it.id == selectedId }?.name ?: stringResource(R.string.report_missing)
    val value = if (selectedId == null) stringResource(all) else stringResource(R.string.report_option_value, selectedName, selectedId)
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(value = value, onValueChange = {}, readOnly = true, enabled = enabled,
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable), label = { Text(stringResource(label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(stringResource(all)) }, onClick = { onSelect(null); expanded = false })
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option.name ?: stringResource(R.string.report_missing)) },
                    trailingIcon = { Text(stringResource(R.string.report_option_id, option.id)) },
                    onClick = { onSelect(option.id); expanded = false })
            }
        }
    }
}
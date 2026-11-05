package com.carwash.mobile.ingreso

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.carwash.mobile.R
import java.math.BigDecimal
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngresoScreen(model: IngresoViewModel, onOpenVehicles: (() -> Unit)? = null, onLogout: (() -> Unit)? = null) {
    val state = model.state
    val editable = !state.loading && !state.saving && !state.reviewRequired
    // Los datos del vehículo se muestran solo después de buscar la placa.
    val showDetails = state.searched || state.reviewRequired
    var showConnection by rememberSaveable { mutableStateOf(state.serverUrl.isBlank()) }
    var showClear by rememberSaveable { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    var showCapture by rememberSaveable { mutableStateOf(false) }
    var focusManual by remember { mutableStateOf(false) }
    val plateFocus = remember { FocusRequester() }

    LaunchedEffect(showCapture, focusManual) {
        if (!showCapture && focusManual) {
            plateFocus.requestFocus()
            keyboard?.show()
            focusManual = false
        }
    }
    if (showCapture) {
        PlateCaptureScreen(
            onConfirm = { plate -> keyboard?.hide(); model.acceptScannedPlate(plate); showCapture = false },
            onManual = { showCapture = false; focusManual = true },
            onCancel = { showCapture = false },
        )
        return
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.ingreso_title), style = MaterialTheme.typography.bodyMedium)
                    }
                },
                actions = {
                    if (onOpenVehicles != null) {
                        ToolButton(Icons.AutoMirrored.Filled.List, R.string.vehicles_open, editable) { onOpenVehicles() }
                    }
                    ToolButton(Icons.Default.Refresh, R.string.refresh, !state.loading && !state.saving) {
                        model.connect(state.serverUrl)
                    }
                    ToolButton(Icons.Default.Settings, R.string.connection, editable) { showConnection = true }
                    if (onLogout != null) {
                        ToolButton(Icons.AutoMirrored.Filled.ExitToApp, R.string.logout, editable) { onLogout() }
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.total_label), style = MaterialTheme.typography.labelMedium)
                        Text(money(parseAmount(state.form.value) ?: BigDecimal.ZERO), style = MaterialTheme.typography.titleMedium)
                    }
                    ToolButton(Icons.Default.Close, R.string.cancel, !state.saving && !state.loading) {
                        if (state.form != IngresoForm() || state.reviewRequired) showClear = true else model.clear()
                    }
                    Button(
                        onClick = { keyboard?.hide(); model.save() },
                        enabled = editable && state.connected && !state.searching && showDetails,
                        modifier = Modifier.testTag("save"),
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(when {
                            state.saving -> R.string.saving
                            state.editingTurnId != null -> R.string.update
                            else -> R.string.save
                        }))
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ConnectionStatus(state)
            if (state.loading || state.searching || state.saving) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.notice?.let { NoticeBanner(it) }
            state.notificationStatus?.let { status ->
                LaunchedEffect(status) {
                    delay(10_000)
                    model.clearEmailStatus()
                }
                EmailStatus(
                    status = status,
                    canRetry = status == "FAILED" || status == "RETRYING",
                    onRetry = model::retryEmail,
                )
            }
            state.whatsAppStatus?.let { status ->
                WhatsAppStatus(status, state.whatsAppCanRetry, state.whatsAppSending,
                    model::retryWhatsApp, model::clearWhatsAppStatus)
            }
            SectionTitle(R.string.vehicle_section)
            FilledTonalButton(
                onClick = { keyboard?.hide(); showCapture = true },
                enabled = editable && !state.searching,
                modifier = Modifier.fillMaxWidth().testTag("openCamera"),
            ) {
                Icon(Icons.Default.PhotoCamera, null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.capture_plate))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                IngresoField(
                    value = state.form.plate,
                    onChange = { value -> model.edit { it.copy(plate = value.uppercase(Locale.ROOT)) } },
                    label = R.string.plate_label, tag = "plate", enabled = editable,
                    error = FormError.PLATE in state.errors, errorText = R.string.required_plate,
                    modifier = Modifier.weight(1f).focusRequester(plateFocus), capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Search,
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); model.lookup() }),
                )
                FilledTonalIconButton(
                    onClick = { keyboard?.hide(); model.lookup() },
                    enabled = editable && state.connected,
                    modifier = Modifier.padding(top = 8.dp).size(52.dp).testTag("lookup"),
                ) { Icon(Icons.Default.Search, stringResource(R.string.search_plate)) }
            }
            if (showDetails) {
                VehicleTypeMenu(state, editable && state.connected, model::selectType)
                IngresoField(
                    state.form.customer, { value -> model.edit { it.copy(customer = value.uppercase(Locale.ROOT)) } },
                    R.string.customer_label, "customer", editable,
                    capitalization = KeyboardCapitalization.Characters,
                )
                IngresoField(
                    state.form.phone, { value -> model.edit { it.copy(phone = value) } },
                    R.string.phone_label, "phone", editable, keyboardType = KeyboardType.Phone,
                )
                IngresoField(
                    state.form.email, { value -> model.edit { it.copy(email = value) } },
                    R.string.email_label, "email", editable, keyboardType = KeyboardType.Email,
                    error = FormError.EMAIL in state.errors, errorText = R.string.invalid_email,
                )
                IngresoField(
                    state.form.brand, { value -> model.edit { it.copy(brand = value.uppercase(Locale.ROOT)) } },
                    R.string.brand_label, "brand", editable, capitalization = KeyboardCapitalization.Characters,
                )

                SectionTitle(R.string.services_section)
                val services = state.catalog.services.filter { it.idTipoVehiculo == state.form.typeId }
                if (services.isEmpty()) {
                    Text(stringResource(if (state.form.typeId == null) R.string.no_type else R.string.no_services),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column {
                    services.forEach { service ->
                        Row(
                            Modifier.fillMaxWidth().testTag("service-${service.idServicio}")
                                .toggleable(
                                    value = service.idServicio in state.form.serviceIds,
                                    enabled = editable, role = Role.Checkbox,
                                    onValueChange = { model.toggleService(service.idServicio) },
                                ).padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Checkbox(checked = service.idServicio in state.form.serviceIds, onCheckedChange = null, enabled = editable)
                            Column(Modifier.weight(1f)) {
                                Text(service.nombre, style = MaterialTheme.typography.bodyLarge)
                                Text(money(service.precio ?: BigDecimal.ZERO), style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider()
                    }
                }
                if (FormError.SERVICES in state.errors) Text(stringResource(R.string.required_services), color = MaterialTheme.colorScheme.error)

                SectionTitle(R.string.amounts_section)
                IngresoField(
                    state.form.value, { value -> model.edit { it.copy(value = value) } },
                    R.string.value_label, "value", editable, keyboardType = KeyboardType.Decimal,
                    error = FormError.VALUE in state.errors, errorText = R.string.invalid_amount,
                )
                IngresoField(
                    state.form.baseValue, { value -> model.edit { it.copy(baseValue = value) } },
                    R.string.base_label, "baseValue", editable, keyboardType = KeyboardType.Decimal,
                    error = FormError.BASE_VALUE in state.errors, errorText = R.string.invalid_amount,
                )
                OutlinedTextField(
                    value = state.form.observations,
                    onValueChange = { value -> model.edit { it.copy(observations = value.uppercase(Locale.ROOT)) } },
                    modifier = Modifier.fillMaxWidth().testTag("observations"), enabled = editable,
                    label = { Text(stringResource(R.string.observations_label)) }, minLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                )
                if (state.searched) HistorySection(state.history)
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showConnection) ConnectionDialog(
        state,
        onDismiss = { showConnection = false },
        onTest = { url, backup -> keyboard?.hide(); model.testConnection(url, backup) },
    ) { url, backup ->
        keyboard?.hide()
        model.connect(url, backup)
        showConnection = false
    }
    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            title = { Text(stringResource(R.string.clear_title)) },
            text = { Text(if (state.reviewRequired) stringResource(R.string.clear_review_body, state.pendingNumber)
                else stringResource(R.string.clear_body)) },
            confirmButton = { TextButton(onClick = { model.clear(); showClear = false }) { Text(stringResource(R.string.clear_confirm)) } },
            dismissButton = { TextButton(onClick = { showClear = false }) { Text(stringResource(R.string.keep_editing)) } },
        )
    }
}

@Composable
private fun ConnectionStatus(state: IngresoState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(if (state.connected) Icons.Default.Wifi else Icons.Default.WifiOff, null,
                tint = if (state.connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp))
            Text(stringResource(when {
                state.loading -> R.string.connecting
                state.connected && state.usingBackup -> R.string.connected_backup
                state.connected -> R.string.connected
                else -> R.string.disconnected
            }), style = MaterialTheme.typography.labelLarge)
        }
        if (state.connected) {
            Text(stringResource(if (state.catalog.cash != null) R.string.cash_open else R.string.cash_closed),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(Modifier.padding(top = 8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehicleTypeMenu(state: IngresoState, enabled: Boolean, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded && enabled, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = state.catalog.types.firstOrNull { it.idTipoVehiculo == state.form.typeId }?.nombre.orEmpty(),
            onValueChange = {}, readOnly = true, enabled = enabled,
            modifier = Modifier.fillMaxWidth().menuAnchor().testTag("vehicleType"),
            label = { Text(stringResource(R.string.type_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            isError = FormError.TYPE in state.errors,
            supportingText = if (FormError.TYPE in state.errors) ({ Text(stringResource(R.string.required_type)) }) else null,
        )
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            state.catalog.types.forEach { type ->
                DropdownMenuItem(text = { Text(type.nombre) }, onClick = { onSelect(type.idTipoVehiculo); expanded = false })
            }
        }
    }
}

@Composable
private fun IngresoField(
    value: String,
    onChange: (String) -> Unit,
    @StringRes label: Int,
    tag: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    error: Boolean = false,
    @StringRes errorText: Int = R.string.invalid_form,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        modifier = modifier.fillMaxWidth().testTag(tag), label = { Text(stringResource(label)) },
        enabled = enabled, singleLine = true, isError = error,
        supportingText = if (error) ({ Text(stringResource(errorText)) }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = capitalization, imeAction = imeAction),
        keyboardActions = keyboardActions,
    )
}

@Composable
private fun SectionTitle(@StringRes title: Int) {
    Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun NoticeBanner(notice: Notice) {
    val message = when (notice.kind) {
        NoticeKind.CONNECTION_FAILED -> R.string.connection_failed
        NoticeKind.INVALID_SERVER -> R.string.invalid_server
        NoticeKind.LOOKUP_FAILED -> R.string.lookup_failed
        NoticeKind.NOT_FOUND -> R.string.plate_not_found
        NoticeKind.CLOSED_CASH -> R.string.closed_cash_error
        NoticeKind.INVALID_FORM -> R.string.invalid_form
        NoticeKind.SAVED -> R.string.saved_ingreso
        NoticeKind.SAVE_FAILED -> R.string.save_failed
        NoticeKind.REVIEW_REQUIRED -> R.string.review_required
        NoticeKind.IN_PROCESS -> R.string.in_process_loaded
        NoticeKind.UPDATED -> R.string.updated_ingreso
    }

    val positive = notice.kind in setOf(NoticeKind.SAVED, NoticeKind.NOT_FOUND, NoticeKind.IN_PROCESS, NoticeKind.UPDATED)
    val background = if (positive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer
    val foreground = if (positive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer
    Row(Modifier.fillMaxWidth().background(background).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (notice.kind == NoticeKind.SAVED) Icon(Icons.Default.CheckCircle, null, tint = foreground)
        Text(stringResource(message, notice.detail), color = foreground, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmailStatus(status: String, canRetry: Boolean, onRetry: () -> Unit) {
    val label = when (status) {
        "NOT_APPLICABLE" -> R.string.email_not_applicable
        "PENDING" -> R.string.email_pending
        "RETRYING" -> R.string.email_retrying
        "ACCEPTED" -> R.string.email_accepted
        "DELIVERED" -> R.string.email_delivered
        "FAILED" -> R.string.email_failed
        else -> R.string.email_unknown
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.email_status_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(label), style = MaterialTheme.typography.bodyMedium)
            if (canRetry) {
                TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.email_retry))
                }
            }
        }
    }
}

@Composable
internal fun WhatsAppStatus(status: String, canRetry: Boolean, sending: Boolean, onRetry: () -> Unit, onDismiss: () -> Unit) {
    val label = when (status) {
        "NOT_CONFIGURED" -> R.string.whatsapp_not_configured
        "NOT_APPLICABLE" -> R.string.whatsapp_not_applicable
        "PENDING", "SENDING" -> R.string.whatsapp_pending
        "RETRYING" -> R.string.whatsapp_retrying
        "ACCEPTED" -> R.string.whatsapp_accepted
        "DELIVERED" -> R.string.whatsapp_delivered
        "FAILED" -> R.string.whatsapp_failed
        "UNAVAILABLE" -> R.string.whatsapp_unavailable
        else -> R.string.whatsapp_unknown
    }
    Row(Modifier.fillMaxWidth().testTag("whatsAppStatus"), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.whatsapp_status_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(label), style = MaterialTheme.typography.bodyMedium)
        }
        if (canRetry) ToolButton(Icons.Default.Refresh, R.string.whatsapp_retry, !sending, onRetry)
        ToolButton(Icons.Default.Close, R.string.whatsapp_dismiss, true, onDismiss)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolButton(icon: ImageVector, @StringRes label: Int, enabled: Boolean, action: () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(stringResource(label)) } },
        state = androidx.compose.material3.rememberTooltipState(),
    ) {
        IconButton(onClick = action, enabled = enabled) { Icon(icon, stringResource(label)) }
    }
}

@Composable
private fun ConnectionDialog(
    state: IngresoState,
    onDismiss: () -> Unit,
    onTest: (String, String) -> Unit,
    onConnect: (String, String) -> Unit,
) {
    var url by rememberSaveable { mutableStateOf(state.serverUrl) }
    var backup by rememberSaveable { mutableStateOf(state.backupUrl) }
    val valid = normalizeServer(url) != null
    val backupValid = backup.isBlank() || normalizeServer(backup) != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.connection)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = url, onValueChange = { url = it }, modifier = Modifier.fillMaxWidth().testTag("serverUrl"),
                    label = { Text(stringResource(R.string.server_label)) },
                    placeholder = { Text(stringResource(R.string.server_hint)) },
                    singleLine = true, isError = url.isNotBlank() && !valid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    supportingText = if (url.isNotBlank() && !valid) ({ Text(stringResource(R.string.invalid_server)) }) else null,
                )
                ProbeLine(normalizeServer(url), state)
                OutlinedTextField(
                    value = backup, onValueChange = { backup = it }, modifier = Modifier.fillMaxWidth().testTag("backupUrl"),
                    label = { Text(stringResource(R.string.backup_label)) },
                    placeholder = { Text(stringResource(R.string.backup_hint)) },
                    singleLine = true, isError = !backupValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    supportingText = if (!backupValid) ({ Text(stringResource(R.string.invalid_server)) }) else null,
                )
                if (backup.isNotBlank()) ProbeLine(normalizeServer(backup), state)
                TextButton(
                    onClick = { onTest(url, backup) },
                    enabled = valid && backupValid && !state.probing,
                    modifier = Modifier.testTag("testConnection"),
                ) {
                    Text(stringResource(if (state.probing) R.string.testing_connection else R.string.test_connection))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConnect(url, backup) }, enabled = valid && backupValid && !state.loading) {
                Text(stringResource(R.string.connect))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

@Composable
private fun ProbeLine(url: String?, state: IngresoState) {
    val result = url?.let { state.probeResults[it] } ?: return
    val message = when (result) {
        ProbeResult.OK -> R.string.probe_ok
        ProbeResult.TIMEOUT -> R.string.probe_timeout
        ProbeResult.REFUSED -> R.string.probe_refused
        ProbeResult.UNREACHABLE -> R.string.probe_unreachable
        ProbeResult.API_ERROR -> R.string.probe_api_error
    }
    Text(
        stringResource(message), style = MaterialTheme.typography.bodySmall,
        color = if (result == ProbeResult.OK) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun HistorySection(entries: List<HistoryEntry>) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.history_title, entries.size), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
        }
        if (expanded) {
            if (entries.isEmpty()) Text(stringResource(R.string.history_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            entries.forEach { entry ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(entry.fechaHoraIngreso.orEmpty(), style = MaterialTheme.typography.labelLarge)
                        Text(listOfNotNull(entry.placa, entry.tipoVehiculo, entry.marca).filter { it.isNotBlank() }.joinToString(" / "))
                        if (!entry.nombreCliente.isNullOrBlank()) Text(entry.nombreCliente)
                        if (!entry.servicio.isNullOrBlank()) Text(entry.servicio)
                        Text(entry.valorPagado.orEmpty(), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
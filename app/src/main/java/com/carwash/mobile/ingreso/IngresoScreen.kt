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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
fun IngresoScreen(model: IngresoViewModel) {
    val state = model.state
    val editable = !state.loading && !state.saving && !state.reviewRequired
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
                    ToolButton(Icons.Default.Refresh, R.string.refresh, !state.loading && !state.saving) {
                        model.connect(state.serverUrl)
                    }
                    ToolButton(Icons.Default.Settings, R.string.connection, editable) { showConnection = true }
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
                        enabled = editable && state.connected && !state.searching,
                        modifier = Modifier.testTag("save"),
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(if (state.saving) R.string.saving else R.string.save))
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
            VehicleTypeMenu(state, editable && state.connected, model::selectType)
            IngresoField(
                state.form.customer, { value -> model.edit { it.copy(customer = value.uppercase(Locale.ROOT)) } },
                R.string.customer_label, "customer", editable,
                capitalization = KeyboardCapitalization.Characters,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IngresoField(
                    state.form.phone, { value -> model.edit { it.copy(phone = value) } },
                    R.string.phone_label, "phone", editable, Modifier.weight(1f), keyboardType = KeyboardType.Phone,
                )
                IngresoField(
                    state.form.order, { value -> model.edit { it.copy(order = value) } },
                    R.string.order_label, "order", editable, Modifier.weight(1f),
                    error = FormError.ORDER in state.errors, errorText = R.string.required_order,
                )
            }
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
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showConnection) ConnectionDialog(state, onDismiss = { showConnection = false }) { url ->
        keyboard?.hide()
        model.connect(url)
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
            Text(stringResource(when { state.loading -> R.string.connecting; state.connected -> R.string.connected; else -> R.string.disconnected }),
                style = MaterialTheme.typography.labelLarge)
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
    }
    val positive = notice.kind == NoticeKind.SAVED || notice.kind == NoticeKind.NOT_FOUND
    val background = if (positive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer
    val foreground = if (positive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer
    Row(Modifier.fillMaxWidth().background(background).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (notice.kind == NoticeKind.SAVED) Icon(Icons.Default.CheckCircle, null, tint = foreground)
        Text(stringResource(message, notice.detail), color = foreground, style = MaterialTheme.typography.bodyMedium)
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
private fun ConnectionDialog(state: IngresoState, onDismiss: () -> Unit, onConnect: (String) -> Unit) {
    var url by rememberSaveable { mutableStateOf(state.serverUrl) }
    val valid = normalizeServer(url) != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.connection)) },
        text = {
            OutlinedTextField(
                value = url, onValueChange = { url = it }, modifier = Modifier.fillMaxWidth().testTag("serverUrl"),
                label = { Text(stringResource(R.string.server_label)) },
                placeholder = { Text(stringResource(R.string.server_hint)) },
                singleLine = true, isError = url.isNotBlank() && !valid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                supportingText = if (url.isNotBlank() && !valid) ({ Text(stringResource(R.string.invalid_server)) }) else null,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConnect(url) }, enabled = valid && !state.loading) {
                Text(stringResource(R.string.connect))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
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
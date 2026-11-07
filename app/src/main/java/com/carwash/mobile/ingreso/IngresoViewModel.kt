package com.carwash.mobile.ingreso

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

interface ServerSettings {
    var serverUrl: String
    var backupUrl: String
        get() = ""
        set(_) {}
    var pendingTurn: String?
    var whatsAppCountryCode: String
        get() = "+57"
        set(_) {}
}

class AndroidServerSettings(context: Context, private val defaultUrl: String) : ServerSettings {
    private val preferences = context.getSharedPreferences("server", Context.MODE_PRIVATE)

    override var serverUrl: String
        get() = preferences.getString("url", defaultUrl).orEmpty()
        set(value) { preferences.edit().putString("url", value).apply() }

    override var backupUrl: String
        get() = preferences.getString("backupUrl", "").orEmpty()
        set(value) { preferences.edit().putString("backupUrl", value).apply() }

    override var pendingTurn: String?
        get() = preferences.getString("pendingTurn", null)
        set(value) { check(preferences.edit().putString("pendingTurn", value).commit()) }

    override var whatsAppCountryCode: String
        get() = preferences.getString("whatsAppCountryCode", "+57").orEmpty()
        set(value) { preferences.edit().putString("whatsAppCountryCode", value).apply() }
}

enum class NoticeKind { CONNECTION_FAILED, INVALID_SERVER, LOOKUP_FAILED, NOT_FOUND, CLOSED_CASH, INVALID_FORM, SAVED, SAVE_FAILED, REVIEW_REQUIRED, IN_PROCESS, UPDATED }
data class Notice(
    val kind: NoticeKind,
    val detail: String = "",
    val registrationStage: RegistrationStage? = null,
    val failureCode: String? = null,
)

data class IngresoState(
    val serverUrl: String = "",
    val backupUrl: String = "",
    val usingBackup: Boolean = false,
    val probing: Boolean = false,
    val probeResults: Map<String, ProbeResult> = emptyMap(),
    val connected: Boolean = false,
    val loading: Boolean = false,
    val searching: Boolean = false,
    val saving: Boolean = false,
    val reviewRequired: Boolean = false,
    val pendingNumber: String = "",
    val catalog: Catalog = Catalog(emptyList(), emptyList(), null),
    val form: IngresoForm = IngresoForm(),
    val history: List<HistoryEntry> = emptyList(),
    val searched: Boolean = false,
    val errors: Set<FormError> = emptySet(),
    val notice: Notice? = null,
    val notificationTurnId: Int? = null,
    val notificationStatus: String? = null,
    val whatsAppTurnId: Int? = null,
    val whatsAppStatus: String? = null,
    val whatsAppCanRetry: Boolean = false,
    val whatsAppSending: Boolean = false,
    val whatsAppMode: WhatsAppMode = WhatsAppMode.MANUAL,
    val whatsAppCountryCode: String = "+57",
    val manualWhatsAppReceipt: ManualWhatsAppReceipt? = null,
    val manualWhatsAppAutoOpen: Boolean = false,
    val pickupReceiptUnavailable: Boolean = false,
    val editingTurnId: Int? = null,
    val entryPrintTurnId: Int? = null,
    val entryPrintStatus: String? = null,
)

class IngresoViewModel(
    private val settings: ServerSettings,
    private val probe: suspend (String) -> ProbeResult = ::probeServer,
    private val repositoryFactory: (String) -> IngresoRepository = ApiIngresoRepository::create,
) : ViewModel() {
    var state by mutableStateOf(
        IngresoState(
            serverUrl = settings.serverUrl,
            backupUrl = settings.backupUrl,
            whatsAppCountryCode = normalizeWhatsAppCountryCode(settings.whatsAppCountryCode) ?: "+57",
            reviewRequired = settings.pendingTurn != null,
            pendingNumber = settings.pendingTurn.orEmpty(),
            notice = settings.pendingTurn?.let { Notice(NoticeKind.REVIEW_REQUIRED, it) },
        ),
    )
        private set

    private var repository: IngresoRepository? = null
    private var lookupJob: Job? = null
    private var pickupCodeProvider: (suspend (String, Int) -> String)? = null
    private var entryPrintProvider: (suspend (String, Int, Set<Int>) -> String)? = null
    private var companyProvider: (suspend (String) -> com.carwash.mobile.empresa.EmpresaDto?)? = null

    fun configurePickup(provider: suspend (String, Int) -> String) { pickupCodeProvider = provider }
    fun configureEntryPrinting(provider: suspend (String, Int, Set<Int>) -> String) { entryPrintProvider = provider }
    fun configureCompany(provider: suspend (String) -> com.carwash.mobile.empresa.EmpresaDto?) { companyProvider = provider }

    init {
        if (state.serverUrl.isNotBlank()) connect(state.serverUrl)
    }

    fun connect(rawUrl: String, rawBackup: String = state.backupUrl) {
        if (state.saving || state.loading) return
        val url = normalizeServer(rawUrl)
        val backup = if (rawBackup.isBlank()) "" else normalizeServer(rawBackup)
        if (url == null || backup == null) {
            state = state.copy(notice = Notice(NoticeKind.INVALID_SERVER))
            return
        }
        if (state.reviewRequired && url != state.serverUrl) return
        lookupJob?.cancel()
        val changed = url != state.serverUrl
        state = state.copy(
            serverUrl = url, backupUrl = backup, usingBackup = false, connected = false, loading = true, searching = false,
            form = if (changed) IngresoForm() else state.form,
            history = if (changed) emptyList() else state.history,
            catalog = if (changed) Catalog(emptyList(), emptyList(), null) else state.catalog,
            searched = if (changed) false else state.searched,
            manualWhatsAppReceipt = if (changed) null else state.manualWhatsAppReceipt,
            manualWhatsAppAutoOpen = if (changed) false else state.manualWhatsAppAutoOpen,
            notice = if (state.reviewRequired) state.notice else null,
        )
        val primary = repositoryFactory(url)
        repository = primary
        viewModelScope.launch {
            val candidates = buildList {
                add(false to primary)
                if (backup.isNotEmpty() && backup != url) add(true to repositoryFactory(backup))
            }
            for ((isBackup, source) in candidates) {
                try {
                    val catalog = source.load()
                    repository = source
                    settings.serverUrl = url
                    settings.backupUrl = backup
                    state = state.copy(connected = true, loading = false, usingBackup = isBackup, catalog = catalog)
                    return@launch
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                }
            }
            state = state.copy(connected = false, loading = false, notice = Notice(NoticeKind.CONNECTION_FAILED))
        }
    }

    fun testConnection(rawUrl: String, rawBackup: String) {
        if (state.probing) return
        val urls = listOf(rawUrl, rawBackup).filter { it.isNotBlank() }.map { normalizeServer(it) }
        if (urls.isEmpty() || urls.any { it == null }) {
            state = state.copy(notice = Notice(NoticeKind.INVALID_SERVER))
            return
        }
        state = state.copy(probing = true, probeResults = emptyMap())
        viewModelScope.launch {
            try {
                for (url in urls.filterNotNull().distinct()) {
                    val result = probe(url)
                    state = state.copy(probeResults = state.probeResults + (url to result))
                }
            } finally {
                state = state.copy(probing = false)
            }
        }
    }

    fun edit(change: (IngresoForm) -> IngresoForm) {
        if (state.loading || state.saving || state.reviewRequired) return
        lookupJob?.cancel()
        val updated = change(state.form)
        val plateChanged = updated.plate != state.form.plate
        state = state.copy(
            form = updated,
            searching = false,
            searched = if (plateChanged) false else state.searched,
            history = if (plateChanged) emptyList() else state.history,
            editingTurnId = if (plateChanged) null else state.editingTurnId,
            errors = if (state.errors.isEmpty()) emptySet() else updated.errors(),
            notice = null,
        )
    }

    fun acceptScannedPlate(value: String) {
        if (state.loading || state.saving || state.reviewRequired) return
        val plate = reviewedPlate(value) ?: return
        val changed = plate != reviewedPlate(state.form.plate)
        edit {
            if (changed) it.copy(plate = plate, customer = "", phone = "", email = "", brand = "", typeId = null)
                .withServices(emptySet(), state.catalog.services)
            else it.copy(plate = plate)
        }
        if (state.connected) lookup()
    }

    fun selectType(id: Int) {
        if (state.catalog.types.none { it.idTipoVehiculo == id }) return
        edit { it.copy(typeId = id).withServices(emptySet(), state.catalog.services) }
    }

    fun toggleService(id: Int) {
        val form = state.form
        if (state.catalog.services.none { it.idServicio == id && it.idTipoVehiculo == form.typeId }) return
        val selected = if (id in form.serviceIds) form.serviceIds - id else form.serviceIds + id
        edit { it.withServices(selected, state.catalog.services) }
    }

    fun lookup() {
        if (state.loading || state.saving || state.reviewRequired || !state.connected) return
        val source = repository ?: return
        val plate = state.form.plate.trim().uppercase(Locale.ROOT)
        if (plate.isEmpty()) {
            state = state.copy(errors = setOf(FormError.PLATE), notice = Notice(NoticeKind.INVALID_FORM))
            return
        }
        lookupJob?.cancel()
        state = state.copy(form = state.form.copy(plate = plate), searching = true, notice = null)
        lookupJob = viewModelScope.launch {
            try {
                val result = source.lookup(plate)
                if (state.form.plate != plate) return@launch
                val vehicle = result.vehicle
                val inProcess = result.inProcess
                val typeId = vehicle?.idTipoVehiculo?.takeIf { id -> state.catalog.types.any { it.idTipoVehiculo == id } }
                val form = if (inProcess != null) {
                    val available = state.catalog.services
                        .filter { it.idTipoVehiculo == inProcess.idTipoVehiculo }.map { it.idServicio }.toSet()
                    state.form.copy(
                        typeId = inProcess.idTipoVehiculo,
                        customer = inProcess.nombreCliente.orEmpty().uppercase(Locale.ROOT),
                        phone = inProcess.numeroCelular.orEmpty(),
                        email = inProcess.correoElectronico.orEmpty(),
                        brand = inProcess.marca.orEmpty().uppercase(Locale.ROOT),
                        observations = inProcess.observaciones.orEmpty(),
                        serviceIds = inProcess.idServicios.filter { it in available }.toSet(),
                        value = inProcess.valor.setScale(2).toPlainString(),
                        baseValue = inProcess.valorBaseComision.setScale(2).toPlainString(),
                    )
                } else state.form.copy(
                    typeId = typeId,
                    customer = vehicle?.nombreCliente.orEmpty().uppercase(Locale.ROOT),
                    phone = vehicle?.numeroCelular.orEmpty(),
                    email = vehicle?.correoElectronico.orEmpty(),
                    brand = vehicle?.marca.orEmpty().uppercase(Locale.ROOT),
                ).withServices(emptySet(), state.catalog.services)
                state = state.copy(
                    form = form, searching = false, searched = true,
                    history = result.history, errors = emptySet(),
                    editingTurnId = inProcess?.idTurno,
                    notice = when {
                        inProcess != null -> Notice(NoticeKind.IN_PROCESS, inProcess.numeroTurno.orEmpty())
                        vehicle == null -> Notice(NoticeKind.NOT_FOUND)
                        else -> null
                    },
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                state = state.copy(searching = false, notice = Notice(NoticeKind.LOOKUP_FAILED))
            }
        }
    }

    fun save() {
        if (state.saving || state.loading || state.searching || state.reviewRequired) return
        if (!state.connected) {
            state = state.copy(notice = Notice(NoticeKind.CONNECTION_FAILED))
            return
        }
        val errors = state.form.errors().toMutableSet()
        if (state.catalog.types.none { it.idTipoVehiculo == state.form.typeId }) errors.add(FormError.TYPE)
        val available = state.catalog.services.filter { it.idTipoVehiculo == state.form.typeId }.map { it.idServicio }.toSet()
        if (!available.containsAll(state.form.serviceIds)) errors.add(FormError.SERVICES)
        if (errors.isNotEmpty()) {
            state = state.copy(errors = errors, notice = Notice(NoticeKind.INVALID_FORM))
            return
        }
        val source = repository ?: return
        val form = state.form
        val whatsAppMode = state.whatsAppMode
        val selectedServices = state.catalog.services.filter { it.idServicio in form.serviceIds && it.idTipoVehiculo == form.typeId }
        val editingId = state.editingTurnId
        if (editingId != null) {
            state = state.copy(saving = true, errors = emptySet(), notice = null, manualWhatsAppReceipt = null, manualWhatsAppAutoOpen = false, pickupReceiptUnavailable = false)
            viewModelScope.launch {
                try {
                    source.updateInProcess(editingId, form)
                    val receipt = if (whatsAppMode == WhatsAppMode.MANUAL) prepareManualReceipt(form, selectedServices, editingId) else null
                    state = state.copy(
                        form = IngresoForm(), history = emptyList(), searched = false,
                        saving = false, editingTurnId = null,
                        manualWhatsAppReceipt = receipt,
                        manualWhatsAppAutoOpen = receipt != null,
                        notice = Notice(NoticeKind.UPDATED, form.plate.trim().uppercase(Locale.ROOT)),
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    state = state.copy(saving = false, notice = Notice(NoticeKind.SAVE_FAILED))
                }
            }
            return
        }
        state = state.copy(
            saving = true,
            errors = emptySet(),
            notice = null,
            notificationTurnId = null,
            notificationStatus = null,
            entryPrintTurnId = null, entryPrintStatus = null,
            pickupReceiptUnavailable = false,
            whatsAppTurnId = null, whatsAppStatus = null,
            whatsAppCanRetry = false, whatsAppSending = false,
            manualWhatsAppReceipt = null,
            manualWhatsAppAutoOpen = false,
        )
        viewModelScope.launch {
            try {
                val saved = source.register(form) { number ->
                    settings.pendingTurn = number
                    state = state.copy(pendingNumber = number)
                }
                settings.pendingTurn = null
                val printUrl = if (state.usingBackup) state.backupUrl else state.serverUrl
                val printer = entryPrintProvider
                if (printer != null) {
                    state = state.copy(entryPrintTurnId = saved.idTurno, entryPrintStatus = "PENDING")
                    viewModelScope.launch {
                        val status = try { printer(printUrl, saved.idTurno, form.serviceIds) }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { "UNKNOWN" }
                        if (repository === source && state.entryPrintTurnId == saved.idTurno)
                            state = state.copy(entryPrintStatus = status)
                    }
                }
                val receipt = if (whatsAppMode == WhatsAppMode.MANUAL) prepareManualReceipt(form, selectedServices, saved.idTurno) else null
                state = state.copy(
                    form = IngresoForm(), history = emptyList(), searched = false,
                    saving = false, reviewRequired = false, pendingNumber = "",
                    notificationTurnId = saved.idTurno,
                    notificationStatus = saved.notificationStatus,
                    whatsAppTurnId = saved.idTurno.takeIf { whatsAppMode == WhatsAppMode.AUTOMATIC },
                    whatsAppStatus = "PENDING".takeIf { whatsAppMode == WhatsAppMode.AUTOMATIC },
                    manualWhatsAppReceipt = receipt,
                    manualWhatsAppAutoOpen = receipt != null,
                    notice = Notice(
                        NoticeKind.SAVED,
                        listOf(saved.numeroTurno.orEmpty(), saved.notificationStatus?.let { "Correo: $it" })
                            .filterNot { it.isNullOrBlank() }
                            .joinToString(" - "),
                    ),
                )
                if (whatsAppMode == WhatsAppMode.AUTOMATIC) sendWhatsApp(source, saved.idTurno)
                viewModelScope.launch {
                    val status = try {
                        source.notifyEmail(saved.idTurno)?.takeIf { it.idTurno == saved.idTurno }?.estado ?: "UNKNOWN"
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        "UNKNOWN"
                    }
                    if (state.notificationTurnId == saved.idTurno && repository === source) {
                        state = state.copy(notificationStatus = status)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: CashClosedException) {
                state = state.copy(
                    saving = false, catalog = state.catalog.copy(cash = null),
                    notice = Notice(NoticeKind.CLOSED_CASH),
                )
            } catch (failure: Exception) {
                val uncertain = settings.pendingTurn != null || (failure as? RegistrationException)?.uncertain == true
                val number = settings.pendingTurn.orEmpty().ifEmpty { (failure as? RegistrationException)?.turnNumber.orEmpty() }
                val registrationFailure = failure as? RegistrationException
                val cause = registrationFailure?.cause ?: failure
                state = state.copy(
                    saving = false, reviewRequired = uncertain, pendingNumber = number,
                    notice = Notice(
                        if (uncertain) NoticeKind.REVIEW_REQUIRED else NoticeKind.SAVE_FAILED, number,
                        registrationFailure?.stage,
                        if (cause is retrofit2.HttpException) "HTTP ${cause.code()}" else cause.javaClass.simpleName,
                    ),
                )
            }
        }
    }

    private suspend fun prepareManualReceipt(form: IngresoForm, services: List<VehicleService>, idTurno: Int): ManualWhatsAppReceipt? {
        val provider = pickupCodeProvider
        val code = if (provider == null) "" else try {
            val url = if (state.usingBackup) state.backupUrl else state.serverUrl
            provider(url, idTurno).also { check(it.matches(Regex("[0-9A-F]{16}"))) }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            state = state.copy(pickupReceiptUnavailable = true)
            return null
        }
        val company = try { companyProvider?.invoke(if (state.usingBackup) state.backupUrl else state.serverUrl) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { null }
        return manualReceipt(form, services).copy(pickupCode = code, empresa = company)
    }

    private fun manualReceipt(form: IngresoForm, services: List<VehicleService>) = ManualWhatsAppReceipt(
        phone = whatsAppRecipient(form.phone, state.whatsAppCountryCode) ?: form.phone.trim(),
        plate = form.plate.trim().uppercase(Locale.ROOT),
        customer = form.customer.trim(), brand = form.brand.trim(),
        services = services, total = requireNotNull(parseAmount(form.value)),
    )

    fun selectWhatsAppMode(mode: WhatsAppMode) {
        if (state.saving || state.loading) return
        state = state.copy(whatsAppMode = mode)
    }

    fun configureWhatsAppCountryCode(value: String): Boolean {
        if (state.saving || state.loading) return false
        val prefix = normalizeWhatsAppCountryCode(value) ?: return false
        settings.whatsAppCountryCode = prefix
        state = state.copy(whatsAppCountryCode = prefix)
        return true
    }

    fun consumeManualWhatsAppAutoOpen() {
        state = state.copy(manualWhatsAppAutoOpen = false)
    }

    fun editManualWhatsAppPhone(phone: String) {
        val receipt = state.manualWhatsAppReceipt ?: return
        state = state.copy(manualWhatsAppReceipt = receipt.copy(phone = phone))
    }

    fun clearManualWhatsApp() {
        state = state.copy(manualWhatsAppReceipt = null, manualWhatsAppAutoOpen = false)
    }

    private fun sendWhatsApp(source: IngresoRepository, idTurno: Int, retry: Boolean = false) {
        state = state.copy(whatsAppSending = true, whatsAppCanRetry = false)
        viewModelScope.launch {
            try {
                val result = if (retry) source.retryWhatsApp(idTurno) else source.notifyWhatsApp(idTurno)
                if (state.whatsAppTurnId == idTurno && repository === source) {
                    val matching = result?.takeIf { it.idTurno == idTurno }
                    state = state.copy(
                        whatsAppStatus = matching?.estado ?: if (result == null) "UNAVAILABLE" else "UNKNOWN",
                        whatsAppCanRetry = matching?.accion == "RETRY_MANUAL",
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (state.whatsAppTurnId == idTurno && repository === source) {
                    state = state.copy(whatsAppStatus = "UNKNOWN", whatsAppCanRetry = false)
                }
            } finally {
                if (state.whatsAppTurnId == idTurno && repository === source) {
                    state = state.copy(whatsAppSending = false)
                }
            }
        }
    }

    fun retryWhatsApp() {
        val idTurno = state.whatsAppTurnId ?: return
        val source = repository ?: return
        if (!state.whatsAppCanRetry || state.whatsAppSending) return
        sendWhatsApp(source, idTurno, retry = true)
    }

    fun clearWhatsAppStatus() {
        state = state.copy(whatsAppTurnId = null, whatsAppStatus = null, whatsAppCanRetry = false, whatsAppSending = false)
    }

    fun retryEmail() {
        val idTurno = state.notificationTurnId ?: return
        val source = repository ?: return
        viewModelScope.launch {
            try {
                val result = source.retryEmail(idTurno) ?: return@launch
                state = state.copy(notificationStatus = result.estado, notice = Notice(
                    NoticeKind.SAVED,
                    "$idTurno - Correo: ${result.estado}",
                ))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                state = state.copy(notificationStatus = "FAILED", notice = Notice(
                    NoticeKind.SAVED,
                    "$idTurno - Correo: FAILED",
                ))
            }
        }

    }

    fun clearEmailStatus() {
        state = state.copy(notificationTurnId = null, notificationStatus = null)
    }

    fun clear() {
        if (state.saving || state.loading) return
        lookupJob?.cancel()
        settings.pendingTurn = null
        state = state.copy(
            form = IngresoForm(), history = emptyList(), searched = false, searching = false,
            errors = emptySet(), notice = null, reviewRequired = false, pendingNumber = "",
            pickupReceiptUnavailable = false,
            notificationTurnId = null, notificationStatus = null, editingTurnId = null,
            whatsAppTurnId = null, whatsAppStatus = null, whatsAppCanRetry = false, whatsAppSending = false,
            manualWhatsAppReceipt = null,
            manualWhatsAppAutoOpen = false,
        )
    }
}
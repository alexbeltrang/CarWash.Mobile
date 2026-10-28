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
    var pendingTurn: String?
}

class AndroidServerSettings(context: Context, private val defaultUrl: String) : ServerSettings {
    private val preferences = context.getSharedPreferences("server", Context.MODE_PRIVATE)

    override var serverUrl: String
        get() = preferences.getString("url", defaultUrl).orEmpty()
        set(value) { preferences.edit().putString("url", value).apply() }

    override var pendingTurn: String?
        get() = preferences.getString("pendingTurn", null)
        set(value) { check(preferences.edit().putString("pendingTurn", value).commit()) }
}

enum class NoticeKind { CONNECTION_FAILED, INVALID_SERVER, LOOKUP_FAILED, NOT_FOUND, CLOSED_CASH, INVALID_FORM, SAVED, SAVE_FAILED, REVIEW_REQUIRED }
data class Notice(val kind: NoticeKind, val detail: String = "")

data class IngresoState(
    val serverUrl: String = "",
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
)

class IngresoViewModel(
    private val settings: ServerSettings,
    private val repositoryFactory: (String) -> IngresoRepository = ApiIngresoRepository::create,
) : ViewModel() {
    var state by mutableStateOf(
        IngresoState(
            serverUrl = settings.serverUrl,
            reviewRequired = settings.pendingTurn != null,
            pendingNumber = settings.pendingTurn.orEmpty(),
            notice = settings.pendingTurn?.let { Notice(NoticeKind.REVIEW_REQUIRED, it) },
        ),
    )
        private set

    private var repository: IngresoRepository? = null
    private var lookupJob: Job? = null

    init {
        if (state.serverUrl.isNotBlank()) connect(state.serverUrl)
    }

    fun connect(rawUrl: String) {
        if (state.saving || state.loading) return
        val url = normalizeServer(rawUrl)
        if (url == null) {
            state = state.copy(notice = Notice(NoticeKind.INVALID_SERVER))
            return
        }
        if (state.reviewRequired && url != state.serverUrl) return
        lookupJob?.cancel()
        val changed = url != state.serverUrl
        state = state.copy(
            serverUrl = url, connected = false, loading = true, searching = false,
            form = if (changed) IngresoForm() else state.form,
            history = if (changed) emptyList() else state.history,
            catalog = if (changed) Catalog(emptyList(), emptyList(), null) else state.catalog,
            searched = if (changed) false else state.searched,
            notice = if (state.reviewRequired) state.notice else null,
        )
        val source = repositoryFactory(url)
        repository = source
        viewModelScope.launch {
            try {
                val catalog = source.load()
                settings.serverUrl = url
                state = state.copy(connected = true, loading = false, catalog = catalog)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                state = state.copy(connected = false, loading = false, notice = Notice(NoticeKind.CONNECTION_FAILED))
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
            errors = if (state.errors.isEmpty()) emptySet() else updated.errors(),
            notice = null,
        )
    }

    fun acceptScannedPlate(value: String) {
        if (state.loading || state.saving || state.reviewRequired) return
        val plate = reviewedPlate(value) ?: return
        val changed = plate != reviewedPlate(state.form.plate)
        edit {
            if (changed) it.copy(plate = plate, customer = "", phone = "", brand = "", typeId = null)
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
                val typeId = vehicle?.idTipoVehiculo?.takeIf { id -> state.catalog.types.any { it.idTipoVehiculo == id } }
                val form = state.form.copy(
                    typeId = typeId,
                    customer = vehicle?.nombreCliente.orEmpty().uppercase(Locale.ROOT),
                    phone = vehicle?.numeroCelular.orEmpty(),
                    brand = vehicle?.marca.orEmpty().uppercase(Locale.ROOT),
                ).withServices(emptySet(), state.catalog.services)
                state = state.copy(
                    form = form, searching = false, searched = true,
                    history = result.history, errors = emptySet(),
                    notice = if (vehicle == null) Notice(NoticeKind.NOT_FOUND) else null,
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
        state = state.copy(saving = true, errors = emptySet(), notice = null)
        viewModelScope.launch {
            try {
                val saved = source.register(form) { number ->
                    settings.pendingTurn = number
                    state = state.copy(pendingNumber = number)
                }
                settings.pendingTurn = null
                state = state.copy(
                    form = IngresoForm(), history = emptyList(), searched = false,
                    saving = false, reviewRequired = false, pendingNumber = "",
                    notice = Notice(NoticeKind.SAVED, saved.numeroTurno.orEmpty()),
                )
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
                state = state.copy(
                    saving = false, reviewRequired = uncertain, pendingNumber = number,
                    notice = Notice(if (uncertain) NoticeKind.REVIEW_REQUIRED else NoticeKind.SAVE_FAILED, number),
                )
            }
        }
    }

    fun clear() {
        if (state.saving || state.loading) return
        lookupJob?.cancel()
        settings.pendingTurn = null
        state = state.copy(
            form = IngresoForm(), history = emptyList(), searched = false, searching = false,
            errors = emptySet(), notice = null, reviewRequired = false, pendingNumber = "",
        )
    }
}
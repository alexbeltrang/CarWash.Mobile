package com.carwash.mobile.reportes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carwash.mobile.auth.Session
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReporteVentasViewModel(private var repository: ReporteVentasRepository, private var session: Session) : ViewModel() {
    private val businessDate = runCatching { session.serverDate?.let(LocalDate::parse) }.getOrNull()
    var state by mutableStateOf(ReportState(edited = ReportCriteria(businessDate, businessDate),
        status = if (session.isAdmin) ReportStatus.NOT_QUERIED else ReportStatus.FORBIDDEN))
        private set
    val canQuery: Boolean get() = session.isAdmin && state.status != ReportStatus.FORBIDDEN
    private var generation = 0L
    private var job: Job? = null
    private var optionsJob: Job? = null
    private var optionsGeneration = 0L
    private var hasDraft = true
    private var activeCriteria: ReportCriteria? = null

    fun edit(criteria: ReportCriteria) {
        state = state.copy(edited = criteria, fieldErrors = emptySet())
    }

    fun consult() {
        val criteria = state.edited.normalized()
        val invalid = criteria.invalidFields()
        if (invalid.isNotEmpty()) {
            state = state.copy(fieldErrors = invalid)
            return
        }
        state = state.copy(edited = criteria)
        firstPage(criteria)
    }

    fun clearFilters() = edit(state.edited.copy(paymentMethodId = null, operatorId = null, plate = null, vehicleTypeId = null))

    fun loadOptions() {
        val criteria = ReportCriteria(state.edited.initialDate, state.edited.finalDate)
        if (!canQuery || session.accessToken == null || criteria.invalidFields().isNotEmpty()) return
        val previous = state.options
        if (previous != null && state.optionsError == null) return
        optionsJob?.cancel()
        val currentGeneration = ++optionsGeneration
        state = state.copy(optionsLoading = true, optionsError = null)
        optionsJob = viewModelScope.launch {
            try {
                val response = repository.options(criteria.initialDate!!, criteria.finalDate!!)
                if (currentGeneration != optionsGeneration) return@launch
                state = state.copy(options = response, optionsLoading = false, optionsError = null)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: ReportError) {
                if (currentGeneration != optionsGeneration) return@launch
                if (error.kind == ReportErrorKind.SESSION_REQUIRED || error.kind == ReportErrorKind.ADMIN_REQUIRED) fail(error, false)
                else state = state.copy(optionsLoading = false, optionsError = error.kind)
            }
        }
    }

    fun refresh() = firstPage(state.page?.criteria ?: activeCriteria ?: state.edited.normalized())

    fun retry() {
        if (state.partial) loadMore() else firstPage(activeCriteria ?: state.edited.normalized())
    }

    fun loadMore() {
        val page = state.page ?: return
        val cursor = page.nextCursor ?: return
        if (state.loadingMore || state.status !in setOf(ReportStatus.RESULTS, ReportStatus.ERROR) || !canQuery) return
        state = state.copy(loadingMore = true, error = null, partial = false)
        fetch(page.criteria, cursor, generation)
    }

    private fun firstPage(criteria: ReportCriteria) {
        if (!canQuery) return
        val invalid = criteria.invalidFields()
        if (invalid.isNotEmpty()) {
            state = state.copy(fieldErrors = invalid)
            return
        }
        job?.cancel()
        generation++
        activeCriteria = criteria
        state = state.copy(page = null, status = ReportStatus.LOADING, fieldErrors = emptySet(), error = null, loadingMore = false, partial = false)
        if (session.accessToken == null) {
            fail(ReportError(ReportErrorKind.SESSION_REQUIRED), false)
            return
        }
        fetch(criteria, null, generation)
    }

    private fun fetch(criteria: ReportCriteria, cursor: String?, currentGeneration: Long) {
        job = viewModelScope.launch {
            try {
                val response = repository.sales(criteria, cursor)
                if (currentGeneration != generation) return@launch
                if (response.criteria != criteria) throw ReportError(ReportErrorKind.INVALID_RESPONSE)
                val previous = state.page
                if (cursor != null && previous != null) {
                    if (response.count != previous.count || response.total.compareTo(previous.total) != 0) {
                        state = state.copy(status = ReportStatus.DATA_CHANGED, loadingMore = false, partial = true)
                        return@launch
                    }
                    if (response.nextCursor == cursor) throw ReportError(ReportErrorKind.INVALID_RESPONSE)
                    state = state.copy(page = response.copy(items = (previous.items + response.items).distinctBy { it.id }),
                        status = ReportStatus.RESULTS, loadingMore = false, partial = false, error = null)
                } else {
                    state = state.copy(page = response, status = if (response.count == 0L) ReportStatus.EMPTY else ReportStatus.RESULTS,
                        loadingMore = false, partial = false, error = null)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: ReportError) {
                if (currentGeneration == generation) fail(error, cursor != null)
            }
        }
    }

    private fun fail(error: ReportError, continuation: Boolean) {
        when (error.kind) {
            ReportErrorKind.SESSION_REQUIRED, ReportErrorKind.ADMIN_REQUIRED -> {
                generation++
                optionsGeneration++
                job?.cancel()
                optionsJob?.cancel()
                session = session.metadata()
                repository = UnauthenticatedReportRepository
                state = state.copy(page = null, status = if (error.kind == ReportErrorKind.SESSION_REQUIRED) ReportStatus.SESSION_REQUIRED else ReportStatus.FORBIDDEN,
                    error = error.kind, loadingMore = false, partial = false, options = null, optionsLoading = false, optionsError = null)
            }
            else -> state = state.copy(status = ReportStatus.ERROR, error = error.kind, fieldErrors = error.fields,
                loadingMore = false, partial = continuation)
        }
    }

    fun clear() {
        generation++
        job?.cancel()
        optionsGeneration++
        optionsJob?.cancel()
        activeCriteria = null
        hasDraft = false
        session = session.metadata()
        repository = UnauthenticatedReportRepository
        state = ReportState()
    }

    fun ownsSession(value: Session): Boolean = session.userId == value.userId && session.baseUrl == value.baseUrl

    fun bind(value: Session, source: ReporteVentasRepository) {
        if (value == session) return
        val preserveDraft = ownsSession(value) && hasDraft
        val edited = state.edited
        clear()
        session = value
        repository = source
        hasDraft = true
        val date = runCatching { value.serverDate?.let(LocalDate::parse) }.getOrNull()
        state = ReportState(edited = if (preserveDraft) edited.copy(initialDate = edited.initialDate ?: date, finalDate = edited.finalDate ?: date) else ReportCriteria(date, date),
            status = if (value.isAdmin) ReportStatus.NOT_QUERIED else ReportStatus.FORBIDDEN)
    }

    override fun onCleared() {
        clear()
        super.onCleared()
    }
}

private object UnauthenticatedReportRepository : ReporteVentasRepository {
    override suspend fun sales(criteria: ReportCriteria, cursor: String?): SalesPage = throw ReportError(ReportErrorKind.SESSION_REQUIRED)
}
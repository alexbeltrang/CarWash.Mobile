package com.carwash.mobile.retiro

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

data class RetiroState(
    val plate: String = "",
    val receipt: RetiroReceipt? = null, val busy: Boolean = false,
    val error: RetiroError? = null,
    val printWidthMm: Int = 80, val printRequestId: String? = null, val printJob: PrintJob? = null,
    val printHistoryLoaded: Boolean = false, val printCheckRequired: Boolean = false, val printError: Boolean = false,
)

class RetiroViewModel(private val repository: RetiroRepository) : ViewModel() {
    var state by mutableStateOf(RetiroState())
        private set
    private var operation: Job? = null

    fun editPlate(value: String) {
        if (!state.busy) state = RetiroState(plate = value.take(20).uppercase(Locale.ROOT))
    }
    fun search() {
        if (state.busy || state.plate.isBlank()) return
        val plate = state.plate.trim().uppercase(Locale.ROOT)
        state = RetiroState(plate = plate, busy = true)
        operation = viewModelScope.launch {
            try { state = state.copy(receipt = repository.issue(IssueReceipt(placa = plate))); loadPrintHistory() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { state = state.copy(error = failure(error)) }
            finally { state = state.copy(busy = false) }
        }
    }
    fun refresh() {
        val receipt = state.receipt ?: return
        if (state.busy) return
        state = state.copy(busy = true, error = null)
        operation = viewModelScope.launch {
            try { state = state.copy(receipt = repository.get(receipt.idTurno)) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { state = state.copy(error = failure(error)) }
            finally { state = state.copy(busy = false) }
        }
    }
    fun printWidth(value: Int) {
        if (!state.busy && value == 80 && !state.printCheckRequired && (state.printRequestId == null || state.printJob != null))
            state = state.copy(printWidthMm = value)
    }

    fun print(newCopy: Boolean = false) {
        val receipt = state.receipt ?: return
        if (state.busy || !state.printHistoryLoaded || state.printCheckRequired || state.printJob?.estado in listOf("PENDING", "SUBMITTING")) return
        if (state.printJob != null && !newCopy || newCopy && state.printJob == null) return
        val id = if (newCopy) UUID.randomUUID().toString() else state.printRequestId ?: UUID.randomUUID().toString()
        val width = state.printWidthMm
        state = state.copy(busy = true, printRequestId = id, printJob = null, printError = false, error = null)
        operation = viewModelScope.launch {
            try { state = state.copy(printJob = repository.requestPrint(receipt.idTurno, id, width), printCheckRequired = false) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                state = state.copy(printCheckRequired = true, printError = true,
                    error = if (failure(error) == RetiroError.SESSION_REQUIRED) RetiroError.SESSION_REQUIRED else null)
                if (state.error == null) readPrintState()
            }
            finally { state = state.copy(busy = false) }
        }
    }

    fun checkPrint() {
        if (state.busy || state.receipt == null) return
        state = state.copy(busy = true, printError = false)
        operation = viewModelScope.launch {
            try { readPrintState() } finally { state = state.copy(busy = false) }
        }
    }

    private suspend fun loadPrintHistory() {
        try {
            val job = repository.latestPrint(state.receipt!!.idTurno)
            state = state.copy(printJob = job, printRequestId = job?.solicitudId, printHistoryLoaded = true, printCheckRequired = false, printError = false)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { state = state.copy(printCheckRequired = true, printError = true,
            error = if (failure(error) == RetiroError.SESSION_REQUIRED) RetiroError.SESSION_REQUIRED else state.error) }
    }

    private suspend fun readPrintState() {
        val id = state.printRequestId
        if (id == null) { loadPrintHistory(); return }
        try { state = state.copy(printJob = repository.getPrint(id), printCheckRequired = false, printHistoryLoaded = true, printError = false) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            if (failure(error) == RetiroError.NOT_FOUND) state = state.copy(printJob = null, printCheckRequired = false, printHistoryLoaded = true, printError = true)
            else state = state.copy(printCheckRequired = true, printError = true,
                error = if (failure(error) == RetiroError.SESSION_REQUIRED) RetiroError.SESSION_REQUIRED else state.error)
        }
    }

    fun clear() { operation?.cancel(); state = RetiroState() }
    private fun failure(error: Exception) = (error as? RetiroFailure)?.kind ?: RetiroError.UNAVAILABLE
}
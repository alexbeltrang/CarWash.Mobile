package com.carwash.mobile.asignacion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carwash.mobile.auth.Session
import kotlinx.coroutines.launch
import java.io.IOException

enum class AsignacionMessage { ASSIGNED, FORBIDDEN, NOT_FOUND, CONFLICT, ERROR }

data class AsignacionState(
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val vehicles: List<VehicleInProcess> = emptyList(),
    val selected: VehicleInProcess? = null,
    val operators: List<Operator> = emptyList(),
    val operatorsLoading: Boolean = false,
    val saving: Boolean = false,
    val message: AsignacionMessage? = null,
)

class AsignacionViewModel(
    private val repository: AsignacionRepository,
    private val session: Session,
) : ViewModel() {
    var state by mutableStateOf(AsignacionState())
        private set

    val canAssign: Boolean get() = session.isAdmin

    // La pantalla lo invoca cada vez que se abre, porque este ViewModel sobrevive a la navegación.
    fun refresh() {
        state = state.copy(selected = null, operators = emptyList(), message = null)
        loadVehicles()
    }

    private fun loadVehicles() {
        state = state.copy(loading = true, loadFailed = false)
        viewModelScope.launch {
            state = try {
                state.copy(loading = false, vehicles = repository.vehicles())
            } catch (_: IOException) {
                state.copy(loading = false, loadFailed = true)
            } catch (_: retrofit2.HttpException) {
                state.copy(loading = false, loadFailed = true)
            }
        }
    }

    fun select(vehicle: VehicleInProcess) {
        if (!canAssign || state.saving) return
        state = state.copy(selected = vehicle, operators = emptyList(), operatorsLoading = true, message = null)
        loadOperators()
    }

    fun dismiss() { if (!state.saving) state = state.copy(selected = null, operators = emptyList()) }
    fun clearMessage() { state = state.copy(message = null) }

    private fun loadOperators() {
        viewModelScope.launch {
            state = try {
                state.copy(operators = repository.availableOperators().sortedBy { it.fullName.lowercase() }, operatorsLoading = false)
            } catch (_: IOException) {
                state.copy(operatorsLoading = false, message = AsignacionMessage.ERROR)
            } catch (_: retrofit2.HttpException) {
                state.copy(operatorsLoading = false, message = AsignacionMessage.ERROR)
            }
        }
    }

    fun assign(operator: Operator) {
        val vehicle = state.selected ?: return
        if (state.saving || !canAssign) return
        state = state.copy(saving = true, message = null)
        viewModelScope.launch {
            val result = try {
                repository.assign(vehicle.idTurno, operator.idOperario, session.userId)
            } catch (_: retrofit2.HttpException) {
                AssignResult.ERROR
            }
            val message = when (result) {
                AssignResult.OK -> AsignacionMessage.ASSIGNED
                AssignResult.FORBIDDEN -> AsignacionMessage.FORBIDDEN
                AssignResult.NOT_FOUND -> AsignacionMessage.NOT_FOUND
                AssignResult.CONFLICT -> AsignacionMessage.CONFLICT
                AssignResult.ERROR -> AsignacionMessage.ERROR
            }
            state = state.copy(saving = false, message = message, selected = if (result == AssignResult.OK) null else state.selected)
            if (result == AssignResult.OK) loadVehicles() else if (result == AssignResult.CONFLICT) loadOperators()
        }
    }
}

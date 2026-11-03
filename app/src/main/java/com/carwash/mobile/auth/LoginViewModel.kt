package com.carwash.mobile.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

enum class LoginError { EMPTY, BAD_CREDENTIALS, UNREACHABLE, REPORT_SESSION_REQUIRED }

data class LoginState(
    val userName: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: LoginError? = null,
    val session: Session? = null,
    val requiresReentry: Boolean = false,
)

// Se recuerda la sesión (sin usuario ni contraseña) hasta que se cierre sesión.
class LoginViewModel(
    private val repository: AuthRepository,
    private val store: SessionStore = NoSessionStore,
) : ViewModel() {
    private var generation = 0L
    private var loginJob: Job? = null
    var state by mutableStateOf(LoginState(session = store.load()?.metadata()))
        private set

    fun updateUser(value: String) { state = state.copy(userName = value, error = null) }
    fun updatePassword(value: String) { state = state.copy(password = value, error = null) }

    fun submit() {
        val current = state
        if (current.loading) return
        if (current.userName.isBlank() || current.password.isEmpty()) {
            state = current.copy(error = LoginError.EMPTY)
            return
        }
        state = current.copy(loading = true, error = null)
        val requestGeneration = ++generation
        loginJob = viewModelScope.launch {
            val outcome = repository.login(current.userName, current.password)
            if (requestGeneration != generation) return@launch
            state = when (outcome) {
                is LoginOutcome.Success -> {
                    if (current.requiresReentry && outcome.session.accessToken == null) {
                        current.copy(password = "", loading = false, error = LoginError.REPORT_SESSION_REQUIRED)
                    } else {
                        store.save(outcome.session.metadata())
                        LoginState(session = outcome.session)
                    }
                }
                LoginOutcome.BadCredentials -> state.copy(loading = false, password = "", error = LoginError.BAD_CREDENTIALS)
                LoginOutcome.Unreachable -> state.copy(loading = false, password = "", error = LoginError.UNREACHABLE)
            }
        }
    }

    fun logout() {
        val previous = state.session
        generation++
        loginJob?.cancel()
        store.save(null)
        state = LoginState()
        if (previous?.accessToken != null) viewModelScope.launch { repository.logout(previous) }
    }

    fun requestReportReentry() {
        generation++
        loginJob?.cancel()
        state = LoginState(session = state.session?.metadata(), requiresReentry = true)
    }

    fun cancelReentry() {
        generation++
        loginJob?.cancel()
        state = LoginState(session = state.session?.metadata())
    }

    fun reportForbidden() {
        val metadata = state.session?.metadata()?.copy(isAdmin = false)
        store.save(metadata)
        state = LoginState(session = metadata)
    }
}

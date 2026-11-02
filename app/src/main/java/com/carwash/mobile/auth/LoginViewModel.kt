package com.carwash.mobile.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

enum class LoginError { EMPTY, BAD_CREDENTIALS, UNREACHABLE }

data class LoginState(
    val userName: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: LoginError? = null,
    val session: Session? = null,
)

// La sesión vive solo en memoria: no se persiste usuario, contraseña ni sesión.
class LoginViewModel(private val repository: AuthRepository) : ViewModel() {
    var state by mutableStateOf(LoginState())
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
        viewModelScope.launch {
            state = when (val outcome = repository.login(current.userName, current.password)) {
                is LoginOutcome.Success -> LoginState(session = outcome.session)
                LoginOutcome.BadCredentials -> state.copy(loading = false, password = "", error = LoginError.BAD_CREDENTIALS)
                LoginOutcome.Unreachable -> state.copy(loading = false, error = LoginError.UNREACHABLE)
            }
        }
    }

    fun logout() { state = LoginState() }
}

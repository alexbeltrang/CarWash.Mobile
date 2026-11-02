package com.carwash.mobile.auth

import com.carwash.mobile.asignacion.buildRetrofit
import retrofit2.http.Body
import retrofit2.http.POST
import java.io.IOException

data class Session(val userId: Int, val displayName: String, val isAdmin: Boolean, val baseUrl: String)

sealed interface LoginOutcome {
    data class Success(val session: Session) : LoginOutcome
    data object BadCredentials : LoginOutcome
    data object Unreachable : LoginOutcome
}

interface AuthRepository {
    suspend fun login(userName: String, password: String): LoginOutcome
}

data class LoginRequest(val userName: String, val password: String)
data class LoginUser(val idUser: Int = 0, val displayName: String? = null, val perfilId: Int = 0, val esAdministrador: Boolean = false)
data class LoginResponse(val esValido: Boolean = false, val usuario: LoginUser? = null)

interface AuthApi {
    @POST("api/Auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
}

// Prueba el servidor principal y luego el alterno; usuario y contraseña se cifran como en el WinForm y no se guardan.
class ApiAuthRepository(
    private val urls: () -> List<String>,
    private val encoder: CredentialEncoder = CredentialCipher,
    private val apiFor: (String) -> AuthApi = { buildRetrofit(it).create(AuthApi::class.java) },
) : AuthRepository {
    override suspend fun login(userName: String, password: String): LoginOutcome {
        val request = LoginRequest(encoder.encode(userName.trim()), encoder.encode(password))
        for (url in urls().filter { it.isNotBlank() }.distinct()) {
            try {
                val response = apiFor(url).login(request)
                val user = response.usuario
                return if (response.esValido && user != null) {
                    LoginOutcome.Success(Session(user.idUser, user.displayName.orEmpty(), user.esAdministrador, url))
                } else {
                    LoginOutcome.BadCredentials
                }
            } catch (_: IOException) {
                continue
            } catch (_: retrofit2.HttpException) {
                continue
            }
        }
        return LoginOutcome.Unreachable
    }
}

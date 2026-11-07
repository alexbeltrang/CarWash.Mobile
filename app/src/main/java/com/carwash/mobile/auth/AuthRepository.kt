package com.carwash.mobile.auth

import com.carwash.mobile.asignacion.buildRetrofit
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Header
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.util.Base64

data class Session(
    val userId: Int,
    val displayName: String,
    val isAdmin: Boolean,
    val baseUrl: String,
    val accessToken: String? = null,
    val expiresAt: String? = null,
    val serverDate: String? = null,
    val idleTimeoutSeconds: Int? = null,
    val localExpiresAtMillis: Long? = null,
) {
    fun metadata() = copy(accessToken = null, expiresAt = null, serverDate = null, idleTimeoutSeconds = null, localExpiresAtMillis = null)
    override fun toString() = "Session(userId=$userId, isAdmin=$isAdmin, baseUrl=$baseUrl)"
}

sealed interface LoginOutcome {
    data class Success(val session: Session) : LoginOutcome
    data object BadCredentials : LoginOutcome
    data object Unreachable : LoginOutcome
}

interface AuthRepository {
    suspend fun login(userName: String, password: String): LoginOutcome
    suspend fun logout(session: Session): Boolean = false
}

data class LoginRequest(val userName: String, val password: String)
data class LoginUser(val idUser: Int = 0, val displayName: String? = null, val perfilId: Int = 0, val esAdministrador: Boolean = false)
data class LoginResponse(
    val esValido: Boolean = false,
    val usuario: LoginUser? = null,
    val accessToken: String? = null,
    val tokenType: String? = null,
    val expiresAt: String? = null,
    val idleTimeoutSeconds: Int? = null,
)

interface AuthApi {
    @POST("api/Auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/Auth/logout")
    suspend fun logout(@Header("Authorization") authorization: String): Response<Unit>
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
                if (!response.isSuccessful) continue
                val body = response.body() ?: continue
                val user = body.usuario
                return if (body.esValido && user != null) {
                    val token = body.accessToken?.takeIf { value ->
                        body.tokenType.equals("Bearer", ignoreCase = true) && validToken(value) &&
                            runCatching { Instant.parse(body.expiresAt) }.isSuccess
                    }
                    val serverDate = response.headers()["X-Server-Date"]?.takeIf { value ->
                        runCatching { LocalDate.parse(value) }.isSuccess
                    }
                    LoginOutcome.Success(Session(user.idUser, user.displayName.orEmpty(), user.esAdministrador, url,
                        token, body.expiresAt?.takeIf { token != null }, serverDate, body.idleTimeoutSeconds))
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

    override suspend fun logout(session: Session): Boolean {
        val token = session.accessToken ?: return false
        return try {
            apiFor(session.baseUrl).logout("Bearer $token").code() == 204
        } catch (_: IOException) {
            false
        } catch (_: retrofit2.HttpException) {
            false
        }
    }

    private fun validToken(value: String): Boolean = value.length == 43 && runCatching {
        val bytes = Base64.getUrlDecoder().decode(value)
        bytes.size == 32 && Base64.getUrlEncoder().withoutPadding().encodeToString(bytes) == value
    }.getOrDefault(false)
}

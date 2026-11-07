package com.carwash.mobile.empresa

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carwash.mobile.asignacion.buildRetrofit
import com.carwash.mobile.auth.Session
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import java.io.IOException

data class EmpresaDto(
    val revision: Int = 0,
    val nombreComercial: String = "",
    val razonSocial: String = "",
    val nit: String = "",
    val digitoVerificacion: String = "",
    val direccion: String = "",
    val municipio: String = "",
    val departamento: String = "",
    val pais: String = "",
    val telefono: String = "",
    val correo: String = "",
    val responsabilidadesTributarias: String = "",
    val logoBase64: String? = null,
)

interface EmpresaRepository { suspend fun get(): EmpresaDto? }
fun empresaReceiptHeader(company: EmpresaDto?, nitLabel: String, phoneLabel: String): String = company?.let {
    listOf(it.nombreComercial, it.razonSocial, "$nitLabel: ${it.nit}-${it.digitoVerificacion}", it.direccion,
        listOf(it.municipio, it.departamento, it.pais).filter(String::isNotBlank).joinToString(" / "),
        "$phoneLabel: ${it.telefono}", it.correo, it.responsabilidadesTributarias)
        .filter(String::isNotBlank).joinToString("\n")
}.orEmpty()

interface EmpresaApi {
    @GET("api/Empresa")
    suspend fun get(@Header("Authorization") authorization: String): Response<EmpresaDto>
}
class EmpresaFailure(val sessionRequired: Boolean = false) : IOException("EMPRESA_UNAVAILABLE")

class ApiEmpresaRepository(session: Session) : EmpresaRepository {
    private val api = buildRetrofit(session.baseUrl).create(EmpresaApi::class.java)
    private val token = session.accessToken
    override suspend fun get(): EmpresaDto? {
        val response = api.get("Bearer ${token?.takeIf { it.isNotBlank() } ?: throw EmpresaFailure(true)}")
        if (response.code() == 204) return null
        if (!response.isSuccessful) throw EmpresaFailure(response.code() == 401)
        return response.body()?.takeIf { it.revision > 0 && it.nombreComercial.isNotBlank()
            && it.nombreComercial.length <= 120 && (it.logoBase64?.length ?: 0) <= 1_400_000 }
            ?: throw EmpresaFailure()
    }
}

class EmpresaViewModel(private val repository: EmpresaRepository) : ViewModel() {
    var empresa by mutableStateOf<EmpresaDto?>(null)
        private set
    var sessionRequired by mutableStateOf(false)
        private set
    private var request: Job? = null

    fun refresh() {
        request?.cancel()
        request = viewModelScope.launch {
            try {
                val current = repository.get()
                ensureActive()
                empresa = current
                sessionRequired = false
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                empresa = null
                sessionRequired = (error as? EmpresaFailure)?.sessionRequired == true
            }
        }
    }

    fun clear() { request?.cancel(); empresa = null; sessionRequired = false }
}
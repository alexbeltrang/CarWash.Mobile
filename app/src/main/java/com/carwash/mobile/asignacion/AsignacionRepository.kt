package com.carwash.mobile.asignacion

import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import java.io.IOException
import java.util.concurrent.TimeUnit

fun buildRetrofit(url: String): Retrofit {
    val client = OkHttpClient.Builder()
        .retryOnConnectionFailure(false)
        .followRedirects(false)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()
    return Retrofit.Builder().baseUrl(url).client(client).addConverterFactory(GsonConverterFactory.create()).build()
}

data class VehicleInProcess(
    val idTurno: Int = 0,
    val placa: String? = null,
    val marca: String? = null,
    val nombreCliente: String? = null,
    val numeroTurno: String? = null,
    val tipoVehiculo: String? = null,
    val operadorAsignado: String? = null,
    val idOperario: Int? = null,
) {
    val operatorName: String? get() = operadorAsignado?.trim()?.takeIf { idOperario != null && it.isNotEmpty() }
}

data class Operator(val idOperario: Int = 0, val nombres: String? = null, val apellidos: String? = null) {
    val fullName: String get() = listOfNotNull(nombres, apellidos).joinToString(" ").trim()
}

data class AssignRequest(val idOperario: Int, val idUsuario: Int)

interface AsignacionApi {
    @GET("api/Turnos/VehiculosEnProceso")
    suspend fun vehicles(): List<VehicleInProcess>

    @GET("api/Operarios/OperadoresDisponibles")
    suspend fun operators(): List<Operator>

    @PUT("api/Turnos/{id}/Operario")
    suspend fun assign(@Path("id") turnId: Int, @Body request: AssignRequest): Response<Unit>
}

enum class AssignResult { OK, FORBIDDEN, NOT_FOUND, CONFLICT, ERROR }

interface AsignacionRepository {
    suspend fun vehicles(): List<VehicleInProcess>
    suspend fun availableOperators(): List<Operator>
    suspend fun assign(turnId: Int, operatorId: Int, userId: Int): AssignResult
}

class ApiAsignacionRepository(private val api: AsignacionApi) : AsignacionRepository {
    override suspend fun vehicles() = api.vehicles()
    override suspend fun availableOperators() = api.operators()

    override suspend fun assign(turnId: Int, operatorId: Int, userId: Int): AssignResult {
        val response = try {
            api.assign(turnId, AssignRequest(operatorId, userId))
        } catch (_: IOException) {
            return AssignResult.ERROR
        }
        return when {
            response.isSuccessful -> AssignResult.OK
            response.code() == 403 -> AssignResult.FORBIDDEN
            response.code() == 404 -> AssignResult.NOT_FOUND
            response.code() == 409 -> AssignResult.CONFLICT
            else -> AssignResult.ERROR
        }
    }

    companion object {
        fun create(url: String) = ApiAsignacionRepository(buildRetrofit(url).create(AsignacionApi::class.java))
    }
}

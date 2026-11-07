package com.carwash.mobile.retiro

import com.carwash.mobile.asignacion.buildRetrofit
import com.carwash.mobile.auth.Session
import com.google.gson.JsonParser
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.io.IOException
import java.math.BigDecimal
import java.util.UUID

data class ReceiptService(val nombre: String, val precio: BigDecimal)
data class RetiroReceipt(
    val idTurno: Int, val codigo: String, val placa: String, val marca: String?,
    val cliente: String?, val fechaIngreso: String, val total: BigDecimal,
    val servicios: List<ReceiptService>, val entregadoEn: String? = null, val entregadoPor: Int? = null,
) {
    override fun toString() = "RetiroReceipt(idTurno=$idTurno, delivered=${entregadoEn != null})"
}
data class IssueReceipt(val idTurno: Int? = null, val placa: String? = null)
data class ConfirmPickup(val revisionConfirmada: Boolean)
data class PrintRequest(val solicitudId: String, val anchoMm: Int)
data class EntryPrintRequest(val idServicios: List<Int>)
data class PrintJob(val solicitudId: String, val idTurno: Int, val anchoMm: Int, val impresora: String,
    val estado: String, val creadoEn: String, val actualizadoEn: String, val errorCodigo: String? = null)
enum class RetiroError { SESSION_REQUIRED, NOT_FOUND, INVALID_CODE, ALREADY_DELIVERED, INVALID_INPUT, UNAVAILABLE }
class RetiroFailure(val kind: RetiroError) : IOException(kind.name)

interface RetiroRepository {
    suspend fun issue(request: IssueReceipt): RetiroReceipt
    suspend fun get(idTurno: Int): RetiroReceipt
    suspend fun confirm(idTurno: Int): RetiroReceipt
    suspend fun requestPrint(idTurno: Int, requestId: String, widthMm: Int): PrintJob = throw RetiroFailure(RetiroError.UNAVAILABLE)
    suspend fun getPrint(requestId: String): PrintJob = throw RetiroFailure(RetiroError.UNAVAILABLE)
    suspend fun latestPrint(idTurno: Int): PrintJob? = null
    suspend fun printEntry(idTurno: Int, serviceIds: Set<Int>): PrintJob = throw RetiroFailure(RetiroError.UNAVAILABLE)
}

interface RetiroApi {
    @POST("api/RetiroVehiculos/comprobante")
    suspend fun issue(@Header("Authorization") authorization: String, @Body request: IssueReceipt): Response<RetiroReceipt>
    @GET("api/RetiroVehiculos/{id}")
    suspend fun get(@Header("Authorization") authorization: String, @Path("id") id: Int): Response<RetiroReceipt>
    @POST("api/RetiroVehiculos/{id}/entrega")
    suspend fun confirm(@Header("Authorization") authorization: String, @Path("id") id: Int, @Body request: ConfirmPickup): Response<RetiroReceipt>
    @POST("api/RetiroVehiculos/{id}/impresiones")
    suspend fun print(@Header("Authorization") authorization: String, @Path("id") id: Int, @Body request: PrintRequest): Response<PrintJob>
    @GET("api/RetiroVehiculos/impresiones/{id}")
    suspend fun getPrint(@Header("Authorization") authorization: String, @Path("id") id: String): Response<PrintJob>
    @GET("api/RetiroVehiculos/{id}/impresiones/ultima")
    suspend fun latestPrint(@Header("Authorization") authorization: String, @Path("id") id: Int): Response<PrintJob>
    @POST("api/RetiroVehiculos/{id}/impresiones/ingreso")
    suspend fun printEntry(@Header("Authorization") authorization: String, @Path("id") id: Int, @Body request: EntryPrintRequest): Response<PrintJob>
}

class ApiRetiroRepository(private val session: Session) : RetiroRepository {
    private val api = buildRetrofit(session.baseUrl).create(RetiroApi::class.java)
    private fun authorization() = "Bearer ${session.accessToken?.takeIf { it.isNotBlank() }
        ?: throw RetiroFailure(RetiroError.SESSION_REQUIRED)}"
    override suspend fun issue(request: IssueReceipt) = checked(api.issue(authorization(), request)).also {
        if (request.idTurno != null && request.idTurno != it.idTurno || request.placa != null && request.placa.trim().uppercase() != it.placa.trim().uppercase())
            throw RetiroFailure(RetiroError.UNAVAILABLE)
    }
    override suspend fun get(idTurno: Int) = checked(api.get(authorization(), idTurno)).also {
        if (it.idTurno != idTurno) throw RetiroFailure(RetiroError.UNAVAILABLE)
    }
    override suspend fun confirm(idTurno: Int) = checked(api.confirm(authorization(), idTurno, ConfirmPickup(true))).also {
        if (it.idTurno != idTurno || it.entregadoEn == null) throw RetiroFailure(RetiroError.UNAVAILABLE)
    }
    override suspend fun requestPrint(idTurno: Int, requestId: String, widthMm: Int) = checkedPrint(api.print(authorization(), idTurno, PrintRequest(requestId, widthMm))).also {
        if (it.idTurno != idTurno || it.anchoMm != widthMm || !it.solicitudId.equals(requestId, true)) throw RetiroFailure(RetiroError.UNAVAILABLE)
    }
    override suspend fun getPrint(requestId: String) = checkedPrint(api.getPrint(authorization(), requestId)).also {
        if (!it.solicitudId.equals(requestId, true)) throw RetiroFailure(RetiroError.UNAVAILABLE)
    }
    override suspend fun latestPrint(idTurno: Int): PrintJob? {
        val response = api.latestPrint(authorization(), idTurno)
        if (response.code() == 204) return null
        return checkedPrint(response).also { if (it.idTurno != idTurno) throw RetiroFailure(RetiroError.UNAVAILABLE) }
    }
    override suspend fun printEntry(idTurno: Int, serviceIds: Set<Int>) = checkedPrint(api.printEntry(authorization(), idTurno, EntryPrintRequest(serviceIds.sorted()))).also {
        if (it.idTurno != idTurno || it.anchoMm != 80) throw RetiroFailure(RetiroError.UNAVAILABLE)
    }
    private fun checkedPrint(response: Response<PrintJob>): PrintJob {
        checkFailure(response)
        return response.body()?.takeIf { it.idTurno > 0 && it.anchoMm in listOf(58, 80) && it.impresora.isNotBlank()
            && runCatching { UUID.fromString(it.solicitudId).toString() == it.solicitudId.lowercase() }.getOrDefault(false)
            && it.estado in listOf("PENDING", "SUBMITTING", "SUBMITTED", "FAILED", "UNKNOWN") } ?: throw RetiroFailure(RetiroError.UNAVAILABLE)
    }
    private fun checked(response: Response<RetiroReceipt>): RetiroReceipt {
        checkFailure(response)
        return response.body()?.takeIf { it.idTurno > 0 && it.codigo.matches(Regex("[0-9A-F]{16}"))
            && it.placa.isNotBlank() && it.servicios.isNotEmpty() } ?: throw RetiroFailure(RetiroError.UNAVAILABLE)
    }
    private fun checkFailure(response: Response<*>) {
        if (!response.isSuccessful) {
            val code = runCatching { response.errorBody()?.use { JsonParser.parseString(it.string()).asJsonObject.get("code")?.asString } }.getOrNull()
            throw RetiroFailure(when {
                response.code() == 401 -> RetiroError.SESSION_REQUIRED
                response.code() == 404 -> RetiroError.NOT_FOUND
                code == "INVALID_CODE" -> RetiroError.INVALID_CODE
                code == "ALREADY_DELIVERED" -> RetiroError.ALREADY_DELIVERED
                response.code() == 400 || response.code() == 409 -> RetiroError.INVALID_INPUT
                else -> RetiroError.UNAVAILABLE
            })
        }
    }
}
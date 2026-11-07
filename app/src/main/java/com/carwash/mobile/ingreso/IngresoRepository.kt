package com.carwash.mobile.ingreso

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

const val HISTORY_LIMIT = 5

interface CarWashApi {
    @GET("api/TipoVehiculo/GetAll")
    suspend fun types(): List<VehicleType>

    @GET("api/Servicios/GetListaCompletaServicios")
    suspend fun services(): List<VehicleService>

    @GET("api/CajaDiaria/getCajaActiva")
    suspend fun cash(): Response<CashRegister>

    @GET("api/Turnos/BuscarPorPlaca")
    suspend fun vehicle(@Query("placa") plate: String): Response<VehicleInfo>

    @GET("api/Turnos/HistorialPorPlaca")
    suspend fun history(@Query("placa") plate: String): Response<List<HistoryEntry>>

    @GET("api/Turnos/EnProcesoPorPlaca")
    suspend fun inProcess(@Query("placa") plate: String): Response<InProcessTurn>

    @PUT("api/Turnos/{idTurno}/EnProceso")
    suspend fun updateInProcess(@Path("idTurno") idTurno: Int, @Body update: InProcessUpdate): Response<Unit>

    @GET("api/TurnosDiarios/ByFecha")
    suspend fun daily(@Query("fecha") date: String? = null): Response<List<DailyTurn>>

    @POST("api/TurnosDiarios/RegistrarTurnoDiario")
    suspend fun createDaily(@Body daily: DailyTurn): DailyTurn

    @PUT("api/TurnosDiarios/ActualizarTurnoDiario/{id}")
    suspend fun updateDaily(@Path("id") id: Int, @Body daily: DailyTurn): Response<Unit>

    @POST("api/Turnos/RegistrarTurno")
    suspend fun createTurn(@Body turn: TurnRequest): SavedTurn

    @POST("api/TurnoServicios/RegistrarTurnoServicio")
    suspend fun createTurnService(@Body service: TurnServiceRequest): Response<Unit>

    @POST("api/Turnos/{idTurno}/notificacion-email")
    suspend fun notifyEmail(@Path("idTurno") idTurno: Int): Response<EmailNotificationResult>

    @GET("api/Turnos/{idTurno}/notificacion-email")
    suspend fun emailStatus(@Path("idTurno") idTurno: Int): Response<EmailNotificationResult>

    @POST("api/Turnos/{idTurno}/notificacion-email/reintentar")
    suspend fun retryEmail(@Path("idTurno") idTurno: Int): Response<EmailNotificationResult>

    @POST("api/Turnos/{idTurno}/notificacion-whatsapp")
    suspend fun notifyWhatsApp(@Path("idTurno") idTurno: Int): Response<WhatsAppNotificationResult>

    @POST("api/Turnos/{idTurno}/notificacion-whatsapp/reintentar")
    suspend fun retryWhatsApp(@Path("idTurno") idTurno: Int): Response<WhatsAppNotificationResult>
}

interface IngresoRepository {
    suspend fun load(): Catalog
    suspend fun lookup(plate: String): PlateLookup
    suspend fun register(form: IngresoForm, beforeWrite: (String) -> Unit): SavedTurn
    suspend fun updateInProcess(idTurno: Int, form: IngresoForm) {
        throw UnsupportedOperationException()
    }
    suspend fun notifyEmail(idTurno: Int): EmailNotificationResult? = null
    suspend fun retryEmail(idTurno: Int): EmailNotificationResult? = null
    suspend fun notifyWhatsApp(idTurno: Int): WhatsAppNotificationResult? = null
    suspend fun retryWhatsApp(idTurno: Int): WhatsAppNotificationResult? = null
}

class CashClosedException : IOException()
enum class RegistrationStage { DAILY, TURN, SERVICES }
class RegistrationException(
    val uncertain: Boolean,
    val turnNumber: String?,
    cause: Exception,
    val stage: RegistrationStage? = null,
) : IOException(cause)

class ApiIngresoRepository(
    private val api: CarWashApi,
    private val now: () -> LocalDateTime = LocalDateTime::now,
) : IngresoRepository {
    override suspend fun load(): Catalog = Catalog(
        types = api.types().filterNot { it.isDelete },
        services = api.services(),
        cash = api.cash().optional()?.takeIf { it.estado && it.idCaja > 0 },
    )

    override suspend fun lookup(plate: String): PlateLookup = PlateLookup(
        vehicle = api.vehicle(plate).optional(),
        history = api.history(plate).optional().orEmpty().take(HISTORY_LIMIT),
        inProcess = api.inProcess(plate).optional(),
    )

    override suspend fun updateInProcess(idTurno: Int, form: IngresoForm) {
        api.updateInProcess(
            idTurno,
            InProcessUpdate(
                nombreCliente = form.customer.trim().uppercase(Locale.ROOT),
                numeroCelular = form.phone.trim(),
                correoElectronico = form.email.trim(),
                marca = form.brand.trim().uppercase(Locale.ROOT),
                observaciones = form.observations.trim().uppercase(Locale.ROOT),
                idTipoVehiculo = requireNotNull(form.typeId),
                valor = requireNotNull(parseAmount(form.value)),
                valorBaseComision = requireNotNull(parseAmount(form.baseValue)),
                idServicios = form.serviceIds.toList(),
            ),
        ).requireSuccess()
    }

    override suspend fun retryEmail(idTurno: Int): EmailNotificationResult? =
        api.retryEmail(idTurno).optional()

    override suspend fun notifyEmail(idTurno: Int): EmailNotificationResult? =
        api.notifyEmail(idTurno).optional()

    override suspend fun notifyWhatsApp(idTurno: Int): WhatsAppNotificationResult? =
        api.notifyWhatsApp(idTurno).optional()

    override suspend fun retryWhatsApp(idTurno: Int): WhatsAppNotificationResult? =
        api.retryWhatsApp(idTurno).optional()

    override suspend fun register(form: IngresoForm, beforeWrite: (String) -> Unit): SavedTurn {
        require(form.errors().isEmpty())
        var started = false
        var number: String? = null
        var stage = RegistrationStage.DAILY
        try {
            val cash = api.cash().optional()?.takeIf { it.estado && it.idCaja > 0 }
                ?: throw CashClosedException()
            val time = now()
            val dailyResponse = api.daily().requireBody()
            val date = dailyResponse.headers()["X-Server-Date"]
                ?.let { LocalDate.parse(it) }
                ?: time.toLocalDate()
            val existing = dailyResponse.body().orEmpty().firstOrNull()
            val next = Math.addExact(existing?.consecutivo ?: 0, 1)
            number = "T-${date.format(DateTimeFormatter.BASIC_ISO_DATE)}-${String.format(Locale.ROOT, "%03d", next)}"
            val daily = DailyTurn(existing?.id ?: 0, date.atStartOfDay().toString(), next)
            beforeWrite(number)
            started = true
            if (existing == null) api.createDaily(daily) else api.updateDaily(existing.id, daily).requireSuccess()
            stage = RegistrationStage.TURN
            val saved = api.createTurn(
                TurnRequest(
                    numeroTurno = number,
                    nombreCliente = form.customer.trim().uppercase(Locale.ROOT),
                    numeroCelular = form.phone.trim(),
                    correoElectronico = form.email.trim(),
                    placa = form.plate.trim().uppercase(Locale.ROOT),
                    fechaHoraIngreso = time.toString(),
                    marca = form.brand.trim().uppercase(Locale.ROOT),
                    numeroOrden = "0",
                    valor = requireNotNull(parseAmount(form.value)),
                    valorBaseComision = requireNotNull(parseAmount(form.baseValue)),
                    observaciones = form.observations.trim().uppercase(Locale.ROOT),
                    idTipoVehiculo = requireNotNull(form.typeId),
                    idCajaDiaria = cash.idCaja,
                ),
            )
            check(saved.idTurno > 0)
            stage = RegistrationStage.SERVICES
            for (serviceId in form.serviceIds) {
                api.createTurnService(TurnServiceRequest(serviceId, saved.idTurno)).requireSuccess()
            }
            return saved
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (closed: CashClosedException) {
            throw closed
        } catch (failure: Exception) {
            if (started && number != null) {
                confirmedRegistration(form, number)?.let { return it }
            }
            throw RegistrationException(started, number, failure, stage)
        }
    }

    private suspend fun confirmedRegistration(form: IngresoForm, number: String): SavedTurn? {
        return try {
            val turn = api.inProcess(form.plate.trim().uppercase(Locale.ROOT)).optional() ?: return null
            val matches = turn.idTurno > 0 && turn.numeroTurno == number &&
                turn.idTipoVehiculo == form.typeId && turn.idServicios.toSet() == form.serviceIds &&
                turn.nombreCliente.orEmpty() == form.customer.trim().uppercase(Locale.ROOT) &&
                turn.numeroCelular.orEmpty() == form.phone.trim() &&
                turn.correoElectronico.orEmpty() == form.email.trim() &&
                turn.marca.orEmpty() == form.brand.trim().uppercase(Locale.ROOT) &&
                turn.observaciones.orEmpty() == form.observations.trim().uppercase(Locale.ROOT) &&
                turn.valor.compareTo(requireNotNull(parseAmount(form.value))) == 0 &&
                turn.valorBaseComision.compareTo(requireNotNull(parseAmount(form.baseValue))) == 0
            if (matches) SavedTurn(turn.idTurno, number) else null
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        fun create(url: String): ApiIngresoRepository {
            val client = OkHttpClient.Builder()
                .retryOnConnectionFailure(false)
                .followRedirects(false)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .build()
            val api = Retrofit.Builder().baseUrl(url).client(client)
                .addConverterFactory(GsonConverterFactory.create()).build().create(CarWashApi::class.java)
            return ApiIngresoRepository(api)
        }
    }
}

enum class ProbeResult { OK, TIMEOUT, REFUSED, UNREACHABLE, API_ERROR }

// Solo lectura: consulta un catalogo existente para distinguir red, puerto y API.
suspend fun probeServer(url: String): ProbeResult {
    val client = OkHttpClient.Builder()
        .retryOnConnectionFailure(false)
        .followRedirects(false)
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()
    val request = okhttp3.Request.Builder().url(url + "api/TipoVehiculo/GetAll").get().build()
    return try {
        runInterruptible(Dispatchers.IO) {
            client.newCall(request).execute().use { if (it.isSuccessful) ProbeResult.OK else ProbeResult.API_ERROR }
        }
    } catch (_: java.net.SocketTimeoutException) {
        ProbeResult.TIMEOUT
    } catch (failure: java.net.ConnectException) {
        val text = generateSequence<Throwable>(failure) { it.cause }.joinToString(" ") { it.message.orEmpty() }
        if (text.contains("refused", ignoreCase = true) || text.contains("ECONNREFUSED")) ProbeResult.REFUSED else ProbeResult.UNREACHABLE
    } catch (_: IOException) {
        ProbeResult.UNREACHABLE
    }
}

private fun <Value> Response<Value>.optional(): Value? = when {
    code() == 404 -> null
    isSuccessful -> body()
    else -> throw retrofit2.HttpException(this)
}

private fun Response<*>.requireSuccess() {
    if (!isSuccessful) throw retrofit2.HttpException(this)
}

private fun <Value> Response<Value>.requireBody(): Response<Value> {
    if (!isSuccessful) throw retrofit2.HttpException(this)
    return this
}
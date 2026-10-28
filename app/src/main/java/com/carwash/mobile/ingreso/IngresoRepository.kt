package com.carwash.mobile.ingreso

import kotlinx.coroutines.CancellationException
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

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

    @GET("api/TurnosDiarios/ByFecha")
    suspend fun daily(@Query("fecha") date: String): List<DailyTurn>

    @POST("api/TurnosDiarios/RegistrarTurnoDiario")
    suspend fun createDaily(@Body daily: DailyTurn): DailyTurn

    @PUT("api/TurnosDiarios/ActualizarTurnoDiario/{id}")
    suspend fun updateDaily(@Path("id") id: Int, @Body daily: DailyTurn): Response<Unit>

    @POST("api/Turnos/RegistrarTurno")
    suspend fun createTurn(@Body turn: TurnRequest): SavedTurn

    @POST("api/TurnoServicios/RegistrarTurnoServicio")
    suspend fun createTurnService(@Body service: TurnServiceRequest): Response<Unit>
}

interface IngresoRepository {
    suspend fun load(): Catalog
    suspend fun lookup(plate: String): PlateLookup
    suspend fun register(form: IngresoForm, beforeWrite: (String) -> Unit): SavedTurn
}

class CashClosedException : IOException()
class RegistrationException(val uncertain: Boolean, val turnNumber: String?, cause: Exception) : IOException(cause)

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
        history = api.history(plate).optional().orEmpty(),
    )

    override suspend fun register(form: IngresoForm, beforeWrite: (String) -> Unit): SavedTurn {
        require(form.errors().isEmpty())
        var started = false
        var number: String? = null
        try {
            val cash = api.cash().optional()?.takeIf { it.estado && it.idCaja > 0 }
                ?: throw CashClosedException()
            val time = now()
            val date = time.toLocalDate()
            val existing = api.daily(date.toString()).firstOrNull()
            val next = Math.addExact(existing?.consecutivo ?: 0, 1)
            number = "T-${date.format(DateTimeFormatter.BASIC_ISO_DATE)}-${String.format(Locale.ROOT, "%03d", next)}"
            val daily = DailyTurn(existing?.id ?: 0, date.atStartOfDay().toString(), next)
            beforeWrite(number)
            started = true
            if (existing == null) api.createDaily(daily) else api.updateDaily(existing.id, daily).requireSuccess()
            val saved = api.createTurn(
                TurnRequest(
                    numeroTurno = number,
                    nombreCliente = form.customer.trim().uppercase(Locale.ROOT),
                    numeroCelular = form.phone.trim(),
                    placa = form.plate.trim().uppercase(Locale.ROOT),
                    fechaHoraIngreso = time.toString(),
                    marca = form.brand.trim().uppercase(Locale.ROOT),
                    numeroOrden = form.order.trim(),
                    valor = requireNotNull(parseAmount(form.value)),
                    valorBaseComision = requireNotNull(parseAmount(form.baseValue)),
                    observaciones = form.observations.trim().uppercase(Locale.ROOT),
                    idTipoVehiculo = requireNotNull(form.typeId),
                    idCajaDiaria = cash.idCaja,
                ),
            )
            check(saved.idTurno > 0)
            for (serviceId in form.serviceIds) {
                api.createTurnService(TurnServiceRequest(serviceId, saved.idTurno)).requireSuccess()
            }
            return saved
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (closed: CashClosedException) {
            throw closed
        } catch (failure: Exception) {
            throw RegistrationException(started, number, failure)
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

private fun <Value> Response<Value>.optional(): Value? = when {
    code() == 404 -> null
    isSuccessful -> body()
    else -> throw retrofit2.HttpException(this)
}

private fun Response<*>.requireSuccess() {
    if (!isSuccessful) throw retrofit2.HttpException(this)
}
package com.carwash.mobile.reportes

import com.carwash.mobile.auth.Session
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.stream.MalformedJsonException
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.QueryMap
import java.io.IOException
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.DateTimeException
import java.util.Base64
import java.util.concurrent.TimeUnit

interface ReporteVentasRepository {
    suspend fun sales(criteria: ReportCriteria, cursor: String? = null): SalesPage
    suspend fun options(initialDate: LocalDate, finalDate: LocalDate): ReportOptions = throw ReportError(ReportErrorKind.UNAVAILABLE)
}

class ApiReporteVentasRepository(private val session: Session) : ReporteVentasRepository {
    private val api by lazy {
        Retrofit.Builder().baseUrl(session.baseUrl)
            .client(OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
                .connectTimeout(5, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).build())
            .addConverterFactory(GsonConverterFactory.create()).build().create(ReportApi::class.java)
    }

    override suspend fun sales(criteria: ReportCriteria, cursor: String?): SalesPage {
        val normalized = criteria.normalized()
        val invalid = normalized.invalidFields()
        if (invalid.isNotEmpty()) throw ReportError(ReportErrorKind.INVALID_FILTERS, invalid)
        val authorization = authorization()
        val parameters = linkedMapOf("fechaInicial" to normalized.initialDate.toString(), "fechaFinal" to normalized.finalDate.toString())
        normalized.paymentMethodId?.let { parameters["idFormaPago"] = it.toString() }
        normalized.operatorId?.let { parameters["idOperario"] = it.toString() }
        normalized.plate?.let { parameters["placa"] = it }
        normalized.vehicleTypeId?.let { parameters["idTipoVehiculo"] = it.toString() }
        cursor?.let { parameters["cursor"] = it }
        return safely {
            val response = api.sales(authorization, parameters)
            if (!response.isSuccessful) throw problem(response)
            val result = parse(response.body() ?: throw ReportError(ReportErrorKind.INVALID_RESPONSE))
            if (result.criteria != normalized) throw ReportError(ReportErrorKind.INVALID_RESPONSE)
            response.headers()["X-Server-Date"]?.let {
                if (LocalDate.parse(it) != result.businessDate) throw ReportError(ReportErrorKind.INVALID_RESPONSE)
            }
            result
        }
    }

    override suspend fun options(initialDate: LocalDate, finalDate: LocalDate): ReportOptions {
        val invalid = ReportCriteria(initialDate, finalDate).invalidFields()
        if (invalid.isNotEmpty()) throw ReportError(ReportErrorKind.INVALID_FILTERS, invalid)
        return safely {
            val response = api.options(authorization(), mapOf("fechaInicial" to initialDate.toString(), "fechaFinal" to finalDate.toString()))
            if (!response.isSuccessful) throw problem(response)
            val body = response.body() ?: throw ReportError(ReportErrorKind.INVALID_RESPONSE)
            fun choices(name: String): List<FilterOption> {
                val values = body.getAsJsonArray(name)?.map { value ->
                    FilterOption(value.asJsonObject.id("id"), value.asJsonObject.optionalText("nombre"))
                } ?: throw ReportError(ReportErrorKind.INVALID_RESPONSE)
                if (values.distinctBy { it.id }.size != values.size) throw ReportError(ReportErrorKind.INVALID_RESPONSE)
                return values
            }
            val result = ReportOptions(LocalDate.parse(body.text("fechaInicial")), LocalDate.parse(body.text("fechaFinal")),
                choices("formasPago"), choices("operarios"), choices("tiposVehiculo"), LocalDate.parse(body.text("fechaOperativa")))
            if (result.initialDate != initialDate || result.finalDate != finalDate
                || response.headers()["X-Server-Date"]?.let { LocalDate.parse(it) != result.businessDate } == true)
                throw ReportError(ReportErrorKind.INVALID_RESPONSE)
            result
        }
    }

    private fun authorization(): String {
        if (!session.isAdmin) throw ReportError(ReportErrorKind.ADMIN_REQUIRED)
        val token = session.accessToken?.takeIf { value ->
            value.length == 43 && runCatching {
                val bytes = Base64.getUrlDecoder().decode(value)
                bytes.size == 32 && Base64.getUrlEncoder().withoutPadding().encodeToString(bytes) == value
            }.getOrDefault(false)
        } ?: throw ReportError(ReportErrorKind.SESSION_REQUIRED)
        return "Bearer $token"
    }

    private suspend fun <T> safely(action: suspend () -> T): T {
        try { return action() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: ReportError) { throw error }
        catch (_: MalformedJsonException) { throw ReportError(ReportErrorKind.INVALID_RESPONSE) }
        catch (_: JsonParseException) { throw ReportError(ReportErrorKind.INVALID_RESPONSE) }
        catch (_: IOException) { throw ReportError(ReportErrorKind.NETWORK) }
        catch (_: IllegalArgumentException) { throw ReportError(ReportErrorKind.INVALID_RESPONSE) }
        catch (_: IllegalStateException) { throw ReportError(ReportErrorKind.INVALID_RESPONSE) }
        catch (_: ArithmeticException) { throw ReportError(ReportErrorKind.INVALID_RESPONSE) }
        catch (_: DateTimeException) { throw ReportError(ReportErrorKind.INVALID_RESPONSE) }
        catch (_: ClassCastException) { throw ReportError(ReportErrorKind.INVALID_RESPONSE) }
    }

    private fun problem(response: Response<JsonObject>): ReportError {
        val body = runCatching { response.errorBody()?.use { JsonParser.parseString(it.string()).asJsonObject } }.getOrNull()
        val code = runCatching { body?.get("code")?.asString }.getOrNull()
        val kind = when (response.code()) {
            400 -> if (code == "INVALID_CURSOR") ReportErrorKind.INVALID_CURSOR else ReportErrorKind.INVALID_FILTERS
            401 -> ReportErrorKind.SESSION_REQUIRED
            403 -> ReportErrorKind.ADMIN_REQUIRED
            503 -> ReportErrorKind.UNAVAILABLE
            else -> ReportErrorKind.UNEXPECTED
        }
        val fields = runCatching { body?.getAsJsonObject("errors")?.keySet()?.toSet().orEmpty() }.getOrDefault(emptySet())
            .intersect(setOf("fechaInicial", "fechaFinal", "idFormaPago", "idOperario", "idTipoVehiculo", "placa", "pageSize", "cursor"))
        return ReportError(kind, fields)
    }

    private fun parse(body: JsonObject): SalesPage {
        val criteria = body.getAsJsonObject("criterios") ?: throw ReportError(ReportErrorKind.INVALID_RESPONSE)
        val applied = ReportCriteria(LocalDate.parse(criteria.text("fechaInicial")), LocalDate.parse(criteria.text("fechaFinal")),
            criteria.optionalId("idFormaPago"), criteria.optionalId("idOperario"), criteria.optionalText("placa"), criteria.optionalId("idTipoVehiculo"))
        val count = body.number("cantidadCobros").longValueExact()
        val total = body.money("importeTotal")
        val size = body.number("pageSize").intValueExact()
        val items = body.getAsJsonArray("items")?.map { value ->
            val item = value.asJsonObject
            CollectionItem(item.id("idMovimiento"), item.id("idTurno"), OffsetDateTime.parse(item.text("fechaCobro")),
                item.optionalText("placa"), item.optionalId("idTipoVehiculo"), item.optionalText("tipoVehiculo"),
                item.optionalId("idOperario"), item.optionalText("operario"), item.id("idFormaPago"), item.optionalText("formaPago"), item.money("montoPagado"))
        } ?: throw ReportError(ReportErrorKind.INVALID_RESPONSE)
        val summary = body.get("resumen")?.takeUnless { it.isJsonNull }?.asJsonArray?.map { value ->
            val row = value.asJsonObject
            PaymentSummary(row.id("idFormaPago"), row.optionalText("formaPago"), row.number("cantidad").longValueExact(), row.money("importe"))
        }.orEmpty()
        val next = body.optionalText("nextCursor")
        if (count < 0 || size !in 1..200 || items.size > size || items.size > count || items.distinctBy { it.id }.size != items.size
            || (next != null && (next.isBlank() || items.isEmpty())) || applied.invalidFields().isNotEmpty())
            throw ReportError(ReportErrorKind.INVALID_RESPONSE)
        return SalesPage(applied, count, total, items, next, size, Instant.parse(body.text("consultadoEn")), LocalDate.parse(body.text("fechaOperativa")), summary)
    }

    private fun JsonObject.text(name: String): String {
        val value = get(name)
        if (value == null || !value.isJsonPrimitive || !value.asJsonPrimitive.isString) throw ReportError(ReportErrorKind.INVALID_RESPONSE)
        return value.asString
    }

    private fun JsonObject.optionalText(name: String): String? = get(name)?.takeUnless { it.isJsonNull }?.let { text(name) }
    private fun JsonObject.optionalId(name: String): Int? = get(name)?.takeUnless { it.isJsonNull }?.let { id(name) }
    private fun JsonObject.id(name: String): Int = number(name).intValueExact().also { if (it <= 0) throw ReportError(ReportErrorKind.INVALID_RESPONSE) }
    private fun JsonObject.number(name: String): BigDecimal {
        val value = get(name)
        if (value == null || !value.isJsonPrimitive || !value.asJsonPrimitive.isNumber) throw ReportError(ReportErrorKind.INVALID_RESPONSE)
        return value.asBigDecimal
    }

    private fun JsonObject.money(name: String): BigDecimal {
        val value = text(name)
        if (!value.matches(Regex("-?[0-9]+(?:\\.[0-9]+)?"))) throw ReportError(ReportErrorKind.INVALID_RESPONSE)
        return value.toBigDecimal()
    }
}

private interface ReportApi {
    @GET("api/Reportes/Ventas")
    suspend fun sales(@Header("Authorization") authorization: String, @QueryMap parameters: Map<String, String>): Response<JsonObject>

    @GET("api/Reportes/Ventas/Filtros")
    suspend fun options(@Header("Authorization") authorization: String, @QueryMap parameters: Map<String, String>): Response<JsonObject>
}
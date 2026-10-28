package com.carwash.mobile.ingreso

import java.math.BigDecimal
import java.net.URI
import java.text.NumberFormat
import java.util.Locale

data class VehicleType(val idTipoVehiculo: Int, val nombre: String, val isDelete: Boolean = false)

data class VehicleService(
    val idServicio: Int,
    val nombre: String,
    val idTipoVehiculo: Int,
    val precio: BigDecimal?,
    val precioBaseComision: BigDecimal?,
)

data class CashRegister(val idCaja: Int, val estado: Boolean)
data class Catalog(val types: List<VehicleType>, val services: List<VehicleService>, val cash: CashRegister?)
data class VehicleInfo(
    val idTipoVehiculo: Int,
    val nombreCliente: String?,
    val numeroCelular: String?,
    val marca: String?,
)

data class HistoryEntry(
    val idTurno: Int,
    val fechaHoraIngreso: String?,
    val nombreCliente: String?,
    val placa: String?,
    val marca: String?,
    val tipoVehiculo: String?,
    val valorPagado: String?,
    val servicio: String?,
)

data class PlateLookup(val vehicle: VehicleInfo?, val history: List<HistoryEntry>)
data class DailyTurn(val id: Int = 0, val fecha: String, val consecutivo: Int)
data class SavedTurn(val idTurno: Int, val numeroTurno: String?)
data class TurnServiceRequest(val idServicios: Int, val idTurno: Int, val isDeleted: Boolean = false)

data class TurnRequest(
    val numeroTurno: String,
    val nombreCliente: String,
    val numeroCelular: String,
    val placa: String,
    val fechaHoraIngreso: String,
    val marca: String,
    val numeroOrden: String,
    val valor: BigDecimal,
    val valorBaseComision: BigDecimal,
    val observaciones: String,
    val idTipoVehiculo: Int,
    val idCajaDiaria: Int,
    val pagado: Boolean = false,
    val estado: Boolean = false,
    val operadorOcupado: Boolean = false,
)

data class IngresoForm(
    val plate: String = "",
    val customer: String = "",
    val phone: String = "",
    val brand: String = "",
    val order: String = "",
    val observations: String = "",
    val typeId: Int? = null,
    val serviceIds: Set<Int> = emptySet(),
    val value: String = "0.00",
    val baseValue: String = "0.00",
)

enum class FormError { PLATE, ORDER, TYPE, SERVICES, VALUE, BASE_VALUE }

fun parseAmount(text: String): BigDecimal? = text.trim()
    .takeIf { it.matches(Regex("[0-9]+([.,][0-9]{1,2})?")) }
    ?.replace(',', '.')?.toBigDecimalOrNull()

fun IngresoForm.errors(): Set<FormError> = buildSet {
    if (plate.isBlank()) add(FormError.PLATE)
    if (order.isBlank()) add(FormError.ORDER)
    if (typeId == null || typeId <= 0) add(FormError.TYPE)
    if (serviceIds.isEmpty()) add(FormError.SERVICES)
    if (parseAmount(value) == null) add(FormError.VALUE)
    if (parseAmount(baseValue) == null) add(FormError.BASE_VALUE)
}

fun IngresoForm.withServices(selected: Set<Int>, catalog: List<VehicleService>): IngresoForm {
    val services = catalog.filter { it.idTipoVehiculo == typeId && it.idServicio in selected }
    return copy(
        serviceIds = services.map { it.idServicio }.toSet(),
        value = services.fold(BigDecimal.ZERO) { total, service -> total + (service.precio ?: BigDecimal.ZERO) }
            .setScale(2).toPlainString(),
        baseValue = services.fold(BigDecimal.ZERO) { total, service -> total + (service.precioBaseComision ?: BigDecimal.ZERO) }
            .setScale(2).toPlainString(),
    )
}

fun money(amount: BigDecimal): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"))
    .format(amount)

fun normalizeServer(input: String): String? = runCatching {
    val uri = URI(input.trim())
    require(uri.scheme == "http" || uri.scheme == "https")
    require(!uri.host.isNullOrBlank() && uri.userInfo == null && uri.query == null && uri.fragment == null)
    require(uri.path.isNullOrBlank() || uri.path == "/")
    require(uri.port == -1 || uri.port in 1..65535)
    if (uri.scheme == "http") {
        val host = uri.host.lowercase(Locale.ROOT)
        val octets = host.split('.').mapNotNull { it.toIntOrNull() }
        val localIp = octets.size == 4 && octets.all { it in 0..255 } &&
            (octets[0] == 10 || octets[0] == 127 ||
                (octets[0] == 192 && octets[1] == 168) ||
                (octets[0] == 172 && octets[1] in 16..31))
        val localName = host == "localhost" || host.endsWith(".local") || !host.contains('.')
        require(localIp || localName)
    }
    URI(uri.scheme, null, uri.host, uri.port, "/", null, null).toASCIIString()
}.getOrNull()
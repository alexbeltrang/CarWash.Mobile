package com.carwash.mobile.reportes

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Locale

data class ReportCriteria(
    val initialDate: LocalDate? = null,
    val finalDate: LocalDate? = null,
    val paymentMethodId: Int? = null,
    val operatorId: Int? = null,
    val plate: String? = null,
    val vehicleTypeId: Int? = null,
) {
    fun normalized() = copy(plate = plate?.trim()?.uppercase(Locale.ROOT)?.takeIf(String::isNotEmpty))

    fun invalidFields(): Set<String> = buildSet {
        if (initialDate == null) add("fechaInicial")
        if (finalDate == null || finalDate == LocalDate.of(9999, 12, 31) || finalDate?.year !in 1..9999) add("fechaFinal")
        if (initialDate != null && initialDate.year !in 1..9999) add("fechaInicial")
        if (initialDate != null && finalDate != null && initialDate > finalDate) add("fechaFinal")
        if (paymentMethodId != null && paymentMethodId <= 0) add("idFormaPago")
        if (operatorId != null && operatorId <= 0) add("idOperario")
        if (vehicleTypeId != null && vehicleTypeId <= 0) add("idTipoVehiculo")
    }
}

data class CollectionItem(
    val id: Int,
    val turnId: Int,
    val collectedAt: OffsetDateTime,
    val plate: String?,
    val vehicleTypeId: Int?,
    val vehicleType: String?,
    val operatorId: Int?,
    val operator: String?,
    val paymentMethodId: Int,
    val paymentMethod: String?,
    val amount: BigDecimal,
)

data class PaymentSummary(val paymentMethodId: Int, val paymentMethod: String?, val count: Long, val total: BigDecimal)

data class SalesPage(
    val criteria: ReportCriteria,
    val count: Long,
    val total: BigDecimal,
    val items: List<CollectionItem>,
    val nextCursor: String?,
    val pageSize: Int,
    val consultedAt: Instant,
    val businessDate: LocalDate,
    val summary: List<PaymentSummary> = emptyList(),
)

enum class ReportErrorKind { INVALID_FILTERS, INVALID_CURSOR, SESSION_REQUIRED, ADMIN_REQUIRED, UNAVAILABLE, NETWORK, INVALID_RESPONSE, UNEXPECTED }

class ReportError(val kind: ReportErrorKind, val fields: Set<String> = emptySet()) : Exception(kind.name)

enum class ReportStatus { NOT_QUERIED, LOADING, RESULTS, EMPTY, ERROR, SESSION_REQUIRED, FORBIDDEN, DATA_CHANGED }

data class FilterOption(val id: Int, val name: String?)
data class ReportOptions(val initialDate: LocalDate, val finalDate: LocalDate,
    val paymentMethods: List<FilterOption>, val operators: List<FilterOption>, val vehicleTypes: List<FilterOption>, val businessDate: LocalDate)

data class ReportState(
    val edited: ReportCriteria = ReportCriteria(),
    val page: SalesPage? = null,
    val status: ReportStatus = ReportStatus.NOT_QUERIED,
    val fieldErrors: Set<String> = emptySet(),
    val error: ReportErrorKind? = null,
    val loadingMore: Boolean = false,
    val partial: Boolean = false,
    val options: ReportOptions? = null,
    val optionsLoading: Boolean = false,
    val optionsError: ReportErrorKind? = null,
) {
    val previousResults: Boolean get() = page != null && edited.normalized() != page.criteria
}
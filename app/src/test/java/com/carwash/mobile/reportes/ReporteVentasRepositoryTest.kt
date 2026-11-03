package com.carwash.mobile.reportes

import com.carwash.mobile.auth.Session
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

class ReporteVentasRepositoryTest {
    private val server = MockWebServer().apply { start() }
    private val criteria = ReportCriteria(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-03"))
    private val session get() = Session(1, "Admin", true, server.url("/").toString(), "A".repeat(43))

    @After fun close() = server.shutdown()

    @Test fun datesOnlyUseIssuerCredentialAndExactDecimalBusinessOffset() = runTest {
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json")
            .setHeader("X-Server-Date", "2026-11-03").setBody(pageJson))
        val result = ApiReporteVentasRepository(session).sales(criteria)
        val request = server.takeRequest(1, TimeUnit.SECONDS)!!
        assertEquals("/api/Reportes/Ventas", request.requestUrl!!.encodedPath)
        assertEquals(setOf("fechaInicial", "fechaFinal"), request.requestUrl!!.queryParameterNames)
        assertEquals("2026-11-01", request.requestUrl!!.queryParameter("fechaInicial"))
        assertEquals("2026-11-03", request.requestUrl!!.queryParameter("fechaFinal"))
        assertEquals("Bearer ${session.accessToken}", request.getHeader("Authorization"))
        assertEquals(2L, result.count)
        assertEquals(0, BigDecimal("120.30").compareTo(result.total))
        assertEquals(BigDecimal("20.20"), result.items.single().amount)
        assertEquals(OffsetDateTime.parse("2026-11-03T23:59:59.999-05:00"), result.items.single().collectedAt)
        assertEquals(LocalDate.parse("2026-11-03"), result.businessDate)
        assertEquals(criteria, result.criteria)
        assertEquals("next-page", result.nextCursor)
    }

    @Test fun explicitProblemsNeverBecomeZeroSales() = runTest {
        val cases = listOf(
            Triple(400, "INVALID_FILTERS", ReportErrorKind.INVALID_FILTERS),
            Triple(401, "SESSION_REQUIRED", ReportErrorKind.SESSION_REQUIRED),
            Triple(403, "ADMIN_REQUIRED", ReportErrorKind.ADMIN_REQUIRED),
            Triple(503, "REPORT_UNAVAILABLE", ReportErrorKind.UNAVAILABLE),
        )
        for ((status, code, kind) in cases) {
            server.enqueue(MockResponse().setResponseCode(status).setHeader("Content-Type", "application/problem+json")
                .setBody("""{"status":$status,"code":"$code","detail":"private server information","errors":{"fechaFinal":["invalid"]}}"""))
            try {
                ApiReporteVentasRepository(session).sales(criteria)
                fail("Expected a report error, not an empty success")
            } catch (error: ReportError) {
                assertEquals(kind, error.kind)
                assertFalse(error.toString().contains("private server information"))
                if (status == 400) assertTrue(error.fields.contains("fechaFinal"))
            }
        }
    }

    @Test fun foreignRedirectNeverReceivesBearerAndNoFallbackIsAttempted() = runTest {
        val other = MockWebServer().apply { start() }
        try {
            server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", other.url("api/Reportes/Ventas")))
            other.enqueue(MockResponse().setBody(pageJson))
            try {
                ApiReporteVentasRepository(session).sales(criteria)
                fail("Redirect must not be followed")
            } catch (_: ReportError) {
                assertEquals(0, other.requestCount)
                assertEquals(1, server.requestCount)
            }
        } finally { other.shutdown() }
    }

    @Test fun malformedMoneyAndMissingCredentialAreNotSuccessfulReports() = runTest {
        server.enqueue(MockResponse().setBody(pageJson.replace("\"120.30\"", "\"not-money\"")))
        try {
            ApiReporteVentasRepository(session).sales(criteria)
            fail("Invalid amounts must not be coerced to zero")
        } catch (error: ReportError) { assertEquals(ReportErrorKind.INVALID_RESPONSE, error.kind) }
        try {
            ApiReporteVentasRepository(session.metadata()).sales(criteria)
            fail("Restored metadata cannot authenticate reports")
        } catch (error: ReportError) { assertEquals(ReportErrorKind.SESSION_REQUIRED, error.kind) }
        assertEquals(1, server.requestCount)
    }

    @Test fun historicalOptionsUseDatesOnlyAndKeepUnknownDescription() = runTest {
        server.enqueue(MockResponse().setHeader("X-Server-Date", "2026-11-03").setBody("""{
          "fechaInicial":"2026-11-01","fechaFinal":"2026-11-03","fechaOperativa":"2026-11-03",
          "formasPago":[{"id":2,"nombre":"Credito"},{"id":999,"nombre":null}],
          "operarios":[{"id":7,"nombre":"Historico"}],"tiposVehiculo":[{"id":1,"nombre":"Automovil"}]
        }"""))
        val result = ApiReporteVentasRepository(session).options(criteria.initialDate!!, criteria.finalDate!!)
        val request = server.takeRequest(1, TimeUnit.SECONDS)!!
        assertEquals("/api/Reportes/Ventas/Filtros", request.requestUrl!!.encodedPath)
        assertEquals(setOf("fechaInicial", "fechaFinal"), request.requestUrl!!.queryParameterNames)
        assertEquals("Bearer ${session.accessToken}", request.getHeader("Authorization"))
        assertEquals(listOf(FilterOption(2, "Credito"), FilterOption(999, null)), result.paymentMethods)
        assertEquals(7, result.operators.single().id)
    }

    @Test fun selectedOptionalsAreIdsAndPlateIsNormalized() = runTest {
        val selected = criteria.copy(paymentMethodId = 2, operatorId = 7, plate = " abc123 ", vehicleTypeId = 1)
        server.enqueue(MockResponse().setBody(pageJson.replace(
            "\"idFormaPago\":null,\"idOperario\":null,\"placa\":null,\"idTipoVehiculo\":null",
            "\"idFormaPago\":2,\"idOperario\":7,\"placa\":\"ABC123\",\"idTipoVehiculo\":1")))
        ApiReporteVentasRepository(session).sales(selected)
        val request = server.takeRequest(1, TimeUnit.SECONDS)!!
        assertEquals("2", request.requestUrl!!.queryParameter("idFormaPago"))
        assertEquals("7", request.requestUrl!!.queryParameter("idOperario"))
        assertEquals("1", request.requestUrl!!.queryParameter("idTipoVehiculo"))
        assertEquals("ABC123", request.requestUrl!!.queryParameter("placa"))
    }

    @Test fun invalidBusinessDatesAndSchemaAreSafeErrorsNotCrashes() = runTest {
        for (body in listOf(pageJson.replace("2026-11-03T23:59:59.999-05:00", "invalid-date"), pageJson.replace("\"criterios\":{", "\"criterios\":\"invalid\",\"ignored\":{"))) {
            server.enqueue(MockResponse().setBody(body))
            try {
                ApiReporteVentasRepository(session).sales(criteria)
                fail("Malformed schema must be a controlled report failure")
            } catch (error: ReportError) { assertEquals(ReportErrorKind.INVALID_RESPONSE, error.kind) }
        }
    }

    companion object {
        val pageJson = """{
          "criterios":{"fechaInicial":"2026-11-01","fechaFinal":"2026-11-03","idFormaPago":null,"idOperario":null,"placa":null,"idTipoVehiculo":null},
          "cantidadCobros":2,"importeTotal":"120.30","pageSize":50,"nextCursor":"next-page",
          "consultadoEn":"2026-11-03T12:00:00Z","fechaOperativa":"2026-11-03",
          "items":[{"idMovimiento":2,"idTurno":10,"fechaCobro":"2026-11-03T23:59:59.999-05:00",
            "placa":"ABC123","idTipoVehiculo":1,"tipoVehiculo":"Automovil","idOperario":null,"operario":null,
            "idFormaPago":2,"formaPago":"Credito","montoPagado":"20.20"}]
        }""".trimIndent()
    }
}
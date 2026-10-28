package com.carwash.mobile.ingreso

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.math.BigDecimal
import java.time.LocalDateTime

class IngresoRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: ApiIngresoRepository
    private val valid = IngresoForm(plate = " abc123 ", typeId = 3, serviceIds = setOf(1, 2), value = "30000,50")

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api = Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create()).build().create(CarWashApi::class.java)
        repository = ApiIngresoRepository(api) { LocalDateTime.of(2026, 10, 28, 10, 30) }
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun validatesRequiredFieldsAndMoneyWithoutGuessingGrouping() {
        assertEquals(3, IngresoForm().errors().size)
        assertTrue(valid.errors().isEmpty())
        assertEquals(BigDecimal("20000.50"), parseAmount("20000,50"))
        assertNull(parseAmount("20.000"))
        assertNull(parseAmount("-1"))
        assertNull(parseAmount("NaN"))
    }

    @Test
    fun totalsOnlySelectedServicesForTheCurrentVehicleType() {
        val services = listOf(
            VehicleService(1, "Lavado", 3, BigDecimal("20.50"), BigDecimal("15.00")),
            VehicleService(2, "Motor", 3, BigDecimal("30"), null),
            VehicleService(1, "Moto", 1, BigDecimal("999"), BigDecimal("999")),
        )
        val selected = valid.withServices(setOf(1, 2), services)
        assertEquals("50.50", selected.value)
        assertEquals("15.00", selected.baseValue)
        assertEquals("30.00", selected.withServices(setOf(2), services).value)
        assertEquals("0.00", selected.withServices(emptySet(), services).value)
    }

    @Test
    fun acceptsLocalHttpOrHttpsButRejectsUnsafeUrls() {
        assertEquals("http://10.0.2.2:5197/", normalizeServer("http://10.0.2.2:5197"))
        assertEquals("http://192.168.1.15:5197/", normalizeServer("http://192.168.1.15:5197/"))
        assertEquals("https://example.com/", normalizeServer("https://example.com"))
        for (invalid in listOf("http://example.com", "file:///x", "http://user:pass@localhost", "http://localhost/api", "http://localhost/?token=x")) {
            assertNull(invalid, normalizeServer(invalid))
        }
    }

    @Test
    fun notFoundPlateAndHistoryAreEmptyResults() = runTest {
        respond(404, "")
        respond(404, "")
        assertEquals(PlateLookup(null, emptyList()), repository.lookup("ABC123"))
        assertTrue(server.takeRequest().path!!.contains("placa=ABC123"))
    }

    @Test
    fun closedCashDoesNotWriteAnything() = runTest {
        respond(404, "")
        var notified = false
        try {
            repository.register(valid) { notified = true }
            fail("Expected closed cash")
        } catch (_: CashClosedException) {
            assertFalse(notified)
            assertEquals(1, server.requestCount)
            assertEquals("GET", server.takeRequest().method)
        }
    }

    @Test
    fun createsDailyTurnAndAssociatesEveryService() = runTest {
        respond(200, """{"idCaja":30,"estado":true}""")
        respond(200, "[]")
        respond(201, """{"id":5,"fecha":"2026-10-28T00:00:00","consecutivo":1}""")
        respond(201, """{"idTurno":123,"numeroTurno":"T-20261028-001"}""")
        respond(201, "{}")
        respond(201, "{}")
        var pending: String? = null
        val saved = repository.register(valid) { pending = it }
        assertEquals(123, saved.idTurno)
        assertEquals("T-20261028-001", pending)
        val requests = List(6) { server.takeRequest() }
        assertEquals("POST", requests[2].method)
        val payload = com.google.gson.JsonParser.parseString(requests[3].body.readUtf8()).asJsonObject
        assertEquals("ABC123", payload["placa"].asString)
        assertTrue(payload["numeroOrden"].asJsonPrimitive.isString)
        assertEquals("0", payload["numeroOrden"].asString)
        assertEquals(BigDecimal("30000.50"), payload["valor"].asBigDecimal)
        assertEquals(30, payload["idCajaDiaria"].asInt)
        assertFalse(payload["pagado"].asBoolean)
        assertEquals(listOf(1, 2), requests.drop(4).map {
            com.google.gson.JsonParser.parseString(it.body.readUtf8()).asJsonObject["idServicios"].asInt
        })
    }

    @Test
    fun fullCreatedResponsesCompleteRegistration() = runTest {
        respond(200, """{"idCaja":30,"estado":true}""")
        respond(200, """[{"id":9,"fecha":"2026-10-28T00:00:00","consecutivo":7}]""")
        respond(204, "")
        server.enqueue(MockResponse().setResponseCode(201)
            .setHeader("Content-Type", "application/json; charset=utf-8")
            .setHeader("Location", "http://localhost:5197/api/Turnos/GetById?id=124")
            .setBody("""{"idTurno":124,"numeroTurno":"T-20261028-008","nombreCliente":"","numeroCelular":"","placa":"ABC123","fechaHoraIngreso":"2026-10-28T10:30:00","fechaHoraFin":null,"marca":"","numeroOrden":"42","valor":30000.50,"valorBaseComision":0.0,"porcentajeComision":0.0,"valorComision":0.0,"pagado":false,"pagadoNomina":false,"observaciones":"","fechaHoraAsignacionOperario":null,"estado":false,"idTipoVehiculo":3,"idOperario":null,"operadorOcupado":false,"idCajaDiaria":30,"idClienteCredito":null}"""))
        respond(201, """{"idTunoServicios":900,"idServicios":1,"idTurno":124,"isDeleted":false}""")
        respond(201, """{"idTunoServicios":901,"idServicios":2,"idTurno":124,"isDeleted":false}""")

        val saved = repository.register(valid) { }

        assertEquals(SavedTurn(124, "T-20261028-008"), saved)
        assertEquals(6, server.requestCount)
    }

    @Test
    fun updatesExistingSequenceAndReportsPartialFailureWithoutRetry() = runTest {
        respond(200, """{"idCaja":30,"estado":true}""")
        respond(200, """[{"id":9,"fecha":"2026-10-28T00:00:00","consecutivo":7}]""")
        respond(204, "")
        respond(201, """{"idTurno":124,"numeroTurno":"T-20261028-008"}""")
        respond(500, "Error")
        try {
            repository.register(valid) { }
            fail("Expected partial registration")
        } catch (failure: RegistrationException) {
            assertTrue(failure.uncertain)
            assertEquals("T-20261028-008", failure.turnNumber)
            assertEquals(5, server.requestCount)
            val requests = List(5) { server.takeRequest() }
            assertEquals("PUT", requests[2].method)
            assertEquals("/api/TurnosDiarios/ActualizarTurnoDiario/9", requests[2].path)
        }
    }

    @Test
    fun configuredClientDoesNotFollowRedirects() = runTest {
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "http://127.0.0.1:1/"))
        try {
            ApiIngresoRepository.create(server.url("/").toString()).load()
            fail("Expected redirect rejection")
        } catch (failure: retrofit2.HttpException) {
            assertEquals(302, failure.code())
            assertEquals(1, server.requestCount)
        }
    }

    private fun respond(code: Int, body: String) {
        server.enqueue(MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body))
    }
}
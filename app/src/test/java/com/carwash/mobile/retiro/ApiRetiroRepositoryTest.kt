package com.carwash.mobile.retiro

import com.carwash.mobile.auth.Session
import com.google.gson.JsonParser
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class ApiRetiroRepositoryTest {
    @Test fun physicalDeliveryPostsOnlyReviewWithoutCode() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"idTurno":10,"codigo":"0123456789ABCDEF","placa":"ABC123","marca":"Marca","cliente":"Prueba","fechaIngreso":"2026-11-06T09:00:00-05:00","total":17500,"servicios":[{"nombre":"Lavado","precio":17500}],"entregadoEn":"2026-11-06T20:00:00Z","entregadoPor":2}"""))
            val source = ApiRetiroRepository(Session(2, "Operador", false, server.url("/").toString(), "A".repeat(43)))
            assertNotNull(source.confirm(10).entregadoEn)
            val request = server.takeRequest()
            assertEquals("/api/RetiroVehiculos/10/entrega", request.path)
            val json = JsonParser.parseString(request.body.readUtf8()).asJsonObject
            assertTrue(json.get("revisionConfirmada").asBoolean)
            assertFalse(json.has("codigo"))
            assertEquals(1, json.size())
        }
    }
    @Test fun automaticPrintUsesAuthenticatedEntryRouteWithConfirmedServices() = runTest {
        MockWebServer().use { server ->
            val id = UUID.randomUUID().toString()
            server.enqueue(MockResponse().setResponseCode(202).setBody(job(id)))
            val source = ApiRetiroRepository(Session(2, "Operador", false, server.url("/").toString(), "A".repeat(43)))
            assertEquals(id, source.printEntry(10, setOf(2, 1)).solicitudId)
            val request = server.takeRequest()
            assertEquals("/api/RetiroVehiculos/10/impresiones/ingreso", request.path)
            assertEquals("Bearer " + "A".repeat(43), request.getHeader("Authorization"))
            val json = JsonParser.parseString(request.body.readUtf8()).asJsonObject
            assertEquals("[1,2]", json.get("idServicios").toString())
            assertEquals(1, json.size())
        }
    }
    @Test fun printUsesBearerUuidAndWidthAndReadsTheSameJob() = runTest {
        MockWebServer().use { server ->
            val id = UUID.randomUUID().toString()
            server.enqueue(MockResponse().setResponseCode(202).setBody(job(id)))
            server.enqueue(MockResponse().setBody(job(id)))
            server.enqueue(MockResponse().setResponseCode(204))
            val source = ApiRetiroRepository(Session(2, "Operador", false, server.url("/").toString(), "A".repeat(43)))
            assertEquals(id, source.requestPrint(10, id, 80).solicitudId)
            val request = server.takeRequest()
            assertEquals("Bearer " + "A".repeat(43), request.getHeader("Authorization"))
            assertEquals("/api/RetiroVehiculos/10/impresiones", request.path)
            val json = JsonParser.parseString(request.body.readUtf8()).asJsonObject
            assertEquals(id, json.get("solicitudId").asString)
            assertEquals(80, json.get("anchoMm").asInt)
            assertFalse(json.has("impresora"))
            assertEquals(id, source.getPrint(id).solicitudId)
            assertEquals("/api/RetiroVehiculos/impresiones/$id", server.takeRequest().path)
            assertNull(source.latestPrint(10))
            assertEquals("/api/RetiroVehiculos/10/impresiones/ultima", server.takeRequest().path)
        }
    }

    @Test fun expiredSessionIsNotReportedAsPrinted() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(401))
            val source = ApiRetiroRepository(Session(2, "Operador", false, server.url("/").toString(), "A".repeat(43)))
            try { source.requestPrint(10, UUID.randomUUID().toString(), 80); fail("Expected expired session") }
            catch (error: RetiroFailure) { assertEquals(RetiroError.SESSION_REQUIRED, error.kind) }
        }
    }

    @Test fun mismatchedPrintIdentifierIsRejected() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(202).setBody(job(UUID.randomUUID().toString())))
            val source = ApiRetiroRepository(Session(2, "Operador", false, server.url("/").toString(), "A".repeat(43)))
            try { source.requestPrint(10, UUID.randomUUID().toString(), 80); fail("Expected mismatched request") }
            catch (error: RetiroFailure) { assertEquals(RetiroError.UNAVAILABLE, error.kind) }
        }
    }

    private fun job(id: String) = """{"solicitudId":"$id","idTurno":10,"anchoMm":80,"impresora":"FAKE USB","estado":"PENDING","creadoEn":"2026-11-06T15:00:00Z","actualizadoEn":"2026-11-06T15:00:00Z"}"""
}
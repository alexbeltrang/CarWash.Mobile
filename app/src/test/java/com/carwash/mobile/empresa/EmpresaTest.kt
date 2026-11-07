package com.carwash.mobile.empresa

import com.carwash.mobile.auth.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class EmpresaTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun cleanup() = Dispatchers.resetMain()

    @Test fun manualHeaderIdentifiesBusinessWithoutCallingItAnInvoice() {
        val company = EmpresaDto(1, "Autolavado & prueba", "Empresa SAS", "900123456", "8",
            "Calle 1", "Bogota", "Bogota D.C.", "Colombia", "3001234567", "empresa@example.invalid")
        val text = empresaReceiptHeader(company, "NIT", "Telefono")
        assertTrue(text.startsWith(company.nombreComercial))
        assertTrue(text.contains("NIT: 900123456-8")); assertTrue(text.contains(company.correo))
        assertFalse(text.contains("CarWash")); assertFalse(text.contains("Factura"))
        assertEquals("", empresaReceiptHeader(null, "NIT", "Telefono"))
    }

    @Test fun companyIsReadOnlyAuthenticatedAndAbsenceIsNotInvented() = runTest(dispatcher) {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"revision":1,"nombreComercial":"Autolavado prueba","nit":"900123456","digitoVerificacion":"8"}"""))
            server.enqueue(MockResponse().setResponseCode(204))
            val source = ApiEmpresaRepository(Session(2, "Operador", false, server.url("/").toString(), "A".repeat(43)))
            assertEquals("Autolavado prueba", source.get()?.nombreComercial)
            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/Empresa", request.path)
            assertEquals("Bearer " + "A".repeat(43), request.getHeader("Authorization"))
            assertNull(source.get())
        }
    }

    @Test fun expiredSessionAndMalformedCompanyAreRejected() = runTest(dispatcher) {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(401))
            server.enqueue(MockResponse().setBody("""{"revision":0,"nombreComercial":"Otro emisor"}"""))
            val source = ApiEmpresaRepository(Session(2, "Operador", false, server.url("/").toString(), "token"))
            try { source.get(); fail("Expired session") } catch (error: EmpresaFailure) { assertTrue(error.sessionRequired) }
            try { source.get(); fail("Malformed company") } catch (error: EmpresaFailure) { assertFalse(error.sessionRequired) }
        }
    }

    @Test fun refreshReplacesIdentityAndFailureOrLogoutClearsIt() = runTest(dispatcher) {
        var current = EmpresaDto(revision = 1, nombreComercial = "Primera empresa")
        var failure: Exception? = null
        val model = EmpresaViewModel(object : EmpresaRepository {
            override suspend fun get(): EmpresaDto { failure?.let { throw it }; return current }
        })
        model.refresh(); runCurrent(); assertEquals(current, model.empresa)
        current = current.copy(revision = 2, nombreComercial = "Empresa actual")
        model.refresh(); runCurrent(); assertEquals(current, model.empresa)
        failure = IOException(); model.refresh(); runCurrent(); assertNull(model.empresa)
        failure = EmpresaFailure(true); model.refresh(); runCurrent(); assertTrue(model.sessionRequired)
        failure = null; model.refresh(); runCurrent(); model.clear()
        assertNull(model.empresa); assertFalse(model.sessionRequired)
    }
}
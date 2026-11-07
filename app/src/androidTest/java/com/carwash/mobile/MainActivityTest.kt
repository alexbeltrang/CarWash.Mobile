package com.carwash.mobile

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import com.carwash.mobile.empresa.EmpresaDto
import com.carwash.mobile.empresa.LocalEmpresa
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.carwash.mobile.ingreso.CashClosedException
import com.carwash.mobile.ingreso.CashRegister
import com.carwash.mobile.ingreso.Catalog
import com.carwash.mobile.ingreso.IngresoForm
import com.carwash.mobile.ingreso.IngresoRepository
import com.carwash.mobile.ingreso.IngresoScreen
import com.carwash.mobile.ingreso.IngresoViewModel
import com.carwash.mobile.ingreso.InProcessTurn
import com.carwash.mobile.ingreso.PlateLookup
import com.carwash.mobile.ingreso.RegistrationException
import com.carwash.mobile.ingreso.RegistrationStage
import com.carwash.mobile.ingreso.SavedTurn
import com.carwash.mobile.ingreso.ServerSettings
import com.carwash.mobile.ingreso.VehicleInfo
import com.carwash.mobile.ingreso.VehicleService
import com.carwash.mobile.ingreso.VehicleType
import com.carwash.mobile.ingreso.EmailNotificationResult
import com.carwash.mobile.ingreso.WhatsAppNotificationResult
import com.carwash.mobile.ingreso.money
import com.carwash.mobile.ingreso.openManualWhatsApp
import com.carwash.mobile.ui.theme.CarWashTheme
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val composeRule = createComposeRule()
    private lateinit var model: IngresoViewModel
    private lateinit var repository: FakeRepository
    private lateinit var settings: MemorySettings
    private val whatsAppIntents = mutableListOf<Intent>()
    private var whatsappMissing = false
    private val showScreen = mutableStateOf(true)
    private val company = mutableStateOf<EmpresaDto?>(null)

    @Before
    fun setUp() {
        composeRule.runOnUiThread {
            repository = FakeRepository()
            settings = MemorySettings()
            model = IngresoViewModel(settings) { repository }
        }
        composeRule.setContent {
            CarWashTheme {
                if (showScreen.value) {
                    CompositionLocalProvider(LocalEmpresa provides company.value) {
                    IngresoScreen(model, onOpenWhatsApp = { intent ->
                        whatsAppIntents += intent
                        if (whatsappMissing) throw ActivityNotFoundException()
                    })
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun formDisplaysAndValidatesWithoutSending() {
        composeRule.onNodeWithText("CarWash").assertIsDisplayed()
        composeRule.onNodeWithText("Ingreso de vehículo").assertIsDisplayed()
        composeRule.onNodeWithText("Conectado").assertIsDisplayed()
        composeRule.onNodeWithTag("save").assertIsNotEnabled()
        composeRule.onNodeWithTag("lookup").performClick()
        composeRule.onNodeWithText("Ingrese la placa.").performScrollTo().assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, repository.writes) }
    }

    @Test
    fun vehicleFieldsStayHiddenUntilSearchAndHideAgainWhenPlateChanges() {
        for (tag in listOf("vehicleType", "observations")) {
            composeRule.onNodeWithTag(tag).assertDoesNotExist()
        }
        composeRule.onNodeWithTag("plate").performTextInput("abc123")
        composeRule.onNodeWithTag("vehicleType").assertDoesNotExist()
        composeRule.onNodeWithTag("lookup").performClick()
        composeRule.onNodeWithTag("vehicleType").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("plate").performScrollTo().performTextReplacement("ZZ")
        composeRule.onNodeWithTag("vehicleType").assertDoesNotExist()
        composeRule.onNodeWithTag("save").assertIsNotEnabled()
    }

    @Test
    fun selectedServicesRecalculateTotalAndBase() {
        search("abc123")
        selectAutomobile()
        composeRule.onNodeWithTag("service-1").performScrollTo().performClick()
        composeRule.runOnIdle {
            assertEquals("30000.00", model.state.form.value)
            assertEquals("25000.00", model.state.form.baseValue)
        }
        composeRule.onNodeWithTag("service-2").performScrollTo().performClick()
        composeRule.runOnIdle { assertEquals("50000.00", model.state.form.value) }
        composeRule.onNodeWithTag("service-1").performScrollTo().performClick()
        composeRule.runOnIdle { assertEquals("20000.00", model.state.form.value) }
    }

    @Test
    fun validSaveUsesFakeRepositoryAndResetsForm() {
        composeRule.onNodeWithTag("order").assertDoesNotExist()
        fillForm()
        composeRule.onNodeWithTag("save").performClick()
        composeRule.onNodeWithText("Ingreso registrado: T-20261028-001", substring = true)
            .performScrollTo().assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(1, repository.writes)
            assertEquals("ABC123", repository.lastForm?.plate)
            assertEquals(IngresoForm(), model.state.form)
            assertNull(settings.pendingTurn)
        }
        fillForm()
        composeRule.onNodeWithTag("save").performClick()
        composeRule.runOnIdle {
            assertEquals(2, repository.writes)
            assertEquals(IngresoForm(), model.state.form)
            assertFalse(model.state.reviewRequired)
            assertNull(settings.pendingTurn)
        }
    }

    @Test
    fun automaticPrintNormalStatesDoNotShowAnExtraMessage() {
        for (status in listOf("PENDING", "SUBMITTING", "SUBMITTED")) {
            composeRule.runOnIdle { model.configureEntryPrinting { _, _, _ -> status } }
            fillForm()
            composeRule.onNodeWithTag("save").performClick()
            composeRule.onNodeWithText("Ingreso registrado: T-20261028-001", substring = true)
                .performScrollTo().assertIsDisplayed()
            composeRule.runOnIdle { assertEquals(status, model.state.entryPrintStatus) }
            composeRule.onNodeWithTag("entryPrintStatus").assertDoesNotExist()
            composeRule.onNodeWithText("Ingreso guardado. Comprobante", substring = true).assertDoesNotExist()
        }
        composeRule.runOnIdle { assertEquals(3, repository.writes) }
    }

    @Test
    fun automaticPrintFailureStillShowsAnIndependentWarning() {
        for (status in listOf("FAILED", "UNKNOWN")) {
            composeRule.runOnIdle { model.configureEntryPrinting { _, _, _ -> status } }
            fillForm()
            composeRule.onNodeWithTag("save").performClick()
            composeRule.onNodeWithText("Ingreso registrado: T-20261028-001", substring = true)
                .performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithTag("entryPrintStatus").performScrollTo().assertIsDisplayed()
            composeRule.runOnIdle {
                assertEquals(status, model.state.entryPrintStatus)
                assertFalse(model.state.reviewRequired)
            }
        }
        composeRule.runOnIdle { assertEquals(2, repository.writes) }
    }

    @Test
    fun manualWhatsAppIncludesTheServerPickupCode() {
        composeRule.runOnIdle { model.configurePickup { _, _ -> "0123456789ABCDEF" } }
        fillForm()
        composeRule.onNodeWithTag("save").performClick()
        composeRule.runOnIdle {
            assertEquals(1, repository.writes)
            assertEquals(1, whatsAppIntents.size)
            assertTrue(whatsAppIntents.single().data!!.getQueryParameter("text")!!.contains("*Código de retiro: 0123456789ABCDEF*"))
            assertNull(model.state.manualWhatsAppReceipt)
        }
    }

    @Test
    fun businessIdentityAppearsInEntryAndManualWhatsAppWithoutAnEditor() {
        val data = EmpresaDto(revision = 1, nombreComercial = "Autolavado prueba & asociados", razonSocial = "Empresa SAS",
            nit = "900123456", digitoVerificacion = "8", direccion = "Calle de prueba 1", municipio = "Bogota",
            departamento = "Bogota D.C.", pais = "Colombia", telefono = "3001234567", correo = "empresa@example.invalid")
        composeRule.runOnIdle { company.value = data; model.configureCompany { data } }
        composeRule.onNodeWithTag("companyName").assertIsDisplayed()
        composeRule.onNodeWithTag("companyLogo").assertDoesNotExist()
        composeRule.onNodeWithText("Guardar empresa").assertDoesNotExist()
        fillForm(); composeRule.onNodeWithTag("save").performClick()
        composeRule.runOnIdle {
            val text = whatsAppIntents.single().data!!.getQueryParameter("text")!!
            assertTrue(text.startsWith(data.nombreComercial))
            assertTrue(text.contains("NIT: 900123456-8")); assertTrue(text.contains(data.correo))
            assertTrue(text.contains(data.telefono)); assertFalse(text.contains("Factura"))
            assertEquals(1, repository.writes); assertNull(model.state.manualWhatsAppReceipt)
        }
    }

    @Test
    fun plateLookupPrefillsAndMissingPlateClearsPreviousVehicle() {
        composeRule.onNodeWithTag("plate").performTextInput("abc123")
        composeRule.onNodeWithTag("lookup").performClick()
        composeRule.runOnIdle {
            assertEquals("CLIENTE DE PRUEBA", model.state.form.customer)
            assertEquals(3, model.state.form.typeId)
        }
        composeRule.onNodeWithTag("plate").performScrollTo().performTextReplacement("ZZZ999")
        composeRule.onNodeWithTag("lookup").performClick()
        composeRule.onNodeWithText("Placa sin registros anteriores").performScrollTo().assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals("", model.state.form.customer)
            assertEquals("", model.state.form.brand)
            assertNull(model.state.form.typeId)
        }
    }

    @Test
    fun partialSaveKeepsDataAndDisablesResubmission() {
        composeRule.runOnIdle { repository.failAfterWrite = true }
        fillForm()
        composeRule.onNodeWithTag("save").performClick()
        composeRule.onNodeWithTag("save").assertIsNotEnabled()
        composeRule.onNodeWithTag("openCamera").performScrollTo().assertIsNotEnabled()
        composeRule.runOnIdle {
            assertTrue(model.state.reviewRequired)
            assertEquals("ABC123", model.state.form.plate)
            assertEquals(1, repository.writes)
        }
    }

    @Test
    fun emailFailureCanBeRetriedWithoutResubmittingTheForm() {
        repository.notificationStatus = "FAILED"
        fillForm()
        composeRule.onNodeWithTag("save").performClick()
        composeRule.onNodeWithText("No se pudo enviar el correo.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Reintentar envío").performScrollTo().performClick()
        composeRule.onNodeWithText("Correo enviado.").performScrollTo().assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(1, repository.writes)
            assertEquals("ACCEPTED", model.state.notificationStatus)
        }
    }

    @Test
    fun closedCashDoesNotWrite() {
        composeRule.runOnIdle { repository.closed = true }
        fillForm()
        composeRule.onNodeWithTag("save").performClick()
        composeRule.onNodeWithText("No existe caja abierta. Abra la caja desde el sistema y vuelva a intentar.")
            .performScrollTo().assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, repository.writes) }
    }

    @Test
    fun uncertainRegistrationShowsSafeStageAndDoesNotOfferWhatsApp() {
        repository.failAfterWrite = true
        fillForm()
        composeRule.onNodeWithTag("save").performClick()
        composeRule.onNodeWithTag("registrationFailure").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Etapa sin confirmar: servicios. Diagnóstico: IOException.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("PRIVATE_RESPONSE_BODY", substring = true).assertDoesNotExist()
        composeRule.onNodeWithTag("openWhatsApp").assertDoesNotExist()
        composeRule.runOnIdle {
            assertTrue(model.state.reviewRequired)
            assertEquals("T-20261028-001", settings.pendingTurn)
            assertEquals(1, repository.writes)
            assertEquals(0, repository.emailCalls)
            assertEquals(0, repository.whatsAppCalls)
            assertTrue(whatsAppIntents.isEmpty())
        }
    }

    @Test
    fun connectionRejectsCredentialsAndPersistsValidServer() {
        composeRule.onNodeWithContentDescription("Conexión").performClick()
        composeRule.onNodeWithTag("serverUrl").performTextReplacement("http://user:pass@localhost:5197")
        composeRule.onNodeWithText("Conectar").assertIsNotEnabled()
        composeRule.onNodeWithTag("serverUrl").performTextReplacement("http://192.168.1.15:5197")
        composeRule.onNodeWithText("Conectar").performClick()
        composeRule.runOnIdle { assertEquals("http://192.168.1.15:5197/", settings.serverUrl) }
    }

    @Test
    fun confirmedCameraPlateUsesExistingLookupWithoutSaving() {
        composeRule.runOnIdle { model.acceptScannedPlate(" abc123 ") }
        composeRule.runOnIdle {
            assertEquals("ABC123", model.state.form.plate)
            assertEquals("CLIENTE DE PRUEBA", model.state.form.customer)
            assertEquals(3, model.state.form.typeId)
            assertEquals(0, repository.writes)
        }
        composeRule.onNodeWithTag("plate").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun whatsappNotConfiguredDoesNotHideSuccessfulRegistration() {
        fillForm()
        composeRule.onNodeWithTag("whatsappMode-AUTOMATIC").performScrollTo().performClick()
        composeRule.onNodeWithTag("save").performClick()
        composeRule.onNodeWithText("WhatsApp no está configurado en el servidor.")
            .performScrollTo().assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(1, repository.writes)
            assertEquals(IngresoForm(), model.state.form)
            assertEquals(com.carwash.mobile.ingreso.NoticeKind.SAVED, model.state.notice?.kind)
        }
        composeRule.onNodeWithContentDescription("Reintentar WhatsApp").assertDoesNotExist()
    }

    @Test
    fun whatsappRetryOnlyRetriesMessageAndDoesNotClaimDelivery() {
        repository.whatsAppResult = WhatsAppNotificationResult(1, "RETRYING", "RETRY_MANUAL")
        fillForm()
        composeRule.onNodeWithTag("whatsappMode-AUTOMATIC").performScrollTo().performClick()
        composeRule.onNodeWithTag("save").performClick()
        composeRule.onNodeWithContentDescription("Reintentar WhatsApp").performScrollTo().performClick()
        composeRule.onNodeWithText("Mensaje aceptado por WhatsApp.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Mensaje entregado.").assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(1, repository.writes)
            assertEquals(1, repository.whatsAppRetries)
        }
    }

    @Test
    fun manualWhatsAppPreviewsConfirmedDataAndOpensWithoutRegisteringAgain() {
        fillForm()
        composeRule.onNodeWithTag("service-2").performScrollTo().performClick()
        composeRule.onNodeWithTag("value").performScrollTo().performTextReplacement("17500.50")
        composeRule.onNodeWithTag("save").performClick()
        composeRule.onNodeWithTag("openWhatsApp").assertDoesNotExist()
        composeRule.runOnIdle {
            val intent = whatsAppIntents.single()
            assertEquals(Intent.ACTION_VIEW, intent.action)
            assertEquals("com.whatsapp", intent.`package`)
            assertEquals("whatsapp", intent.data?.scheme)
            assertEquals("send", intent.data?.host)
            assertEquals("573000000000", intent.data?.getQueryParameter("phone"))
            val text = requireNotNull(intent.data?.getQueryParameter("text"))
            assertTrue(text.startsWith("🚘 *RECEPCIÓN DE VEHÍCULO*\n\n"))
            assertTrue(text.contains("Hola, CLIENTE DE PRUEBA 👋"))
            assertTrue(text.contains("🔖 *Placa:* ABC123\n🚗 *Marca:* MARCA"))
            assertTrue(text.contains("\n\n🧽 *Servicios autorizados*\n✅ Lavado Exterior y Aspirado:"))
            assertTrue(text.contains("\n✅ Lavado Express:"))
            assertTrue(text.contains("\n\n💰 *TOTAL A PAGAR: ${money(BigDecimal("17500.50"))}*\n\n"))
            assertTrue(text.endsWith("¡Gracias por confiar en nosotros! ✨"))
            assertTrue(text.contains("ABC123"))
            assertTrue(text.contains("CLIENTE DE PRUEBA"))
            assertTrue(text.contains("MARCA"))
            assertTrue(text.contains("Lavado Exterior y Aspirado"))
            assertTrue(text.contains("Lavado Express"))
            assertTrue(text.contains(money(BigDecimal("17500.50"))))
            assertEquals(1, repository.writes)
            assertEquals(0, repository.whatsAppCalls)
            assertNull(model.state.whatsAppStatus)
            assertNull(model.state.manualWhatsAppReceipt)
        }
        composeRule.onNodeWithText("Mensaje entregado.").assertDoesNotExist()
        composeRule.onNodeWithText("Mensaje aceptado por WhatsApp.").assertDoesNotExist()
    }

    @Test
    fun missingWhatsAppKeepsReceiptAndSavedTurn() {
        whatsappMissing = true
        fillForm()
        composeRule.onNodeWithTag("save").performClick()
        composeRule.onNodeWithText("No se pudo abrir WhatsApp ni WhatsApp Business. El ingreso sigue registrado.")
            .performScrollTo().assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(listOf("com.whatsapp", "com.whatsapp.w4b"), whatsAppIntents.map { it.`package` })
            assertEquals(1, repository.writes)
            assertNotNull(model.state.manualWhatsAppReceipt)
        }
        composeRule.onNodeWithContentDescription("Descartar mensaje manual").performScrollTo().performClick()
        composeRule.onNodeWithTag("openWhatsApp").assertDoesNotExist()
    }

    @Test
    fun manualIntentEncodesTextAndSupportsBusinessWithoutBrowserFallback() {
        val attempts = mutableListOf<Intent>()
        val message = "🚘 *Placa ABC123* & servicios #1: 50%\n✅ Vehículo: revisión + aspirado\n💰 Total: $30.000"
        assertTrue(openManualWhatsApp("+573000000000", message) { intent ->
            attempts += intent
            if (intent.`package` == "com.whatsapp") throw ActivityNotFoundException()
        })
        assertEquals(2, attempts.size)
        assertEquals("com.whatsapp.w4b", attempts.last().`package`)
        assertEquals("whatsapp", attempts.last().data?.scheme)
        assertEquals("send", attempts.last().data?.host)
        assertEquals("573000000000", attempts.last().data?.getQueryParameter("phone"))
        assertEquals(message, attempts.last().data?.getQueryParameter("text"))
        assertFalse(openManualWhatsApp("3000000000", message) { fail("Invalid phone must not open another app") })
    }

    @Test
    fun configuredCountryPrefixIsSavedAndAutomaticallyOpensLocalRecipient() {
        composeRule.onNodeWithContentDescription("Conexión").performClick()
        composeRule.onNodeWithTag("whatsappCountryCode").performScrollTo().performTextReplacement("52")
        composeRule.onNodeWithText("Conectar").assertIsNotEnabled()
        composeRule.onNodeWithTag("whatsappCountryCode").performTextReplacement("+52")
        composeRule.onNodeWithText("Conectar").performClick()
        fillForm()
        composeRule.onNodeWithTag("save").performClick()
        composeRule.runOnIdle {
            assertEquals("+52", settings.whatsAppCountryCode)
            assertEquals("523000000000", whatsAppIntents.single().data?.getQueryParameter("phone"))
            assertEquals("3000000000", repository.lastForm?.phone)
            assertEquals(1, repository.writes)
        }
        composeRule.onNodeWithTag("openWhatsApp").assertDoesNotExist()
    }

    @Test
    fun automaticOpeningIsNotRepeatedWhenScreenIsRecreated() {
        fillForm()
        composeRule.onNodeWithTag("save").performClick()
        composeRule.runOnIdle { assertEquals(1, whatsAppIntents.size) }
        composeRule.runOnIdle { showScreen.value = false }
        composeRule.waitForIdle()
        composeRule.runOnIdle { showScreen.value = true }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertEquals(1, whatsAppIntents.size)
            assertEquals(1, repository.writes)
            assertFalse(model.state.manualWhatsAppAutoOpen)
        }
    }

    @Test
    fun invalidPhoneNeedsCorrectionWithoutRepeatingRegistration() {
        fillForm()
        composeRule.runOnIdle { model.edit { it.copy(phone = "INVALID") } }
        composeRule.onNodeWithTag("save").performClick()
        composeRule.onNodeWithTag("openWhatsApp").performScrollTo().assertIsNotEnabled()
        composeRule.runOnIdle { assertTrue(whatsAppIntents.isEmpty()) }
        composeRule.onNodeWithTag("whatsappRecipient").performScrollTo().performTextReplacement("+573000000000")
        composeRule.onNodeWithTag("openWhatsApp").performScrollTo().performClick()
        composeRule.runOnIdle {
            assertEquals(1, whatsAppIntents.size)
            assertEquals(1, repository.writes)
            assertEquals(0, repository.whatsAppCalls)
            assertNull(model.state.manualWhatsAppReceipt)
        }
    }

    @Test
    fun successfulOpeningClearsPreviousMessageBeforeNextPlate() {
        fillForm()
        composeRule.onNodeWithTag("save").performClick()
        composeRule.runOnIdle {
            assertEquals(1, whatsAppIntents.size)
            assertEquals(IngresoForm(), model.state.form)
            assertNull(model.state.manualWhatsAppReceipt)
            assertFalse(model.state.manualWhatsAppAutoOpen)
            assertNull(model.state.editingTurnId)
        }
        composeRule.onNodeWithTag("whatsappPreview").assertDoesNotExist()
        composeRule.onNodeWithTag("whatsappRecipient").assertDoesNotExist()
        composeRule.onNodeWithTag("plate").performScrollTo().performTextInput("XYZ789")
        composeRule.onNodeWithTag("lookup").performClick()
        composeRule.onNodeWithTag("whatsappPreview").assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals("XYZ789", model.state.form.plate)
            assertNull(model.state.manualWhatsAppReceipt)
            assertEquals(1, repository.writes)
            assertEquals(1, whatsAppIntents.size)
        }
    }

    @Test
    fun successfulRetryClearsReceiptWithoutRegisteringAgain() {
        whatsappMissing = true
        fillForm()
        composeRule.onNodeWithTag("save").performClick()
        composeRule.runOnIdle {
            assertNotNull(model.state.manualWhatsAppReceipt)
            assertEquals(2, whatsAppIntents.size)
            whatsappMissing = false
        }
        composeRule.onNodeWithTag("openWhatsApp").performScrollTo().performClick()
        composeRule.onNodeWithTag("whatsappPreview").assertDoesNotExist()
        composeRule.onNodeWithTag("whatsappRecipient").assertDoesNotExist()
        composeRule.runOnIdle {
            assertNull(model.state.manualWhatsAppReceipt)
            assertEquals(IngresoForm(), model.state.form)
            assertEquals(3, whatsAppIntents.size)
            assertEquals(1, repository.writes)
        }
    }

    @Test
    fun emailExceptionStillAllowsOpeningManualWhatsApp() {
        repository.failEmail = true
        fillForm()
        composeRule.onNodeWithTag("save").performClick()
        composeRule.onNodeWithText("No se pudo confirmar el envío del correo.").performScrollTo().assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(1, whatsAppIntents.size)
            assertEquals(1, repository.writes)
            assertEquals(1, repository.emailCalls)
            assertFalse(model.state.reviewRequired)
            assertNull(settings.pendingTurn)
            assertNull(model.state.manualWhatsAppReceipt)
        }
    }

    @Test
    fun confirmedUpdateOffersManualWhatsAppWithoutNewRegistrationOrEmail() {
        repository.lookupInProcess = InProcessTurn(
            9, "T-20261106-001", "CLIENTE DE PRUEBA", "+573000000000", "cliente@example.com",
            "MARCA", "", 3, BigDecimal("30000"), BigDecimal("25000"), listOf(1),
        )
        fillForm()
        composeRule.onNodeWithTag("save").performClick()
        composeRule.onNodeWithText("Turno en proceso actualizado: ABC123").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("openWhatsApp").assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(1, whatsAppIntents.size)
            assertEquals(0, repository.writes)
            assertEquals(1, repository.updates)
            assertEquals(0, repository.emailCalls)
            assertEquals(0, repository.whatsAppCalls)
        }
    }

    private fun selectAutomobile() {
        composeRule.onNodeWithTag("vehicleType").performScrollTo().performClick()
        composeRule.onAllNodesWithText("Automóviles").onLast().performClick()
    }

    private fun search(plate: String) {
        composeRule.onNodeWithTag("plate").performTextInput(plate)
        composeRule.onNodeWithTag("lookup").performClick()
        composeRule.waitForIdle()
    }

    private fun fillForm() {
        search("abc123")
        selectAutomobile()
        composeRule.onNodeWithTag("service-1").performScrollTo().performClick()
    }

    private class MemorySettings : ServerSettings {
        override var serverUrl = "http://fake.local:5197/"
        override var pendingTurn: String? = null
        override var whatsAppCountryCode = "+57"
    }

    private class FakeRepository : IngresoRepository {
        var writes = 0
        var failAfterWrite = false
        var closed = false
        var notificationStatus = "NOT_APPLICABLE"
        var lastForm: IngresoForm? = null
        var whatsAppResult = WhatsAppNotificationResult(1, "NOT_CONFIGURED")
        var whatsAppRetries = 0
        var whatsAppCalls = 0
        var failEmail = false
        var emailCalls = 0
        var updates = 0
        var lookupInProcess: InProcessTurn? = null

        override suspend fun notifyEmail(idTurno: Int): EmailNotificationResult {
            emailCalls++
            if (failEmail) throw IOException("Email unavailable")
            return EmailNotificationResult(idTurno, notificationStatus, "NONE")
        }

        override suspend fun updateInProcess(idTurno: Int, form: IngresoForm) {
            updates++
            lastForm = form
        }

        override suspend fun notifyWhatsApp(idTurno: Int): WhatsAppNotificationResult {
            whatsAppCalls++
            return whatsAppResult
        }

        override suspend fun retryWhatsApp(idTurno: Int): WhatsAppNotificationResult {
            whatsAppRetries++
            return WhatsAppNotificationResult(idTurno, "ACCEPTED")
        }

        override suspend fun load() = Catalog(
            listOf(VehicleType(3, "Automóviles"), VehicleType(1, "Moto")),
            listOf(
                VehicleService(1, "Lavado Exterior y Aspirado", 3, BigDecimal("30000"), BigDecimal("25000")),
                VehicleService(2, "Lavado Express", 3, BigDecimal("20000"), BigDecimal("15000")),
            ),
            CashRegister(30, true),
        )

        override suspend fun lookup(plate: String) = PlateLookup(
            if (plate == "ABC123") VehicleInfo(3, "CLIENTE DE PRUEBA", "3000000000", "cliente@example.com", "MARCA") else null,
            emptyList(),
            lookupInProcess,
        )

        override suspend fun register(form: IngresoForm, beforeWrite: (String) -> Unit): SavedTurn {
            if (closed) throw CashClosedException()
            beforeWrite("T-20261028-001")
            writes++
            lastForm = form
            if (failAfterWrite) throw RegistrationException(
                true, "T-20261028-001", IOException("PRIVATE_RESPONSE_BODY"), RegistrationStage.SERVICES,
            )
            return SavedTurn(1, "T-20261028-001", notificationStatus)
        }

        override suspend fun retryEmail(idTurno: Int) =
            EmailNotificationResult(idTurno, "ACCEPTED", "NONE")
    }
}
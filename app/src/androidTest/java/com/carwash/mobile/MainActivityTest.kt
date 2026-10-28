package com.carwash.mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
import com.carwash.mobile.ingreso.PlateLookup
import com.carwash.mobile.ingreso.RegistrationException
import com.carwash.mobile.ingreso.SavedTurn
import com.carwash.mobile.ingreso.ServerSettings
import com.carwash.mobile.ingreso.VehicleInfo
import com.carwash.mobile.ingreso.VehicleService
import com.carwash.mobile.ingreso.VehicleType
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

    @Before
    fun setUp() {
        composeRule.runOnUiThread {
            repository = FakeRepository()
            settings = MemorySettings()
            model = IngresoViewModel(settings) { repository }
        }
        composeRule.setContent { CarWashTheme { IngresoScreen(model) } }
        composeRule.waitForIdle()
    }

    @Test
    fun formDisplaysAndValidatesWithoutSending() {
        composeRule.onNodeWithText("CarWash").assertIsDisplayed()
        composeRule.onNodeWithText("Ingreso de vehículo").assertIsDisplayed()
        composeRule.onNodeWithText("Conectado").assertIsDisplayed()
        composeRule.onNodeWithTag("save").performClick()
        composeRule.onNodeWithText("Ingrese la placa.").performScrollTo().assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, repository.writes) }
    }

    @Test
    fun selectedServicesRecalculateTotalAndBase() {
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
        fillForm()
        composeRule.onNodeWithTag("save").performClick()
        composeRule.onNodeWithText("Ingreso registrado: T-20261028-001").performScrollTo().assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(1, repository.writes)
            assertEquals("ABC123", repository.lastForm?.plate)
            assertEquals("42", repository.lastForm?.order)
            assertEquals(IngresoForm(), model.state.form)
            assertNull(settings.pendingTurn)
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
    fun closedCashDoesNotWrite() {
        composeRule.runOnIdle { repository.closed = true }
        fillForm()
        composeRule.onNodeWithTag("save").performClick()
        composeRule.onNodeWithText("No existe caja abierta. Abra la caja desde el sistema y vuelva a intentar.")
            .performScrollTo().assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, repository.writes) }
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

    private fun selectAutomobile() {
        composeRule.onNodeWithTag("vehicleType").performScrollTo().performClick()
        composeRule.onNodeWithText("Automóviles").performClick()
    }

    private fun fillForm() {
        composeRule.onNodeWithTag("plate").performTextInput("abc123")
        composeRule.onNodeWithTag("order").performScrollTo().performTextInput("42")
        selectAutomobile()
        composeRule.onNodeWithTag("service-1").performScrollTo().performClick()
    }

    private class MemorySettings : ServerSettings {
        override var serverUrl = "http://fake.local:5197/"
        override var pendingTurn: String? = null
    }

    private class FakeRepository : IngresoRepository {
        var writes = 0
        var failAfterWrite = false
        var closed = false
        var lastForm: IngresoForm? = null

        override suspend fun load() = Catalog(
            listOf(VehicleType(3, "Automóviles"), VehicleType(1, "Moto")),
            listOf(
                VehicleService(1, "Lavado Exterior y Aspirado", 3, BigDecimal("30000"), BigDecimal("25000")),
                VehicleService(2, "Lavado Express", 3, BigDecimal("20000"), BigDecimal("15000")),
            ),
            CashRegister(30, true),
        )

        override suspend fun lookup(plate: String) = PlateLookup(
            if (plate == "ABC123") VehicleInfo(3, "CLIENTE DE PRUEBA", "3000000000", "MARCA") else null,
            emptyList(),
        )

        override suspend fun register(form: IngresoForm, beforeWrite: (String) -> Unit): SavedTurn {
            if (closed) throw CashClosedException()
            beforeWrite("T-20261028-001")
            writes++
            lastForm = form
            if (failAfterWrite) throw RegistrationException(true, "T-20261028-001", IOException())
            return SavedTurn(1, "T-20261028-001")
        }
    }
}
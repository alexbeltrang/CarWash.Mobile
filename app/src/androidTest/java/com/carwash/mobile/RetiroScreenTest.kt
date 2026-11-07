package com.carwash.mobile

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import com.carwash.mobile.empresa.EmpresaDto
import com.carwash.mobile.empresa.LocalEmpresa
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.carwash.mobile.retiro.*
import com.carwash.mobile.ui.theme.CarWashTheme
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.math.BigDecimal
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class RetiroScreenTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var model: RetiroViewModel
    private lateinit var source: FakeRetiro
    private val company = mutableStateOf<EmpresaDto?>(null)

    @Before fun setup() {
        rule.runOnUiThread { source = FakeRetiro(); model = RetiroViewModel(source) }
        rule.setContent { CarWashTheme { Box(Modifier.width(320.dp)) {
            CompositionLocalProvider(LocalEmpresa provides company.value) {
            AppNavigation(isAdmin = false, destination = AppDestination.RETIRO, onDestination = {}) {
                RetiroScreen(model, onBack = {})
            }
            }
        } } }
    }

    @Test fun companyLongNameAndLogoFitWithoutEditingOrDeliveryControls() {
        val logo = Bitmap.createBitmap(64, 32, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
        val bytes = ByteArrayOutputStream().apply { logo.compress(Bitmap.CompressFormat.PNG, 100, this) }.toByteArray()
        logo.recycle()
        rule.runOnIdle {
            company.value = EmpresaDto(revision = 1, nombreComercial = "Autolavado de prueba con nombre comercial muy largo y servicios especializados",
                logoBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP))
        }
        rule.onNodeWithTag("companyName").assertIsDisplayed()
        rule.onNodeWithTag("companyLogo").assertIsDisplayed()
        val name = rule.onNodeWithTag("companyName").fetchSemanticsNode().boundsInRoot
        val image = rule.onNodeWithTag("companyLogo").fetchSemanticsNode().boundsInRoot
        val title = rule.onNodeWithText("Reimpresión del recibo").fetchSemanticsNode().boundsInRoot
        assertTrue(image.right <= name.left)
        assertTrue(name.bottom <= title.top + 1f)
        assertTrue(name.right <= rule.onRoot().fetchSemanticsNode().boundsInRoot.right)
        val rendered = rule.onNodeWithTag("companyLogo").captureToImage().asAndroidBitmap()
        assertEquals(Color.GREEN, rendered.getPixel(rendered.width / 2, rendered.height / 2))
        rule.onNodeWithText("Guardar empresa").assertDoesNotExist()
        rule.onNodeWithTag("pickupConfirm").assertDoesNotExist()
        screenshot("empresa-reimpresion-320dp.png")
        rule.runOnIdle { company.value = null }
        rule.onNodeWithTag("companyName").assertDoesNotExist()
        rule.onNodeWithTag("companyLogo").assertDoesNotExist()
        rule.onNodeWithText("Reimpresión del recibo").assertIsDisplayed()
    }

    @Test fun companyWithoutLogoShowsNameAndReceiptRemainsPrintable() {
        rule.runOnIdle { company.value = EmpresaDto(revision = 2, nombreComercial = "Autolavado actual") }
        rule.onNodeWithTag("companyName").assertIsDisplayed()
        rule.onNodeWithTag("companyLogo").assertDoesNotExist()
        search()
        rule.onNodeWithTag("pickupPrint").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1, source.prints.size); assertEquals(0, source.deliveries) }
    }

    @Test fun receiptReprintHasNoDeliveryControlsAtNarrowWidth() {
        search()
        rule.onNodeWithText("Reimpresión del recibo").assertIsDisplayed()
        rule.onNodeWithText("Reimprimir").assertIsDisplayed()
        rule.onNodeWithText("Retiro").assertDoesNotExist()
        rule.onNodeWithText("Confirmar entrega").assertDoesNotExist()
        rule.onNodeWithTag("pickupReceiptCode").assertDoesNotExist()
        rule.onNodeWithTag("pickupCode").assertDoesNotExist()
        rule.onNodeWithTag("pickupScan").assertDoesNotExist()
        rule.onNodeWithTag("pickupReviewed").assertDoesNotExist()
        rule.onNodeWithTag("pickupConfirm").assertDoesNotExist()
        rule.onNodeWithTag("pickupDelivered").assertDoesNotExist()
        rule.onNodeWithTag("pickupPrint").performScrollTo().assertIsDisplayed()
        rule.runOnIdle { assertEquals(0, source.deliveries); assertEquals(1, source.issues) }
        screenshot("recibo-reimpresion.png")
    }

    @Test fun legacyDeliveryDataIsNotDisplayedAndReceiptCanStillPrint() {
        rule.runOnIdle { source.receipt = source.receipt.copy(entregadoEn = "2026-11-06T15:00:00Z", entregadoPor = 2) }
        search()
        rule.onNodeWithTag("pickupDelivered").assertDoesNotExist()
        rule.onNodeWithTag("pickupReviewed").assertDoesNotExist()
        rule.onNodeWithTag("pickupConfirm").assertDoesNotExist()
        rule.onNodeWithTag("pickupPrint").performScrollTo().performClick()
        rule.onNodeWithTag("pickupPrintStatus").performScrollTo().assertIsDisplayed()
        rule.runOnIdle { assertEquals(1, source.prints.size); assertEquals(0, source.deliveries) }
    }

    @Test fun printUsesServerQueueAndAnotherCopyRequiresConfirmation() {
        search()
        rule.onNodeWithTag("pickupPrint").performScrollTo().performClick()
        rule.onNodeWithTag("pickupPrintStatus").performScrollTo().assertIsDisplayed()
        rule.runOnIdle { assertEquals(1, source.prints.size); assertEquals(0, source.deliveries) }
        screenshot("retiro-impresion.png")
        rule.onNodeWithTag("pickupPrint").performScrollTo().performClick()
        rule.onNodeWithTag("pickupPrintCancel").performClick()
        rule.runOnIdle { assertEquals(1, source.prints.size) }
        rule.onNodeWithTag("pickupPrint").performClick()
        rule.onNodeWithTag("pickupPrintConfirm").performClick()
        rule.runOnIdle { assertEquals(2, source.prints.distinct().size); assertEquals(0, source.deliveries) }
    }

    private fun search() {
        rule.onNodeWithTag("pickupPlate").performTextInput("abc123")
        rule.onNodeWithTag("pickupSearch").performClick()
        rule.waitForIdle()
    }

    private fun screenshot(name: String) {
        val directory = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)
        File(directory, name).outputStream().use { rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private class FakeRetiro : RetiroRepository {
        var receipt = RetiroReceipt(10, "0123456789ABCDEF", "ABC123", "Marca de prueba", "Cliente de prueba", "2026-11-06T09:00:00-05:00", BigDecimal("17500.50"),
            listOf(ReceiptService("Lavado exterior y limpieza interior", BigDecimal("30000")), ReceiptService("Aspirado", BigDecimal("20000"))))
        var deliveries = 0
        var failDelivery = false
        var issues = 0
        val prints = mutableListOf<String>()
        var printJob: PrintJob? = null
        override suspend fun latestPrint(idTurno: Int) = printJob
        override suspend fun getPrint(requestId: String) = printJob ?: throw RetiroFailure(RetiroError.NOT_FOUND)
        override suspend fun requestPrint(idTurno: Int, requestId: String, widthMm: Int): PrintJob {
            prints.add(requestId)
            return PrintJob(requestId, idTurno, widthMm, "Impresora de prueba USB", "SUBMITTED", "2026-11-06T15:00:00Z", "2026-11-06T15:00:00Z").also { printJob = it }
        }
        override suspend fun issue(request: IssueReceipt): RetiroReceipt { issues++; return receipt }
        override suspend fun get(idTurno: Int) = receipt
        override suspend fun confirm(idTurno: Int): RetiroReceipt {
            if (failDelivery) throw RetiroFailure(RetiroError.UNAVAILABLE)
            deliveries++
            receipt = receipt.copy(entregadoEn = "2026-11-06T15:00:00Z", entregadoPor = 2)
            return receipt
        }
    }
}
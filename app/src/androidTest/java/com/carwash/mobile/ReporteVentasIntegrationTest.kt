package com.carwash.mobile

import android.os.Build
import android.os.SystemClock
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.carwash.mobile.auth.ApiAuthRepository
import com.carwash.mobile.auth.CredentialCipher
import com.carwash.mobile.auth.LoginOutcome
import com.carwash.mobile.reportes.ApiReporteVentasRepository
import com.carwash.mobile.reportes.ReportStatus
import com.carwash.mobile.reportes.ReporteVentasScreen
import com.carwash.mobile.reportes.ReporteVentasViewModel
import com.carwash.mobile.ui.theme.CarWashTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.text.format.DateFormat

@RunWith(AndroidJUnit4::class)
class ReporteVentasIntegrationTest {
    @get:Rule val rule = createComposeRule()

    @Test fun realApiRendersTwentyDateOnlyQueriesWithOneThousandCollections() {
        val arguments = InstrumentationRegistry.getArguments()
        val origin = arguments.getString("reportIntegrationUrl")
        assumeTrue("Optional isolated API integration", origin != null)
        require(origin!!.startsWith("http://127.0.0.1:"))
        val connection = URL(origin + "__fixture/seed").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 5_000
        connection.readTimeout = 5_000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        val user = "fixture-client"
        val password = "fixture-password"
        fun encoded(value: String) = URLEncoder.encode(CredentialCipher.encode(value), "UTF-8")
        connection.outputStream.use { it.write("userName=${encoded(user)}&password=${encoded(password)}".toByteArray()) }
        assertEquals(204, connection.responseCode)
        connection.disconnect()
        val auth = ApiAuthRepository({ listOf(origin) })
        val outcome = runBlocking { auth.login(user, password) }
        assertTrue("Synthetic login must succeed", outcome is LoginOutcome.Success)
        val session = (outcome as LoginOutcome.Success).session
        assertTrue(session.isAdmin && session.accessToken != null)
        val model = ReporteVentasViewModel(ApiReporteVentasRepository(session), session)
        rule.setContent {
            var destination by remember { mutableStateOf(AppDestination.INGRESO) }
            CarWashTheme {
                AppNavigation(true, destination, { destination = it }) {
                    if (destination == AppDestination.REPORTE) ReporteVentasScreen(model, onBack = { destination = AppDestination.INGRESO }, onLogout = {})
                    else Text("fixture")
                }
            }
        }
        val requestStarted = SystemClock.elapsedRealtime()
        rule.onNodeWithText("Ventas").performClick()
        rule.onNodeWithContentDescription("Seleccionar fecha inicial").performClick()
        selectDay(1)
        rule.onNodeWithText("Aceptar").performClick()
        rule.onNodeWithContentDescription("Seleccionar fecha final").performClick()
        selectDay(8)
        rule.onNodeWithText("Aceptar").performClick()
        val samples = mutableListOf<Long>()
        var requestMillis = 0L
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        repeat(20) { index ->
            rule.onNodeWithTag("report_list").performScrollToNode(hasText("Consultar"))
            val started = SystemClock.elapsedRealtime()
            rule.onNodeWithText("Consultar").performClick()
            if (index == 0) requestMillis = SystemClock.elapsedRealtime() - requestStarted
            rule.waitUntil(30_000) { model.state.status == ReportStatus.RESULTS }
            val page = model.state.page!!
            assertEquals(1000L, page.count)
            assertEquals(0, page.total.compareTo("1152.90".toBigDecimal()))
            val reference = context.getString(R.string.report_reference, page.items.first().id, page.items.first().turnId)
            rule.onNodeWithTag("report_list").performScrollToNode(hasText(reference))
            rule.onNodeWithText(context.getString(R.string.report_count, 1000L)).assertIsDisplayed()
            rule.onNodeWithText(reference).assertIsDisplayed()
            rule.waitForIdle()
            samples += SystemClock.elapsedRealtime() - started
        }
        assertTrue("Open, dates and request must take less than 30s", requestMillis < 30_000)
        assertTrue("At least 19/20 rendered queries must take less than 5s", samples.count { it < 5_000 } >= 19)
        val directory = File(arguments.getString("additionalTestOutputDir") ?: context.getExternalFilesDir(null)!!.path)
        directory.mkdirs()
        File(directory, "report-metrics.txt").writeText("Device=${Build.MODEL}; API=${Build.VERSION.SDK_INT}; network=adb reverse loopback; collections=1000\nSC-001 requestMs=$requestMillis\nSC-005 renderMs=${samples.joinToString(",")}; under5s=${samples.count { it < 5_000 }}/20\n")
        assertTrue(runBlocking { auth.logout(session) })
        rule.runOnIdle { model.clear() }
    }

    private fun selectDay(day: Int) {
        val locale = Locale.getDefault()
        val description = LocalDate.of(2026, 11, day).format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "yMMMMEEEEd"), locale))
        rule.onNode(hasText(description, substring = true) or hasContentDescription(description, substring = true)).performClick()
    }
}
package com.carwash.mobile

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.carwash.mobile.auth.Session
import com.carwash.mobile.auth.AndroidSessionStore
import android.content.Context
import com.carwash.mobile.reportes.*
import com.carwash.mobile.ui.theme.CarWashTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.TimeZone
import java.io.File
import java.io.FileOutputStream
import android.graphics.Bitmap
import android.view.accessibility.AccessibilityNodeInfo

@RunWith(AndroidJUnit4::class)
class ReporteVentasScreenTest {
    @get:Rule val rule = createComposeRule()

    private class FakeRepo(val empty: Boolean = false) : ReporteVentasRepository {
        val requests = mutableListOf<ReportCriteria>()
        var behavior: ((ReportCriteria, String?) -> SalesPage)? = null
        override suspend fun options(initialDate: LocalDate, finalDate: LocalDate) = ReportOptions(initialDate, finalDate,
            listOf(FilterOption(2, "Credito")), if (finalDate.dayOfMonth == 4) emptyList() else listOf(FilterOption(2, "Operario historico")),
            listOf(FilterOption(2, "Moto")), LocalDate.parse("2026-11-03"))
        override suspend fun sales(criteria: ReportCriteria, cursor: String?): SalesPage {
            requests += criteria
            behavior?.let { return it(criteria, cursor) }
            return SalesPage(criteria, if (empty) 0 else 1, if (empty) BigDecimal.ZERO else BigDecimal("20.20"),
                if (empty) emptyList() else listOf(CollectionItem(2, 10, OffsetDateTime.parse("2026-11-03T12:00:00-05:00"),
                    "ABC123", 1, "Automovil", null, null, 2, "Credito", BigDecimal("20.20"))),
                null, 50, Instant.parse("2026-11-03T12:00:00Z"), LocalDate.parse("2026-11-03"))
        }
    }

    private fun show(repo: FakeRepo, admin: Boolean = true, date: String? = "2026-11-03", dark: Boolean = false, fontScale: Float = 1f, compact: Boolean = false): ReporteVentasViewModel {
        val model = ReporteVentasViewModel(repo, Session(1, "U", admin, "http://fixture/", "A".repeat(43), serverDate = date))
        rule.setContent {
            val density = LocalDensity.current
            val configuration = Configuration(LocalConfiguration.current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale), LocalConfiguration provides configuration) {
                CarWashTheme {
                    Box(if (compact) Modifier.size(360.dp, 320.dp) else Modifier.fillMaxSize()) {
                        ReporteVentasScreen(model, onBack = {}, onLogout = {})
                    }
                }
            }
        }
        return model
    }

    @Test fun datesAreRequiredWhenBusinessDayIsUnknown() {
        val repo = FakeRepo()
        show(repo, date = null)
        rule.onNodeWithText("Consultar").performClick()
        rule.onNodeWithText("Seleccione la fecha inicial.").assertExists()
        rule.onNodeWithText("Seleccione la fecha final.").assertExists()
        assertEquals(0, repo.requests.size)
    }

    @Test fun datesOnlyShowGlobalSummaryAndCollectionDetail() {
        val repo = FakeRepo()
        show(repo)
        rule.onNodeWithText("Consultar").performClick()
        rule.waitUntil(5_000) { repo.requests.isNotEmpty() }
        rule.onNodeWithText("ABC123").assertExists()
        rule.onNodeWithText("Credito").assertExists()
        rule.onNodeWithText("Sin informar").assertExists()
        assertEquals(ReportCriteria(LocalDate.parse("2026-11-03"), LocalDate.parse("2026-11-03")), repo.requests.single())
    }

    @Test fun emptyResponseIsDifferentFromNotConsulted() {
        val repo = FakeRepo(empty = true)
        show(repo)
        rule.onNodeWithText("Sin resultados").assertDoesNotExist()
        rule.onNodeWithText("Consultar").performClick()
        rule.waitUntil(5_000) { repo.requests.isNotEmpty() }
        rule.onNodeWithText("Sin resultados").assertExists()
    }

    @Test fun nonAdministratorCannotQueryEvenIfScreenIsOpenedDirectly() {
        val repo = FakeRepo()
        show(repo, admin = false)
        rule.onNodeWithText("Solo el administrador puede consultar ventas.").assertExists()
        rule.onNodeWithText("Consultar").assertDoesNotExist()
        assertEquals(0, repo.requests.size)
    }

    @Test fun administratorCanSwitchBetweenThreeExplicitDestinations() {
        val repo = FakeRepo()
        val model = ReporteVentasViewModel(repo, Session(1, "U", true, "http://fixture/", "A".repeat(43), serverDate = "2026-11-03"))
        rule.setContent {
            var destination by remember { mutableStateOf(AppDestination.INGRESO) }
            CarWashTheme {
                AppNavigation(true, destination, { destination = it }) {
                    when (destination) {
                        AppDestination.INGRESO -> Text("form-fixture")
                        AppDestination.VEHICULOS -> Text("vehicles-fixture")
                        AppDestination.REPORTE -> ReporteVentasScreen(model, onBack = { destination = AppDestination.INGRESO }, onLogout = {})
                    }
                }
            }
        }
        rule.onNodeWithText("Vehículos").performClick()
        rule.onNodeWithText("vehicles-fixture").assertExists()
        rule.onNodeWithText("Ventas").performClick()
        rule.onNodeWithText("Consultar").performClick()
        rule.waitUntil(5_000) { repo.requests.isNotEmpty() }
        rule.onNodeWithText("Ingreso").performClick()
        rule.onNodeWithText("form-fixture").assertExists()
    }

    @Test fun nonAdministratorNavigationHasNoSalesDestination() {
        rule.setContent { CarWashTheme { AppNavigation(false, AppDestination.INGRESO, {}) { Text("form-fixture") } } }
        rule.onNodeWithText("Ingreso").assertExists()
        rule.onNodeWithText("Vehículos").assertExists()
        rule.onNodeWithText("Ventas").assertDoesNotExist()
    }

    @Test fun fourOptionalFiltersIntersectAndClearingKeepsDates() {
        val repo = FakeRepo()
        val model = show(repo)
        rule.onNodeWithText("Filtros opcionales").performClick()
        rule.onNodeWithText("Forma de pago").performScrollTo().performClick()
        rule.onNodeWithText("#2", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Credito").performClick()
        rule.onNodeWithText("Credito").assertIsDisplayed()
        rule.onNodeWithText("Operario").performScrollTo().performClick()
        rule.onNodeWithText("#2", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Operario historico").performClick()
        rule.onNodeWithText("Operario historico").assertIsDisplayed()
        rule.onNodeWithText("Tipo de vehículo").performScrollTo().performClick()
        rule.onNodeWithText("#2", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Moto").performClick()
        rule.onNodeWithText("Moto").assertIsDisplayed()
        rule.onNodeWithTag("report_plate").performScrollTo().performTextInput(" abc123 ")
        rule.onNodeWithText("Consultar").performScrollTo().performClick()
        rule.waitUntil(5_000) { repo.requests.isNotEmpty() }
        assertEquals(ReportCriteria(LocalDate.parse("2026-11-03"), LocalDate.parse("2026-11-03"), 2, 2, "ABC123", 2), repo.requests.single())
        rule.onNodeWithText("Limpiar filtros").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(ReportCriteria(LocalDate.parse("2026-11-03"), LocalDate.parse("2026-11-03")), model.state.edited) }
        assertEquals(1, repo.requests.size)
    }

    @Test fun selectedOperatorIsKeptWhenNewPeriodHasNoMatchingOption() {
        val model = show(FakeRepo())
        rule.onNodeWithText("Filtros opcionales").performClick()
        rule.onNodeWithText("Operario").performScrollTo().performClick()
        rule.onNodeWithText("Operario historico").performClick()
        rule.runOnIdle { model.edit(model.state.edited.copy(finalDate = LocalDate.parse("2026-11-04"))) }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(2, model.state.edited.operatorId) }
        rule.onNodeWithText("Limpiar filtros").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(LocalDate.parse("2026-11-04"), model.state.edited.finalDate) }
    }

    private fun response(criteria: ReportCriteria, id: Int = 2, count: Long = 2, next: String? = "cursor1") =
        SalesPage(criteria, count, BigDecimal("40.40"), listOf(CollectionItem(id, 10, OffsetDateTime.parse("2026-11-03T12:00:00-05:00"),
            "ABC123", 1, "Automovil", 1, "Operario", 2, "Credito", BigDecimal("20.20"))), next, 50,
            Instant.parse("2026-11-03T12:00:00Z"), LocalDate.parse("2026-11-03"))

    @Test fun errorIsNotEmptyAndRetryRecovers() {
        val repo = FakeRepo().apply { behavior = { _, _ -> throw ReportError(ReportErrorKind.UNAVAILABLE) } }
        val model = show(repo)
        rule.onNodeWithText("Consultar").performClick()
        rule.onNodeWithText("No se pudo consultar el reporte. Intente de nuevo.").performScrollTo().assertExists()
        rule.onNodeWithText("Sin resultados").assertDoesNotExist()
        rule.runOnIdle { repo.behavior = null }
        rule.onNodeWithText("Reintentar").performScrollTo().performClick()
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(ReportStatus.RESULTS, model.state.status) }
    }

    @Test fun editingMarksPreviousResultsWithoutChangingAppliedCriteria() {
        val repo = FakeRepo()
        val model = show(repo)
        rule.onNodeWithText("Consultar").performClick()
        rule.waitForIdle()
        rule.runOnIdle { model.edit(model.state.edited.copy(plate = "OTHER")) }
        rule.onNodeWithText("Resultados anteriores").performScrollTo().assertExists()
        rule.onNodeWithText("Período: 2026-11-03 a 2026-11-03").assertExists()
        rule.runOnIdle { assertEquals(null, model.state.page!!.criteria.plate) }
        assertEquals(1, repo.requests.size)
    }

    @Test fun failedContinuationIsPartialAndRetryCompletesWithoutFalseZero() {
        val repo = FakeRepo().apply { behavior = { criteria, cursor ->
            if (cursor != null) throw ReportError(ReportErrorKind.NETWORK)
            response(criteria)
        } }
        val model = show(repo)
        rule.onNodeWithText("Consultar").performClick()
        rule.onNodeWithText("Cargar más").performScrollTo().performClick()
        rule.onNodeWithText("Recorrido incompleto").performScrollTo().assertExists()
        rule.runOnIdle { assertEquals(listOf(2), model.state.page!!.items.map { it.id }); assertEquals(2, model.state.page!!.count) }
        rule.runOnIdle { repo.behavior = { criteria, _ -> response(criteria, id = 3, next = null) } }
        rule.onNodeWithText("Reintentar").performScrollTo().performClick()
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(listOf(2, 3), model.state.page!!.items.map { it.id }); assertEquals(2, model.state.page!!.count) }
    }

    @Test fun changedSummaryOffersRefreshAndDoesNotMixNewPage() {
        val repo = FakeRepo().apply { behavior = { criteria, cursor -> response(criteria, id = if (cursor == null) 2 else 3, count = if (cursor == null) 2 else 3) } }
        val model = show(repo)
        rule.onNodeWithText("Consultar").performClick()
        rule.onNodeWithText("Cargar más").performScrollTo().performClick()
        rule.onNodeWithText("Los cobros cambiaron durante la consulta.").performScrollTo().assertExists()
        rule.runOnIdle { assertEquals(listOf(2), model.state.page!!.items.map { it.id }) }
        rule.onNodeWithText("Actualizar").performScrollTo().performClick()
        rule.waitForIdle()
        assertEquals(3, repo.requests.size)
    }

    @Test fun invalidCursorOffersRefreshInsteadOfRepeatingInvalidContinuation() {
        val repo = FakeRepo().apply { behavior = { criteria, cursor ->
            if (cursor != null) throw ReportError(ReportErrorKind.INVALID_CURSOR)
            response(criteria)
        } }
        show(repo)
        rule.onNodeWithText("Consultar").performClick()
        rule.onNodeWithText("Cargar más").performScrollTo().performClick()
        rule.onNodeWithText("Actualizar").performScrollTo().performClick()
        rule.waitForIdle()
        assertEquals(3, repo.requests.size)
    }

    @Test fun unauthorizedResponseClearsDetailWithoutOfferingReportLogin() {
        val repo = FakeRepo().apply { behavior = { _, _ -> throw ReportError(ReportErrorKind.SESSION_REQUIRED) } }
        val model = show(repo)
        rule.onNodeWithText("Consultar").performClick()
        rule.onNodeWithText("La sesión ha finalizado.").performScrollTo().assertExists()
        rule.onNodeWithText("Reingresar para consultar ventas").assertDoesNotExist()
        rule.runOnIdle { assertEquals(null, model.state.page) }
    }

    @Test fun forbiddenResponseRemovesQueryControlsAndDetail() {
        val repo = FakeRepo().apply { behavior = { _, _ -> throw ReportError(ReportErrorKind.ADMIN_REQUIRED) } }
        val model = show(repo)
        rule.onNodeWithText("Consultar").performClick()
        rule.onNodeWithText("Solo el administrador puede consultar ventas.").assertExists()
        rule.onNodeWithText("Consultar").assertDoesNotExist()
        rule.runOnIdle { assertEquals(null, model.state.page) }
    }

    @Test fun largeFontCompactDarkScreenHasReadableTitleAndScrollableQuery() {
        show(FakeRepo(), dark = true, fontScale = 2f, compact = true)
        val layouts = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText("Reporte de ventas").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertFalse("Report title must not be clipped at large font scale", layouts.single().hasVisualOverflow)
        rule.onNodeWithContentDescription("Seleccionar fecha inicial").assertExists()
        rule.onNodeWithText("Consultar").performScrollTo().performClick()
        rule.onNodeWithTag("report_list").performScrollToNode(hasText("Cobros: 1"))
        rule.onNodeWithText("Cobros: 1").assertIsDisplayed()
        saveScreenshot("report-dark-compact-font2")
    }

    @Test fun lightPortraitHasAccessibleCalendarAndCollectionReferences() {
        show(FakeRepo())
        rule.onNodeWithContentDescription("Seleccionar fecha inicial").assertExists()
        rule.onNodeWithContentDescription("Seleccionar fecha final").assertExists()
        rule.onNodeWithContentDescription("Volver al ingreso").assertExists()
        rule.onNodeWithContentDescription("Cerrar sesión").assertExists()
        rule.onNodeWithText("Consultar").performClick()
        rule.onNodeWithTag("report_list").performScrollToNode(hasText("Movimiento 2 · Turno 10"))
        rule.onNodeWithText("Movimiento 2 · Turno 10").assertIsDisplayed()
        val orientation = InstrumentationRegistry.getInstrumentation().targetContext.resources.configuration.orientation
        saveScreenshot(if (orientation == Configuration.ORIENTATION_LANDSCAPE) "report-light-landscape" else "report-light-portrait")
    }

    @Test fun calendarKeepsBothDatesInAnotherPhoneTimeZone() {
        val previous = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"))
            val model = show(FakeRepo())
            rule.runOnIdle { model.edit(ReportCriteria(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-03"))) }
            rule.onNodeWithContentDescription("Seleccionar fecha inicial").performClick()
            rule.onNodeWithText("Aceptar").performClick()
            rule.onNodeWithContentDescription("Seleccionar fecha final").performClick()
            rule.onNodeWithText("Aceptar").performClick()
            rule.runOnIdle { assertEquals(ReportCriteria(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-03")), model.state.edited) }
        } finally { TimeZone.setDefault(previous) }
    }

    @Test fun androidAccessibilityTreeExposesCalendarAndQueryActions() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        show(FakeRepo())
        rule.waitForIdle()
        fun nodes(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> = buildList {
            add(root)
            for (index in 0 until root.childCount) root.getChild(index)?.let { addAll(nodes(it)) }
        }
        rule.waitUntil(5_000) {
            automation.rootInActiveWindow?.let { root -> nodes(root).any { it.contentDescription?.toString() == "Seleccionar fecha inicial" } } == true
        }
        val actions = nodes(automation.rootInActiveWindow!!)
        fun clickable(node: AccessibilityNodeInfo): Boolean = node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK }
            || node.parent?.let(::clickable) == true
        assertTrue("Initial calendar needs an accessible click action", actions.any { it.contentDescription?.toString() == "Seleccionar fecha inicial" && clickable(it) })
        assertTrue("Final calendar needs an accessible click action", actions.any { it.contentDescription?.toString() == "Seleccionar fecha final" && clickable(it) })
        assertTrue("Query needs an accessible click action", actions.any { it.text?.toString() == "Consultar" && clickable(it) })
        rule.onNodeWithContentDescription("Seleccionar fecha inicial").assertIsDisplayed()
        rule.onNodeWithContentDescription("Seleccionar fecha final").assertIsDisplayed()
    }

    @Test fun sessionPreferencesKeepOnlyMetadataAndRemoveStaleSensitiveKeys() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("carwash_session", Context.MODE_PRIVATE)
        preferences.edit().putString("accessToken", "old-fixture-token").putString("password", "old-fixture-password")
            .putString("sales", "old-fixture-results").commit()
        val store = AndroidSessionStore(context)
        try {
            store.save(Session(1, "Admin", true, "http://fixture/", "A".repeat(43), serverDate = "2026-11-03"))
            assertEquals(setOf("userId", "displayName", "isAdmin", "baseUrl"), preferences.all.keys)
            assertEquals(null, store.load()!!.accessToken)
            assertEquals(null, store.load()!!.serverDate)
            assertFalse(preferences.all.values.any { it == "A".repeat(43) })
        } finally { store.save(null) }
        assertTrue(preferences.all.isEmpty())
    }

    private fun saveScreenshot(name: String) {
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val output = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
            ?: context.getExternalFilesDir(null)!!.absolutePath
        val folder = File(output, "report-screenshots").apply { mkdirs() }
        FileOutputStream(File(folder, "$name.png")).use { stream -> assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) }
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        assertTrue("Rendered report must not be blank", pixels.any { it != pixels[0] })
    }
}
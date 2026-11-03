package com.carwash.mobile.reportes

import com.carwash.mobile.auth.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class ReporteVentasViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val date = LocalDate.parse("2026-11-03")
    private val session = Session(1, "Admin", true, "http://fixture/", "A".repeat(43), serverDate = date.toString())
    private val criteria = ReportCriteria(date, date)
    private class FakeRepo : ReporteVentasRepository {
        val requests = mutableListOf<ReportCriteria>()
        var error: ReportError? = null
        var optionsError: ReportError? = null
        override suspend fun options(initialDate: LocalDate, finalDate: LocalDate): ReportOptions {
            optionsError?.let { throw it }
            return ReportOptions(initialDate, finalDate, emptyList(), emptyList(), emptyList(), finalDate)
        }
        override suspend fun sales(criteria: ReportCriteria, cursor: String?): SalesPage {
            requests += criteria
            error?.let { throw it }
            return SalesPage(criteria, 0, BigDecimal.ZERO, emptyList(), null, 50, Instant.parse("2026-11-03T12:00:00Z"), criteria.finalDate!!)
        }
    }

    private class ControlledRepo : ReporteVentasRepository {
        data class Request(val criteria: ReportCriteria, val cursor: String?, val response: CompletableDeferred<SalesPage> = CompletableDeferred())
        val requests = mutableListOf<Request>()
        override suspend fun sales(criteria: ReportCriteria, cursor: String?): SalesPage {
            val request = Request(criteria, cursor)
            requests += request
            return withContext(NonCancellable) { request.response.await() }
        }
    }

    private fun page(applied: ReportCriteria = criteria, ids: List<Int> = listOf(1), count: Long = 2, total: String = "0.30", next: String? = "next") =
        SalesPage(applied, count, BigDecimal(total), ids.map { id ->
            CollectionItem(id, 10, OffsetDateTime.parse("2026-11-03T12:00:00Z"), "ABC123", 1, "Automovil", 1, "Operario", 1, "Efectivo", BigDecimal("0.10").multiply(BigDecimal(id)))
        }, next, 50, Instant.parse("2026-11-03T12:00:00Z"), date)

    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun cleanup() = Dispatchers.resetMain()

    @Test fun serverBusinessDayInitializesDatesAndOnlyConsultSendsRequest() = runTest(dispatcher) {
        val repo = FakeRepo()
        val model = ReporteVentasViewModel(repo, session)
        assertEquals(criteria, model.state.edited)
        assertEquals(ReportStatus.NOT_QUERIED, model.state.status)
        assertTrue(repo.requests.isEmpty())
        model.consult()
        advanceUntilIdle()
        assertEquals(listOf(criteria), repo.requests)
        assertEquals(ReportStatus.EMPTY, model.state.status)
        assertEquals(BigDecimal.ZERO, model.state.page!!.total)
    }

    @Test fun missingOrReversedDatesDoNotQuery() = runTest(dispatcher) {
        val repo = FakeRepo()
        val model = ReporteVentasViewModel(repo, session.copy(serverDate = null))
        model.consult()
        assertEquals(setOf("fechaInicial", "fechaFinal"), model.state.fieldErrors)
        model.edit(ReportCriteria(date, date.minusDays(1)))
        model.consult()
        advanceUntilIdle()
        assertEquals(setOf("fechaFinal"), model.state.fieldErrors)
        assertTrue(repo.requests.isEmpty())
    }

    @Test fun errorsAndNonAdministratorNeverBecomeEmptySuccess() = runTest(dispatcher) {
        val repo = FakeRepo().apply { error = ReportError(ReportErrorKind.UNAVAILABLE) }
        val model = ReporteVentasViewModel(repo, session)
        model.consult()
        advanceUntilIdle()
        assertEquals(ReportStatus.ERROR, model.state.status)
        assertNull(model.state.page)
        val nonAdmin = ReporteVentasViewModel(repo, session.copy(isAdmin = false))
        nonAdmin.consult()
        advanceUntilIdle()
        assertEquals(1, repo.requests.size)
        assertEquals(ReportStatus.FORBIDDEN, nonAdmin.state.status)
    }

    @Test fun optionalSelectionSurvivesNewCatalogAndClearKeepsDatesWithoutSalesQuery() = runTest(dispatcher) {
        val repo = FakeRepo()
        val model = ReporteVentasViewModel(repo, session)
        model.edit(criteria.copy(paymentMethodId = 2, operatorId = 7, plate = "ABC123", vehicleTypeId = 2))
        model.loadOptions()
        advanceUntilIdle()
        model.edit(model.state.edited.copy(finalDate = date.plusDays(1)))
        model.loadOptions()
        advanceUntilIdle()
        assertEquals(7, model.state.edited.operatorId)
        assertTrue(model.state.options!!.operators.isEmpty())
        model.clearFilters()
        assertEquals(ReportCriteria(date, date.plusDays(1)), model.state.edited)
        assertTrue(repo.requests.isEmpty())
    }

    @Test fun optionsFailureDoesNotPreventDatesOnlySalesQuery() = runTest(dispatcher) {
        val model = ReporteVentasViewModel(FakeRepo().apply { optionsError = ReportError(ReportErrorKind.UNAVAILABLE) }, session)
        model.loadOptions()
        advanceUntilIdle()
        assertEquals(ReportErrorKind.UNAVAILABLE, model.state.optionsError)
        model.consult()
        advanceUntilIdle()
        assertEquals(ReportStatus.EMPTY, model.state.status)
    }

    @Test fun lateResponseCannotReplaceNewCriteriaEvenWhenRepositoryIgnoresCancellation() = runTest(dispatcher) {
        val repo = ControlledRepo()
        val model = ReporteVentasViewModel(repo, session)
        model.consult()
        runCurrent()
        val newer = ReportCriteria(date.plusDays(1), date.plusDays(1))
        model.edit(newer)
        model.consult()
        runCurrent()
        repo.requests[1].response.complete(page(newer, listOf(2), next = null))
        runCurrent()
        repo.requests[0].response.complete(page(ids = listOf(1)))
        advanceUntilIdle()
        assertEquals(newer, model.state.page!!.criteria)
        assertEquals(listOf(2), model.state.page!!.items.map { it.id })
    }

    @Test fun oneContinuationAtATimeAndRetryKeepsCursorWithoutAccumulatingSummary() = runTest(dispatcher) {
        val repo = ControlledRepo()
        val model = ReporteVentasViewModel(repo, session)
        model.consult()
        runCurrent()
        repo.requests[0].response.complete(page())
        runCurrent()
        model.loadMore()
        model.loadMore()
        runCurrent()
        assertEquals(2, repo.requests.size)
        repo.requests[1].response.completeExceptionally(ReportError(ReportErrorKind.NETWORK))
        runCurrent()
        assertTrue(model.state.partial)
        assertEquals(listOf(1), model.state.page!!.items.map { it.id })
        model.retry()
        runCurrent()
        assertEquals("next", repo.requests[2].cursor)
        assertEquals(criteria, repo.requests[2].criteria)
        repo.requests[2].response.complete(page(ids = listOf(1, 2), next = null))
        advanceUntilIdle()
        assertEquals(listOf(1, 2), model.state.page!!.items.map { it.id })
        assertEquals(2, model.state.page!!.count)
        assertEquals(BigDecimal("0.30"), model.state.page!!.total)
        assertFalse(model.state.partial)
        assertFalse(model.state.loadingMore)
    }

    @Test fun changedAggregatesKeepOldRowsAndRequireRefreshFromFirstPage() = runTest(dispatcher) {
        val repo = ControlledRepo()
        val model = ReporteVentasViewModel(repo, session)
        model.consult()
        runCurrent()
        repo.requests[0].response.complete(page())
        runCurrent()
        model.loadMore()
        runCurrent()
        repo.requests[1].response.complete(page(ids = listOf(2), count = 3, total = "0.40", next = null))
        runCurrent()
        assertEquals(ReportStatus.DATA_CHANGED, model.state.status)
        assertEquals(listOf(1), model.state.page!!.items.map { it.id })
        assertEquals(BigDecimal("0.30"), model.state.page!!.total)
        model.loadMore()
        assertEquals(2, repo.requests.size)
        model.refresh()
        runCurrent()
        assertNull(repo.requests[2].cursor)
        repo.requests[2].response.complete(page(ids = listOf(1, 2, 3), count = 3, total = "0.40", next = null))
        advanceUntilIdle()
        assertEquals(3, model.state.page!!.count)
    }

    @Test fun editingDoesNotQueryOrRelabelAppliedCriteriaAndRetryUsesOriginalCriteria() = runTest(dispatcher) {
        val repo = ControlledRepo()
        val model = ReporteVentasViewModel(repo, session)
        model.consult()
        runCurrent()
        repo.requests[0].response.completeExceptionally(ReportError(ReportErrorKind.UNAVAILABLE))
        runCurrent()
        model.edit(criteria.copy(plate = "OTHER"))
        assertEquals(1, repo.requests.size)
        model.retry()
        runCurrent()
        assertEquals(criteria, repo.requests[1].criteria)
        repo.requests[1].response.complete(page(next = null))
        advanceUntilIdle()
        assertEquals(criteria, model.state.page!!.criteria)
        assertTrue(model.state.previousResults)
        assertEquals("OTHER", model.state.edited.plate)
    }

    @Test fun authorizationFailuresClearRowsOptionsAndTemporaryCredential() = runTest(dispatcher) {
        for (error in listOf(ReportErrorKind.SESSION_REQUIRED, ReportErrorKind.ADMIN_REQUIRED)) {
            val repo = ControlledRepo()
            val model = ReporteVentasViewModel(repo, session)
            model.consult()
            runCurrent()
            repo.requests[0].response.complete(page())
            runCurrent()
            model.loadMore()
            runCurrent()
            repo.requests[1].response.completeExceptionally(ReportError(error))
            advanceUntilIdle()
            assertNull(model.state.page)
            assertNull(model.state.options)
            assertEquals(if (error == ReportErrorKind.SESSION_REQUIRED) ReportStatus.SESSION_REQUIRED else ReportStatus.FORBIDDEN, model.state.status)
            if (error == ReportErrorKind.ADMIN_REQUIRED) assertFalse(model.canQuery)
        }
    }

    @Test fun logoutAndNewIdentityDiscardLateResultsAndOptionalSelections() = runTest(dispatcher) {
        val previous = ControlledRepo()
        val current = ControlledRepo()
        val model = ReporteVentasViewModel(previous, session)
        model.edit(criteria.copy(plate = "PRIVATE"))
        model.consult()
        runCurrent()
        model.clear()
        assertNull(model.state.page)
        assertNull(model.state.edited.plate)
        model.bind(session.copy(userId = 2, baseUrl = "http://other-fixture/"), current)
        model.consult()
        runCurrent()
        current.requests[0].response.complete(page(ids = listOf(2), next = null))
        runCurrent()
        previous.requests[0].response.complete(page(criteria.copy(plate = "PRIVATE")))
        advanceUntilIdle()
        assertEquals(listOf(2), model.state.page!!.items.map { it.id })
        assertNull(model.state.edited.plate)
        assertTrue(model.ownsSession(session.copy(userId = 2, baseUrl = "http://other-fixture/")))
        assertFalse(model.ownsSession(session))
    }

    @Test fun reentryKeepsFiltersButFreshLoginAfterLogoutInitializesBusinessDay() = runTest(dispatcher) {
        val repo = FakeRepo().apply { error = ReportError(ReportErrorKind.SESSION_REQUIRED) }
        val model = ReporteVentasViewModel(repo, session)
        val draft = criteria.copy(initialDate = date.minusDays(1), operatorId = 7)
        model.edit(draft)
        model.consult()
        advanceUntilIdle()
        model.bind(session.copy(accessToken = "B".repeat(42) + "A"), FakeRepo())
        assertEquals(draft, model.state.edited)
        model.clear()
        model.bind(session.copy(accessToken = "C".repeat(42) + "A"), FakeRepo())
        assertEquals(criteria, model.state.edited)
        assertNull(model.state.page)
    }
}
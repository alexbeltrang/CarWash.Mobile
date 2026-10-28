package com.carwash.mobile.ingreso

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class IngresoViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun typeChangeClearsSelectionAndTotals() = runTest(dispatcher) {
        val model = model(FakeRepository())
        runCurrent()
        model.selectType(3)
        model.toggleService(1)
        assertEquals("20000.00", model.state.form.value)
        model.selectType(1)
        assertTrue(model.state.form.serviceIds.isEmpty())
        assertEquals("0.00", model.state.form.value)
    }

    @Test
    fun invalidFormNeverCallsRegister() = runTest(dispatcher) {
        val source = FakeRepository()
        val model = model(source)
        runCurrent()
        model.save()
        runCurrent()
        assertEquals(0, source.calls)
        assertTrue(FormError.PLATE in model.state.errors)
    }

    @Test
    fun doubleTapSendsOnceAndClearsOnlyAfterCompletion() = runTest(dispatcher) {
        val source = FakeRepository()
        val gate = CompletableDeferred<Unit>()
        source.saveGate = gate
        val model = model(source)
        runCurrent()
        fill(model)
        model.save()
        model.save()
        runCurrent()
        assertEquals(1, source.calls)
        assertTrue(model.state.saving)
        assertEquals("ABC123", model.state.form.plate)
        gate.complete(Unit)
        runCurrent()
        assertEquals(IngresoForm(), model.state.form)
        assertEquals(NoticeKind.SAVED, model.state.notice?.kind)
    }

    @Test
    fun editingPlateCancelsStaleLookup() = runTest(dispatcher) {
        val source = FakeRepository()
        val gate = CompletableDeferred<PlateLookup>()
        source.lookupGate = gate
        val model = model(source)
        runCurrent()
        model.edit { it.copy(plate = "ABC123") }
        model.lookup()
        runCurrent()
        model.edit { it.copy(plate = "XYZ789") }
        gate.complete(PlateLookup(VehicleInfo(3, "ANTERIOR", null, null), emptyList()))
        runCurrent()
        assertEquals("XYZ789", model.state.form.plate)
        assertEquals("", model.state.form.customer)
        assertFalse(model.state.searching)
    }

    @Test
    fun partialSaveRetainsDataAndBlocksResubmissionAcrossRestart() = runTest(dispatcher) {
        val source = FakeRepository().apply { failAfterWrite = true }
        val settings = MemorySettings()
        val model = IngresoViewModel(settings) { source }
        runCurrent()
        fill(model)
        model.save()
        runCurrent()
        assertTrue(model.state.reviewRequired)
        assertEquals("ABC123", model.state.form.plate)
        model.save()
        runCurrent()
        assertEquals(1, source.calls)
        val restarted = IngresoViewModel(settings) { source }
        runCurrent()
        assertTrue(restarted.state.reviewRequired)
        restarted.clear()
        assertFalse(restarted.state.reviewRequired)
        assertNull(settings.pendingTurn)
    }

    @Test
    fun failedConnectionDoesNotKeepCatalogsFromAnotherServer() = runTest(dispatcher) {
        val source = FakeRepository()
        val model = model(source)
        runCurrent()
        assertTrue(model.state.connected)
        source.failLoad = true
        model.connect("http://192.168.1.15:5197")
        runCurrent()
        assertFalse(model.state.connected)
        assertTrue(model.state.catalog.types.isEmpty())
    }

    @Test
    fun confirmedCameraPlateLooksUpWithoutRegistering() = runTest(dispatcher) {
        val source = FakeRepository()
        val model = model(source)
        runCurrent()
        model.edit { it.copy(order = "42", observations = "CONSERVAR") }
        model.acceptScannedPlate(" aoi012 ")
        runCurrent()
        assertEquals("AOI012", model.state.form.plate)
        assertEquals("AOI012", source.lastLookup)
        assertEquals("42", model.state.form.order)
        assertEquals("CONSERVAR", model.state.form.observations)
        assertEquals(0, source.calls)
    }

    @Test
    fun cameraConfirmationWorksOfflineAndDoesNotAcceptBlank() = runTest(dispatcher) {
        val source = FakeRepository()
        val model = IngresoViewModel(MemorySettings().apply { serverUrl = "" }) { source }
        model.acceptScannedPlate("xyz789")
        model.acceptScannedPlate(" ")
        assertEquals("XYZ789", model.state.form.plate)
        assertNull(source.lastLookup)
        assertEquals(0, source.calls)
    }

    @Test
    fun cameraCannotReplacePlateWhileSavingOrReviewIsRequired() = runTest(dispatcher) {
        val source = FakeRepository()
        val gate = CompletableDeferred<Unit>()
        source.saveGate = gate
        source.failAfterWrite = true
        val model = model(source)
        runCurrent()
        fill(model)
        model.save()
        runCurrent()
        model.acceptScannedPlate("XYZ789")
        assertEquals("ABC123", model.state.form.plate)
        gate.complete(Unit)
        runCurrent()
        assertTrue(model.state.reviewRequired)
        model.acceptScannedPlate("XYZ789")
        assertEquals("ABC123", model.state.form.plate)
        assertNull(source.lastLookup)
        assertEquals(1, source.calls)
    }

    @Test
    fun confirmingDifferentPlateDoesNotKeepPreviousVehicleWhenLookupFails() = runTest(dispatcher) {
        val source = FakeRepository()
        val model = model(source)
        runCurrent()
        fill(model)
        model.edit { it.copy(customer = "ANTERIOR", phone = "300", brand = "ANTERIOR") }
        val gate = CompletableDeferred<PlateLookup>()
        source.lookupGate = gate
        model.acceptScannedPlate("XYZ789")
        runCurrent()
        gate.completeExceptionally(IOException())
        runCurrent()
        assertEquals("XYZ789", model.state.form.plate)
        assertEquals("42", model.state.form.order)
        assertEquals("", model.state.form.customer)
        assertEquals("", model.state.form.phone)
        assertEquals("", model.state.form.brand)
        assertNull(model.state.form.typeId)
        assertTrue(model.state.form.serviceIds.isEmpty())
        assertEquals("0.00", model.state.form.value)
        assertEquals(NoticeKind.LOOKUP_FAILED, model.state.notice?.kind)
        assertEquals(0, source.calls)
    }

    private fun model(source: FakeRepository) = IngresoViewModel(MemorySettings()) { source }

    private fun fill(model: IngresoViewModel) {
        model.edit { it.copy(plate = "ABC123", order = "42") }
        model.selectType(3)
        model.toggleService(1)
    }

    private class MemorySettings : ServerSettings {
        override var serverUrl = "http://10.0.2.2:5197/"
        override var pendingTurn: String? = null
    }

    private class FakeRepository : IngresoRepository {
        var calls = 0
        var lastLookup: String? = null
        var failLoad = false
        var failAfterWrite = false
        var saveGate: CompletableDeferred<Unit>? = null
        var lookupGate: CompletableDeferred<PlateLookup>? = null

        override suspend fun load(): Catalog {
            if (failLoad) throw IOException()
            return Catalog(
                listOf(VehicleType(3, "Automoviles"), VehicleType(1, "Moto")),
                listOf(VehicleService(1, "Lavado", 3, BigDecimal("20000"), BigDecimal("15000"))),
                CashRegister(30, true),
            )
        }

        override suspend fun lookup(plate: String): PlateLookup {
            lastLookup = plate
            return lookupGate?.await() ?: PlateLookup(null, emptyList())
        }

        override suspend fun register(form: IngresoForm, beforeWrite: (String) -> Unit): SavedTurn {
            calls++
            beforeWrite("T-20261028-001")
            saveGate?.await()
            if (failAfterWrite) throw RegistrationException(true, "T-20261028-001", IOException())
            return SavedTurn(1, "T-20261028-001")
        }
    }
}
package com.carwash.mobile.retiro

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
class RetiroViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun cleanup() = Dispatchers.resetMain()

    @Test fun searchLoadsReceiptWithoutDelivering() = runTest(dispatcher) {
        val source = FakeRetiro()
        val model = RetiroViewModel(source)
        model.editPlate("abc123"); model.search(); runCurrent()
        assertEquals(0, source.confirmations)
        assertEquals(1, source.issues)
        assertEquals("ABC123", model.state.plate)
        assertEquals(source.receipt, model.state.receipt)
        assertFalse(model.state.busy)
    }

    @Test fun refreshOnlyReadsReceiptEvenWithLegacyDeliveryData() = runTest(dispatcher) {
        val source = FakeRetiro().apply { receipt = receipt.copy(entregadoEn = "2026-11-06T15:00:00Z", entregadoPor = 2) }
        val model = RetiroViewModel(source)
        model.editPlate("ABC123"); model.search(); runCurrent()
        model.refresh(); runCurrent()
        assertEquals(0, source.confirmations)
        assertEquals(1, source.reads)
        assertEquals(source.receipt, model.state.receipt)
        assertNull(model.state.error)
    }

    @Test fun failedRefreshPreservesReceiptAndCanBeRetried() = runTest(dispatcher) {
        val source = FakeRetiro().apply { readsFail = true }
        val model = RetiroViewModel(source)
        model.editPlate("ABC123"); model.search(); runCurrent()
        model.refresh(); runCurrent()
        assertEquals(RetiroError.UNAVAILABLE, model.state.error)
        assertEquals(source.receipt, model.state.receipt)
        assertFalse(model.state.busy)
        source.readsFail = false
        model.refresh(); runCurrent()
        assertNull(model.state.error)
        assertEquals(0, source.confirmations)
        model.editPlate("XYZ789")
        assertNull(model.state.receipt)
    }

    @Test fun lostPrintResponseRecoversSameRequestWithoutAnotherCopy() = runTest(dispatcher) {
        val source = FakeRetiro().apply { printResponseLost = true }
        val model = RetiroViewModel(source)
        model.editPlate("ABC123"); model.search(); runCurrent()
        model.print(); runCurrent()
        assertEquals(1, source.printRequests.size)
        assertEquals("SUBMITTED", model.state.printJob?.estado)
        assertFalse(model.state.printCheckRequired)
        model.print(); runCurrent()
        assertEquals(1, source.printRequests.size)
        model.print(newCopy = true); runCurrent()
        assertEquals(2, source.printRequests.distinct().size)
        assertEquals(0, source.confirmations)
    }

    @Test fun historyFailureBlocksPrintUntilReadAndReloadRecoversLastJob() = runTest(dispatcher) {
        val source = FakeRetiro().apply { historyFails = true }
        val model = RetiroViewModel(source)
        model.editPlate("ABC123"); model.search(); runCurrent()
        model.print(); runCurrent()
        assertTrue(model.state.printCheckRequired)
        assertTrue(source.printRequests.isEmpty())
        source.historyFails = false
        model.checkPrint(); runCurrent(); model.print(); runCurrent()
        assertEquals(1, source.printRequests.size)
        model.clear(); model.editPlate("ABC123"); model.search(); runCurrent()
        assertEquals(source.printRequests.single(), model.state.printRequestId)
        model.print(); runCurrent()
        assertEquals(1, source.printRequests.size)
    }

    private class FakeRetiro : RetiroRepository {
        var receipt = RetiroReceipt(10, "0123456789ABCDEF", "ABC123", "Marca", "Cliente", "2026-11-06T09:00:00-05:00", BigDecimal("17500"), listOf(ReceiptService("Lavado", BigDecimal("30000"))))
        var confirmations = 0
        var reads = 0
        var issues = 0
        var lastReadId: Int? = null
        var lostResponse = false
        var readsFail = false
        var printResponseLost = false
        var historyFails = false
        val printRequests = mutableListOf<String>()
        var printJob: PrintJob? = null
        override suspend fun latestPrint(idTurno: Int): PrintJob? { if (historyFails) throw IOException(); return printJob }
        override suspend fun getPrint(requestId: String) = printJob ?: throw RetiroFailure(RetiroError.NOT_FOUND)
        override suspend fun requestPrint(idTurno: Int, requestId: String, widthMm: Int): PrintJob {
            printRequests.add(requestId)
            val job = PrintJob(requestId, idTurno, widthMm, "FAKE USB", "SUBMITTED", "2026-11-06T15:00:00Z", "2026-11-06T15:00:00Z")
            printJob = job
            if (printResponseLost) { printResponseLost = false; throw IOException() }
            return job
        }
        override suspend fun issue(request: IssueReceipt): RetiroReceipt { issues++; return receipt }
        override suspend fun get(idTurno: Int): RetiroReceipt { reads++; lastReadId = idTurno; if (readsFail) throw IOException(); return receipt }
        override suspend fun confirm(idTurno: Int): RetiroReceipt {
            confirmations++
            receipt = receipt.copy(entregadoEn = "2026-11-06T15:00:00Z", entregadoPor = 2)
            if (lostResponse) throw IOException()
            return receipt
        }
    }
}
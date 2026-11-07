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
    fun pickupCodeFromServerIsAddedToManualReceipt() = runTest(dispatcher) {
        val model = model(FakeRepository())
        model.configurePickup { _, _ -> "0123456789ABCDEF" }
        runCurrent()
        fill(model); model.save(); runCurrent()
        assertEquals("0123456789ABCDEF", model.state.manualWhatsAppReceipt?.pickupCode)
        assertTrue(model.state.manualWhatsAppAutoOpen)
    }

    @Test
    fun companyIsFrozenInManualReceiptAndReadFailureDoesNotRegisterAgain() = runTest(dispatcher) {
        val source = FakeRepository()
        val model = model(source)
        var company = com.carwash.mobile.empresa.EmpresaDto(revision = 1, nombreComercial = "Empresa vigente")
        model.configureCompany { company }
        runCurrent(); fill(model); model.save(); runCurrent()
        assertEquals(company, model.state.manualWhatsAppReceipt?.empresa)
        company = company.copy(revision = 2, nombreComercial = "Empresa nueva")
        assertEquals("Empresa vigente", model.state.manualWhatsAppReceipt?.empresa?.nombreComercial)
        model.configureCompany { throw IOException() }
        fill(model); model.save(); runCurrent()
        assertEquals(2, source.calls)
        assertEquals(NoticeKind.SAVED, model.state.notice?.kind)
        assertFalse(model.state.reviewRequired)
        assertNull(model.state.manualWhatsAppReceipt?.empresa)
        assertTrue(model.state.manualWhatsAppAutoOpen)
    }

    @Test
    fun automaticPrintFollowsConfirmedSaveAndFailureDoesNotRegisterAgain() = runTest(dispatcher) {
        val source = FakeRepository().apply { saveGate = CompletableDeferred() }
        val model = model(source)
        val printGate = CompletableDeferred<String>()
        var prints = 0
        model.configureEntryPrinting { url, id, services ->
            assertEquals("http://10.0.2.2:5197/", url)
            assertEquals(1, id)
            assertEquals(setOf(1), services)
            prints++
            printGate.await()
        }
        runCurrent(); fill(model); model.save(); model.save(); runCurrent()
        assertEquals(0, prints)
        source.saveGate!!.complete(Unit); runCurrent()
        assertEquals(1, prints)
        assertEquals(NoticeKind.SAVED, model.state.notice?.kind)
        assertFalse(model.state.saving)
        printGate.completeExceptionally(IOException()); runCurrent()
        assertEquals("UNKNOWN", model.state.entryPrintStatus)
        assertFalse(model.state.reviewRequired)
        assertEquals(1, source.calls)
    }

    @Test
    fun updatingExistingEntryDoesNotAutomaticallyPrint() = runTest(dispatcher) {
        val source = FakeRepository().apply { lookupResult = PlateLookup(null, emptyList(), InProcessTurn(
            9, "T-1", "ANA", "+573000000000", "ana@example.com", "KIA", "", 3,
            BigDecimal("20000"), BigDecimal("15000"), listOf(1))) }
        val model = model(source)
        var prints = 0
        model.configureEntryPrinting { _, _, _ -> prints++; "PENDING" }
        runCurrent(); model.edit { it.copy(plate = "ABC123") }; model.lookup(); runCurrent()
        model.save(); runCurrent()
        assertEquals(NoticeKind.UPDATED, model.state.notice?.kind)
        assertEquals(0, prints)
        assertEquals(0, source.calls)
    }

    @Test
    fun pickupFailurePreservesSavedTurnAndDoesNotOpenIncompleteReceipt() = runTest(dispatcher) {
        val source = FakeRepository()
        val model = model(source)
        model.configurePickup { _, _ -> throw IOException() }
        runCurrent()
        fill(model); model.save(); runCurrent()
        assertEquals(1, source.calls)
        assertEquals(NoticeKind.SAVED, model.state.notice?.kind)
        assertTrue(model.state.pickupReceiptUnavailable)
        assertFalse(model.state.reviewRequired)
        assertFalse(model.state.manualWhatsAppAutoOpen)
        assertNull(model.state.manualWhatsAppReceipt)
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
        assertFalse(model.state.reviewRequired)
        assertEquals("", model.state.pendingNumber)
        fill(model)
        model.save()
        runCurrent()
        assertEquals(2, source.calls)
        assertEquals(IngresoForm(), model.state.form)
        assertEquals(NoticeKind.SAVED, model.state.notice?.kind)
        assertFalse(model.state.reviewRequired)
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
        gate.complete(PlateLookup(VehicleInfo(3, "ANTERIOR", null, null, null), emptyList()))
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
        model.edit { it.copy(observations = "CONSERVAR") }
        model.acceptScannedPlate(" aoi012 ")
        runCurrent()
        assertEquals("AOI012", model.state.form.plate)
        assertEquals("AOI012", source.lastLookup)
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
        assertEquals("", model.state.form.customer)
        assertEquals("", model.state.form.phone)
        assertEquals("", model.state.form.brand)
        assertNull(model.state.form.typeId)
        assertTrue(model.state.form.serviceIds.isEmpty())
        assertEquals("0.00", model.state.form.value)
        assertEquals(NoticeKind.LOOKUP_FAILED, model.state.notice?.kind)
        assertEquals(0, source.calls)
    }

    @Test
    fun plateInProcessLoadsTurnAndSavingUpdatesInsteadOfRegistering() = runTest(dispatcher) {
        val source = FakeRepository()
        source.lookupResult = PlateLookup(
            null, emptyList(),
            InProcessTurn(9, "T-1", "ana", "3", "ana@example.com", "kia", "obs", 3, BigDecimal("20000"), BigDecimal("15000"), listOf(1)),
        )
        val model = model(source)
        runCurrent()
        model.edit { it.copy(plate = "ABC123") }
        model.lookup()
        runCurrent()
        assertEquals(9, model.state.editingTurnId)
        assertEquals(NoticeKind.IN_PROCESS, model.state.notice?.kind)
        assertEquals(setOf(1), model.state.form.serviceIds)
        assertEquals("ANA", model.state.form.customer)
        model.toggleService(1)
        assertTrue(model.state.form.serviceIds.isEmpty())
        model.toggleService(1)
        model.edit { it.copy(brand = "MAZDA") }
        model.save()
        runCurrent()
        assertEquals(0, source.calls)
        assertEquals(9, source.updatedId)
        assertEquals("MAZDA", source.updatedForm?.brand)
        assertEquals(NoticeKind.UPDATED, model.state.notice?.kind)
        assertNull(model.state.editingTurnId)
        assertEquals(IngresoForm(), model.state.form)
        assertEquals("ABC123", model.state.manualWhatsAppReceipt?.plate)
        assertEquals("MAZDA", model.state.manualWhatsAppReceipt?.brand)
        assertEquals(0, source.emailCalls)
        assertEquals(0, source.whatsAppCalls)
    }

    @Test
    fun changingPlateLeavesEditingModeSoNewVehiclesAreRegistered() = runTest(dispatcher) {
        val source = FakeRepository()
        source.lookupResult = PlateLookup(
            null, emptyList(),
            InProcessTurn(9, "T-1", "ana", "3", "ana@example.com", "kia", "", 3, BigDecimal("20000"), BigDecimal("15000"), listOf(1)),
        )
        val model = model(source)
        runCurrent()
        model.edit { it.copy(plate = "ABC123") }
        model.lookup()
        runCurrent()
        model.edit { it.copy(plate = "XYZ789") }
        assertNull(model.state.editingTurnId)
    }

    @Test
    fun fallsBackToAlternateServerAndPersistsBoth() = runTest(dispatcher) {
        val loaded = mutableListOf<String>()
        val settings = MemorySettings().apply { serverUrl = "" }
        val model = IngresoViewModel(settings) { url ->
            loaded += url
            FakeRepository().apply { failLoad = url.contains("192.168.1.15") }
        }
        model.connect("http://192.168.1.15:5197", "http://192.168.1.20:5197")
        runCurrent()
        assertEquals(listOf("http://192.168.1.15:5197/", "http://192.168.1.20:5197/"), loaded)
        assertTrue(model.state.connected)
        assertTrue(model.state.usingBackup)
        assertEquals("http://192.168.1.15:5197/", settings.serverUrl)
        assertEquals("http://192.168.1.20:5197/", settings.backupUrl)
    }

    @Test
    fun failsWhenPrimaryAndBackupFailAndRejectsInvalidBackup() = runTest(dispatcher) {
        val model = IngresoViewModel(MemorySettings().apply { serverUrl = "" }) { FakeRepository().apply { failLoad = true } }
        model.connect("http://192.168.1.15:5197", "http://example.com")
        assertEquals(NoticeKind.INVALID_SERVER, model.state.notice?.kind)
        model.connect("http://192.168.1.15:5197", "http://192.168.1.20:5197")
        runCurrent()
        assertFalse(model.state.connected)
        assertEquals(NoticeKind.CONNECTION_FAILED, model.state.notice?.kind)
    }

    @Test
    fun testConnectionReportsEachAddressWithoutConnecting() = runTest(dispatcher) {
        val model = IngresoViewModel(
            MemorySettings().apply { serverUrl = "" },
            probe = { if (it.contains("15")) ProbeResult.OK else ProbeResult.TIMEOUT },
        ) { FakeRepository() }
        model.testConnection("http://192.168.1.15:5197", "http://192.168.1.20:5197")
        assertTrue(model.state.probing)
        runCurrent()
        assertFalse(model.state.probing)
        assertFalse(model.state.connected)
        assertEquals(ProbeResult.OK, model.state.probeResults["http://192.168.1.15:5197/"])
        assertEquals(ProbeResult.TIMEOUT, model.state.probeResults["http://192.168.1.20:5197/"])
    }

    @Test
    fun whatsappAndEmailAreIndependentAndDoNotDelaySavedState() = runTest(dispatcher) {
        val source = FakeRepository().apply { whatsAppGate = CompletableDeferred() }
        val model = model(source)
        runCurrent()
        fill(model)
        model.selectWhatsAppMode(WhatsAppMode.AUTOMATIC)
        model.save()
        runCurrent()
        assertEquals(NoticeKind.SAVED, model.state.notice?.kind)
        assertFalse(model.state.saving)
        assertEquals(IngresoForm(), model.state.form)
        assertEquals(1, source.emailCalls)
        assertEquals(1, source.whatsAppCalls)
        assertEquals("PENDING", model.state.whatsAppStatus)

        source.whatsAppGate!!.complete(WhatsAppNotificationResult(1, "ACCEPTED"))
        runCurrent()
        assertEquals("ACCEPTED", model.state.whatsAppStatus)
        assertEquals("ACCEPTED", model.state.notificationStatus)
        assertEquals(1, source.calls)
    }

    @Test
    fun partialSaveNeverNotifiesEitherChannel() = runTest(dispatcher) {
        val source = FakeRepository().apply { failAfterWrite = true }
        val model = model(source)
        runCurrent()
        fill(model)
        model.save()
        runCurrent()
        assertTrue(model.state.reviewRequired)
        assertEquals(0, source.emailCalls)
        assertEquals(0, source.whatsAppCalls)
    }

    @Test
    fun whatsappFailureKeepsEmailAndDoesNotAllowUnsafeRetry() = runTest(dispatcher) {
        val source = FakeRepository().apply { failWhatsApp = true }
        val model = model(source)
        runCurrent()
        fill(model)
        model.selectWhatsAppMode(WhatsAppMode.AUTOMATIC)
        model.save()
        runCurrent()
        assertEquals("UNKNOWN", model.state.whatsAppStatus)
        assertEquals("ACCEPTED", model.state.notificationStatus)
        assertEquals(NoticeKind.SAVED, model.state.notice?.kind)
        model.retryWhatsApp()
        runCurrent()
        assertEquals(0, source.whatsAppRetries)
        assertEquals(1, source.calls)
    }

    @Test
    fun retryWhatsAppUsesOnlyNotificationEndpoint() = runTest(dispatcher) {
        val source = FakeRepository().apply {
            whatsAppResult = WhatsAppNotificationResult(1, "RETRYING", "RETRY_MANUAL")
        }
        val model = model(source)
        runCurrent()
        fill(model)
        model.selectWhatsAppMode(WhatsAppMode.AUTOMATIC)
        model.save()
        runCurrent()
        assertTrue(model.state.whatsAppCanRetry)
        model.retryWhatsApp()
        model.retryWhatsApp()
        runCurrent()
        assertEquals("ACCEPTED", model.state.whatsAppStatus)
        assertEquals(1, source.whatsAppRetries)
        assertEquals(1, source.calls)
    }

    @Test
    fun dismissingNotificationIgnoresLateWhatsAppResponse() = runTest(dispatcher) {
        val source = FakeRepository().apply { whatsAppGate = CompletableDeferred() }
        val model = model(source)
        runCurrent()
        fill(model)
        model.selectWhatsAppMode(WhatsAppMode.AUTOMATIC)
        model.save()
        runCurrent()
        model.clearWhatsAppStatus()
        source.whatsAppGate!!.complete(WhatsAppNotificationResult(1, "ACCEPTED"))
        runCurrent()
        assertNull(model.state.whatsAppStatus)
        assertNull(model.state.whatsAppTurnId)
    }

    @Test
    fun manualIsDefaultAndKeepsConfirmedSnapshotWithoutAutomaticSend() = runTest(dispatcher) {
        val source = FakeRepository()
        val model = model(source)
        runCurrent()
        fill(model)
        model.edit { it.copy(customer = "ANA", phone = "+57 300 000 0000", brand = "KIA", value = "17500.50") }
        model.save()
        runCurrent()
        val receipt = requireNotNull(model.state.manualWhatsAppReceipt)
        assertEquals(WhatsAppMode.MANUAL, model.state.whatsAppMode)
        assertEquals("ABC123", receipt.plate)
        assertEquals("ANA", receipt.customer)
        assertEquals("KIA", receipt.brand)
        assertEquals(listOf("Lavado"), receipt.services.map { it.nombre })
        assertEquals(BigDecimal("17500.50"), receipt.total)
        assertEquals(1, source.emailCalls)
        assertEquals(0, source.whatsAppCalls)
        assertNull(model.state.whatsAppStatus)
        model.edit { it.copy(plate = "XYZ789", value = "999") }
        model.editManualWhatsAppPhone("+573111111111")
        assertEquals("ABC123", model.state.manualWhatsAppReceipt?.plate)
        assertEquals(BigDecimal("17500.50"), model.state.manualWhatsAppReceipt?.total)
        assertEquals("+573111111111", model.state.manualWhatsAppReceipt?.phone)
        assertEquals(1, source.calls)
    }

    @Test
    fun manualReceiptRequiresCompleteSave() = runTest(dispatcher) {
        val source = FakeRepository().apply { failAfterWrite = true }
        val model = model(source)
        runCurrent()
        fill(model)
        model.save()
        runCurrent()
        assertNull(model.state.manualWhatsAppReceipt)
        assertTrue(model.state.reviewRequired)
        assertFalse(model.state.manualWhatsAppAutoOpen)
        assertEquals(0, source.whatsAppCalls)
    }

    @Test
    fun countryPrefixIsRememberedAndUsedForConfirmedManualReceipt() = runTest(dispatcher) {
        val settings = MemorySettings()
        val source = FakeRepository()
        val model = IngresoViewModel(settings) { source }
        runCurrent()
        assertFalse(model.configureWhatsAppCountryCode("57"))
        assertTrue(model.configureWhatsAppCountryCode("+52"))
        fill(model)
        model.edit { it.copy(phone = "3000000000") }
        model.save()
        runCurrent()
        assertEquals("+52", settings.whatsAppCountryCode)
        assertEquals("+523000000000", model.state.manualWhatsAppReceipt?.phone)
        assertTrue(model.state.manualWhatsAppAutoOpen)
        model.consumeManualWhatsAppAutoOpen()
        model.consumeManualWhatsAppAutoOpen()
        assertFalse(model.state.manualWhatsAppAutoOpen)
        val restored = IngresoViewModel(settings) { source }
        runCurrent()
        assertEquals("+52", restored.state.whatsAppCountryCode)
        assertFalse(restored.state.manualWhatsAppAutoOpen)
        assertEquals(1, source.calls)
    }

    @Test
    fun configuredCountryPrefixAppliesOnlyToLocalWhatsAppRecipients() {
        assertEquals("+573000000000", whatsAppRecipient("300 000 0000", "+57"))
        assertEquals("+523000000000", whatsAppRecipient("3000000000", "+52"))
        assertEquals("+13005550123", whatsAppRecipient("+1 (300) 555-0123", "+57"))
        assertNull(whatsAppRecipient("", "+57"))
        assertNull(whatsAppRecipient("ABC3000000000", "+57"))
        assertNull(whatsAppRecipient("3000000000", "57"))
        assertNull(whatsAppRecipient("3000000000", "+000"))
        assertNull(whatsAppRecipient("+003000000000", "+57"))
    }

    @Test
    fun manualReceiptIsClearedWhenDismissedClearedOrChangingServer() = runTest(dispatcher) {
        val source = FakeRepository()
        val model = model(source)
        runCurrent()
        suspend fun saveReceipt() {
            fill(model)
            model.save()
            runCurrent()
            assertNotNull(model.state.manualWhatsAppReceipt)
        }
        saveReceipt()
        model.clearManualWhatsApp()
        assertNull(model.state.manualWhatsAppReceipt)
        saveReceipt()
        model.clear()
        assertNull(model.state.manualWhatsAppReceipt)
        saveReceipt()
        model.connect("http://192.168.1.15:5197")
        assertNull(model.state.manualWhatsAppReceipt)
        runCurrent()
    }

    @Test
    fun nextSaveClearsOldManualReceiptAndAutomaticModeDoesNotPrepareOne() = runTest(dispatcher) {
        val source = FakeRepository()
        val model = model(source)
        runCurrent()
        fill(model)
        model.save()
        runCurrent()
        assertNotNull(model.state.manualWhatsAppReceipt)
        fill(model)
        model.selectWhatsAppMode(WhatsAppMode.AUTOMATIC)
        source.saveGate = CompletableDeferred()
        model.save()
        assertNull(model.state.manualWhatsAppReceipt)
        model.selectWhatsAppMode(WhatsAppMode.MANUAL)
        assertEquals(WhatsAppMode.AUTOMATIC, model.state.whatsAppMode)
        runCurrent()
        source.saveGate!!.complete(Unit)
        runCurrent()
        assertNull(model.state.manualWhatsAppReceipt)
        assertEquals(1, source.whatsAppCalls)
    }

    @Test
    fun manualPhoneRequiresExplicitInternationalCodeWithoutGuessing() {
        assertEquals("+573000000000", normalizeWhatsAppPhone(" +57 (300) 000-0000 "))
        listOf("3000000000", "", "+0123456789", "+123", "+1234567890123456", "+57300abc0000")
            .forEach { assertNull(normalizeWhatsAppPhone(it)) }
    }

    @Test
    fun emailExceptionNeverBlocksManualWhatsAppOrMarksRegistrationUncertain() = runTest(dispatcher) {
        val source = FakeRepository().apply { failEmail = true }
        val settings = MemorySettings()
        val model = IngresoViewModel(settings) { source }
        runCurrent()
        fill(model)
        model.edit { it.copy(phone = "+573000000000") }
        model.save()
        runCurrent()
        assertEquals("UNKNOWN", model.state.notificationStatus)
        assertEquals(NoticeKind.SAVED, model.state.notice?.kind)
        assertFalse(model.state.reviewRequired)
        assertNull(settings.pendingTurn)
        assertEquals("ABC123", model.state.manualWhatsAppReceipt?.plate)
        assertEquals("+573000000000", model.state.manualWhatsAppReceipt?.phone)
        assertEquals(1, source.calls)
        assertEquals(0, source.whatsAppCalls)
    }

    @Test
    fun failedUpdateDoesNotPrepareManualConfirmation() = runTest(dispatcher) {
        val source = FakeRepository().apply {
            failUpdate = true
            lookupResult = PlateLookup(null, emptyList(), InProcessTurn(
                9, "T-1", "ANA", "+573000000000", "ana@example.com", "KIA", "",
                3, BigDecimal("20000"), BigDecimal("15000"), listOf(1),
            ))
        }
        val model = model(source)
        runCurrent()
        model.edit { it.copy(plate = "ABC123") }
        model.lookup()
        runCurrent()
        model.save()
        runCurrent()
        assertEquals(NoticeKind.SAVE_FAILED, model.state.notice?.kind)
        assertNull(model.state.manualWhatsAppReceipt)
        assertEquals(0, source.calls)
        assertEquals(0, source.emailCalls)
    }

    private fun model(source: FakeRepository) = IngresoViewModel(MemorySettings()) { source }

    private fun fill(model: IngresoViewModel) {
        model.edit { it.copy(plate = "ABC123", email = "cliente@example.com") }
        model.selectType(3)
        model.toggleService(1)
    }

    private class MemorySettings : ServerSettings {
        override var serverUrl = "http://10.0.2.2:5197/"
        override var backupUrl = ""
        override var pendingTurn: String? = null
        override var whatsAppCountryCode = "+57"
    }

    private class FakeRepository : IngresoRepository {
        var calls = 0
        var lastLookup: String? = null
        var failLoad = false
        var failAfterWrite = false
        var saveGate: CompletableDeferred<Unit>? = null
        var lookupGate: CompletableDeferred<PlateLookup>? = null
        var lookupResult = PlateLookup(null, emptyList())
        var updatedId: Int? = null
        var updatedForm: IngresoForm? = null
        var failUpdate = false
        var emailCalls = 0
        var failEmail = false
        var whatsAppCalls = 0
        var whatsAppRetries = 0
        var failWhatsApp = false
        var whatsAppGate: CompletableDeferred<WhatsAppNotificationResult>? = null
        var whatsAppResult = WhatsAppNotificationResult(1, "ACCEPTED")

        override suspend fun notifyEmail(idTurno: Int): EmailNotificationResult {
            emailCalls++
            if (failEmail) throw IOException("Email unavailable")
            return EmailNotificationResult(idTurno, "ACCEPTED", "NONE")
        }

        override suspend fun notifyWhatsApp(idTurno: Int): WhatsAppNotificationResult {
            whatsAppCalls++
            if (failWhatsApp) throw IOException()
            return whatsAppGate?.await() ?: whatsAppResult
        }

        override suspend fun retryWhatsApp(idTurno: Int): WhatsAppNotificationResult {
            whatsAppRetries++
            return WhatsAppNotificationResult(idTurno, "ACCEPTED")
        }

        override suspend fun updateInProcess(idTurno: Int, form: IngresoForm) {
            if (failUpdate) throw IOException("Update unavailable")
            updatedId = idTurno
            updatedForm = form
        }

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
            return lookupGate?.await() ?: lookupResult
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
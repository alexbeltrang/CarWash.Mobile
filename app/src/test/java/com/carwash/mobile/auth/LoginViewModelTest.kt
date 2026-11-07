package com.carwash.mobile.auth

import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.advanceTimeBy
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val credential = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
    private fun modelWithClock(repository: AuthRepository, store: SessionStore) =
        LoginViewModel(repository, store, elapsedRealtime = { dispatcher.scheduler.currentTime }, nowMillis = { dispatcher.scheduler.currentTime })
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun successfulLoginPersistsSessionWithoutPassword() = runTest(dispatcher) {
        val session = credentialSession()
        val store = MemoryStore()
        val model = modelWithClock(object : AuthRepository {
            override suspend fun login(userName: String, password: String) = LoginOutcome.Success(session)
        }, store)
        model.updateUser("admin"); model.updatePassword("fixture"); model.submit(); runCurrent()
        assertEquals(credential, Gson().toJsonTree(model.state.session).asJsonObject.get("accessToken")?.asString)
        assertEquals(credential, store.saved?.accessToken)
        assertEquals(9 * 60 * 60 * 1000L, store.saved?.localExpiresAtMillis)
        assertFalse(Gson().toJsonTree(store.saved).asJsonObject.has("password"))
        assertEquals("", model.state.password)
    }

    @Test fun savedSessionRestoresWithoutAnotherLoginAndLogoutClearsIt() = runTest(dispatcher) {
        val store = MemoryStore(credentialSession())
        val model = modelWithClock(object : AuthRepository {
            override suspend fun login(userName: String, password: String) = LoginOutcome.Unreachable
        }, store)
        assertEquals(credential, model.state.session?.accessToken)
        assertFalse(model.state.requiresReentry)
        model.logout()
        assertNull(model.state.session)
        assertNull(store.saved)
    }

    @Test fun lateLoginCannotRestoreSessionAfterLogout() = runTest(dispatcher) {
        val pending = CompletableDeferred<LoginOutcome>()
        val store = MemoryStore()
        val model = modelWithClock(object : AuthRepository {
            override suspend fun login(userName: String, password: String) = pending.await()
        }, store)
        model.updateUser("admin"); model.updatePassword("fixture"); model.submit(); runCurrent()
        model.logout()
        assertNull(model.state.session)
        pending.complete(LoginOutcome.Success(credentialSession())); runCurrent()
        assertNull(model.state.session)
        assertNull(store.saved)
    }

    private fun credentialSession() = Gson().fromJson("""{"userId":1,"displayName":"Admin","isAdmin":true,"baseUrl":"http://fixture/","accessToken":"$credential","expiresAt":"2026-11-03T20:00:00Z","serverDate":"2026-11-03","localExpiresAtMillis":32400000}""", Session::class.java)

    @Test fun restoringDoesNotRestartTheNineHourDeadline() = runTest(dispatcher) {
        val repository = object : AuthRepository {
            override suspend fun login(userName: String, password: String) = LoginOutcome.Unreachable
        }
        advanceTimeBy(8 * 60 * 60 * 1000L)
        val store = MemoryStore(credentialSession())
        val restored = modelWithClock(repository, store)
        assertNotNull(restored.state.session)
        advanceTimeBy(60 * 60 * 1000L); runCurrent()
        assertNull(restored.state.session); assertNull(store.saved)
    }

    @Test fun expiredSessionAndLegacyMetadataRequireLogin() = runTest(dispatcher) {
        val repository = object : AuthRepository {
            override suspend fun login(userName: String, password: String) = LoginOutcome.Unreachable
        }
        for (saved in listOf(credentialSession().copy(localExpiresAtMillis = 0), credentialSession().metadata(),
            credentialSession().copy(expiresAt = "1970-01-01T00:00:00Z"))) {
            val store = MemoryStore(saved)
            assertNull(modelWithClock(repository, store).state.session)
            assertNull(store.saved)
        }
    }

    @Test fun reentryWithoutServerCredentialStaysBlockedAndCanReturnToIngreso() = runTest(dispatcher) {
        val metadata = Session(1, "Admin", true, "http://fixture/")
        val model = modelWithClock(object : AuthRepository {
            override suspend fun login(userName: String, password: String) = LoginOutcome.Success(metadata)
        }, MemoryStore(metadata))
        model.updateUser("admin"); model.updatePassword("fixture"); model.submit(); runCurrent()
        model.requestReportReentry()
        assertTrue(model.state.requiresReentry)
        model.updateUser("admin"); model.updatePassword("fixture"); model.submit(); runCurrent()
        assertTrue(model.state.requiresReentry)
        assertEquals(LoginError.REPORT_SESSION_REQUIRED, model.state.error)
        assertEquals("", model.state.password)
        model.cancelReentry()
        assertFalse(model.state.requiresReentry)
        assertEquals(metadata, model.state.session)
    }

    @Test fun localLogoutDoesNotWaitForRemoteConfirmation() = runTest(dispatcher) {
        val remote = CompletableDeferred<Boolean>()
        var remoteCalls = 0
        val store = MemoryStore()
        val model = modelWithClock(object : AuthRepository {
            override suspend fun login(userName: String, password: String) = LoginOutcome.Success(credentialSession())
            override suspend fun logout(session: Session): Boolean { remoteCalls++; return remote.await() }
        }, store)
        model.updateUser("admin"); model.updatePassword("fixture"); model.submit(); runCurrent()
        model.logout()
        assertNull(model.state.session)
        assertNull(store.saved)
        runCurrent()
        assertEquals(1, remoteCalls)
        remote.complete(false); runCurrent()
        assertNull(model.state.session)
    }

    @Test fun forbiddenReportRemovesAdminWithoutEndingOrExtendingOperationalSession() = runTest(dispatcher) {
        val store = MemoryStore()
        val model = modelWithClock(object : AuthRepository {
            override suspend fun login(userName: String, password: String) = LoginOutcome.Success(credentialSession())
        }, store)
        model.updateUser("admin"); model.updatePassword("fixture"); model.submit(); runCurrent()
        model.reportForbidden()
        assertEquals(1, model.state.session!!.userId)
        assertFalse(model.state.session!!.isAdmin)
        assertEquals(credential, model.state.session!!.accessToken)
        assertFalse(model.state.requiresReentry)
        assertFalse(store.saved!!.isAdmin)
        assertEquals(credential, store.saved!!.accessToken)
        assertEquals("", model.state.password)
        advanceTimeBy(9 * 60 * 60 * 1000L); runCurrent()
        assertNull(model.state.session)
    }

    @Test fun entireSessionClosesAtNineHoursWithoutAnyReportLoginOrActivity() = runTest(dispatcher) {
        val store = MemoryStore()
        var logins = 0
        var logouts = 0
        val model = modelWithClock(object : AuthRepository {
            override suspend fun login(userName: String, password: String): LoginOutcome { logins++; return LoginOutcome.Success(credentialSession()) }
            override suspend fun logout(session: Session): Boolean { logouts++; return true }
        }, store)
        model.updateUser("admin"); model.updatePassword("fixture"); model.submit(); runCurrent()
        advanceTimeBy(9 * 60 * 60 * 1000L - 1); runCurrent()
        model.checkSessionExpiry()
        assertNotNull(model.state.session)
        assertEquals(1, logins)
        advanceTimeBy(1); runCurrent()
        assertNull(model.state.session)
        assertNull(store.saved)
        assertFalse(model.state.requiresReentry)
        assertEquals(1, logouts)
    }

    @Test fun resumeAfterSleepingPastDeadlineClosesWithoutWaitingForTimer() = runTest(dispatcher) {
        var elapsed = 0L
        val store = MemoryStore()
        val model = LoginViewModel(object : AuthRepository {
            override suspend fun login(userName: String, password: String) = LoginOutcome.Success(credentialSession())
        }, store, elapsedRealtime = { elapsed })
        model.updateUser("admin"); model.updatePassword("fixture"); model.submit(); runCurrent()
        elapsed = 9 * 60 * 60 * 1000L
        model.checkSessionExpiry()
        assertNull(model.state.session)
        assertNull(store.saved)
    }

    @Test fun newLoginGetsNewNineHoursWithoutAnOldTimerClosingIt() = runTest(dispatcher) {
        val model = modelWithClock(object : AuthRepository {
            override suspend fun login(userName: String, password: String) = LoginOutcome.Success(credentialSession())
        }, MemoryStore())
        model.updateUser("admin"); model.updatePassword("fixture"); model.submit(); runCurrent()
        advanceTimeBy(8 * 60 * 60 * 1000L); runCurrent()
        model.logout()
        model.updateUser("admin"); model.updatePassword("fixture"); model.submit(); runCurrent()
        advanceTimeBy(60 * 60 * 1000L); runCurrent()
        assertNotNull(model.state.session)
        advanceTimeBy(8 * 60 * 60 * 1000L); runCurrent()
        assertNull(model.state.session)
    }

    private class MemoryStore(var saved: Session? = null) : SessionStore {
        override fun load() = saved
        override fun save(session: Session?) { saved = session }
    }
}
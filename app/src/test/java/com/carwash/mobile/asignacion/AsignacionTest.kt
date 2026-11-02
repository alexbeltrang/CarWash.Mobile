package com.carwash.mobile.asignacion

import com.carwash.mobile.auth.ApiAuthRepository
import com.carwash.mobile.auth.AuthApi
import com.carwash.mobile.auth.AuthRepository
import com.carwash.mobile.auth.CredentialEncoder
import com.carwash.mobile.auth.LoginError
import com.carwash.mobile.auth.LoginOutcome
import com.carwash.mobile.auth.LoginRequest
import com.carwash.mobile.auth.LoginResponse
import com.carwash.mobile.auth.LoginUser
import com.carwash.mobile.auth.LoginViewModel
import com.carwash.mobile.auth.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AsignacionTest {
    private val dispatcher = StandardTestDispatcher()
    private val admin = Session(1, "Admin", true, "http://a/")
    private val recep = Session(2, "Recep", false, "http://a/")

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private class FakeRepo(var result: AssignResult = AssignResult.OK) : AsignacionRepository {
        var vehicles = listOf(VehicleInProcess(10, "ABC123"))
        val assigned = mutableListOf<Triple<Int, Int, Int>>()
        override suspend fun vehicles() = vehicles
        var operators = listOf(Operator(5, "Ana", "Paz"))
        override suspend fun availableOperators() = operators
        override suspend fun assign(turnId: Int, operatorId: Int, userId: Int): AssignResult {
            assigned += Triple(turnId, operatorId, userId)
            return result
        }
    }

    @Test fun adminAssignsAndListRefreshes() = runTest(dispatcher) {
        val repo = FakeRepo()
        val model = AsignacionViewModel(repo, admin)
        model.refresh(); runCurrent()
        model.select(repo.vehicles[0]); runCurrent()
        repo.vehicles = listOf(VehicleInProcess(10, "ABC123", operadorAsignado = "Ana Paz", idOperario = 5))
        model.assign(model.state.operators[0]); runCurrent()
        assertEquals(listOf(Triple(10, 5, 1)), repo.assigned)
        assertEquals(AsignacionMessage.ASSIGNED, model.state.message)
        assertNull(model.state.selected)
        assertEquals("Ana Paz", model.state.vehicles[0].operatorName)
    }

    @Test fun reassignmentSendsSameRequest() = runTest(dispatcher) {
        val repo = FakeRepo()
        repo.vehicles = listOf(VehicleInProcess(10, "ABC123", operadorAsignado = "Luis Gil", idOperario = 7))
        val model = AsignacionViewModel(repo, admin)
        model.refresh(); runCurrent()
        model.select(repo.vehicles[0]); runCurrent()
        model.assign(model.state.operators[0]); runCurrent()
        assertEquals(Triple(10, 5, 1), repo.assigned.single())
    }

    @Test fun reopeningScreenReloadsVehiclesAndOperatorsSortedByName() = runTest(dispatcher) {
        val repo = FakeRepo()
        val model = AsignacionViewModel(repo, admin)
        model.refresh(); runCurrent()
        assertEquals(1, model.state.vehicles.size)
        repo.vehicles = repo.vehicles + VehicleInProcess(12, "NEW001")
        repo.operators = listOf(Operator(8, "zoe", "Ruiz"), Operator(6, "Beto", "Lara"), Operator(5, "Ana", "Paz"))
        model.refresh(); runCurrent()
        assertEquals(2, model.state.vehicles.size)
        model.select(model.state.vehicles[0]); runCurrent()
        assertEquals(listOf("Ana Paz", "Beto Lara", "zoe Ruiz"), model.state.operators.map { it.fullName })
    }
    @Test fun nonAdminCannotAssign() = runTest(dispatcher) {
        val repo = FakeRepo()
        val model = AsignacionViewModel(repo, recep)
        model.refresh(); runCurrent()
        model.select(repo.vehicles[0]); runCurrent()
        assertNull(model.state.selected)
        assertTrue(repo.assigned.isEmpty())
    }

    @Test fun conflictKeepsDialogAndReportsMessage() = runTest(dispatcher) {
        val repo = FakeRepo(AssignResult.CONFLICT)
        val model = AsignacionViewModel(repo, admin)
        model.refresh(); runCurrent()
        model.select(repo.vehicles[0]); runCurrent()
        model.assign(model.state.operators[0]); runCurrent()
        assertEquals(AsignacionMessage.CONFLICT, model.state.message)
        assertNotNull(model.state.selected)
    }

    @Test fun repositoryMapsStatusCodes() = runTest {
        MockWebServer().use { server ->
            server.start()
            val repo = ApiAsignacionRepository.create(server.url("/").toString())
            for ((code, expected) in listOf(204 to AssignResult.OK, 403 to AssignResult.FORBIDDEN, 404 to AssignResult.NOT_FOUND, 409 to AssignResult.CONFLICT, 500 to AssignResult.ERROR)) {
                server.enqueue(MockResponse().setResponseCode(code))
                assertEquals(expected, repo.assign(10, 5, 1))
            }
            val request = server.takeRequest()
            assertEquals("/api/Turnos/10/Operario", request.path)
            assertEquals("PUT", request.method)
            assertEquals("""{"idOperario":5,"idUsuario":1}""", request.body.readUtf8())
        }
    }

    @Test fun repositoryParsesVehiclesWithOperator() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse().setBody("""[{"idTurno":1,"placa":"AAA111","operadorAsignado":"Ana Paz","idOperario":5},{"idTurno":2,"placa":"BBB222","operadorAsignado":" ","idOperario":null}]"""))
            val list = ApiAsignacionRepository.create(server.url("/").toString()).vehicles()
            assertEquals("Ana Paz", list[0].operatorName)
            assertNull(list[1].operatorName)
        }
    }

    private class FakeAuthApi(val response: LoginResponse?, val failure: Boolean = false) : AuthApi {
        var last: LoginRequest? = null
        override suspend fun login(request: LoginRequest): LoginResponse {
            last = request
            if (failure) throw IOException("down")
            return response!!
        }
    }

    private val encoder = CredentialEncoder { "enc:$it" }

    @Test fun loginEncodesPasswordAndFallsBackToBackup() = runTest {
        val apis = mapOf(
            "http://main/" to FakeAuthApi(null, failure = true),
            "http://backup/" to FakeAuthApi(LoginResponse(true, LoginUser(9, "Admin", 1, true))),
        )
        val repo = ApiAuthRepository({ listOf("http://main/", "http://backup/") }, encoder) { apis.getValue(it) }
        val outcome = repo.login(" admin ", "secret") as LoginOutcome.Success
        assertEquals(Session(9, "Admin", true, "http://backup/"), outcome.session)
        assertEquals(LoginRequest("enc:admin", "enc:secret"), apis.getValue("http://backup/").last)
    }

    @Test fun loginReportsBadCredentialsAndUnreachable() = runTest {
        val bad = ApiAuthRepository({ listOf("http://a/") }, encoder) { FakeAuthApi(LoginResponse(false, null)) }
        assertEquals(LoginOutcome.BadCredentials, bad.login("u", "p"))
        val down = ApiAuthRepository({ listOf("http://a/") }, encoder) { FakeAuthApi(null, failure = true) }
        assertEquals(LoginOutcome.Unreachable, down.login("u", "p"))
    }

    @Test fun loginViewModelValidatesClearsPasswordAndLogsOut() = runTest(dispatcher) {
        var outcome: LoginOutcome = LoginOutcome.BadCredentials
        val model = LoginViewModel(object : AuthRepository {
            override suspend fun login(userName: String, password: String) = outcome
        })
        model.submit()
        assertEquals(LoginError.EMPTY, model.state.error)
        model.updateUser("u"); model.updatePassword("p"); model.submit(); runCurrent()
        assertEquals(LoginError.BAD_CREDENTIALS, model.state.error)
        assertEquals("", model.state.password)
        outcome = LoginOutcome.Success(admin)
        model.updatePassword("p"); model.submit(); runCurrent()
        assertEquals(admin, model.state.session)
        assertEquals("", model.state.password)
        model.logout()
        assertNull(model.state.session)
    }

    private class MemoryStore(var saved: Session? = null) : com.carwash.mobile.auth.SessionStore {
        override fun load() = saved
        override fun save(session: Session?) { saved = session }
    }

    @Test fun sessionIsRememberedAcrossRestartsUntilLogout() = runTest(dispatcher) {
        val store = MemoryStore()
        val repo = object : AuthRepository {
            override suspend fun login(userName: String, password: String) = LoginOutcome.Success(admin)
        }
        val first = LoginViewModel(repo, store)
        first.updateUser("u"); first.updatePassword("p"); first.submit(); runCurrent()
        assertEquals(admin, store.saved)

        val restarted = LoginViewModel(repo, store)
        assertEquals(admin, restarted.state.session)

        restarted.logout()
        assertNull(store.saved)
        assertNull(LoginViewModel(repo, store).state.session)
    }
}

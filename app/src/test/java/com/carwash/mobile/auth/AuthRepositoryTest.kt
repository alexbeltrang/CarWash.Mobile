package com.carwash.mobile.auth

import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class AuthRepositoryTest {
    private val token = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
    private val encoder = CredentialEncoder { "encoded:$it" }

    @Test fun loginReadsCredentialAndBusinessDateFromAuthenticatedOrigin() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse().addHeader("X-Server-Date", "2026-11-03").setBody(success()))
            val repository = ApiAuthRepository({ listOf(server.url("/").toString()) }, encoder)
            val session = (repository.login(" admin ", "fixture") as LoginOutcome.Success).session
            val fields = Gson().toJsonTree(session).asJsonObject
            assertEquals(token, fields.get("accessToken")?.asString)
            assertEquals("2026-11-03", fields.get("serverDate")?.asString)
            assertEquals("2026-11-03T20:00:00Z", fields.get("expiresAt")?.asString)
            val request = server.takeRequest()
            assertEquals("/api/Auth/login", request.path)
            assertNull(request.getHeader("Authorization"))
            assertEquals("""{"userName":"encoded:admin","password":"encoded:fixture"}""", request.body.readUtf8())
            assertFalse(session.toString().contains(token))
        }
    }

    @Test fun logoutUsesOnlyIssuerAndDoesNotConfirmNetworkFailure() = runTest {
        MockWebServer().use { primary ->
            MockWebServer().use { backup ->
                primary.start(); backup.start()
                primary.enqueue(MockResponse().setBody(success()))
                val repository = ApiAuthRepository({ listOf(primary.url("/").toString(), backup.url("/").toString()) }, encoder)
                val session = (repository.login("admin", "fixture") as LoginOutcome.Success).session
                primary.takeRequest()
                primary.enqueue(MockResponse().setResponseCode(204))
                assertTrue(repository.logout(session))
                val request = primary.takeRequest()
                assertEquals("/api/Auth/logout", request.path)
                assertEquals("Bearer $token", request.getHeader("Authorization"))
                primary.enqueue(MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST))
                assertFalse(repository.logout(session))
                assertEquals(0, backup.requestCount)
            }
        }
    }

    @Test fun legacyLoginRemainsOperationalWithoutGrantingReportCredential() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse().setBody("""{"esValido":true,"usuario":{"idUser":1,"displayName":"Admin","esAdministrador":true}}"""))
            val repository = ApiAuthRepository({ listOf(server.url("/").toString()) }, encoder)
            val session = (repository.login("admin", "fixture") as LoginOutcome.Success).session
            assertNull(Gson().toJsonTree(session).asJsonObject.get("accessToken"))
            assertEquals(1, session.userId)
        }
    }

    @Test fun rejectedCredentialsAreNotSentToBackup() = runTest {
        MockWebServer().use { primary ->
            MockWebServer().use { backup ->
                primary.start(); backup.start()
                primary.enqueue(MockResponse().setBody("""{"esValido":false}"""))
                val repository = ApiAuthRepository({ listOf(primary.url("/").toString(), backup.url("/").toString()) }, encoder)
                assertEquals(LoginOutcome.BadCredentials, repository.login("admin", "fixture"))
                assertEquals(0, backup.requestCount)
            }
        }
    }

    @Test fun alternateLoginNeverReceivesExistingBearer() = runTest {
        MockWebServer().use { primary ->
            MockWebServer().use { backup ->
                primary.start(); backup.start()
                primary.enqueue(MockResponse().setBody(success()))
                val repository = ApiAuthRepository({ listOf(primary.url("/").toString(), backup.url("/").toString()) }, encoder)
                repository.login("admin", "fixture")
                primary.enqueue(MockResponse().setResponseCode(503))
                backup.enqueue(MockResponse().setBody(success()))
                val session = (repository.login("admin", "fixture") as LoginOutcome.Success).session
                assertEquals(backup.url("/").toString(), session.baseUrl)
                assertNull(backup.takeRequest().getHeader("Authorization"))
            }
        }
    }

    private fun success() = """{"esValido":true,"usuario":{"idUser":1,"displayName":"Admin","esAdministrador":true},"accessToken":"$token","tokenType":"Bearer","expiresAt":"2026-11-03T20:00:00Z","idleTimeoutSeconds":1800}"""
}
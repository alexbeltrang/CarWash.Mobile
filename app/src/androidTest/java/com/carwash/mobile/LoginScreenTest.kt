package com.carwash.mobile

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.carwash.mobile.auth.AuthRepository
import com.carwash.mobile.auth.LoginOutcome
import com.carwash.mobile.auth.LoginScreen
import com.carwash.mobile.auth.LoginViewModel
import com.carwash.mobile.auth.AndroidSessionStore
import com.carwash.mobile.auth.Session
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import java.time.Instant
import java.util.UUID
import com.carwash.mobile.ui.theme.CarWashTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {
    @get:Rule val rule = createComposeRule()

    private val repository = object : AuthRepository {
        override suspend fun login(userName: String, password: String): LoginOutcome = error("no usado")
    }

    @Test
    fun encryptedSessionRestoresAfterStoreAndModelRecreationWithoutCredentials() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "test-session-" + UUID.randomUUID()
        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        val token = "A".repeat(43)
        val deadline = System.currentTimeMillis() + 9 * 60 * 60 * 1000L
        val session = Session(2, "Usuario de prueba", false, "http://127.0.0.1:5197/", token,
            Instant.ofEpochMilli(deadline).toString(), localExpiresAtMillis = deadline)
        try {
            AndroidSessionStore(context, name).save(session)
            val stored = prefs.all.values.joinToString()
            assertFalse(stored.contains(token)); assertFalse(stored.contains(session.displayName))
            assertFalse(stored.contains(session.baseUrl))
            assertEquals(1, prefs.all.size)
            assertEquals(session, AndroidSessionStore(context, name).load())
            rule.runOnUiThread {
                val restored = LoginViewModel(repository, AndroidSessionStore(context, name))
                assertEquals(session, restored.state.session)
                assertFalse(restored.state.requiresReentry)
                assertEquals("", restored.state.userName); assertEquals("", restored.state.password)
                restored.logout()
                assertNull(AndroidSessionStore(context, name).load())
            }
        } finally { prefs.edit().clear().commit() }
    }

    @Test
    fun tamperedEncryptedSessionDoesNotRestoreAnIdentity() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "test-session-" + UUID.randomUUID()
        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        try {
            val deadline = System.currentTimeMillis() + 60_000
            val store = AndroidSessionStore(context, name)
            store.save(Session(2, "Prueba", false, "http://127.0.0.1:5197/", "A".repeat(43),
                Instant.ofEpochMilli(deadline).toString(), localExpiresAtMillis = deadline))
            val original = prefs.getString("encryptedSession", null)!!
            val changed = (if (original.first() == 'A') "B" else "A") + original.drop(1)
            prefs.edit().putString("encryptedSession", changed).commit()
            assertNull(AndroidSessionStore(context, name).load())
            assertTrue(prefs.all.isEmpty())
        } finally { prefs.edit().clear().commit() }
    }

    private fun shows(text: String) = SemanticsMatcher("EditableText = $text") {
        (if (SemanticsProperties.EditableText in it.config) it.config[SemanticsProperties.EditableText].text else null) == text
    }

    @Test
    fun passwordIsHiddenByDefaultAndCanBeToggled() {
        val model = LoginViewModel(repository)
        rule.setContent { CarWashTheme { LoginScreen(model, "http://localhost:5197/", {}) } }
        rule.onNodeWithText("Contraseña").performTextInput("secreto1")
        rule.onNodeWithText("Contraseña").assert(shows("••••••••"))
        rule.onNodeWithContentDescription("Mostrar contraseña", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Contraseña").assert(shows("secreto1"))
        rule.onNodeWithContentDescription("Ocultar contraseña", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Contraseña").assert(shows("••••••••"))
    }
}
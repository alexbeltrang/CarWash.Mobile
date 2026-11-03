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
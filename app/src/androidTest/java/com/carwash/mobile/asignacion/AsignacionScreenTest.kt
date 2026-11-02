package com.carwash.mobile.asignacion

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.carwash.mobile.auth.Session
import com.carwash.mobile.ui.theme.CarWashTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AsignacionScreenTest {
    @get:Rule val rule = createComposeRule()

    private class FakeRepo : AsignacionRepository {
        var vehicles = listOf(VehicleInProcess(10, "ABC123", tipoVehiculo = "Carro"), VehicleInProcess(11, "XYZ789", operadorAsignado = "Luis Gil", idOperario = 7))
        val assigned = mutableListOf<Pair<Int, Int>>()
        override suspend fun vehicles() = vehicles
        override suspend fun availableOperators() = listOf(Operator(5, "Ana", "Paz"))
        override suspend fun assign(turnId: Int, operatorId: Int, userId: Int): AssignResult {
            assigned += turnId to operatorId
            vehicles = vehicles.map { if (it.idTurno == turnId) it.copy(operadorAsignado = "Ana Paz", idOperario = operatorId) else it }
            return AssignResult.OK
        }
    }

    private fun show(repo: FakeRepo, admin: Boolean) {
        val model = AsignacionViewModel(repo, Session(1, "U", admin, "http://a/"))
        rule.setContent { CarWashTheme { AsignacionScreen(model, onBack = {}, onLogout = {}) } }
    }

    @Test fun listShowsOperatorOrUnassigned() {
        show(FakeRepo(), admin = true)
        rule.onNodeWithText("ABC123").assertExists()
        rule.onNodeWithText("Sin asignar").assertExists()
        rule.onNodeWithText("Operario: Luis Gil").assertExists()
    }

    @Test fun adminAssignsOperator() {
        val repo = FakeRepo()
        show(repo, admin = true)
        rule.onNodeWithText("ABC123").performClick()
        rule.onNodeWithText("Ana Paz").performClick()
        rule.waitUntil(5_000) { repo.assigned.isNotEmpty() }
        assertEquals(listOf(10 to 5), repo.assigned)
        rule.waitUntil(5_000) { rule.onAllNodesWithTextCount("Operario: Ana Paz") > 0 }
    }

    @Test fun nonAdminSeesHintAndCannotAssign() {
        val repo = FakeRepo()
        show(repo, admin = false)
        rule.onNodeWithText("Su perfil permite consultar, pero no asignar operarios.").assertExists()
        rule.onNodeWithText("ABC123").performClick()
        rule.onNodeWithText("Ana Paz").assertDoesNotExist()
    }
}

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTextCount(text: String) =
    onAllNodes(androidx.compose.ui.test.hasText(text)).fetchSemanticsNodes().size

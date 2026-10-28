package com.carwash.mobile.ingreso

import org.junit.Assert.*
import org.junit.Test

class PlateReadingTest {
    @Test
    fun suggestsSeparateJoinedAndMotorcycleLinesWithoutDuplicates() {
        assertEquals(listOf("ABC123", "XYZ45D"), plateCandidates("ABC-123\nABC 123\nXYZ\n45D"))
    }

    @Test
    fun doesNotInventCharacterCorrectionsOrRejectForeignSuggestions() {
        assertTrue("AOI012" in plateCandidates("AOI012"))
        assertTrue("A1234" in plateCandidates("A1234"))
        assertTrue("AB123CD" in plateCandidates("AB123CD"))
    }

    @Test
    fun ignoresEmptyNonPlateAndExcessivelyLongText() {
        assertTrue(plateCandidates("\n COLOMBIA\n123456\nAB12\nABCDEFGHIJ12345").isEmpty())
    }

    @Test
    fun limitsSuggestionsAndLeavesConfirmationEditable() {
        val result = plateCandidates((100..115).joinToString("\n") { "ABC$it" })
        assertEquals(8, result.size)
        assertEquals("ABC100", result.first())
        assertEquals("OI-012", reviewedPlate("  oi-012  "))
        assertEquals("ESPECIAL", reviewedPlate("especial"))
        assertNull(reviewedPlate("  "))
    }
}
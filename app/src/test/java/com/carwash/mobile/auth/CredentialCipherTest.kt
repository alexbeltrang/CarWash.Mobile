package com.carwash.mobile.auth

import org.junit.Assert.assertEquals
import org.junit.Test

// Vectores generados con RC2 de .NET 9 (misma clave, IV y relleno que el WinForm).
class CredentialCipherTest {
    @Test
    fun matchesWinFormCipher() {
        assertEquals("C/phaf+SSI4=", CredentialCipher.encode("admin"))
        assertEquals("4VNLJ1Ute9R7ko/Emzcomg==", CredentialCipher.encode("Admin2026*"))
        assertEquals("WZO0EBhZIjHHvTgWYR95ug==", CredentialCipher.encode("operario1"))
    }
}

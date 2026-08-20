package pl.i40.android.obd

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PidCatalogTest {
    @Test
    fun katalogZawieraWszystkiePidZTabeliWlacznieZNieobslugiwanymi() {
        val expected = setOf(
            0x01, 0x03, 0x04, 0x05, 0x06, 0x07, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
            0x10, 0x11, 0x13, 0x1C, 0x1F, 0x21, 0x23,
            0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B,
            0x2C, 0x2D, 0x2E, 0x2F, 0x30, 0x31, 0x33,
            0x3C, 0x3D, 0x3E, 0x3F,
            0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48,
            0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x51, 0x5A,
            0x5C, 0x5D, 0x5E, 0x61, 0x62, 0x63,
        )
        assertEquals(expected, PidCatalog.all.map { it.id }.toSet())
        assertTrue(PidCatalog.definition(0x5C) != null)
        assertTrue(PidCatalog.definition(0x10) != null)
        assertTrue(PidCatalog.definition(0x5E) != null)
    }

    @Test
    fun dekoderyZTabeli91() {
        assertEquals(DecodedPid.Numeric(750.0), PidCatalog.definition(0x0C)!!.decode(listOf(0x0B, 0xB8)))
        assertEquals(DecodedPid.Numeric(90.0), PidCatalog.definition(0x05)!!.decode(listOf(0x82)))
        assertEquals(DecodedPid.Numeric(0.0), PidCatalog.definition(0x06)!!.decode(listOf(0x80)))
        assertEquals(DecodedPid.Numeric(5.0), PidCatalog.definition(0x10)!!.decode(listOf(0x01, 0xF4)))
        val rpmPoza = PidCatalog.definition(0x0C)!!.decode(listOf(0xFF, 0xFF))!!
        assertTrue(PidCatalog.definition(0x0C)!!.isSuspect(rpmPoza))
    }
}

package pl.i40.android.obd

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SupportedPidsTest {
    @Test
    fun regresjaMasekZZapisuZAuta() {
        val all = SupportedPids.bezKontynuacji(
            SupportedPids.merge(
                SupportedPids.pids(fromHex = "4100BE3EA813"),
                SupportedPids.pids(fromHex = "4120A007F011"),
                SupportedPids.pids(fromHex = "4140FED00400"),
            ),
        )
        val oczekiwane = setOf(
            0x01, 0x03, 0x04, 0x05, 0x06, 0x07, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
            0x11, 0x13, 0x15, 0x1C, 0x1F,
            0x21, 0x23, 0x2E, 0x2F, 0x30, 0x31, 0x32, 0x33, 0x34, 0x3C,
            0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x49, 0x4A, 0x4C, 0x56,
        )
        assertEquals(oczekiwane, all)
        assertFalse(0x5C in all)
        assertFalse(0x5E in all)
        assertFalse(0x10 in all)
        assertTrue(0x2F in all)
        assertFalse(0x20 in all)
        assertFalse(0x40 in all)
        assertFalse(0x80 in all)
    }

    @Test
    fun bitKontynuacjiNieJestPidemPomiarowymAleJestWSurowejMasce() {
        val next = SupportedPids.pids(fromMask = listOf(0x00, 0x00, 0x00, 0x01), firstPid = 0x01)
        assertEquals(setOf(0x20), next)
        assertTrue(SupportedPids.indicatesNextRange(listOf(0x00, 0x00, 0x00, 0x01), 0x01))
        assertEquals(emptySet<Int>(), SupportedPids.bezKontynuacji(next))
    }

    @Test
    fun brak0160GdyBitKontynuacjiWyzerowany() {
        assertFalse(
            SupportedPids.indicatesNextRange(listOf(0xFE, 0xD0, 0x04, 0x00), firstPid = 0x41),
        )
        val commands = SupportedPids.maskCommandsToQuery(
            listOf(
                0x00 to listOf(0xBE, 0x3E, 0xA8, 0x13),
                0x20 to listOf(0xA0, 0x07, 0xF0, 0x11),
                0x40 to listOf(0xFE, 0xD0, 0x04, 0x00),
            ),
        )
        assertEquals(listOf(0x00, 0x20, 0x40), commands)
    }
}

package pl.i40.android.obd

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MultiPidTest {
    @Test
    fun budujeZapytanieDlaZestawu() {
        assertEquals("010C", MultiPid.command(listOf(0x0C)))
        assertEquals("010C0D05040E42", MultiPid.command(listOf(0x0C, 0x0D, 0x05, 0x04, 0x0E, 0x42)))
    }

    @Test
    fun parsujeOdpowiedzJednoramkowa() {
        val readings = MultiPid.parse("410C0E760D00055C\r\r>")
        assertEquals(listOf(0x0C, 0x0D, 0x05), readings.map { it.pid })
        assertEquals(DecodedPid.Numeric(925.5), readings[0].decoded)
        assertEquals(DecodedPid.Numeric(0.0), readings[1].decoded)
        assertEquals(DecodedPid.Numeric(52.0), readings[2].decoded)
    }

    @Test
    fun parsujeOdpowiedzWieloramkowa() {
        val text = "00F\r0:410C0E760D0005\r1:5C043C0E87423795\r>"
        val map = MultiPid.parseMap(text)
        assertEquals(6, map.size)
        assertEquals(DecodedPid.Numeric(925.5), map[0x0C]?.decoded)
        assertEquals(DecodedPid.Numeric(0.0), map[0x0D]?.decoded)
        assertEquals(DecodedPid.Numeric(52.0), map[0x05]?.decoded)
    }

    @Test
    fun nieznanyPidKonczyParsowanie() {
        val truncated = MultiPid.parse("410C0E76\r>")
        assertEquals(listOf(0x0C), truncated.map { it.pid })
    }

    @Test
    fun bledyElmDajaPustaListe() {
        assertTrue(MultiPid.parse("NO DATA\r\r>").isEmpty())
        assertTrue(MultiPid.parse("?\r>").isEmpty())
        assertTrue(MultiPid.parse("UNABLE TO CONNECT\r>").isEmpty())
    }
}

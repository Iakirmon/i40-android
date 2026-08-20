package pl.i40.android.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.rules.PasmaOdniesienia

class SiatkaPasmaTest {
    @Test
    fun linieZPasmemNormyZPasmaOdniesienia() {
        val linie = SiatkaPasma.linie(0x05)
        assertEquals(listOf(PasmaOdniesienia.plyn.start, PasmaOdniesienia.plyn.endInclusive), linie)
        assertFalse(linie.contains(20.0) && linie.size > 2)
    }

    @Test
    fun pasmoBrakDajeZeroLinii() {
        assertEquals(emptyList<Double>(), SiatkaPasma.linie(0x0E))
        assertEquals(emptyList<Double>(), SiatkaPasma.linie(0x0B))
        assertEquals(emptyList<Double>(), SiatkaPasma.linie(0x0F))
        assertEquals(emptyList<Double>(), SiatkaPasma.linie(FormatPowietrza.PID_PODCISNIENIE))
        assertFalse(SiatkaPasma.maSiatke(0x0E))
    }

    @Test
    fun sumaKorektZWirtualnegoPid() {
        val linie = SiatkaPasma.linie(FormatRaportu.PID_SUMA_KOREKT)
        assertEquals(
            listOf(PasmaOdniesienia.sumaKorekt.start, PasmaOdniesienia.sumaKorekt.endInclusive),
            linie
        )
        assertTrue(SiatkaPasma.maSiatke(FormatRaportu.PID_SUMA_KOREKT))
    }

    @Test
    fun olejTylkoDolnaGranica() {
        assertEquals(listOf(PasmaOdniesienia.OLEJ_MIN_C), SiatkaPasma.linie(0x5C))
    }
}

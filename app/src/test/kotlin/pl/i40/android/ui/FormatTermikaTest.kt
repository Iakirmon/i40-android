package pl.i40.android.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.acquisition.RingSample
import pl.i40.android.rules.PasmaOdniesienia

class FormatTermikaTest {
    @Test
    fun linieZTychSamychStalychCoKat1Kat2() {
        val linie = FormatTermika.linieKatalizatora()
        assertEquals(PasmaOdniesienia.KATALIZATOR_ZAPLON_C, linie[0])
        assertEquals(PasmaOdniesienia.katalizatorPraca.start, linie[1])
        assertEquals(PasmaOdniesienia.katalizatorPraca.endInclusive, linie[2])
        assertEquals(PasmaOdniesienia.progKat2C, linie[2])
        assertEquals(
            listOf(PasmaOdniesienia.plyn.start, PasmaOdniesienia.plyn.endInclusive),
            FormatTermika.liniePlynu()
        )
        assertEquals(listOf(PasmaOdniesienia.OLEJ_MIN_C), FormatTermika.linieOleju())
    }

    @Test
    fun osKatalizatoraDo1000() {
        assertEquals(0.0..1000.0, OsY.zakres(0x3C))
    }

    @Test
    fun kolejnoscRozgrzewaniaKatPlynOlej() {
        val kat = listOf(RingSample(0.0, 80.0), RingSample(30.0, 320.0), RingSample(120.0, 700.0))
        val plyn = listOf(RingSample(0.0, 20.0), RingSample(80.0, 91.0), RingSample(120.0, 95.0))
        val olej = listOf(RingSample(0.0, 20.0), RingSample(80.0, 60.0), RingSample(200.0, 91.0))
        val kolejnosc = FormatTermika.kolejnoscRozgrzewania(kat, plyn, olej)
        assertEquals(listOf("katalizator", "płyn", "olej"), kolejnosc)
    }

    @Test
    fun czasDo90ToPauzaGdyNigdy() {
        assertEquals(FormatPomiaru.NIEDOSTEPNE, FormatTermika.czasMmSs(null))
        assertFalseZero(FormatTermika.wierszCzasow(null, null))
        val wiersz = FormatTermika.wierszCzasow(384.0, 730.0)
        assertTrue(wiersz.contains("6:24"))
        assertTrue(wiersz.contains("12:10"))
    }

    @Test
    fun dolotIOtoczenieBezNormy() {
        assertEquals(FormatPomiaru.NIEDOSTEPNE, FormatTermika.normaDolotu())
        assertEquals(FormatPomiaru.NIEDOSTEPNE, FormatTermika.normaOtoczenia())
    }

    private fun assertFalseZero(text: String) {
        assertTrue(text.contains(FormatPomiaru.NIEDOSTEPNE))
        assertTrue(!text.contains("0:00"))
    }
}

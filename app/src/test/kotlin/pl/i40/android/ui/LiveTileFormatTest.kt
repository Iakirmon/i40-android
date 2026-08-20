package pl.i40.android.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.acquisition.OilTempEstimator
import pl.i40.android.acquisition.SampleStream

class LiveTileFormatTest {
    @Test
    fun czwartyKafelToKorektaDluga0107ANiePaliwo() {
        assertEquals(listOf(0x5C, 0x05, 0x42, 0x07), FormatKafla.KAFLI_DOMYSLNE)
        assertFalse(0x2F in FormatKafla.KAFLI_DOMYSLNE)
        assertEquals("KOREKTA D", FormatKafla.krotkaEtykieta(0x07))
        assertEquals(FormatKafla.PASMO_KOREKTY_DLUGIEJ, -10.0..10.0)
    }

    @Test
    fun rotacjaZimnaNieZawiera2fAZawiera0107() {
        val zimna = SampleStream.DEFAULT_COLD_PIDS
        assertFalse(0x2F in zimna)
        assertTrue(0x07 in zimna)
    }

    @Test
    fun krotkieEtykiety() {
        assertEquals("OLEJ", FormatKafla.krotkaEtykieta(0x5C))
        assertEquals("PŁYN", FormatKafla.krotkaEtykieta(0x05))
        assertEquals("NAPIĘCIE", FormatKafla.krotkaEtykieta(0x42))
    }

    @Test
    fun brakWartosciToPauzaNigdyZero() {
        assertEquals(FormatPomiaru.NIEDOSTEPNE, FormatKafla.wartosc(0x05, null))
        assertFalse(FormatKafla.wartosc(0x05, null).contains("0"))
    }

    @Test
    fun napiecieZJednymMiejscem() {
        val text = FormatKafla.wartosc(0x42, 13.9)
        assertTrue(text.contains("13,9") || text.contains("13.9"))
        assertTrue(text.contains("V"))
    }

    @Test
    fun olejZTyldaISzacunkiem() {
        val text = FormatKafla.olejTekst(85.0)
        assertTrue(text.startsWith("~"))
        assertTrue(text.contains("85"))
        val podpis = FormatKafla.olejPodpis(OilTempEstimator.Pewnosc.Srednia)
        assertTrue(podpis.contains("szacunek"))
        assertTrue(podpis.contains("0…150") || podpis.contains("0...150"))
        assertTrue(podpis.contains("średnia"))
    }
}

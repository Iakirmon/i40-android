package pl.i40.android.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.acquisition.OilTempEstimator
import pl.i40.android.acquisition.SampleStream
import pl.i40.android.rules.PasmaOdniesienia

class LiveTileFormatTest {
    @Test
    fun czwartyKafelToKorektaDluga0107ANiePaliwo() {
        assertEquals(listOf(0x5C, 0x05, 0x42, 0x07), FormatKafla.KAFLI_DOMYSLNE)
        assertFalse(0x2F in FormatKafla.KAFLI_DOMYSLNE)
        assertEquals("KOREKTA D", FormatKafla.krotkaEtykieta(0x07))
        assertEquals(FormatKafla.PASMO_KOREKTY_DLUGIEJ, -10.0..10.0)
    }

    @Test
    fun poziomCNieZawiera012FApoziomBZawiera0107() {
        val wolny = SampleStream.DEFAULT_SLOW_PIDS
        assertFalse(0x2F in wolny)
        assertTrue(0x07 in SampleStream.DEFAULT_MEDIUM_PIDS)
        assertFalse(0x07 in wolny)
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
        assertTrue(podpis.contains("≥ 90") || podpis.contains(">= 90"))
        assertTrue(podpis.contains("średnia"))
    }

    @Test
    fun trzeciWierszKafliZPasmaOdniesienia() {
        assertEquals("≥ 90", FormatKafla.pasmoKafla(0x5C))
        assertEquals("70–105", FormatKafla.pasmoKafla(0x05))
        assertTrue(FormatKafla.pasmoKafla(0x42).contains("13"))
        assertTrue(FormatKafla.pasmoKafla(0x07).contains("10"))
        assertEquals(FormatKafla.PASMO_KOREKTY_DLUGIEJ, PasmaOdniesienia.korektaDluga)
    }

    @Test
    fun czwartyKafelMilczyBezPotwierdzeniaPetliZamknietej() {
        val liczba = FormatKafla.wartosc(0x07, 3.9)
        assertEquals(liczba, FormatKafla.wartoscKorektyDlugiej(3.9, 2))
        assertEquals(liczba, FormatKafla.wartoscKorektyDlugiej(3.9, 16))
        val kreska = "${FormatPomiaru.NIEDOSTEPNE} ○"
        assertEquals(kreska, FormatKafla.wartoscKorektyDlugiej(3.9, 1))
        assertEquals(kreska, FormatKafla.wartoscKorektyDlugiej(3.9, 4))
        assertEquals(kreska, FormatKafla.wartoscKorektyDlugiej(3.9, 8))
        assertEquals(kreska, FormatKafla.wartoscKorektyDlugiej(3.9, 0))
        assertEquals(kreska, FormatKafla.wartoscKorektyDlugiej(3.9, null))
        assertEquals(kreska, FormatKafla.wartoscKorektyDlugiej(3.9, 3))
        assertTrue(FormatKafla.pasmoKafla(0x07).contains("10"))
        assertFalse(0x2F in SampleStream.DEFAULT_SLOW_PIDS)
        assertFalse(0x2F in SampleStream.DEFAULT_FAST_PIDS)
        assertFalse(0x2F in SampleStream.DEFAULT_MEDIUM_PIDS)
    }
}

package pl.i40.android.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.rules.PasmaOdniesienia
import pl.i40.android.storage.PodsumowaniePrzejazdu

class FormatRaportuTest {
    @Test
    fun diagnostykaPokazujeKreskeDlaPolNull() {
        val p = PodsumowaniePrzejazdu(czasTrwaniaS = 100.0)
        val d = FormatRaportu.diagnostyka(p)
        assertEquals(FormatPomiaru.NIEDOSTEPNE, d.cisnienie.wartosc)
        assertEquals(FormatPomiaru.NIEDOSTEPNE, d.katalizator.wartosc)
        assertEquals(FormatPomiaru.NIEDOSTEPNE, d.plyn90.wartosc)
        assertEquals(FormatPomiaru.NIEDOSTEPNE, d.korektyPoza.wartosc)
        val obciazenie = PasmaOdniesienia.szynaObciazenie
        assertEquals(
            "${obciazenie.start.toInt()} – ${obciazenie.endInclusive.toInt()} bar",
            d.cisnienie.norma
        )
        val kat = PasmaOdniesienia.katalizatorPraca
        assertEquals("${kat.start.toInt()} – ${kat.endInclusive.toInt()} °C", d.katalizator.norma)
        assertEquals(FormatPomiaru.NIEDOSTEPNE, d.plyn90.norma)
        assertTrue(d.korektyPoza.norma.contains(PasmaOdniesienia.sumaKorekt.endInclusive.toInt().toString()))
    }

    @Test
    fun naglowekMaPasmoAlboKreskeIZnacznikPozaPasmem() {
        val wNormie = FormatRaportu.naglowek(
            PodsumowaniePrzejazdu(maxPlynC = 94.0, maxObroty = 4210.0, maxPredkoscKmh = 118.0)
        )
        val plyn = wNormie.first { it.etykieta.contains("płyn") }
        assertEquals("", plyn.znacznik)
        assertEquals(
            "${PasmaOdniesienia.plyn.start.toInt()} – ${PasmaOdniesienia.plyn.endInclusive.toInt()}",
            plyn.norma
        )
        val obroty = wNormie.first { it.etykieta.contains("obroty") }
        assertEquals(FormatPomiaru.NIEDOSTEPNE, obroty.norma)

        val poza = FormatRaportu.naglowek(PodsumowaniePrzejazdu(maxPlynC = 108.0))
        assertEquals("▲", poza.first { it.etykieta.contains("płyn") }.znacznik)
        val zimny = FormatRaportu.naglowek(PodsumowaniePrzejazdu(maxPlynC = 68.0))
        assertEquals("▼", zimny.first { it.etykieta.contains("płyn") }.znacznik)
    }
}

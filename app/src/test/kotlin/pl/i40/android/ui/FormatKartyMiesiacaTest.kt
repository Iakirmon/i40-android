package pl.i40.android.ui

import java.util.Calendar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.storage.PodsumowaniePrzejazdu
import pl.i40.android.storage.Przejazd
import pl.i40.android.storage.StatusPrzejazdu
import pl.i40.android.storage.TrackBlob

class FormatKartyMiesiacaTest {
    private val cal = SiatkaMiesiaca.kalendarzPolski()

    @Test
    fun pustyMiesiacDajeKreskiNieZera() {
        cal.clear()
        cal.set(2026, Calendar.AUGUST, 1)
        val karta = FormatKartyMiesiaca.zPrzejazdow(emptyList(), cal.timeInMillis, cal)
        val tekst = karta.wiersze.joinToString("\n") { "${it.wartosc} ${it.poprzedni} ${it.roznica}" }
        assertFalse(tekst.contains(Regex("(?<![0-9])0(?![0-9])")))
        for (w in karta.wiersze) {
            assertEquals(FormatPomiaru.NIEDOSTEPNE, w.wartosc, w.etykieta)
        }
    }

    @Test
    fun kolumnaRoznicyBezSlowOceniajacych() {
        val sierpien = miesiac(2026, Calendar.AUGUST)
        val lipiec = miesiac(2026, Calendar.JULY)
        val przejazdy = listOf(
            przejazd(sierpien + 86_400_000L, czasDo90 = null, dystans = 10.0, czasS = 600.0),
            przejazd(sierpien + 2 * 86_400_000L, czasDo90 = 400.0, dystans = 20.0, czasS = 1200.0),
            przejazd(lipiec + 86_400_000L, czasDo90 = 372.0, dystans = 15.0, czasS = 900.0)
        )
        val karta = FormatKartyMiesiaca.zPrzejazdow(przejazdy, sierpien, cal)
        val roznice = karta.wiersze.joinToString(" ") { it.roznica }
        for (slowo in listOf("pogorszenie", "lepiej", "gorzej", "poprawa", "▲", "▼", "zalecane")) {
            assertFalse(roznice.contains(slowo), slowo)
        }
        assertTrue(karta.wiersze.any { it.roznica.contains("+") || it.roznica == FormatPomiaru.NIEDOSTEPNE })
    }

    @Test
    fun bezRozgrzaniaLiczyNulleBezProguTemperatury() {
        val sierpien = miesiac(2026, Calendar.AUGUST)
        val przejazdy = listOf(
            przejazd(sierpien + 1_000L, czasDo90 = null, maxPlyn = 80.0),
            przejazd(sierpien + 2_000L, czasDo90 = 12.0, maxPlyn = 40.0),
            przejazd(sierpien + 3_000L, czasDo90 = null, maxPlyn = 95.0)
        )
        val karta = FormatKartyMiesiaca.zPrzejazdow(przejazdy, sierpien, cal)
        val w = karta.wiersze.first { it.etykieta.contains("rozgrzania", ignoreCase = true) }
        assertEquals("2 z 3", w.wartosc)
        assertFalse(w.wartosc.contains("90"))
        assertFalse(w.wartosc.contains("70"))
    }

    private fun miesiac(rok: Int, miesiac: Int): Long {
        cal.clear()
        cal.set(rok, miesiac, 1)
        return SiatkaMiesiaca.poczatekMiesiaca(cal.timeInMillis, cal)
    }

    private fun przejazd(
        start: Long,
        czasDo90: Double? = null,
        dystans: Double? = null,
        czasS: Double = 0.0,
        maxPlyn: Double? = null
    ) = Przejazd(
        id = "$start",
        poczatekMs = start,
        koniecMs = start + 1000,
        status = StatusPrzejazdu.Zamkniety,
        vin = null,
        notatka = "",
        podsumowanie = PodsumowaniePrzejazdu(
            czasTrwaniaS = czasS,
            dystansKm = dystans,
            maxPlynC = maxPlyn,
            czasDo90CSekundy = czasDo90
        ),
        przebieg = TrackBlob(),
        checkpointMs = start
    )
}

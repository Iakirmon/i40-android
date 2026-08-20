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

class FormatKartyOdPoczatkuTest {
    private val cal = SiatkaMiesiaca.kalendarzPolski()

    @Test
    fun pustyZbiorDajeKreskiNieZera() {
        val karta = FormatKartyOdPoczatku.zPrzejazdow(emptyList(), cal)
        for (w in karta.wiersze) {
            assertEquals(FormatPomiaru.NIEDOSTEPNE, w.wartosc, w.etykieta)
        }
        val tekst = karta.wiersze.joinToString(" ") { it.wartosc }
        assertFalse(tekst.contains(Regex("(?<![0-9])0(?![0-9])")))
    }

    @Test
    fun kartaLiczySumyNieMediany() {
        cal.clear()
        cal.set(2026, Calendar.AUGUST, 1, 8, 0, 0)
        val a = przejazd("a", cal.timeInMillis, dystans = 10.0, czasS = 600.0, do90 = 100.0)
        cal.set(Calendar.DAY_OF_MONTH, 2)
        val b = przejazd("b", cal.timeInMillis, dystans = 30.0, czasS = 1800.0, do90 = 500.0)
        val karta = FormatKartyOdPoczatku.zPrzejazdow(listOf(a, b), cal)
        assertEquals("40,0 km", karta.wiersze.first { it.etykieta == "Dystans" }.wartosc)
        assertFalse(karta.wiersze.any { it.wartosc.contains("20,0") })
    }

    @Test
    fun bezRozgrzaniaLiczyNulleBezProgu() {
        cal.clear()
        cal.set(2026, Calendar.AUGUST, 1)
        val a = przejazd("a", cal.timeInMillis, do90 = null, maxPlyn = 95.0)
        val b = przejazd("b", cal.timeInMillis + 1, do90 = 12.0, maxPlyn = 40.0)
        val karta = FormatKartyOdPoczatku.zPrzejazdow(listOf(a, b), cal)
        val w = karta.wiersze.first { it.etykieta.contains("rozgrzania", ignoreCase = true) }
        assertEquals("1 z 2", w.wartosc)
        assertFalse(w.wartosc.contains("90"))
        assertFalse(w.wartosc.contains("70"))
    }

    @Test
    fun filtrZawezaKropkiIListeJednoczesnie() {
        cal.clear()
        cal.set(2026, Calendar.AUGUST, 8, 10, 0, 0)
        val zKodem = przejazd("k", cal.timeInMillis, kody = listOf("P0171"))
        cal.set(Calendar.DAY_OF_MONTH, 9)
        val bez = przejazd("b", cal.timeInMillis)
        val wszystkie = listOf(zKodem, bez)
        val filtr = FiltrHistorii(zKodami = true)
        val widoczne = filtr.zastosuj(wszystkie)
        val miesiac = SiatkaMiesiaca.poczatekMiesiaca(zKodem.poczatekMs, cal)
        val dni = widoczne.map { SiatkaMiesiaca.poczatekDnia(it.poczatekMs, cal) }.toSet()
        val cells = SiatkaMiesiaca.komorki(miesiac, dni, cal)
        val lista8 = SiatkaMiesiaca.sesjeDnia(zKodem.poczatekMs, widoczne, cal)
        val lista9 = SiatkaMiesiaca.sesjeDnia(bez.poczatekMs, widoczne, cal)
        assertTrue(cells.filterIsInstance<KomorkaMiesiaca.Dzien>().any { it.maSesje })
        assertEquals(1, lista8.size)
        assertTrue(lista9.isEmpty())
        val dniZKropka = cells.filterIsInstance<KomorkaMiesiaca.Dzien>().filter { it.maSesje }
        for (d in dniZKropka) {
            assertTrue(SiatkaMiesiaca.sesjeDnia(d.dzienMs, widoczne, cal).isNotEmpty())
        }
    }

    @Test
    fun pustyWynikFiltraMaKomunikat() {
        val filtr = FiltrHistorii(chronione = true)
        val widoczne = filtr.zastosuj(listOf(przejazd("a", 1_000L)))
        assertTrue(widoczne.isEmpty())
        assertEquals("Brak przejazdów spełniających filtr", FiltrHistorii.KOMUNIKAT_PUSTY)
    }

    private fun przejazd(
        id: String,
        start: Long,
        dystans: Double? = 1.0,
        czasS: Double = 60.0,
        do90: Double? = 10.0,
        maxPlyn: Double? = null,
        kody: List<String> = emptyList()
    ) = Przejazd(
        id = id,
        poczatekMs = start,
        koniecMs = start + 1,
        status = StatusPrzejazdu.Zamkniety,
        vin = "VIN",
        notatka = "",
        podsumowanie = PodsumowaniePrzejazdu(
            czasTrwaniaS = czasS,
            dystansKm = dystans,
            czasDo90CSekundy = do90,
            maxPlynC = maxPlyn,
            kodyNaKoncu = kody
        ),
        przebieg = TrackBlob(),
        checkpointMs = start
    )
}

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

class TrybZaznaczaniaTest {
    private val cal = SiatkaMiesiaca.kalendarzPolski()

    @Test
    fun zaznaczDzienPomijaWToku() {
        val a = przejazd("a", StatusPrzejazdu.Zamkniety)
        val b = przejazd("b", StatusPrzejazdu.WToku)
        val c = przejazd("c", StatusPrzejazdu.Odzyskany)
        val stan = TrybZaznaczania().poPrzytrzymaniu(a).zaznaczDzien(listOf(a, b, c))
        assertEquals(setOf("a", "c"), stan.zaznaczone)
        assertFalse(stan.maPoleWyboru(b))
        assertTrue(stan.maPoleWyboru(a))
    }

    @Test
    fun przytrzymanieWTokuWchodziWTrybBezZaznaczenia() {
        val b = przejazd("b", StatusPrzejazdu.WToku)
        val stan = TrybZaznaczania().poPrzytrzymaniu(b)
        assertTrue(stan.aktywny)
        assertTrue(stan.zaznaczone.isEmpty())
    }

    @Test
    fun kasowanieWielokrotneOdrzucaWToku() {
        val a = przejazd("a", StatusPrzejazdu.Zamkniety)
        val b = przejazd("b", StatusPrzejazdu.WToku)
        val mag = pl.i40.android.storage.PamiecPrzejazdow()
        mag.wstaw(a)
        mag.wstaw(b)
        val wynik = mag.usunWiele(listOf("a", "b"))
        assertEquals(listOf("a"), wynik.doUsuniecia)
        assertEquals(listOf("b"), wynik.odrzuconoWToku)
        assertTrue(mag.czytaj("b") != null)
        assertTrue(mag.czytaj("a") == null)
    }

    @Test
    fun oknoWielokrotnePokazujeSumeIListe() {
        val a = przejazd(
            "a",
            StatusPrzejazdu.Zamkniety,
            start = chwila(15, 14, 3),
            czasS = 12 * 60.0,
            dystans = 6.1
        )
        val b = przejazd(
            "b",
            StatusPrzejazdu.Odzyskany,
            start = chwila(15, 17, 48),
            czasS = 8 * 60.0,
            dystans = 3.2
        )
        val t = FormatPotwierdzenia.wielokrotne(listOf(a, b), liczbaPunktow = 2, bajty = 524_288L, cal = cal)
        assertEquals("Usunąć 2 przejazdy?", t.naglowek)
        assertTrue(t.coGinie.contains("20 min"))
        assertTrue(t.coGinie.contains("9,3 km"))
        assertTrue(t.coGinie.contains("0,5 MB"))
        assertEquals(2, t.pozycje.size)
        assertTrue(t.pozycje[1].contains("przerwany"))
        assertEquals("Tych nagrań nie da się odtworzyć.", t.nieodtwarzalne)
        assertTrue(t.coZostaje.contains("2 punkty"))
        assertEquals("Usuń 2", t.przyciskUsun)
        assertFalse(t.coGinie.contains("▲"))
        assertFalse(t.coGinie.contains("lepiej"))
    }

    @Test
    fun powyzejDziesieciuListaSieZwijaASumaZostaje() {
        val lista = (1..12).map { i ->
            przejazd("p$i", StatusPrzejazdu.Zamkniety, start = chwila(1, 8, i), czasS = 60.0, dystans = 1.0)
        }
        val t = FormatPotwierdzenia.wielokrotne(lista, liczbaPunktow = 0, bajty = 1024L, cal = cal)
        assertEquals(10, t.pozycje.size)
        assertEquals("… i 2 dalszych", t.dalsze)
        assertTrue(t.naglowek.contains("12"))
        assertTrue(t.coGinie.contains("12 min"))
    }

    private fun chwila(dzien: Int, godzina: Int, minuta: Int): Long {
        cal.clear()
        cal.set(2026, Calendar.AUGUST, dzien, godzina, minuta, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun przejazd(
        id: String,
        status: StatusPrzejazdu,
        start: Long = 1_000L,
        czasS: Double = 60.0,
        dystans: Double? = 1.0
    ) = Przejazd(
        id = id,
        poczatekMs = start,
        koniecMs = start + 1_000L,
        status = status,
        vin = "VIN",
        notatka = "",
        podsumowanie = PodsumowaniePrzejazdu(czasTrwaniaS = czasS, dystansKm = dystans, liczbaProbek = 10),
        przebieg = TrackBlob(),
        checkpointMs = start
    )
}

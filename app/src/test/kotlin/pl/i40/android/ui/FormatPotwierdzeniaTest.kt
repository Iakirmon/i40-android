package pl.i40.android.ui

import java.util.Calendar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.storage.PodsumowaniePrzejazdu
import pl.i40.android.storage.Przejazd
import pl.i40.android.storage.StatusPrzejazdu
import pl.i40.android.storage.TrackBlob

class FormatPotwierdzeniaTest {
    private val cal = SiatkaMiesiaca.kalendarzPolski()

    @Test
    fun czteryBlokiNiePytanieCzyNaPewno() {
        val p = przejazd(
            start = chwila(2026, Calendar.AUGUST, 15, 14, 3),
            czasS = 12 * 60.0,
            dystans = 6.1,
            probek = 2880
        )
        val t = FormatPotwierdzenia.pojedynczy(p, liczbaPunktow = 1, bajty = 314_573L, cal = cal)
        assertEquals("Usunąć ten przejazd?", t.naglowek)
        assertEquals("15 sierpnia 2026, 14:03", t.dataGodzina)
        assertTrue(t.coGinie.contains("12 min"))
        assertTrue(t.coGinie.contains("6,1 km"))
        assertTrue(t.coGinie.contains("2 880"))
        assertTrue(t.coGinie.contains("0,3 MB"))
        assertEquals("Tego nagrania nie da się odtworzyć.", t.nieodtwarzalne)
        assertTrue(t.coZostaje.contains("1 punkt"))
        assertTrue(t.coZostaje.contains("poprzednio"))
        assertEquals("Karta miesiąca dla sierpnia przeliczy się.", t.kartaMiesiaca)
        assertNull(t.chroniony)
        val zlaczone = listOf(t.naglowek, t.dataGodzina, t.coGinie, t.nieodtwarzalne, t.coZostaje, t.kartaMiesiaca)
            .joinToString("\n")
        assertFalse(zlaczone.contains("Czy na pewno"))
        assertFalse(zlaczone.contains("cofnij", ignoreCase = true))
    }

    @Test
    fun chronionyDopisujeWierszOchrony() {
        val p = przejazd(start = chwila(2026, Calendar.JULY, 1, 8, 0), chroniony = true)
        val t = FormatPotwierdzenia.pojedynczy(p, liczbaPunktow = 0, bajty = 1024L, cal = cal)
        assertEquals("Ten przejazd jest chroniony.", t.chroniony)
    }

    @Test
    fun gestNieKasujeBezOkna() {
        val p = przejazd(start = chwila(2026, Calendar.AUGUST, 15, 14, 3))
        var stan = StanPotwierdzenia()
        stan = stan.poGescie(p)
        assertEquals(p.id, stan.oczekujace?.id)
        assertTrue(stan.oczekujePotwierdzenia)
        val (poAnulowaniu, usuniete) = stan.poAnulowaniu()
        assertNull(usuniete)
        assertFalse(poAnulowaniu.oczekujePotwierdzenia)
    }

    @Test
    fun gestNaWTokuOdrzucony() {
        val p = przejazd(start = chwila(2026, Calendar.AUGUST, 15, 14, 3), status = StatusPrzejazdu.WToku)
        val stan = StanPotwierdzenia().poGescie(p)
        assertFalse(stan.oczekujePotwierdzenia)
        assertNull(stan.oczekujace)
    }

    @Test
    fun przyciskWTokuOdrzucony() {
        val p = przejazd(start = chwila(2026, Calendar.AUGUST, 15, 14, 3), status = StatusPrzejazdu.WToku)
        val stan = StanPotwierdzenia().poPrzycisku(p)
        assertFalse(stan.oczekujePotwierdzenia)
    }

    @Test
    fun potwierdzenieDopieroKasuje() {
        val p = przejazd(start = chwila(2026, Calendar.AUGUST, 15, 14, 3))
        val poGescie = StanPotwierdzenia().poGescie(p)
        val (poPotwierdzeniu, doUsuniecia) = poGescie.poPotwierdzeniu()
        assertEquals(p.id, doUsuniecia?.id)
        assertFalse(poPotwierdzeniu.oczekujePotwierdzenia)
    }

    private fun chwila(rok: Int, miesiac: Int, dzien: Int, godzina: Int, minuta: Int): Long {
        cal.clear()
        cal.set(rok, miesiac, dzien, godzina, minuta, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun przejazd(
        start: Long,
        czasS: Double = 600.0,
        dystans: Double? = 1.0,
        probek: Int = 10,
        status: StatusPrzejazdu = StatusPrzejazdu.Zamkniety,
        chroniony: Boolean = false
    ) = Przejazd(
        id = "p-$start",
        poczatekMs = start,
        koniecMs = start + (czasS * 1000).toLong(),
        status = status,
        vin = "VIN",
        notatka = "",
        podsumowanie = PodsumowaniePrzejazdu(czasTrwaniaS = czasS, dystansKm = dystans, liczbaProbek = probek),
        przebieg = TrackBlob(),
        checkpointMs = start,
        chroniony = chroniony
    )
}

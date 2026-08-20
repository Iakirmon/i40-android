package pl.i40.android.ui

import java.util.Calendar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.storage.PodsumowaniePrzejazdu
import pl.i40.android.storage.Przejazd
import pl.i40.android.storage.StatusPrzejazdu
import pl.i40.android.storage.TrackBlob

class SiatkaMiesiacaTest {
    private val cal = SiatkaMiesiaca.kalendarzPolski()

    @Test
    fun pierwszyDzienTygodniaToPoniedzialek() {
        assertEquals(Calendar.MONDAY, cal.firstDayOfWeek)
        val symbols = SiatkaMiesiaca.skrotyDni(cal)
        assertEquals(7, symbols.size)
        assertTrue(symbols.first().lowercase().startsWith("p"))
    }

    @Test
    fun lutyPrzestepnyMa29Dni() {
        cal.clear()
        cal.set(2024, Calendar.FEBRUARY, 1)
        val cells = SiatkaMiesiaca.komorki(cal.timeInMillis, emptySet(), cal)
        val dni = cells.filterIsInstance<KomorkaMiesiaca.Dzien>()
        assertEquals(29, dni.size)
    }

    @Test
    fun miesiacZaczynajacySieWNiedzieleMaSzescPustych() {
        // 1.03.2026 = niedziela; firstWeekday=pon → 6 pustych komórek.
        cal.clear()
        cal.set(2026, Calendar.MARCH, 1)
        val cells = SiatkaMiesiaca.komorki(cal.timeInMillis, emptySet(), cal)
        val leading = cells.takeWhile { it is KomorkaMiesiaca.Pusta }.size
        assertEquals(6, leading)
        assertEquals(31, cells.filterIsInstance<KomorkaMiesiaca.Dzien>().size)
    }

    @Test
    fun kropkaNaDniuZSesja() {
        cal.clear()
        cal.set(2026, Calendar.AUGUST, 8)
        val day = cal.timeInMillis
        val month = SiatkaMiesiaca.poczatekMiesiaca(day, cal)
        val cells = SiatkaMiesiaca.komorki(month, setOf(day), cal)
        assertTrue(
            cells.any { it is KomorkaMiesiaca.Dzien && it.maSesje && SiatkaMiesiaca.tenSamDzien(it.dzienMs, day, cal) }
        )
    }

    @Test
    fun filtrSesjiDnia() {
        cal.clear()
        cal.set(2026, Calendar.AUGUST, 8, 10, 0, 0)
        val morning = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 18)
        val evening = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, 9)
        val nextDay = cal.timeInMillis
        val a = przejazd("a", morning)
        val b = przejazd("b", evening)
        val c = przejazd("c", nextDay)
        val onEighth = SiatkaMiesiaca.sesjeDnia(morning, listOf(a, b, c), cal)
        assertEquals(2, onEighth.size)
        assertTrue(onEighth[0].poczatekMs >= onEighth[1].poczatekMs)
    }

    private fun przejazd(id: String, start: Long) = Przejazd(
        id = id,
        poczatekMs = start,
        koniecMs = start + 1000,
        status = StatusPrzejazdu.Zamkniety,
        vin = null,
        notatka = "",
        podsumowanie = PodsumowaniePrzejazdu(),
        przebieg = TrackBlob(),
        checkpointMs = start
    )
}

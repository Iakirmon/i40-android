package pl.i40.android.ui

import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale
import pl.i40.android.storage.Przejazd

sealed class KomorkaMiesiaca {
    data object Pusta : KomorkaMiesiaca()
    data class Dzien(val dzienMs: Long, val maSesje: Boolean) : KomorkaMiesiaca()
}

/** Siatka miesiąca — `java.util.Calendar`, bez bibliotek do dat. Poniedziałek pierwszy (PL). */
object SiatkaMiesiaca {
    fun kalendarzPolski(): Calendar {
        val cal = Calendar.getInstance(Locale("pl", "PL"))
        cal.firstDayOfWeek = Calendar.MONDAY
        return cal
    }

    fun skrotyDni(cal: Calendar = kalendarzPolski()): List<String> {
        val symbols = DateFormatSymbols(Locale("pl", "PL")).shortWeekdays
        val first = cal.firstDayOfWeek
        return (0 until 7).map { i ->
            val idx = (first - 1 + i) % 7 + 1
            symbols[idx]
        }
    }

    fun komorki(miesiacMs: Long, dniSesjiMs: Set<Long>, cal: Calendar = kalendarzPolski()): List<KomorkaMiesiaca> {
        val c = cal.clone() as Calendar
        c.timeInMillis = miesiacMs
        c.set(Calendar.DAY_OF_MONTH, 1)
        zerujCzas(c)
        val weekdayOfFirst = c.get(Calendar.DAY_OF_WEEK)
        val leading = (weekdayOfFirst - c.firstDayOfWeek + 7) % 7
        val days = c.getActualMaximum(Calendar.DAY_OF_MONTH)
        val sessionKeys = dniSesjiMs.map { kluczDnia(it, cal) }.toSet()
        val out = MutableList<KomorkaMiesiaca>(leading) { KomorkaMiesiaca.Pusta }
        for (day in 1..days) {
            c.timeInMillis = miesiacMs
            c.set(Calendar.DAY_OF_MONTH, day)
            zerujCzas(c)
            val ms = c.timeInMillis
            out.add(KomorkaMiesiaca.Dzien(ms, kluczDnia(ms, cal) in sessionKeys))
        }
        return out
    }

    fun przesunMiesiac(miesiacMs: Long, delta: Int, cal: Calendar = kalendarzPolski()): Long {
        val c = cal.clone() as Calendar
        c.timeInMillis = poczatekMiesiaca(miesiacMs, cal)
        c.add(Calendar.MONTH, delta)
        return c.timeInMillis
    }

    fun poczatekMiesiaca(ms: Long, cal: Calendar = kalendarzPolski()): Long {
        val c = cal.clone() as Calendar
        c.timeInMillis = ms
        c.set(Calendar.DAY_OF_MONTH, 1)
        zerujCzas(c)
        return c.timeInMillis
    }

    fun poczatekDnia(ms: Long, cal: Calendar = kalendarzPolski()): Long {
        val c = cal.clone() as Calendar
        c.timeInMillis = ms
        zerujCzas(c)
        return c.timeInMillis
    }

    fun tenSamDzien(a: Long, b: Long, cal: Calendar = kalendarzPolski()): Boolean =
        kluczDnia(a, cal) == kluczDnia(b, cal)

    fun tytulMiesiaca(ms: Long, cal: Calendar = kalendarzPolski()): String {
        val c = cal.clone() as Calendar
        c.timeInMillis = ms
        val miesiac = c.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("pl", "PL"))
        return "${miesiac.orEmpty()} ${c.get(Calendar.YEAR)}"
    }

    fun sesjeDnia(dzienMs: Long, z: List<Przejazd>, cal: Calendar = kalendarzPolski()): List<Przejazd> =
        z.filter { tenSamDzien(it.poczatekMs, dzienMs, cal) }.sortedByDescending { it.poczatekMs }

    fun kluczDnia(ms: Long, cal: Calendar = kalendarzPolski()): String {
        val c = cal.clone() as Calendar
        c.timeInMillis = ms
        return "${c.get(Calendar.YEAR)}-${c.get(Calendar.MONTH) + 1}-${c.get(Calendar.DAY_OF_MONTH)}"
    }

    private fun zerujCzas(c: Calendar) {
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
    }
}

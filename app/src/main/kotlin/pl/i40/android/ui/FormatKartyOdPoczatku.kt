package pl.i40.android.ui

import java.util.Calendar
import java.util.Locale
import pl.i40.android.storage.Przejazd
import pl.i40.android.storage.StatusPrzejazdu

data class FiltrHistorii(val zKodami: Boolean = false, val przerwane: Boolean = false, val chronione: Boolean = false) {
    val aktywny: Boolean get() = zKodami || przerwane || chronione

    fun przepusc(p: Przejazd): Boolean {
        if (zKodami && p.podsumowanie.kodyNaStarcie.isEmpty() && p.podsumowanie.kodyNaKoncu.isEmpty()) {
            return false
        }
        if (przerwane && p.status != StatusPrzejazdu.Odzyskany) return false
        if (chronione && !p.chroniony) return false
        return true
    }

    fun zastosuj(przejazdy: List<Przejazd>): List<Przejazd> = przejazdy.filter { przepusc(it) }

    companion object {
        const val KOMUNIKAT_PUSTY = "Brak przejazdów spełniających filtr"
    }
}

data class WierszOdPoczatku(val etykieta: String, val wartosc: String)

data class KartaOdPoczatku(val wiersze: List<WierszOdPoczatku>)

/**
 * Karta „od początku” — §7 warstwy historii. Sumy, nie mediany. Pusty zbiór to kreski.
 */
object FormatKartyOdPoczatku {
    fun zPrzejazdow(przejazdy: List<Przejazd>, cal: Calendar): KartaOdPoczatku {
        val zamkniete = przejazdy.filter { it.status != StatusPrzejazdu.WToku }
        if (zamkniete.isEmpty()) {
            return KartaOdPoczatku(
                listOf(
                    WierszOdPoczatku("Pierwszy zapis", FormatPomiaru.NIEDOSTEPNE),
                    WierszOdPoczatku("Przejazdy", FormatPomiaru.NIEDOSTEPNE),
                    WierszOdPoczatku("Dystans", FormatPomiaru.NIEDOSTEPNE),
                    WierszOdPoczatku("Czas za kierownicą", FormatPomiaru.NIEDOSTEPNE),
                    WierszOdPoczatku("Bez rozgrzania", FormatPomiaru.NIEDOSTEPNE),
                    WierszOdPoczatku("Zajęte miejsce", FormatPomiaru.NIEDOSTEPNE)
                )
            )
        }
        val pierwszy = zamkniete.minBy { it.poczatekMs }
        val km = zamkniete.mapNotNull { it.podsumowanie.dystansKm }.sum()
        val czasS = zamkniete.sumOf { it.podsumowanie.czasTrwaniaS }
        val bez = zamkniete.count { it.podsumowanie.czasDo90CSekundy == null }
        val mb = FormatPomiaru.liczba(ZajeteMiejsce.suma(przejazdy) / (1024.0 * 1024.0), 1, "MB")
        return KartaOdPoczatku(
            listOf(
                WierszOdPoczatku("Pierwszy zapis", dataDluga(pierwszy.poczatekMs, cal)),
                WierszOdPoczatku("Przejazdy", zamkniete.size.toString()),
                WierszOdPoczatku("Dystans", FormatPomiaru.liczba(km, 1, "km")),
                WierszOdPoczatku("Czas za kierownicą", formatGodziny(czasS)),
                WierszOdPoczatku("Bez rozgrzania", "$bez z ${zamkniete.size}"),
                WierszOdPoczatku("Zajęte miejsce", mb)
            )
        )
    }

    private fun dataDluga(ms: Long, cal: Calendar): String {
        val c = cal.clone() as Calendar
        c.timeInMillis = ms
        val dzien = c.get(Calendar.DAY_OF_MONTH)
        val miesiac = c.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("pl", "PL")).orEmpty()
        val rok = c.get(Calendar.YEAR)
        return "$dzien $miesiac $rok"
    }

    private fun formatGodziny(seconds: Double): String {
        val totalMin = kotlin.math.round(seconds / 60.0).toInt()
        val h = totalMin / 60
        val m = totalMin % 60
        return "$h h ${"%02d".format(m)}"
    }
}

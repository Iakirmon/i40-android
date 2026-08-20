package pl.i40.android.ui

import java.util.Calendar
import java.util.Locale
import pl.i40.android.storage.KryteriumPorzadkow
import pl.i40.android.storage.Przejazd
import pl.i40.android.storage.WyborPorzadkow

data class WidokPorzadkow(
    val zajete: String,
    val wierszUsuniecia: String,
    val wierszChronione: String,
    val wierszWToku: String,
    val lista: List<String>,
    val ids: List<String>,
    val przyciskAktywny: Boolean,
    val przycisk: String
)

object ZajeteMiejsce {
    fun bajty(p: Przejazd): Long = p.przebieg.encode().size.toLong()

    fun suma(przejazdy: List<Przejazd>): Long = przejazdy.sumOf { bajty(it) }
}

/**
 * Panel Porządki — §5 warstwy historii.
 * Minuty i miesiące to kryteria wyboru, nie progi i nie wartości zalecane.
 */
object FormatPorzadkow {
    val MINUTY = listOf(2, 5, 10, 15)
    val MIESIACE = listOf(3, 6, 12)

    fun widok(przejazdy: List<Przejazd>, kryterium: KryteriumPorzadkow?, terazMs: Long): WidokPorzadkow {
        val mbAll = FormatPomiaru.liczba(ZajeteMiejsce.suma(przejazdy) / (1024.0 * 1024.0), 1, "MB")
        val zajete = "$mbAll w ${przejazdy.size} przejazdach"
        if (kryterium == null) {
            return WidokPorzadkow(
                zajete = zajete,
                wierszUsuniecia = "0 przejazdów · 0,0 MB",
                wierszChronione = "0 chronionych — pominięte",
                wierszWToku = "0 nagrywana teraz — pominięta",
                lista = emptyList(),
                ids = emptyList(),
                przyciskAktywny = false,
                przycisk = "Usuń 0 przejazdów"
            )
        }
        val wybor = WyborPorzadkow.ktoreDoUsuniecia(przejazdy, kryterium, terazMs)
        val doUsuniecia = przejazdy.filter { it.id in wybor.doUsuniecia }
        val mbUsun = FormatPomiaru.liczba(ZajeteMiejsce.suma(doUsuniecia) / (1024.0 * 1024.0), 1, "MB")
        val n = doUsuniecia.size
        val cal = Calendar.getInstance(Locale("pl", "PL"))
        return WidokPorzadkow(
            zajete = zajete,
            wierszUsuniecia = "${odmiana(n)} · $mbUsun",
            wierszChronione = chronione(wybor.pominietoChronione),
            wierszWToku = wToku(wybor.pominietoWToku),
            lista = doUsuniecia.map { wiersz(it, cal) },
            ids = doUsuniecia.map { it.id },
            przyciskAktywny = n > 0,
            przycisk = "Usuń ${odmiana(n)}"
        )
    }

    private fun odmiana(n: Int): String {
        val slowo = when {
            n == 1 -> "przejazd"
            n % 10 in 2..4 && n % 100 !in 12..14 -> "przejazdy"
            else -> "przejazdów"
        }
        return "$n $slowo"
    }

    private fun chronione(n: Int): String {
        val slowo = when {
            n == 1 -> "chroniony"
            n in 2..4 -> "chronione"
            else -> "chronionych"
        }
        val pomin = if (n == 1) "pominięty" else "pominięte"
        return "$n $slowo — $pomin"
    }

    private fun wToku(n: Int): String = "$n nagrywana teraz — pominięta"

    private fun wiersz(p: Przejazd, cal: Calendar): String {
        val c = cal.clone() as Calendar
        c.timeInMillis = p.poczatekMs
        val dzien = c.get(Calendar.DAY_OF_MONTH)
        val skrot = c.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale("pl", "PL")).orEmpty()
        val godz = String.format(Locale("pl", "PL"), "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
        val minuty = kotlin.math.round(p.podsumowanie.czasTrwaniaS / 60.0).toInt()
        val km = p.podsumowanie.dystansKm?.let { FormatPomiaru.liczba(it, 1, "km") } ?: FormatPomiaru.NIEDOSTEPNE
        return "$dzien $skrot $godz    $minuty min   $km"
    }
}

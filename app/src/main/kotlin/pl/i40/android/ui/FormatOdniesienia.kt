package pl.i40.android.ui

import pl.i40.android.storage.PunktOdniesienia

/**
 * Tekst „poprzednio” na panelu Podstawowy — sekcja 10.3.
 * Zakres od drugiego punktu, nie od trzeciego. Nigdy etykieta „norma”.
 */
object FormatOdniesienia {
    const val PIERWSZY_POMIAR = "pierwszy pomiar — brak porównania"

    fun wiersz(pid: Int, punkty: List<PunktOdniesienia>): String {
        val wartosci = punkty.mapNotNull { it.odczyty[pid] }
        if (wartosci.isEmpty()) return PIERWSZY_POMIAR
        val ostatnia = FormatPomiaru.liczba(wartosci.last(), FormatKafla.cyfryPoPrzecinku(pid), "")
        if (wartosci.size == 1) return "poprzednio $ostatnia"
        val n = wartosci.size
        val min = FormatPomiaru.liczba(wartosci.min(), FormatKafla.cyfryPoPrzecinku(pid), "")
        val max = FormatPomiaru.liczba(wartosci.max(), FormatKafla.cyfryPoPrzecinku(pid), "")
        return "poprzednio $ostatnia  ·  $n pomiarów $min–$max"
    }
}

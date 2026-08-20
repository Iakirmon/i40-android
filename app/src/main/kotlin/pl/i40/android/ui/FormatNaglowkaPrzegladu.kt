package pl.i40.android.ui

import pl.i40.android.checkup.PorownaniePrzegladow
import pl.i40.android.checkup.Raport
import pl.i40.android.checkup.ZapisanyPrzeglad

/**
 * Nagłówek przeglądu — sekcja 9.1 warstwy odniesienia.
 * Poza stanem liczby nie są porównywane; kody i monitory — tak, i trzeba to powiedzieć.
 */
object FormatNaglowkaPrzegladu {
    const val W_STANIE = "● jałowy rozgrzany"
    const val POZA_STANEM = "○ silnik nierozgrzany"
    const val KODY_POROWNANE =
        "Kody błędów i monitory porównane mimo to   ✓"
    const val LICZBY_NIEDOSTEPNE =
        "Porównanie liczbowe niedostępne — poprzedni przegląd był na rozgrzanym silniku, wartości nieporównywalne"

    fun tekst(teraz: Raport, poprzedni: ZapisanyPrzeglad?): String {
        val linie = mutableListOf<String>()
        if (teraz.jalowyRozgrzany) {
            linie.add("Warunki    $W_STANIE")
            linie.add("           ${szczegolyStanu(teraz)}")
            if (poprzedni != null && poprzedni.stan == PorownaniePrzegladow.STAN_JALOWY_ROZGRZANY) {
                val data = FormatZmianPrzegladu.dataDoNaglowka(poprzedni.kiedyMs)
                linie.add("Porównanie z $data — ten sam stan   ✓")
            }
        } else {
            val plyn = FormatPomiaru.liczba(teraz.odczyt(0x05), 0, "°C")
            linie.add("Warunki    $POZA_STANEM, płyn $plyn")
            if (poprzedni != null) {
                linie.add(LICZBY_NIEDOSTEPNE)
                linie.add(KODY_POROWNANE)
            }
        }
        return linie.joinToString("\n")
    }

    private fun szczegolyStanu(r: Raport): String {
        val plyn = FormatPomiaru.liczba(r.odczyt(0x05), 0, "°C")
        val rpm = FormatPomiaru.liczba(r.odczyt(0x0C), 0, "obr/min")
        val min = FormatPomiaru.liczba(r.odczyt(0x1F)?.div(60.0), 0, "min")
        val predkosc = r.odczyt(0x0D)
        val postoj = if (predkosc == 0.0) "postój" else FormatPomiaru.liczba(predkosc, 0, "km/h")
        return "płyn $plyn · $postoj · $rpm · $min"
    }
}

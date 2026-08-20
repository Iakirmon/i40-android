package pl.i40.android.ui

import java.util.Calendar
import java.util.Locale
import pl.i40.android.storage.Przejazd
import pl.i40.android.storage.SummaryCalculator

data class WierszKartyMiesiaca(val etykieta: String, val wartosc: String, val poprzedni: String, val roznica: String)

data class KartaMiesiaca(val wiersze: List<WierszKartyMiesiaca>)

/**
 * Karta miesiąca nad kalendarzem — sekcja 10 warstwy kontekstowej.
 * Nie orzeka: kolumna różnicy to liczba ze znakiem, bez słów oceniających.
 */
object FormatKartyMiesiaca {
    fun zPrzejazdow(przejazdy: List<Przejazd>, miesiacMs: Long, cal: Calendar): KartaMiesiaca {
        val tu = sesje(przejazdy, miesiacMs, cal)
        val poprzedniMs = SiatkaMiesiaca.przesunMiesiac(miesiacMs, -1, cal)
        val tam = sesje(przejazdy, poprzedniMs, cal)
        val nazwaPop = nazwaMiesiaca(poprzedniMs, cal)
        return KartaMiesiaca(
            listOf(
                wierszLiczby("Przejazdy", tu.size, tam.size, nazwaPop, pusteJesliZero = true),
                wierszSumyKm("Dystans", tu, tam, nazwaPop),
                wierszCzasu("Czas za kierownicą", tu, tam, nazwaPop),
                wierszBezRozgrzania(tu, tam, nazwaPop),
                wierszMedianuCzasu("Mediana do 90 °C", tu.map { it.podsumowanie.czasDo90CSekundy }, tam, nazwaPop),
                wierszMedianuKorekty(tu, tam, nazwaPop),
                wierszMax(
                    "Najwyższa temp. płynu",
                    tu.map { it.podsumowanie.maxPlynC },
                    tam.map { it.podsumowanie.maxPlynC },
                    "°C",
                    nazwaPop
                ),
                wierszMin(
                    "Najniższe napięcie",
                    tu.map { it.podsumowanie.minNapiecie },
                    tam.map { it.podsumowanie.minNapiecie },
                    "V",
                    nazwaPop
                )
            )
        )
    }

    private fun sesje(przejazdy: List<Przejazd>, miesiacMs: Long, cal: Calendar): List<Przejazd> {
        val klucz = SiatkaMiesiaca.poczatekMiesiaca(miesiacMs, cal)
        return przejazdy.filter { SiatkaMiesiaca.poczatekMiesiaca(it.poczatekMs, cal) == klucz }
    }

    private fun nazwaMiesiaca(ms: Long, cal: Calendar): String {
        val c = cal.clone() as Calendar
        c.timeInMillis = ms
        val locale = Locale("pl", "PL")
        return c.getDisplayName(Calendar.MONTH, Calendar.LONG, locale).orEmpty().lowercase(locale)
    }

    private fun wierszLiczby(
        etykieta: String,
        tu: Int,
        tam: Int,
        nazwaPop: String,
        pusteJesliZero: Boolean
    ): WierszKartyMiesiaca {
        val wartosc = if (pusteJesliZero && tu == 0) FormatPomiaru.NIEDOSTEPNE else tu.toString()
        val poprzedni = if (tam == 0) FormatPomiaru.NIEDOSTEPNE else "$nazwaPop $tam"
        val roznica = if (tu == 0 || tam == 0) FormatPomiaru.NIEDOSTEPNE else znak(tu - tam)
        return WierszKartyMiesiaca(etykieta, wartosc, poprzedni, roznica)
    }

    private fun wierszSumyKm(
        etykieta: String,
        tu: List<Przejazd>,
        tam: List<Przejazd>,
        nazwaPop: String
    ): WierszKartyMiesiaca {
        val a = tu.mapNotNull { it.podsumowanie.dystansKm }.takeIf { it.isNotEmpty() }?.sum()
        val b = tam.mapNotNull { it.podsumowanie.dystansKm }.takeIf { it.isNotEmpty() }?.sum()
        return WierszKartyMiesiaca(
            etykieta,
            a?.let { FormatPomiaru.liczba(it, 0, "km") } ?: FormatPomiaru.NIEDOSTEPNE,
            b?.let { "$nazwaPop ${FormatPomiaru.liczba(it, 0, "km")}" } ?: FormatPomiaru.NIEDOSTEPNE,
            if (a != null && b != null) "${znak((a - b).toInt())} km" else FormatPomiaru.NIEDOSTEPNE
        )
    }

    private fun wierszCzasu(
        etykieta: String,
        tu: List<Przejazd>,
        tam: List<Przejazd>,
        nazwaPop: String
    ): WierszKartyMiesiaca {
        if (tu.isEmpty()) return puste(etykieta)
        val a = tu.sumOf { it.podsumowanie.czasTrwaniaS }
        val b = tam.takeIf { it.isNotEmpty() }?.sumOf { it.podsumowanie.czasTrwaniaS }
        return WierszKartyMiesiaca(
            etykieta,
            formatGodziny(a),
            b?.let { "$nazwaPop ${formatGodziny(it)}" } ?: FormatPomiaru.NIEDOSTEPNE,
            if (b != null) roznicaCzasu(a - b) else FormatPomiaru.NIEDOSTEPNE
        )
    }

    private fun wierszBezRozgrzania(tu: List<Przejazd>, tam: List<Przejazd>, nazwaPop: String): WierszKartyMiesiaca {
        if (tu.isEmpty()) return puste("Bez rozgrzania")
        val a = tu.count { it.podsumowanie.czasDo90CSekundy == null }
        val b = tam.takeIf { it.isNotEmpty() }?.count { it.podsumowanie.czasDo90CSekundy == null }
        return WierszKartyMiesiaca(
            "Bez rozgrzania",
            "$a z ${tu.size}",
            b?.let { "$nazwaPop $it z ${tam.size}" } ?: FormatPomiaru.NIEDOSTEPNE,
            FormatPomiaru.NIEDOSTEPNE
        )
    }

    private fun wierszMedianuCzasu(
        etykieta: String,
        tu: List<Double?>,
        tam: List<Przejazd>,
        nazwaPop: String
    ): WierszKartyMiesiaca {
        val a = mediana(tu.mapNotNull { it })
        val b = mediana(tam.mapNotNull { it.podsumowanie.czasDo90CSekundy })
        return WierszKartyMiesiaca(
            etykieta,
            a?.let { FormatTermika.czasMmSs(it) } ?: FormatPomiaru.NIEDOSTEPNE,
            b?.let { "$nazwaPop ${FormatTermika.czasMmSs(it)}" } ?: FormatPomiaru.NIEDOSTEPNE,
            if (a != null && b != null) "${znak(kotlin.math.round(a - b).toInt())} s" else FormatPomiaru.NIEDOSTEPNE
        )
    }

    private fun wierszMedianuKorekty(tu: List<Przejazd>, tam: List<Przejazd>, nazwaPop: String): WierszKartyMiesiaca {
        val a = mediana(tu.mapNotNull { it.podsumowanie.medianaKorektyDlugoterminowej })
        val b = mediana(tam.mapNotNull { it.podsumowanie.medianaKorektyDlugoterminowej })
        return WierszKartyMiesiaca(
            "Mediana korekty długiej",
            a?.let { FormatPomiaru.liczba(it, 1, "%") } ?: FormatPomiaru.NIEDOSTEPNE,
            b?.let { "$nazwaPop ${FormatPomiaru.liczba(it, 1, "%")}" } ?: FormatPomiaru.NIEDOSTEPNE,
            if (a != null && b != null) "${znakPp(a - b)} pp" else FormatPomiaru.NIEDOSTEPNE
        )
    }

    private fun wierszMax(
        etykieta: String,
        tu: List<Double?>,
        tam: List<Double?>,
        jednostka: String,
        nazwaPop: String
    ): WierszKartyMiesiaca {
        val a = tu.filterNotNull().maxOrNull()
        val b = tam.filterNotNull().maxOrNull()
        return WierszKartyMiesiaca(
            etykieta,
            a?.let { FormatPomiaru.liczba(it, 0, jednostka) } ?: FormatPomiaru.NIEDOSTEPNE,
            b?.let { "$nazwaPop ${FormatPomiaru.liczba(it, 0, jednostka)}" } ?: FormatPomiaru.NIEDOSTEPNE,
            FormatPomiaru.NIEDOSTEPNE
        )
    }

    private fun wierszMin(
        etykieta: String,
        tu: List<Double?>,
        tam: List<Double?>,
        jednostka: String,
        nazwaPop: String
    ): WierszKartyMiesiaca {
        val a = tu.filterNotNull().minOrNull()
        val b = tam.filterNotNull().minOrNull()
        val cyfry = if (jednostka == "V") 1 else 0
        return WierszKartyMiesiaca(
            etykieta,
            a?.let { FormatPomiaru.liczba(it, cyfry, jednostka) } ?: FormatPomiaru.NIEDOSTEPNE,
            b?.let { "$nazwaPop ${FormatPomiaru.liczba(it, cyfry, jednostka)}" } ?: FormatPomiaru.NIEDOSTEPNE,
            FormatPomiaru.NIEDOSTEPNE
        )
    }

    private fun puste(etykieta: String) = WierszKartyMiesiaca(
        etykieta,
        FormatPomiaru.NIEDOSTEPNE,
        FormatPomiaru.NIEDOSTEPNE,
        FormatPomiaru.NIEDOSTEPNE
    )

    private fun mediana(values: List<Double>): Double? {
        if (values.isEmpty()) return null
        return SummaryCalculator.mediana(values.map { it.toFloat() })
    }

    private fun znak(n: Int): String = if (n > 0) "+$n" else n.toString()

    private fun znakPp(n: Double): String {
        val t = FormatPomiaru.liczba(kotlin.math.abs(n), 1, "")
        return if (n >= 0) "+$t" else "-$t"
    }

    private fun formatGodziny(seconds: Double): String {
        val totalMin = kotlin.math.round(seconds / 60.0).toInt()
        val h = totalMin / 60
        val m = totalMin % 60
        return "$h h ${"%02d".format(m)}"
    }

    private fun roznicaCzasu(deltaS: Double): String {
        val totalMin = kotlin.math.round(deltaS / 60.0).toInt()
        val znak = if (totalMin >= 0) "+" else "-"
        val abs = kotlin.math.abs(totalMin)
        val h = abs / 60
        val m = abs % 60
        return if (h == 0) "${znak}$m min" else "$znak$h h ${"%02d".format(m)}"
    }
}

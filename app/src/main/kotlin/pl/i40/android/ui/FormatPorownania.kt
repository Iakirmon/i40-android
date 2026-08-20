package pl.i40.android.ui

import java.util.Calendar
import java.util.Locale
import pl.i40.android.storage.Przejazd
import pl.i40.android.storage.StatusPrzejazdu

data class WierszPorownania(val etykieta: String, val ten: String, val tam: String, val roznica: String?)

data class BlokPorownania(val tytul: String, val wiersze: List<WierszPorownania>)

data class WidokPorownania(
    val dataTen: String,
    val dataTam: String,
    val przerwanyTen: Boolean,
    val przerwanyTam: Boolean,
    val bloki: List<BlokPorownania>
) {
    fun wiersz(etykieta: String): WierszPorownania = bloki.flatMap { it.wiersze }.first { it.etykieta == etykieta }
}

/**
 * Porównanie dwóch przejazdów — §6 warstwy historii.
 * Kolumna różnicy to liczba ze znakiem, bez oceny.
 */
object FormatPorownania {
    fun poprzedni(ten: Przejazd, wszystkie: List<Przejazd>): Przejazd? {
        val vin = ten.vin ?: return null
        return wszystkie.filter { it.vin == vin && it.id != ten.id && it.poczatekMs < ten.poczatekMs }
            .maxByOrNull { it.poczatekMs }
    }

    fun widok(ten: Przejazd, tam: Przejazd): WidokPorownania? {
        if (ten.vin == null || tam.vin == null || ten.vin != tam.vin) return null
        val cal = Calendar.getInstance(Locale("pl", "PL"))
        val a = ten.podsumowanie
        val b = tam.podsumowanie
        val predkoscA = sredniaKmH(a.dystansKm, a.czasTrwaniaS)
        val predkoscB = sredniaKmH(b.dystansKm, b.czasTrwaniaS)
        return WidokPorownania(
            dataTen = dataKrotka(ten.poczatekMs, cal),
            dataTam = dataKrotka(tam.poczatekMs, cal),
            przerwanyTen = ten.status == StatusPrzejazdu.Odzyskany,
            przerwanyTam = tam.status == StatusPrzejazdu.Odzyskany,
            bloki = listOf(
                BlokPorownania(
                    "PRZEJAZD",
                    listOf(
                        liczba("Dystans", a.dystansKm, b.dystansKm, 1, "km"),
                        minuty("Czas", a.czasTrwaniaS, b.czasTrwaniaS),
                        liczba("Średnia prędkość", predkoscA, predkoscB, 0, "km/h")
                    )
                ),
                BlokPorownania(
                    "SILNIK",
                    listOf(
                        liczba("Maks. obroty", a.maxObroty, b.maxObroty, 0, ""),
                        liczba("Średnie obroty", a.srednieObroty, b.srednieObroty, 0, ""),
                        liczba("Maks. prędkość", a.maxPredkoscKmh, b.maxPredkoscKmh, 0, "km/h")
                    )
                ),
                BlokPorownania(
                    "TERMIKA",
                    listOf(
                        liczba("Maks. płyn", a.maxPlynC, b.maxPlynC, 0, "°C"),
                        czasDo90("Do 90 °C", a.czasDo90CSekundy, b.czasDo90CSekundy),
                        liczba("Maks. katalizator", a.maxTempKatalizatoraC, b.maxTempKatalizatoraC, 0, "°C")
                    )
                ),
                BlokPorownania(
                    "MIESZANKA",
                    listOf(
                        liczba(
                            "Mediana korekty",
                            a.medianaKorektyDlugoterminowej,
                            b.medianaKorektyDlugoterminowej,
                            1,
                            "%"
                        ),
                        sekundy(
                            "Poza pasmem",
                            a.czasPozaPasmemWPetliZamknietejSekundy,
                            b.czasPozaPasmemWPetliZamknietejSekundy
                        )
                    )
                ),
                BlokPorownania(
                    "WTRYSK",
                    listOf(liczba("Maks. ciśnienie", a.maxCisnienieSzynyBar, b.maxCisnienieSzynyBar, 0, "bar"))
                ),
                BlokPorownania(
                    "ZASILANIE",
                    listOf(liczba("Napięcie min", a.minNapiecie, b.minNapiecie, 1, "V"))
                ),
                BlokPorownania(
                    "KODY",
                    listOf(
                        kody("Na starcie", a.kodyNaStarcie, b.kodyNaStarcie),
                        kody("Na końcu", a.kodyNaKoncu, b.kodyNaKoncu)
                    )
                )
            )
        )
    }

    private fun sredniaKmH(dystansKm: Double?, czasS: Double): Double? {
        if (dystansKm == null || czasS <= 0.0) return null
        return dystansKm / (czasS / 3600.0)
    }

    private fun liczba(etykieta: String, a: Double?, b: Double?, cyfry: Int, jednostka: String): WierszPorownania {
        val ten = a?.let { FormatPomiaru.liczba(it, cyfry, jednostka) } ?: FormatPomiaru.NIEDOSTEPNE
        val tam = b?.let { FormatPomiaru.liczba(it, cyfry, jednostka) } ?: FormatPomiaru.NIEDOSTEPNE
        val roznica = if (a == null || b == null || a == b) {
            FormatPomiaru.NIEDOSTEPNE
        } else {
            val d = a - b
            val t = FormatPomiaru.liczba(kotlin.math.abs(d), cyfry, jednostka)
            if (d > 0) "+$t" else "−$t"
        }
        return WierszPorownania(etykieta, ten, tam, roznica)
    }

    private fun minuty(etykieta: String, aS: Double, bS: Double): WierszPorownania {
        val a = kotlin.math.round(aS / 60.0).toInt()
        val b = kotlin.math.round(bS / 60.0).toInt()
        val roznica = if (a == b) FormatPomiaru.NIEDOSTEPNE else znakInt(a - b, "min")
        return WierszPorownania(etykieta, "$a min", "$b min", roznica)
    }

    private fun czasDo90(etykieta: String, a: Double?, b: Double?): WierszPorownania {
        val ten = a?.let { mmss(it) } ?: FormatPomiaru.NIEDOSTEPNE
        val tam = b?.let { mmss(it) } ?: FormatPomiaru.NIEDOSTEPNE
        val roznica = if (a == null || b == null || a == b) {
            FormatPomiaru.NIEDOSTEPNE
        } else {
            znakInt(kotlin.math.round(a - b).toInt(), "s")
        }
        return WierszPorownania(etykieta, ten, tam, roznica)
    }

    private fun sekundy(etykieta: String, a: Double?, b: Double?): WierszPorownania {
        val ten = a?.let { formatS(it) } ?: FormatPomiaru.NIEDOSTEPNE
        val tam = b?.let { formatS(it) } ?: FormatPomiaru.NIEDOSTEPNE
        val roznica = if (a == null || b == null || a == b) {
            FormatPomiaru.NIEDOSTEPNE
        } else {
            znakInt(kotlin.math.round(a - b).toInt(), "s")
        }
        return WierszPorownania(etykieta, ten, tam, roznica)
    }

    private fun kody(etykieta: String, a: List<String>, b: List<String>): WierszPorownania =
        WierszPorownania(etykieta, kodyTekst(a), kodyTekst(b), null)

    private fun kodyTekst(k: List<String>): String = if (k.isEmpty()) "brak" else k.joinToString(" ")

    private fun mmss(s: Double): String {
        val total = kotlin.math.round(s).toInt()
        return "${total / 60}:${"%02d".format(total % 60)}"
    }

    private fun formatS(s: Double): String {
        val total = kotlin.math.round(s).toInt()
        val m = total / 60
        val sec = total % 60
        return if (m == 0) "$sec s" else "$m min $sec s"
    }

    private fun znakInt(n: Int, jednostka: String): String {
        val t = "${kotlin.math.abs(n)} $jednostka"
        return if (n > 0) "+$t" else "−$t"
    }

    private fun dataKrotka(ms: Long, cal: Calendar): String {
        val c = cal.clone() as Calendar
        c.timeInMillis = ms
        val dzien = c.get(Calendar.DAY_OF_MONTH)
        val skrot = c.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale("pl", "PL")).orEmpty()
        val godz = String.format(Locale("pl", "PL"), "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
        return "$dzien $skrot $godz"
    }
}

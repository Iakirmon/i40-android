package pl.i40.android.ui

import java.util.Calendar
import java.util.Locale
import pl.i40.android.storage.Przejazd
import pl.i40.android.storage.StatusPrzejazdu

data class PotwierdzenieUsuniecia(
    val naglowek: String,
    val dataGodzina: String,
    val coGinie: String,
    val nieodtwarzalne: String,
    val coZostaje: String,
    val kartaMiesiaca: String,
    val chroniony: String?,
    val pozycje: List<String> = emptyList(),
    val dalsze: String? = null,
    val przyciskUsun: String = "Usuń"
)

data class StanPotwierdzenia(val oczekujace: Przejazd? = null) {
    val oczekujePotwierdzenia: Boolean get() = oczekujace != null

    fun poGescie(p: Przejazd): StanPotwierdzenia {
        if (p.status == StatusPrzejazdu.WToku) return this
        return copy(oczekujace = p)
    }

    fun poPrzycisku(p: Przejazd): StanPotwierdzenia = poGescie(p)

    fun poAnulowaniu(): Pair<StanPotwierdzenia, Przejazd?> = copy(oczekujace = null) to null

    fun poPotwierdzeniu(): Pair<StanPotwierdzenia, Przejazd?> {
        val p = oczekujace ?: return this to null
        return copy(oczekujace = null) to p
    }
}

/**
 * Tekst okna potwierdzenia kasowania — układ z §4.2 warstwy historii.
 * Nie pyta „czy na pewno?”; wymienia, co ginie.
 */
object FormatPotwierdzenia {
    const val LIMIT_LISTY = 10

    private val MIESIACE_DOPELNIACZ = listOf(
        "stycznia",
        "lutego",
        "marca",
        "kwietnia",
        "maja",
        "czerwca",
        "lipca",
        "sierpnia",
        "września",
        "października",
        "listopada",
        "grudnia"
    )

    fun pojedynczy(przejazd: Przejazd, liczbaPunktow: Int, bajty: Long, cal: Calendar): PotwierdzenieUsuniecia {
        val c = cal.clone() as Calendar
        c.timeInMillis = przejazd.poczatekMs
        val dzien = c.get(Calendar.DAY_OF_MONTH)
        val miesiac = MIESIACE_DOPELNIACZ[c.get(Calendar.MONTH)]
        val rok = c.get(Calendar.YEAR)
        val godz = String.format(Locale("pl", "PL"), "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
        val minuty = kotlin.math.round(przejazd.podsumowanie.czasTrwaniaS / 60.0).toInt()
        val km = przejazd.podsumowanie.dystansKm?.let { FormatPomiaru.liczba(it, 1, "km") } ?: FormatPomiaru.NIEDOSTEPNE
        val probek = grupuj(przejazd.podsumowanie.liczbaProbek)
        val mb = FormatPomiaru.liczba(bajty / (1024.0 * 1024.0), 1, "MB")
        return PotwierdzenieUsuniecia(
            naglowek = "Usunąć ten przejazd?",
            dataGodzina = "$dzien $miesiac $rok, $godz",
            coGinie = "$minuty min · $km · $probek próbek · $mb",
            nieodtwarzalne = "Tego nagrania nie da się odtworzyć.",
            coZostaje = zostaje(liczbaPunktow),
            kartaMiesiaca = "Karta miesiąca dla $miesiac przeliczy się.",
            chroniony = if (przejazd.chroniony) "Ten przejazd jest chroniony." else null
        )
    }

    fun wielokrotne(przejazdy: List<Przejazd>, liczbaPunktow: Int, bajty: Long, cal: Calendar): PotwierdzenieUsuniecia {
        val n = przejazdy.size
        val minuty = kotlin.math.round(przejazdy.sumOf { it.podsumowanie.czasTrwaniaS } / 60.0).toInt()
        val kmSuma = przejazdy.mapNotNull { it.podsumowanie.dystansKm }.takeIf { it.isNotEmpty() }?.sum()
        val km = kmSuma?.let { FormatPomiaru.liczba(it, 1, "km") } ?: FormatPomiaru.NIEDOSTEPNE
        val mb = FormatPomiaru.liczba(bajty / (1024.0 * 1024.0), 1, "MB")
        val pozycjePelne = przejazdy.map { wierszListy(it, cal) }
        val dalsze = if (pozycjePelne.size > LIMIT_LISTY) {
            "… i ${pozycjePelne.size - LIMIT_LISTY} dalszych"
        } else {
            null
        }
        val miesiace = przejazdy.map { miesiacDopelniacz(it.poczatekMs, cal) }.distinct()
        val karta = when (miesiace.size) {
            0 -> "Karta miesiąca przeliczy się."
            1 -> "Karta miesiąca dla ${miesiace[0]} przeliczy się."
            else -> "Karta miesiąca dla ${miesiace.joinToString(" i ")} przeliczy się."
        }
        return PotwierdzenieUsuniecia(
            naglowek = "Usunąć ${odmianaPrzejazdow(n)}?",
            dataGodzina = "",
            coGinie = "Łącznie $minuty min · $km · $mb",
            nieodtwarzalne = "Tych nagrań nie da się odtworzyć.",
            coZostaje = zostajeWiele(liczbaPunktow),
            kartaMiesiaca = karta,
            chroniony = null,
            pozycje = pozycjePelne.take(LIMIT_LISTY),
            dalsze = dalsze,
            przyciskUsun = "Usuń $n"
        )
    }

    fun rozmiarBajtow(przejazd: Przejazd): Long = przejazd.przebieg.encode().size.toLong()

    fun rozmiarBajtow(przejazdy: List<Przejazd>): Long = przejazdy.sumOf { rozmiarBajtow(it) }

    private fun wierszListy(p: Przejazd, cal: Calendar): String {
        val c = cal.clone() as Calendar
        c.timeInMillis = p.poczatekMs
        val dzien = c.get(Calendar.DAY_OF_MONTH)
        val miesiac = MIESIACE_DOPELNIACZ[c.get(Calendar.MONTH)]
        val godz = String.format(Locale("pl", "PL"), "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
        val minuty = kotlin.math.round(p.podsumowanie.czasTrwaniaS / 60.0).toInt()
        val km = p.podsumowanie.dystansKm?.let { FormatPomiaru.liczba(it, 1, "km") } ?: FormatPomiaru.NIEDOSTEPNE
        val baza = "$dzien $miesiac $godz   $minuty min   $km"
        return if (p.status == StatusPrzejazdu.Odzyskany) "$baza    przerwany" else baza
    }

    private fun miesiacDopelniacz(ms: Long, cal: Calendar): String {
        val c = cal.clone() as Calendar
        c.timeInMillis = ms
        return MIESIACE_DOPELNIACZ[c.get(Calendar.MONTH)]
    }

    private fun odmianaPrzejazdow(n: Int): String {
        val slowo = when {
            n == 1 -> "przejazd"
            n % 10 in 2..4 && n % 100 !in 12..14 -> "przejazdy"
            else -> "przejazdów"
        }
        return "$n $slowo"
    }

    private fun zostaje(n: Int): String {
        val ile = when (n) {
            1 -> "1 punkt odniesienia zebrany"
            in 2..4 -> "$n punkty odniesienia zebrane"
            else -> "$n punktów odniesienia zebranych"
        }
        return "Zostaje $ile podczas tego przejazdu — kolumna „poprzednio” się nie zmieni."
    }

    private fun zostajeWiele(n: Int): String {
        val ile = when {
            n == 1 -> "1 punkt odniesienia"
            n in 2..4 -> "$n punkty odniesienia"
            else -> "$n punktów odniesienia"
        }
        val czasownik = if (n in 2..4) "Zostają" else "Zostaje"
        return "$czasownik $ile."
    }

    private fun grupuj(n: Int): String {
        val s = n.toString()
        val sb = StringBuilder()
        s.forEachIndexed { i, ch ->
            if (i > 0 && (s.length - i) % 3 == 0) sb.append(' ')
            sb.append(ch)
        }
        return sb.toString()
    }
}

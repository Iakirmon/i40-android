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
    val chroniony: String?
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

    fun rozmiarBajtow(przejazd: Przejazd): Long = przejazd.przebieg.encode().size.toLong()

    private fun zostaje(n: Int): String {
        val ile = when (n) {
            1 -> "1 punkt odniesienia zebrany"
            in 2..4 -> "$n punkty odniesienia zebrane"
            else -> "$n punktów odniesienia zebranych"
        }
        return "Zostaje $ile podczas tego przejazdu — kolumna „poprzednio” się nie zmieni."
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

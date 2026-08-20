package pl.i40.android.ui

import pl.i40.android.rules.PasmaOdniesienia
import pl.i40.android.rules.RodzajPasma
import pl.i40.android.rules.WagaWniosku
import pl.i40.android.rules.Wniosek

data class WierszOdchylenia(
    val pid: Int,
    val zdanie: String,
    val znacznik: String,
    val wartoscPasmo: String,
    val skrot: String,
    val waga: WagaWniosku
)

data class WidokPaneluStan(
    val stan: StanPanelu,
    val tytul: String,
    val kontekst: String,
    val kody: List<String>,
    val odchylenia: List<WierszOdchylenia>,
    val dalsze: String?,
    val niezmierzone: String?
)

/**
 * Tekst panelu Stan — §4 warstwy objaśnień. Zero dźwięków; wnioski z istniejących reguł.
 */
object FormatPaneluStan {
    const val LIMIT_ODCHYLEN = 4

    val PIDY_OCENIANE: List<Int> = PasmaOdniesienia.wpisy.mapNotNull { it.pid }.distinct()

    fun widok(
        odczyty: Map<Int, Double?>,
        odczytane: Set<Int>,
        status0103: Int?,
        kody: List<String>,
        silnikRozgrzany: Boolean,
        olejGotowy: Boolean,
        wnioski: List<Wniosek>
    ): WidokPaneluStan {
        val stany = PIDY_OCENIANE.associateWith { pid ->
            StanParametru.ocen(
                pid,
                odczyty[pid],
                odczytane.contains(pid),
                WarunkiWaznosci.spelnione(pid, status0103)
            )
        }
        val panel = StanPanelu.zloz(stany.values.toList())
        val odchyleniaPelne = PIDY_OCENIANE.mapNotNull { pid ->
            val s = stany.getValue(pid)
            if (s != StanParametru.Ponizej && s != StanParametru.Powyzej) return@mapNotNull null
            wierszOdchylenia(pid, odczyty[pid], s, wnioski)
        }.sortedWith(
            compareBy<WierszOdchylenia> {
                when (it.waga) {
                    WagaWniosku.Usterka -> 0
                    WagaWniosku.Uwaga -> 1
                    WagaWniosku.Informacja -> 2
                }
            }.thenBy { PIDY_OCENIANE.indexOf(it.pid) }
        )
        val dalszeN = (odchyleniaPelne.size - LIMIT_ODCHYLEN).coerceAtLeast(0)
        val niezm = PIDY_OCENIANE.filter { stany[it] == StanParametru.NieZmierzony }.map { nazwaKrotka(it) }.distinct()
        val kontekst = when {
            silnikRozgrzany && olejGotowy -> "Silnik rozgrzany · olej gotowy"
            silnikRozgrzany -> "Silnik rozgrzany"
            else -> "Silnik się rozgrzewa"
        }
        val tytul = when (panel) {
            StanPanelu.WNormie -> "Wszystko w normie"
            StanPanelu.JeszczeNieWiem -> if (odchyleniaPelne.isEmpty()) kontekst else ""
            StanPanelu.Odchylenia -> ""
        }
        return WidokPaneluStan(
            stan = panel,
            tytul = tytul,
            kontekst = if (panel == StanPanelu.WNormie) kontekst else "",
            kody = kody,
            odchylenia = odchyleniaPelne.take(LIMIT_ODCHYLEN),
            dalsze = if (dalszeN > 0) "… i $dalszeN dalszych → Przegląd" else null,
            niezmierzone = if (niezm.isEmpty()) null else niezm.joinToString(" · ")
        )
    }

    private fun wierszOdchylenia(
        pid: Int,
        wartosc: Double?,
        s: StanParametru,
        wnioski: List<Wniosek>
    ): WierszOdchylenia {
        val pasujace = wnioski.filter { pidDlaReguly(it.ruleId) == pid }
        val wniosek = pasujace.minByOrNull { it.waga.ordinal }
        val dopisek = if (s == StanParametru.Ponizej) " poniżej normy" else " powyżej normy"
        val zdanie = wniosek?.tytul ?: (nazwaKrotka(pid) + dopisek)
        val znacznik = if (s == StanParametru.Powyzej) "▲" else "▼"
        val waga = wniosek?.waga ?: WagaWniosku.Uwaga
        val pasmo = PasmaOdniesienia.wpisy.firstOrNull {
            it.pid == pid && it.rodzaj == RodzajPasma.Norma
        }
        val wartoscTekst = FormatKafla.wartosc(pid, wartosc)
        val pasmoTekst = when {
            pasmo?.min != null && pasmo.max != null -> "${pasmo.min.toInt()}–${pasmo.max.toInt()}"
            pasmo?.min != null -> "≥ ${pasmo.min.toInt()}"
            else -> FormatPomiaru.NIEDOSTEPNE
        }
        val cel = CelSkrotu.dla(pid)
        val skrot = when (cel) {
            CelSkrotu.PrzegladOdczyty -> "→ Przegląd"
            CelSkrotu.WtryskGdi -> "→ Wtrysk GDI"
            else -> "→ ${cel.name}"
        }
        return WierszOdchylenia(pid, zdanie, znacznik, "$wartoscTekst    norma $pasmoTekst", skrot, waga)
    }

    private fun pidDlaReguly(ruleId: String): Int? = when (ruleId) {
        "ltft_lean", "ltft_rich" -> 0x07
        "overheat", "thermostat" -> 0x05
        "oil_cold" -> 0x5C
        "alternator_low", "overcharge", "battery_weak" -> 0x42
        "GDI-1" -> 0x23
        "KAT-1", "KAT-2" -> 0x3C
        else -> null
    }

    private fun nazwaKrotka(pid: Int): String = when (pid) {
        0x3C -> "katalizator"
        0x23 -> "ciśnienie paliwa"
        0x07, 0x06 -> "korekty"
        0x05 -> "płyn"
        0x5C -> "olej"
        0x42 -> "napięcie"
        0x44 -> "lambda"
        else -> FormatKafla.krotkaEtykieta(pid).lowercase()
    }
}

package pl.i40.android.ui

import pl.i40.android.acquisition.RingSample
import pl.i40.android.rules.PasmaOdniesienia
import pl.i40.android.storage.SummaryCalculator

/** Panel Termika. Linie z [PasmaOdniesienia] — te same stałe co `KAT-1` / `KAT-2` / `oil_cold`. */
object FormatTermika {
    fun linieKatalizatora(): List<Double> = listOf(
        PasmaOdniesienia.KATALIZATOR_ZAPLON_C,
        PasmaOdniesienia.katalizatorPraca.start,
        PasmaOdniesienia.katalizatorPraca.endInclusive
    )

    fun liniePlynu(): List<Double> = listOf(PasmaOdniesienia.plyn.start, PasmaOdniesienia.plyn.endInclusive)

    fun linieOleju(): List<Double> = listOf(PasmaOdniesienia.OLEJ_MIN_C)

    fun normaDolotu(): String = FormatPomiaru.NIEDOSTEPNE

    fun normaOtoczenia(): String = FormatPomiaru.NIEDOSTEPNE

    fun czasDo(samples: List<RingSample>, prog: Double): Double? {
        for (s in samples) {
            if (s.value >= prog) return s.time
        }
        return null
    }

    fun kolejnoscRozgrzewania(kat: List<RingSample>, plyn: List<RingSample>, olej: List<RingSample>): List<String> {
        val zdarzenia = mutableListOf<Pair<Double, String>>()
        czasDo(kat, PasmaOdniesienia.KATALIZATOR_ZAPLON_C)?.let { zdarzenia.add(it to "katalizator") }
        czasDo(plyn, SummaryCalculator.PROG_PLYN_90_C)?.let { zdarzenia.add(it to "płyn") }
        czasDo(olej, PasmaOdniesienia.OLEJ_MIN_C)?.let { zdarzenia.add(it to "olej") }
        return zdarzenia.sortedBy { it.first }.map { it.second }
    }

    fun czasMmSs(seconds: Double?): String {
        if (seconds == null) return FormatPomiaru.NIEDOSTEPNE
        val total = kotlin.math.max(0, kotlin.math.round(seconds).toInt())
        return "%d:%02d".format(total / 60, total % 60)
    }

    fun wierszCzasow(plynS: Double?, olejS: Double?): String {
        val p = czasMmSs(plynS)
        val o = czasMmSs(olejS)
        return "Płyn 90 °C po $p · olej po $o"
    }
}

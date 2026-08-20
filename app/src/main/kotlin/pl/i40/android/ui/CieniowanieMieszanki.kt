package pl.i40.android.ui

import pl.i40.android.acquisition.RingSample
import pl.i40.android.obd.FuelSystemStatus

enum class RodzajCienia {
    Przedmuchiwanie,
    PetlaOtwarta,
}

data class PasmoCienia(val start: Double, val end: Double, val rodzaj: RodzajCienia)

/**
 * Cieniowanie wykresu sumy korekt — sekcja 8.3 warstwy kontekstowej.
 * `16` to pętla zamknięta: nie trafia do cienia „otwarta".
 */
object CieniowanieMieszanki {
    fun pasma(purge: List<RingSample>, status: List<RingSample>, t0: Double, t1: Double): List<PasmoCienia> {
        if (t1 <= t0) return emptyList()
        val times = (purge.map { it.time } + status.map { it.time } + listOf(t0, t1))
            .filter { it in t0..t1 }
            .toSortedSet()
            .toList()
        if (times.size < 2) return emptyList()
        val out = mutableListOf<PasmoCienia>()
        out += regiony(times) { t -> przedmuchAktywny(ostatniaDo(purge, t)) }
            .map { PasmoCienia(it.first, it.second, RodzajCienia.Przedmuchiwanie) }
        out += regiony(times) { t -> petlaOtwartaDoCienia(ostatniaDo(status, t)) }
            .map { PasmoCienia(it.first, it.second, RodzajCienia.PetlaOtwarta) }
        return out
    }

    fun przedmuchAktywny(v: Double?): Boolean = v != null && v > 0

    /** `0103` ∉ {2, 16}, w tym brak odczytu. `16` zostaje zamknięta. */
    fun petlaOtwartaDoCienia(status: Double?): Boolean = !FuelSystemStatus.korektyWazne(status?.toInt())

    private fun regiony(times: List<Double>, aktywne: (Double) -> Boolean): List<Pair<Double, Double>> {
        val out = mutableListOf<Pair<Double, Double>>()
        var start: Double? = null
        for (i in 0 until times.lastIndex) {
            val a = times[i]
            val b = times[i + 1]
            if (b <= a) continue
            if (aktywne(a)) {
                if (start == null) start = a
            } else if (start != null) {
                out.add(start to a)
                start = null
            }
        }
        if (start != null) out.add(start to times.last())
        return out
    }

    private fun ostatniaDo(samples: List<RingSample>, t: Double): Double? {
        var last: Double? = null
        for (s in samples) {
            if (s.time <= t) last = s.value else break
        }
        return last
    }
}

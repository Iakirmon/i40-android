package pl.i40.android.ui

import pl.i40.android.acquisition.RingSample
import pl.i40.android.rules.PasmaOdniesienia

/**
 * Panel Wtrysk GDI. Oś 0–240 bar z §8.6 (241 bar z pasma obciążeniowego, zaokrąglone w dół).
 * Linie z [PasmaOdniesienia] — ta sama referencja co `GDI-1`.
 */
object FormatGdi {
    const val PID_SZYNA = 0x23
    const val PID_OBCIAZENIE_ABS = 0x43
    const val PID_PRZEPUSTNICA = 0x11

    fun linieSzyny(): List<Double> = listOf(
        PasmaOdniesienia.szynaJalowy.start,
        PasmaOdniesienia.szynaJalowy.endInclusive,
        PasmaOdniesienia.szynaObciazenie.start,
        PasmaOdniesienia.szynaObciazenie.endInclusive
    )

    fun linieObciazenia(): List<Double> = emptyList()

    fun liniePrzepustnicy(): List<Double> = emptyList()

    fun probkiBar(kpa: List<RingSample>): List<RingSample> =
        kpa.map { RingSample(it.time, PasmaOdniesienia.kpaNaBar(it.value)) }

    fun szczytSesji(railKpa: List<RingSample>, load: List<RingSample>): Pair<Double?, Double?> {
        if (railKpa.isEmpty()) return null to null
        var best = 0
        for (i in 1 until railKpa.size) {
            if (railKpa[i].value > railKpa[best].value) best = i
        }
        val peak = railKpa[best]
        val bar = PasmaOdniesienia.kpaNaBar(peak.value)
        return bar to najblizsza(load, peak.time)
    }

    fun maxWiersz(maxBar: Double?, loadPct: Double?): String {
        if (maxBar == null) return "Max w sesji: ${FormatPomiaru.NIEDOSTEPNE}"
        val bar = FormatPomiaru.liczba(maxBar, 0, "bar")
        val obc = loadPct?.let { FormatPomiaru.liczba(it, 0, "%") } ?: FormatPomiaru.NIEDOSTEPNE
        return "Max w sesji: $bar przy $obc obciążenia"
    }

    fun pasmoObciazeniowe(): String {
        val p = PasmaOdniesienia.szynaObciazenie
        return "${p.start.toInt()} – ${p.endInclusive.toInt()} bar"
    }

    fun cisnienieZadane(): String = "Ciśnienie zadane: auto nie oddaje (PID poza OBD-II)"

    private fun najblizsza(samples: List<RingSample>, t: Double): Double? {
        if (samples.isEmpty()) return null
        var best = 0
        var bestD = kotlin.math.abs(samples[0].time - t)
        for (i in 1 until samples.size) {
            val d = kotlin.math.abs(samples[i].time - t)
            if (d < bestD) {
                bestD = d
                best = i
            }
        }
        return samples[best].value
    }
}

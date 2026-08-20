package pl.i40.android.ui

import pl.i40.android.acquisition.RingSample
import pl.i40.android.obd.PidCatalog

/** Sztywne zakresy osi Y z tabeli 10.7 — bez autoscale. */
object OsY {
    const val OKNO_S = 60.0

    fun zakres(pid: Int): ClosedFloatingPointRange<Double> = when (pid) {
        0x0C -> 0.0..7000.0
        0x0D -> 0.0..200.0
        0x04 -> 0.0..100.0
        0x0E -> -10.0..50.0
        0x06, 0x07 -> -25.0..25.0
        0x10 -> 0.0..150.0
        0x05 -> 0.0..130.0
        0x5C -> 0.0..150.0
        0x42 -> 0.0..20.0
        0x0F -> -40.0..80.0
        0x23 -> 0.0..240.0
        0x3C -> 0.0..1000.0
        else -> PidCatalog.definition(pid)?.range ?: 0.0..100.0
    }

    data class Przyciecie(val wartosc: Double, val przyciete: Boolean)

    fun przytnij(value: Double, pid: Int): Przyciecie {
        val r = zakres(pid)
        if (value < r.start) return Przyciecie(r.start, true)
        if (value > r.endInclusive) return Przyciecie(r.endInclusive, true)
        return Przyciecie(value, false)
    }

    fun etykietaZakresu(pid: Int): String {
        val r = zakres(pid)
        val digits = FormatKafla.cyfryPoPrzecinku(pid)
        return "${kraniec(r.start, digits)}…${kraniec(r.endInclusive, digits)}"
    }

    fun domenaCzasu(samples: List<RingSample>, teraz: Double? = null): ClosedFloatingPointRange<Double> {
        val end = teraz ?: samples.lastOrNull()?.time ?: 0.0
        val start = end - OKNO_S
        return start..maxOf(end, start + 0.001)
    }

    private fun kraniec(value: Double, digits: Int): String = if (digits == 0) {
        "%.0f".format(java.util.Locale.US, value)
    } else {
        "%.${digits}f".format(java.util.Locale.US, value).replace('.', ',')
    }
}

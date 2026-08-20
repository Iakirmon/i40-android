package pl.i40.android.ui

import pl.i40.android.acquisition.RingSample
import pl.i40.android.rules.PasmaOdniesienia
import pl.i40.android.rules.RodzajPasma

/**
 * Panel Powietrze — sekcja 9 warstwy kontekstowej.
 * Podciśnienie to jedyna wartość liczona z dwóch pomiarów; bez którejkolwiek jest `null`.
 */
object FormatPowietrza {
    /** Wirtualny PID osi podciśnienia — nie jest zapytaniem OBD. */
    const val PID_PODCISNIENIE = 0xE633
    const val PID_ATMOSFERA = 0x33
    const val PID_KOLEKTOR = 0x0B
    const val PID_ZADANA = 0x4C
    const val PID_RZECZYWISTA = 0x11
    const val PID_PEDAL = 0x49

    fun podcisnienieKpa(atmosfera: Double?, kolektor: Double?): Double? {
        if (atmosfera == null || kolektor == null) return null
        return atmosfera - kolektor
    }

    fun probkiPodcisnienia(atmosfera: List<RingSample>, kolektor: List<RingSample>): List<RingSample> {
        if (atmosfera.isEmpty() || kolektor.isEmpty()) return emptyList()
        val times = (atmosfera.map { it.time } + kolektor.map { it.time }).toSortedSet()
        val out = mutableListOf<RingSample>()
        for (t in times) {
            val a = ostatniaDo(atmosfera, t) ?: continue
            val k = ostatniaDo(kolektor, t) ?: continue
            out.add(RingSample(t, a - k))
        }
        return out
    }

    fun rozjazdPkt(zadana: Double?, rzeczywista: Double?): Double? {
        if (zadana == null || rzeczywista == null) return null
        return kotlin.math.abs(zadana - rzeczywista)
    }

    fun norma(id: String): String {
        val wpis = PasmaOdniesienia.wpisy.first { it.id == id }
        if (wpis.rodzaj == RodzajPasma.Brak || wpis.min == null || wpis.max == null) {
            return FormatPomiaru.NIEDOSTEPNE
        }
        return "${wpis.min.toInt()} – ${wpis.max.toInt()} ${wpis.jednostka}".trim()
    }

    fun podpisWyliczone(): String = "PODCIŚNIENIE  (wyliczone)"

    fun rozjazdTekst(pkt: Double?): String {
        if (pkt == null) return FormatPomiaru.NIEDOSTEPNE
        return FormatPomiaru.liczba(pkt, 0, "pkt")
    }

    private fun ostatniaDo(samples: List<RingSample>, t: Double): Double? {
        var last: Double? = null
        for (s in samples) {
            if (s.time <= t) last = s.value else break
        }
        return last
    }
}

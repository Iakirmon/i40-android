package pl.i40.android.ui

import pl.i40.android.rules.PasmaOdniesienia
import pl.i40.android.rules.RodzajPasma

/**
 * Linie siatki pola kalibrowanego — granice pasm z [PasmaOdniesienia], §6.1 wyglądu.
 * Brak normy → zero linii. Nie okrągłe liczby co dwadzieścia.
 */
object SiatkaPasma {
    fun linie(pid: Int): List<Double> {
        val wpisy = when (pid) {
            FormatRaportu.PID_SUMA_KOREKT ->
                PasmaOdniesienia.wpisy.filter { it.id == "suma_korekt" }
            FormatPowietrza.PID_PODCISNIENIE ->
                PasmaOdniesienia.wpisy.filter { it.id == "podcisnienie" }
            else -> PasmaOdniesienia.wpisyDlaPid(pid)
        }
        if (wpisy.any { it.rodzaj == RodzajPasma.Brak } &&
            wpisy.none { it.rodzaj == RodzajPasma.Norma }
        ) {
            return emptyList()
        }
        val normy = wpisy.filter { it.rodzaj == RodzajPasma.Norma }
        if (normy.isEmpty()) return emptyList()
        val out = linkedSetOf<Double>()
        for (w in normy) {
            w.min?.let { out.add(it) }
            w.max?.let { out.add(it) }
        }
        return out.toList()
    }

    fun maSiatke(pid: Int): Boolean = linie(pid).isNotEmpty()
}

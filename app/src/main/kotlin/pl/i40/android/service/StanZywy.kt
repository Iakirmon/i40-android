package pl.i40.android.service

import pl.i40.android.acquisition.OilTempEstimator
import pl.i40.android.acquisition.RingBufferStore
import pl.i40.android.acquisition.RingSample
import pl.i40.android.acquisition.SampleTick
import pl.i40.android.obd.DecodedPid

/**
 * Stan żywy ekranu — kafle, ring, model oleju. Mieszka w usłudze, nie w ViewModel.
 * PID `015C` z OBD jest ignorowany; olej liczy model.
 */
class StanZywy {
    private val najnowsze = mutableMapOf<Int, Double>()
    val ring = RingBufferStore()
    val olej = OilTempEstimator()
    var nagrywa: Boolean = false
    var elapsedSeconds: Double = 0.0
    var measuredHz: Double = 0.0
    var totalQueries: Int = 0

    fun wartosc(pid: Int): Double? {
        if (pid == PID_OLEJ) return olej.estimateC
        return najnowsze[pid]
    }

    fun samples(pid: Int): List<RingSample> = ring.samples(pid)

    fun zastosuj(tick: SampleTick) {
        var runtime: Double? = null
        for (reading in tick.readings) {
            if (reading.pid == PID_OLEJ) continue
            val n = reading.decoded as? DecodedPid.Numeric ?: continue
            najnowsze[reading.pid] = n.value
            ring.append(reading.pid, tick.time, n.value)
            if (reading.pid == 0x1F) runtime = n.value
        }
        uaktualnijOlej(tick.time, runtime)
        if (nagrywa) elapsedSeconds = tick.time
    }

    fun zastosuj(values: Map<Int, Double>, at: Double = 0.0) {
        var runtime: Double? = null
        for ((pid, value) in values) {
            if (pid == PID_OLEJ) continue
            najnowsze[pid] = value
            ring.append(pid, at, value)
            if (pid == 0x1F) runtime = value
        }
        uaktualnijOlej(at, runtime)
    }

    val olejC: Double? get() = olej.estimateC

    val wRuchu: Boolean get() = BlokadaPredkosci.wRuchu(najnowsze[BlokadaPredkosci.PID_PREDKOSC])

    val blokujeChrome: Boolean get() = wRuchu

    fun pozwala(rodzaj: InterakcjaZywa): Boolean = BlokadaPredkosci.pozwala(rodzaj, wRuchu = wRuchu, nagrywa = nagrywa)

    fun wartosciAlarmu(): Map<Int, Double> {
        val out = najnowsze.toMutableMap()
        val est = olej.estimateC
        if (est != null) out[PID_OLEJ] = est
        return out
    }

    fun migawka(stan: StanPrzejazdu): MigawkaZywego = MigawkaZywego(
        stan = stan,
        nagrywa = nagrywa,
        wRuchu = wRuchu,
        predkoscKmh = najnowsze[BlokadaPredkosci.PID_PREDKOSC],
        olejC = olej.estimateC,
        olejPewnosc = olej.pewnosc,
        wartosci = najnowsze.toMap(),
        serie = FormatKaflaWykresow.PIDY_WYKRESOW.associateWith { ring.samples(it) },
        elapsedSeconds = elapsedSeconds,
        hz = measuredHz,
        queries = totalQueries
    )

    private fun uaktualnijOlej(t: Double, runtimeSeconds: Double?) {
        val coolant = najnowsze[0x05] ?: return
        val load = najnowsze[0x04] ?: return
        olej.update(
            t = t,
            coolantC = coolant,
            loadPct = load,
            ambientC = najnowsze[0x46],
            runtimeSeconds = runtimeSeconds ?: najnowsze[0x1F]
        )
    }

    companion object {
        const val PID_OLEJ = 0x5C
    }
}

/** Trzy wykresy z ASCII sekcji 12.2: obroty, obciążenie, zapłon. */
object FormatKaflaWykresow {
    val PIDY_WYKRESOW: List<Int> = listOf(0x0C, 0x04, 0x0E)
}

data class MigawkaZywego(
    val stan: StanPrzejazdu = StanPrzejazdu.Rozlaczony,
    val nagrywa: Boolean = false,
    val wRuchu: Boolean = false,
    val predkoscKmh: Double? = null,
    val olejC: Double? = null,
    val olejPewnosc: OilTempEstimator.Pewnosc = OilTempEstimator.Pewnosc.Niska,
    val wartosci: Map<Int, Double> = emptyMap(),
    val serie: Map<Int, List<RingSample>> = emptyMap(),
    val elapsedSeconds: Double = 0.0,
    val hz: Double = 0.0,
    val queries: Int = 0
)

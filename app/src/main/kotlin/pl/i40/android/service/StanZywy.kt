package pl.i40.android.service

import pl.i40.android.acquisition.OilTempEstimator
import pl.i40.android.acquisition.RingBufferStore
import pl.i40.android.acquisition.RingSample
import pl.i40.android.acquisition.SampleTick
import pl.i40.android.obd.DecodedPid
import pl.i40.android.obd.FuelSystemStatus
import pl.i40.android.rules.PasmaOdniesienia

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
    var czasPozaPasmemWPetliZamknietejSekundy: Double = 0.0
    var czasWPetliZamknietejSekundy: Double = 0.0
    private var ostatniCzasLicznika: Double? = null

    fun wartosc(pid: Int): Double? {
        if (pid == PID_OLEJ) return olej.estimateC
        return najnowsze[pid]
    }

    fun samples(pid: Int): List<RingSample> = ring.samples(pid)

    fun zastosuj(tick: SampleTick) {
        if (nagrywa) {
            val prev = ostatniCzasLicznika
            if (prev != null) dodajOdstępLicznika(tick.time - prev)
            ostatniCzasLicznika = tick.time
            elapsedSeconds = tick.time
        }
        var runtime: Double? = null
        for (reading in tick.readings) {
            if (reading.pid == PID_OLEJ) continue
            val n = when (val d = reading.decoded) {
                is DecodedPid.Numeric -> d.value
                is DecodedPid.Code -> d.value.toDouble()
                else -> continue
            }
            najnowsze[reading.pid] = n
            ring.append(reading.pid, tick.time, n)
            if (reading.pid == 0x1F) runtime = n
        }
        uaktualnijOlej(tick.time, runtime)
        olej.estimateC?.let { ring.append(PID_OLEJ, tick.time, it) }
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
        olej.estimateC?.let { ring.append(PID_OLEJ, at, it) }
    }

    fun odczytyDoPunktu(): Map<Int, Double> = najnowsze.toMap()

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
        serie = (
            FormatKaflaWykresow.PIDY_WYKRESOW +
                listOf(0x06, 0x07, 0x44, 0x23, 0x43, 0x11, 0x3C, 0x05, 0x5C, 0x2E, 0x03, 0x33, 0x0B, 0x4C, 0x49)
            ).associateWith { ring.samples(it) },
        elapsedSeconds = elapsedSeconds,
        hz = measuredHz,
        queries = totalQueries,
        czasPozaPasmemWPetliZamknietejSekundy = czasPozaPasmemWPetliZamknietejSekundy,
        czasWPetliZamknietejSekundy = czasWPetliZamknietejSekundy
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

    fun resetLicznikowSesji() {
        czasPozaPasmemWPetliZamknietejSekundy = 0.0
        czasWPetliZamknietejSekundy = 0.0
        ostatniCzasLicznika = null
    }

    private fun dodajOdstępLicznika(dt: Double) {
        if (dt <= 0) return
        val stft = najnowsze[0x06] ?: return
        val ltft = najnowsze[0x07] ?: return
        if (!FuelSystemStatus.korektyWazne(najnowsze[0x03]?.toInt())) return
        czasWPetliZamknietejSekundy += dt
        if (kotlin.math.abs(stft + ltft) > PasmaOdniesienia.sumaKorekt.endInclusive) {
            czasPozaPasmemWPetliZamknietejSekundy += dt
        }
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
    val queries: Int = 0,
    val czasPozaPasmemWPetliZamknietejSekundy: Double? = null,
    val czasWPetliZamknietejSekundy: Double? = null
)

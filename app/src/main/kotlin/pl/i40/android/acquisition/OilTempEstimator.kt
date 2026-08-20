package pl.i40.android.acquisition

import kotlin.math.exp

/** Model temperatury oleju — nie pomiar OBD. Na i40 PID `5C` nie istnieje. */
class OilTempEstimator {
    enum class Pewnosc(val label: String) {
        Niska("niska"),
        Srednia("średnia"),
        Dobra("dobra"),
    }

    /** Bazowa stała czasowa τ (s) — jedyny parametr do kalibracji IR na misce. */
    var tauBase: Double = 300.0

    var estimateC: Double? = null
        private set
    private var lastT: Double? = null
    var runSeconds: Double = 0.0
        private set
    private var lastRuntimeSeconds: Double? = null

    fun reset(coolantC: Double) {
        estimateC = coolantC
        lastT = null
        runSeconds = 0.0
    }

    fun update(t: Double, coolantC: Double, loadPct: Double, ambientC: Double?, runtimeSeconds: Double? = null) {
        if (runtimeSeconds != null) {
            val previousRuntime = lastRuntimeSeconds
            if (previousRuntime != null && runtimeSeconds + 2 < previousRuntime) {
                reset(coolantC)
                lastRuntimeSeconds = runtimeSeconds
            } else {
                lastRuntimeSeconds = runtimeSeconds
            }
        }

        val previous = estimateC
        if (previous == null) {
            reset(coolantC)
            return
        }
        val last = lastT
        if (last == null) {
            lastT = t
            return
        }
        val dt = t - last
        if (dt <= 0) return
        lastT = t
        runSeconds += dt

        val load = minOf(maxOf(loadPct, 0.0), 100.0) / 100.0
        val ambient = ambientC ?: 20.0

        val target = coolantC + 25 * load
        val loadFactor = 0.6 + 0.8 * load
        val ambientFactor = 1 + (20 - ambient) / 60
        val tau = minOf(maxOf(tauBase * ambientFactor / loadFactor, 150.0), 900.0)
        val alpha = 1 - exp(-dt / tau)
        estimateC = previous + alpha * (target - previous)
    }

    val pewnosc: Pewnosc
        get() = when {
            runSeconds < 180 -> Pewnosc.Niska
            runSeconds < 600 -> Pewnosc.Srednia
            else -> Pewnosc.Dobra
        }

    val warmupProgress: Double
        get() = minOf(maxOf(runSeconds / 180.0, 0.0), 1.0)

    val silnikRozgrzany: Boolean
        get() {
            val est = estimateC ?: return false
            return est >= 90.0 && runSeconds >= 600.0
        }
}

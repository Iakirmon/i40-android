package pl.i40.android.acquisition

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OilTempEstimatorTest {
    @Test
    fun spokojnaJazdaZimnyStartPrzyblizaTabele() {
        val e = OilTempEstimator()
        val ambient = 15.0
        val load = 20.0
        step(e, from = 0.0, to = 180.0, coolantFrom = 15.0, coolantTo = 72.0, load = load, ambient = ambient)
        val po3min = requireNotNull(e.estimateC)
        assertTrue(po3min > 25 && po3min < 55)

        step(e, from = 180.0, to = 300.0, coolantFrom = 72.0, coolantTo = 88.0, load = load, ambient = ambient)
        val po5min = requireNotNull(e.estimateC)
        assertTrue(po5min > 40 && po5min < 75)

        step(e, from = 300.0, to = 620.0, coolantFrom = 88.0, coolantTo = 90.0, load = load, ambient = ambient)
        val po10min = requireNotNull(e.estimateC)
        assertTrue(po10min > 65 && po10min < 95)
        assertEquals(OilTempEstimator.Pewnosc.Dobra, e.pewnosc)

        step(e, from = 620.0, to = 1200.0, coolantFrom = 90.0, coolantTo = 90.0, load = load, ambient = ambient)
        val po20min = requireNotNull(e.estimateC)
        assertTrue(po20min > 85)
        assertTrue(po20min < 105)
    }

    @Test
    fun spadekRuntimeResetujeEstymator() {
        val e = OilTempEstimator()
        e.update(t = 0.0, coolantC = 40.0, loadPct = 20.0, ambientC = 15.0, runtimeSeconds = 100.0)
        e.update(t = 1.0, coolantC = 50.0, loadPct = 20.0, ambientC = 15.0, runtimeSeconds = 101.0)
        e.update(t = 10.0, coolantC = 70.0, loadPct = 20.0, ambientC = 15.0, runtimeSeconds = 110.0)
        assertNotNull(e.estimateC)

        e.update(t = 11.0, coolantC = 25.0, loadPct = 15.0, ambientC = 15.0, runtimeSeconds = 2.0)
        assertEquals(25.0, e.estimateC)
        assertEquals(0.0, e.runSeconds)
    }

    @Test
    fun silnikRozgrzanyWymagaCzasuITemperatury() {
        val e = OilTempEstimator()
        e.reset(coolantC = 95.0)
        e.update(t = 0.0, coolantC = 95.0, loadPct = 30.0, ambientC = 20.0)
        e.update(t = 100.0, coolantC = 95.0, loadPct = 30.0, ambientC = 20.0)
        assertFalse(e.silnikRozgrzany)

        step(e, from = 100.0, to = 800.0, coolantFrom = 95.0, coolantTo = 95.0, load = 40.0, ambient = 20.0)
        assertTrue(e.silnikRozgrzany)
    }

    @Test
    fun zbiegaDoPlynuPlus25RazyObciazenie() {
        val e = OilTempEstimator()
        e.reset(coolantC = 90.0)
        repeat(2000) { i ->
            e.update(t = i.toDouble(), coolantC = 90.0, loadPct = 100.0, ambientC = 20.0)
        }
        val estimate = requireNotNull(e.estimateC)
        assertTrue(kotlin.math.abs(estimate - 115.0) < 1.0)
    }

    private fun step(
        estimator: OilTempEstimator,
        from: Double,
        to: Double,
        coolantFrom: Double,
        coolantTo: Double,
        load: Double,
        ambient: Double,
        dt: Double = 1.0
    ) {
        var t = from
        while (t <= to) {
            val f = (t - from) / maxOf(to - from, 0.001)
            val coolant = coolantFrom + (coolantTo - coolantFrom) * f
            estimator.update(
                t = t,
                coolantC = coolant,
                loadPct = load,
                ambientC = ambient,
                runtimeSeconds = t,
            )
            t += dt
        }
    }
}

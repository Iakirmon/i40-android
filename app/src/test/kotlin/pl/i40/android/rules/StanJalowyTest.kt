package pl.i40.android.rules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StanJalowyTest {
    @Test
    fun wszystkoSpelnioneToJalowyRozgrzany() {
        assertTrue(PasmaOdniesienia.jalowyRozgrzany(rpm = 750.0, predkoscKmh = 0.0, plynC = 90.0, runtimeS = 700.0))
    }

    @Test
    fun obroty400ResztaSpelnionaToNie() {
        assertFalse(PasmaOdniesienia.jalowyRozgrzany(rpm = 400.0, predkoscKmh = 0.0, plynC = 90.0, runtimeS = 700.0))
    }

    @Test
    fun predkosc3ResztaSpelnionaToNie() {
        assertFalse(PasmaOdniesienia.jalowyRozgrzany(rpm = 750.0, predkoscKmh = 3.0, plynC = 90.0, runtimeS = 700.0))
    }

    @Test
    fun plyn65ResztaSpelnionaToNie() {
        assertFalse(PasmaOdniesienia.jalowyRozgrzany(rpm = 750.0, predkoscKmh = 0.0, plynC = 65.0, runtimeS = 700.0))
    }

    @Test
    fun czasPracy400ResztaSpelnionaToNie() {
        assertFalse(PasmaOdniesienia.jalowyRozgrzany(rpm = 750.0, predkoscKmh = 0.0, plynC = 90.0, runtimeS = 400.0))
    }

    @Test
    fun teSameStaleCoGdi1() {
        assertEquals(PasmaOdniesienia.plyn.start, 70.0, 0.0)
        assertEquals(PasmaOdniesienia.CZAS_ROZGRZANY_S, 600.0, 0.0)
        assertEquals(PasmaOdniesienia.OBROTY_PRACA_MIN, 500.0, 0.0)
        val jalowy = PasmaOdniesienia.jalowyRozgrzany(800.0, 0.0, 80.0, 700.0)
        val gdi = PasmaOdniesienia.silnikRozgrzany(80.0, 700.0) &&
            800.0 > PasmaOdniesienia.OBROTY_PRACA_MIN
        assertTrue(jalowy)
        assertTrue(gdi)
        assertFalse(PasmaOdniesienia.jalowyRozgrzany(800.0, 1.0, 80.0, 700.0))
    }
}

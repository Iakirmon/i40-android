package pl.i40.android.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.acquisition.SampleStream
import pl.i40.android.storage.PunktOdniesienia
import pl.i40.android.ui.FormatOdniesienia

class LicznikWejsciaPorownaniaTest {
    @Test
    fun wejsciePoPelnychObiegu() {
        val l = LicznikWejsciaPorownania()
        repeat(SampleStream.SLOW_EVERY_N - 1) {
            assertFalse(l.naCyklGoracy(true))
        }
        assertTrue(l.naCyklGoracy(true))
    }

    @Test
    fun wyjscieNatychmiast() {
        val l = LicznikWejsciaPorownania()
        repeat(SampleStream.SLOW_EVERY_N) { l.naCyklGoracy(true) }
        assertTrue(l.naCyklGoracy(true))
        assertFalse(l.naCyklGoracy(false))
    }
}

class FormatOdniesieniaTest {
    @Test
    fun trzyStopnieProgresjiOdDrugiegoPunktu() {
        assertEquals(FormatOdniesienia.PIERWSZY_POMIAR, FormatOdniesienia.wiersz(0x0C, emptyList()))
        val jeden = listOf(punkt(708.0))
        val t1 = FormatOdniesienia.wiersz(0x0C, jeden)
        assertTrue(t1.startsWith("poprzednio"))
        assertTrue(t1.contains("708"))
        assertFalse(t1.contains("pomiarów"))
        assertFalse(t1.contains("norma"))
        val dwa = listOf(punkt(705.0), punkt(714.0))
        val t2 = FormatOdniesienia.wiersz(0x0C, dwa)
        assertTrue(t2.contains("pomiarów"))
        assertTrue(t2.contains("705"))
        assertTrue(t2.contains("714"))
        assertFalse(t2.contains("norma"))
    }

    private fun punkt(rpm: Double) = PunktOdniesienia(
        id = rpm.toString(),
        kiedyMs = 1L,
        vin = "VIN",
        stan = "jalowy_rozgrzany",
        zrodlo = "przejazd",
        probek = 20,
        odczyty = mapOf(0x0C to rpm)
    )
}
